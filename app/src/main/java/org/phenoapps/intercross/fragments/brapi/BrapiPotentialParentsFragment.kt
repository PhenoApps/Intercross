package org.phenoapps.intercross.fragments.brapi

import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ArrayAdapter
import android.widget.Toast
import androidx.appcompat.widget.Toolbar
import androidx.databinding.DataBindingUtil
import androidx.lifecycle.lifecycleScope
import androidx.navigation.fragment.findNavController
import androidx.navigation.fragment.navArgs
import com.google.gson.Gson
import com.google.gson.GsonBuilder
import com.google.gson.JsonParser
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import org.brapi.v2.model.germ.BrAPICrossParent
import org.brapi.v2.model.pheno.BrAPIObservation
import org.phenoapps.intercross.R
import org.phenoapps.intercross.activities.MainActivity
import org.phenoapps.intercross.brapi.service.BrAPIServiceV2
import org.phenoapps.intercross.data.ParentsRepository
import org.phenoapps.intercross.data.models.Parent
import org.phenoapps.intercross.databinding.FragmentBrapiPotentialParentsBinding
import org.phenoapps.intercross.databinding.ListItemPotentialParentBinding
import org.phenoapps.intercross.fragments.IntercrossBaseFragment
import java.time.OffsetDateTime
import java.time.ZoneOffset

