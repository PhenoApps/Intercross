package org.phenoapps.intercross.ui.components

import androidx.annotation.DrawableRes
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.TextStyle
import org.phenoapps.intercross.R

@DrawableRes
fun defaultWishTypeIconRes(wishType: String): Int? {
    val type = wishType.lowercase()
    return when {
        type == "cross" -> R.drawable.ic_cross
        else -> null
    }
}

fun defaultWishTypeEmoji(wishType: String): String? {
    val type = wishType.lowercase()
    return when {
        type.contains("fruit") -> "🍎"
        type.contains("seed") -> "🌱"
        type.contains("flower") -> "🌸"
        else -> null
    }
}

@DrawableRes
fun wishTypeIconRes(wishType: String): Int {
    return defaultWishTypeIconRes(wishType) ?: R.drawable.ic_sprout
}

@Composable
fun WishTypeIcon(
    wishType: String,
    customIcon: String?,
    modifier: Modifier = Modifier,
    textStyle: TextStyle = MaterialTheme.typography.titleMedium,
    tint: Color = Color.Unspecified,
) {
    val defaultIconRes = defaultWishTypeIconRes(wishType)
    val customTextIcon = customIcon?.takeIf { it.isNotBlank() } ?: defaultWishTypeEmoji(wishType)
    when {
        defaultIconRes != null -> Icon(
            painter = painterResource(defaultIconRes),
            contentDescription = null,
            modifier = modifier,
            tint = tint,
        )

        customTextIcon != null -> Text(customTextIcon, modifier = modifier, style = textStyle)

        else -> Icon(
            painter = painterResource(R.drawable.ic_sprout),
            contentDescription = null,
            modifier = modifier,
            tint = tint,
        )
    }
}

// ─── Previews ───────────────────────────────────────────────────────────────────

@androidx.compose.ui.tooling.preview.Preview(showBackground = true, name = "WishTypeIcon - Cross (drawable)")
@Composable
private fun WishTypeIconCrossPreview() {
    org.phenoapps.intercross.ui.theme.IntercrossPreviewTheme {
        WishTypeIcon(wishType = "Cross", customIcon = null)
    }
}

@androidx.compose.ui.tooling.preview.Preview(showBackground = true, name = "WishTypeIcon - Seeds (emoji)")
@Composable
private fun WishTypeIconSeedsPreview() {
    org.phenoapps.intercross.ui.theme.IntercrossPreviewTheme {
        WishTypeIcon(wishType = "Seeds", customIcon = null)
    }
}

@androidx.compose.ui.tooling.preview.Preview(showBackground = true, name = "WishTypeIcon - Custom Emoji")
@Composable
private fun WishTypeIconCustomPreview() {
    org.phenoapps.intercross.ui.theme.IntercrossPreviewTheme {
        WishTypeIcon(wishType = "Custom", customIcon = "🧬")
    }
}
