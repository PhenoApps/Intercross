package org.phenoapps.intercross.data.viewmodels

import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.launch
import org.phenoapps.intercross.data.EventsRepository
import org.phenoapps.intercross.data.models.Event

/**
 * This view model handles loading the events from the DB asynchronously,
 *  and transforms the model into event detail view models for serving to the UI.
 *
 *  This list is potentially null, and should be observed during onCreate.
 */
class EventListViewModel(private val eventRepo: EventsRepository): BaseViewModel<Event>(eventRepo) {

    fun deleteById(eid: Long) {

        eventRepo.deleteById(eid)

    }

    fun deleteAll() {

        viewModelScope.launch {

            eventRepo.drop()

        }
    }

    fun getRowid(e: Event): Long = eventRepo.getRowid(e)

    fun insert(item: Event): Long = eventRepo.insert(item)

    val parents = eventRepo.getParentCount()

    val allParents = eventRepo.getAllParents()

    val events = eventRepo.selectAll()

    val metadata = eventRepo.getMetadata()
}
