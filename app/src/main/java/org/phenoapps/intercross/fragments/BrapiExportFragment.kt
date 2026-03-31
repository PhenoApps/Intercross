package org.phenoapps.intercross.fragments

import android.annotation.SuppressLint
import android.content.Context
import android.net.ConnectivityManager
import android.util.Log
import android.view.Menu
import android.view.MenuInflater
import android.view.MenuItem
import android.view.View
import android.widget.ArrayAdapter
import android.widget.ListView
import android.widget.Toast
import androidx.fragment.app.viewModels
import androidx.navigation.fragment.findNavController
import com.google.gson.JsonObject
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import org.brapi.v2.model.BrAPIExternalReference
import org.brapi.v2.model.germ.BrAPICross
import org.brapi.v2.model.germ.BrAPICrossParent
import org.brapi.v2.model.germ.BrAPICrossType
import org.brapi.v2.model.germ.BrAPICrossingProject
import org.brapi.v2.model.germ.BrAPIGermplasm
import org.brapi.v2.model.germ.BrAPIParentType
import org.brapi.v2.model.pheno.BrAPIObservationUnit
import org.phenoapps.intercross.R
import org.phenoapps.intercross.brapi.model.BrapiTrial
import org.phenoapps.intercross.brapi.service.BrAPIService
import org.phenoapps.intercross.brapi.service.BrAPIServiceV2
import org.phenoapps.intercross.brapi.service.BrapiPaginationManager
import org.phenoapps.intercross.data.EventsRepository
import org.phenoapps.intercross.data.ParentsRepository
import org.phenoapps.intercross.data.WishlistRepository
import org.phenoapps.intercross.data.models.CrossType
import org.phenoapps.intercross.data.models.Wishlist
import org.phenoapps.intercross.data.viewmodels.EventListViewModel
import org.phenoapps.intercross.data.viewmodels.ParentsListViewModel
import org.phenoapps.intercross.data.viewmodels.WishlistViewModel
import org.phenoapps.intercross.data.viewmodels.factory.EventsListViewModelFactory
import org.phenoapps.intercross.data.viewmodels.factory.ParentsListViewModelFactory
import org.phenoapps.intercross.data.viewmodels.factory.WishlistViewModelFactory
import org.phenoapps.intercross.databinding.FragmentBrapiImportBinding

class BrapiExportFragment: IntercrossBaseFragment<FragmentBrapiImportBinding>(R.layout.fragment_brapi_import) {

    private val viewModel: EventListViewModel by viewModels {
        EventsListViewModelFactory(EventsRepository.getInstance(db.eventsDao()))
    }

    private val wishViewModel: WishlistViewModel by viewModels {
        WishlistViewModelFactory(WishlistRepository.getInstance(db.wishlistDao()))
    }

    private val parentsViewModel: ParentsListViewModel by viewModels {
        ParentsListViewModelFactory(ParentsRepository.getInstance(db.parentsDao()))
    }

