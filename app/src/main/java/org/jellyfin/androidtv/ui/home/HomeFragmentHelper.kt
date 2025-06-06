package org.jellyfin.androidtv.ui.home

import android.annotation.SuppressLint
import android.content.Context
import android.widget.ImageView
import androidx.leanback.widget.BaseCardView
import androidx.leanback.widget.Presenter
import org.jellyfin.androidtv.R
import org.jellyfin.androidtv.auth.repository.UserRepository
import org.jellyfin.androidtv.constant.ChangeTriggerType
import org.jellyfin.androidtv.constant.ImageType as JellyfinImageType
import org.jellyfin.androidtv.data.repository.ItemRepository
import org.jellyfin.androidtv.ui.browsing.BrowseRowDef
import org.jellyfin.androidtv.ui.card.LegacyImageCardView
import org.jellyfin.androidtv.ui.presentation.MutableObjectAdapter
import org.jellyfin.androidtv.util.ImageHelper
import org.jellyfin.sdk.model.api.BaseItemKind
import org.jellyfin.sdk.model.api.ImageType
import org.jellyfin.sdk.model.api.ItemSortBy
import org.jellyfin.sdk.model.api.SortOrder
import org.jellyfin.sdk.model.api.MediaType
import org.jellyfin.sdk.model.api.request.GetItemsRequest
import org.jellyfin.sdk.model.api.request.GetNextUpRequest
import org.jellyfin.sdk.model.api.request.GetRecommendedProgramsRequest
import org.jellyfin.sdk.model.api.request.GetRecordingsRequest
import org.jellyfin.sdk.model.api.request.GetResumeItemsRequest
import timber.log.Timber

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
            val row = HomeFragmentBrowseRowDefRow(BrowseRowDef("Suggested Movies", suggestedQuery, 50, false, true))
            return createNoInfoRow(row)
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

            val row = HomeFragmentBrowseRowDefRow(BrowseRowDef("Suggested Movies", fallbackQuery, 50, false, true))
            return createNoInfoRow(row)
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
            val row = HomeFragmentBrowseRowDefRow(BrowseRowDef(context.getString(R.string.lbl_suggested_tv_shows), suggestedQuery, 50, false, true))
            Timber.d("Suggested TV Shows row created: $row")
            return createNoInfoRow(row)
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

            val row = HomeFragmentBrowseRowDefRow(BrowseRowDef(context.getString(R.string.lbl_suggested_tv_shows), fallbackQuery, 50, false, true))
            Timber.d("Suggested TV Shows fallback row created: $row")
            return createNoInfoRow(row)
        }
    }

    fun loadMyCollectionsRow(): HomeFragmentRow {
        val query = org.jellyfin.sdk.model.api.request.GetItemsRequest(
            fields = ItemRepository.itemFields,
            includeItemTypes = setOf(BaseItemKind.BOX_SET),
			sortOrder = SortOrder.Descending,
			filters = setOf(ItemFilter.IS_FAVORITE),
            recursive = true,
            imageTypeLimit = 1,
            limit = 20,
            sortBy = setOf(ItemSortBy.DATE_CREATED),
        )

        // Create a custom row with no-info card style
        val row = HomeFragmentBrowseRowDefRow(
            org.jellyfin.androidtv.ui.browsing.BrowseRowDef(context.getString(R.string.lbl_my_collections), query, 20, false, true)
        )

        return createNoInfoRow(row)
    }

    fun loadFavoritesRow(): HomeFragmentRow {
        val query = GetItemsRequest(
            isFavorite = true,
            sortBy = setOf(ItemSortBy.DATE_CREATED),
            limit = 20,
            fields = ItemRepository.itemFields,
            recursive = true,
            excludeItemTypes = setOf(
                BaseItemKind.BOX_SET,
				BaseItemKind.EPISODE,
                BaseItemKind.MUSIC_ARTIST,
                BaseItemKind.MUSIC_ALBUM,
                BaseItemKind.MUSIC_VIDEO,
                BaseItemKind.AUDIO
            )
        )

        // Create a custom row with no-info card style
        val row = HomeFragmentBrowseRowDefRow(BrowseRowDef(context.getString(R.string.lbl_favorites), query, 20, false, true))
        return createNoInfoRow(row)
    }

    fun loadSciFiRow(): HomeFragmentRow = genreRow("Science Fiction")
    fun loadComedyRow(): HomeFragmentRow = genreRow("Comedy")
    fun loadRomanceRow(): HomeFragmentRow = genreRow("Romance")
    fun loadAnimeRow(): HomeFragmentRow = genreRow("Anime")
    fun loadActionRow(): HomeFragmentRow = genreRow("Action")
    fun loadActionAdventureRow(): HomeFragmentRow = genreRow("Action & Adventure")
    fun loadDocumentaryRow(): HomeFragmentRow = genreRow("Documentary")
    fun loadRealityRow(): HomeFragmentRow = genreRow("Reality")
    fun loadFamilyRow(): HomeFragmentRow = genreRow("Family")
    fun loadHorrorRow(): HomeFragmentRow = genreRow("Horror")
    fun loadFantasyRow(): HomeFragmentRow = genreRow("Fantasy")
    fun loadHistoryRow(): HomeFragmentRow = genreRow("History")
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
    fun loadThrillerRow(): HomeFragmentRow = genreRow("Thriller")
    fun loadWarRow(): HomeFragmentRow = genreRow("War")


    fun loadRecentlyAdded(userViews: Collection<org.jellyfin.sdk.model.api.BaseItemDto>): HomeFragmentRow {
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

        // Create a custom row that will handle episode cards with consistent sizing
        return object : HomeFragmentRow {
            override fun addToRowsAdapter(
                context: Context,
                cardPresenter: org.jellyfin.androidtv.ui.presentation.CardPresenter,
                rowsAdapter: org.jellyfin.androidtv.ui.presentation.MutableObjectAdapter<androidx.leanback.widget.Row>
            ) {
                // Use a custom card presenter to ensure consistent sizing with Continue Watching
                val customCardPresenter = object : org.jellyfin.androidtv.ui.presentation.CardPresenter(true, 150) {
                    override fun onBindViewHolder(viewHolder: Presenter.ViewHolder, item: Any) {
                        super.onBindViewHolder(viewHolder, item)

                        // Apply default card presentation
                        (viewHolder.view as? org.jellyfin.androidtv.ui.card.LegacyImageCardView)?.apply {
                            setCardType(BaseCardView.CARD_TYPE_INFO_UNDER)
                            // Match the dimensions of Continue Watching cards (width: 260dp, height: 150dp)
                            setMainImageDimensions(260, 150)
                        }
                    }
                }.apply {
                    setHomeScreen(true)
                    setUniformAspect(true)
                }

                // Add the row with our custom card presenter
                HomeFragmentBrowseRowDefRow(BrowseRowDef(
                    context.getString(R.string.lbl_next_up),
                    query,
                    arrayOf(ChangeTriggerType.TvPlayback)
                )).addToRowsAdapter(context, customCardPresenter, rowsAdapter)
            }
        }
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

        // Create a custom row that will handle movie and episode cards differently
        return object : HomeFragmentRow {
            override fun addToRowsAdapter(
                context: Context,
                cardPresenter: org.jellyfin.androidtv.ui.presentation.CardPresenter,
                rowsAdapter: org.jellyfin.androidtv.ui.presentation.MutableObjectAdapter<androidx.leanback.widget.Row>
            ) {
                // Create a custom card presenter that uses thumb posters for movies and shows info
                val continueWatchingPresenter = object : org.jellyfin.androidtv.ui.presentation.CardPresenter(
                    true,  // showInfo - set to true to show info below cards
                    org.jellyfin.androidtv.constant.ImageType.THUMB,  // Use THUMB image type
                    260  // Increase height to accommodate info text
                ) {
                    override fun onBindViewHolder(viewHolder: Presenter.ViewHolder, item: Any) {
                        super.onBindViewHolder(viewHolder, item)
                        
                        // Set fixed dimensions for all cards in the row
                        (viewHolder.view as? org.jellyfin.androidtv.ui.card.LegacyImageCardView)?.let { cardView ->
                            cardView.setMainImageDimensions(260, 150) // Match Next up card height
                            // Ensure card type is set to show info below
                            cardView.cardType = BaseCardView.CARD_TYPE_INFO_UNDER
                        }
                    }
                }.apply {
                    setHomeScreen(true)
                    setUniformAspect(true)
                }

                // Add the row with our custom card presenter
                HomeFragmentBrowseRowDefRow(BrowseRowDef(
                    title,
                    query,
                    0,
                    false,
                    true,
                    arrayOf(ChangeTriggerType.TvPlayback, ChangeTriggerType.MoviePlayback)
                )).addToRowsAdapter(context, continueWatchingPresenter, rowsAdapter)
            }
        }
    }

    companion object {
        private const val ITEM_LIMIT_RESUME = 50
        private const val ITEM_LIMIT_RECORDINGS = 40
        private const val ITEM_LIMIT_NEXT_UP = 50
        private const val ITEM_LIMIT_ON_NOW = 20
    }

    // Helper function to create a genre row with consistent styling
    private fun genreRow(genreName: String): HomeFragmentRow {
        val query = GetItemsRequest(
            includeItemTypes = setOf(
                BaseItemKind.MOVIE,
                BaseItemKind.SERIES,
                BaseItemKind.MUSIC_ALBUM,
                BaseItemKind.MUSIC_ARTIST,
                BaseItemKind.MUSIC_VIDEO
            ),
            excludeItemTypes = setOf(BaseItemKind.EPISODE),
            genres = listOf(genreName),
            sortBy = listOf(ItemSortBy.RANDOM),
            limit = 50,
            recursive = true,
            fields = ItemRepository.itemFields,
            imageTypeLimit = 1,
            enableTotalRecordCount = false
        )

        // Create a custom row that will use a card presenter with no info
        return object : HomeFragmentRow {
            override fun addToRowsAdapter(
                context: Context,
                cardPresenter: org.jellyfin.androidtv.ui.presentation.CardPresenter,
                rowsAdapter: org.jellyfin.androidtv.ui.presentation.MutableObjectAdapter<androidx.leanback.widget.Row>
            ) {
                // Create a CardPresenter with no info and height of 195dp to match Recently Added
                val noInfoCardPresenter = org.jellyfin.androidtv.ui.presentation.CardPresenter(false, 195).apply {
                    setHomeScreen(true)
                    setUniformAspect(true)
                }

                // Create and add the row with our custom card presenter
                HomeFragmentBrowseRowDefRow(BrowseRowDef(genreName, query, 50, false, true))
                    .addToRowsAdapter(context, noInfoCardPresenter, rowsAdapter)
            }
        }
    }

    // Helper function to create a row with no info card style
    private fun createNoInfoRow(row: HomeFragmentRow): HomeFragmentRow {
        return object : HomeFragmentRow {
            override fun addToRowsAdapter(
                context: Context,
                cardPresenter: org.jellyfin.androidtv.ui.presentation.CardPresenter,
                rowsAdapter: org.jellyfin.androidtv.ui.presentation.MutableObjectAdapter<androidx.leanback.widget.Row>
            ) {
                // Create a standard CardPresenter with no info and height of 195dp to match Recently Added
                val noInfoCardPresenter = org.jellyfin.androidtv.ui.presentation.CardPresenter(false, 195).apply {
                    setHomeScreen(true)
                    setUniformAspect(true)
                }

                row.addToRowsAdapter(context, noInfoCardPresenter, rowsAdapter)
            }
        }
    }
}
