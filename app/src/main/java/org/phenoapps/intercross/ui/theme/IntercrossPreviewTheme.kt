package org.phenoapps.intercross.ui.theme

import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.remember
import org.phenoapps.intercross.ui.theme.colors.DefaultAppColors
import org.phenoapps.intercross.ui.theme.typography.CompactTypography

/**
 * Lightweight theme wrapper for @Preview functions.
 *
 * Provides the same CompositionLocals and Material3 theming as [AppTheme]
 * but without Hilt, ViewModel, or Activity dependencies, making it safe
 * to use in Android Studio's preview panel.
 */
@Composable
fun IntercrossPreviewTheme(content: @Composable () -> Unit) {
    val colors = DefaultAppColors
    val typography = CompactTypography.medium
    val materialColorScheme = remember { colors.toMaterialColorScheme() }
    val materialTypography = remember { typography.toMaterialTypography() }

    CompositionLocalProvider(
        LocalAppColors provides colors,
        LocalAppTypography provides typography,
    ) {
        MaterialTheme(
            colorScheme = materialColorScheme,
            typography = materialTypography,
            content = content,
        )
    }
}
