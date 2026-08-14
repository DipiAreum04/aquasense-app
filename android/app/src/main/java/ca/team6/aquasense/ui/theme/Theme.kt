package ca.team6.aquasense.ui.theme

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable

private val DarkColorScheme = darkColorScheme(
    primary = AquaDarkPrimary,
    onPrimary = AquaDarkOnPrimary,
    secondary = AquaDarkSecondary,
    onSecondary = AquaDarkOnPrimary,
    tertiary = AquaDarkTertiary,
    background = AquaDarkBackground,
    onBackground = AquaDarkOnSurface,
    surface = AquaDarkSurface,
    onSurface = AquaDarkOnSurface,
    surfaceVariant = AquaDarkSurfaceVariant,
    onSurfaceVariant = AquaDarkOnSurfaceVariant,
    error = AquaDarkError,
    onError = AquaDarkOnPrimary,
    outline = AquaDarkOutline
)

private val LightColorScheme = lightColorScheme(
    primary = AquaLightPrimary,
    onPrimary = AquaLightOnPrimary,
    secondary = AquaLightSecondary,
    onSecondary = AquaLightOnPrimary,
    tertiary = AquaLightTertiary,
    background = AquaLightBackground,
    onBackground = AquaLightOnSurface,
    surface = AquaLightSurface,
    onSurface = AquaLightOnSurface,
    surfaceVariant = AquaLightSurfaceVariant,
    onSurfaceVariant = AquaLightOnSurfaceVariant,
    error = AquaLightError,
    onError = AquaLightOnPrimary,
    outline = AquaLightOutline
)

@Composable
fun AquaSenseTheme(
    darkTheme: Boolean = isSystemInDarkTheme(),
    content: @Composable () -> Unit
) {
    val colorScheme = if (darkTheme) DarkColorScheme else LightColorScheme

    MaterialTheme(
        colorScheme = colorScheme,
        typography = Typography,
        content = content
    )
}
