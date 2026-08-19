package org.phenoapps.intercross.ui.summary

import org.phenoapps.intercross.data.dao.EventsDao
import org.phenoapps.intercross.data.models.CrossType
import org.phenoapps.intercross.data.models.Event
import org.phenoapps.intercross.util.DateUtil

data class SummarySlice(val label: String, val value: Float)
data class SummaryPoint(val label: String, val value: Float)

data class SummaryChartData(
    val typeSlices: List<SummarySlice>,
    val metadataBars: List<SummarySlice>,
    val crossesOverTime: List<SummaryPoint>,
)

fun buildSummaryChartData(events: List<Event>, metadata: List<EventsDao.CrossMetadata>): SummaryChartData {
    val typeSlices = CrossType.entries.mapNotNull { type ->
        val count = events.count { it.type == type }.toFloat()
        count.takeIf { it > 0f }?.let { SummarySlice(type.name.lowercase().replaceFirstChar(Char::uppercase), it) }
    }
    val metadataBars = metadata.groupBy { it.property }
        .map { (property, values) -> SummarySlice(property, values.sumOf { it.value ?: 0 }.toFloat()) }
        .filter { it.value > 0f }
        .sortedBy { it.label }
    val dateUtil = DateUtil()
    var cumulative = 0
    val crossesOverTime = events.sortedBy { it.timestamp }
        .groupBy { event ->
            runCatching { dateUtil.getFormattedDate(event.timestamp) }
                .getOrElse { event.timestamp.substringBefore("_") }
        }
        .toSortedMap()
        .map { (date, items) ->
            cumulative += items.size
            SummaryPoint(date, cumulative.toFloat())
        }
    return SummaryChartData(typeSlices, metadataBars, crossesOverTime)
}
