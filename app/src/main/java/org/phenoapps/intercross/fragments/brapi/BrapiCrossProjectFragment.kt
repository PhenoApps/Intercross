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
import androidx.databinding.DataBindingUtil
import androidx.lifecycle.lifecycleScope
import androidx.navigation.fragment.findNavController
import androidx.navigation.fragment.navArgs
import com.google.gson.Gson
import kotlinx.coroutines.launch
import org.brapi.v2.model.germ.BrAPICrossingProject
import org.phenoapps.intercross.R
import org.phenoapps.intercross.activities.MainActivity
import org.phenoapps.intercross.brapi.service.BrAPIService
import org.phenoapps.intercross.brapi.service.BrAPIServiceV2
import org.phenoapps.intercross.brapi.service.BrapiPaginationManager
import org.phenoapps.intercross.databinding.FragmentBrapiCrossProjectsBinding
import org.phenoapps.intercross.databinding.ListItemCrossingProjectBinding
import org.phenoapps.intercross.fragments.IntercrossBaseFragment
import org.phenoapps.intercross.util.ImportUtil

class BrapiCrossProjectFragment: IntercrossBaseFragment<FragmentBrapiCrossProjectsBinding>(R.layout.fragment_brapi_cross_projects) {

    private val args: BrapiCrossProjectFragmentArgs by navArgs()

    private companion object {

        val TAG = BrapiCrossProjectFragment::class.simpleName

        const val CROSSPROJECTS = 0
    }

    /**
     * mFilterState is a state variable that represents which table we are filtering for.
     */
    private var mFilterState: Int = CROSSPROJECTS

    /**
     * Whenever a list row is chosen, it is added to a set of ids (or removed if already chosen).
     * When the next table is chosen, all the ids in the respective set will be used to query.
     */
    private var mProjects: BrAPICrossingProject? = null

    private val mService: BrAPIServiceV2 by lazy {

        BrAPIServiceV2(this@BrapiCrossProjectFragment.context)

    }

    private val mPaginationManager by lazy {

        BrapiPaginationManager(context)

    }

    private fun setupPageController() {

        with (mBinding.pageController) {

            this.setNextClick {

                mPaginationManager.setNewPage(nextButton.id)

                loadBrAPIData()

            }

            this.setPrevClick {

                mPaginationManager.setNewPage(prevButton.id)

                loadBrAPIData()

            }
        }
    }

    private fun importSelected() {
        val crossProject = mProjects
        if (crossProject == null) {
            Toast.makeText(context, R.string.brapi_warning_select_crossing_project, Toast.LENGTH_SHORT).show()
            return
        }

        val direction = when (args.mode) {
            ImportUtil.BRAPI_MODE_PARENTS -> BrapiCrossProjectFragmentDirections
                .actionBrapiCrossProjectImportFragmentToBrapiPotentialParentsFragment(
                    programDbId = crossProject.programDbId,
                    crossingProjectDbId = crossProject.crossingProjectDbId ?: "",
                    crossingProjectName = crossProject.crossingProjectName ?: "",
                    crossingProjectDescription = crossProject.crossingProjectDescription ?: "",
                    programName = crossProject.programName ?: "",
                    commonCropName = crossProject.commonCropName ?: "",
                    parentCount = crossProject.potentialParents?.size ?: 0,
                    potentialParentsJson = crossProject.potentialParents
                        ?.mapNotNull { parent ->
                            runCatching {
                                Gson().toJson(parent)
                            }.getOrNull()
                        }
                        ?.toTypedArray()
                        ?: emptyArray()
                )
            ImportUtil.BRAPI_MODE_IMPORT_CROSSES -> BrapiCrossProjectFragmentDirections
                .actionBrapiCrossProjectImportFragmentToBrapiCrossImportFragment(
                    crossingProjectDbId = crossProject.crossingProjectDbId ?: "",
                    crossingProjectName = crossProject.crossingProjectName ?: "",
                    crossingProjectDescription = crossProject.crossingProjectDescription ?: "",
                    programName = crossProject.programName ?: "",
                    commonCropName = crossProject.commonCropName ?: "",
                    parentCount = crossProject.potentialParents?.size ?: 0
                )
            ImportUtil.BRAPI_MODE_EXPORT_CROSSES -> BrapiCrossProjectFragmentDirections
                .actionToExportBrapiCrosses(
                    crossingProjectDbId = crossProject.crossingProjectDbId ?: "",
                    crossingProjectName = crossProject.crossingProjectName ?: "",
                    crossingProjectDescription = crossProject.crossingProjectDescription ?: "",
                    programName = crossProject.programName ?: "",
                    commonCropName = crossProject.commonCropName ?: "",
                    parentCount = crossProject.potentialParents?.size ?: 0
                )
            else -> BrapiCrossProjectFragmentDirections //wishlist
                .actionBrapiCrossProjectImportFragmentToBrapiPlannedCrossesFragment(
                    crossingProjectDbId = crossProject.crossingProjectDbId ?: "",
                    crossingProjectName = crossProject.crossingProjectName ?: "",
                    crossingProjectDescription = crossProject.crossingProjectDescription ?: "",
                    programName = crossProject.programName ?: "",
                    commonCropName = crossProject.commonCropName ?: "",
                    parentCount = crossProject.potentialParents?.size ?: 0
                )

        }

        findNavController().navigate(direction)
    }

    /**
     * For this import activity, the top bar shows the current table we are filtering for,
     * which navigates to the previous table on click (like a back button).
     * Similarly, the bottom bar moves to the next table but is like a select all query if no fields are chosen.
     */
    private fun setupTopAndBottomButtons() {

        mBinding.toolbar.setNavigationOnClickListener {
            findNavController().popBackStack()
        }

        mBinding.nextButton.setOnClickListener {

            when(mFilterState) {
                CROSSPROJECTS -> importSelected()
            }

           loadBrAPIData()
        }

    }

