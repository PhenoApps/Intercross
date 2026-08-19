package org.phenoapps.intercross.ui.summary

import android.graphics.Typeface
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ElevatedCard
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.platform.LocalInspectionMode
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.viewinterop.AndroidView
import com.github.mikephil.charting.charts.BarChart
import com.github.mikephil.charting.charts.LineChart
import com.github.mikephil.charting.components.XAxis
import com.github.mikephil.charting.data.BarData
import com.github.mikephil.charting.data.BarDataSet
import com.github.mikephil.charting.data.BarEntry
import com.github.mikephil.charting.data.Entry
import com.github.mikephil.charting.data.LineData
import com.github.mikephil.charting.data.LineDataSet
import com.github.mikephil.charting.formatter.IndexAxisValueFormatter
import org.phenoapps.intercross.R
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.ui.res.painterResource
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.NavigationBarItemDefaults
import org.phenoapps.intercross.ui.app.TopBarState
import org.phenoapps.intercross.ui.app.BottomBarState
import org.phenoapps.intercross.ui.app.BottomDestinations
import org.phenoapps.intercross.ui.components.SafeIcon
import org.phenoapps.intercross.ui.preview.PreviewSampleData
import org.phenoapps.intercross.ui.theme.AppTheme
import org.phenoapps.intercross.ui.theme.IntercrossPreviewTheme

private val ChartBarColors = listOf(
    Color(0xFF3F51B5), // Indigo
    Color(0xFFFF5722), // Deep Orange
    Color(0xFF4CAF50), // Green
    Color(0xFFFFC107), // Amber
    Color(0xFF9C27B0), // Purple
    Color(0xFF00BCD4), // Cyan
    Color(0xFFE91E63), // Pink
    Color(0xFF795548), // Brown
)

@Composable
fun SummaryScreen(
    chartData: SummaryChartData,
    topBarState: TopBarState? = null,
    bottomBarState: BottomBarState? = null,
) {
    val totalCrosses = chartData.typeSlices.sumOf { it.value.toInt() }

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
    ) { innerPadding ->
    LazyColumn(
        Modifier.fillMaxSize().padding(innerPadding).padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp),
    ) {
        // Stats overview cards
        item {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(12.dp),
            ) {
                StatCard(
                    label = stringResource(R.string.crosses),
                    value = totalCrosses.toString(),
                    color = AppTheme.colors.primary,
                    modifier = Modifier.weight(1f),
                )
                StatCard(
                    label = "Types",
                    value = chartData.typeSlices.size.toString(),
                    color = AppTheme.colors.accent,
                    modifier = Modifier.weight(1f),
                )
                StatCard(
                    label = "Days",
                    value = chartData.crossesOverTime.size.toString(),
                    color = Color(0xFF4CAF50),
                    modifier = Modifier.weight(1f),
                )
            }
        }

        // Cross types bar chart
        item {
            SummaryBarSection(
                title = stringResource(R.string.crosses),
                slices = chartData.typeSlices,
            )
        }

        // Metadata bar chart
        item {
            SummaryBarSection(
                title = stringResource(R.string.metaData),
                slices = chartData.metadataBars,
            )
        }

        // Crosses over time line chart
        item { SummaryLineSection(points = chartData.crossesOverTime) }
    }
    } // Scaffold
}

