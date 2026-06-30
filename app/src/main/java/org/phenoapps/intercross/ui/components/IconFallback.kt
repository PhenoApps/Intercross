package org.phenoapps.intercross.ui.components

import android.util.Log
import androidx.annotation.DrawableRes
import androidx.compose.material3.Icon
import androidx.compose.material3.LocalContentColor
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.painterResource

/**
 * A safe wrapper around [Icon] that handles drawable resource loading failures gracefully.
 *
 * If the given [resId] cannot be loaded, a system alert icon is displayed as a placeholder
 * and the failure is logged at ERROR level.
 */
@Composable
fun SafeIcon(
    @DrawableRes resId: Int,
    contentDescription: String?,
    modifier: Modifier = Modifier,
    tint: Color = LocalContentColor.current,
) {
    val painter = runCatching { painterResource(resId) }.getOrNull()
    if (painter != null) {
        Icon(painter = painter, contentDescription = contentDescription, modifier = modifier, tint = tint)
    } else {
        Log.e("SafeIcon", "Failed to load drawable resource: $resId")
        Icon(
            painter = painterResource(android.R.drawable.ic_dialog_alert),
            contentDescription = contentDescription,
            modifier = modifier,
            tint = tint,
        )
    }
}

// ─── Previews ───────────────────────────────────────────────────────────────────

@androidx.compose.ui.tooling.preview.Preview(showBackground = true, name = "SafeIcon - Valid Resource")
@Composable
private fun SafeIconValidPreview() {
    org.phenoapps.intercross.ui.theme.IntercrossPreviewTheme {
        SafeIcon(
            resId = android.R.drawable.ic_menu_camera,
            contentDescription = "Camera",
        )
    }
}

@androidx.compose.ui.tooling.preview.Preview(showBackground = true, name = "SafeIcon - Fallback")
@Composable
private fun SafeIconFallbackPreview() {
    org.phenoapps.intercross.ui.theme.IntercrossPreviewTheme {
        SafeIcon(
            resId = 0,
            contentDescription = "Invalid",
        )
    }
}
