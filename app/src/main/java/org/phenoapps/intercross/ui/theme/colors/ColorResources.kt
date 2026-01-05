package org.phenoapps.intercross.ui.theme.colors

import androidx.compose.ui.graphics.Color

/**
 * Define colors in BaseColors. Specify overriding colors in other objects
 * You can reuse colors like Primary, PrimaryDark, etc. for other colors eg. TraitPercentStart
 *
 * For other themes, only define colors that are specific to it (colors different than its corresponding value in BaseColors)
 */

val LightGray = Color(0xFFE7E8E8)
val Gray = Color(0xFF9E9E9E)

object BaseColors {
    val Primary = Color(0xFF3F51B5)
    val PrimaryDark = Color(0xFF303F9F)
    val PrimaryTransparent = Color(0x423F51B5)
    val Accent = Color(0xFFFF5722)
    val AccentTransparent = Color(0x42FF5722)
    val Disabled = Color(0xFFFBE9E7)
    val Background = Color.White
    val LightGrayColor = LightGray

    // surface
    val Border = Color.Black
    val IconTint = Color.Black
    val IconFillTint = Color.Black

    // text
    val TextLight = Color.Black
    val TextDark = Color.Black
    val TextSecondary = Color(0xFF595959)
    val HintText = Color.Black
    val SubheadingColor = Color(0xFF595959)
    val ButtonText = Color.Black

    // status
    val ErrorMessage = Color.Red

    // wishlist progress
    val NoProgress = Color(0xAAAAAAAA)
    val ProgressStart = Color(0x90FFFF00)
    val ProgressMid = Color(0x90FF0000)
    val ProgressEnd = Color(0x9000FF00)
    val ProgressMax = Color(0xFF2E7D32)
    val ProgressMin = Color(0xFF8BC34A)
    val ProgressMoreThanTwoThird = Color(0xFFFFEB3B)
    val ProgressLessThanTwoThird = Color(0xFFFF9800)
    val ProgressLessThanOneThird = Color(0xFFF44336)

    // chip
    val DefaultChipBackground = PrimaryTransparent
    val SelectableChipBackground = Color.White
    val SelectableChipStroke = Primary
}