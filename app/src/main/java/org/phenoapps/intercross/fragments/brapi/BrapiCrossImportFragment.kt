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
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import org.brapi.v2.model.germ.BrAPICross
import org.brapi.v2.model.germ.BrAPICrossParent
import org.phenoapps.intercross.R
import androidx.appcompat.widget.Toolbar
import org.phenoapps.intercross.activities.MainActivity
import org.phenoapps.intercross.brapi.service.BrAPIServiceV2
import org.phenoapps.intercross.brapi.service.BrAPIService
import org.phenoapps.intercross.brapi.service.BrapiPaginationManager
import org.phenoapps.intercross.data.EventsRepository
import org.phenoapps.intercross.data.ParentsRepository
import org.phenoapps.intercross.data.models.CrossType
import org.phenoapps.intercross.data.models.Event
import org.phenoapps.intercross.data.models.Parent
import org.phenoapps.intercross.databinding.FragmentBrapiCrossesBinding
import org.phenoapps.intercross.databinding.ListItemEventsBinding
import org.phenoapps.intercross.fragments.IntercrossBaseFragment
import org.phenoapps.intercross.util.DateUtil

/**
 * Imports crosses from a BrAPI crossing project into the local events table.
 * Similar UI to planned crosses import but creates Event rows instead of Wishlist entries.
 */
class BrapiCrossImportFragment : IntercrossBaseFragment<FragmentBrapiCrossesBinding>(R.layout.fragment_brapi_crosses) {

    private val argsBundle by lazy { requireArguments() }

    private val crossingProjectDbId: String by lazy { argsBundle.getString("crossingProjectDbId") ?: "" }
    private val crossingProjectName: String by lazy { argsBundle.getString("crossingProjectName") ?: "" }
    private val crossingProjectDescription: String by lazy { argsBundle.getString("crossingProjectDescription") ?: "" }
    private val programName: String by lazy { argsBundle.getString("programName") ?: "" }
    private val commonCropName: String by lazy { argsBundle.getString("commonCropName") ?: "" }
    private val parentCount: Int by lazy { argsBundle.getInt("parentCount", 0) }

    private val eventsRepository by lazy { EventsRepository.getInstance(db.eventsDao()) }

    private val parentsRepository by lazy { ParentsRepository.getInstance(db.parentsDao()) }

    private val mService: BrAPIServiceV2 by lazy { BrAPIServiceV2(this.context) }

    private val paginationManager by lazy { BrapiPaginationManager(context) }

    private var crosses: List<BrAPICross> = emptyList()

    private companion object {
        val TAG = BrapiCrossImportFragment::class.simpleName
    }

    override fun FragmentBrapiCrossesBinding.afterCreateView() {
        emptyContainer.visibility = View.GONE
        errorContainer.visibility = View.GONE

        (activity as? MainActivity)?.applyFragmentInsets(root, toolbar as? Toolbar)

        toolbar.setNavigationOnClickListener {
            findNavController().popBackStack()
        }

        importButton.setOnClickListener {
            importCrossesAsEvents()
        }

        retryButton.setOnClickListener {
            loadCrosses()
        }

        bindProjectSummary()
        loadCrosses()
    }

    private fun bindProjectSummary() {
        mBinding.toolbar.title = getString(R.string.brapi_cross_import_title)
        mBinding.projectNameTextView.text = crossingProjectName
        mBinding.projectDescriptionTextView.text =
            crossingProjectDescription.ifBlank { getString(R.string.brapi_project_description_unavailable) }
        mBinding.programValueTextView.text = programName.ifBlank { getString(R.string.brapi_project_value_unavailable) }
        mBinding.cropValueTextView.text = commonCropName.ifBlank { getString(R.string.brapi_project_value_unavailable) }
        mBinding.projectIdChip.text = crossingProjectDbId.ifBlank { getString(R.string.brapi_project_id_unavailable) }
        mBinding.parentCountChip.text = resources.getQuantityString(R.plurals.brapi_parent_count, parentCount, parentCount)
    }

    @SuppressLint("MissingPermission")
    private fun isConnected(context: Context): Boolean {
        val connMgr = context.getSystemService(Context.CONNECTIVITY_SERVICE) as ConnectivityManager
        val networkInfo = connMgr.activeNetworkInfo
        return networkInfo != null && networkInfo.isConnected
    }

    private fun loadCrosses() {
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
        crosses = emptyList()
        showError(false)
        mBinding.emptyContainer.visibility = View.GONE
        mBinding.listView.visibility = View.GONE
        showLoading(true)

        lifecycleScope.launch {
            try {
                val results = mService.awaitCrosses(crossingProjectDbId, paginationManager)
                crosses = results.filter { it.crossingProjectDbId == crossingProjectDbId }
                bindCrosses(crosses)
            } catch (error: BrapiRequestException) {
                crosses = emptyList()
                Log.d(TAG, "BrAPI callback failed. ${error.code}")
                showError(true)
            } finally {
                showLoading(false)
            }
        }
    }

