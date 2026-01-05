package org.phenoapps.intercross.ui.theme

import androidx.compose.material3.ColorScheme
import androidx.compose.material3.Typography
import androidx.compose.material3.lightColorScheme
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.TextStyle
import org.phenoapps.intercross.ui.theme.colors.AppColors
import org.phenoapps.intercross.ui.theme.typography.ThemeTypography

/**
 * Maps AppColors to Material 3 ColorScheme
 */
fun AppColors.toMaterialColorScheme(): ColorScheme {
    return lightColorScheme(
        primary = this.primary,
        primaryContainer = this.primaryTransparent,
        onPrimaryContainer = this.text.primary,

        secondary = this.accent,
        onSecondary = this.text.primary,
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
        onSurfaceVariant = this.text.secondary,

        outline = this.surface.border,
        outlineVariant = this.surface.border.copy(alpha = 0.4f),

        scrim = Color.Black.copy(alpha = 0.32f),

        inverseSurface = this.text.primary,
        inverseOnSurface = this.background,
        inversePrimary = this.primary.copy(alpha = 0.8f),

        surfaceDim = this.background,
        surfaceBright = this.background,
        surfaceContainerLowest = this.background,
    )
}

/**
 * Maps app text sizes to Material 3 Typography
 */
fun ThemeTypography.toMaterialTypography(): Typography {
    return Typography(
        displayLarge = TextStyle(fontSize = this.titleSize * 1.5f),
        displayMedium = TextStyle(fontSize = this.titleSize * 1.25f),
        displaySmall = TextStyle(fontSize = this.titleSize),

        headlineLarge = TextStyle(fontSize = this.titleSize * 1.25f),
        headlineMedium = TextStyle(fontSize = this.titleSize),
        headlineSmall = TextStyle(fontSize = this.titleSize * 0.9f),

        titleLarge = TextStyle(fontSize = this.titleSize),
        titleMedium = TextStyle(fontSize = this.titleSize * 0.9f),
        titleSmall = TextStyle(fontSize = this.titleSize * 0.8f),

        bodyLarge = TextStyle(fontSize = this.bodySize * 1.1f),
        bodyMedium = TextStyle(fontSize = this.bodySize),
        bodySmall = TextStyle(fontSize = this.bodySize * 0.9f),

        labelLarge = TextStyle(fontSize = this.subheadingSize * 1.2f),
        labelMedium = TextStyle(fontSize = this.subheadingSize),
        labelSmall = TextStyle(fontSize = this.subheadingSize * 0.9f)
    )
}