    /**
     * TODO: This is what field book uses, might need to be updated
     */
    @SuppressLint("MissingPermission")
    private fun isConnected(context: Context): Boolean {

        val connMgr = context.getSystemService(Context.CONNECTIVITY_SERVICE) as ConnectivityManager

        val networkInfo = connMgr.activeNetworkInfo

        return networkInfo != null && networkInfo.isConnected
    }

    /**
     * Updates the top/bottom bar text fields with the current state.
     */
    private fun updateUi(state: Int) {

        mFilterState = state

        mBinding.toolbar.title = getString(R.string.brapi_cross_projects_title)

        mBinding.nextButton.text =
            if (args.mode == ImportUtil.BRAPI_MODE_PARENTS) {
                getString(R.string.brapi_review_parents)
            } else {
                getString(R.string.brapi_import_selected)
            }
    }

    private fun loadCrossingProjects() {
        mBinding.listView.choiceMode = ListView.CHOICE_MODE_SINGLE
    }

    /**
     * Logs each time a failure api callback occurs
     */
    private fun handleFailure(fail: Int): Void? {

        Log.d(TAG, "BrAPI callback failed. $fail")

        return null //default fail callback return type for brapi

    }

    //async loads the brapi calls using coroutines
    //updates the top/bottom bar text fields depending on the state
    private fun loadBrAPIData() {

        updateUi(mFilterState)
        mBinding.progressVisibility = View.VISIBLE
        lifecycleScope.launch {

            try {

                when (mFilterState) {
                    CROSSPROJECTS -> {
                        loadCrossingProjects()
                        val crosses = mService.awaitCrossingProjects(mPaginationManager)
                        val filtered = crosses.filter {
                            it.crossingProjectName?.isNotEmpty() ?: false && it.crossingProjectDbId != null
                        }
                        buildArrayAdapter(filtered)
                    }
                }

                mBinding.pageController.visibility = if (mPaginationManager.totalPages > 1) View.VISIBLE else View.GONE

            } catch (error: BrapiRequestException) {
                handleFailure(error.code)
            } catch (cme: ConcurrentModificationException) {
                Log.d(TAG, cme.localizedMessage ?: "Async update error.")
                cme.printStackTrace()
            } finally {
                mBinding.progressVisibility = null
            }
        }
    }

    /**
     * Updates the UI with the data parameter. Type T can be BrapiProgram, BrapiStudyDetails,
     * or ProgramTrialPair. All of which contain information to reconstruct the filter tree
     * from user input.
     */
    private fun buildArrayAdapter(data: List<BrAPICrossingProject>) {

        val listView = mBinding.listView

        context?.let { ctx ->

            this@BrapiCrossProjectFragment.activity?.runOnUiThread {

                val adapter = object : ArrayAdapter<BrAPICrossingProject>(
                    ctx,
                    R.layout.list_item_crossing_project,
                    data
                ) {
                    private fun selectProject(project: BrAPICrossingProject?) {
                        mProjects = project
                        notifyDataSetChanged()
                    }

                    override fun getView(position: Int, convertView: View?, parent: ViewGroup): View {
                        val binding = if (convertView == null) {
                            DataBindingUtil.inflate<ListItemCrossingProjectBinding>(
                                LayoutInflater.from(context),
                                R.layout.list_item_crossing_project,
                                parent,
                                false
                            )
                        } else {
                            DataBindingUtil.getBinding<ListItemCrossingProjectBinding>(convertView)
                                ?: DataBindingUtil.inflate(
                                    LayoutInflater.from(context),
                                    R.layout.list_item_crossing_project,
                                    parent,
                                    false
                                )
                        }

                        val item = getItem(position)
                        binding.title = item?.crossingProjectName ?: ""
                        binding.description = item?.crossingProjectDescription
                            ?: item?.additionalInfo?.toString()
                            ?: ""
                        binding.programName = item?.programName ?: ""
                        binding.commonCropName = item?.commonCropName ?: ""
                        binding.parentCount = item?.potentialParents?.size ?: 0
                        binding.projectId = item?.crossingProjectDbId ?: ""
                        binding.isSelected = item?.crossingProjectDbId == mProjects?.crossingProjectDbId
                        binding.root.setOnClickListener {
                            selectProject(item)
                        }
                        binding.selectedRadioButton.setOnClickListener {
                            selectProject(item)
                        }
                        binding.executePendingBindings()

                        return binding.root
                    }
                }

                listView.setOnItemClickListener { _, _, position, _ ->
                    adapter.getItem(position)?.let { project ->
                        mProjects = project
                        adapter.notifyDataSetChanged()
                    }
                }

                listView.adapter = adapter

            }
        }
    }

    override fun FragmentBrapiCrossProjectsBinding.afterCreateView() {

        activity?.let { act ->

            (activity as? MainActivity)?.applyFragmentInsets(root, toolbar)

            //check if device is connected to a network
            if (isConnected(act.applicationContext)) {

                //checks that the preference brapi url matches a web url
                if (BrAPIService.hasValidBaseUrl(act.applicationContext)) {

                    mBinding.serverTextView.text = BrAPIService.getBrapiUrl(act)

                    setupPageController()

                    setupTopAndBottomButtons()

                    loadBrAPIData()

                } else {

                    Toast.makeText(act.applicationContext, R.string.brapi_must_configure_url, Toast.LENGTH_SHORT).show()

                    findNavController().popBackStack()
                }

            } else {

                Toast.makeText(act.applicationContext, R.string.device_offline_warning, Toast.LENGTH_SHORT).show()

                findNavController().popBackStack()
            }
        }
    }
}