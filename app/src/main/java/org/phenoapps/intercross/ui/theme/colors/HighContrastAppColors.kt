package org.phenoapps.intercross.ui.theme.colors

import androidx.compose.ui.graphics.Color

val HighContrastAppColors = AppColors(
    primary = Color(0xFF000000),           // Pure black
    primaryDark = Color(0xFF000000),
    primaryTransparent = Color(0x42000000),
    accent = Color(0xFFFF0000),            // Bright red accent
    accentTransparent = Color(0x42FF0000),
    disabled = Color(0xFFE0E0E0),
    background = Color.White,              // Pure white background
    lightGray = Color(0xFFE0E0E0),

    surface = SurfaceColors(
        border = Color.Black,
        iconTint = Color.Black,
        iconFillTint = Color.Black,
        topBarContentColor = Color.White,
    ),

    text = TextColors(
        primary = Color.Black,
        secondary = Color(0xFF212121),
        tertiary = Color.Black,
        hint = Color(0xFF424242),
        title = Color.Black,
        subheading = Color(0xFF212121),
        button = Color.White,
    ),

    status = StatusColors(
        error = Color(0xFFFF0000),
    ),

    wishlistProgress = WishlistProgressColors(
        blank = Color(0xAAAAAAAA),
        start = Color(0xFFFFFF00),
        mid = Color(0xFFFF0000),
        end = Color(0xFF00FF00),
        max = Color(0xFF006400),
        min = Color(0xFF00CC00),
        moreThanTwoThird = Color(0xFFFFFF00),
        lessThanTwoThird = Color(0xFFFF8C00),
        lessThanOneThird = Color(0xFFFF0000),
    ),

    chip = ChipColors(
        defaultBackground = Color(0x42000000),
        selectableBackground = Color.White,
        selectableStroke = Color.Black,
    ),
)
