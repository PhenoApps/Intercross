package org.phenoapps.intercross.fragments

import androidx.appcompat.app.AlertDialog
import androidx.fragment.app.viewModels
import androidx.lifecycle.Observer
import androidx.navigation.findNavController
import androidx.navigation.fragment.navArgs
import androidx.recyclerview.widget.LinearLayoutManager
import org.phenoapps.intercross.R
import org.phenoapps.intercross.activities.MainActivity
import org.phenoapps.intercross.adapters.ParentsAdapter
import org.phenoapps.intercross.data.ParentsRepository
import org.phenoapps.intercross.data.PollenGroupRepository
import org.phenoapps.intercross.data.models.Parent
import org.phenoapps.intercross.data.models.PollenGroup
import org.phenoapps.intercross.data.viewmodels.ParentsListViewModel
import org.phenoapps.intercross.data.viewmodels.PollenGroupListViewModel
import org.phenoapps.intercross.data.viewmodels.factory.ParentsListViewModelFactory
import org.phenoapps.intercross.data.viewmodels.factory.PollenGroupListViewModelFactory
import org.phenoapps.intercross.databinding.FragmentPollenManagerBinding
import org.phenoapps.intercross.util.Dialogs

class PollenManagerFragment : IntercrossBaseFragment<FragmentPollenManagerBinding>(R.layout.fragment_pollen_manager) {

    private val args: PollenManagerFragmentArgs by navArgs()

    private lateinit var mAdapter: ParentsAdapter

    private var mMales: List<Parent> = ArrayList()

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

        mAdapter = ParentsAdapter(parentList, groupList)

        parentList.males.observe(viewLifecycleOwner, Observer {

            it?.let { males ->

                mMales = males

                groupList.groups.observeForever { groups ->

                    groups?.let { _ ->

                        /**
                         * Transform poly crosses to simple parent object before submitting to parent adapter.
                         */
                        mAdapter.submitList(males.distinctBy { m -> m.codeId })

                    }
                }
            }
        })

        recyclerView.layoutManager = LinearLayoutManager(requireContext())

        recyclerView.adapter = mAdapter

        newButton.setOnClickListener {

            val codeText = args.codeId

            if (codeText.isNotBlank()) {

                var readableName = args.readableName

                if (readableName.isBlank()) {

                    readableName = codeText

                }

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

            } else {

                Dialogs.notify(AlertDialog.Builder(requireContext()), getString(R.string.cross_id_cannot_be_blank))

            }
        }
    }

    /**
     * This function initializes and returns a PollenGroup object with the elements of the UI.
     */
    private fun buildGroup(id: Long) =
            PollenGroup(args.codeId, args.readableName, id)

}
