package com.jboy.emulator.ui.theme

import android.app.Activity
import android.os.Build
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.ColorScheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.dynamicDarkColorScheme
import androidx.compose.material3.dynamicLightColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.SideEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.luminance
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalView
import androidx.core.view.WindowCompat
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.stringPreferencesKey
import com.jboy.emulator.data.settingsDataStore
import kotlinx.coroutines.flow.map

private val PREF_THEME_CUSTOM_ENABLED = booleanPreferencesKey("theme_custom_enabled")
private val PREF_THEME_PRIMARY_HEX = stringPreferencesKey("theme_primary_hex")
private val PREF_THEME_SECONDARY_HEX = stringPreferencesKey("theme_secondary_hex")
private val PREF_THEME_TERTIARY_HEX = stringPreferencesKey("theme_tertiary_hex")
private val PREF_THEME_BACKGROUND_HEX = stringPreferencesKey("theme_background_hex")
private val PREF_THEME_SURFACE_HEX = stringPreferencesKey("theme_surface_hex")

private data class ThemePrefs(
    val customEnabled: Boolean = false,
    val primaryHex: String = "#00696B",
    val secondaryHex: String = "#4A6364",
    val tertiaryHex: String = "#4B607B",
    val backgroundHex: String = "#FAFDFC",
    val surfaceHex: String = "#FAFDFC"
)

private val LightColorScheme = lightColorScheme(
    primary = primaryLight,
    onPrimary = onPrimaryLight,
    primaryContainer = primaryContainerLight,
    onPrimaryContainer = onPrimaryContainerLight,
    secondary = secondaryLight,
    onSecondary = onSecondaryLight,
    secondaryContainer = secondaryContainerLight,
    onSecondaryContainer = onSecondaryContainerLight,
    tertiary = tertiaryLight,
    onTertiary = onTertiaryLight,
    tertiaryContainer = tertiaryContainerLight,
    onTertiaryContainer = onTertiaryContainerLight,
    error = errorLight,
    onError = onErrorLight,
    errorContainer = errorContainerLight,
    onErrorContainer = onErrorContainerLight,
    background = backgroundLight,
    onBackground = onBackgroundLight,
    surface = surfaceLight,
    onSurface = onSurfaceLight,
    surfaceVariant = surfaceVariantLight,
    onSurfaceVariant = onSurfaceVariantLight,
    outline = outlineLight,
    outlineVariant = outlineVariantLight,
    scrim = scrimLight,
    inverseSurface = inverseSurfaceLight,
    inverseOnSurface = inverseOnSurfaceLight,
    inversePrimary = inversePrimaryLight,
    surfaceDim = surfaceDimLight,
    surfaceBright = surfaceBrightLight,
    surfaceContainerLowest = surfaceContainerLowestLight,
    surfaceContainerLow = surfaceContainerLowLight,
    surfaceContainer = surfaceContainerLight,
    surfaceContainerHigh = surfaceContainerHighLight,
    surfaceContainerHighest = surfaceContainerHighestLight,
)

private val DarkColorScheme = darkColorScheme(
    primary = primaryDark,
    onPrimary = onPrimaryDark,
    primaryContainer = primaryContainerDark,
    onPrimaryContainer = onPrimaryContainerDark,
    secondary = secondaryDark,
    onSecondary = onSecondaryDark,
    secondaryContainer = secondaryContainerDark,
    onSecondaryContainer = onSecondaryContainerDark,
    tertiary = tertiaryDark,
    onTertiary = onTertiaryDark,
    tertiaryContainer = tertiaryContainerDark,
    onTertiaryContainer = onTertiaryContainerDark,
    error = errorDark,
    onError = onErrorDark,
    errorContainer = errorContainerDark,
    onErrorContainer = onErrorContainerDark,
    background = backgroundDark,
    onBackground = onBackgroundDark,
    surface = surfaceDark,
    onSurface = onSurfaceDark,
    surfaceVariant = surfaceVariantDark,
    onSurfaceVariant = onSurfaceVariantDark,
    outline = outlineDark,
    outlineVariant = outlineVariantDark,
    scrim = scrimDark,
    inverseSurface = inverseSurfaceDark,
    inverseOnSurface = inverseOnSurfaceDark,
    inversePrimary = inversePrimaryDark,
    surfaceDim = surfaceDimDark,
    surfaceBright = surfaceBrightDark,
    surfaceContainerLowest = surfaceContainerLowestDark,
    surfaceContainerLow = surfaceContainerLowDark,
    surfaceContainer = surfaceContainerDark,
    surfaceContainerHigh = surfaceContainerHighDark,
    surfaceContainerHighest = surfaceContainerHighestDark,
)

