package org.phenoapps.intercross.fragments.brapi

import android.util.Log
import com.google.gson.Gson
import com.google.gson.GsonBuilder
import com.google.gson.JsonParser
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import org.brapi.v2.model.germ.BrAPICrossParent
import org.phenoapps.intercross.R
import org.phenoapps.intercross.data.ParentsRepository
import org.phenoapps.intercross.data.models.Parent
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ArrayAdapter
import android.widget.Toast
import androidx.databinding.DataBindingUtil
import androidx.lifecycle.lifecycleScope
import androidx.navigation.fragment.findNavController
import androidx.navigation.fragment.navArgs
import org.phenoapps.intercross.databinding.FragmentBrapiPotentialParentsBinding
import org.phenoapps.intercross.databinding.ListItemPotentialParentBinding
import org.phenoapps.intercross.fragments.IntercrossBaseFragment
import androidx.appcompat.widget.Toolbar
import org.phenoapps.intercross.activities.MainActivity
import org.phenoapps.intercross.brapi.service.BrAPIServiceV2

class BrapiPotentialParentsFragment :
    IntercrossBaseFragment<FragmentBrapiPotentialParentsBinding>(R.layout.fragment_brapi_potential_parents) {

    companion object {
        const val TAG = "BrAPI Parents Fragment"
    }

    private val args: BrapiPotentialParentsFragmentArgs by navArgs()

    private val parentsRepository by lazy {
        ParentsRepository.getInstance(db.parentsDao())
    }

    private val mService: BrAPIServiceV2 by lazy {

        BrAPIServiceV2(this@BrapiPotentialParentsFragment.context)

    }

    private val gson by lazy { Gson() }
    private val prettyGson by lazy { GsonBuilder().setPrettyPrinting().create() }

    private val potentialParents by lazy {
        (args.potentialParentsJson ?: emptyArray()).mapNotNull { encoded ->
            runCatching { gson.fromJson(encoded, BrAPICrossParent::class.java) }.getOrNull()
        }.distinctBy { it.observationUnitDbId ?: it.observationUnitName }
    }

    private val expandedParentIds = mutableSetOf<String>()

    override fun FragmentBrapiPotentialParentsBinding.afterCreateView() {
        emptyContainer.visibility = View.GONE

        (activity as? MainActivity)?.applyFragmentInsets(root, toolbar as? Toolbar)

        toolbar.title = getString(R.string.brapi_potential_parents_title)
        toolbar.setNavigationOnClickListener {
            findNavController().popBackStack()
        }

        bindProjectSummary()

        //if (potentialParents.isEmpty()){
            //loading obs. units and germplasm
//            lifecycleScope.launch {
//
//                val units = withContext(Dispatchers.IO) {
//                    mService.observationUnitsApi.getAllObservationUnits(
//                        programDbId = args.programDbId,
//                        pageSize = 1,
//                        maxParallel = 3
//                    ) { completed, total ->
//                        println("Progress: $completed / $total pages")
//                    }
//                }
//
//                Log.d(TAG, "BrAPI returned ${units.size} observation units.")
//
//                val items = units.map { unit ->
//                    BrAPICrossParent().apply {
//                        observationUnitDbId = unit.observationUnitDbId
//                        observationUnitName = unit.observationUnitName
//                    }
//                }
//
//                bindPotentialParents(items)
//            }

//        } else {
            bindPotentialParents(potentialParents)
//        }


        importButton.setOnClickListener {
            importPotentialParents()
        }
    }

    private fun bindProjectSummary() {
        mBinding.projectNameTextView.text = args.crossingProjectName
        mBinding.projectDescriptionTextView.text =
            args.crossingProjectDescription.ifBlank {
                getString(R.string.brapi_project_description_unavailable)
            }
        mBinding.programValueTextView.text =
            args.programName.ifBlank { getString(R.string.brapi_project_value_unavailable) }
        mBinding.cropValueTextView.text =
            args.commonCropName.ifBlank { getString(R.string.brapi_project_value_unavailable) }
        mBinding.projectIdChip.text =
            args.crossingProjectDbId.ifBlank { getString(R.string.brapi_project_id_unavailable) }
        mBinding.parentCountChip.text =
            resources.getQuantityString(
                R.plurals.brapi_parent_count,
                args.parentCount,
                args.parentCount
            )
    }

    private fun bindPotentialParents(items: List<BrAPICrossParent>) {
        val context = context ?: return
        mBinding.plannedCrossCountTextView.text =
            resources.getQuantityString(R.plurals.brapi_parent_count, items.size, items.size)
        mBinding.emptyContainer.visibility = if (items.isEmpty()) View.VISIBLE else View.GONE
        mBinding.listView.visibility = if (items.isEmpty()) View.GONE else View.VISIBLE
        mBinding.importButton.isEnabled = items.isNotEmpty()

        if (items.isEmpty()) {
            return
        }

        mBinding.listView.adapter = object : ArrayAdapter<BrAPICrossParent>(
            context,
            R.layout.list_item_potential_parent,
            items
        ) {
            override fun getView(position: Int, convertView: View?, parent: ViewGroup): View {
                val binding = if (convertView == null) {
                    DataBindingUtil.inflate<ListItemPotentialParentBinding>(
                        LayoutInflater.from(context),
                        R.layout.list_item_potential_parent,
                        parent,
                        false
                    )
                } else {
                    DataBindingUtil.getBinding<ListItemPotentialParentBinding>(convertView)
                        ?: DataBindingUtil.inflate(
                            LayoutInflater.from(context),
                            R.layout.list_item_potential_parent,
                            parent,
                            false
                        )
                }

                val item = getItem(position)
                val stableId = item?.observationUnitDbId ?: item?.observationUnitName.orEmpty()
                val rawJson = item?.let { gson.toJson(it) }.orEmpty()
                val isExpanded = stableId in expandedParentIds

                binding.title = item?.observationUnitName ?: getString(R.string.brapi_project_value_unavailable)
                binding.observationUnitId =
                    item?.observationUnitDbId ?: getString(R.string.brapi_project_id_unavailable)
                binding.parentType =
                    item?.parentType?.toString() ?: getString(R.string.brapi_parent_type_unknown)
                binding.additionalInfo = formatJson(rawJson)
                binding.showAdditionalInfo = isExpanded
                binding.additionalInfoToggleText =
                    getString(
                        if (isExpanded) R.string.brapi_hide_additional_info
                        else R.string.brapi_show_additional_info
                    )

                binding.additionalInfoToggleChip.setOnClickListener {
                    if (isExpanded) expandedParentIds.remove(stableId) else expandedParentIds.add(stableId)
                    notifyDataSetChanged()
                }

                binding.executePendingBindings()
                return binding.root
            }
        }
    }

    private fun importPotentialParents() {
        if (potentialParents.isEmpty()) {
            Toast.makeText(context, R.string.brapi_no_potential_parents_found, Toast.LENGTH_SHORT).show()
            return
        }

        mBinding.progressVisibility = View.VISIBLE
        mBinding.importButton.isEnabled = false

        lifecycleScope.launch {
            try {
                withContext(Dispatchers.IO) {
                    val grouped = potentialParents
                        .groupBy { it.observationUnitDbId ?: it.observationUnitName.orEmpty() }
                        .filterKeys { it.isNotBlank() }

                    val parents = grouped.map { (codeId, items) ->
                        val first = items.first()
                        val inferredSex = when {
                            items.any { it.parentType?.toString() == "MALE" } -> 1
                            items.any { it.parentType?.toString() == "FEMALE" } -> 0
                            else -> 0
                        }
                        Parent(codeId = codeId, sex = inferredSex).also { parent ->
                            parent.name = first.observationUnitName ?: codeId
                        }
                    }

                    if (parents.isNotEmpty()) {
                        parentsRepository.insertIgnore(*parents.toTypedArray())
                        parentsRepository.updateName(*parents.toTypedArray())
                    }
                }

                Toast.makeText(
                    requireContext(),
                    getString(R.string.brapi_imported_parents, potentialParents.size),
                    Toast.LENGTH_SHORT
                ).show()
                findNavController().popBackStack(R.id.parents_fragment, false)
            } finally {
                mBinding.progressVisibility = View.GONE
                mBinding.importButton.isEnabled = potentialParents.isNotEmpty()
            }
        }
    }

    private fun formatJson(rawJson: String): String {
        if (rawJson.isBlank()) {
            return getString(R.string.brapi_project_value_unavailable)
        }

        return runCatching {
            prettyGson.toJson(JsonParser.parseString(rawJson))
        }.getOrElse { rawJson }
    }
}