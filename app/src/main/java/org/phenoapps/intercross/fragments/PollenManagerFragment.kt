package org.phenoapps.intercross.fragments

import androidx.appcompat.app.AlertDialog
import androidx.core.widget.addTextChangedListener
import androidx.fragment.app.viewModels
import androidx.lifecycle.Observer
import androidx.navigation.findNavController
import androidx.navigation.fragment.navArgs
import androidx.recyclerview.widget.LinearLayoutManager
import org.phenoapps.intercross.R
import org.phenoapps.intercross.activities.MainActivity
import org.phenoapps.intercross.adapters.ParentsAdapter
import org.phenoapps.intercross.data.EventsRepository
import org.phenoapps.intercross.data.ParentsRepository
import org.phenoapps.intercross.data.PollenGroupRepository
import org.phenoapps.intercross.data.models.Event
import org.phenoapps.intercross.data.models.Parent
import org.phenoapps.intercross.data.models.PollenGroup
import org.phenoapps.intercross.data.viewmodels.EventListViewModel
import org.phenoapps.intercross.data.viewmodels.ParentsListViewModel
import org.phenoapps.intercross.data.viewmodels.PollenGroupListViewModel
import org.phenoapps.intercross.data.viewmodels.factory.EventsListViewModelFactory
import org.phenoapps.intercross.data.viewmodels.factory.ParentsListViewModelFactory
import org.phenoapps.intercross.data.viewmodels.factory.PollenGroupListViewModelFactory
import org.phenoapps.intercross.databinding.FragmentPollenManagerBinding
import org.phenoapps.intercross.util.Dialogs
import java.util.*

class PollenManagerFragment : IntercrossBaseFragment<FragmentPollenManagerBinding>(R.layout.fragment_pollen_manager) {

    private val args: PollenManagerFragmentArgs by navArgs()

    private lateinit var mAdapter: ParentsAdapter

    private var mEvents: List<Event> = ArrayList()

    private var mParents: List<Parent> = ArrayList()

    private var mMales: List<Parent> = ArrayList()

    private var mGroups: List<PollenGroup> = ArrayList()

    private val eventList: EventListViewModel by viewModels {

        EventsListViewModelFactory(
                EventsRepository.getInstance(db.eventsDao()))
    }

    private val parentList: ParentsListViewModel by viewModels {

        ParentsListViewModelFactory(
                ParentsRepository.getInstance(db.parentsDao())
        )
    }

    private val groupList: PollenGroupListViewModel by viewModels {

        PollenGroupListViewModelFactory(
                PollenGroupRepository.getInstance(db.pollenGroupDao())
        )
    }

    override fun FragmentPollenManagerBinding.afterCreateView() {

        (activity as? MainActivity)?.applyBottomInsets(root)

        (activity as MainActivity).setBackButtonToolbar()
        (activity as MainActivity).supportActionBar?.apply {
            title = getString(R.string.frag_parents_new_parent_title)
            show()
        }

        parentList.updateSelection(0)

        groupList.updateSelection(0)

        /***
         * When the safe args = 0 we are creating females, otherwise we are creating males/groups
         */
        //an error is shown when a barcode already exists in the database
        val error = getString(R.string.ErrorCodeExists)

        parentList.parents.observe(viewLifecycleOwner, Observer { parents ->

            parents?.let {

                mParents = it

            }
        })

        if (args.mode == 1) {

            mAdapter = ParentsAdapter(parentList, groupList)

            parentList.males.observe(viewLifecycleOwner, Observer {

                it?.let { males ->

                    mMales = males

                    groupList.groups.observeForever { groups ->

                        groups?.let { gs ->

                            mGroups = gs
                            /**
                             * Transform polycrosses to simple parent object before submitting to parent adapter.
                             */
                            mAdapter.submitList(males.distinctBy { m -> m.codeId })

                            updateButtonText()
                        }
                    }
                }

                updateButtonText()
            })

            recyclerView.layoutManager = LinearLayoutManager(requireContext())

            recyclerView.adapter = mAdapter

            mode = args.mode

        }

        /**
         * Error check, ensure that the entered code does not exist in the events table.
         */
        eventList.events.observe(viewLifecycleOwner, Observer {

            it?.let {

                mEvents = it

                codeEditText.addTextChangedListener {

                    val codes = mEvents.map { event -> event.eventDbId } + mParents.map { parent -> parent.codeId }.distinct()

                    if (codeEditText.text.toString() in codes) {

                        if (codeTextHolder.error == null) codeTextHolder.error = error

                    } else codeTextHolder.error = null

                }
            }
        })

        codeEditText.setText(UUID.randomUUID().toString())

        newButton.setOnClickListener {

            val codeText = codeEditText.text.toString()

            if (codeText.isNotBlank()) {

                var readableName = nameEditText.text.toString()

                if (readableName.isBlank()) {

                    readableName = codeText

                }

                if (mParents.any { parent -> parent.codeId == codeEditText.text.toString() }) {

                    Dialogs.notify(AlertDialog.Builder(requireContext()), getString(R.string.parent_already_exists))

                } else when (args.mode) {
                    /**
                     * Insert a single female into the database based on the data entry.
                     */
                    0 -> {

                        parentList.insert(Parent(codeText, 0, readableName))

                        mBinding.root.findNavController().navigate(
                                PollenManagerFragmentDirections
                                        .actionReturnToParentsFragment(0))
                    }
                    /**
                     * Either enter a group if a list is selected, or a single male with the
                     * entered data
                     */
                    1 -> {
                        val addedMales = ArrayList<PollenGroup>()

                        for (p: Parent in mMales) {

                            if (p.selected) {

                                p.id?.let { id ->

                                    addedMales.add(buildGroup(id))
                                }
                            }
                        }

                        /***
                         * check if list has been created, otherwise insert a single male
                         */
                        if (addedMales.isEmpty()) {

                            parentList.insert(Parent(codeText, 1, readableName))

                        } else {

                            groupList.insert(*addedMales.toTypedArray())

                        }

                        mBinding.root.findNavController().navigate(
                                PollenManagerFragmentDirections
                                        .actionReturnToParentsFragment(1))
                    }
                }
            } else {

                Dialogs.notify(AlertDialog.Builder(requireContext()), getString(R.string.cross_id_cannot_be_blank))

            }
        }
    }

    private fun FragmentPollenManagerBinding.updateButtonText() {

        if (isAdded) {

            val act = requireActivity()

            newButton.text =
                when {
                    mMales.any { male -> male.selected } -> {

                        act.getString(R.string.add_male_group)

                    }
                    args.mode == 0 -> {

                        act.getString(R.string.add_female)

                    }
                    else -> act.getString(R.string.add_male)
                }
        }

    }

    /**
     * This function initializes and returns a PollenGroup object with the elements of the UI.
     */
    private fun FragmentPollenManagerBinding.buildGroup(id: Long) =
            PollenGroup(codeEditText.text.toString(),
                    nameEditText.text.toString(), id)


}
