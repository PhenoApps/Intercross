package org.phenoapps.intercross.fragments

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.Fragment
import androidx.lifecycle.Observer
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.ViewModelProviders
import androidx.recyclerview.widget.LinearLayoutManager
import org.phenoapps.intercross.adapters.SummaryAdapter
import org.phenoapps.intercross.data.Events
import org.phenoapps.intercross.data.EventsRepository
import org.phenoapps.intercross.data.IntercrossDatabase
import org.phenoapps.intercross.databinding.FragmentSummaryBinding
import org.phenoapps.intercross.viewmodels.EventsListViewModel


class SummaryFragment : Fragment() {

    private lateinit var mEventsListViewModel: EventsListViewModel

    private lateinit var mBinding: FragmentSummaryBinding

    private lateinit var mAdapter: SummaryAdapter

    data class SummaryData(var m: Events?, var f: Events?, var event: Events, var count: Int)

    override fun onCreateView(
            inflater: LayoutInflater,
            container: ViewGroup?,
            savedInstanceState: Bundle?
    ): View? {

        mBinding = FragmentSummaryBinding
                .inflate(inflater, container, false)

        val db = IntercrossDatabase.getInstance(requireContext())

        mEventsListViewModel = ViewModelProviders.of(this,
                object : ViewModelProvider.NewInstanceFactory() {

                    override fun <T : ViewModel?> create(modelClass: Class<T>): T {
                        @Suppress("UNCHECKED_CAST")
                        return EventsListViewModel(
                                EventsRepository.getInstance(db.eventsDao())) as T

                    }
                }
        ).get(EventsListViewModel::class.java)

        mBinding.recyclerView.layoutManager = LinearLayoutManager(requireContext())

        mAdapter = SummaryAdapter(requireContext())

        mBinding.recyclerView.adapter = mAdapter

        mEventsListViewModel.events.observe(viewLifecycleOwner, Observer { events ->
            events.let {
                var summaryList = ArrayList<SummaryData>()

                events.forEach { x ->

                    var f: Events? = null
                    var m: Events? = null
                    var count = 0

                    events.forEach { y ->

                        if (x.maleOBsUnitDbId == y.maleOBsUnitDbId
                                && x.femaleObsUnitDbId == y.femaleObsUnitDbId) {
                            count++
                        }

                        if (x.maleOBsUnitDbId == y.eventDbId) m = y
                        if (x.femaleObsUnitDbId == y.eventDbId) f = y
                    }

                    summaryList.add(SummaryData(m, f, x, count))
                }

                mAdapter.submitList(
                        summaryList.distinctBy { "${it.event.femaleObsUnitDbId}/${it.event.maleOBsUnitDbId}" }
                )
            }
        })

        return mBinding.root
    }
}