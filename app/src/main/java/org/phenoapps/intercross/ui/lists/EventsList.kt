package org.phenoapps.intercross.ui.lists

import androidx.annotation.DrawableRes
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyListState
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.compose.material3.Text
import org.phenoapps.intercross.R
import org.phenoapps.intercross.data.models.Event
import org.phenoapps.intercross.ui.theme.AppTheme
import org.phenoapps.intercross.ui.listItems.EventListItem

@Composable
fun EventsList(
    events: List<Event>,
    onEventClick: (Long) -> Unit,
    modifier: Modifier = Modifier,
    listState: LazyListState = rememberLazyListState(),
    selectedEventIds: Set<Long> = emptySet(),
    selectionMode: Boolean = selectedEventIds.isNotEmpty(),
    swipesEnabled: Boolean = true,
    enableSwipeStartToEnd: Boolean = false,
    enableSwipeEndToStart: Boolean = true,
    @DrawableRes startToEndIconRes: Int = R.drawable.ic_archive,
    @DrawableRes endToStartIconRes: Int = R.drawable.ic_delete,
    startToEndColor: Color? = null,
    endToStartColor: Color? = null,
    startToEndContentDescription: String? = null,
    endToStartContentDescription: String? = null,
    emptyText: String? = null,
    onSwipeStartToEnd: (Event) -> Unit = {},
    onSwipeEndToStart: (Event) -> Unit = {},
    onEventLongPress: (Event) -> Unit = {},
    onSelectionToggle: (Event) -> Unit = {},
) {
    val resolvedStartToEndColor = startToEndColor ?: AppTheme.colors.primary
    val resolvedEndToStartColor = endToStartColor ?: AppTheme.colors.status.error
    val resolvedStartToEndContentDescription =
        startToEndContentDescription ?: stringResource(R.string.archive_event)
    val resolvedEndToStartContentDescription =
        endToStartContentDescription ?: stringResource(R.string.delete_event)
    val resolvedEmptyText = emptyText ?: stringResource(R.string.summary_empty)

    if (events.isEmpty()) {
        Box(
            modifier = modifier.fillMaxSize(),
            contentAlignment = Alignment.Center
        ) {
            Text(text = resolvedEmptyText)
        }
        return
    }

    LazyColumn(
        modifier = modifier.fillMaxSize(),
        state = listState,
        contentPadding = PaddingValues(vertical = 8.dp)
    ) {
        items(
            items = events,
            key = { it.id ?: it.eventDbId }
        ) { event ->
            EventListItem(
                event = event,
                onEventClick = onEventClick,
                selected = event.id?.let { it in selectedEventIds } == true,
                selectionMode = selectionMode,
                swipesEnabled = swipesEnabled,
                enableSwipeStartToEnd = enableSwipeStartToEnd,
                enableSwipeEndToStart = enableSwipeEndToStart,
                startToEndIconRes = startToEndIconRes,
                endToStartIconRes = endToStartIconRes,
                startToEndColor = resolvedStartToEndColor,
                endToStartColor = resolvedEndToStartColor,
                startToEndContentDescription = resolvedStartToEndContentDescription,
                endToStartContentDescription = resolvedEndToStartContentDescription,
                onSwipeStartToEnd = onSwipeStartToEnd,
                onSwipeEndToStart = onSwipeEndToStart,
                onEventLongPress = onEventLongPress,
                onSelectionToggle = onSelectionToggle
            )
        }
    }
}
