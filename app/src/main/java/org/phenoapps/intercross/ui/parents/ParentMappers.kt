package org.phenoapps.intercross.ui.parents

import org.phenoapps.intercross.data.models.Event
import org.phenoapps.intercross.data.models.Parent
import org.phenoapps.intercross.data.models.PollenGroup

data class ParentListRow(
    val codeId: String,
    val name: String,
    val sex: Int,
    val selected: Boolean,
    val crossCount: Int,
    val isGroup: Boolean,
)

enum class ParentSortType { NAME, ID, CROSSES }

fun buildParentRows(
    parents: List<Parent>,
    groups: List<PollenGroup>,
    events: List<Event>,
    tabIndex: Int,
    sortType: ParentSortType,
): List<ParentListRow> {
    val femaleCounts = events.groupingBy { it.femaleObsUnitDbId }.eachCount()
    val maleCounts = events.groupingBy { it.maleObsUnitDbId }.eachCount()
    val rows = when (tabIndex) {
        2 -> groups.distinctBy { it.codeId }.map {
            ParentListRow(it.codeId, it.name, 1, it.selected, maleCounts[it.codeId] ?: 0, true)
        } + parents.filter { it.sex == 1 }.distinctBy { it.codeId }.map {
            ParentListRow(it.codeId, it.name, it.sex, it.selected, maleCounts[it.codeId] ?: 0, false)
        }
        0 -> parents.filter { it.sex == 0 }.distinctBy { it.codeId }.map {
            ParentListRow(it.codeId, it.name, it.sex, it.selected, femaleCounts[it.codeId] ?: 0, false)
        } + groups.distinctBy { it.codeId }.map {
            ParentListRow(it.codeId, it.name, 1, it.selected, maleCounts[it.codeId] ?: 0, true)
        } + parents.filter { it.sex == 1 }.distinctBy { it.codeId }.map {
            ParentListRow(it.codeId, it.name, it.sex, it.selected, maleCounts[it.codeId] ?: 0, false)
        }
        else -> parents.filter { it.sex == 0 }.distinctBy { it.codeId }.map {
            ParentListRow(it.codeId, it.name, it.sex, it.selected, femaleCounts[it.codeId] ?: 0, false)
        }
    }

    return when (sortType) {
        ParentSortType.ID -> rows.sortedBy { it.codeId.lowercase() }
        ParentSortType.CROSSES -> rows.sortedByDescending { it.crossCount }
        ParentSortType.NAME -> rows.sortedBy { it.name.lowercase() }
    }
}