    private fun importCrossesAsEvents() {
        if (crosses.isEmpty()) {
            Toast.makeText(context, R.string.fragment_brapi_import_no_planned_crosses_found, Toast.LENGTH_SHORT).show()
            return
        }

        mBinding.importButton.isEnabled = false
        showLoading(true)

        lifecycleScope.launch {
            try {
                val total = crosses.size
                withContext(Dispatchers.IO) {
                    crosses.forEachIndexed { index, cross ->
                        val orderedParents = orderedParents(cross)

                        val maleId = orderedParents.first.observationUnitDbId ?: ""
                        val femaleId = orderedParents.second.observationUnitDbId ?: ""

                        val event = Event(
                            eventDbId = cross.crossDbId ?: "",
                            femaleObsUnitDbId = femaleId,
                            maleObsUnitDbId = maleId,
                            readableName = cross.crossName ?: "$femaleId x $maleId",
                            timestamp = DateUtil().getTime(),
                            person = "",
                            experiment = programName.ifBlank { "" },
                            type = determineCrossType(maleId, femaleId)
                        )

                        // insert parents
                        val maleParent = Parent(codeId = maleId, sex = 1).also { it.name = orderedParents.first.observationUnitName ?: maleId }
                        val femaleParent = Parent(codeId = femaleId, sex = 0).also { it.name = orderedParents.second.observationUnitName ?: femaleId }
                        parentsRepository.insertIgnore(maleParent, femaleParent)
                        parentsRepository.updateName(maleParent, femaleParent)

                        eventsRepository.insert(event)

                        withContext(Dispatchers.Main) {
                            mBinding.plannedCrossCountTextView.text = getString(R.string.brapi_import_progress, index + 1, total)
                        }
                    }
                }

                Toast.makeText(requireContext(), getString(R.string.brapi_imported_crosses, crosses.size), Toast.LENGTH_SHORT).show()
                findNavController().popBackStack()
            } finally {
                showLoading(false)
            }
        }
    }

    private fun showLoading(isLoading: Boolean) {
        mBinding.progressVisibility = if (isLoading) View.VISIBLE else View.GONE
        mBinding.importButton.isEnabled = !isLoading && crosses.isNotEmpty()
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

    private fun orderedParents(cross: BrAPICross): Pair<BrAPICrossParent, BrAPICrossParent> {
        return if (cross.parent1.parentType.toString() == "MALE") {
            cross.parent1 to cross.parent2
        } else {
            cross.parent2 to cross.parent1
        }
    }

    private fun determineCrossType(maleId: String, femaleId: String): CrossType {
        return when {
            maleId == "blank" -> CrossType.OPEN
            femaleId == maleId -> CrossType.SELF
            else -> CrossType.BIPARENTAL
        }
    }

    private data class CrossUiModel(
        val crossId: String,
        val maleName: String,
        val maleId: String,
        val femaleName: String,
        val femaleId: String
    )

    private fun bindCrosses(items: List<BrAPICross>) {
        val context = context ?: return
        mBinding.emptyContainer.visibility = if (items.isEmpty()) View.VISIBLE else View.GONE
        mBinding.listView.visibility = if (items.isEmpty()) View.GONE else View.VISIBLE
        mBinding.importButton.isEnabled = items.isNotEmpty()
        mBinding.plannedCrossCountTextView.text = resources.getQuantityString(R.plurals.brapi_cross_count, items.size, items.size)
        showError(false)

        if (items.isEmpty()) return

        val rows = items.map { cross ->
            val orderedParents = orderedParents(cross)
            CrossUiModel(
                crossId = cross.crossDbId ?: "",
                maleName = orderedParents.first.observationUnitName ?: getString(R.string.brapi_project_value_unavailable),
                maleId = orderedParents.first.observationUnitDbId ?: getString(R.string.brapi_project_id_unavailable),
                femaleName = orderedParents.second.observationUnitName ?: getString(R.string.brapi_project_value_unavailable),
                femaleId = orderedParents.second.observationUnitDbId ?: getString(R.string.brapi_project_id_unavailable)
            )
        }

        mBinding.listView.adapter = object : ArrayAdapter<CrossUiModel>(context, R.layout.list_item_events, rows) {
            override fun getView(position: Int, convertView: View?, parent: ViewGroup): View {
                val binding = if (convertView == null) {
                    DataBindingUtil.inflate<ListItemEventsBinding>(LayoutInflater.from(context), R.layout.list_item_events, parent, false)
                } else {
                    DataBindingUtil.getBinding<ListItemEventsBinding>(convertView)
                        ?: DataBindingUtil.inflate(LayoutInflater.from(context), R.layout.list_item_events, parent, false)
                }

                val item = getItem(position)
                binding.clickListener = null
                binding.event = Event(
                    eventDbId = item?.crossId ?: "",
                    femaleObsUnitDbId = item?.femaleId ?: "",
                    maleObsUnitDbId = item?.maleId ?: "",
                    readableName = "${item?.femaleName ?: ""} x ${item?.maleName ?: ""}",
                    timestamp = DateUtil().getTime(),
                    person = "",
                    experiment = programName,
                    type = CrossType.BIPARENTAL
                )
                binding.female = item?.femaleName ?: ""
                binding.male = item?.maleName ?: ""
                binding.timestamp = ""
                binding.executePendingBindings()
                return binding.root
            }
        }
    }
}