package org.phenoapps.intercross.fragments

import android.os.Bundle
import android.view.Menu
import android.view.MenuInflater
import android.view.MenuItem
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import androidx.compose.runtime.getValue
import androidx.compose.runtime.livedata.observeAsState
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.core.os.bundleOf
import androidx.fragment.app.viewModels
import androidx.lifecycle.lifecycleScope
import androidx.navigation.fragment.findNavController
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import org.phenoapps.intercross.R
import org.phenoapps.intercross.activities.MainActivity
import org.phenoapps.intercross.data.EventsRepository
import org.phenoapps.intercross.data.models.Event
import org.phenoapps.intercross.data.viewmodels.EventListViewModel
import org.phenoapps.intercross.data.viewmodels.factory.EventsListViewModelFactory
import org.phenoapps.intercross.databinding.FragmentArchivedEventsBinding
import org.phenoapps.intercross.ui.lists.EventsList
import org.phenoapps.intercross.ui.theme.AppTheme
import org.phenoapps.intercross.util.SnackbarQueue

@AndroidEntryPoint
class ArchivedEventsFragment :
    IntercrossBaseFragment<FragmentArchivedEventsBinding>(R.layout.fragment_archived_events) {

    private val viewModel: EventListViewModel by viewModels {
        EventsListViewModelFactory(EventsRepository.getInstance(db.eventsDao()))
    }

    private var selectedEventIds by mutableStateOf<Set<Long>>(emptySet())

    private var archivedEvents: List<Event> = emptyList()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        setHasOptionsMenu(true)
    }

    override fun FragmentArchivedEventsBinding.afterCreateView() {

        (activity as? MainActivity)?.applyBottomInsets(root)
        (activity as MainActivity).setBackButtonToolbar()

        (activity as AppCompatActivity).supportActionBar?.apply {
            title = getString(R.string.archived_crosses)
            show()
        }

        setupComposeArchivedEventsList()

        viewModel.archivedEvents.observe(viewLifecycleOwner) {
            archivedEvents = it
        }
    }

    private fun FragmentArchivedEventsBinding.setupComposeArchivedEventsList() {
        composeArchivedEventsList.setContent {
            AppTheme {
                val events by viewModel.archivedEvents.observeAsState(emptyList())

                EventsList(
                    events = events,
                    onEventClick = { eventId ->
                        findNavController().navigate(
                            R.id.event_fragment,
                            bundleOf("eid" to eventId)
                        )
                    },
                    selectedEventIds = selectedEventIds,
                    swipesEnabled = selectedEventIds.isEmpty(),
                    enableSwipeStartToEnd = true,
                    enableSwipeEndToStart = true,
                    startToEndIconRes = R.drawable.ic_unarchive,
                    endToStartIconRes = R.drawable.ic_delete,
                    startToEndContentDescription = getString(R.string.unarchive_event),
                    endToStartContentDescription = getString(R.string.delete_event),
                    emptyText = getString(R.string.no_archived_crosses),
                    onSwipeStartToEnd = { event ->
                        unarchiveEvent(event)
                    },
                    onSwipeEndToStart = { event ->
                        deleteEvent(event)
                    },
                    onEventLongPress = { event ->
                        toggleEventSelection(event)
                    },
                    onSelectionToggle = { event ->
                        toggleEventSelection(event)
                    }
                )
            }
        }
    }

    private fun unarchiveEvent(event: Event) {
        event.id?.let { eventId ->
            viewModel.unarchiveById(eventId)
            mSnackbar.push(
                SnackbarQueue.SnackJob(
                    mBinding.root,
                    getString(R.string.snackbar_unarchived_cross, event.eventDbId)
                )
            )
        }
    }

    private fun deleteEvent(event: Event) {
        event.id?.let { eventId ->
            viewModel.deleteById(eventId)
            mSnackbar.push(
                SnackbarQueue.SnackJob(
                    mBinding.root,
                    getString(R.string.snackbar_deleted_cross, event.eventDbId),
                    getString(R.string.undo)
                ) {
                    restoreDeletedEvents(listOf(event))
                }
            )
        }
    }

    private fun toggleEventSelection(event: Event) {
        val eventId = event.id ?: return

        selectedEventIds = if (eventId in selectedEventIds) {
            selectedEventIds - eventId
        } else {
            selectedEventIds + eventId
        }

        activity?.invalidateOptionsMenu()
    }

    override fun onCreateOptionsMenu(menu: Menu, inflater: MenuInflater) {
        inflater.inflate(R.menu.archived_crosses_toolbar, menu)

        super.onCreateOptionsMenu(menu, inflater)
    }

    override fun onPrepareOptionsMenu(menu: Menu) {
        menu.findItem(R.id.action_unarchive)?.isVisible = selectedEventIds.isNotEmpty()
        menu.findItem(R.id.action_delete)?.isVisible = selectedEventIds.isNotEmpty()

        super.onPrepareOptionsMenu(menu)
    }

    override fun onOptionsItemSelected(item: MenuItem): Boolean {
        return when (item.itemId) {
            R.id.action_unarchive -> {
                showUnarchiveSelectedDialog()
                true
            }
            R.id.action_delete -> {
                showDeleteSelectedDialog()
                true
            }
            else -> super.onOptionsItemSelected(item)
        }
    }

    private fun showUnarchiveSelectedDialog() {
        AlertDialog.Builder(requireContext())
            .setTitle(R.string.unarchive_selected_crosses_title)
            .setNegativeButton(R.string.cancel) { dialog, _ -> dialog.dismiss() }
            .setPositiveButton(android.R.string.ok) { _, _ ->
                val count = selectedEventIds.size
                viewModel.unarchiveByIds(selectedEventIds.toList())
                clearSelection()
                mSnackbar.push(
                    SnackbarQueue.SnackJob(
                        mBinding.root,
                        getString(R.string.snackbar_unarchived_cross, count.toString())
                    )
                )
            }
            .show()
    }

    private fun showDeleteSelectedDialog() {
        AlertDialog.Builder(requireContext())
            .setTitle(R.string.delete_cross_entry_title)
            .setNegativeButton(R.string.cancel) { dialog, _ -> dialog.dismiss() }
            .setPositiveButton(android.R.string.ok) { _, _ ->
                val count = selectedEventIds.size
                val deletedEvents = archivedEvents.filter { it.id in selectedEventIds }
                viewModel.deleteByIds(selectedEventIds.toList())
                clearSelection()
                mSnackbar.push(
                    SnackbarQueue.SnackJob(
                        mBinding.root,
                        getString(R.string.snackbar_deleted_cross, count.toString()),
                        getString(R.string.undo)
                    ) {
                        restoreDeletedEvents(deletedEvents)
                    }
                )
            }
            .show()
    }

    private fun restoreDeletedEvents(events: List<Event>) {
        viewLifecycleOwner.lifecycleScope.launch(Dispatchers.IO) {
            events.forEach { event ->
                viewModel.insert(event.copy(isArchived = true))
            }
        }
    }

    private fun clearSelection() {
        selectedEventIds = emptySet()
        activity?.invalidateOptionsMenu()
    }
}
