package org.phenoapps.intercross.ui.theme.colors

import androidx.compose.ui.graphics.Color

val AutumnAppColors = AppColors(
    primary = Color(0xFFD84315),          // Burnt Orange
    primaryDark = Color(0xFFBF360C),
    primaryTransparent = Color(0x42D84315),
    accent = Color(0xFF6D4C41),           // Brown accent
    accentTransparent = Color(0x426D4C41),
    disabled = Color(0xFFEFEBE9),
    background = Color(0xFFFBE9E7),       // Light Orange background
    lightGray = Color(0xFFFFCCBC),

    surface = SurfaceColors(
        border = Color(0xFF3E2723),
        iconTint = Color(0xFF3E2723),
        iconFillTint = Color(0xFF3E2723),
        topBarContentColor = Color.White,
    ),

    text = TextColors(
        primary = Color(0xFF3E2723),
        secondary = Color(0xFF4E342E),
        tertiary = Color(0xFF5D4037),
        hint = Color(0xFF4E342E),
        title = Color.White,
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
        max = Color(0xFF3E2723),
        min = Color(0xFF8D6E63),
        moreThanTwoThird = Color(0xFFFFEB3B),
        lessThanTwoThird = Color(0xFFFF9800),
        lessThanOneThird = Color(0xFFF44336),
    ),

    chip = ChipColors(
        defaultBackground = Color(0x42D84315),
        selectableBackground = Color.White,
        selectableStroke = Color(0xFFD84315),
    ),
)
