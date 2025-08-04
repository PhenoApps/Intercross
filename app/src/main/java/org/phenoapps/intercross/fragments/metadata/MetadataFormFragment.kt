package org.phenoapps.intercross.fragments.metadata

import androidx.fragment.app.viewModels
import androidx.navigation.fragment.findNavController
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.MainScope
import kotlinx.coroutines.launch
import org.phenoapps.intercross.R
import org.phenoapps.intercross.activities.MainActivity
import org.phenoapps.intercross.data.IntercrossDatabase
import org.phenoapps.intercross.data.MetaValuesRepository
import org.phenoapps.intercross.data.MetadataRepository
import org.phenoapps.intercross.data.models.Meta
import org.phenoapps.intercross.data.viewmodels.MetaValuesViewModel
import org.phenoapps.intercross.data.viewmodels.MetadataViewModel
import org.phenoapps.intercross.data.viewmodels.factory.MetaValuesViewModelFactory
import org.phenoapps.intercross.data.viewmodels.factory.MetadataViewModelFactory
import org.phenoapps.intercross.databinding.FragmentMetadataFormBinding
import org.phenoapps.intercross.fragments.IntercrossBaseFragment
import org.phenoapps.intercross.util.observeOnce

class MetadataFormFragment: IntercrossBaseFragment<FragmentMetadataFormBinding>(R.layout.fragment_metadata_form),
    CoroutineScope by MainScope() {

    private val argPropertyName by lazy {
        arguments?.getString("property") ?: ""
    }

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

    private var mMeta: Meta? = null
    private var mMetaList: List<Meta>? = null

    override fun FragmentMetadataFormBinding.afterCreateView() {

        setupToolbar()

        fragMetadataNameEt.setText(argPropertyName)

        metadataViewModel.metadata.observeOnce(viewLifecycleOwner) { metadata ->

            mMetaList = metadata

            metaValuesViewModel.metaValues.observeOnce(viewLifecycleOwner) { values ->

                metadata.find { it.property == argPropertyName }?.let { meta ->

                    mMeta = meta

                    fragMetadataDefaultValueEt.setText(meta.defaultValue?.toString())

                }
            }
        }

        fragMetadataSubmitBtn.setOnClickListener {

            val name = fragMetadataNameEt.text.toString()
            var default: Int? = fragMetadataDefaultValueEt.text?.toString()?.toIntOrNull()

            if (argPropertyName.isBlank()) {

                launch(Dispatchers.IO) {

                    metadataViewModel.insert(Meta(property = name, defaultValue = default))

                }

            } else {

                mMeta?.let { meta ->

                    launch {

                        metadataViewModel.update(meta.apply {
                            this.property = name
                            this.defaultValue = default
                        })

                    }
                }
            }

            findNavController().popBackStack()
        }
    }

    private fun setupToolbar() {
        (activity as MainActivity).setBackButtonToolbar()
        (activity as MainActivity).supportActionBar?.apply {
            title = getString(
                if (argPropertyName.isBlank()) R.string.frag_new_metadata_title
                else R.string.frag_edit_metadata_title
            )
            show()
        }
    }
}