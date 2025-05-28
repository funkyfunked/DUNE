package org.jellyfin.androidtv.ui.shared.toolbar

import android.widget.FrameLayout
import android.widget.ImageView
import androidx.compose.animation.core.animateDpAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsFocusedAsState
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.focus.onFocusChanged
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.compose.ui.viewinterop.AndroidView
import org.jellyfin.androidtv.R
import org.jellyfin.androidtv.auth.repository.UserRepository
import org.jellyfin.androidtv.preference.UserSettingPreferences
import org.jellyfin.androidtv.ui.AsyncImageView
import org.jellyfin.androidtv.ui.base.Icon
import org.jellyfin.androidtv.ui.base.Text
import org.jellyfin.androidtv.ui.base.button.IconButton
import org.jellyfin.androidtv.ui.base.button.IconButtonDefaults
import org.jellyfin.sdk.api.client.ApiClient
import org.jellyfin.sdk.api.client.extensions.imageApi
import org.koin.compose.koinInject

@Composable
fun HomeToolbar(
    openSearch: () -> Unit,
    openLiveTv: () -> Unit,
    openSettings: () -> Unit,
    switchUsers: () -> Unit,
    openLibrary: () -> Unit = {},
    userSettingPreferences: UserSettingPreferences = koinInject(),
    userRepository: UserRepository = koinInject()
) {
    // Get the Live TV button preference
    val showLiveTvButton = userSettingPreferences.get(userSettingPreferences.showLiveTvButton)
    // Track which button is currently focused
    var focusedButton by remember { mutableStateOf<String?>(null) }
    // Collapse when focus is lost from the entire toolbar
    val interactionSource = remember { MutableInteractionSource() }
    val isToolbarFocused by interactionSource.collectIsFocusedAsState()

    LaunchedEffect(isToolbarFocused) {
        if (!isToolbarFocused) {
            focusedButton = null
        }
    }

    Toolbar(
        modifier = Modifier
            .onFocusChanged {
                if (!it.isFocused) {
                    focusedButton = null
                }
            }
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.Start,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                // User Profile Button
                val currentUser by userRepository.currentUser.collectAsState()
                val context = LocalContext.current

                // Get user image URL if available
                val userImageUrl = currentUser?.let { user ->
                    user.primaryImageTag?.let { tag ->
                        koinInject<ApiClient>().imageApi.getUserImageUrl(
                            userId = user.id,
                            tag = tag,
                            maxHeight = 100 // Small size for the toolbar
                        )
                    }
                }


                // User Profile Button
                AnimatedToolbarButton(
                    iconRes = R.drawable.ic_user,
                    label = "",
                    isFocused = focusedButton == "user",
                    onClick = switchUsers,
                    onFocusChanged = { hasFocus ->
                        focusedButton = if (hasFocus) "user" else null
                    },
                    modifier = Modifier.size(48.dp)
                ) {
                    // Custom content for user profile
                    Box(
                        modifier = Modifier
                            .size(36.dp)
                            .clip(CircleShape)
                            .background(Color(0x44FFFFFF)),
                        contentAlignment = Alignment.Center
                    ) {
                        if (userImageUrl != null) {
                            AndroidView(
                                factory = { ctx ->
                                    FrameLayout(ctx).apply {
                                        layoutParams = FrameLayout.LayoutParams(
                                            FrameLayout.LayoutParams.MATCH_PARENT,
                                            FrameLayout.LayoutParams.MATCH_PARENT
                                        )
                                        AsyncImageView(ctx).apply {
                                            layoutParams = FrameLayout.LayoutParams(
                                                FrameLayout.LayoutParams.MATCH_PARENT,
                                                FrameLayout.LayoutParams.MATCH_PARENT
                                            )
                                            scaleType = ImageView.ScaleType.CENTER_CROP
                                            circleCrop = true
                                            load(url = userImageUrl)
                                        }.also { addView(it) }
                                    }
                                },
                                modifier = Modifier.fillMaxSize()
                            )
                        } else {
                            Icon(
                                painter = painterResource(R.drawable.ic_user),
                                contentDescription = stringResource(R.string.lbl_switch_user),
                                modifier = Modifier.size(24.dp),
                                tint = Color.White
                            )
                        }
                    }
                }

                // Search Button
                AnimatedToolbarButton(
                    iconRes = R.drawable.ic_search,
                    label = stringResource(R.string.lbl_search),
                    isFocused = focusedButton == "search",
                    onClick = openSearch,
                    onFocusChanged = { hasFocus ->
                        focusedButton = if (hasFocus) "search" else null
                    }
                )

                // Library Button
                AnimatedToolbarButton(
                    iconRes = R.drawable.ic_loop,
                    label = stringResource(R.string.lbl_library),
                    isFocused = focusedButton == "library",
                    onClick = openLibrary,
                    onFocusChanged = { hasFocus ->
                        focusedButton = if (hasFocus) "library" else null
                    }
                )

                // Live TV Button - Only show if enabled in preferences
                if (showLiveTvButton) {
                    AnimatedToolbarButton(
                        iconRes = R.drawable.ic_tv,
                        label = stringResource(R.string.lbl_live_tv),
                        isFocused = focusedButton == "liveTv",
                        onClick = openLiveTv,
                        onFocusChanged = { hasFocus ->
                            focusedButton = if (hasFocus) "liveTv" else null
                        }
                    )
                }

                // Settings Button
                AnimatedToolbarButton(
                    iconRes = R.drawable.ic_settings,
                    label = stringResource(R.string.lbl_settings),
                    isFocused = focusedButton == "settings",
                    onClick = openSettings,
                    onFocusChanged = { hasFocus ->
                        focusedButton = if (hasFocus) "settings" else null
                    }
                )
            }
        }
    }
}

