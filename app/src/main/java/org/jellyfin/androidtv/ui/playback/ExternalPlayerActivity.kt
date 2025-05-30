package org.jellyfin.androidtv.ui.playback

import android.content.ActivityNotFoundException
import android.content.Intent
import android.os.Bundle
import android.widget.Toast
import androidx.activity.result.contract.ActivityResultContracts
import androidx.core.net.toUri
import androidx.fragment.app.FragmentActivity
import androidx.lifecycle.lifecycleScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import org.jellyfin.androidtv.R
import org.jellyfin.androidtv.data.model.DataRefreshService
import org.jellyfin.androidtv.util.sdk.getDisplayName
import org.jellyfin.sdk.api.client.ApiClient
import org.jellyfin.sdk.api.client.extensions.playStateApi
import org.jellyfin.sdk.api.client.extensions.subtitleApi
import org.jellyfin.sdk.api.client.extensions.userLibraryApi
import org.jellyfin.sdk.api.client.extensions.videosApi
import org.jellyfin.sdk.model.api.BaseItemDto
import org.jellyfin.sdk.model.api.BaseItemKind
import org.jellyfin.sdk.model.api.MediaSourceInfo
import org.jellyfin.sdk.model.api.MediaStream
import org.jellyfin.sdk.model.api.MediaStreamType
import org.jellyfin.sdk.model.api.MediaType
import org.jellyfin.sdk.model.api.PlaybackStopInfo
import org.jellyfin.sdk.model.extensions.inWholeTicks
import org.jellyfin.sdk.model.extensions.ticks
import org.jellyfin.sdk.model.serializer.toUUIDOrNull
import org.koin.android.ext.android.inject
import timber.log.Timber
import java.io.File
import java.time.Instant
import kotlin.time.Duration
import kotlin.time.Duration.Companion.milliseconds

/**
 * Activity that, once opened, opens the first item of the [VideoQueueManager.getCurrentVideoQueue] list in an external media player app.
 * Once returned it will notify the server of item completion.
 */
class ExternalPlayerActivity : FragmentActivity() {
	companion object {
		const val EXTRA_POSITION = "position"

		// Minimum percentage of the video that needs to be watched to be marked as completed
		private const val MINIMUM_COMPLETION_PERCENTAGE = 0.9
		
		// Minimum duration (in seconds) that needs to be watched to update the resume position
		private const val MINIMUM_WATCH_DURATION_SECONDS = 10L

		// https://mx.j2inter.com/api
		private const val API_MX_TITLE = "title"
		private const val API_MX_SEEK_POSITION = "position"
		private const val API_MX_FILENAME = "filename"
		private const val API_MX_SECURE_URI = "secure_uri"
		private const val API_MX_RETURN_RESULT = "return_result"
		private const val API_MX_RESULT_POSITION = "position"
		private const val API_MX_SUBS = "subs"
		private const val API_MX_SUBS_NAME = "subs.name"
		private const val API_MX_SUBS_FILENAME = "subs.filename"

		// https://wiki.videolan.org/Android_Player_Intents/
		private const val API_VLC_SUBTITLES = "subtitles_location"
		private const val API_VLC_RESULT_POSITION = "extra_position"

		// https://www.vimu.tv/player-api
		private const val API_VIMU_TITLE = "forcename"
		private const val API_VIMU_SEEK_POSITION = "startfrom"
		private const val API_VIMU_RESUME = "forceresume"

		// The extra keys used by various video players to read the end position
		private val resultPositionExtras = arrayOf(API_MX_RESULT_POSITION, API_VLC_RESULT_POSITION)
	}

	private val videoQueueManager by inject<VideoQueueManager>()
	private val dataRefreshService by inject<DataRefreshService>()
	private val api by inject<ApiClient>()

	private val playVideoLauncher = registerForActivityResult(ActivityResultContracts.StartActivityForResult()) { result ->
		Timber.i("Playback finished with result code ${result.resultCode}")
		
		// Only show an error if the result indicates a failure (like RESULT_CANCELED with specific error data)
		val isError = result.resultCode == RESULT_CANCELED && result.data?.extras?.keySet()?.isNotEmpty() == true
		
		if (isError) {
			Timber.w("External player reported an error: ${result.data}")
			Toast.makeText(this, R.string.video_error_unknown_error, Toast.LENGTH_LONG).show()
		}
		
		// Always update the queue position and process the result
		videoQueueManager.setCurrentMediaPosition(videoQueueManager.getCurrentMediaPosition() + 1)
		onItemFinished(result.data)
	}

	private var currentItem: Pair<BaseItemDto, MediaSourceInfo>? = null

	override fun onCreate(savedInstanceState: Bundle?) {
		super.onCreate(savedInstanceState)

		val position = intent.getLongExtra(EXTRA_POSITION, 0).milliseconds
		playNext(position)
	}

	private fun playNext(position: Duration = Duration.ZERO) {
		val currentPosition = videoQueueManager.getCurrentMediaPosition()
		val item = videoQueueManager.getCurrentVideoQueue().getOrNull(currentPosition) ?: return finish()
		val mediaSource = item.mediaSources?.firstOrNull { it.id?.toUUIDOrNull() == item.id }

		if (mediaSource == null) {
			Toast.makeText(this, R.string.msg_no_playable_items, Toast.LENGTH_LONG).show()
			finish()
		} else {
			playItem(item, mediaSource, position)
		}
	}

