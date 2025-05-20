package org.jellyfin.androidtv.ui.preference.screen

import org.jellyfin.androidtv.R
import org.jellyfin.androidtv.preference.UserPreferences
import org.jellyfin.androidtv.preference.UserSettingPreferences
import org.jellyfin.androidtv.preference.constant.AppTheme
import org.koin.android.ext.android.inject
import org.jellyfin.androidtv.ui.preference.dsl.OptionsFragment
import org.jellyfin.androidtv.ui.preference.dsl.enum
import org.jellyfin.androidtv.ui.preference.dsl.checkbox
import org.jellyfin.androidtv.ui.preference.dsl.list
import org.jellyfin.androidtv.ui.preference.dsl.optionsScreen

class EnhancedTweaksPreferencesScreen : OptionsFragment() {
    private val userPreferences: UserPreferences by inject()
    private val userSettingPreferences: UserSettingPreferences by inject()

    override val screen by optionsScreen {
        setTitle(R.string.enhanced_tweaks)

        category {
            setTitle(R.string.enhanced_tweaks)

            enum<AppTheme> {
                setTitle(R.string.pref_app_theme)
                bind(userPreferences, UserPreferences.appTheme)
            }

            checkbox {
                setTitle(R.string.lbl_show_backdrop)
                setContent(R.string.pref_show_backdrop_description)
                bind(userPreferences, UserPreferences.backdropEnabled)
            }

            checkbox {
                setTitle(R.string.show_white_borders)
                setContent(R.string.show_white_borders_summary)
                bind(userPreferences, UserPreferences.showWhiteBorders)
            }

            list {
                setTitle(R.string.lbl_backdrop_fading)
                entries = mapOf(
                    "0.0" to "0%",
                    "0.1" to "10%",
                    "0.2" to "20%",
                    "0.3" to "30%",
                    "0.4" to "40%",
                    "0.5" to "50%",
                    "0.6" to "60%",
                    "0.7" to "70%",
                    "0.8" to "80%",
                    "0.9" to "90%",
                    "1.0" to "100%"
                )
                bind {
                    get { userPreferences[UserPreferences.backdropFadingIntensity].toString() }
                    set { value -> userPreferences[UserPreferences.backdropFadingIntensity] = value.toFloat() }
                    default { UserPreferences.backdropFadingIntensity.defaultValue.toString() }
                }
            }

            list {
                setTitle(R.string.lbl_backdrop_blur)
                entries = mapOf(
                    "0.0" to "0%",
                    "0.1" to "10%",
                    "0.2" to "20%",
                    "0.3" to "30%",
                    "0.4" to "40%",
                    "0.5" to "50%",
                    "0.6" to "60%",
                    "0.7" to "70%",
                    "0.8" to "80%",
                    "0.9" to "90%",
                    "1.0" to "100%"
                )
                bind {
                    get { userPreferences[UserPreferences.backdropBlurIntensity].toString() }
                    set { value -> userPreferences[UserPreferences.backdropBlurIntensity] = value.toFloat() }
                    default { UserPreferences.backdropBlurIntensity.defaultValue.toString() }
                }
            }

            list {
                setTitle(R.string.lbl_backdrop_dimming)
                entries = mapOf(
                    "0.0" to "0%",
                    "0.1" to "10%",
                    "0.2" to "20%",
                    "0.3" to "30%",
                    "0.4" to "40%",
                    "0.5" to "50%",
                    "0.6" to "60%",
                    "0.7" to "70%",
                    "0.8" to "80%",
                    "0.9" to "90%",
                    "1.0" to "100%"
                )
                bind {
                    get { userPreferences[UserPreferences.backdropDimmingIntensity].toString() }
                    set { value -> userPreferences[UserPreferences.backdropDimmingIntensity] = value.toFloat() }
                    default { UserPreferences.backdropDimmingIntensity.defaultValue.toString() }
                }
            }


            list {
                setTitle(R.string.image_quality)
                entries = mapOf(
                    "low" to getString(R.string.image_quality_low),
                    "normal" to getString(R.string.image_quality_normal),
                    "high" to getString(R.string.image_quality_high)
                )
                bind {
                    get { userPreferences[UserPreferences.imageQuality] }
                    set { value -> userPreferences[UserPreferences.imageQuality] = value }
                    default { UserPreferences.imageQuality.defaultValue }
                }
            }
        }

        category {
            setTitle(R.string.genre_rows)
            checkbox {
                setTitle(R.string.genre_row_favorites)
                bind(userSettingPreferences, userSettingPreferences.showFavoritesRow)
            }
            checkbox {
                setTitle(R.string.show_my_collections_row)
                bind(userSettingPreferences, userSettingPreferences.showMyCollectionsRow)
            }
            checkbox {
                setTitle(R.string.show_suggested_tv_shows_row)
                bind(userSettingPreferences, userSettingPreferences.showSuggestedTvShowsRow)
            }
            checkbox {
                setTitle(R.string.show_suggested_for_you_row)
                bind(userSettingPreferences, userSettingPreferences.showSuggestedMoviesRow)
            }
            checkbox {
                setTitle(R.string.show_sci_fi_row)
                bind(userSettingPreferences, userSettingPreferences.showSciFiRow)
            }
            checkbox {
                setTitle(R.string.show_romance_row)
                bind(userSettingPreferences, userSettingPreferences.showRomanceRow)
            }
            checkbox {
                setTitle(R.string.show_anime_row)
                bind(userSettingPreferences, userSettingPreferences.showAnimeRow)
            }
            checkbox {
                setTitle(R.string.show_action_row)
                bind(userSettingPreferences, userSettingPreferences.showActionRow)
            }
            checkbox {
                setTitle(R.string.genre_row_action_adventure)
                bind(userSettingPreferences, userSettingPreferences.showActionAdventureRow)
            }
            checkbox {
                setTitle(R.string.show_comedy_row)
                bind(userSettingPreferences, userSettingPreferences.showComedyRow)
            }
            checkbox {
                setTitle(R.string.show_documentary_row)
                bind(userSettingPreferences, userSettingPreferences.showDocumentaryRow)
            }
            checkbox {
                setTitle(R.string.show_reality_row)
                bind(userSettingPreferences, userSettingPreferences.showRealityRow)
            }
            checkbox {
                setTitle(R.string.show_family_row)
                bind(userSettingPreferences, userSettingPreferences.showFamilyRow)
            }
            checkbox {
                setTitle(R.string.show_horror_row)
                bind(userSettingPreferences, userSettingPreferences.showHorrorRow)
            }
            checkbox {
                setTitle(R.string.show_fantasy_row)
                bind(userSettingPreferences, userSettingPreferences.showFantasyRow)
            }
            checkbox {
                setTitle(R.string.show_history_row)
                bind(userSettingPreferences, userSettingPreferences.showHistoryRow)
            }
            checkbox {
                setTitle(R.string.show_music_row)
                bind(userSettingPreferences, userSettingPreferences.showMusicRow)
            }
            checkbox {
                setTitle(R.string.show_mystery_row)
                bind(userSettingPreferences, userSettingPreferences.showMysteryRow)
            }
            checkbox {
                setTitle(R.string.show_thriller_row)
                bind(userSettingPreferences, userSettingPreferences.showThrillerRow)
            }
            checkbox {
                setTitle(R.string.show_war_row)
                bind(userSettingPreferences, userSettingPreferences.showWarRow)
            }
        }
    }
}
