package org.phenoapps.intercross.ui.crosstracker

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.gestures.awaitEachGesture
import androidx.compose.foundation.gestures.awaitFirstDown
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.input.pointer.PointerEventPass
import androidx.compose.ui.input.pointer.PointerInputChange
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.input.pointer.positionChanged
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import eu.wewox.lazytable.LazyTable
import eu.wewox.lazytable.LazyTableItem
import eu.wewox.lazytable.lazyTableDimensions
import eu.wewox.lazytable.lazyTablePinConfiguration
import org.phenoapps.intercross.R
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.ui.res.painterResource
import org.phenoapps.intercross.ui.app.TopBarAction
import org.phenoapps.intercross.ui.app.TopBarState
import org.phenoapps.intercross.ui.preview.PreviewSampleData
import org.phenoapps.intercross.ui.theme.AppTheme
import org.phenoapps.intercross.ui.theme.IntercrossPreviewTheme

/**
 * Progress level categories used for legend filtering.
 */
enum class ProgressLevel(val color: Color, val label: String) {
    MAX(Color(0xFF2E7D32), "≥ Max"),
    MIN(Color(0xFF8BC34A), "≥ Min"),
    TWO_THIRD(Color(0xFFFFEB3B), "> 66%"),
    ONE_THIRD(Color(0xFFFF9800), "> 33%"),
    STARTED(Color(0xFFF44336), "> 0%"),
    EMPTY(Color(0xAAAAAAAA), "Empty"),
}

@Composable
fun CrossBlockScreen(
    matrix: CrossBlockMatrix,
    selectedWishType: String,
    wishTypes: List<String>,
    onWishTypeChange: (String) -> Unit,
    onCellClick: (CrossBlockCell) -> Unit,
    topBarState: TopBarState? = null,
) {
    var selectedLevelFilter by remember { mutableStateOf<ProgressLevel?>(null) }
    var scale by remember { mutableFloatStateOf(1f) }

    // Build a filtered/condensed matrix when a filter is active
    val displayMatrix = remember(matrix, selectedLevelFilter) {
        if (selectedLevelFilter == null) {
            matrix
        } else {
            filterMatrix(matrix, selectedLevelFilter)
        }
    }

    androidx.compose.material3.Scaffold(
        topBar = {
            if (topBarState != null) {
                @OptIn(ExperimentalMaterial3Api::class)
                (TopAppBar(
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
                ))
            }
        },
    ) { innerPadding ->
    Column(
        Modifier
            .fillMaxSize()
            .padding(innerPadding)
            .padding(horizontal = 8.dp, vertical = 8.dp),
        verticalArrangement = Arrangement.spacedBy(8.dp),
    ) {
        // Legend with clickable filter chips
        CrossBlockLegend(
            selectedLevelFilter = selectedLevelFilter,
            onLevelFilterSelected = { level ->
                selectedLevelFilter = if (selectedLevelFilter == level) null else level
            },
            selectedWishType = selectedWishType,
            wishTypes = wishTypes,
            onWishTypeSelected = onWishTypeChange,
        )

        if (displayMatrix.maleHeaders.isEmpty() || displayMatrix.femaleHeaders.isEmpty()) {
            Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                Text(
                    if (selectedLevelFilter != null) "No cells match this filter"
                    else stringResource(R.string.summary_and_wishlist_empty),
                    style = MaterialTheme.typography.bodyMedium,
                )
            }
        } else {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .prioritizedPinchZoom { zoom ->
                        scale = (scale * zoom).coerceIn(0.5f, 3f)
                    },
            ) {
                LazyTable(
                    pinConfiguration = lazyTablePinConfiguration(
                        columns = 1,
                        rows = 1,
                    ),
                    dimensions = lazyTableDimensions(
                        columnSize = { col ->
                            if (col == 0) (100 * scale).dp else (80 * scale).dp
                        },
                        rowSize = { row ->
                            if (row == 0) (48 * scale).dp else (56 * scale).dp
                        },
                    ),
                    modifier = Modifier.fillMaxSize(),
                ) {
                    // Top-left corner cell
                    items(
                        count = 1,
                        layoutInfo = { LazyTableItem(column = 0, row = 0) },
                    ) {
                        Box(
                            Modifier
                                .background(MaterialTheme.colorScheme.surfaceVariant)
                                .border(Dp.Hairline, MaterialTheme.colorScheme.outline),
                            contentAlignment = Alignment.Center,
                        ) {
                            Text(
                                text = "♀ \\ ♂",
                                style = MaterialTheme.typography.labelSmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                            )
                        }
                    }

                    // Male header row
                    items(
                        count = displayMatrix.maleHeaders.size,
                        layoutInfo = { index -> LazyTableItem(column = index + 1, row = 0) },
                    ) { index ->
                        val (_, name) = displayMatrix.maleHeaders[index]
                        Box(
                            contentAlignment = Alignment.Center,
                            modifier = Modifier
                                .background(MaterialTheme.colorScheme.primaryContainer)
                                .border(Dp.Hairline, MaterialTheme.colorScheme.outline),
                        ) {
                            Text(
                                text = name,
                                maxLines = 2,
                                overflow = TextOverflow.Ellipsis,
                                style = MaterialTheme.typography.labelSmall,
                                textAlign = TextAlign.Center,
                                modifier = Modifier.padding(4.dp),
                            )
                        }
                    }

                    // Female header column
                    items(
                        count = displayMatrix.femaleHeaders.size,
                        layoutInfo = { index -> LazyTableItem(column = 0, row = index + 1) },
                    ) { index ->
                        val (_, name) = displayMatrix.femaleHeaders[index]
                        Box(
                            contentAlignment = Alignment.Center,
                            modifier = Modifier
                                .background(MaterialTheme.colorScheme.secondaryContainer)
                                .border(Dp.Hairline, MaterialTheme.colorScheme.outline),
                        ) {
                            Text(
                                text = name,
                                maxLines = 2,
                                overflow = TextOverflow.Ellipsis,
                                style = MaterialTheme.typography.labelSmall,
                                textAlign = TextAlign.Center,
                                modifier = Modifier.padding(4.dp),
                            )
                        }
                    }

                    // Data cells
                    items(
                        count = displayMatrix.femaleHeaders.size * displayMatrix.maleHeaders.size,
                        layoutInfo = { index ->
                            val col = index % displayMatrix.maleHeaders.size
                            val row = index / displayMatrix.maleHeaders.size
                            LazyTableItem(column = col + 1, row = row + 1)
                        },
                    ) { index ->
                        val col = index % displayMatrix.maleHeaders.size
                        val row = index / displayMatrix.maleHeaders.size
                        val cell = displayMatrix.rows[row][col]
                        val cellColor = Color(cell.progressColor)
                        Box(
                            contentAlignment = Alignment.Center,
                            modifier = Modifier
                                .background(cellColor)
                                .border(Dp.Hairline, MaterialTheme.colorScheme.outlineVariant)
                                .singlePointerCellClick { onCellClick(cell) },
                        ) {}
                    }
                }
            }
        }
    }
    } // Scaffold
}

