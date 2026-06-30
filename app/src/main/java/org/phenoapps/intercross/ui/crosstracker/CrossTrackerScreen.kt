package org.phenoapps.intercross.ui.crosstracker

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.DateRange
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import org.phenoapps.intercross.R
import androidx.compose.material3.IconButton
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import org.phenoapps.intercross.ui.app.BottomBarState
import org.phenoapps.intercross.ui.app.BottomDestinations
import org.phenoapps.intercross.ui.app.TopBarAction
import org.phenoapps.intercross.ui.app.TopBarState
import org.phenoapps.intercross.ui.components.SafeIcon
import org.phenoapps.intercross.ui.components.WishTypeIcon
import org.phenoapps.intercross.ui.preview.PreviewSampleData
import org.phenoapps.intercross.ui.theme.AppTheme
import org.phenoapps.intercross.ui.theme.IntercrossPreviewTheme
import androidx.compose.ui.tooling.preview.Preview
import java.time.LocalDate
import java.time.YearMonth
import java.time.format.TextStyle
import androidx.compose.material3.NavigationBarItemDefaults
import java.util.Locale

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun CrossTrackerScreen(
    rows: List<CrossTrackerRow>,
    filter: CrossFilter,
    onFilterChange: (CrossFilter) -> Unit,
    onOpenCrossBlock: () -> Unit,
    onCreateWishlist: () -> Unit,
    onRowClick: (CrossTrackerRow) -> Unit,
    hideCompleted: Boolean = false,
    onToggleHideCompleted: () -> Unit = {},
    metaIcons: Map<String, String?> = emptyMap(),
    topBarState: TopBarState? = null,
    bottomBarState: BottomBarState? = null,
) {
    Scaffold(
        topBar = {
            if (topBarState != null) {
                TopAppBar(
                    title = {
                        Text(topBarState.title ?: topBarState.titleRes?.let { stringResource(it) }.orEmpty())
                    },
                    navigationIcon = {
                        if (topBarState.showBack) {
                            IconButton(onClick = { topBarState.onBack?.invoke() }) {
                                Icon(painterResource(R.drawable.arrow_back_24px), contentDescription = stringResource(R.string.navigate_back))
                            }
                        }
                    },
                    actions = {
                        topBarState.actions.forEach { action ->
                            if (action.iconRes != null) {
                                IconButton(onClick = { action.onClick?.invoke() }) {
                                    Icon(painterResource(action.iconRes), contentDescription = stringResource(action.labelRes))
                                }
                            } else {
                                TextButton(onClick = { action.onClick?.invoke() }) {
                                    Text(stringResource(action.labelRes))
                                }
                            }
                        }
                    },
                    colors = TopAppBarDefaults.topAppBarColors(
                        containerColor = AppTheme.colors.primary,
                        titleContentColor = AppTheme.colors.surface.topBarContentColor,
                        actionIconContentColor = AppTheme.colors.surface.topBarContentColor,
                        navigationIconContentColor = AppTheme.colors.surface.topBarContentColor,
                    ),
                )
            }
        },
        bottomBar = {
            if (bottomBarState != null) {
                NavigationBar(
                    containerColor = AppTheme.colors.primary,
                ) {
                    BottomDestinations.forEach { destination ->
                        NavigationBarItem(
                            selected = bottomBarState.selectedRoute == destination.route,
                            onClick = { bottomBarState.onTabSelected(destination.route) },
                            icon = { SafeIcon(resId = destination.iconRes, contentDescription = null, tint = Color.White) },
                            label = if (bottomBarState.selectedRoute == destination.route) {
                                { Text(stringResource(destination.labelRes), color = Color.White) }
                            } else null,
                            colors = NavigationBarItemDefaults.colors(
                                indicatorColor = Color.White.copy(alpha = 0.2f),
                            ),
                        )
                    }
                }
            }
        },
        floatingActionButton = {
            FloatingActionButton(
                onClick = onCreateWishlist,
                shape = CircleShape,
                containerColor = AppTheme.colors.accent,
            ) {
                Icon(Icons.Default.Add, contentDescription = "Add a wishlist item", tint = Color.White)
            }
        }
    ) { innerPadding ->
    LazyColumn(
        Modifier.fillMaxSize()
            .padding(innerPadding)
            .padding(horizontal = 8.dp),
        verticalArrangement = Arrangement.spacedBy(8.dp),
        contentPadding = androidx.compose.foundation.layout.PaddingValues(top = 0.dp),
    ) {
        item {
            Row(
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier.padding(horizontal = 8.dp),
            ) {
                CrossFilter.entries.forEach { option ->
                    FilterChip(
                        selected = filter == option,
                        onClick = { onFilterChange(option) },
                        label = { Text(option.name.lowercase()) },
                        shape = CircleShape,
                        colors = FilterChipDefaults.filterChipColors(
                            containerColor = AppTheme.colors.lightGray,
                            selectedContainerColor = AppTheme.colors.chip.selectableBackground,
                        ),
                        border = if (filter == option) {
                            FilterChipDefaults.filterChipBorder(
                                enabled = true,
                                selected = true,
                                borderColor = AppTheme.colors.chip.selectableStroke,
                                selectedBorderColor = AppTheme.colors.chip.selectableStroke,
                            )
                        } else null,
                    )
                }
            }
        }
        if (rows.isEmpty()) {
            item { Text(stringResource(R.string.summary_and_wishlist_empty), modifier = Modifier.padding(24.dp)) }
        }
        items(rows, key = { "${it.femaleId}:${it.maleId}:${it.count}" }) { row ->
            CrossTrackerRowCard(row = row, onClick = { onRowClick(row) }, metaIcons = metaIcons)
        }
    }
    }
}

