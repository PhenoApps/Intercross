package org.phenoapps.intercross.ui.theme.colors

import androidx.compose.runtime.Immutable
import androidx.compose.ui.graphics.Color

@Immutable
data class AppColors(
    val primary: Color,
    val primaryDark: Color,
    val primaryTransparent: Color,
    val accent: Color,
    val accentTransparent: Color,
    val background: Color,
    val lightGray: Color,

    val disabled: Color,

    val surface: SurfaceColors,

    val text: TextColors,

    val status: StatusColors,

    val wishlistProgress: WishlistProgressColors,

    val chip: ChipColors,
)

@Immutable
data class SurfaceColors(
    val border: Color,
    val iconTint: Color,
    val iconFillTint: Color,
)


@Immutable
data class TextColors(
    val primary: Color,
    val secondary: Color,
    val tertiary: Color,
    val hint: Color,
    val title: Color,
    val subheading: Color,
    val button: Color,
)

@Immutable
data class StatusColors(
    val error: Color,
)

@Immutable
data class WishlistProgressColors(
    val blank: Color,
    val start: Color,
    val mid: Color,
    val end: Color,
    val max: Color,
    val min: Color,
    val moreThanTwoThird: Color,
    val lessThanTwoThird: Color,
    val lessThanOneThird: Color,
)

@Immutable
data class ChipColors(
    val defaultBackground: Color,
    val selectableBackground: Color,
    val selectableStroke: Color,
)