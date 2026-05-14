package org.phenoapps.intercross.fragments.brapi

import android.annotation.SuppressLint
import android.content.Context
import android.net.ConnectivityManager
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ArrayAdapter
import android.widget.ListView
import android.widget.Toast
import androidx.appcompat.widget.Toolbar
import androidx.databinding.DataBindingUtil
import androidx.lifecycle.lifecycleScope
import androidx.navigation.fragment.findNavController
import androidx.navigation.fragment.navArgs
import kotlinx.coroutines.launch
import org.brapi.v2.model.pheno.BrAPIObservationVariable
import org.phenoapps.intercross.R
import org.phenoapps.intercross.activities.MainActivity
import org.phenoapps.intercross.brapi.service.BrAPIService
import org.phenoapps.intercross.brapi.service.BrAPIServiceV2
import org.phenoapps.intercross.brapi.service.BrapiPaginationManager
import org.phenoapps.intercross.databinding.FragmentBrapiImportParentsPickerBinding
import org.phenoapps.intercross.databinding.ListItemBrapiObservationVariableBinding
import org.phenoapps.intercross.fragments.IntercrossBaseFragment

class BrapiImportParentsObservationVariableFragment :
    IntercrossBaseFragment<FragmentBrapiImportParentsPickerBinding>(R.layout.fragment_brapi_import_parents_picker) {

    private val args: BrapiImportParentsObservationVariableFragmentArgs by navArgs()

    private companion object {
        val TAG: String = BrapiImportParentsObservationVariableFragment::class.java.simpleName
    }

    private val mService: BrAPIServiceV2 by lazy { BrAPIServiceV2(requireContext()) }

    private val mPaginationManager by lazy { BrapiPaginationManager(requireActivity()) }

    private var mSelected: BrAPIObservationVariable? = null

    private fun setupPageController() {
        with(mBinding.pageController) {
            setNextClick {
                mPaginationManager.setNewPage(nextButton.id)
                loadBrAPIData()
            }
            setPrevClick {
                mPaginationManager.setNewPage(prevButton.id)
                loadBrAPIData()
            }
        }
    }

    private fun setupToolbarAndButtons() {
        mBinding.toolbar.setNavigationOnClickListener {
            findNavController().popBackStack()
        }
        mBinding.toolbar.title = getString(R.string.brapi_import_parents_variable_title)
        mBinding.hintTextView.text = getString(R.string.brapi_import_parents_variable_hint)
        mBinding.nextButton.text = getString(R.string.brapi_import_parents_review_potential_parents)
        mBinding.nextButton.setOnClickListener {
            navigateToPotentialParents(
                observationVariableDbId = mSelected?.observationVariableDbId ?: ""
            )
        }
        mBinding.skipObservationSearchButton.setOnClickListener {
            navigateToPotentialParents(observationVariableDbId = "")
        }
    }

    private fun navigateToPotentialParents(observationVariableDbId: String) {
        findNavController().navigate(
            BrapiImportParentsObservationVariableFragmentDirections
                .actionBrapiImportParentsObservationVariableToBrapiPotentialParentsFragment(
                    programDbId = args.programDbId,
                    crossingProjectDbId = args.crossingProjectDbId,
                    crossingProjectName = args.crossingProjectName,
                    crossingProjectDescription = args.crossingProjectDescription,
                    programName = args.programName,
                    commonCropName = args.commonCropName,
                    parentCount = args.parentCount,
                    potentialParentsJson = args.potentialParentsJson,
                    studyDbId = args.selectedStudyDbId,
                    observationVariableDbId = observationVariableDbId
                )
        )
    }

    @SuppressLint("MissingPermission")
    private fun isConnected(context: Context): Boolean {
        val connMgr = context.getSystemService(Context.CONNECTIVITY_SERVICE) as ConnectivityManager
        val networkInfo = connMgr.activeNetworkInfo
        return networkInfo != null && networkInfo.isConnected
    }

    private fun handleFailure(fail: Int): Void? {
        Log.d(TAG, "BrAPI callback failed. $fail")
        return null
    }

    private fun loadBrAPIData() {
        mBinding.progressVisibility = View.VISIBLE
        lifecycleScope.launch {
            try {
                val variables = mService.awaitObservationVariables(
                    args.programDbId,
                    args.selectedStudyDbId,
                    mPaginationManager
                )
                val filtered = variables.filter {
                    (it.observationVariableName?.isNotEmpty() == true) &&
                        it.observationVariableDbId != null
                }
                applyPreselection(filtered)
                buildArrayAdapter(filtered)
                mBinding.pageController.visibility =
                    if (mPaginationManager.totalPages > 1) View.VISIBLE else View.GONE
            } catch (error: BrapiRequestException) {
                handleFailure(error.code)
                Toast.makeText(
                    requireContext(),
                    R.string.brapi_import_parents_variable_load_error,
                    Toast.LENGTH_LONG
                ).show()
            } catch (cme: ConcurrentModificationException) {
                Log.d(TAG, cme.localizedMessage ?: "Async update error.")
                cme.printStackTrace()
            } finally {
                mBinding.progressVisibility = null
            }
        }
    }

    private fun applyPreselection(variables: List<BrAPIObservationVariable>) {
        val pref = args.prefilledObservationVariableDbId
        if (pref.isBlank()) return
        mSelected = variables.find { it.observationVariableDbId == pref }
    }

    private fun variableDescription(item: BrAPIObservationVariable?): String {
        if (item == null) return ""
        val traitName = item.trait?.traitName
        val pui = item.observationVariablePUI
        return listOfNotNull(
            traitName?.takeIf { it.isNotBlank() },
            pui?.takeIf { it.isNotBlank() }
        ).joinToString(separator = " · ").ifBlank { item.observationVariableName ?: "" }
    }

    private fun buildArrayAdapter(data: List<BrAPIObservationVariable>) {
        val listView = mBinding.listView
        val ctx = context ?: return
        activity?.runOnUiThread {
            val adapter = object : ArrayAdapter<BrAPIObservationVariable>(
                ctx,
                R.layout.list_item_brapi_observation_variable,
                data
            ) {
                private fun selectVariable(variable: BrAPIObservationVariable?) {
                    mSelected = variable
                    notifyDataSetChanged()
                }

                override fun getView(position: Int, convertView: View?, parent: ViewGroup): View {
                    val binding = if (convertView == null) {
                        DataBindingUtil.inflate<ListItemBrapiObservationVariableBinding>(
                            LayoutInflater.from(context),
                            R.layout.list_item_brapi_observation_variable,
                            parent,
                            false
                        )
                    } else {
                        DataBindingUtil.getBinding<ListItemBrapiObservationVariableBinding>(convertView)
                            ?: DataBindingUtil.inflate(
                                LayoutInflater.from(context),
                                R.layout.list_item_brapi_observation_variable,
                                parent,
                                false
                            )
                    }

                    val item = getItem(position)
                    binding.title = item?.observationVariableName ?: ""
                    binding.description = variableDescription(item)
                    binding.variableId =
                        item?.observationVariableDbId ?: getString(R.string.brapi_project_id_unavailable)
                    binding.isSelected =
                        item?.observationVariableDbId != null &&
                        item.observationVariableDbId == mSelected?.observationVariableDbId
                    binding.root.setOnClickListener {
                        selectVariable(item)
                    }
                    binding.selectedRadioButton.setOnClickListener {
                        selectVariable(item)
                    }
                    binding.executePendingBindings()
                    return binding.root
                }
            }

            listView.setOnItemClickListener { _, _, position, _ ->
                adapter.getItem(position)?.let { variable ->
                    mSelected = variable
                    adapter.notifyDataSetChanged()
                }
            }
            listView.choiceMode = ListView.CHOICE_MODE_SINGLE
            listView.adapter = adapter
        }
    }

    override fun FragmentBrapiImportParentsPickerBinding.afterCreateView() {
        activity?.let { act ->
            (activity as? MainActivity)?.applyFragmentInsets(root, toolbar as? Toolbar)
            if (!isConnected(act.applicationContext)) {
                Toast.makeText(act.applicationContext, R.string.device_offline_warning, Toast.LENGTH_SHORT).show()
                findNavController().popBackStack()
                return
            }
            if (!BrAPIService.hasValidBaseUrl(act.applicationContext)) {
                Toast.makeText(act.applicationContext, R.string.brapi_must_configure_url, Toast.LENGTH_SHORT).show()
                findNavController().popBackStack()
                return
            }
            serverTextView.text = BrAPIService.getBrapiUrl(act)
            setupPageController()
            setupToolbarAndButtons()
            loadBrAPIData()
        }
    }
}
