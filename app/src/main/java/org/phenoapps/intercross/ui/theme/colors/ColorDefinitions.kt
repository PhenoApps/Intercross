package org.phenoapps.intercross.ui.theme.colors

/**
 * Define theme colors for default theme, and override specific colors for other theme in their own implementation
 */
val DefaultAppColors = AppColors(
    primary = BaseColors.Primary,
    primaryDark = BaseColors.PrimaryDark,
    primaryTransparent = BaseColors.PrimaryTransparent,
    accent = BaseColors.Accent,
    accentTransparent = BaseColors.AccentTransparent,
    disabled = BaseColors.Disabled,
    background = BaseColors.Background,
    lightGray = BaseColors.LightGrayColor,

    surface = SurfaceColors(
        border = BaseColors.Border,
        iconTint = BaseColors.IconTint,
        iconFillTint = BaseColors.IconFillTint,
    ),

    text = TextColors(
        primary = BaseColors.TextDark,
        secondary = BaseColors.TextSecondary,
        tertiary = BaseColors.TextLight,
        hint = BaseColors.HintText,
        title = BaseColors.TextLight,
        subheading = BaseColors.SubheadingColor,
        button = BaseColors.ButtonText,
    ),

    status = StatusColors(
        error = BaseColors.ErrorMessage,
    ),

    wishlistProgress = WishlistProgressColors(
        blank = BaseColors.NoProgress,
        start = BaseColors.ProgressStart,
        mid = BaseColors.ProgressMid,
        end = BaseColors.ProgressEnd,
        max = BaseColors.ProgressMax,
        min = BaseColors.ProgressMin,
        moreThanTwoThird = BaseColors.ProgressMoreThanTwoThird,
        lessThanTwoThird = BaseColors.ProgressLessThanTwoThird,
        lessThanOneThird = BaseColors.ProgressLessThanOneThird,
    ),

    chip = ChipColors(
        defaultBackground = BaseColors.DefaultChipBackground,
        selectableStroke = BaseColors.SelectableChipStroke,
        selectableBackground = BaseColors.SelectableChipBackground
    )
)