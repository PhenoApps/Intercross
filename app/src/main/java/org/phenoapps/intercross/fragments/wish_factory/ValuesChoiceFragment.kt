package org.phenoapps.intercross.fragments.wish_factory

import android.view.WindowManager
import androidx.appcompat.app.AppCompatActivity
import androidx.navigation.fragment.findNavController
import org.phenoapps.intercross.R
import org.phenoapps.intercross.activities.MainActivity
import org.phenoapps.intercross.databinding.FragmentWfChooseWishValuesBinding
import org.phenoapps.intercross.fragments.IntercrossBaseFragment
import org.phenoapps.intercross.util.SnackbarQueue

/**
 * @author Chaney 8/16/2021
 * Simple fragment for the user to choose a metadata min/max value
 * This is the fourth fragment in the wish factory workflow graph
 * Inputs: female and male ids and names, the metadata property name or "cross"
 * Outputs: female and male ids and names, the type name and min/max values
 */
class ValuesChoiceFragment : IntercrossBaseFragment<FragmentWfChooseWishValuesBinding>(R.layout.fragment_wf_choose_wish_values) {

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

    private val wishType by lazy {
        arguments?.getString("typeName") ?: "cross"
    }

    override fun FragmentWfChooseWishValuesBinding.afterCreateView() {

        activity?.window?.setSoftInputMode(WindowManager.LayoutParams.SOFT_INPUT_ADJUST_RESIZE)

        mBinding.wfWishValuesSummaryTv.text = getString(R.string.frag_wf_values_summary,
            wishType, femaleName, maleName)

        //check if the entered values are valid
        mBinding.wfWishValuesNextBt.setOnClickListener {
            val min = mBinding.wfWishValuesMinEt.text.toString().toIntOrNull()
            val max = mBinding.wfWishValuesMaxEt.text.toString().toIntOrNull()
            if (min != null && min > 0) {
                if (max != null && max > min) {
                    findNavController().navigate(ValuesChoiceFragmentDirections
                        .actionFromValuesToSummaryFragment(femaleId, femaleName,
                            maleId, maleName, wishType, min, max))
                } else mSnackbar.push(SnackbarQueue.SnackJob(mBinding.root,
                    getString(R.string.frag_wf_values_max_must_not_be_null)))
            } else mSnackbar.push(SnackbarQueue.SnackJob(mBinding.root,
                getString(R.string.frag_wf_values_min_must_not_be_null)))

        }
    }

    override fun onResume() {
        super.onResume()
        (activity as MainActivity).setBackButtonToolbar()
        (activity as AppCompatActivity).supportActionBar?.show()
    }
}