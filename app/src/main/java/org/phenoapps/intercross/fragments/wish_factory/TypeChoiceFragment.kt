package org.phenoapps.intercross.fragments.wish_factory

import androidx.appcompat.app.AppCompatActivity
import androidx.fragment.app.viewModels
import androidx.lifecycle.MediatorLiveData
import androidx.navigation.fragment.findNavController
import org.phenoapps.intercross.R
import org.phenoapps.intercross.activities.MainActivity
import org.phenoapps.intercross.adapters.WishValueAdapter
import org.phenoapps.intercross.adapters.WishValueItem
import org.phenoapps.intercross.data.MetadataRepository
import org.phenoapps.intercross.data.WishlistRepository
import org.phenoapps.intercross.data.models.Meta
import org.phenoapps.intercross.data.models.Wishlist
import org.phenoapps.intercross.data.viewmodels.MetadataViewModel
import org.phenoapps.intercross.data.viewmodels.WishlistViewModel
import org.phenoapps.intercross.data.viewmodels.factory.MetadataViewModelFactory
import org.phenoapps.intercross.data.viewmodels.factory.WishlistViewModelFactory
import org.phenoapps.intercross.databinding.FragmentWfChooseWishTypeBinding
import org.phenoapps.intercross.fragments.IntercrossBaseFragment

/**
 * @author Chaney 8/16/2021
 * Combined fragment for choosing wish types and values.
 * This screen displays all metadata types and the default cross type with min/max inputs.
 * Inputs: female and male ids and names.
 * Outputs: Inserts multiple Wishlist entries and navigates back.
 */
class TypeChoiceFragment : IntercrossBaseFragment<FragmentWfChooseWishTypeBinding>(R.layout.fragment_wf_choose_wish_type) {

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

    private val metadataViewModel: MetadataViewModel by viewModels {
        MetadataViewModelFactory(MetadataRepository.getInstance(db.metadataDao()))
    }

    private val wishStore: WishlistViewModel by viewModels {
        WishlistViewModelFactory(WishlistRepository.getInstance(db.wishlistDao()))
    }

    private val wishAdapter = WishValueAdapter()

    private var isDataLoaded = false
    private var metaReceived = false
    private var wishesReceived = false

    override fun FragmentWfChooseWishTypeBinding.afterCreateView() {

        (activity as? MainActivity)?.applyBottomInsets(root)

        mBinding.wfWishTypeSummaryTv.text = getString(R.string.frag_wf_choose_type_summary,
            femaleName, maleName)

        mBinding.wfWishTypeRv.adapter = wishAdapter

        val combinedData = MediatorLiveData<Pair<List<Meta>, List<Wishlist>>>()
        
        combinedData.addSource(metadataViewModel.metadata) { meta ->
            metaReceived = true
            val currentWishes = combinedData.value?.second ?: emptyList()
            combinedData.value = (meta ?: emptyList()) to currentWishes
        }
        
        combinedData.addSource(wishStore.getByParents(femaleId, maleId)) { wishes ->
            wishesReceived = true
            val currentMeta = combinedData.value?.first ?: emptyList()
            combinedData.value = currentMeta to (wishes ?: emptyList())
        }

        combinedData.observe(viewLifecycleOwner) { data ->
            if (isDataLoaded || !metaReceived || !wishesReceived) return@observe

            val metadata = data.first
            val existingWishes = data.second
            
            val items = mutableListOf<WishValueItem>()
            
            // Default "cross" wish
            val crossWish = existingWishes.find { it.wishType == getString(R.string.literal_cross) }
            if (crossWish == null) {
                items.add(WishValueItem(
                    type = getString(R.string.literal_cross)
                ))
            } else {
                items.add(WishValueItem(
                    type = crossWish.wishType,
                    min = crossWish.wishMin,
                    max = crossWish.wishMax
                ))
            }

            // Metadata wishes
            for (meta in metadata) {
                val wish = existingWishes.find { it.wishType == meta.property }
                items.add(WishValueItem(
                    type = meta.property,
                    min = wish?.wishMin ?: meta.defaultValue,
                    max = wish?.wishMax
                ))
            }
            
            wishAdapter.submitList(items)
            isDataLoaded = true
        }

        mBinding.wfWishTypeNextBt.setOnClickListener {
            val validWishes = wishAdapter.currentList.filter { 
                it.min != null && it.min!! > 0 && (it.max == null || it.max!! >= it.min!!) 
            }

            if (validWishes.isNotEmpty()) {
                val wishesToInsert = validWishes.map { item ->
                    Wishlist(femaleId, maleId, femaleName, maleName, item.type, item.min!!, item.max)
                }
                wishStore.insert(*wishesToInsert.toTypedArray())
            }
            
            findNavController().navigate(TypeChoiceFragmentDirections.actionFromTypesToWishlistBack())
        }
    }

    override fun onResume() {
        super.onResume()
        (activity as MainActivity).setBackButtonToolbar()
        (activity as AppCompatActivity).supportActionBar?.show()
    }
}