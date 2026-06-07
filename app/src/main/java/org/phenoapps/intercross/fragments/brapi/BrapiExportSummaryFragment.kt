package org.phenoapps.intercross.fragments.brapi

import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ArrayAdapter
import android.widget.Toast
import androidx.appcompat.widget.Toolbar
import androidx.databinding.DataBindingUtil
import androidx.fragment.app.viewModels
import androidx.lifecycle.lifecycleScope
import androidx.navigation.fragment.findNavController
import androidx.navigation.fragment.navArgs
import com.google.gson.JsonObject
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import org.brapi.v2.model.BrAPIExternalReference
import org.brapi.v2.model.germ.BrAPICross
import org.brapi.v2.model.germ.BrAPICrossParent
import org.brapi.v2.model.germ.BrAPICrossType
import org.brapi.v2.model.germ.BrAPIParentType
import org.phenoapps.intercross.R
import org.phenoapps.intercross.activities.MainActivity
import org.phenoapps.intercross.brapi.service.BrAPIServiceV2
import org.phenoapps.intercross.data.EventsRepository
import org.phenoapps.intercross.data.ParentsRepository
import org.phenoapps.intercross.data.models.CrossType
import org.phenoapps.intercross.data.models.Event
import org.phenoapps.intercross.data.viewmodels.EventListViewModel
import org.phenoapps.intercross.data.viewmodels.ParentsListViewModel
import org.phenoapps.intercross.data.viewmodels.factory.EventsListViewModelFactory
import org.phenoapps.intercross.data.viewmodels.factory.ParentsListViewModelFactory
import org.phenoapps.intercross.databinding.FragmentBrapiExportSummaryBinding
import org.phenoapps.intercross.databinding.ListItemEventsBinding
import org.phenoapps.intercross.fragments.IntercrossBaseFragment

