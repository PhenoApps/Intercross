package org.phenoapps.intercross.ui.theme.colors

import androidx.compose.ui.graphics.Color

val GreenAppColors = AppColors(
    primary = Color(0xFF2E7D32),          // Forest Green
    primaryDark = Color(0xFF1B5E20),      // Dark Green
    primaryTransparent = Color(0x422E7D32),
    accent = Color(0xFFFF6F00),           // Amber accent
    accentTransparent = Color(0x42FF6F00),
    disabled = Color(0xFFE8F5E9),
    background = Color(0xFFE8F5E9),       // Light green background
    lightGray = Color(0xFFC8E6C9),

    surface = SurfaceColors(
        border = Color(0xFF1B5E20),
        iconTint = Color(0xFF1B5E20),
        iconFillTint = Color(0xFF1B5E20),
        topBarContentColor = Color.White,
    ),

    text = TextColors(
        primary = Color(0xFF1B5E20),
        secondary = Color(0xFF4E342E),
        tertiary = Color(0xFF33691E),
        hint = Color(0xFF4E342E),
        title = Color(0xFF1B5E20),
        subheading = Color(0xFF4E342E),
        button = Color.White,
    ),

    status = StatusColors(
        error = Color(0xFFD32F2F),
    ),

    wishlistProgress = WishlistProgressColors(
        blank = Color(0xAAAAAAAA),
        start = Color(0x90FFFF00),
        mid = Color(0x90FF0000),
        end = Color(0x9000FF00),
        max = Color(0xFF1B5E20),
        min = Color(0xFF66BB6A),
        moreThanTwoThird = Color(0xFFFFEB3B),
        lessThanTwoThird = Color(0xFFFF9800),
        lessThanOneThird = Color(0xFFF44336),
    ),

    chip = ChipColors(
        defaultBackground = Color(0x422E7D32),
        selectableBackground = Color.White,
        selectableStroke = Color(0xFF2E7D32),
    ),
)