@Composable
private fun AnimatedToolbarButton(
    iconRes: Int,
    label: String,
    isFocused: Boolean,
    onClick: () -> Unit,
    onFocusChanged: (Boolean) -> Unit,
    modifier: Modifier = Modifier,
    content: @Composable (() -> Unit)? = null
) {
    val width by animateDpAsState(
        targetValue = if (isFocused) 150.dp else 48.dp,
        animationSpec = tween(durationMillis = 200)
    )

    val interactionSource = remember { MutableInteractionSource() }
    val isFocusedState by interactionSource.collectIsFocusedAsState()

    LaunchedEffect(isFocusedState) {
        if (isFocusedState) {
            onFocusChanged(true)
        } else if (isFocused) {
            onFocusChanged(false)
        }
    }

    val borderWidth = if (isFocused) 2.dp else 0.dp

    Box(
        modifier = modifier
            .width(width + borderWidth * 2)
            .height(48.dp + borderWidth * 2)
            .padding(borderWidth)
            .onFocusChanged {
                if (it.isFocused) onFocusChanged(true)
            }
    ) {
        if (isFocused) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .border(
                        width = 2.dp,
                        color = Color.White,
                        shape = RoundedCornerShape(4.dp)
                    )
            )
        }

IconButton(
            onClick = onClick,
            modifier = Modifier
                .fillMaxSize()
                .background(Color.Transparent),
            colors = IconButtonDefaults.colors(
                containerColor = Color.Transparent,
                contentColor = Color.White,
                focusedContainerColor = Color.Transparent,
                focusedContentColor = Color.White,
                disabledContainerColor = Color.Transparent,
                disabledContentColor = Color.White
            )
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.Start,
                modifier = Modifier
                    .padding(horizontal = 4.dp)
                    .fillMaxSize()
            ) {
                // Custom content or default icon
                if (content != null) {
                    content()
                } else {
                    Icon(
                        painter = painterResource(iconRes),
                        contentDescription = label,
                        modifier = Modifier.size(28.dp)
                    )
                }

                // Animated text
                if (isFocused) {
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        text = label,
                        color = Color.White,
                        maxLines = 1
                    )
                }
            }
        }
    }
}
