package org.jellyfin.androidtv.ui.home

import android.content.Context
import org.jellyfin.androidtv.R
import org.jellyfin.androidtv.auth.repository.UserRepository
import org.jellyfin.androidtv.constant.ChangeTriggerType
import org.jellyfin.androidtv.data.repository.ItemRepository
import org.jellyfin.androidtv.ui.browsing.BrowseRowDef
import org.jellyfin.sdk.model.api.BaseItemDto
import org.jellyfin.sdk.model.api.BaseItemKind
import org.jellyfin.sdk.model.api.MediaType
import org.jellyfin.sdk.model.api.request.GetItemsRequest
import timber.log.Timber
import org.jellyfin.sdk.model.api.request.GetRecommendedProgramsRequest
import org.jellyfin.sdk.model.api.request.GetRecordingsRequest
import org.jellyfin.sdk.model.api.request.GetResumeItemsRequest
import org.jellyfin.sdk.model.api.request.GetNextUpRequest

import org.jellyfin.sdk.model.api.ItemSortBy

class HomeFragmentHelper(
    private val context: Context,
    private val userRepository: UserRepository
) {
    // ... (previous methods remain the same)

    fun loadSuggestedMoviesRow(): HomeFragmentRow {
        // Get the current user ID
        val currentUserId = userRepository.currentUser.value?.id

        // Create a query that uses Jellyfin's recommendation capabilities
        if (currentUserId != null) {
            // Create a query for recently played movies to get genres the user likes
            val recentlyPlayedQuery = org.jellyfin.sdk.model.api.request.GetItemsRequest(
                userId = currentUserId,
                includeItemTypes = setOf(BaseItemKind.MOVIE),
                sortBy = setOf(ItemSortBy.DATE_PLAYED),
                sortOrder = setOf(org.jellyfin.sdk.model.api.SortOrder.DESCENDING),
                filters = setOf(org.jellyfin.sdk.model.api.ItemFilter.IS_PLAYED),
                limit = 5,  // Just get a few recently watched items to extract genres
                fields = ItemRepository.itemFields,
                recursive = true
            )

            // Create a generic recommended items query
            // This will show unplayed movies sorted by which match the user's taste better
            val suggestedQuery = org.jellyfin.sdk.model.api.request.GetItemsRequest(
                userId = currentUserId,
                includeItemTypes = setOf(BaseItemKind.MOVIE),
                // Sort by rating to get better movies first
                sortBy = setOf(ItemSortBy.COMMUNITY_RATING),
                sortOrder = setOf(org.jellyfin.sdk.model.api.SortOrder.DESCENDING),
                // Only show unwatched content
                isPlayed = false,
                recursive = true,
                limit = 50,
                fields = ItemRepository.itemFields,
                excludeItemTypes = setOf(BaseItemKind.EPISODE),
                // Using a null filter here will get Jellyfin to give recommended items
                filters = null
            )

            // Create a row with the suggested movies
            return HomeFragmentBrowseRowDefRow(BrowseRowDef("Suggested Movies", suggestedQuery, 50))
        } else {
            // Fallback query if no user is available
            val fallbackQuery = org.jellyfin.sdk.model.api.request.GetItemsRequest(
                includeItemTypes = setOf(BaseItemKind.MOVIE),
                sortBy = setOf(ItemSortBy.RANDOM),
                limit = 50,
                fields = ItemRepository.itemFields,
                recursive = true,
                excludeItemTypes = setOf(BaseItemKind.EPISODE)
            )

            return HomeFragmentBrowseRowDefRow(BrowseRowDef("Suggested Movies", fallbackQuery, 50))
        }
    }

    fun loadSuggestedTvShowsRow(): HomeFragmentRow {
        Timber.d("Loading Suggested TV Shows row")
        // Get the current user ID
        val currentUserId = userRepository.currentUser.value?.id

        // Create a query that uses Jellyfin's recommendation capabilities
        if (currentUserId != null) {
            // Create a query for recently played TV shows to get genres the user likes
            val recentlyPlayedQuery = org.jellyfin.sdk.model.api.request.GetItemsRequest(
                userId = currentUserId,
                includeItemTypes = setOf(BaseItemKind.SERIES),
                sortBy = setOf(ItemSortBy.DATE_PLAYED),
                sortOrder = setOf(org.jellyfin.sdk.model.api.SortOrder.DESCENDING),
                filters = setOf(org.jellyfin.sdk.model.api.ItemFilter.IS_PLAYED),
                limit = 5,  // Just get a few recently watched items to extract genres
                fields = ItemRepository.itemFields,
                recursive = true
            )

            // Create a generic recommended items query
            // This will show unplayed TV shows sorted by which match the user's taste better
            val suggestedQuery = org.jellyfin.sdk.model.api.request.GetItemsRequest(
                userId = currentUserId,
                includeItemTypes = setOf(BaseItemKind.SERIES),
                // Sort by rating to get better shows first
                sortBy = setOf(ItemSortBy.COMMUNITY_RATING),
                sortOrder = setOf(org.jellyfin.sdk.model.api.SortOrder.DESCENDING),
                // Only show unwatched content
                isPlayed = false,
                recursive = true,
                limit = 50,
                fields = ItemRepository.itemFields,
                excludeItemTypes = setOf(BaseItemKind.EPISODE),
                // Using a null filter here will get Jellyfin to give recommended items
                filters = null
            )

            // Create a row with the suggested TV shows
            val row = HomeFragmentBrowseRowDefRow(BrowseRowDef(context.getString(R.string.lbl_suggested_tv_shows), suggestedQuery, 50))
            Timber.d("Suggested TV Shows row created: $row")
            return row
        } else {
            // Fallback query if no user is available
            val fallbackQuery = org.jellyfin.sdk.model.api.request.GetItemsRequest(
                includeItemTypes = setOf(BaseItemKind.SERIES),
                sortBy = setOf(ItemSortBy.RANDOM),
                limit = 50,
                fields = ItemRepository.itemFields,
                recursive = true,
                excludeItemTypes = setOf(BaseItemKind.EPISODE)
            )

            val row = HomeFragmentBrowseRowDefRow(BrowseRowDef(context.getString(R.string.lbl_suggested_tv_shows), fallbackQuery, 50))
            Timber.d("Suggested TV Shows fallback row created: $row")
            return row
        }
    }

    fun loadMyCollectionsRow(): HomeFragmentRow {
        val query = org.jellyfin.sdk.model.api.request.GetItemsRequest(
            fields = ItemRepository.itemFields,
            includeItemTypes = setOf(BaseItemKind.BOX_SET),
            recursive = true,
            imageTypeLimit = 1,
            limit = 20,
            sortBy = setOf(ItemSortBy.SORT_NAME),
        )
        return HomeFragmentBrowseRowDefRow(
            org.jellyfin.androidtv.ui.browsing.BrowseRowDef(context.getString(R.string.lbl_my_collections), query, 20)
        )
    }

    fun loadFavoritesRow(): HomeFragmentRow {
        val query = GetItemsRequest(
            isFavorite = true,
            sortBy = setOf(ItemSortBy.DATE_CREATED),
            limit = 20,
            fields = ItemRepository.itemFields,
            recursive = true,
            excludeItemTypes = setOf(
                BaseItemKind.EPISODE,
                BaseItemKind.MUSIC_ARTIST,
                BaseItemKind.MUSIC_ALBUM,
                BaseItemKind.MUSIC_VIDEO,
                BaseItemKind.AUDIO
            )
        )
        return HomeFragmentBrowseRowDefRow(BrowseRowDef(context.getString(R.string.lbl_favorites), query, 20))
    }

    fun loadSciFiRow(): HomeFragmentRow = genreRow("Science Fiction")
    fun loadComedyRow(): HomeFragmentRow = genreRow("Comedy")
        ?: genreRow("Comedies")
    fun loadRomanceRow(): HomeFragmentRow = genreRow("Romance")
        ?: genreRow("Romantic")
    fun loadAnimeRow(): HomeFragmentRow = genreRow("Anime")
        ?: genreRow("Animation Japonaise")
    fun loadActionRow(): HomeFragmentRow = genreRow("Action")
        ?: genreRow("Action & Adventure")
        ?: genreRow("Action/Adventure")
    fun loadActionAdventureRow(): HomeFragmentRow = genreRow("Action & Adventure")
        ?: genreRow("Action/Adventure")
        ?: genreRow("Action Adventure")
    fun loadDocumentaryRow(): HomeFragmentRow = genreRow("Documentary")
        ?: genreRow("Documentaries")
    fun loadRealityRow(): HomeFragmentRow = genreRow("Reality")
        ?: genreRow("Reality TV")
        ?: genreRow("Reality Television")
    fun loadFamilyRow(): HomeFragmentRow = genreRow("Family")
        ?: genreRow("Kids & Family")
        ?: genreRow("Family & Kids")
    fun loadHorrorRow(): HomeFragmentRow = genreRow("Horror")
        ?: genreRow("Horror Movies")
    fun loadFantasyRow(): HomeFragmentRow = genreRow("Fantasy")
        ?: genreRow("Fantastique")
    fun loadHistoryRow(): HomeFragmentRow = genreRow("History")
        ?: genreRow("Historical")
    fun loadMusicRow(): HomeFragmentRow {
        val currentUserId = userRepository.currentUser.value?.id
        val musicPlaylistQuery = org.jellyfin.sdk.model.api.request.GetItemsRequest(
            userId = currentUserId,
            includeItemTypes = setOf(BaseItemKind.PLAYLIST),
            mediaTypes = setOf(MediaType.AUDIO),
            sortBy = setOf(ItemSortBy.SORT_NAME),
            limit = 50,
            fields = ItemRepository.itemFields,
            recursive = true,
            excludeItemTypes = setOf(BaseItemKind.MOVIE, BaseItemKind.SERIES, BaseItemKind.EPISODE)
        )
        return HomeFragmentBrowseRowDefRow(BrowseRowDef(context.getString(R.string.lbl_music_playlists), musicPlaylistQuery, 50))
    }
    fun loadMysteryRow(): HomeFragmentRow = genreRow("Mystery")
        ?: genreRow("Mysteries")
    fun loadThrillerRow(): HomeFragmentRow = genreRow("Thriller")
        ?: genreRow("Suspense")
    fun loadWarRow(): HomeFragmentRow = genreRow("War")
        ?: genreRow("War & Politics")


    fun loadRecentlyAdded(userViews: Collection<BaseItemDto>): HomeFragmentRow {
        return HomeFragmentLatestRow(userRepository, userViews)
    }

    fun loadResumeVideo(): HomeFragmentRow {
        return loadResume(context.getString(R.string.lbl_continue_watching), listOf(MediaType.VIDEO))
    }

    fun loadResumeAudio(): HomeFragmentRow {
        return loadResume(context.getString(R.string.continue_listening), listOf(MediaType.AUDIO))
    }

    fun loadLatestLiveTvRecordings(): HomeFragmentRow {
        val query = GetRecordingsRequest(
            fields = ItemRepository.itemFields,
            enableImages = true,
            limit = ITEM_LIMIT_RECORDINGS
        )

        return HomeFragmentBrowseRowDefRow(BrowseRowDef(context.getString(R.string.lbl_recordings), query))
    }

    fun loadNextUp(): HomeFragmentRow {
        val query = GetNextUpRequest(
            imageTypeLimit = 1,
            limit = ITEM_LIMIT_NEXT_UP,
            enableResumable = false,
            fields = ItemRepository.itemFields
        )

        return HomeFragmentBrowseRowDefRow(BrowseRowDef(context.getString(R.string.lbl_next_up), query, arrayOf(ChangeTriggerType.TvPlayback)))
    }

    fun loadOnNow(): HomeFragmentRow {
        val query = GetRecommendedProgramsRequest(
            isAiring = true,
            fields = ItemRepository.itemFields,
            imageTypeLimit = 1,
            enableTotalRecordCount = false,
            limit = ITEM_LIMIT_ON_NOW
        )

        return HomeFragmentBrowseRowDefRow(BrowseRowDef(context.getString(R.string.lbl_on_now), query))
    }

    private fun loadResume(title: String, includeMediaTypes: Collection<MediaType>): HomeFragmentRow {
        val query = GetResumeItemsRequest(
            limit = ITEM_LIMIT_RESUME,
            fields = ItemRepository.itemFields,
            imageTypeLimit = 1,
            enableTotalRecordCount = false,
            mediaTypes = includeMediaTypes,
            excludeItemTypes = setOf(BaseItemKind.AUDIO_BOOK),
        )

        return HomeFragmentBrowseRowDefRow(BrowseRowDef(title, query, 0, false, true, arrayOf(ChangeTriggerType.TvPlayback, ChangeTriggerType.MoviePlayback)))
    }

    private fun genreRow(genreName: String): HomeFragmentRow {
        val query = org.jellyfin.sdk.model.api.request.GetItemsRequest(
            genres = setOf(genreName),
            sortBy = setOf(ItemSortBy.COMMUNITY_RATING),
            sortOrder = setOf(org.jellyfin.sdk.model.api.SortOrder.DESCENDING),
            limit = 50,
            fields = ItemRepository.itemFields,
            recursive = true,
            excludeItemTypes = setOf(BaseItemKind.EPISODE)
        )

        return HomeFragmentBrowseRowDefRow(BrowseRowDef(genreName, query, 50))
    }

    companion object {
        // Maximum amount of items loaded for a row
        private const val ITEM_LIMIT_RESUME = 50
        private const val ITEM_LIMIT_RECORDINGS = 40
        private const val ITEM_LIMIT_NEXT_UP = 50
        private const val ITEM_LIMIT_ON_NOW = 20
    }
}