@Composable
private fun CrossTrackerRowCard(row: CrossTrackerRow, onClick: () -> Unit, metaIcons: Map<String, String?> = emptyMap()) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick),
        shape = RoundedCornerShape(12.dp),
        colors = CardDefaults.cardColors(containerColor = Color.White),
        elevation = CardDefaults.cardElevation(defaultElevation = 4.dp),
    ) {
        Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
            // Header: count circle on left, female/male names on right
            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                // Circular count badge (matches circle_background drawable)
                Surface(
                    shape = CircleShape,
                    color = AppTheme.colors.lightGray,
                    modifier = Modifier.size(40.dp),
                ) {
                    Box(contentAlignment = Alignment.Center, modifier = Modifier.fillMaxSize()) {
                        Text(
                            text = row.count.toString(),
                            style = MaterialTheme.typography.titleSmall,
                            color = Color(0xFF666666),
                        )
                    }
                }
                Column(modifier = Modifier.weight(1f)) {
                    Text(row.female, style = MaterialTheme.typography.bodyMedium)
                    Text(row.male, style = MaterialTheme.typography.bodyMedium)
                }
            }
            // Person and date chips with icons
            if (row.persons.isNotEmpty() || row.dates.isNotEmpty()) {
                var showDateCalendar by remember { mutableStateOf(false) }
                FlowRow(
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    verticalArrangement = Arrangement.spacedBy(4.dp),
                ) {
                    row.persons.forEach { person ->
                        Surface(
                            shape = CircleShape,
                            color = AppTheme.colors.lightGray,
                            modifier = Modifier.testTag("styled_wish_chip"),
                        ) {
                            Row(
                                modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp),
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(6.dp),
                            ) {
                                Icon(Icons.Default.Person, contentDescription = null, modifier = Modifier.size(16.dp))
                                Text("${person.name}: ${person.count}", style = MaterialTheme.typography.labelMedium)
                            }
                        }
                    }
                    // Show only the latest date chip
                    if (row.dates.isNotEmpty()) {
                        val latestDate = row.dates.maxByOrNull { it.date } ?: row.dates.last()
                        val formattedDate = latestDate.date.substringBefore("_").take(10)
                        Surface(
                            shape = CircleShape,
                            color = AppTheme.colors.lightGray,
                            modifier = Modifier
                                .testTag("styled_wish_chip")
                                .clickable { showDateCalendar = true },
                        ) {
                            Row(
                                modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp),
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(6.dp),
                            ) {
                                Icon(Icons.Default.DateRange, contentDescription = null, modifier = Modifier.size(16.dp))
                                Text(formattedDate, style = MaterialTheme.typography.labelMedium)
                                if (row.dates.size > 1) {
                                    Text("+${row.dates.size - 1}", style = MaterialTheme.typography.labelSmall, color = Color.Gray)
                                }
                            }
                        }
                    }
                }
                // Calendar dialog showing all dates highlighted
                if (showDateCalendar) {
                    val allDates = row.dates.mapNotNull { dateCount ->
                        runCatching {
                            LocalDate.parse(dateCount.date.substringBefore("_").take(10))
                        }.getOrNull()
                    }.toSet()
                    CrossDatesCalendarDialog(
                        dates = allDates,
                        onDismiss = { showDateCalendar = false },
                    )
                }
            }
            // Wish type chips with progress
            if (row is CrossTrackerRow.Planned && row.wishes.isNotEmpty()) {
                row.wishes.forEach { wish ->
                    WishProgressBar(wish = wish, icon = metaIcons[wish.wishType])
                }
            }
        }
    }
}

/**
 * Progress bar showing min/max sections with a warning when progress exceeds max.
 *
 * The bar uses Canvas to draw:
 * - A dimmed zone from 0:min showing where the minimum target sits
 * - A solid fill from 0:progress showing actual progress
 * - A thin white marker line at the min boundary
 * - Red coloring + warning icon when progress exceeds max
 */
