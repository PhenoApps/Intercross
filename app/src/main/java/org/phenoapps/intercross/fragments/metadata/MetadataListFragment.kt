package org.phenoapps.intercross.fragments.metadata

import androidx.appcompat.app.AlertDialog
import androidx.fragment.app.viewModels
import androidx.navigation.fragment.findNavController
import androidx.recyclerview.widget.LinearLayoutManager
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.MainScope
import kotlinx.coroutines.launch
import org.phenoapps.intercross.R
import org.phenoapps.intercross.activities.MainActivity
import org.phenoapps.intercross.adapters.SimpleListAdapter
import org.phenoapps.intercross.data.IntercrossDatabase
import org.phenoapps.intercross.data.MetaValuesRepository
import org.phenoapps.intercross.data.MetadataRepository
import org.phenoapps.intercross.data.models.Meta
import org.phenoapps.intercross.data.viewmodels.MetaValuesViewModel
import org.phenoapps.intercross.data.viewmodels.MetadataViewModel
import org.phenoapps.intercross.data.viewmodels.factory.MetaValuesViewModelFactory
import org.phenoapps.intercross.data.viewmodels.factory.MetadataViewModelFactory
import org.phenoapps.intercross.databinding.FragmentMetadataListBinding
import org.phenoapps.intercross.fragments.IntercrossBaseFragment
import org.phenoapps.intercross.interfaces.OnSimpleItemClicked
import org.phenoapps.intercross.util.Dialogs

class MetadataListFragment: IntercrossBaseFragment<FragmentMetadataListBinding>(R.layout.fragment_metadata_list),
    OnSimpleItemClicked, CoroutineScope by MainScope() {

    private val metaValuesViewModel: MetaValuesViewModel by viewModels {
        MetaValuesViewModelFactory(
            MetaValuesRepository
            .getInstance(IntercrossDatabase.getInstance(requireContext()).metaValuesDao()))
    }

    private val metadataViewModel: MetadataViewModel by viewModels {
        MetadataViewModelFactory(
            MetadataRepository
            .getInstance(IntercrossDatabase.getInstance(requireContext()).metadataDao()))
    }

    override fun FragmentMetadataListBinding.afterCreateView() {

        (activity as? MainActivity)?.applyBottomInsets(root)

        setupToolbar()

        fragMetadataListRv.layoutManager = LinearLayoutManager(context)
        fragMetadataListRv.adapter = SimpleListAdapter(this@MetadataListFragment)

        metadataViewModel.metadata.observe(viewLifecycleOwner) { metadata ->
            metaValuesViewModel.metaValues.observe(viewLifecycleOwner) { values ->

                (fragMetadataListRv.adapter as? SimpleListAdapter)?.submitList(
                    metadata.mapIndexed { _, model ->
                        (model.id?.toString() ?: "") to model.property
                    }
                )
            }
        }

        fragMetadataNewBtn.setOnClickListener {

            findNavController().navigate(MetadataListFragmentDirections
                .actionFromMetadataListToForm())
        }
    }

    private fun setupToolbar() {
        (activity as MainActivity).setBackButtonToolbar()
        (activity as MainActivity).supportActionBar?.apply {
            title = getString(R.string.frag_manage_metadata_title)
            show()
        }
    }

    override fun onItemClicked(pair: Pair<String, String>) {
        findNavController().navigate(MetadataListFragmentDirections
            .actionFromMetadataListToForm(pair.second))
    }

    override fun onItemLongClicked(pair: Pair<String, String>) {
        super.onItemLongClicked(pair)

        context?.let { ctx ->

            Dialogs.onOk(
                AlertDialog.Builder(ctx),
                getString(R.string.frag_metadata_list_delete_dialog_title),
                getString(android.R.string.cancel),
                getString(android.R.string.ok),
                getString(R.string.frag_metadata_confirm_delete_message)) {

                launch {

                    metadataViewModel.delete(Meta(pair.second, id = pair.first.toLong()))

                }
            }
        }
    }
}