package org.phenoapps.intercross.fragments.wish_factory

import androidx.appcompat.app.AppCompatActivity
import androidx.fragment.app.viewModels
import androidx.navigation.fragment.findNavController
import org.phenoapps.intercross.R
import org.phenoapps.intercross.activities.MainActivity
import org.phenoapps.intercross.adapters.SimpleListAdapter
import org.phenoapps.intercross.data.MetadataRepository
import org.phenoapps.intercross.data.viewmodels.MetadataViewModel
import org.phenoapps.intercross.data.viewmodels.factory.MetadataViewModelFactory
import org.phenoapps.intercross.databinding.FragmentWfChooseWishTypeBinding
import org.phenoapps.intercross.fragments.IntercrossBaseFragment
import org.phenoapps.intercross.interfaces.OnSimpleItemClicked

/**
 * @author Chaney 8/16/2021
 * Simple fragment for the user to choose a metadata type, or #crosses wish
 * This is the third fragment in the wish factory workflow graph
 * Inputs: female and male ids and names, one can be an empty parent for self cross
 * Outputs: female and male ids and names, the type name
 */
class TypeChoiceFragment : IntercrossBaseFragment<FragmentWfChooseWishTypeBinding>(R.layout.fragment_wf_choose_wish_type),
    OnSimpleItemClicked {

    private val femaleName by lazy {
        arguments?.getString("femaleName") ?: "?"
    }

    private val femaleId by lazy {
        arguments?.getString("femaleId") ?: "-1"
    }

    private val maleName by lazy {
        arguments?.getString("maleName") ?: "blank"
    }

    private val maleId by lazy {
        arguments?.getString("maleId") ?: "-1"
    }

    private val metadataModel: MetadataViewModel by viewModels {
        MetadataViewModelFactory(MetadataRepository.getInstance(db.metadataDao()))
    }

    val defaultWishType by lazy {
        "-1" to getString(R.string.literal_cross)
    }

    override fun FragmentWfChooseWishTypeBinding.afterCreateView() {

        (activity as? MainActivity)?.applyBottomInsets(root)

        mBinding.wfWishTypeSummaryTv.text = getString(R.string.frag_wf_choose_type_summary,
            femaleName, maleName)

        //initialize button click
        mBinding.wfWishTypeNextBt.setOnClickListener {
            onItemClicked(defaultWishType)
        }

        mBinding.wfWishTypeRv.adapter = SimpleListAdapter(this@TypeChoiceFragment)

        metadataModel.metadata.observe(viewLifecycleOwner) { metadata ->
            val headers = metadata.map { it.property }
            (mBinding.wfWishTypeRv.adapter as SimpleListAdapter).submitList(
                listOf(defaultWishType) + headers.mapIndexed { index, key ->
                    index.toString() to key
                }
            )
        }
    }

    //1. we can assume that the correct data for male/female has been passed
    //2. navigate to the values fragment
    override fun onItemClicked(pair: Pair<String, String>) {

        mBinding.wfWishTypeNextBt.setOnClickListener {
           findNavController().navigate(TypeChoiceFragmentDirections
               .actionFromTypesToValuesFragment(femaleId, femaleName, maleId, maleName, pair.second))
        }
    }

    override fun onResume() {
        super.onResume()
        (activity as MainActivity).setBackButtonToolbar()
        (activity as AppCompatActivity).supportActionBar?.show()
    }
}