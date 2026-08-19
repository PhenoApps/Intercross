package org.phenoapps.intercross.ui.theme.colors

import androidx.compose.ui.graphics.Color

val SkyAppColors = AppColors(
    primary = Color(0xFF03A9F4),          // Sky Blue
    primaryDark = Color(0xFF0288D1),
    primaryTransparent = Color(0x4203A9F4),
    accent = Color(0xFFE91E63),           // Pink accent
    accentTransparent = Color(0x42E91E63),
    disabled = Color(0xFFE1F5FE),
    background = Color(0xFFE0F7FA),       // Light Cyan background
    lightGray = Color(0xFFB3E5FC),

    surface = SurfaceColors(
        border = Color(0xFF01579B),
        iconTint = Color(0xFF01579B),
        iconFillTint = Color(0xFF01579B),
        topBarContentColor = Color.White,
    ),

    text = TextColors(
        primary = Color(0xFF01579B),
        secondary = Color(0xFF0277BD),
        tertiary = Color(0xFF0288D1),
        hint = Color(0xFF0277BD),
        title = Color.White,
        subheading = Color(0xFF0277BD),
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
        max = Color(0xFF01579B),
        min = Color(0xFF4FC3F7),
        moreThanTwoThird = Color(0xFFFFEB3B),
        lessThanTwoThird = Color(0xFFFF9800),
        lessThanOneThird = Color(0xFFF44336),
    ),

    chip = ChipColors(
        defaultBackground = Color(0x4203A9F4),
        selectableBackground = Color.White,
        selectableStroke = Color(0xFF03A9F4),
    ),
)
