package org.phenoapps.intercross.ui.listItems

import androidx.annotation.DrawableRes
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.SwipeToDismissBox
import androidx.compose.material3.SwipeToDismissBoxValue
import androidx.compose.material3.Text
import androidx.compose.material3.rememberSwipeToDismissBoxState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.LocalWindowInfo
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import org.phenoapps.intercross.R
import org.phenoapps.intercross.data.models.Event
import org.phenoapps.intercross.ui.qr.QRCodeImage
import org.phenoapps.intercross.ui.theme.AppTheme
import org.phenoapps.intercross.util.DateUtil

@OptIn(ExperimentalFoundationApi::class)
@Composable
fun EventListItem(
    event: Event,
    onEventClick: (Long) -> Unit,
    modifier: Modifier = Modifier,
    selected: Boolean = false,
    selectionMode: Boolean = false,
    swipesEnabled: Boolean = true,
    enableSwipeStartToEnd: Boolean = false,
    enableSwipeEndToStart: Boolean = true,
    @DrawableRes startToEndIconRes: Int = R.drawable.ic_archive,
    @DrawableRes endToStartIconRes: Int = R.drawable.ic_delete,
    startToEndColor: Color? = null,
    endToStartColor: Color? = null,
    startToEndContentDescription: String? = null,
    endToStartContentDescription: String? = null,
    onSwipeStartToEnd: (Event) -> Unit = {},
    onSwipeEndToStart: (Event) -> Unit = {},
    onEventLongPress: (Event) -> Unit = {},
    onSelectionToggle: (Event) -> Unit = {},
) {
    val swipeToDismissBoxState = rememberSwipeToDismissBoxState(
        positionalThreshold = { totalDistance -> totalDistance * 0.92f }
    )

    val screenContainerSize = LocalWindowInfo.current.containerSize
    val density = LocalDensity.current
    val screenHeight = with(density) { screenContainerSize.height.toDp() }
    val qrCodeHeight = screenHeight * 0.10f
    val resolvedStartToEndColor = startToEndColor ?: AppTheme.colors.primary
    val resolvedEndToStartColor = endToStartColor ?: AppTheme.colors.status.error
    val resolvedStartToEndContentDescription =
        startToEndContentDescription ?: stringResource(R.string.archive_event)
    val resolvedEndToStartContentDescription =
        endToStartContentDescription ?: stringResource(R.string.delete_event)

    LaunchedEffect(swipeToDismissBoxState.currentValue) {
        when (swipeToDismissBoxState.currentValue) {
            SwipeToDismissBoxValue.StartToEnd -> {
                onSwipeStartToEnd(event)
                swipeToDismissBoxState.reset()
            }
            SwipeToDismissBoxValue.EndToStart -> {
                onSwipeEndToStart(event)
                swipeToDismissBoxState.reset()
            }
            SwipeToDismissBoxValue.Settled -> Unit
        }
    }

    SwipeToDismissBox(
        state = swipeToDismissBoxState,
        modifier = modifier,
        enableDismissFromStartToEnd = swipesEnabled && enableSwipeStartToEnd,
        enableDismissFromEndToStart = swipesEnabled && enableSwipeEndToStart,
        backgroundContent = {
            when (swipeToDismissBoxState.dismissDirection) {
                SwipeToDismissBoxValue.StartToEnd -> {
                    val progress = swipeToDismissBoxState.progress

                    SwipeBackground(
                        progress = progress,
                        alignment = Alignment.CenterStart,
                        color = resolvedStartToEndColor,
                        iconRes = startToEndIconRes,
                        contentDescription = resolvedStartToEndContentDescription
                    )
                }
                SwipeToDismissBoxValue.EndToStart -> {
                    val progress = swipeToDismissBoxState.progress

                    SwipeBackground(
                        progress = progress,
                        alignment = Alignment.CenterEnd,
                        color = resolvedEndToStartColor,
                        iconRes = endToStartIconRes,
                        contentDescription = resolvedEndToStartContentDescription
                    )
                }
                else -> {
                    // No background for other directions
                }
            }
        }
    ) {
        Card(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 8.dp, vertical = 4.dp)
                .combinedClickable(
                    onClick = {
                        if (selectionMode) {
                            onSelectionToggle(event)
                        } else {
                            event.id?.let { onEventClick(it) }
                        }
                    },
                    onLongClick = {
                        onEventLongPress(event)
                    }
                ),
            colors = CardDefaults.cardColors(
                containerColor = if (selected) AppTheme.colors.accentTransparent else AppTheme.colors.background
            ),
            elevation = CardDefaults.cardElevation(defaultElevation = 4.dp),
            shape = RoundedCornerShape(12.dp),
            border = if (selected) BorderStroke(2.dp, AppTheme.colors.primary) else null
        ) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(16.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                // QR Code/Barcode Image
                QRCodeImage(text = event.eventDbId, modifier = Modifier.height(qrCodeHeight))

                Spacer(modifier = Modifier.width(16.dp))

                // Text Information
                Column(
                    modifier = Modifier.weight(1f),
                    verticalArrangement = Arrangement.spacedBy(4.dp)
                ) {
                    val formattedTimestamp = if ("_" in event.timestamp) {
                        DateUtil().getEntireTimestamp(event.timestamp)
                    } else {
                        event.timestamp
                    }

                    listOf(
                        event.femaleObsUnitDbId,
                        event.maleObsUnitDbId,
                        formattedTimestamp,
                        event.eventDbId,
                        event.person
                    ).filter { it.isNotBlank() }
                        .forEach { value ->
                            Text(text = value)
                        }
                }
            }
        }
    }
}

@Composable
private fun SwipeBackground(
    progress: Float,
    alignment: Alignment,
    color: Color,
    @DrawableRes iconRes: Int,
    contentDescription: String,
) {
    val iconProgress = ((progress - 0.35f) / 0.65f).coerceIn(0f, 1f)

    Box(
        modifier = Modifier
            .fillMaxSize()
            .padding(vertical = 4.dp),
        contentAlignment = alignment
    ) {
        Card(
            modifier = Modifier
                .fillMaxWidth()
                .fillMaxHeight()
                .padding(start = 8.dp, end = 8.dp)
                .align(alignment),
            colors = CardDefaults.cardColors(
                containerColor = color
            ),
            elevation = CardDefaults.cardElevation(defaultElevation = 4.dp),
            shape = RoundedCornerShape(12.dp)
        ) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(16.dp),
                contentAlignment = alignment
            ) {
                Icon(
                    painter = painterResource(iconRes),
                    contentDescription = contentDescription,
                    tint = Color.White,
                    modifier = Modifier
                        .size(24.dp)
                        .graphicsLayer {
                            alpha = iconProgress
                            scaleX = 0.85f + (iconProgress * 0.15f)
                            scaleY = 0.85f + (iconProgress * 0.15f)
                        }
                )
            }
        }
    }
}

@Preview(showBackground = true)
@Composable
private fun EventListItemPreview() {
    EventListItem(
        event = Event(
            eventDbId = "eventDbId",
            maleObsUnitDbId = "maleParent",
            femaleObsUnitDbId = "femaleParent",
            timestamp = "2026-01-13_14_40_49_234",
            person = "Person"
        ),
        onEventClick = { },
        modifier = Modifier,
    )
}
