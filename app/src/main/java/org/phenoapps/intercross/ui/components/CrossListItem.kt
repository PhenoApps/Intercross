package org.phenoapps.intercross.ui.components

import android.graphics.Bitmap
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.produceState
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.google.zxing.BarcodeFormat
import com.google.zxing.qrcode.QRCodeWriter
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.phenoapps.intercross.data.models.Event
import org.phenoapps.intercross.ui.preview.PreviewSampleData
import org.phenoapps.intercross.ui.theme.IntercrossPreviewTheme
import androidx.core.graphics.createBitmap
import androidx.core.graphics.set

/**
 * A reusable cross/event list item styled like a printed cross label.
 * Displays a QR code on the left, cross ID prominently, and parent/metadata fields in a structured grid.
 */
@OptIn(ExperimentalFoundationApi::class)
@Composable
fun CrossListItem(
    event: Event,
    onClick: () -> Unit = {},
    onLongClick: () -> Unit = {},
    modifier: Modifier = Modifier,
    showQrCode: Boolean = true,
    selected: Boolean = false,
) {
    Card(
        modifier = modifier
            .fillMaxWidth()
            .combinedClickable(
                onClick = onClick,
                onLongClick = onLongClick,
            ),
        shape = RoundedCornerShape(8.dp),
        colors = CardDefaults.cardColors(
            containerColor = if (selected) MaterialTheme.colorScheme.primary else Color.White,
        ),
        elevation = CardDefaults.cardElevation(defaultElevation = if (selected) 6.dp else 3.dp),
    ) {
        Row(
            modifier = Modifier.padding(12.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            // QR Code
            if (showQrCode) {
                val qrBitmap = rememberQrBitmap(event.eventDbId)
                Box(
                    modifier = Modifier
                        .size(72.dp)
                        .background(Color.White, RoundedCornerShape(4.dp)),
                    contentAlignment = Alignment.Center,
                ) {
                    qrBitmap?.let { bmp ->
                        Image(
                            bitmap = bmp.asImageBitmap(),
                            contentDescription = "QR code for ${event.eventDbId}",
                            modifier = Modifier.size(68.dp),
                        )
                    }
                }
            }

            // Label content
            Column(
                modifier = Modifier.weight(1f),
                verticalArrangement = Arrangement.spacedBy(4.dp),
            ) {
                // Cross ID (prominent, like a label title)
                Text(
                    text = event.eventDbId,
                    style = MaterialTheme.typography.titleMedium.copy(
                        fontWeight = FontWeight.Bold,
                        fontFamily = FontFamily.Monospace,
                        letterSpacing = 0.5.sp,
                    ),
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                )

                HorizontalDivider(color = Color(0xFFE0E0E0), thickness = 0.5.dp)

                // Parent pair row
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(4.dp),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    LabelField(label = "♀", value = event.femaleObsUnitDbId, modifier = Modifier.weight(1f))
                    Text("×", color = Color.Gray, style = MaterialTheme.typography.bodySmall)
                    LabelField(label = "♂", value = event.maleObsUnitDbId, modifier = Modifier.weight(1f))
                }

                // Timestamp and person row
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                ) {
                    val formattedDate = event.timestamp
                        .replace("_", " ")
                        .take(10)
                    Text(
                        text = formattedDate,
                        style = MaterialTheme.typography.labelSmall,
                        color = Color.Gray,
                    )
                    if (event.person.isNotBlank() && event.person != "?") {
                        Text(
                            text = event.person,
                            style = MaterialTheme.typography.labelSmall,
                            color = Color.Gray,
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun LabelField(label: String, value: String, modifier: Modifier = Modifier) {
    Row(
        modifier = modifier,
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(2.dp),
    ) {
        Text(
            text = label,
            style = MaterialTheme.typography.labelSmall,
            fontWeight = FontWeight.Bold,
            color = if (label == "♀") Color(0xFFE91E63) else Color(0xFF2196F3),
        )
        Text(
            text = value,
            style = MaterialTheme.typography.bodySmall,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
        )
    }
}

@Composable
fun rememberQrBitmap(content: String, size: Int = 128): Bitmap? {
    val qrBitmap = produceState<Bitmap?>(initialValue = null, content, size) {
        value = withContext(Dispatchers.Default) {
            createQrBitmap(content, size)
        }
    }
    return qrBitmap.value
}

private fun createQrBitmap(content: String, size: Int): Bitmap? {
    return try {
        val writer = QRCodeWriter()
        val bitMatrix = writer.encode(content, BarcodeFormat.QR_CODE, size, size)
        val bitmap = createBitmap(size, size, Bitmap.Config.RGB_565)
        for (x in 0 until size) {
            for (y in 0 until size) {
                bitmap[x, y] =
                    if (bitMatrix[x, y]) android.graphics.Color.BLACK else android.graphics.Color.WHITE
            }
        }
        bitmap
    } catch (e: Exception) {
        null
    }
}


@Preview(showBackground = true, name = "CrossListItem - Default")
@Preview(showBackground = true, name = "CrossListItem - Large Font", fontScale = 1.5f)
@Composable
private fun CrossListItemPreview() {
    IntercrossPreviewTheme {
        CrossListItem(
            event = PreviewSampleData.events.first(),
        )
    }
}
