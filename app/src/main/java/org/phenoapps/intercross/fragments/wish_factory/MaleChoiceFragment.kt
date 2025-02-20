package org.phenoapps.intercross.fragments.wish_factory

import androidx.appcompat.app.AppCompatActivity
import androidx.fragment.app.viewModels
import androidx.navigation.fragment.findNavController
import org.phenoapps.intercross.R
import org.phenoapps.intercross.activities.MainActivity
import org.phenoapps.intercross.adapters.SimpleListAdapter
import org.phenoapps.intercross.data.ParentsRepository
import org.phenoapps.intercross.data.PollenGroupRepository
import org.phenoapps.intercross.data.models.Parent
import org.phenoapps.intercross.data.viewmodels.ParentsListViewModel
import org.phenoapps.intercross.data.viewmodels.PollenGroupListViewModel
import org.phenoapps.intercross.data.viewmodels.factory.ParentsListViewModelFactory
import org.phenoapps.intercross.data.viewmodels.factory.PollenGroupListViewModelFactory
import org.phenoapps.intercross.databinding.FragmentWfChooseMaleBinding
import org.phenoapps.intercross.fragments.IntercrossBaseFragment
import org.phenoapps.intercross.interfaces.OnSimpleItemClicked

/**
 * @author Chaney 8/16/2021
 * Simple fragment for the user to choose a male name or "None"
 * This is the second fragment in the wish factory workflow graph
 * Inputs: id: Long, name: String defaults are -1L and "None" which means a self cross
 * Outputs: femaleId, femaleName, maleId: Long, maleName: String
 */
class MaleChoiceFragment : IntercrossBaseFragment<FragmentWfChooseMaleBinding>(R.layout.fragment_wf_choose_male),
    OnSimpleItemClicked {

    private val femaleName by lazy {
        arguments?.getString("name")
    }

    private val femaleId by lazy {
        arguments?.getString("id")
    }

    companion object {
        val EMPTY_PARENT = "-1" to "blank"
    }

    private val groupList: PollenGroupListViewModel by viewModels {
        PollenGroupListViewModelFactory(PollenGroupRepository.getInstance(db.pollenGroupDao()))
    }

    private val parentList: ParentsListViewModel by viewModels {
        ParentsListViewModelFactory(ParentsRepository.getInstance(db.parentsDao()))
    }

    override fun FragmentWfChooseMaleBinding.afterCreateView() {

        (activity as? MainActivity)?.applyBottomInsets(root)

        mBinding.wfMaleSummaryTv.text = getString(R.string.frag_wf_male_choice_summary, femaleName)

        mBinding.wfMaleRv.adapter = SimpleListAdapter(this@MaleChoiceFragment)

        parentList.males.observe(viewLifecycleOwner) { dads ->

            groupList.groups.observe(viewLifecycleOwner) { groups ->

                val groupPairs = groups.filter { it.name.isNotBlank() }
                    .map { it.codeId to it.name }
                val dadPairs = dads.filter { it.name.isNotBlank() }
                    .map { it.codeId to it.name }

                //only add empty parent if it was not chosen for the female
                (mBinding.wfMaleRv.adapter as SimpleListAdapter).submitList(
                    (listOf(EMPTY_PARENT) + dadPairs + groupPairs).distinctBy { it.first }
                )

                arguments?.let { args ->
                    with(args) {
                        if ("barcode" in keySet()) {
                            val femaleName = getString("name") ?: "?"
                            val femaleId = getString("id") ?: "-1"
                            getString("barcode")?.let { code ->
                                val dad = dads.find { it.codeId == code }
                                val group = groups.find { it.codeId == code }
                                val maleName = dad?.name
                                    ?: if (group != null) {
                                        group.name
                                    } else {
                                        parentList.insert(Parent(code, 1))
                                        code
                                    }
                                findNavController().navigate(MaleChoiceFragmentDirections
                                    .actionFromMalesToTypesFragment(femaleId, femaleName,
                                        code, maleName))
                            }
                        }
                    }
                }
            }
        }

        mBinding.wfChooseMaleBarcodeScanBtn.setOnClickListener {

            findNavController().navigate(MaleChoiceFragmentDirections
                .actionFromMalesToBarcodeScanFragment(femaleId ?: "-1", femaleName ?: "?"))

        }
    }

    private fun navigateToTypeChoiceFragment(pair: Pair<String, String>) {
        findNavController().navigate(MaleChoiceFragmentDirections
            .actionFromMalesToTypesFragment(femaleId ?: "-1", femaleName ?: "?",
                pair.first, pair.second))
    }

    override fun onItemClicked(pair: Pair<String, String>) {

        mBinding.wfMaleNextBt.setOnClickListener {
            navigateToTypeChoiceFragment(pair)
        }
    }

    override fun onResume() {
        super.onResume()
        (activity as MainActivity).setBackButtonToolbar()
        (activity as AppCompatActivity).supportActionBar?.show()
    }
}