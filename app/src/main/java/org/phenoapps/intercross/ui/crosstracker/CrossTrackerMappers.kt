package org.phenoapps.intercross.ui.crosstracker

import android.graphics.Color
import org.phenoapps.intercross.data.dao.EventsDao
import org.phenoapps.intercross.data.models.Event
import org.phenoapps.intercross.data.models.Meta
import org.phenoapps.intercross.data.models.MetadataValues
import org.phenoapps.intercross.data.models.Parent
import org.phenoapps.intercross.data.models.WishlistView

enum class CrossFilter { ALL, PLANNED, UNPLANNED }

data class CrossPersonCount(val name: String, val count: Int)
data class CrossDateCount(val date: String, val count: Int)
data class WishlistProgressItem(val wishType: String, val min: Int, val max: Int, val progress: Int)

private val WishlistProgressItem.isComplete: Boolean
    get() = progress >= min

sealed class CrossTrackerRow {
    abstract val male: String
    abstract val female: String
    abstract val count: Int
    abstract val maleId: String
    abstract val femaleId: String
    abstract val persons: List<CrossPersonCount>
    abstract val dates: List<CrossDateCount>

    data class Unplanned(
        override val male: String,
        override val female: String,
        override val count: Int,
        override val maleId: String,
        override val femaleId: String,
        override val persons: List<CrossPersonCount> = emptyList(),
        override val dates: List<CrossDateCount> = emptyList(),
    ) : CrossTrackerRow()

    data class Planned(
        override val male: String,
        override val female: String,
        override val count: Int,
        override val maleId: String,
        override val femaleId: String,
        override val persons: List<CrossPersonCount> = emptyList(),
        override val dates: List<CrossDateCount> = emptyList(),
        val wishes: List<WishlistProgressItem> = emptyList(),
    ) : CrossTrackerRow()
}

data class CrossBlockCell(
    val femaleId: String,
    val maleId: String,
    val progressColor: Int,
    val hasWish: Boolean,
)

data class CrossBlockMatrix(
    val maleHeaders: List<Pair<String, String>>,
    val femaleHeaders: List<Pair<String, String>>,
    val rows: List<List<CrossBlockCell>>,
)

fun buildCrossTrackerRows(
    crosses: List<EventsDao.ParentCount>,
    wishes: List<WishlistView>,
    parents: List<Parent>,
    events: List<Event>,
    metaValues: List<MetadataValues>,
    metadata: List<Meta>,
    filter: CrossFilter,
    commutative: Boolean,
    showCompleted: Boolean,
    crossWishType: String,
): List<CrossTrackerRow> {
    val parentLookup = parents.associate { it.codeId to it.name }
    fun normalize(id: String) = parentLookup[id] ?: id
    fun samePair(aDad: String, aMom: String, bDad: String, bMom: String): Boolean {
        val aD = normalize(aDad)
        val aM = normalize(aMom)
        val bD = normalize(bDad)
        val bM = normalize(bMom)
        return if (commutative) {
            (aD == bD && aM == bM) || (aD == bM && aM == bD)
        } else {
            aD == bD && aM == bM
        }
    }

    // Build a lookup: parent pair to set of event IDs for filtering metadata values
    fun eventIdsForPair(femaleId: String, maleId: String): Set<Int> {
        return events.mapNotNull { event ->
            val mom = event.femaleObsUnitDbId
            val dad = event.maleObsUnitDbId
            val match = if (commutative) {
                samePair(dad, mom, maleId, femaleId)
            } else {
                mom == femaleId && dad == maleId
            }
            if (match) event.id?.toInt() else null
        }.toSet()
    }

    val crossRows = crosses.map { cross ->
        val matchingWishes = wishes.filter { wish ->
            samePair(cross.dad, cross.mom, wish.dadId, wish.momId)
        }
        val persons = cross.person.takeIf { it.isNotBlank() }?.let { listOf(CrossPersonCount(it, cross.count)) }.orEmpty()
        val dates = cross.date.takeIf { it.isNotBlank() }?.let { listOf(CrossDateCount(it, cross.count)) }.orEmpty()
        if (matchingWishes.isEmpty()) {
            CrossTrackerRow.Unplanned(
                male = normalize(cross.dad),
                female = normalize(cross.mom),
                count = cross.count,
                maleId = cross.dad,
                femaleId = cross.mom,
                persons = persons,
                dates = dates,
            )
        } else {
            val first = matchingWishes.first()
            // Compute correct progress per wish type using events + metaValues
            val eventIds = eventIdsForPair(first.momId, first.dadId)
            val wishItems = matchingWishes.map { wish ->
                val progress = progressForWishType(
                    wishType = wish.wishType,
                    crossWishType = crossWishType,
                    count = cross.count,
                    eventIds = eventIds,
                    metadata = metadata,
                    metaValues = metaValues,
                )
                WishlistProgressItem(wish.wishType, wish.wishMin, wish.wishMax, progress)
            }
            CrossTrackerRow.Planned(
                male = first.dadName,
                female = first.momName,
                count = cross.count,
                maleId = first.dadId,
                femaleId = first.momId,
                persons = persons,
                dates = dates,
                wishes = wishItems,
            )
        }
    }

    val remainingWishes = wishes.filter { wish ->
        crossRows.none { row -> samePair(row.maleId, row.femaleId, wish.dadId, wish.momId) }
    }.map { wish ->
        // No events exist for this pair yet, so progress is 0 for all types
        CrossTrackerRow.Planned(
            male = wish.dadName,
            female = wish.momName,
            count = 0,
            maleId = wish.dadId,
            femaleId = wish.momId,
            wishes = listOf(WishlistProgressItem(wish.wishType, wish.wishMin, wish.wishMax, 0)),
        )
    }

    val filtered = (crossRows + remainingWishes).filter {
        when (filter) {
            CrossFilter.ALL -> true
            CrossFilter.PLANNED -> it is CrossTrackerRow.Planned
            CrossFilter.UNPLANNED -> it is CrossTrackerRow.Unplanned
        }
    }

    val grouped = filtered.groupBy { row ->
        val first = normalize(row.maleId)
        val second = normalize(row.femaleId)
        if (commutative && first > second) "$second::$first" else "$first::$second"
    }.values.map { rows ->
        val first = rows.first()
        val count = rows.sumOf { it.count }
        val persons = rows.flatMap { it.persons }.groupBy { it.name }
            .map { (name, items) -> CrossPersonCount(name, items.sumOf { it.count }) }
        val dates = rows.flatMap { it.dates }.groupBy { it.date }
            .map { (date, items) -> CrossDateCount(date, items.sumOf { it.count }) }
        val plannedRows = rows.filterIsInstance<CrossTrackerRow.Planned>()
        if (plannedRows.isEmpty()) {
            CrossTrackerRow.Unplanned(first.male, first.female, count, first.maleId, first.femaleId, persons, dates)
        } else {
            val wishesForRow = plannedRows.flatMap { it.wishes }.groupBy { it.wishType }
                .map { (type, items) ->
                    val eventIds = eventIdsForPair(first.femaleId, first.maleId)
                    val progress = progressForWishType(
                        wishType = type,
                        crossWishType = crossWishType,
                        count = count,
                        eventIds = eventIds,
                        metadata = metadata,
                        metaValues = metaValues,
                    )
                    WishlistProgressItem(type, items.minOf { it.min }, items.maxOf { it.max }, progress)
                }
            CrossTrackerRow.Planned(first.male, first.female, count, first.maleId, first.femaleId, persons, dates, wishesForRow)
        }
    }

    return grouped.mapNotNull { row ->
        if (showCompleted || row !is CrossTrackerRow.Planned) {
            row
        } else {
            val incompleteWishes = row.wishes.filterNot { it.isComplete }
            row.copy(wishes = incompleteWishes).takeIf { incompleteWishes.isNotEmpty() }
        }
    }.sortedWith(compareByDescending<CrossTrackerRow> { it is CrossTrackerRow.Planned }.thenBy { it.female }.thenBy { it.male })
}

