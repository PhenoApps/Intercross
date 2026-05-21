package org.phenoapps.intercross.fragments.brapi

import android.annotation.SuppressLint
import android.content.Context
import android.net.ConnectivityManager
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ArrayAdapter
import android.widget.Toast
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
import org.brapi.v2.model.germ.BrAPIPlannedCross
import org.phenoapps.intercross.R
import androidx.appcompat.widget.Toolbar
import org.phenoapps.intercross.activities.MainActivity
import org.phenoapps.intercross.brapi.service.BrAPIService
import org.phenoapps.intercross.brapi.service.BrAPIServiceV2
import org.phenoapps.intercross.brapi.service.BrapiPaginationManager
import org.phenoapps.intercross.data.ParentsRepository
import org.phenoapps.intercross.data.WishlistRepository
import org.phenoapps.intercross.data.models.Parent
import org.phenoapps.intercross.data.models.Wishlist
import org.phenoapps.intercross.databinding.FragmentBrapiPlannedCrossesBinding
import org.phenoapps.intercross.databinding.ListItemPlannedCrossBinding
import org.phenoapps.intercross.fragments.IntercrossBaseFragment

class BrapiPlannedCrossesFragment :
    IntercrossBaseFragment<FragmentBrapiPlannedCrossesBinding>(R.layout.fragment_brapi_planned_crosses) {

    private val args: BrapiPlannedCrossesFragmentArgs by navArgs()

    private val wishRepository by lazy {
        WishlistRepository.getInstance(db.wishlistDao())
    }

    private val parentsRepository by lazy {
        ParentsRepository.getInstance(db.parentsDao())
    }

    private val mService: BrAPIServiceV2 by lazy {
        BrAPIServiceV2(this@BrapiPlannedCrossesFragment.context)
    }

    private val paginationManager by lazy {
        BrapiPaginationManager(context)
    }

    private var plannedCrosses: List<BrAPIPlannedCross> = emptyList()
    private val expandedCrossIds = mutableSetOf<String>()
    private val prettyGson by lazy { GsonBuilder().setPrettyPrinting().create() }

    private companion object {
        val TAG = BrapiPlannedCrossesFragment::class.simpleName
    }

    private data class PlannedCrossUiModel(
        val title: String,
        val plannedCrossId: String,
        val maleName: String,
        val maleId: String,
        val femaleName: String,
        val femaleId: String,
        val wishType: String,
        val wishMin: Int,
        val wishMax: Int,
        val note: String
    )

    override fun FragmentBrapiPlannedCrossesBinding.afterCreateView() {
        emptyContainer.visibility = View.GONE
        errorContainer.visibility = View.GONE

        (activity as? MainActivity)?.applyFragmentInsets(root, toolbar as? Toolbar)

        toolbar.setNavigationOnClickListener {
            findNavController().popBackStack()
        }

        importButton.setOnClickListener {
            importPlannedCrosses()
        }

        retryButton.setOnClickListener {
            loadPlannedCrosses()
        }

        bindProjectSummary()
        loadPlannedCrosses()
    }

    private fun bindProjectSummary() {
        mBinding.toolbar.title = getString(R.string.brapi_planned_crosses_title)
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

    @SuppressLint("MissingPermission")
    private fun isConnected(context: Context): Boolean {
        val connMgr = context.getSystemService(Context.CONNECTIVITY_SERVICE) as ConnectivityManager
        val networkInfo = connMgr.activeNetworkInfo
        return networkInfo != null && networkInfo.isConnected
    }

    private fun loadPlannedCrosses() {
        val context = context ?: return
        if (!isConnected(context)) {
            Toast.makeText(context, R.string.device_offline_warning, Toast.LENGTH_SHORT).show()
            return
        }
        if (!BrAPIService.hasValidBaseUrl(context)) {
            Toast.makeText(context, R.string.brapi_must_configure_url, Toast.LENGTH_SHORT).show()
            return
        }

        paginationManager.reset()
        plannedCrosses = emptyList()
        showError(false)
        mBinding.emptyContainer.visibility = View.GONE
        mBinding.listView.visibility = View.GONE
        showLoading(true)

        lifecycleScope.launch {
            try {
                val results = mService.awaitPlannedCrosses(args.crossingProjectDbId, paginationManager)
                plannedCrosses = results.filter { it.crossingProjectDbId == args.crossingProjectDbId }
                bindPlannedCrosses(plannedCrosses)
            } catch (error: BrapiRequestException) {
                plannedCrosses = emptyList()
                Log.d(TAG, "BrAPI callback failed. ${error.code}")
                showError(true)
            } finally {
                showLoading(false)
            }
        }
    }

    private fun bindPlannedCrosses(items: List<BrAPIPlannedCross>) {
        val context = context ?: return
        mBinding.emptyContainer.visibility = if (items.isEmpty()) View.VISIBLE else View.GONE
        mBinding.listView.visibility = if (items.isEmpty()) View.GONE else View.VISIBLE
        mBinding.importButton.isEnabled = items.isNotEmpty()
        mBinding.plannedCrossCountTextView.text =
            resources.getQuantityString(R.plurals.brapi_planned_cross_count, items.size, items.size)
        showError(false)

        if (items.isEmpty()) {
            return
        }

        val rows = items.map { plannedCross ->
            val orderedParents = orderedParents(plannedCross)
            val metadata = parseWishMetadata(plannedCross.additionalInfo)
            PlannedCrossUiModel(
                title = plannedCross.plannedCrossName?.ifBlank {
                    "${orderedParents.second.observationUnitName} x ${orderedParents.first.observationUnitName}"
                } ?: "${orderedParents.second.observationUnitName} x ${orderedParents.first.observationUnitName}",
                plannedCrossId = plannedCross.plannedCrossDbId ?: "",
                maleName = orderedParents.first.observationUnitName ?: getString(R.string.brapi_project_value_unavailable),
                maleId = orderedParents.first.observationUnitDbId ?: getString(R.string.brapi_project_id_unavailable),
                femaleName = orderedParents.second.observationUnitName ?: getString(R.string.brapi_project_value_unavailable),
                femaleId = orderedParents.second.observationUnitDbId ?: getString(R.string.brapi_project_id_unavailable),
                wishType = metadata.wishType,
                wishMin = metadata.wishMin,
                wishMax = metadata.wishMax,
                note = plannedCross.additionalInfo?.toString().orEmpty()
            )
        }

        mBinding.listView.adapter = object : ArrayAdapter<PlannedCrossUiModel>(
            context,
            R.layout.list_item_planned_cross,
            rows
        ) {
            override fun getView(position: Int, convertView: View?, parent: ViewGroup): View {
                val binding = if (convertView == null) {
                    DataBindingUtil.inflate<ListItemPlannedCrossBinding>(
                        LayoutInflater.from(context),
                        R.layout.list_item_planned_cross,
                        parent,
                        false
                    )
                } else {
                    DataBindingUtil.getBinding<ListItemPlannedCrossBinding>(convertView)
                        ?: DataBindingUtil.inflate(
                            LayoutInflater.from(context),
                            R.layout.list_item_planned_cross,
                            parent,
                            false
                        )
                }

                val item = getItem(position)
                val stableId = item?.plannedCrossId ?: position.toString()
                val isExpanded = stableId in expandedCrossIds
                binding.title = item?.title ?: ""
                binding.plannedCrossId = item?.plannedCrossId ?: ""
                binding.maleName = item?.maleName ?: ""
                binding.maleId = item?.maleId ?: ""
                binding.femaleName = item?.femaleName ?: ""
                binding.femaleId = item?.femaleId ?: ""
                binding.wishType = item?.wishType ?: ""
                binding.wishMin = item?.wishMin ?: 0
                binding.wishMax = item?.wishMax ?: 0
                binding.note = formatJson(item?.note ?: "")
                binding.hasNote = !item?.note.isNullOrBlank()
                binding.showNote = isExpanded
                binding.noteToggleText = context.getString(
                    if (isExpanded) R.string.brapi_hide_additional_info
                    else R.string.brapi_show_additional_info
                )
                binding.additionalInfoToggleChip.setOnClickListener {
                    if (isExpanded) expandedCrossIds.remove(stableId) else expandedCrossIds.add(stableId)
                    notifyDataSetChanged()
                }
                binding.executePendingBindings()
                return binding.root
            }
        }
    }

    private fun importPlannedCrosses() {
        if (plannedCrosses.isEmpty()) {
            Toast.makeText(context, R.string.fragment_brapi_import_no_planned_crosses_found, Toast.LENGTH_SHORT).show()
            return
        }

        mBinding.importParentsCheckBox.isEnabled = false
        mBinding.importButton.isEnabled = false
        showLoading(true)

        lifecycleScope.launch {
            try {
                val total = plannedCrosses.size
                withContext(Dispatchers.IO) {
                    plannedCrosses.forEachIndexed { index, plan ->
                        val orderedParents = orderedParents(plan)
                        val metadata = parseWishMetadata(plan.additionalInfo)

                        wishRepository.insert(
                            Wishlist(
                                femaleDbId = orderedParents.second.observationUnitDbId,
                                maleDbId = orderedParents.first.observationUnitDbId,
                                femaleName = orderedParents.second.observationUnitName,
                                maleName = orderedParents.first.observationUnitName,
                                wishType = metadata.wishType,
                                wishMin = metadata.wishMin,
                                wishMax = metadata.wishMax
                            )
                        )

                        val maleParent = Parent(
                            codeId = orderedParents.first.observationUnitDbId,
                            sex = 1
                        ).also { it.name = orderedParents.first.observationUnitName ?: orderedParents.first.observationUnitDbId }

                        val femaleParent = Parent(
                            codeId = orderedParents.second.observationUnitDbId,
                            sex = 0
                        ).also { it.name = orderedParents.second.observationUnitName ?: orderedParents.second.observationUnitDbId }

                        if (mBinding.importParentsCheckBox.isChecked) {
                            parentsRepository.insertIgnore(maleParent, femaleParent)
                            parentsRepository.updateName(maleParent, femaleParent)
                        }

                        withContext(Dispatchers.Main) {
                            mBinding.plannedCrossCountTextView.text = getString(R.string.brapi_import_progress, index + 1, total)
                        }
                    }
                }

                Toast.makeText(
                    requireContext(),
                    getString(R.string.brapi_imported_planned_crosses, plannedCrosses.size),
                    Toast.LENGTH_SHORT
                ).show()

                if (!findNavController().popBackStack(R.id.parents_fragment, false)) {
                    findNavController().navigate(R.id.parents_fragment)
                }

            } finally {
                showLoading(false)
                if (view != null) {
                    mBinding.importParentsCheckBox.isEnabled = true
                }
            }
        }
    }

    private fun showLoading(isLoading: Boolean) {
        mBinding.progressVisibility = if (isLoading) View.VISIBLE else View.GONE
        mBinding.importButton.isEnabled = !isLoading && plannedCrosses.isNotEmpty()
        mBinding.retryButton.isEnabled = !isLoading
    }

    private fun showError(show: Boolean) {
        mBinding.errorContainer.visibility = if (show) View.VISIBLE else View.GONE
        if (show) {
            mBinding.emptyContainer.visibility = View.GONE
            mBinding.listView.visibility = View.GONE
            mBinding.importButton.isEnabled = false
        }
    }

    private fun orderedParents(plan: BrAPIPlannedCross): Pair<BrAPICrossParent, BrAPICrossParent> {
        return if (plan.parent1.parentType.toString() == "MALE") {
            plan.parent1 to plan.parent2
        } else {
            plan.parent2 to plan.parent1
        }
    }

    private fun parseWishMetadata(additionalInfo: Any?): WishMetadata {
        val map = runCatching {
            Gson().fromJson(additionalInfo?.toString().orEmpty(), HashMap::class.java)
        }.getOrNull()

        return WishMetadata(
            wishType = map?.get("wishType")?.toString()?.ifBlank { "cross" } ?: "cross",
            wishMin = map?.get("wishMin")?.toString()?.toIntOrNull() ?: 1,
            wishMax = map?.get("wishMax")?.toString()?.toIntOrNull() ?: 10
        )
    }

    private fun formatJson(rawJson: String): String {
        if (rawJson.isBlank()) {
            return getString(R.string.brapi_project_value_unavailable)
        }

        return runCatching {
            prettyGson.toJson(JsonParser.parseString(rawJson))
        }.getOrElse { rawJson }
    }

    private data class WishMetadata(
        val wishType: String,
        val wishMin: Int,
        val wishMax: Int
    )
}