class BrapiExportSummaryFragment :
    IntercrossBaseFragment<FragmentBrapiExportSummaryBinding>(R.layout.fragment_brapi_export_summary) {

    companion object {
        const val TAG = "BrAPI Export"
    }

    private val args: BrapiExportSummaryFragmentArgs by navArgs()

    private val eventsViewModel: EventListViewModel by viewModels {
        EventsListViewModelFactory(EventsRepository.getInstance(db.eventsDao()))
    }

    private val parentsViewModel: ParentsListViewModel by viewModels {
        ParentsListViewModelFactory(ParentsRepository.getInstance(db.parentsDao()))
    }

    private val mService: BrAPIServiceV2 by lazy {
        BrAPIServiceV2(this@BrapiExportSummaryFragment.context)
    }

    override fun FragmentBrapiExportSummaryBinding.afterCreateView() {
        (activity as? MainActivity)?.applyFragmentInsets(root, toolbar as? Toolbar)

        toolbar.setNavigationOnClickListener {
            findNavController().popBackStack()
        }

        bindProjectSummary()

        lifecycleScope.launch {
            withContext(Dispatchers.IO) {
                mService.getCrossingProject(args.crossingProjectDbId, { projects ->

                    if (projects.isNotEmpty()) {

                        val project = projects.first()

                        val maleParents = project.potentialParents.filter { it.parentType == BrAPIParentType.MALE }.map { it.observationUnitDbId }
                        val femaleParents = project.potentialParents.filter { it.parentType == BrAPIParentType.FEMALE }.map { it.observationUnitDbId }

                        lifecycleScope.launch(Dispatchers.Main) {

                            eventsViewModel.events.observe(viewLifecycleOwner) { items ->

                                val projectEvents = items.filter {
                                    it.maleObsUnitDbId in maleParents && it.femaleObsUnitDbId in femaleParents
                                }

                                bindCrosses(projectEvents)

                                exportButton.setOnClickListener {
                                    exportCrosses(projectEvents)
                                }
                            }
                        }
                    }

                    null

                }) { failCode ->

                    Log.e(TAG, "Failed to get crossing project $failCode")

                    null

                }
            }
        }
    }

    private fun bindProjectSummary() {
        mBinding.toolbar.title = getString(R.string.brapi_export_summary_title)
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

    private fun bindCrosses(items: List<Event>) {
        val context = context ?: return
        mBinding.crossCountTextView.text =
            getString(R.string.brapi_export_ready_count, items.size)
        mBinding.exportButton.isEnabled = items.isNotEmpty()

        mBinding.listView.adapter = object : ArrayAdapter<Event>(
            context,
            R.layout.list_item_events,
            items
        ) {
            override fun getView(position: Int, convertView: View?, parent: ViewGroup): View {
                val binding = if (convertView == null) {
                    DataBindingUtil.inflate<ListItemEventsBinding>(
                        LayoutInflater.from(context),
                        R.layout.list_item_events,
                        parent,
                        false
                    )
                } else {
                    DataBindingUtil.getBinding<ListItemEventsBinding>(convertView)
                        ?: DataBindingUtil.inflate(
                            LayoutInflater.from(context),
                            R.layout.list_item_events,
                            parent,
                            false
                        )
                }

                val item = getItem(position)
                item?.let {
                    binding.event = it
                    binding.female = it.femaleObsUnitDbId
                    binding.male = it.maleObsUnitDbId
                    binding.timestamp = it.timestamp
                }
                binding.executePendingBindings()
                return binding.root
            }
        }
    }

    private fun exportCrosses(crosses: List<Event>) {
         if (crosses.isEmpty()) return

        mBinding.progressVisibility = View.VISIBLE
        mBinding.exportButton.isEnabled = false

        parentsViewModel.parents.observe(viewLifecycleOwner) { parents ->
            lifecycleScope.launch {
                try {
                    val brapiCrosses = crosses.map { cross ->
                        BrAPICross().apply {
                            externalReferences = listOf(BrAPIExternalReference().apply {
                                referenceID = cross.eventDbId
                                referenceId = cross.eventDbId
                                referenceSource = "Intercross"
                            })
                            this.additionalInfo = JsonObject()
                            this.crossAttributes = listOf()
                            this.crossType = cross.type.toBrAPICrossType()
                            crossingProjectDbId = args.crossingProjectDbId
                            crossingProjectName = args.crossingProjectName
                            parent1 = BrAPICrossParent().apply {
                                this.observationUnitDbId = cross.femaleObsUnitDbId
                                parents.find { it.codeId == cross.femaleObsUnitDbId }?.let { parent ->
                                    this.observationUnitName = parent.name
                                    this.parentType = if (parent.sex == 0) BrAPIParentType.FEMALE else BrAPIParentType.MALE
                                }
                            }
                            parent2 = BrAPICrossParent().apply {
                                this.observationUnitDbId = cross.maleObsUnitDbId
                                parents.find { it.codeId == cross.maleObsUnitDbId }?.let { parent ->
                                    this.observationUnitName = parent.name
                                    this.parentType = if (parent.sex == 0) BrAPIParentType.FEMALE else BrAPIParentType.MALE
                                }
                            }
                            crossName = "${cross.femaleObsUnitDbId} x ${cross.maleObsUnitDbId}"
                        }
                    }

                    mService.postCrosses(brapiCrosses, { _ ->
                        lifecycleScope.launch(Dispatchers.Main) {
                            Toast.makeText(
                                requireContext(),
                                getString(R.string.brapi_exported_crosses, crosses.size),
                                Toast.LENGTH_SHORT
                            ).show()

                            findNavController().popBackStack(R.id.events_fragment, false)
                        }
                        null
                    }, { fail ->
                        Log.e("BrapiExportSummary", "BrAPI Export failed with code $fail")
                        lifecycleScope.launch(Dispatchers.Main) {
                            Toast.makeText(
                                requireContext(),
                                getString(R.string.fragment_brapi_export_crosses_failed),
                                Toast.LENGTH_SHORT
                            ).show()

                            findNavController().popBackStack(R.id.events_fragment, false)
                        }
                        null
                    })

                } catch (error: BrapiRequestException) {
                    Log.e("BrapiExportSummary", "BrAPI Export failed with code ${error.code}", error)
                    val message = when (error.code) {
                        403 -> getString(R.string.brapi_error_forbidden)
                        else -> getString(R.string.fragment_brapi_export_crosses_failed)
                    }
                    Toast.makeText(requireContext(), message, Toast.LENGTH_LONG).show()
                } catch (e: Exception) {
                    Log.e("BrapiExportSummary", "Export failed", e)
                    Toast.makeText(
                        requireContext(),
                        getString(R.string.fragment_brapi_export_crosses_failed),
                        Toast.LENGTH_SHORT
                    ).show()
                } finally {
                    mBinding.progressVisibility = View.GONE
                    mBinding.exportButton.isEnabled = true
                }
            }
        }
    }

    private fun CrossType.toBrAPICrossType(): BrAPICrossType = when (this) {
        CrossType.BIPARENTAL -> BrAPICrossType.BIPARENTAL
        CrossType.OPEN -> BrAPICrossType.OPEN_POLLINATED
        CrossType.POLY -> BrAPICrossType.BULK_OPEN_POLLINATED
        else -> BrAPICrossType.SELF
    }
}