/**
 * Computes the progress for a given wish type.
 *
 * For the "cross" type, progress is simply the count of crosses for the parent pair.
 * For other types (e.g. "seeds"), progress is the sum of metadata values for that property,
 * filtered to only the events that belong to this parent pair.
 */
internal fun progressForWishType(
    wishType: String,
    crossWishType: String,
    count: Int,
    eventIds: Set<Int>,
    metadata: List<Meta>,
    metaValues: List<MetadataValues>,
): Int {
    if (wishType == crossWishType) return count
    val metaId = metadata.firstOrNull { it.property == wishType }?.id?.toInt() ?: return 0
    return metaValues
        .filter { it.metaId == metaId && it.eid in eventIds }
        .sumOf { it.value ?: 0 }
}

fun buildCrossBlockMatrix(entries: List<WishlistView>, colorForWish: (WishlistView) -> Int): CrossBlockMatrix {
    val maleHeaders = entries.map { it.dadId to it.dadName }.distinctBy { it.first }.sortedBy { it.second }
    val femaleHeaders = entries.map { it.momId to it.momName }.distinctBy { it.first }.sortedBy { it.second }
    val rows = femaleHeaders.map { female ->
        maleHeaders.map { male ->
            val wish = entries.firstOrNull { it.momId == female.first && it.dadId == male.first }
            CrossBlockCell(
                femaleId = female.first,
                maleId = male.first,
                progressColor = wish?.let(colorForWish) ?: Color.TRANSPARENT,
                hasWish = wish != null,
            )
        }
    }
    return CrossBlockMatrix(maleHeaders, femaleHeaders, rows)
}

fun List<WishlistView>.calculateActualProgress(
    events: List<Event>,
    metadata: List<Meta>,
    metaValues: List<MetadataValues>,
    commutative: Boolean
): List<WishlistView> {
    return this.map { wish ->
        if (wish.wishType == "cross") {
            wish
        } else {
            val eventIds = events.filter { event ->
                val mom = event.femaleObsUnitDbId
                val dad = event.maleObsUnitDbId
                val dadMatch = dad == wish.dadId || (dad == "blank" && wish.dadId == "-1")
                val normalMatch = mom == wish.momId && dadMatch
                if (commutative) {
                    val momMatchRev = mom == wish.dadId || (mom == "blank" && wish.dadId == "-1")
                    val revMatch = dad == wish.momId && momMatchRev
                    normalMatch || revMatch
                } else {
                    normalMatch
                }
            }.mapNotNull { it.id?.toInt() }.toSet()

            val actualProgress = progressForWishType(
                wishType = wish.wishType,
                crossWishType = "cross",
                count = eventIds.size,
                eventIds = eventIds,
                metadata = metadata,
                metaValues = metaValues
            )
            wish.copy(wishProgress = actualProgress)
        }
    }
}