	private fun playItem(item: BaseItemDto, mediaSource: MediaSourceInfo, position: Duration) {
		val url = api.videosApi.getVideoStreamUrl(
			itemId = item.id,
			mediaSourceId = mediaSource.id,
			static = true,
		)

		val title = item.getDisplayName(this)
		val fileName = mediaSource.path?.let { File(it).name }
		val externalSubtitles = mediaSource.mediaStreams
			?.filter { it.type == MediaStreamType.SUBTITLE && it.isExternal }
			?.sortedWith(compareBy<MediaStream> { it.isDefault }.thenBy { it.index })
			.orEmpty()

		val subtitleUrls = externalSubtitles.map {
			api.subtitleApi.getSubtitleUrl(
				routeItemId = item.id,
				routeMediaSourceId = mediaSource.id.toString(),
				routeIndex = it.index,
				routeFormat = it.codec.orEmpty(),
			)
		}.toTypedArray()
		val subtitleNames = externalSubtitles.map { it.displayTitle ?: it.title.orEmpty() }.toTypedArray()
		val subtitleLanguages = externalSubtitles.map { it.language.orEmpty() }.toTypedArray()

		Timber.i(
			"Starting item ${item.id} from $position with ${subtitleUrls.size} external subtitles: $url${
				subtitleUrls.joinToString(", ", ", ")
			}"
		)

		val playIntent = Intent(Intent.ACTION_VIEW).apply {
			val mediaType = when (item.mediaType) {
				MediaType.VIDEO -> "video/*"
				MediaType.AUDIO -> "audio/*"
				else -> null
			}

			setDataAndTypeAndNormalize(url.toUri(), mediaType)

			putExtra(API_MX_SEEK_POSITION, position.inWholeMilliseconds.toInt())
			putExtra(API_MX_RETURN_RESULT, true)
			putExtra(API_MX_TITLE, title)
			putExtra(API_MX_FILENAME, fileName)
			putExtra(API_MX_SECURE_URI, true)
			putExtra(API_MX_SUBS, subtitleUrls)
			putExtra(API_MX_SUBS_NAME, subtitleNames)
			putExtra(API_MX_SUBS_FILENAME, subtitleLanguages)

			if (subtitleUrls.isNotEmpty()) putExtra(API_VLC_SUBTITLES, subtitleUrls.first().toString())

			putExtra(API_VIMU_SEEK_POSITION, position.inWholeMilliseconds.toInt())
			putExtra(API_VIMU_RESUME, false)
			putExtra(API_VIMU_TITLE, title)
		}

		try {
			currentItem = item to mediaSource
			playVideoLauncher.launch(playIntent)
		} catch (_: ActivityNotFoundException) {
			Toast.makeText(this, R.string.no_player_message, Toast.LENGTH_LONG).show()
			finish()
		}
	}

    private fun onItemFinished(result: Intent?) {
        if (currentItem == null) {
            Timber.w("No current item when finishing playback")
            // Don't show an error here as it might be a normal exit
            finish()
            return
        }

        val (item, mediaSource) = currentItem!!
        val extras = result?.extras ?: Bundle.EMPTY

        // Get the final position from the external player
        val endPosition = Companion.resultPositionExtras.firstNotNullOfOrNull { key ->
            @Suppress("DEPRECATION") val value = extras.get(key)
            if (value is Number) value.toLong().milliseconds
            else null
        }

        val runtime = (mediaSource.runTimeTicks ?: item.runTimeTicks)?.ticks
        
        // Only mark as watched if a significant portion was played
        val shouldMarkAsWatched = runtime?.let { 
            endPosition != null && endPosition >= (it * MINIMUM_COMPLETION_PERCENTAGE)
        } ?: false
        
        // Only update the resume position if enough time was watched
        val shouldUpdateResumePosition = runtime?.let {
            endPosition != null && 
            endPosition.inWholeSeconds >= MINIMUM_WATCH_DURATION_SECONDS &&
            endPosition < (it * 0.9) // Don't update if we're close to the end
        } ?: false

        Timber.d("Playback finished - Runtime: ${runtime?.inWholeSeconds}s, Position: ${endPosition?.inWholeSeconds}s, " +
                "Mark as watched: $shouldMarkAsWatched, Update resume: $shouldUpdateResumePosition")

        lifecycleScope.launch {
            try {
                withContext(Dispatchers.IO) {
                    // Only report stop if we have a valid position or we're marking as watched
                    if (shouldMarkAsWatched || shouldUpdateResumePosition) {
                        // Report the playback stopped with the final position
                        api.playStateApi.reportPlaybackStopped(
                            PlaybackStopInfo(
                                itemId = item.id,
                                mediaSourceId = mediaSource.id,
                                positionTicks = if (shouldMarkAsWatched) null else endPosition?.inWholeTicks,
                                failed = false,
                            )
                        )
                        
                        // If we're marking as watched, also update the user data
                        if (shouldMarkAsWatched) {
                            Timber.d("Marking item ${item.id} as watched")
                            // The playStateApi call above with positionTicks=null should mark as watched
                            // No need for additional API calls
                        }
                    } else {
                        Timber.d("Not enough watch time to update progress")
                    }
                }
            } catch (error: Exception) {
                Timber.w(error, "Failed to report playback stop event")
                // Don't show an error toast as it might be confusing to the user
            }

            // Update the last playback time
            dataRefreshService.lastPlayback = Instant.now()
            when (item.type) {
                BaseItemKind.MOVIE -> dataRefreshService.lastMoviePlayback = Instant.now()
                BaseItemKind.EPISODE -> dataRefreshService.lastTvPlayback = Instant.now()
                else -> Unit
            }

            // Only auto-play next if we've watched enough of the current item
            if (shouldMarkAsWatched) {
                playNext()
            } else {
                finish()
            }
        }
    }
}