private fun Modifier.prioritizedPinchZoom(onZoom: (Float) -> Unit): Modifier = pointerInput(onZoom) {
    awaitEachGesture {
        var previousDistance: Float? = null
        do {
            val event = awaitPointerEvent(PointerEventPass.Initial)
            val pressedChanges = event.changes.filter { it.pressed }
            if (pressedChanges.size >= 2) {
                val distance = pressedChanges.take(2).distanceBetween()
                previousDistance?.takeIf { it > 0f }?.let { previous ->
                    val zoom = distance / previous
                    if (zoom.isFinite() && zoom > 0f) {
                        onZoom(zoom)
                    }
                }
                previousDistance = distance
                event.changes.forEach { change ->
                    if (change.positionChanged()) {
                        change.consume()
                    }
                }
            } else {
                previousDistance = null
            }
        } while (event.changes.any { it.pressed })
    }
}

private fun Modifier.singlePointerCellClick(onClick: () -> Unit): Modifier = pointerInput(onClick) {
    awaitEachGesture {
        val down = awaitFirstDown(requireUnconsumed = false)
        val startPosition = down.position
        var cancelTap = down.isConsumed
        var pointerIsDown = true

        while (pointerIsDown) {
            val event = awaitPointerEvent()
            val pressedChanges = event.changes.filter { it.pressed }
            if (pressedChanges.size > 1) {
                cancelTap = true
            }

            val trackedChange = event.changes.firstOrNull { it.id == down.id }
            if (trackedChange == null || !trackedChange.pressed) {
                pointerIsDown = false
            } else {
                if ((trackedChange.position - startPosition).getDistance() > viewConfiguration.touchSlop) {
                    cancelTap = true
                }
                if (trackedChange.isConsumed) {
                    cancelTap = true
                }
            }
        }

        if (!cancelTap) {
            onClick()
        }
    }
}

private fun List<PointerInputChange>.distanceBetween(): Float {
    val first = this[0].position
    val second = this[1].position
    return (second - first).getDistance()
}

/**
 * Filters the matrix to only include rows and columns that have at least one cell
 * matching the given filters. This condenses the grid.
 */