    companion object {

        val TAG = BrapiExportFragment::class.simpleName

        const val CROSSPROJECTS = 0
        const val PLANNEDCROSSES = 1
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
    //private var mPlannedCrosses = HashSet<BrAPIPlannedCross>()
    private var mGermplasm: BrAPIGermplasm? = null
    private var mTrial: BrapiTrial? = null
    private var mObservationUnits: List<BrAPIObservationUnit>? = null
    private val mScope by lazy {
        CoroutineScope(Dispatchers.IO)
    }

    private val mService: BrAPIServiceV2 by lazy {

        BrAPIServiceV2(this@BrapiExportFragment.context)

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

    /**
     * For this import activity, the top bar shows the current table we are filtering for,
     * which navigates to the previous table on click (like a back button).
     * Similarly, the bottom bar moves to the next table but is like a select all query if no fields are chosen.
     */
    private fun setupTopAndBottomButtons() {

        mBinding.currentButton.setOnClickListener {
            //update the state (go back one)
            //reset respective sets
            when(mFilterState) {
                CROSSPROJECTS -> findNavController().popBackStack()
            }

            //update UI with the new state
            loadBrAPIData()
        }

        mBinding.nextButton.setOnClickListener {
            //update the state (go forward one)
            when(mFilterState) {
                CROSSPROJECTS -> exportSelected()

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

        mBinding.currentButton.text = "Cross Projects"

        mBinding.nextButton.text = "Export Selected"

    }

    private fun getGermplasmList() {

        mService.getGermplasm(mPaginationManager, { germs ->

            mGermplasm = germs.firstOrNull()

            null

        }) { fail -> handleFailure(fail) }
    }

    private fun getTrialList() {

        mService.searchTrials(listOf(), mPaginationManager, { trials ->

            mTrial = trials.firstOrNull()

            null

        }) { fail -> handleFailure(fail) }
    }

    private fun getObservationUnits(parents: List<String>) {

        mService.searchObservationUnits(parents, mPaginationManager, { units ->

            mObservationUnits = units

            null

        }) { fail -> handleFailure(fail) }
    }

    private fun postObservationUnits(observations: List<BrAPIObservationUnit>) {

        mService.postObservationUnits(observations, { success ->
            null
        }) { fail -> handleFailure(fail) }
    }

    /**
     * Crosses get call might need a cross project db id parameter
     */
    private fun loadCrossingProjects() {

        mPaginationManager.reset()

        mBinding.listView.choiceMode = ListView.CHOICE_MODE_SINGLE

        mService.getCrossingProjects(mPaginationManager, { crosses ->

            mBinding.progressVisibility = View.GONE

            buildArrayAdapter(crosses.filter { it.crossingProjectName?.isNotEmpty() ?: false && it.crossingProjectDbId != null })

            null

        }) { fail ->

            mBinding.progressVisibility = View.GONE

            //todo toast

            handleFailure(fail)
        }
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

        mScope.launch { //uses Dispatchers.IO for network background processing

            try {

                when (mFilterState) {
                    CROSSPROJECTS -> loadCrossingProjects()
                    //PLANNEDCROSSES -> loadPlannedCrosses()
                }

            } catch (cme: ConcurrentModificationException) {

                Log.d(TAG, cme.localizedMessage ?: "Async update error.")

                cme.printStackTrace()

            }
        }
    }

    //function used to toggle existence of an item in a set
    private fun <T> HashSet<T>.addOrRemove(item: T) {
        if (this.contains(item)) {
            this.remove(item)
        } else this.add(item)
    }

    /**
     * Updates the UI with the data parameter. Type T can be BrapiProgram, BrapiStudyDetails,
     * or ProgramTrialPair. All of which contain information to reconstruct the filter tree
     * from user input.
     */
    private fun <T> buildArrayAdapter(data: List<T>) {

        val listView = mBinding.listView

        //set up list item click event listener
        listView.setOnItemClickListener { _, _, position, _ ->

            when (val item = data[position]) {

                is BrAPICrossingProject -> mProjects = item
                //is BrAPIPlannedCross -> mPlannedCrosses.addOrRemove(item)

            }
        }

        val itemDataList: MutableList<Any?> = ArrayList()

        //load data into adapter
        data.forEach {

            when (it) {

                is BrAPICrossingProject -> it.crossingProjectName?.let { name -> itemDataList.add(name) }
                //is BrAPIPlannedCross -> it.plannedCrossName?.let { name -> itemDataList.add(name) }

            }
        }

        context?.let { ctx ->

            this@BrapiExportFragment.activity?.runOnUiThread {

                listView.adapter = ArrayAdapter(ctx, android.R.layout.simple_list_item_single_choice, itemDataList)

            }
        }
    }

    //Intercross types: BIPARENTAL, OPEN, SELF, POLY, UNKNOWN
    private fun CrossType.toBrAPICrossType(): BrAPICrossType = when(this) {
        CrossType.BIPARENTAL -> BrAPICrossType.BIPARENTAL
        CrossType.OPEN -> BrAPICrossType.OPEN_POLLINATED
        CrossType.POLY -> BrAPICrossType.BULK_OPEN_POLLINATED
        else -> BrAPICrossType.SELF
       // CrossType.UNKNOWN -> BrAPICrossType.
    }

    /**
     * BrAPI Cross Parent:
        private String germplasmDbId = null;
        private String germplasmName = null;
        private String observationUnitDbId = null;
        private String observationUnitName = null;
        private BrAPIParentType parentType = null;
     */
    private fun Wishlist.toBrAPICross(sex: Int, brapiIds: List<BrAPIObservationUnit>) = BrAPICrossParent().apply {
        //this.germplasmDbId = germplasm.germplasmDbId
        //this.germplasmName = germplasm.germplasmName
        val reference = BrAPIExternalReference().apply {
            referenceID = if (sex == 0) this@toBrAPICross.femaleDbId else this@toBrAPICross.maleDbId
        }
        val brapUnit = brapiIds
            .find {  reference in it.externalReferences }

        this.observationUnitDbId = brapUnit?.observationUnitDbId
        this.observationUnitName = brapUnit?.observationUnitName
        this.parentType = if (sex == 0) BrAPIParentType.FEMALE else BrAPIParentType.MALE

    }

    fun Wishlist.toParent(sex: Int) = BrAPIObservationUnit().apply {

        externalReferences = listOf(BrAPIExternalReference().apply {
            referenceID = if (sex == 1) maleDbId else femaleDbId
        })
        //observationUnitDbId = created by brapi
        observationUnitName = if (sex == 1) maleName else femaleName

    }


    //transform wishlist to observation units
    fun List<Wishlist>.toParents(): List<BrAPIObservationUnit> = this.map {
        listOf(it.toParent(0), it.toParent(1))
    }.flatten()


//    /**
//     * This is a method for developers to post crossing projects / planned crosses / and required obs. units from the local wishlist
//     * for the selected cross project, post or put planned crosses from the local wishlist
//     * handle ETL for intercross -> brapi
//     */
//    private fun exportSelected() {
//
//        mProjects?.let { crossProject ->
//
//            mGermplasm?.let { germ ->
//
//                //get local wishlist
//                wishViewModel.wishlist.observe(viewLifecycleOwner, { wishlist ->
//
//                    //transform wishlist to observation units list
//                    val parents = wishlist.toParents()
//
//                    //(first search if they exist, post required observation units)
//                    mService.postObservationUnits(parents, { success ->
//
//                        //post planned crosses, map wishlist ids to brapi external ids
//                        mService.postPlannedCross(wishlist.map { wish ->
//
//                            /**
//                            private String plannedCrossDbId = null;
//                            private Map<String, String> additionalInfo = null;
//                            private BrAPICrossType crossType = null;
//                            private String crossingProjectDbId = null;
//                            private String crossingProjectName = null;
//                            private List<BrAPIExternalReference> externalReferences = null;
//                            private BrAPICrossParent parent1 = null;
//                            private BrAPICrossParent parent2 = null;
//                             */
//                            BrAPIPlannedCross().apply {
//                                externalReferences = listOf(BrAPIExternalReference().apply {
//                                    referenceID = UUID.randomUUID().toString()
//                                })
//                                //this.plannedCrossDbId = UUID.randomUUID().toString()
//                                //this.plannedCrossName = "Test"
//                                //load wish metadata into additional info map
//                                this.additionalInfo = mapOf("wishMin" to wish.wishMin.toString(),
//                                    "wishMax" to (wish.wishMax ?: 10).toString(),
//                                    "wishType" to wish.wishType)
//                                crossingProjectDbId = crossProject.crossingProjectDbId
//                                crossingProjectName = crossProject.crossingProjectName
//                                parent1 = wish.toBrAPICross(0, success)
//                                parent2 = wish.toBrAPICross(1, success)
//                                crossType = BrAPICrossType.BIPARENTAL
//                            }
//                        }, { successList ->
//
//                            null
//                        }) { fail -> handleFailure(fail) }
//
//                        null
//                    }) { fail -> handleFailure(fail) }
//
//                    //leave the fragment after the wishlist upload begins
//                    findNavController().popBackStack()
//
//                })
//            }
//        }
//    }

    /**
     * Post crosses to brapi
     */
    private fun exportSelected() {

        mProjects?.let { crossProject ->

            parentsViewModel.parents.observe(viewLifecycleOwner) { parents ->

                viewModel.events.observe(viewLifecycleOwner) { crosses ->

                    mBinding.progressVisibility = View.VISIBLE

                    mService.postCrosses(crosses.map { cross ->
                        BrAPICross().apply {
                            //this.crossDbId =
                            externalReferences = listOf(BrAPIExternalReference().apply {
                                referenceID = cross.eventDbId
                            })
                            //this.plannedCrossDbId = UUID.randomUUID().toString()
                            //this.plannedCrossName = "Test"
                            //load wish metadata into additional info map
                            this.additionalInfo = JsonObject()
                            this.crossAttributes = listOf()
                            //this.crossName =
                            this.crossType = cross.type.toBrAPICrossType()
                            crossingProjectDbId = crossProject.crossingProjectDbId
                            crossingProjectName = crossProject.crossingProjectName
                            parent1 = BrAPICrossParent().apply {
                                this.observationUnitDbId = cross.femaleObsUnitDbId
                                parents.find { it.codeId == cross.femaleObsUnitDbId }
                                    ?.let { parent ->
                                        this.observationUnitName = parent.name
                                        this.parentType =
                                            if (parent.sex == 0) BrAPIParentType.FEMALE else BrAPIParentType.MALE

                                    }
                            }
                            parent2 = BrAPICrossParent().apply {
                                this.observationUnitDbId = cross.maleObsUnitDbId
                                parents.find { it.codeId == cross.maleObsUnitDbId }?.let { parent ->
                                    this.observationUnitName = parent.name
                                    this.parentType =
                                        if (parent.sex == 0) BrAPIParentType.FEMALE else BrAPIParentType.MALE
                                }
                            }
                            //this.pollinationTimeStamp =

                        }

                    }, { _ ->

                        activity?.runOnUiThread {

                            mBinding.progressVisibility = View.GONE

                            findNavController().popBackStack()
                        }

                        null
                    }) { fail ->

                        activity?.runOnUiThread {

                            mBinding.progressVisibility = View.GONE

                            Toast.makeText(
                                context,
                                getString(R.string.fragment_brapi_export_crosses_failed),
                                Toast.LENGTH_SHORT
                            ).show()

                        }

                        handleFailure(fail)
                    }
                }
            }
        }
    }

    override fun FragmentBrapiImportBinding.afterCreateView() {

        activity?.let { act ->

            //check if device is connected to a network
            if (isConnected(act.applicationContext)) {

                //checks that the preference brapi url matches a web url
                if (BrAPIService.hasValidBaseUrl(act.applicationContext)) {

                    mBinding.serverTextView.text = BrAPIService.getBrapiUrl(act)

                    setupPageController()

                    setupTopAndBottomButtons()

                    getTrialList()
                    getGermplasmList()

                    loadBrAPIData()

                } else {

                    Toast.makeText(act.applicationContext, R.string.brapi_must_configure_url, Toast.LENGTH_SHORT).show()

                    findNavController().popBackStack()
                }

            } else {

                Toast.makeText(act.applicationContext, R.string.device_offline_warning, Toast.LENGTH_SHORT).show()

                findNavController().popBackStack()
            }

            setHasOptionsMenu(true)
        }
    }

    override fun onCreateOptionsMenu(menu: Menu, inflater: MenuInflater) {
        super.onCreateOptionsMenu(menu, inflater)
        inflater.inflate(R.menu.menu_export_brapi, menu)
    }

    override fun onOptionsItemSelected(item: MenuItem): Boolean {

        when(item.itemId) {
            R.id.action_new_crossing_project -> {
                activity?.let { act ->
                    findNavController().navigate(BrapiExportFragmentDirections
                        .actionToProjectCreator())
                }
            }
        }
        return super.onOptionsItemSelected(item)
    }
}