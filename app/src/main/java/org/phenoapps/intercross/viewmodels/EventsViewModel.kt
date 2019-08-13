package org.phenoapps.intercross.viewmodels

import androidx.databinding.ObservableField
import androidx.lifecycle.ViewModel
import org.phenoapps.intercross.data.Events

class EventsViewModel(event: Events) : ViewModel() {
    val name = ObservableField<String>(event.eventDbId)
    val date = ObservableField<String>(event.date)
    val male = ObservableField<String>(event.maleOBsUnitDbId)
    val female = ObservableField<String>(event.femaleObsUnitDbId)
    val count = ObservableField<Int>(event.eid)
}