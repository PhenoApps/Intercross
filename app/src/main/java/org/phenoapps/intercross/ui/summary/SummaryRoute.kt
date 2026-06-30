package org.phenoapps.intercross.ui.summary

import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.lifecycle.asFlow
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import org.phenoapps.intercross.data.EventsRepository
import org.phenoapps.intercross.data.viewmodels.EventListViewModel
import org.phenoapps.intercross.data.viewmodels.factory.EventsListViewModelFactory
import org.phenoapps.intercross.ui.app.TopBarState
import org.phenoapps.intercross.ui.app.BottomBarState
import org.phenoapps.intercross.ui.app.rememberDatabase
import org.phenoapps.intercross.R

@Composable
fun SummaryRoute(bottomBarState: BottomBarState? = null) {
    val db = rememberDatabase()
    val eventsModel: EventListViewModel = viewModel(factory = EventsListViewModelFactory(EventsRepository.getInstance(db.eventsDao())))
    val events by eventsModel.events.asFlow().collectAsStateWithLifecycle(initialValue = emptyList())
    val metadata by eventsModel.metadata.asFlow().collectAsStateWithLifecycle(initialValue = emptyList())
    val chartData = remember(events, metadata) { buildSummaryChartData(events, metadata) }
    SummaryScreen(
        chartData = chartData,
        topBarState = TopBarState(titleRes = R.string.summary_label),
        bottomBarState = bottomBarState,
    )
}