@Composable
private fun StatCard(
    label: String,
    value: String,
    color: Color,
    modifier: Modifier = Modifier,
) {
    ElevatedCard(
        modifier = modifier,
        shape = RoundedCornerShape(12.dp),
        colors = CardDefaults.elevatedCardColors(containerColor = Color.White),
        elevation = CardDefaults.elevatedCardElevation(defaultElevation = 4.dp),
    ) {
        Column(
            modifier = Modifier.padding(16.dp).fillMaxWidth(),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(4.dp),
        ) {
            Box(
                modifier = Modifier
                    .size(8.dp)
                    .background(color, CircleShape),
            )
            Text(
                text = value,
                style = MaterialTheme.typography.headlineMedium,
                fontWeight = FontWeight.Bold,
                color = color,
            )
            Text(
                text = label,
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
    }
}

@Composable
private fun SummaryBarSection(title: String, slices: List<SummarySlice>) {
    val isPreview = LocalInspectionMode.current
    ChartCard(title) {
        if (slices.isEmpty()) {
            Text(stringResource(R.string.no_crosses_text), Modifier.padding(12.dp))
        } else {
            // Color legend
            FlowRow(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(12.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp),
            ) {
                slices.forEachIndexed { index, slice ->
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(4.dp),
                    ) {
                        Box(
                            Modifier
                                .size(10.dp)
                                .background(
                                    ChartBarColors[index % ChartBarColors.size],
                                    CircleShape,
                                ),
                        )
                        Text(
                            text = "${slice.label} (${slice.value.toInt()})",
                            style = MaterialTheme.typography.labelSmall,
                        )
                    }
                }
            }
            AndroidView(
                factory = { context ->
                    BarChart(context).apply {
                        description.isEnabled = false
                        legend.isEnabled = false
                        setFitBars(true)
                        if (!isPreview) {
                            animateY(800)
                        }
                        setDrawValueAboveBar(true)
                        xAxis.position = XAxis.XAxisPosition.BOTTOM
                        xAxis.setDrawGridLines(false)
                        xAxis.granularity = 1f
                        xAxis.textSize = 11f
                        axisLeft.axisMinimum = 0f
                        axisLeft.setDrawGridLines(true)
                        axisLeft.gridColor = android.graphics.Color.parseColor("#E0E0E0")
                        axisRight.isEnabled = false
                        setExtraOffsets(8f, 8f, 8f, 8f)
                    }
                },
                update = { chart ->
                    val entries = slices.mapIndexed { index, slice ->
                        BarEntry(index.toFloat(), slice.value)
                    }
                    val colors = slices.mapIndexed { index, _ ->
                        ChartBarColors[index % ChartBarColors.size].toArgb()
                    }
                    val dataSet = BarDataSet(entries, title).apply {
                        setColors(colors)
                        valueTextSize = 12f
                        valueTypeface = Typeface.DEFAULT_BOLD
                    }
                    chart.data = BarData(dataSet).apply { barWidth = 0.6f }
                    chart.xAxis.valueFormatter = IndexAxisValueFormatter(slices.map { it.label })
                    chart.xAxis.labelCount = slices.size
                    chart.notifyDataSetChanged()
                    chart.invalidate()
                },
                modifier = Modifier
                    .fillMaxWidth()
                    .heightIn(min = 220.dp)
                    .testTag("mpandroid_bar_chart"),
            )
        }
    }
}

@Composable
private fun SummaryLineSection(points: List<SummaryPoint>) {
    val isPreview = LocalInspectionMode.current
    ChartCard(stringResource(R.string.graph_cross_over_time_title)) {
        if (points.isEmpty()) {
            Text(stringResource(R.string.no_crosses_text), Modifier.padding(12.dp))
        } else {
            val primaryColor = AppTheme.colors.primary
            val accentColor = AppTheme.colors.accent
            val chartLabel = stringResource(R.string.graph_cross_over_time_title)
            // Summary text
            val latest = points.lastOrNull()
            if (latest != null) {
                Text(
                    text = "${latest.value.toInt()} total crosses as of ${latest.label}",
                    style = MaterialTheme.typography.labelMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.padding(bottom = 4.dp),
                )
            }
            AndroidView(
                factory = { context ->
                    LineChart(context).apply {
                        description.isEnabled = false
                        legend.isEnabled = false
                        if (!isPreview) {
                            animateX(1000)
                        }
                        xAxis.position = XAxis.XAxisPosition.BOTTOM
                        xAxis.setDrawGridLines(false)
                        xAxis.granularity = 1f
                        xAxis.textSize = 10f
                        xAxis.labelRotationAngle = -30f
                        axisLeft.axisMinimum = 0f
                        axisLeft.setDrawGridLines(true)
                        axisLeft.gridColor = android.graphics.Color.parseColor("#E0E0E0")
                        axisRight.isEnabled = false
                        setTouchEnabled(true)
                        isDragEnabled = true
                        setScaleEnabled(true)
                        setPinchZoom(true)
                        setExtraOffsets(8f, 8f, 8f, 16f)
                    }
                },
                update = { chart ->
                    val entries = points.mapIndexed { index, point ->
                        Entry(index.toFloat(), point.value)
                    }
                    val dataSet = LineDataSet(entries, chartLabel).apply {
                        color = primaryColor.toArgb()
                        lineWidth = 3f
                        setCircleColor(accentColor.toArgb())
                        circleRadius = 5f
                        setDrawCircleHole(true)
                        circleHoleRadius = 2.5f
                        valueTextSize = 10f
                        valueTypeface = Typeface.DEFAULT_BOLD
                        mode = LineDataSet.Mode.CUBIC_BEZIER
                        setDrawFilled(true)
                        fillColor = primaryColor.toArgb()
                        fillAlpha = 30
                    }
                    chart.data = LineData(dataSet)
                    chart.xAxis.valueFormatter = IndexAxisValueFormatter(points.map { it.label })
                    chart.xAxis.labelCount = points.size.coerceAtMost(6)
                    chart.notifyDataSetChanged()
                    chart.invalidate()
                },
                modifier = Modifier
                    .fillMaxWidth()
                    .heightIn(min = 240.dp)
                    .testTag("mpandroid_line_chart"),
            )
        }
    }
}

@Composable
private fun ChartCard(title: String, content: @Composable () -> Unit) {
    ElevatedCard(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.elevatedCardColors(containerColor = Color.White),
        elevation = CardDefaults.elevatedCardElevation(defaultElevation = 4.dp),
    ) {
        Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
            Text(title, style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.SemiBold)
            content()
        }
    }
}

// ─── Previews ───────────────────────────────────────────────────────────────────

@Preview(showBackground = true, name = "SummaryScreen - Populated")
@Composable
internal fun SummaryScreenPopulatedPreview() {
    IntercrossPreviewTheme {
        SummaryScreen(
            chartData = PreviewSampleData.populatedChartData,
            topBarState = TopBarState(titleRes = R.string.summary_label),
            bottomBarState = BottomBarState(
                selectedRoute = "summary",
                onTabSelected = {},
            ),
        )
    }
}

@Preview(showBackground = true, name = "SummaryScreen - Empty")
@Composable
internal fun SummaryScreenEmptyPreview() {
    IntercrossPreviewTheme {
        SummaryScreen(
            chartData = PreviewSampleData.emptyChartData,
            topBarState = TopBarState(titleRes = R.string.summary_label),
            bottomBarState = BottomBarState(
                selectedRoute = "summary",
                onTabSelected = {},
            ),
        )
    }
}

@Preview(showBackground = true, name = "SummaryBarSection - Populated")
@Composable
private fun SummaryBarSectionPopulatedPreview() {
    IntercrossPreviewTheme {
        SummaryBarSection(
            title = "Crosses by Type",
            slices = PreviewSampleData.populatedChartData.typeSlices,
        )
    }
}

@Preview(showBackground = true, name = "SummaryLineSection - Populated")
@Composable
private fun SummaryLineSectionPopulatedPreview() {
    IntercrossPreviewTheme {
        SummaryLineSection(points = PreviewSampleData.populatedChartData.crossesOverTime)
    }
}