@Composable
fun JBoyEmulatorTheme(
    darkTheme: Boolean = isSystemInDarkTheme(),
    dynamicColor: Boolean = true,
    content: @Composable () -> Unit
) {
    val context = LocalContext.current
    val themePrefs by context.settingsDataStore.data
        .map { prefs ->
            ThemePrefs(
                customEnabled = prefs[PREF_THEME_CUSTOM_ENABLED] ?: false,
                primaryHex = prefs[PREF_THEME_PRIMARY_HEX] ?: "#00696B",
                secondaryHex = prefs[PREF_THEME_SECONDARY_HEX] ?: "#4A6364",
                tertiaryHex = prefs[PREF_THEME_TERTIARY_HEX] ?: "#4B607B",
                backgroundHex = prefs[PREF_THEME_BACKGROUND_HEX] ?: "#FAFDFC",
                surfaceHex = prefs[PREF_THEME_SURFACE_HEX] ?: "#FAFDFC"
            )
        }
        .collectAsState(initial = ThemePrefs())

    val colorScheme = when {
        themePrefs.customEnabled -> {
            if (darkTheme) buildCustomDarkScheme(themePrefs) else buildCustomLightScheme(themePrefs)
        }
        dynamicColor && Build.VERSION.SDK_INT >= Build.VERSION_CODES.S -> {
            if (darkTheme) dynamicDarkColorScheme(context) else dynamicLightColorScheme(context)
        }
        darkTheme -> DarkColorScheme
        else -> LightColorScheme
    }

    val view = LocalView.current
    if (!view.isInEditMode) {
        SideEffect {
            val window = (view.context as Activity).window
            window.statusBarColor = colorScheme.primary.toArgb()
            WindowCompat.getInsetsController(window, view).isAppearanceLightStatusBars = !darkTheme
        }
    }

    MaterialTheme(
        colorScheme = colorScheme,
        typography = Typography,
        content = content
    )
}

private fun buildCustomLightScheme(prefs: ThemePrefs): ColorScheme {
    val primary = parseHexColorOrDefault(prefs.primaryHex, primaryLight)
    val secondary = parseHexColorOrDefault(prefs.secondaryHex, secondaryLight)
    val tertiary = parseHexColorOrDefault(prefs.tertiaryHex, tertiaryLight)
    val background = parseHexColorOrDefault(prefs.backgroundHex, backgroundLight)
    val surface = parseHexColorOrDefault(prefs.surfaceHex, surfaceLight)

    val primaryContainer = mixColors(primary, Color.White, 0.82f)
    val secondaryContainer = mixColors(secondary, Color.White, 0.78f)
    val tertiaryContainer = mixColors(tertiary, Color.White, 0.78f)

    val surfaceVariant = mixColors(surface, secondary, 0.18f)
    val outline = mixColors(surfaceVariant, Color.Black, 0.35f)

    val onBackground = contentColorFor(background)
    val onSurface = contentColorFor(surface)

    return lightColorScheme(
        primary = primary,
        onPrimary = contentColorFor(primary),
        primaryContainer = primaryContainer,
        onPrimaryContainer = contentColorFor(primaryContainer),
        secondary = secondary,
        onSecondary = contentColorFor(secondary),
        secondaryContainer = secondaryContainer,
        onSecondaryContainer = contentColorFor(secondaryContainer),
        tertiary = tertiary,
        onTertiary = contentColorFor(tertiary),
        tertiaryContainer = tertiaryContainer,
        onTertiaryContainer = contentColorFor(tertiaryContainer),
        error = errorLight,
        onError = onErrorLight,
        errorContainer = errorContainerLight,
        onErrorContainer = onErrorContainerLight,
        background = background,
        onBackground = onBackground,
        surface = surface,
        onSurface = onSurface,
        surfaceVariant = surfaceVariant,
        onSurfaceVariant = contentColorFor(surfaceVariant),
        outline = outline,
        outlineVariant = mixColors(surfaceVariant, Color.White, 0.36f),
        scrim = Color.Black,
        inverseSurface = Color(0xFF202324),
        inverseOnSurface = Color(0xFFF2F3F3),
        inversePrimary = mixColors(primary, Color.White, 0.30f),
        surfaceDim = mixColors(surface, Color.Black, 0.10f),
        surfaceBright = mixColors(surface, Color.White, 0.06f),
        surfaceContainerLowest = mixColors(surface, Color.White, 0.10f),
        surfaceContainerLow = mixColors(surface, Color.White, 0.06f),
        surfaceContainer = mixColors(surface, secondary, 0.06f),
        surfaceContainerHigh = mixColors(surface, secondary, 0.10f),
        surfaceContainerHighest = mixColors(surface, secondary, 0.14f),
    )
}

