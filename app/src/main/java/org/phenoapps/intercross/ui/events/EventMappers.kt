package org.phenoapps.intercross.ui.events

import org.phenoapps.intercross.data.models.Event
import org.phenoapps.intercross.data.models.WishlistView

enum class EventSortType {
    DATE,
    NAME,
    ID,
    FEMALE,
    MALE,
    PERSON,
    WISH
}

fun List<Event>.sortEvents(
    sortType: EventSortType,
    wishes: List<WishlistView> = emptyList()
): List<Event> {
    return when (sortType) {
        EventSortType.DATE -> sortedByDescending { it.timestamp }
        EventSortType.NAME -> sortedBy { it.readableName }
        EventSortType.ID -> sortedBy { it.eventDbId }
        EventSortType.FEMALE -> sortedBy { it.femaleObsUnitDbId }
        EventSortType.MALE -> sortedBy { it.maleObsUnitDbId }
        EventSortType.PERSON -> sortedBy { it.person }
        EventSortType.WISH -> {
            val wishPairs = wishes.map { it.momId to it.dadId }.toSet()
            sortedWith(compareByDescending<Event> {
                (it.femaleObsUnitDbId to it.maleObsUnitDbId) in wishPairs
            }.thenByDescending { it.timestamp })
        }
    }
}
