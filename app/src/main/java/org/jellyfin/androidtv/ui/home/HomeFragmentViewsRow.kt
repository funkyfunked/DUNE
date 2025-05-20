package org.jellyfin.androidtv.ui.home

import android.content.Context
import android.graphics.Color
import android.graphics.Typeface
import android.graphics.drawable.GradientDrawable
import android.view.Gravity
import android.view.View
import android.view.ViewGroup
import android.widget.FrameLayout
import android.widget.TextView
import androidx.core.content.ContextCompat
import androidx.leanback.widget.ArrayObjectAdapter
import androidx.leanback.widget.BaseCardView
import androidx.leanback.widget.HeaderItem
import androidx.leanback.widget.ListRow
import androidx.leanback.widget.ListRowPresenter
import androidx.leanback.widget.Presenter
import androidx.leanback.widget.Row
import androidx.leanback.widget.RowPresenter
import org.jellyfin.androidtv.R
import org.jellyfin.androidtv.data.querying.GetUserViewsRequest
import org.jellyfin.androidtv.data.repository.UserViewsRepository
import org.jellyfin.androidtv.preference.UserPreferences
import org.jellyfin.androidtv.preference.UserSettingPreferences
import org.jellyfin.androidtv.ui.itemhandling.BaseRowItem
import org.jellyfin.androidtv.ui.itemhandling.ItemRowAdapter
import org.jellyfin.androidtv.ui.presentation.CardPresenter
import org.jellyfin.androidtv.ui.presentation.MutableObjectAdapter
import org.jellyfin.androidtv.ui.presentation.UserViewCardPresenter
import org.koin.java.KoinJavaComponent

/**
 * ButtonViewPresenter creates compact button-style views for media folders
 * instead of the standard card with poster image.
 */
class ButtonViewPresenter : Presenter() {
    class ExtraSmallTextView(context: Context) : TextView(context) {
        // Create a drawable once for performance
        private val focusedBackground = GradientDrawable().apply {
            shape = GradientDrawable.RECTANGLE
            cornerRadius = 16f
            setColor(Color.argb(179, 48, 48, 48)) // 70% opacity grey
        }
        
        init {
            // Basic styling
            gravity = Gravity.CENTER
            setPadding(25, 15, 25, 15)
            textSize = 14f
            setTypeface(null, Typeface.BOLD)
            setTextColor(Color.WHITE)
        }
        
        // Handle the focus change directly in the view
        override fun onFocusChanged(gainFocus: Boolean, direction: Int, previouslyFocusedRect: android.graphics.Rect?) {
            super.onFocusChanged(gainFocus, direction, previouslyFocusedRect)
            
            // Set background and elevation based on focus state
            if (gainFocus) {
                background = focusedBackground
                elevation = 8f
            } else {
                background = null
                elevation = 0f
            }
        }
    }
    
    override fun onCreateViewHolder(parent: ViewGroup): ViewHolder {
        val textView = ExtraSmallTextView(parent.context)
        textView.isFocusable = true
        textView.isFocusableInTouchMode = true
        return ViewHolder(textView)
    }
    
    override fun onBindViewHolder(viewHolder: ViewHolder, item: Any?) {
        if (item !is BaseRowItem) return
        
        val textView = viewHolder.view as ExtraSmallTextView
        textView.text = item.getName(textView.context)
    }
    
    override fun onUnbindViewHolder(viewHolder: ViewHolder) {
        // Nothing to clean up
    }
}

class HomeFragmentViewsRow(
	val small: Boolean,
) : HomeFragmentRow {
	private companion object {
		val smallCardPresenter = UserViewCardPresenter(true)
		val largeCardPresenter = UserViewCardPresenter(false)
		val buttonPresenter = ButtonViewPresenter()
	}

	override fun addToRowsAdapter(context: Context, cardPresenter: CardPresenter, rowsAdapter: MutableObjectAdapter<Row>) {
		// Get user preferences to check if extra small option is enabled
		val userPrefs = KoinJavaComponent.get<UserSettingPreferences>(UserSettingPreferences::class.java)
		val useExtraSmall = userPrefs.get(userPrefs.useExtraSmallMediaFolders)

		// Choose the appropriate presenter based on preference
		val presenter = when {
			useExtraSmall -> buttonPresenter  // Use button-style for extra small
			small -> smallCardPresenter      // Use small cards if small is enabled
			else -> largeCardPresenter       // Use large cards by default
		}

		// Create the adapter with the selected presenter
		val rowAdapter = ItemRowAdapter(context, GetUserViewsRequest, presenter, rowsAdapter)

		// Determine the header text based on the presentation style
		val headerText = if (useExtraSmall) {
			context.getString(R.string.lbl_my_media_extra_small)
		} else if (small) {
			context.getString(R.string.lbl_my_media_small)
		} else {
			context.getString(R.string.lbl_my_media)
		}

		val header = HeaderItem(headerText)
		val row = ListRow(header, rowAdapter)
		rowAdapter.setRow(row)
		rowAdapter.Retrieve()
		rowsAdapter.add(row)
	}
}