private fun filterMatrix(
    matrix: CrossBlockMatrix,
    levelFilter: ProgressLevel?
): CrossBlockMatrix {
    fun matches(cell: CrossBlockCell): Boolean {
        return levelFilter == null || cellColorMatches(Color(cell.progressColor), levelFilter.color)
    }

    // Find which column indices have at least one matching cell
    val matchingColIndices = matrix.maleHeaders.indices.filter { colIdx ->
        matrix.rows.any { row -> matches(row[colIdx]) }
    }

    // Find which row indices have at least one matching cell
    val matchingRowIndices = matrix.femaleHeaders.indices.filter { rowIdx ->
        matrix.rows[rowIdx].any { cell -> matches(cell) }
    }

    if (matchingColIndices.isEmpty() || matchingRowIndices.isEmpty()) {
        return CrossBlockMatrix(emptyList(), emptyList(), emptyList())
    }

    val filteredMaleHeaders = matchingColIndices.map { matrix.maleHeaders[it] }
    val filteredFemaleHeaders = matchingRowIndices.map { matrix.femaleHeaders[it] }
    val filteredRows = matchingRowIndices.map { rowIdx ->
        matchingColIndices.map { colIdx ->
            val cell = matrix.rows[rowIdx][colIdx]
            // If the cell itself doesn't match the filter, hide it in the UI
            if (matches(cell)) cell else cell.copy(progressColor = Color.Transparent.toArgb(), hasWish = false)
        }
    }

    return CrossBlockMatrix(filteredMaleHeaders, filteredFemaleHeaders, filteredRows)
}

private fun cellColorMatches(cellColor: Color, filterColor: Color): Boolean {
    return cellColor == filterColor
}

@Composable
private fun CrossBlockLegend(
    selectedLevelFilter: ProgressLevel?,
    onLevelFilterSelected: (ProgressLevel) -> Unit,
    selectedWishType: String,
    wishTypes: List<String>,
    onWishTypeSelected: (String) -> Unit,
) {
    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
        if (wishTypes.isNotEmpty()) {
            FlowRow(
                horizontalArrangement = Arrangement.spacedBy(6.dp),
                verticalArrangement = Arrangement.spacedBy(4.dp),
                modifier = Modifier.fillMaxWidth(),
            ) {
                wishTypes.forEach { type ->
                    FilterChip(
                        selected = selectedWishType == type,
                        onClick = { onWishTypeSelected(type) },
                        label = { Text(type, style = MaterialTheme.typography.labelSmall) },
                        shape = RoundedCornerShape(16.dp),
                        colors = FilterChipDefaults.filterChipColors(
                            selectedContainerColor = MaterialTheme.colorScheme.primaryContainer,
                        ),
                    )
                }
            }
        }

        FlowRow(
            horizontalArrangement = Arrangement.spacedBy(6.dp),
            verticalArrangement = Arrangement.spacedBy(4.dp),
            modifier = Modifier.fillMaxWidth(),
        ) {
            ProgressLevel.entries.forEach { level ->
                FilterChip(
                    selected = selectedLevelFilter == level,
                    onClick = { onLevelFilterSelected(level) },
                    label = { Text(level.label, style = MaterialTheme.typography.labelSmall) },
                    leadingIcon = {
                        Box(
                            Modifier
                                .size(12.dp)
                                .background(level.color, RoundedCornerShape(2.dp))
                                .border(Dp.Hairline, Color.DarkGray, RoundedCornerShape(2.dp)),
                        )
                    },
                    colors = FilterChipDefaults.filterChipColors(
                        selectedContainerColor = MaterialTheme.colorScheme.primaryContainer,
                    ),
                )
            }
        }
    }
}

@Preview(showBackground = true, name = "CrossBlockScreen - Default")
@Composable
internal fun CrossBlockScreenPreview() {
    IntercrossPreviewTheme {
        CrossBlockScreen(
            matrix = PreviewSampleData.crossBlockMatrix,
            selectedWishType = "seeds",
            wishTypes = listOf("seeds", "flowers", "chips"),
            onWishTypeChange = {},
            onCellClick = {},
            topBarState = TopBarState(
                titleRes = R.string.cross_block_label,
                showBack = true,
                actions = listOf(
                    TopBarAction("crossblock_add_wish", R.string.add_wishlist_item, iconRes = R.drawable.ic_add_black_24dp),
                ),
            ),
        )
    }
}

@Preview(showBackground = true, name = "CrossBlockScreen - Complex")
@Composable
internal fun CrossBlockScreenComplexPreview() {
    IntercrossPreviewTheme {
        CrossBlockScreen(
            matrix = PreviewSampleData.crossBlockMatrix,
            selectedWishType = "chips",
            wishTypes = listOf("chips", "beans"),
            onWishTypeChange = {},
            onCellClick = {},
            topBarState = TopBarState(
                titleRes = R.string.cross_block_label,
                showBack = true,
                actions = listOf(
                    TopBarAction("crossblock_add_wish", R.string.add_wishlist_item, iconRes = R.drawable.ic_add_black_24dp),
                ),
            ),
        )
    }
}