@Composable
fun WishProgressBar(wish: WishlistProgressItem, icon: String? = null) {
    val isMinMet = wish.progress >= wish.min
    val isOverMax = wish.max > 0 && wish.progress > wish.max

    // Use max as the full-bar reference; fall back to min if max is 0
    val effectiveMax = wish.max.coerceAtLeast(wish.min)
    val progressFraction = (wish.progress.toFloat() / effectiveMax.coerceAtLeast(1)).coerceIn(0f, 1f)
    val minFraction = (wish.min.toFloat() / effectiveMax.coerceAtLeast(1)).coerceIn(0f, 1f)

    val fillColor = when {
        isOverMax -> AppTheme.colors.wishlistProgress.mid   // red for over max
        isMinMet -> AppTheme.colors.wishlistProgress.max    // dark green for min met
        else -> AppTheme.colors.wishlistProgress.min        // light green for in progress
    }
    val minZoneColor = fillColor.copy(alpha = 0.35f)
    val trackColor = AppTheme.colors.wishlistProgress.blank
    val density = LocalDensity.current
    val markerWidthPx = with(density) { 2.dp.toPx() }

    Row(
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(8.dp),
    ) {
        WishTypeIcon(
            wishType = wish.wishType,
            customIcon = icon,
            modifier = Modifier.size(20.dp),
        )

        // Canvas-drawn segmented progress bar
        Canvas(
            modifier = Modifier
                .weight(1f)
                .height(12.dp)
                .testTag("progress_indicator"),
        ) {
            val barWidth = size.width
            val barHeight = size.height
            val cornerRadius = barHeight / 2f

            // Track background
            drawRoundRect(
                color = trackColor,
                cornerRadius = CornerRadius(cornerRadius),
            )

            // Dimmed min zone (shows where the minimum target sits)
            if (minFraction in 0.01f..0.99f) {
                drawRoundRect(
                    color = minZoneColor,
                    cornerRadius = CornerRadius(cornerRadius),
                    size = Size(barWidth * minFraction, barHeight),
                )
            }

            // Solid progress fill
            if (progressFraction > 0f) {
                drawRoundRect(
                    color = fillColor,
                    cornerRadius = CornerRadius(cornerRadius),
                    size = Size(barWidth * progressFraction, barHeight),
                )
            }

            // Min marker: thin white vertical line at the min boundary
            if (minFraction in 0.01f..0.99f) {
                val markerX = barWidth * minFraction
                drawLine(
                    color = Color.White.copy(alpha = 0.9f),
                    start = Offset(markerX, 0f),
                    end = Offset(markerX, barHeight),
                    strokeWidth = markerWidthPx,
                )
            }
        }

        // Progress detail: current / min–max
        Column(
            horizontalAlignment = Alignment.End,
            modifier = Modifier.widthIn(min = 56.dp),
        ) {
            Text(
                text = "${wish.progress}",
                style = MaterialTheme.typography.labelLarge,
                fontWeight = FontWeight.Bold,
                color = when {
                    isOverMax -> AppTheme.colors.wishlistProgress.mid
                    isMinMet -> AppTheme.colors.wishlistProgress.max
                    else -> MaterialTheme.colorScheme.onSurface
                },
            )
            Text(
                text = if (wish.max > 0) "${wish.min}–${wish.max}" else "min ${wish.min}",
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }

        if (isOverMax) {
            Icon(
                imageVector = Icons.Default.Warning,
                contentDescription = "Over maximum",
                modifier = Modifier.size(20.dp),
                tint = AppTheme.colors.wishlistProgress.mid,
            )
        } else if (isMinMet) {
            Icon(
                painter = painterResource(R.drawable.ic_wishes_complete),
                contentDescription = "Minimum met",
                modifier = Modifier.size(20.dp),
                tint = Color.Unspecified,
            )
        }
    }
}

/**
 * Calendar dialog that highlights all days a wishlist item was inserted.
 */
@Composable
private fun CrossDatesCalendarDialog(
    dates: Set<LocalDate>,
    onDismiss: () -> Unit,
) {
    val month = dates.maxOrNull()?.let { YearMonth.from(it) } ?: YearMonth.now()
    var displayMonth by remember(month) { mutableStateOf(month) }
    val daysInMonth = displayMonth.lengthOfMonth()
    val firstDayOfWeek = displayMonth.atDay(1).dayOfWeek.value % 7 // Sunday = 0

    AlertDialog(
        onDismissRequest = onDismiss,
        title = {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically,
            ) {
                TextButton(onClick = { displayMonth = displayMonth.minusMonths(1) }) {
                    Text("◀")
                }
                Text(
                    text = "${displayMonth.month.getDisplayName(TextStyle.FULL, Locale.getDefault())} ${displayMonth.year}",
                    style = MaterialTheme.typography.titleMedium,
                )
                TextButton(onClick = { displayMonth = displayMonth.plusMonths(1) }) {
                    Text("▶")
                }
            }
        },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                // Day-of-week headers
                Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceEvenly) {
                    listOf("S", "M", "T", "W", "T", "F", "S").forEach { day ->
                        Text(
                            text = day,
                            style = MaterialTheme.typography.labelSmall,
                            fontWeight = FontWeight.Bold,
                            modifier = Modifier.size(36.dp),
                            textAlign = TextAlign.Center,
                        )
                    }
                }
                // Calendar grid
                LazyVerticalGrid(
                    columns = GridCells.Fixed(7),
                    modifier = Modifier.fillMaxWidth(),
                    verticalArrangement = Arrangement.spacedBy(2.dp),
                    horizontalArrangement = Arrangement.spacedBy(2.dp),
                ) {
                    // Empty cells for days before the 1st
                    items(firstDayOfWeek) {
                        Box(Modifier.size(36.dp))
                    }
                    // Day cells
                    items(daysInMonth) { dayIndex ->
                        val day = dayIndex + 1
                        val date = displayMonth.atDay(day)
                        val isHighlighted = date in dates
                        Box(
                            modifier = Modifier
                                .size(36.dp)
                                .then(
                                    if (isHighlighted) {
                                        Modifier.background(
                                            AppTheme.colors.primary.copy(alpha = 0.3f),
                                            CircleShape,
                                        )
                                    } else Modifier
                                ),
                            contentAlignment = Alignment.Center,
                        ) {
                            Text(
                                text = day.toString(),
                                style = MaterialTheme.typography.bodySmall,
                                fontWeight = if (isHighlighted) FontWeight.Bold else FontWeight.Normal,
                                color = if (isHighlighted) AppTheme.colors.primary else Color.Unspecified,
                            )
                        }
                    }
                }
                // Summary
                Text(
                    text = "${dates.size} date(s) with crosses",
                    style = MaterialTheme.typography.labelSmall,
                    color = Color.Gray,
                    modifier = Modifier.padding(top = 8.dp),
                )
            }
        },
        confirmButton = {
            TextButton(onClick = onDismiss) {
                Text(stringResource(R.string.dialog_ok))
            }
        },
    )
}


