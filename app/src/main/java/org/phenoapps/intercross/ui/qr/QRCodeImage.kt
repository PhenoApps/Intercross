package org.phenoapps.intercross.ui.qr

import android.graphics.Bitmap
import android.graphics.Color
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.graphics.Color as ComposeColor
import com.google.zxing.BarcodeFormat
import com.google.zxing.qrcode.QRCodeWriter
import androidx.core.graphics.createBitmap
import androidx.core.graphics.set

@Composable
fun QRCodeImage(
    text: String,
    modifier: Modifier = Modifier,
) {
    val qrCodeBitmap = remember(text) {
        generateQRCode(text)
    }

    if (qrCodeBitmap != null) {
        Image(
            bitmap = qrCodeBitmap.asImageBitmap(),
            contentDescription = "QR Code for $text",
            modifier = modifier
        )
    } else {
        // Fallback placeholder
        Box(
            modifier = modifier.background(color = ComposeColor.Gray)
        )
    }
}

private fun generateQRCode(text: String): Bitmap? {
    return runCatching {
        val bitMatrix = QRCodeWriter().encode(text, BarcodeFormat.QR_CODE, 256, 256)

        val w = bitMatrix.width
        val h = bitMatrix.height
        val pixels = IntArray(w * h)

        for (y in 0 until h) {
            val offset = y * w
            for (x in 0 until w) {
                pixels[offset + x] = if (bitMatrix.get(x, y)) Color.BLACK else Color.WHITE
            }
        }

        val bitmap = createBitmap(w, h, Bitmap.Config.RGB_565)
        bitmap.setPixels(pixels, 0, w, 0, 0, w, h)
        bitmap
    }.getOrNull()
}

@Preview
@Composable
private fun QRCodeImagePreview() {
    QRCodeImage(text = "123")
}
