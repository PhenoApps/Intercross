package org.phenoapps.intercross.ui.theme

import androidx.compose.material3.ColorScheme
import androidx.compose.material3.Typography
import androidx.compose.material3.lightColorScheme
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.sp
import org.phenoapps.intercross.ui.theme.colors.AppColors
import org.phenoapps.intercross.ui.theme.typography.ThemeTypography

/**
 * Maps AppColors to Material 3 ColorScheme.
 *
 * Color mappings:
 * - colorPrimary (#3F51B5) → primary
 * - colorAccent (#FF5722) → secondary
 * - colorPrimaryDark (#303F9F) → surfaceVariant
 * - background (White) → background / surface
 * - surfaceContainer variants derived from background via darken()
 */
fun AppColors.toMaterialColorScheme(): ColorScheme {
    return lightColorScheme(
        primary = this.primary,
        onPrimary = Color.White,
        primaryContainer = this.primaryTransparent,
        onPrimaryContainer = this.text.primary,

        secondary = this.accent,
        onSecondary = Color.White,
        secondaryContainer = this.accent.copy(alpha = 0.12f),
        onSecondaryContainer = this.text.primary,

        onTertiary = this.text.primary,
        onTertiaryContainer = this.text.primary,

        error = this.status.error,
        onError = Color.White,
        errorContainer = this.status.error.copy(alpha = 0.12f),
        onErrorContainer = this.status.error,

        background = this.background,
        onBackground = this.text.primary,

        surface = this.background,
        onSurface = this.text.primary,
        surfaceVariant = this.primaryDark,
        onSurfaceVariant = this.text.secondary,

        // Surface container variants — explicit fallbacks derived from background
        surfaceContainerLowest = this.background,
        surfaceContainerLow = this.background.darken(0.01f),
        surfaceContainer = this.background.darken(0.03f),
        surfaceContainerHigh = this.background.darken(0.05f),
        surfaceContainerHighest = this.background.darken(0.08f),

        surfaceDim = this.background,
        surfaceBright = this.background,

        outline = this.surface.border,
        outlineVariant = this.surface.border.copy(alpha = 0.4f),

        scrim = Color.Black.copy(alpha = 0.32f),

        inverseSurface = this.text.primary,
        inverseOnSurface = this.background,
        inversePrimary = this.primary.copy(alpha = 0.8f),
    )
}

/**
 * Darkens a color by reducing each RGB channel by the given fraction.
 * A fraction of 0.05f darkens by 5%.
 */
private fun Color.darken(fraction: Float): Color {
    return Color(
        red = (this.red * (1f - fraction)).coerceIn(0f, 1f),
        green = (this.green * (1f - fraction)).coerceIn(0f, 1f),
        blue = (this.blue * (1f - fraction)).coerceIn(0f, 1f),
        alpha = this.alpha,
    )
}

/**
 * Maps app text sizes to Material 3 Typography with proper weights, letter spacing, and line heights.
 */
fun ThemeTypography.toMaterialTypography(): Typography {
    return Typography(
        displayLarge = TextStyle(
            fontSize = this.titleSize * 1.5f,
            fontWeight = FontWeight.Normal,
            letterSpacing = (-0.25).sp,
            lineHeight = this.titleSize * 2f,
        ),
        displayMedium = TextStyle(
            fontSize = this.titleSize * 1.25f,
            fontWeight = FontWeight.Normal,
            letterSpacing = 0.sp,
            lineHeight = this.titleSize * 1.7f,
        ),
        displaySmall = TextStyle(
            fontSize = this.titleSize,
            fontWeight = FontWeight.Normal,
            letterSpacing = 0.sp,
            lineHeight = this.titleSize * 1.5f,
        ),

        headlineLarge = TextStyle(
            fontSize = this.titleSize * 1.25f,
            fontWeight = FontWeight.SemiBold,
            letterSpacing = 0.sp,
            lineHeight = this.titleSize * 1.6f,
        ),
        headlineMedium = TextStyle(
            fontSize = this.titleSize,
            fontWeight = FontWeight.SemiBold,
            letterSpacing = 0.sp,
            lineHeight = this.titleSize * 1.4f,
        ),
        headlineSmall = TextStyle(
            fontSize = this.titleSize * 0.9f,
            fontWeight = FontWeight.SemiBold,
            letterSpacing = 0.sp,
            lineHeight = this.titleSize * 1.3f,
        ),

        titleLarge = TextStyle(
            fontSize = this.titleSize,
            fontWeight = FontWeight.Medium,
            letterSpacing = 0.sp,
            lineHeight = this.titleSize * 1.4f,
        ),
        titleMedium = TextStyle(
            fontSize = this.titleSize * 0.85f,
            fontWeight = FontWeight.Medium,
            letterSpacing = 0.15.sp,
            lineHeight = this.titleSize * 1.2f,
        ),
        titleSmall = TextStyle(
            fontSize = this.titleSize * 0.75f,
            fontWeight = FontWeight.Medium,
            letterSpacing = 0.1.sp,
            lineHeight = this.titleSize * 1.1f,
        ),

        bodyLarge = TextStyle(
            fontSize = this.bodySize * 1.1f,
            fontWeight = FontWeight.Normal,
            letterSpacing = 0.5.sp,
            lineHeight = this.bodySize * 1.6f,
        ),
        bodyMedium = TextStyle(
            fontSize = this.bodySize,
            fontWeight = FontWeight.Normal,
            letterSpacing = 0.25.sp,
            lineHeight = this.bodySize * 1.5f,
        ),
        bodySmall = TextStyle(
            fontSize = this.bodySize * 0.85f,
            fontWeight = FontWeight.Normal,
            letterSpacing = 0.4.sp,
            lineHeight = this.bodySize * 1.4f,
        ),

        labelLarge = TextStyle(
            fontSize = this.subheadingSize * 1.15f,
            fontWeight = FontWeight.Medium,
            letterSpacing = 0.1.sp,
            lineHeight = this.subheadingSize * 1.5f,
        ),
        labelMedium = TextStyle(
            fontSize = this.subheadingSize,
            fontWeight = FontWeight.Medium,
            letterSpacing = 0.5.sp,
            lineHeight = this.subheadingSize * 1.4f,
        ),
        labelSmall = TextStyle(
            fontSize = this.subheadingSize * 0.85f,
            fontWeight = FontWeight.Medium,
            letterSpacing = 0.5.sp,
            lineHeight = this.subheadingSize * 1.3f,
        ),
    )
}