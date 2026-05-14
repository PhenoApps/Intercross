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
import org.brapi.v2.model.core.BrAPIStudy
import org.phenoapps.intercross.R
import org.phenoapps.intercross.activities.MainActivity
import org.phenoapps.intercross.brapi.service.BrAPIService
import org.phenoapps.intercross.brapi.service.BrAPIServiceV2
import org.phenoapps.intercross.brapi.service.BrapiPaginationManager
import org.phenoapps.intercross.databinding.FragmentBrapiImportParentsPickerBinding
import org.phenoapps.intercross.databinding.ListItemBrapiStudyBinding
import org.phenoapps.intercross.fragments.IntercrossBaseFragment

class BrapiImportParentsStudyFragment :
    IntercrossBaseFragment<FragmentBrapiImportParentsPickerBinding>(R.layout.fragment_brapi_import_parents_picker) {

    private val args: BrapiImportParentsStudyFragmentArgs by navArgs()

    private companion object {
        val TAG: String = BrapiImportParentsStudyFragment::class.java.simpleName
    }

    private val mService: BrAPIServiceV2 by lazy { BrAPIServiceV2(requireContext()) }

    private val mPaginationManager by lazy { BrapiPaginationManager(requireActivity()) }

    private var mSelected: BrAPIStudy? = null

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
        mBinding.toolbar.title = getString(R.string.brapi_import_parents_study_title)
        mBinding.hintTextView.text = getString(R.string.brapi_import_parents_study_hint)
        mBinding.nextButton.text = getString(R.string.brapi_import_parents_continue_variable)
        mBinding.nextButton.setOnClickListener {
            goToObservationVariableStep()
        }
        mBinding.skipObservationSearchButton.setOnClickListener {
            navigateToPotentialParents(studyDbId = "", observationVariableDbId = "")
        }
    }

    private fun goToObservationVariableStep() {
        val study = mSelected
        findNavController().navigate(
            BrapiImportParentsStudyFragmentDirections
                .actionBrapiImportParentsStudyToBrapiImportParentsObservationVariable(
                    programDbId = "", //args.programDbId,
                    crossingProjectDbId = args.crossingProjectDbId,
                    crossingProjectName = args.crossingProjectName,
                    crossingProjectDescription = args.crossingProjectDescription,
                    programName = args.programName,
                    commonCropName = args.commonCropName,
                    parentCount = args.parentCount,
                    potentialParentsJson = args.potentialParentsJson,
                    prefilledObservationVariableDbId = args.prefilledObservationVariableDbId,
                    selectedStudyDbId = study?.studyDbId ?: "",
                    selectedStudyName = study?.studyName ?: ""
                )
        )
    }

    private fun navigateToPotentialParents(studyDbId: String, observationVariableDbId: String) {
        findNavController().navigate(
            BrapiImportParentsStudyFragmentDirections.actionBrapiImportParentsStudyToBrapiPotentialParentsFragment(
                programDbId = args.programDbId,
                crossingProjectDbId = args.crossingProjectDbId,
                crossingProjectName = args.crossingProjectName,
                crossingProjectDescription = args.crossingProjectDescription,
                programName = args.programName,
                commonCropName = args.commonCropName,
                parentCount = args.parentCount,
                potentialParentsJson = args.potentialParentsJson,
                studyDbId = studyDbId,
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
                val studies = mService.awaitStudies(args.programDbId, mPaginationManager)
                val filtered = studies
//                    .filter {
//                    (it.studyName?.isNotEmpty() == true) && it.studyDbId != null
//                }
                applyPreselection(filtered)
                buildArrayAdapter(filtered)
                mBinding.pageController.visibility =
                    if (mPaginationManager.totalPages > 1) View.VISIBLE else View.GONE
            } catch (error: BrapiRequestException) {
                handleFailure(error.code)
                Toast.makeText(requireContext(), R.string.brapi_import_parents_study_load_error, Toast.LENGTH_LONG).show()
            } catch (cme: ConcurrentModificationException) {
                Log.d(TAG, cme.localizedMessage ?: "Async update error.")
                cme.printStackTrace()
            } finally {
                mBinding.progressVisibility = null
            }
        }
    }

    private fun applyPreselection(studies: List<BrAPIStudy>) {
        val pref = args.prefilledStudyDbId
        if (pref.isBlank()) return
        mSelected = studies.find { it.studyDbId == pref }
    }

    private fun buildArrayAdapter(data: List<BrAPIStudy>) {
        val listView = mBinding.listView
        val ctx = context ?: return
        activity?.runOnUiThread {
            val adapter = object : ArrayAdapter<BrAPIStudy>(
                ctx,
                R.layout.list_item_brapi_study,
                data
            ) {
                private fun selectStudy(study: BrAPIStudy?) {
                    mSelected = study
                    notifyDataSetChanged()
                }

                override fun getView(position: Int, convertView: View?, parent: ViewGroup): View {
                    val binding = if (convertView == null) {
                        DataBindingUtil.inflate<ListItemBrapiStudyBinding>(
                            LayoutInflater.from(context),
                            R.layout.list_item_brapi_study,
                            parent,
                            false
                        )
                    } else {
                        DataBindingUtil.getBinding<ListItemBrapiStudyBinding>(convertView)
                            ?: DataBindingUtil.inflate(
                                LayoutInflater.from(context),
                                R.layout.list_item_brapi_study,
                                parent,
                                false
                            )
                    }

                    val item = getItem(position)
                    binding.title = item?.studyName ?: ""
                    binding.description = item?.studyDescription ?: ""
                    binding.studyId = item?.studyDbId ?: getString(R.string.brapi_project_id_unavailable)
                    binding.trialName = item?.trialName?.takeIf { it.isNotBlank() }
                        ?: getString(R.string.brapi_project_value_unavailable)
                    binding.isSelected = item?.studyDbId != null && item.studyDbId == mSelected?.studyDbId
                    binding.root.setOnClickListener {
                        selectStudy(item)
                    }
                    binding.selectedRadioButton.setOnClickListener {
                        selectStudy(item)
                    }
                    binding.executePendingBindings()
                    return binding.root
                }
            }

            listView.setOnItemClickListener { _, _, position, _ ->
                adapter.getItem(position)?.let { study ->
                    mSelected = study
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
