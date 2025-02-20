package org.phenoapps.intercross.fragments.wish_factory

import androidx.appcompat.app.AppCompatActivity
import androidx.fragment.app.viewModels
import androidx.navigation.fragment.findNavController
import org.phenoapps.intercross.R
import org.phenoapps.intercross.activities.MainActivity
import org.phenoapps.intercross.data.WishlistRepository
import org.phenoapps.intercross.data.models.Wishlist
import org.phenoapps.intercross.data.viewmodels.WishlistViewModel
import org.phenoapps.intercross.data.viewmodels.factory.WishlistViewModelFactory
import org.phenoapps.intercross.databinding.FragmentWfSummaryBinding
import org.phenoapps.intercross.fragments.IntercrossBaseFragment

/**
 * @author Chaney 8/16/2021
 * Simple fragment for the user to review the wish factory process.
 * This is the final fragment in the wish factory workflow graph
 * Inputs: female and male ids and names, the metadata property name or "cross", min and max values
 * Outputs: Nothing
 */
class SummaryFragment : IntercrossBaseFragment<FragmentWfSummaryBinding>(R.layout.fragment_wf_summary) {

    private val wishStore: WishlistViewModel by viewModels {
        WishlistViewModelFactory(WishlistRepository.getInstance(db.wishlistDao()))
    }

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

    private val min by lazy {
        arguments?.getInt("min")
    }

    private val max by lazy {
        arguments?.getInt("max")
    }

    override fun FragmentWfSummaryBinding.afterCreateView() {

        (activity as? MainActivity)?.applyBottomInsets(root)

        mBinding.wfSummaryTv.text = getString(R.string.frag_wf_summary_summary,
            min, max, wishType, femaleName, maleName)

        mBinding.wfWishSummaryNextBt.setOnClickListener {
            wishStore.insert(Wishlist(
                arrayOf(femaleId,
                    maleId,
                    femaleName,
                    maleName,
                    wishType,
                    min.toString(),
                    max.toString())))

            findNavController().navigate(SummaryFragmentDirections
                .actionFromWfToWishlistBack())
        }
    }

    override fun onResume() {
        super.onResume()
        (activity as MainActivity).setBackButtonToolbar()
        (activity as AppCompatActivity).supportActionBar?.show()
    }
}