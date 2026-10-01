package ch.simibu.sport.core.designsystem

import android.app.Activity
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.SideEffect
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.platform.LocalView
import androidx.core.view.WindowCompat

private val LightColorScheme = lightColorScheme(
    primary = SportPurple,
    onPrimary = Color.White,
    primaryContainer = Color(0xFFF3E8F6),
    onPrimaryContainer = Color(0xFF381541),
    secondary = SportRuby,
    onSecondary = Color.White,
    secondaryContainer = Color(0xFFFFE8EF),
    onSecondaryContainer = Color(0xFF4A0A1F),
    background = LightBackground,
    onBackground = LightOnSurface,
    surface = LightSurface,
    onSurface = LightOnSurface,
    surfaceVariant = LightSurfaceVariant,
    onSurfaceVariant = LightOnSurfaceVariant,
    error = StatusError,
    onError = Color.White
)

private val DarkColorScheme = darkColorScheme(
    primary = SportPurpleSoft,
    onPrimary = Color(0xFF280B34),
    primaryContainer = Color(0xFF3D184E),
    onPrimaryContainer = Color(0xFFF3E8F6),
    secondary = SportRubySoft,
    onSecondary = Color(0xFF3B0718),
    secondaryContainer = Color(0xFF4E1626),
    onSecondaryContainer = Color(0xFFFFE8EF),
    background = DarkBackground,
    onBackground = DarkOnSurface,
    surface = DarkSurface,
    onSurface = DarkOnSurface,
    surfaceVariant = DarkSurfaceVariant,
    onSurfaceVariant = DarkOnSurfaceVariant,
    error = StatusError,
    onError = Color.White
)

data class TactileColors(
    val highlight: Color,
    val shadow: Color,
    val isDark: Boolean
)

val LocalTactileColors = staticCompositionLocalOf {
    TactileColors(
        highlight = NeumorphicLightHighlight,
        shadow = NeumorphicLightShadow,
        isDark = false
    )
}

@Composable
fun SportTheme(
    darkTheme: Boolean = isSystemInDarkTheme(),
    content: @Composable () -> Unit
) {
    val colorScheme = if (darkTheme) DarkColorScheme else LightColorScheme
    val tactileColors = if (darkTheme) {
        TactileColors(
            highlight = NeumorphicDarkHighlight,
            shadow = NeumorphicDarkShadow,
            isDark = true
        )
    } else {
        TactileColors(
            highlight = NeumorphicLightHighlight,
            shadow = NeumorphicLightShadow,
            isDark = false
        )
    }

    val view = LocalView.current
    if (!view.isInEditMode) {
        SideEffect {
            val window = (view.context as? Activity)?.window
            if (window != null) {
                window.statusBarColor = colorScheme.background.toArgb()
                window.navigationBarColor = colorScheme.background.toArgb()
                WindowCompat.getInsetsController(window, view).isAppearanceLightStatusBars = !darkTheme
                WindowCompat.getInsetsController(window, view).isAppearanceLightNavigationBars = !darkTheme
            }
        }
    }

    CompositionLocalProvider(
        LocalTactileColors provides tactileColors
    ) {
        MaterialTheme(
            colorScheme = colorScheme,
            typography = Typography,
            content = content
        )
    }
}
