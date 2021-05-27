package org.phenoapps.intercross.adapters

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.databinding.DataBindingUtil
import androidx.lifecycle.LifecycleOwner
import androidx.lifecycle.Observer
import androidx.navigation.Navigation
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView
import org.phenoapps.intercross.R
import org.phenoapps.intercross.data.models.Event
import org.phenoapps.intercross.data.viewmodels.EventListViewModel
import org.phenoapps.intercross.databinding.ListItemEventsBinding
import org.phenoapps.intercross.fragments.EventsFragmentDirections


class EventsAdapter(private val owner: LifecycleOwner, private val viewModel: EventListViewModel) : ListAdapter<Event, RecyclerView.ViewHolder>(Event.Companion.DiffCallback()) {

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): RecyclerView.ViewHolder {

        return EventViewHolder(DataBindingUtil.inflate(
                        LayoutInflater.from(parent.context),
                        R.layout.list_item_events, parent, false))
    }

    override fun onBindViewHolder(holder: RecyclerView.ViewHolder, position: Int) {

        getItem(position).let { event ->

            with(holder as EventViewHolder) {

                viewModel.parents.observe(owner, Observer { data ->

                    data?.let { parents ->

                        val actualParents = parents.filter { it.dad == event.maleObsUnitDbId && it.mom == event.femaleObsUnitDbId }

                        if (actualParents.isNotEmpty()) {

                            bind(event, actualParents[0].momReadable, actualParents[0].dadReadable)

                        }
                    }
                })
            }
        }
    }

    class EventViewHolder(private val binding: ListItemEventsBinding
    ) : RecyclerView.ViewHolder(binding.root) {

        fun bind(event: Event, mom: String, dad: String) {

            with(binding) {

                event.id?.let { key ->

                    this.event = event

                    this.male = dad

                    this.female = mom

                    if ("_" in event.timestamp) {

                        this.timestamp = event.timestamp.split("_")[0]

                    } else this.timestamp = event.timestamp

                    clickListener = View.OnClickListener {

                        it?.let {
                            Navigation.findNavController(binding.root)
                                    .navigate(EventsFragmentDirections
                                            .actionToEventFragment(key))
                        }
                    }
                }
            }
        }
    }
}