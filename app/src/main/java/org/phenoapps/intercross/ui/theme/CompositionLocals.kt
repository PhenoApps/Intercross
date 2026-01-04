package org.phenoapps.intercross.ui.theme

import androidx.compose.runtime.staticCompositionLocalOf
import org.phenoapps.intercross.ui.theme.colors.DefaultAppColors
import org.phenoapps.intercross.ui.theme.typography.CompactTypography

val LocalAppColors = staticCompositionLocalOf {
    DefaultAppColors
}

val LocalAppTypography = staticCompositionLocalOf {
    CompactTypography.medium
}