private data class PotentialParentRow(
    val parent: BrAPICrossParent,
    val fromRecentObservation: Boolean
)

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

    private val basePotentialParents by lazy {
        (args.potentialParentsJson ?: emptyArray()).mapNotNull { encoded ->
            runCatching { gson.fromJson(encoded, BrAPICrossParent::class.java) }.getOrNull()
        }.distinctBy { it.observationUnitDbId ?: it.observationUnitName }
    }

    private var displayRows: List<PotentialParentRow> = emptyList()

    private val expandedParentIds = mutableSetOf<String>()

    override fun FragmentBrapiPotentialParentsBinding.afterCreateView() {
        emptyContainer.visibility = View.GONE

        (activity as? MainActivity)?.applyFragmentInsets(root, toolbar as? Toolbar)

        toolbar.title = getString(R.string.brapi_potential_parents_title)
        toolbar.setNavigationOnClickListener {
            findNavController().popBackStack()
        }

        bindProjectSummary()

        displayRows = basePotentialParents.map { PotentialParentRow(it, false) }
        bindPotentialParents(displayRows)

        if (args.studyDbId.isNotBlank() && args.observationVariableDbId.isNotBlank()) {
            lifecycleScope.launch {
                progressVisibility = View.VISIBLE
                try {
                    val rangeEnd = OffsetDateTime.now(ZoneOffset.UTC)
                    val rangeStart = rangeEnd.minusDays(7)
                    val allObs = withContext(Dispatchers.IO) {
                        mService.observationsApi.getAllObservationsForStudyVariableInTimestampRange(
                            args.studyDbId,
                            args.observationVariableDbId,
                            rangeStart.toString(),
                            rangeEnd.toString()
                        )
                    }
                    val filtered = filterObservationsLastWeek(allObs)
                    val suggested = parentsFromObservations(filtered)
                    displayRows = mergeParentRows(basePotentialParents, suggested)
                    bindPotentialParents(displayRows)
                } catch (e: Exception) {
                    Log.e(TAG, "Failed to load observations for potential parents", e)
                    Toast.makeText(
                        requireContext(),
                        getString(R.string.brapi_potential_parents_observations_load_error),
                        Toast.LENGTH_LONG
                    ).show()
                } finally {
                    progressVisibility = View.GONE
                }
            }
        }

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

    private fun bindPotentialParents(rows: List<PotentialParentRow>) {
        val context = context ?: return
        mBinding.plannedCrossCountTextView.text =
            resources.getQuantityString(R.plurals.brapi_parent_count, rows.size, rows.size)
        mBinding.emptyContainer.visibility = if (rows.isEmpty()) View.VISIBLE else View.GONE
        mBinding.listView.visibility = if (rows.isEmpty()) View.GONE else View.VISIBLE
        mBinding.importButton.isEnabled = rows.isNotEmpty()

        if (rows.isEmpty()) {
            return
        }

        mBinding.listView.adapter = object : ArrayAdapter<PotentialParentRow>(
            context,
            R.layout.list_item_potential_parent,
            rows
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

                val row = getItem(position) ?: return binding.root
                val item = row.parent
                val stableId = item.observationUnitDbId ?: item.observationUnitName.orEmpty()
                val rawJson = gson.toJson(item)
                val isExpanded = stableId in expandedParentIds

                binding.title = item.observationUnitName ?: getString(R.string.brapi_project_value_unavailable)
                binding.observationUnitId =
                    item.observationUnitDbId ?: getString(R.string.brapi_project_id_unavailable)
                binding.parentType =
                    item.parentType?.toString() ?: getString(R.string.brapi_parent_type_unknown)
                binding.additionalInfo = formatJson(rawJson)
                binding.showAdditionalInfo = isExpanded
                binding.additionalInfoToggleText =
                    getString(
                        if (isExpanded) R.string.brapi_hide_additional_info
                        else R.string.brapi_show_additional_info
                    )
                binding.externalReference = ""
                binding.showRecentObservationIcon = row.fromRecentObservation

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
        if (displayRows.isEmpty()) {
            Toast.makeText(context, R.string.brapi_no_potential_parents_found, Toast.LENGTH_SHORT).show()
            return
        }

        mBinding.progressVisibility = View.VISIBLE
        mBinding.importButton.isEnabled = false

        lifecycleScope.launch {
            try {
                withContext(Dispatchers.IO) {
                    val grouped = displayRows
                        .map { it.parent }
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
                    getString(R.string.brapi_imported_parents, displayRows.size),
                    Toast.LENGTH_SHORT
                ).show()
                findNavController().popBackStack(R.id.parents_fragment, false)
            } finally {
                mBinding.progressVisibility = View.GONE
                mBinding.importButton.isEnabled = displayRows.isNotEmpty()
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

private fun stableParentKey(parent: BrAPICrossParent): String =
    (parent.observationUnitDbId ?: parent.observationUnitName).orEmpty()

private fun filterObservationsLastWeek(observations: List<BrAPIObservation>): List<BrAPIObservation> {
    val cutoff = OffsetDateTime.now(ZoneOffset.UTC).minusDays(7)
    return observations.filter { obs ->
        val ts = obs.observationTimeStamp ?: return@filter false
        !ts.isBefore(cutoff)
    }
}

private fun parentsFromObservations(observations: List<BrAPIObservation>): List<BrAPICrossParent> =
    observations
        .distinctBy { obs ->
            obs.observationUnitDbId?.takeIf { it.isNotBlank() }
                ?: obs.observationUnitName?.takeIf { it.isNotBlank() }.orEmpty()
        }
        .mapNotNull { obs ->
            val unitId = obs.observationUnitDbId?.takeIf { it.isNotBlank() }
            val unitName = obs.observationUnitName?.takeIf { it.isNotBlank() }
            if (unitId == null && unitName == null) return@mapNotNull null
            BrAPICrossParent().apply {
                observationUnitDbId = unitId
                observationUnitName = unitName ?: unitId
            }
        }

private fun mergeParentRows(
    base: List<BrAPICrossParent>,
    suggested: List<BrAPICrossParent>
): List<PotentialParentRow> {
    val usedKeys = mutableSetOf<String>()
    val rows = mutableListOf<PotentialParentRow>()
    suggested.forEach { p ->
        val key = stableParentKey(p)
        if (key.isBlank() || key in usedKeys) return@forEach
        usedKeys.add(key)
        rows.add(PotentialParentRow(p, true))
    }
    base.forEach { p ->
        val key = stableParentKey(p)
        if (key.isNotBlank() && key !in usedKeys) usedKeys.add(key)
        rows.add(PotentialParentRow(p, false))
    }
    return rows
}
