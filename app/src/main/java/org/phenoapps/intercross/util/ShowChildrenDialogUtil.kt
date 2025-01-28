package org.phenoapps.intercross.util

import android.content.Context
import android.view.LayoutInflater
import androidx.appcompat.app.AlertDialog
import androidx.lifecycle.LifecycleOwner
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import org.phenoapps.intercross.R
import org.phenoapps.intercross.adapters.EventsAdapter
import org.phenoapps.intercross.data.models.Event
import org.phenoapps.intercross.data.viewmodels.EventListViewModel
import org.phenoapps.intercross.interfaces.EventClickListener

class ShowChildrenDialogUtil(
    private val owner: LifecycleOwner,
    private val context: Context?,
    private val onMakeCrossClick: (male: String, female: String) -> Unit,
    private val eventsModel: EventListViewModel,
    private val eventClickListener: EventClickListener,
    private val isCommutativeCrossing: Boolean
) {
    private var dialog: AlertDialog? = null

    fun showChildren(events: List<Event>, male: String, female: String) {
        val children = events.filter { event ->
            if (isCommutativeCrossing) {
                (event.maleObsUnitDbId == male && event.femaleObsUnitDbId == female) ||
                        (event.maleObsUnitDbId == female && event.femaleObsUnitDbId == male)
            } else {
                event.femaleObsUnitDbId == female && event.maleObsUnitDbId == male
            }
        }

        context?.let {
            dialog = AlertDialog.Builder(it)
                .setTitle(
                    if (children.isNotEmpty()) it.getString(R.string.click_item_to_open_child) else it.getString(
                        R.string.no_child_exists
                    )
                )
                .setNegativeButton(R.string.cancel) { _, _ -> }
                .setNeutralButton(R.string.make_cross_option) { _, _ ->
                    onMakeCrossClick(male, female)
                }
                .create()

            val view = LayoutInflater.from(it).inflate(R.layout.dialog_cross_children, null)
            val recyclerView = view.findViewById<RecyclerView>(R.id.children_rv)
            recyclerView.layoutManager = LinearLayoutManager(it)

            val eventsAdapter = EventsAdapter(owner, eventsModel, eventClickListener)
            recyclerView.adapter = eventsAdapter
            eventsAdapter.submitList(children)

            dialog?.setView(view)
            dialog?.show()
        }
    }

    fun dismiss() {
        dialog?.dismiss()
        dialog = null
    }
}