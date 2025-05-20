package org.jellyfin.androidtv.preference

import android.content.Context
import org.jellyfin.preference.store.SharedPreferenceStore
import org.jellyfin.preference.stringPreference

/**
 * Preferences related to card sizes in the UI.
 * This functionality is now integrated directly into UserPreferences.
 */
@Deprecated("Card size preferences are now part of UserPreferences")
class CardSizePreference(context: Context) : SharedPreferenceStore(
    sharedPreferences = context.getSharedPreferences("card_size", Context.MODE_PRIVATE)
) {
    companion object {
        /**
         * The size of cards on the home screen and library views
         * Possible values: "small", "medium", "large"
         */
        @Deprecated("Use UserPreferences.cardSize instead")
        val cardSize = stringPreference("card_size", "medium")
    }
}