/**
 * Internal wrapper exposing [CrossDatesCalendarDialog] for screenshot testing.
 * The original composable retains `private` visibility for production encapsulation.
 */
@Composable
internal fun CrossDatesCalendarDialogPreview(
    dates: Set<LocalDate>,
    onDismiss: () -> Unit = {},
) {
    CrossDatesCalendarDialog(dates = dates, onDismiss = onDismiss)
}

// ─── Previews ───────────────────────────────────────────────────────────────────

@Preview(showBackground = true, name = "CrossTrackerScreen - Populated")
@Preview(showBackground = true, name = "CrossTrackerScreen - Tablet", device = "spec:width=800dp,height=1280dp")
@Composable
private fun CrossTrackerScreenPopulatedPreview() {
    IntercrossPreviewTheme {
        CrossTrackerScreen(
            rows = PreviewSampleData.crossTrackerRowsWithComplete,
            filter = CrossFilter.ALL,
            onFilterChange = {},
            onOpenCrossBlock = {},
            onCreateWishlist = {},
            onRowClick = {},
            topBarState = TopBarState(
                title = "",
                actions = listOf(
                    TopBarAction("cross_block", R.string.crossblock, iconRes = R.drawable.ic_grid_view_black_24dp),
                    TopBarAction("hide_completed", R.string.summary_label, iconRes = R.drawable.ic_visibility),
                ),
            ),
            bottomBarState = BottomBarState(
                selectedRoute = "cross_tracker",
                onTabSelected = {},
            ),
        )
    }
}

@Preview(showBackground = true, name = "CrossTrackerScreen - Empty")
@Composable
private fun CrossTrackerScreenEmptyPreview() {
    IntercrossPreviewTheme {
        CrossTrackerScreen(
            rows = emptyList(),
            filter = CrossFilter.ALL,
            onFilterChange = {},
            onOpenCrossBlock = {},
            onCreateWishlist = {},
            onRowClick = {},
            topBarState = TopBarState(
                title = "",
                actions = listOf(
                    TopBarAction("cross_block", R.string.crossblock, iconRes = R.drawable.ic_grid_view_black_24dp),
                    TopBarAction("hide_completed", R.string.summary_label, iconRes = R.drawable.ic_visibility),
                ),
            ),
            bottomBarState = BottomBarState(
                selectedRoute = "cross_tracker",
                onTabSelected = {},
            ),
        )
    }
}
