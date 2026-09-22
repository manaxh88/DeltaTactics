package com.delta.tactics.core.ui.theme

import android.app.Activity
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.SideEffect
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.platform.LocalView
import androidx.core.view.WindowCompat

private val LightColorScheme = lightColorScheme(
    primary = PillNavBackground,
    onPrimary = CardWhite,
    primaryContainer = TacticalOrangeSoft,
    onPrimaryContainer = TacticalOrange,
    secondary = TacticalOrange,
    onSecondary = CardWhite,
    background = AppBackgroundLight,
    onBackground = TextPrimaryDark,
    surface = CardWhite,
    onSurface = TextPrimaryDark,
    surfaceVariant = BadgeGrayBg,
    onSurfaceVariant = TextSecondaryGray,
    outline = CardBorderLight,
    error = AccentRed
)

@Composable
fun DeltaTacticsTheme(
    content: @Composable () -> Unit
) {
    val colorScheme = LightColorScheme
    val view = LocalView.current
    if (!view.isInEditMode) {
        SideEffect {
            val window = (view.context as Activity).window
            window.statusBarColor = android.graphics.Color.TRANSPARENT
            window.navigationBarColor = android.graphics.Color.TRANSPARENT
            val insetsController = WindowCompat.getInsetsController(window, view)
            insetsController.isAppearanceLightStatusBars = true
            insetsController.isAppearanceLightNavigationBars = true
        }
    }

    MaterialTheme(
        colorScheme = colorScheme,
        typography = Typography,
        content = content
    )
}
