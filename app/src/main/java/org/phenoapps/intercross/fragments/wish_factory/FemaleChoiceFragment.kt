package org.phenoapps.intercross.fragments.wish_factory

import androidx.appcompat.app.AppCompatActivity
import androidx.fragment.app.viewModels
import androidx.navigation.fragment.findNavController
import org.phenoapps.intercross.R
import org.phenoapps.intercross.activities.MainActivity
import org.phenoapps.intercross.adapters.SimpleListAdapter
import org.phenoapps.intercross.data.ParentsRepository
import org.phenoapps.intercross.data.models.Parent
import org.phenoapps.intercross.data.viewmodels.ParentsListViewModel
import org.phenoapps.intercross.data.viewmodels.factory.ParentsListViewModelFactory
import org.phenoapps.intercross.databinding.FragmentWfChooseFemaleBinding
import org.phenoapps.intercross.fragments.IntercrossBaseFragment
import org.phenoapps.intercross.interfaces.OnSimpleItemClicked
import org.phenoapps.intercross.util.SnackbarQueue
import org.phenoapps.intercross.util.observeOnce

/**
 * @author Chaney 8/16/2021
 * Simple fragment for the user to choose a female name
 * This is the first fragment in the wish factory workflow graph
 * This fragment is initiated by clicking on the plus icon in the parents fragment.
 * When onNext is activated (next button is clicked) one option must be chosen.
 * If nothing is chosen, None is assumed and a notification should be displayed to the user.
 */
class FemaleChoiceFragment : IntercrossBaseFragment<FragmentWfChooseFemaleBinding>(R.layout.fragment_wf_choose_female),
    OnSimpleItemClicked {

    private val parentList: ParentsListViewModel by viewModels {
        ParentsListViewModelFactory(ParentsRepository.getInstance(db.parentsDao()))
    }

    override fun FragmentWfChooseFemaleBinding.afterCreateView() {

        mBinding.wfFemaleNextBt.setOnClickListener {
            mSnackbar.push(SnackbarQueue.SnackJob(mBinding.root,
                getString(R.string.frag_wf_a_female_must_be_chosen)))
        }

        mBinding.wfFemaleRv.adapter = SimpleListAdapter(this@FemaleChoiceFragment)

        parentList.females.observeOnce(viewLifecycleOwner) { moms ->

            (mBinding.wfFemaleRv.adapter as? SimpleListAdapter)?.submitList(moms
                .filter { it.name.isNotBlank() }
                .map { it.codeId to it.name }
                .distinctBy { it.first })

            arguments?.let { args ->
                with(args) {
                    if ("barcode" in keySet()) {
                        getString("barcode")?.let { code ->
                            val mom = moms.find { it.codeId == code }
                            val femaleName = if (mom != null) {
                                mom.name
                            } else {
                                parentList.insert(Parent(code, 0))
                                code
                            }
                            findNavController().navigate(FemaleChoiceFragmentDirections
                                .actionFromFemalesToMalesFragment(code, femaleName))
                        }
                    }
                }
            }
        }

        mBinding.wfChooseFemaleBarcodeScanBtn.setOnClickListener {

            findNavController().navigate(FemaleChoiceFragmentDirections
                .actionFromFemalesToBarcodeScanFragment())

        }
    }

    override fun onResume() {
        super.onResume()
        (activity as MainActivity).setBackButtonToolbar()
        (activity as AppCompatActivity).supportActionBar?.apply {
            title = getString(R.string.frag_wf_toolbar_title)
            show()
        }
    }

    override fun onItemClicked(pair: Pair<String, String>) {

        mBinding.wfFemaleNextBt.setOnClickListener {
            findNavController().navigate(FemaleChoiceFragmentDirections
                .actionFromFemalesToMalesFragment(pair.first, pair.second))
        }
    }
}