private fun buildCustomDarkScheme(prefs: ThemePrefs): ColorScheme {
    val primary = mixColors(parseHexColorOrDefault(prefs.primaryHex, primaryDark), Color.White, 0.30f)
    val secondary = mixColors(parseHexColorOrDefault(prefs.secondaryHex, secondaryDark), Color.White, 0.28f)
    val tertiary = mixColors(parseHexColorOrDefault(prefs.tertiaryHex, tertiaryDark), Color.White, 0.26f)
    val backgroundBase = parseHexColorOrDefault(prefs.backgroundHex, backgroundDark)
    val surfaceBase = parseHexColorOrDefault(prefs.surfaceHex, surfaceDark)

    val background = mixColors(backgroundBase, Color.Black, 0.86f)
    val surface = mixColors(surfaceBase, Color.Black, 0.82f)

    val primaryContainer = mixColors(primary, Color.Black, 0.55f)
    val secondaryContainer = mixColors(secondary, Color.Black, 0.56f)
    val tertiaryContainer = mixColors(tertiary, Color.Black, 0.58f)

    val surfaceVariant = mixColors(surface, secondary, 0.22f)
    val outline = mixColors(surfaceVariant, Color.White, 0.45f)

    return darkColorScheme(
        primary = primary,
        onPrimary = contentColorFor(primary),
        primaryContainer = primaryContainer,
        onPrimaryContainer = contentColorFor(primaryContainer),
        secondary = secondary,
        onSecondary = contentColorFor(secondary),
        secondaryContainer = secondaryContainer,
        onSecondaryContainer = contentColorFor(secondaryContainer),
        tertiary = tertiary,
        onTertiary = contentColorFor(tertiary),
        tertiaryContainer = tertiaryContainer,
        onTertiaryContainer = contentColorFor(tertiaryContainer),
        error = errorDark,
        onError = onErrorDark,
        errorContainer = errorContainerDark,
        onErrorContainer = onErrorContainerDark,
        background = background,
        onBackground = contentColorFor(background),
        surface = surface,
        onSurface = contentColorFor(surface),
        surfaceVariant = surfaceVariant,
        onSurfaceVariant = contentColorFor(surfaceVariant),
        outline = outline,
        outlineVariant = mixColors(surfaceVariant, Color.Black, 0.20f),
        scrim = Color.Black,
        inverseSurface = Color(0xFFE3E6E6),
        inverseOnSurface = Color(0xFF1A1D1E),
        inversePrimary = mixColors(primary, Color.Black, 0.38f),
        surfaceDim = mixColors(surface, Color.Black, 0.18f),
        surfaceBright = mixColors(surface, Color.White, 0.12f),
        surfaceContainerLowest = mixColors(surface, Color.Black, 0.20f),
        surfaceContainerLow = mixColors(surface, Color.Black, 0.14f),
        surfaceContainer = mixColors(surface, secondary, 0.08f),
        surfaceContainerHigh = mixColors(surface, secondary, 0.14f),
        surfaceContainerHighest = mixColors(surface, secondary, 0.20f),
    )
}

private fun parseHexColorOrDefault(input: String, fallback: Color): Color {
    val normalized = normalizeHex(input)
    if (!Regex("^#[0-9A-F]{6}$").matches(normalized)) {
        return fallback
    }

    val rgb = normalized.removePrefix("#").toLongOrNull(16) ?: return fallback
    return Color((0xFF000000 or rgb).toInt())
}

private fun normalizeHex(input: String): String {
    val cleaned = input.trim().uppercase().replace("[^#0-9A-F]".toRegex(), "")
    return when {
        cleaned.startsWith("#") -> cleaned
        cleaned.isEmpty() -> ""
        else -> "#$cleaned"
    }
}

private fun mixColors(start: Color, end: Color, ratio: Float): Color {
    val t = ratio.coerceIn(0f, 1f)
    return Color(
        red = start.red + (end.red - start.red) * t,
        green = start.green + (end.green - start.green) * t,
        blue = start.blue + (end.blue - start.blue) * t,
        alpha = start.alpha + (end.alpha - start.alpha) * t
    )
}

private fun contentColorFor(color: Color): Color {
    return if (color.luminance() > 0.45f) Color(0xFF111315) else Color.White
}
