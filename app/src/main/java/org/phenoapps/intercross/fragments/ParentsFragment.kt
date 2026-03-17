package org.phenoapps.intercross.fragments

import android.content.SharedPreferences
import android.content.pm.PackageManager
import android.os.Build
import android.os.Bundle
import android.view.MenuItem
import android.view.View
import android.widget.AdapterView
import android.widget.TextView
import android.widget.Toast
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AlertDialog
import androidx.fragment.app.viewModels
import androidx.navigation.Navigation
import androidx.navigation.fragment.findNavController
import androidx.recyclerview.widget.LinearLayoutManager
import com.google.android.material.chip.ChipGroup
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.MainScope
import org.phenoapps.intercross.R
import org.phenoapps.intercross.activities.MainActivity
import org.phenoapps.intercross.adapters.ParentsAdapter
import org.phenoapps.intercross.data.EventsRepository
import org.phenoapps.intercross.data.ParentsRepository
import org.phenoapps.intercross.data.PollenGroupRepository
import org.phenoapps.intercross.data.models.BaseParent
import org.phenoapps.intercross.data.models.Event
import org.phenoapps.intercross.data.models.Parent
import org.phenoapps.intercross.data.models.PollenGroup
import org.phenoapps.intercross.data.viewmodels.EventListViewModel
import org.phenoapps.intercross.data.viewmodels.ParentsListViewModel
import org.phenoapps.intercross.data.viewmodels.PollenGroupListViewModel
import org.phenoapps.intercross.data.viewmodels.factory.EventsListViewModelFactory
import org.phenoapps.intercross.data.viewmodels.factory.ParentsListViewModelFactory
import org.phenoapps.intercross.data.viewmodels.factory.PollenGroupListViewModelFactory
import org.phenoapps.intercross.databinding.FragmentParentsBinding
import org.phenoapps.intercross.dialogs.ListAddDialog
import org.phenoapps.intercross.util.BluetoothUtil
import org.phenoapps.intercross.util.Dialogs
import org.phenoapps.intercross.util.ImportUtil
import org.phenoapps.intercross.util.KeyUtil
import javax.inject.Inject

@AndroidEntryPoint
class ParentsFragment: IntercrossBaseFragment<FragmentParentsBinding>(R.layout.fragment_parents),
    CoroutineScope by MainScope() {

    private val requestBluetoothPermissions = registerForActivityResult(ActivityResultContracts.RequestMultiplePermissions()) { granted ->

        granted?.let { grant ->

            if (grant.filter { it.value == false }.isNotEmpty()) {

                Toast.makeText(context, R.string.error_no_bluetooth_permission, Toast.LENGTH_SHORT).show()
            }
        }
    }

    private val viewModel: ParentsListViewModel by viewModels {
        ParentsListViewModelFactory(ParentsRepository.getInstance(db.parentsDao()))
    }

    private val eventsModel: EventListViewModel by viewModels {
        EventsListViewModelFactory(EventsRepository.getInstance(db.eventsDao()))
    }

    private val groupList: PollenGroupListViewModel by viewModels {
        PollenGroupListViewModelFactory(PollenGroupRepository.getInstance(db.pollenGroupDao()))
    }

    private val parentList: ParentsListViewModel by viewModels {
        ParentsListViewModelFactory(ParentsRepository.getInstance(db.parentsDao()))
    }

    @Inject
    lateinit var mPref: SharedPreferences

    @Inject
    lateinit var mKeyUtil: KeyUtil

    private var mCrosses: List<Event> = ArrayList()
    private var femaleCrossCounts: Map<String, Int> = emptyMap()
    private var maleCrossCounts: Map<String, Int> = emptyMap()

    private lateinit var mMaleAdapter: ParentsAdapter

    private lateinit var mFemaleAdapter: ParentsAdapter

    private var mNextMaleSelection = true
    private var mNextFemaleSelection = true

    private val PERMISSIONS_REQUEST_STORAGE = 102

    private enum class SortType {
        NAME, ID, CROSSES
    }

    private var currentSortType = SortType.NAME

    //simple gesture listener to detect left and right swipes,
    //on a detected swipe the viewed gender will change
   // private val gestureListener = object : GestureDetector.SimpleOnGestureListener() {
   //
   //     override fun onFling(
   //         e1: MotionEvent?,
   //         e2: MotionEvent,
   //         velocityX: Float,
   //         velocityY: Float
   //     ): Boolean {
   //
   //         e1?.let {
   //
   //             val dx = e1.x - e2.x
   //             val x = abs(dx)
   //
   //             if (x in 100.0..1000.0) {
   //                 if (dx > 0) {
   //                     //swipe to left
   //                     mBinding.swipeLeft()
   //                 } else {
   //                     //swipe right
   //                     mBinding.swipeRight()
   //                 }
   //             }
   //
   //             return true
   //
   //         }
   //         return false
   //     }
   // }

    private fun updateCrossCounts(events: List<Event>) {
        femaleCrossCounts = events.groupingBy { it.femaleObsUnitDbId }.eachCount()
        maleCrossCounts = events.groupingBy { it.maleObsUnitDbId }.eachCount()

        if (::mFemaleAdapter.isInitialized) {
            mFemaleAdapter.notifyDataSetChanged()
        }

        if (::mMaleAdapter.isInitialized) {
            mMaleAdapter.notifyDataSetChanged()
        }
    }

    override fun FragmentParentsBinding.afterCreateView() {

        val ctx = requireContext()

        (activity as MainActivity).applyFragmentInsets(root, fragParentsTb)

        val tabFocus = arguments?.getInt("malesFirst") ?: 0

        viewModel.updateSelection(0)
        groupList.updateSelection(0)

        mMaleAdapter = ParentsAdapter(viewModel, groupList) { parent ->
            when (parent) {
                is Parent -> maleCrossCounts[parent.codeId] ?: 0
                is PollenGroup -> maleCrossCounts[parent.codeId] ?: 0
                else -> 0
            }
        }
        mFemaleAdapter = ParentsAdapter(viewModel, groupList) { parent ->
            when (parent) {
                is Parent -> femaleCrossCounts[parent.codeId] ?: 0
                is PollenGroup -> femaleCrossCounts[parent.codeId] ?: 0
                else -> 0
            }
        }

        femaleRecycler.adapter = mFemaleAdapter
        femaleRecycler.layoutManager = LinearLayoutManager(ctx)

        maleRecycler.adapter = mMaleAdapter
        maleRecycler.layoutManager = LinearLayoutManager(ctx)

        filterChipGroup.setOnCheckedStateChangeListener { _: ChipGroup, checkedIds: List<Int> ->
            when (checkedIds.firstOrNull()) {
                R.id.filter_female -> {
                    femaleRecycler.visibility = View.VISIBLE
                    maleRecycler.visibility = View.GONE
                    fragParentsSelectAllCb.isChecked = !mNextFemaleSelection
                }
                R.id.filter_male -> {
                    maleRecycler.visibility = View.VISIBLE
                    femaleRecycler.visibility = View.GONE
                    fragParentsSelectAllCb.isChecked = !mNextMaleSelection
                }
            }
            viewModel.parents.observe(viewLifecycleOwner) { parents ->
                groupList.groups.observe(viewLifecycleOwner) { groups ->
                    mBinding.updateSelectionText(
                        parents.filter { it.selected },
                        groups.filter { it.selected })
                }
            }
            updateNoDataVisibility()
        }

        /*
        On startup, select the chip matching the tabFocus argument (0 = female, 1 = male)
         */
        if (tabFocus == 1) filterMale.isChecked = true

        sortChipGroup.setOnCheckedStateChangeListener { _, checkedIds ->
            currentSortType = when (checkedIds.firstOrNull()) {
                R.id.sort_id -> SortType.ID
                R.id.sort_crosses -> SortType.CROSSES
                else -> SortType.NAME
            }
            // Re-submit the lists with new sort
            viewModel.parents.value?.let { parents ->
                groupList.groups.value?.let { groups ->
                    updateLists(parents, groups)
                }
            }
        }

        eventsModel.events.observe(viewLifecycleOwner) { parents ->

            parents?.let {

                mCrosses = it
                updateCrossCounts(it)

            }
        }

        viewModel.parents.observe(viewLifecycleOwner) { parents ->

            groupList.groups.observe(viewLifecycleOwner) { groups ->

                updateLists(parents, groups)

                mBinding.updateSelectionText(parents.filter { it.selected }, groups.filter { it.selected })

                updateNoDataVisibility()

            }
        }

        fragParentsNewParentBtn.setOnClickListener {

            showAddParentsDialog()

        }

        fragParentsDeleteFab.setOnClickListener {
            mBinding.deleteParents()
        }

        fragParentsPrintFab.setOnClickListener {
            mBinding.printParents()
        }

        fragParentsSelectAllCb.setOnClickListener {

            mBinding.selectAll()

        }

       // val gdc = GestureDetectorCompat(requireContext(), gestureListener)
       //
       // maleRecycler.setOnTouchListener { _, motionEvent ->
       //     gdc.onTouchEvent(motionEvent)
       // }
       //
       // femaleRecycler.setOnTouchListener { _, motionEvent ->
       //     gdc.onTouchEvent(motionEvent)
       // }

        bottomNavBar.selectedItemId = R.id.action_nav_parents

        setupBottomNavBar()

        setupToolbar()

    }

    private fun FragmentParentsBinding.setupToolbar() {

        //(activity as? AppCompatActivity)?.setSupportActionBar(mBinding.fragParentsTb)

        mBinding.fragParentsTb.setOnMenuItemClickListener {
            onOptionsItemSelected(it)
        }
    }

    private fun FragmentParentsBinding.updateSelectionText(parents: List<Parent>, groups: List<PollenGroup>? = null) {

        val selectedSex = if (filterFemale.isChecked) 0 else 1

        var count = parents.count { it.sex == selectedSex }
        if (selectedSex == 1) count += groups?.count() ?: 0

        val tv = fragParentsTb.findViewById<TextView>(R.id.frag_parents_toolbar_count_tv)

        when (count) {
            0 -> {
                tv.visibility = View.GONE
                updateMenuButtons(expanded = false)
            }
            else -> {
                tv.text = count.toString()
                tv.visibility = View.VISIBLE
                updateMenuButtons(expanded = true)
            }
        }
    }

    private fun updateMenuButtons(expanded: Boolean = false) {
        arrayOf(R.id.action_parents_delete, R.id.action_parents_print).forEach {
            mBinding.fragParentsTb.menu?.findItem(it)?.isVisible = false
        }
        mBinding.fragParentsTb.invalidateMenu()
        mBinding.fragParentsDeleteFab.visibility = if (expanded) View.VISIBLE else View.GONE
        mBinding.fragParentsPrintFab.visibility = if (expanded) View.VISIBLE else View.GONE
    }

    private fun FragmentParentsBinding.swipeLeft() {
        filterMale.isChecked = true
    }

    private fun FragmentParentsBinding.swipeRight() {
        filterFemale.isChecked = true
    }

    private fun FragmentParentsBinding.updateNoDataVisibility() {
        val currentListEmpty = if (filterFemale.isChecked) {
            mFemaleAdapter.currentList.isEmpty()
        } else {
            mMaleAdapter.currentList.isEmpty()
        }
        noDataText.visibility = if (currentListEmpty) View.VISIBLE else View.GONE
    }

    /**
     * Toggles the selected field for all parents in the current rv.
     */
    private fun FragmentParentsBinding.selectAll() {

        if (filterFemale.isChecked) {

            val updatedParents = mFemaleAdapter.currentList
                .filterIsInstance<Parent>()
                .map { mom -> mom.apply { mom.selected = mNextFemaleSelection } }

            parentList.update(*getSortedParents(updatedParents, femaleCrossCounts).toTypedArray())

            mNextFemaleSelection = !mNextFemaleSelection

            mFemaleAdapter.notifyItemRangeChanged(0, mFemaleAdapter.itemCount)


        } else {

            val updatedParents = mMaleAdapter.currentList
                .filterIsInstance<Parent>()
                .map { dad -> dad.apply { dad.selected = mNextMaleSelection } }

            parentList.update(*getSortedParents(updatedParents, maleCrossCounts).toTypedArray())

            val updatedGroups = mMaleAdapter.currentList
                .filterIsInstance<PollenGroup>()
                .map { g -> g.apply { selected = mNextMaleSelection } }

            groupList.update(*getSortedGroups(updatedGroups, maleCrossCounts).toTypedArray())

            mNextMaleSelection = !mNextMaleSelection

            mMaleAdapter.notifyItemRangeChanged(0, mMaleAdapter.itemCount)
        }
    }

    /**
     * Show a dialog to confirm deletion to the user.
     * If the user selects OK then this function either deletes males or females from the database.
     * If a parent is used in a cross then it will not be deleted and a message is shown.
     * Similarly poly groups will not be deleted if used as a parent.
     * The count could also add the number of males when a poly is selected but as of now it just deletes/prints the group id.
     */
    private fun FragmentParentsBinding.deleteParents() {

        context?.let { ctx ->

            Dialogs.onOk(
                AlertDialog.Builder(ctx),
                getString(R.string.frag_parent_delete_selected_title),
                getString(android.R.string.cancel),
                getString(android.R.string.ok),
                getString(R.string.frag_parent_confirm_delete_message)
            ) {

                if (filterFemale.isChecked) {

                    val out: List<Parent> =
                        mFemaleAdapter.currentList.filterIsInstance<Parent>()
                            .filter { p -> p.selected }

                    //find all parents with crosses (that are selected)
                    val parentOfCrossed =
                        out.filter { p -> mCrosses.any { crossed -> p.codeId == crossed.femaleObsUnitDbId } }

                    viewModel.delete(*(out - parentOfCrossed).toTypedArray())

                    if (parentOfCrossed.isNotEmpty()) {

                        Toast.makeText(
                            context, R.string.frag_parents_parents_not_deleted_reason,
                            Toast.LENGTH_SHORT
                        ).show()
                    }

                } else {

                    val outParents = mMaleAdapter.currentList.filterIsInstance<Parent>()
                        .filter { p -> p.selected }

                    val outGroups =
                        mMaleAdapter.currentList.filterIsInstance<PollenGroup>()
                            .filter { g -> g.selected }

                    val parentOfCrossed =
                        outParents.filter { p -> mCrosses.any { crossed -> p.codeId == crossed.maleObsUnitDbId } }
                    val parentOfGroup =
                        outGroups.filter { p -> mCrosses.any { crossed -> p.codeId == crossed.maleObsUnitDbId } }

                    viewModel.delete(*(outParents - parentOfCrossed).toTypedArray())

                    groupList.deleteByCode(((outGroups - parentOfGroup).map { g -> g.codeId }))

                    if (parentOfCrossed.isNotEmpty() || parentOfGroup.isNotEmpty()) {

                        Toast.makeText(
                            context, R.string.frag_parents_parents_not_deleted_reason,
                            Toast.LENGTH_SHORT
                        ).show()
                    }
                }
            }
        }
    }

    private fun checkBluetoothRuntimePermission(): Boolean {

        var permit = true

        context?.let { ctx ->

            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
                if (ctx.checkSelfPermission(android.Manifest.permission.BLUETOOTH_SCAN) == PackageManager.PERMISSION_GRANTED
                    && ctx.checkSelfPermission(android.Manifest.permission.BLUETOOTH_CONNECT) == PackageManager.PERMISSION_GRANTED
                ) {
                    permit = true
                } else {
                    requestBluetoothPermissions.launch(
                        arrayOf(
                            android.Manifest.permission.BLUETOOTH_SCAN,
                            android.Manifest.permission.BLUETOOTH_CONNECT
                        )
                    )
                }
            } else if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
                if (ctx.checkSelfPermission(android.Manifest.permission.BLUETOOTH) == PackageManager.PERMISSION_GRANTED
                    && ctx.checkSelfPermission(android.Manifest.permission.BLUETOOTH_ADMIN) == PackageManager.PERMISSION_GRANTED
                ) {
                    permit = true
                } else {
                    requestBluetoothPermissions.launch(
                        arrayOf(
                            android.Manifest.permission.BLUETOOTH,
                            android.Manifest.permission.BLUETOOTH_ADMIN
                        )
                    )
                }
            }
        }

        return permit
    }

    private fun FragmentParentsBinding.setupBottomNavBar() {

        bottomNavBar.setOnNavigationItemSelectedListener { item ->

            when (item.itemId) {

                R.id.action_nav_preferences -> {

                    findNavController().navigate(R.id.global_action_to_preferences_fragment)
                }

                R.id.action_nav_home -> {

                    findNavController().navigate(ParentsFragmentDirections.globalActionToEvents())

                }

                R.id.action_nav_crosses -> {

                    findNavController().navigate(ParentsFragmentDirections.globalActionToCrossTracker())
                }

                R.id.action_nav_summary -> {
                    findNavController().navigate(ParentsFragmentDirections.actionToSummary())
                }
            }

            true
        }
    }

    private fun FragmentParentsBinding.printParents() {

        if (!checkBluetoothRuntimePermission()) return

        if (filterFemale.isChecked) {

            val outParents = mFemaleAdapter.currentList.filterIsInstance<Parent>()

            BluetoothUtil().print(
                requireContext(),
                outParents.filter { p -> p.selected }.toTypedArray()
            )
        } else {

            val outParents = mMaleAdapter.currentList
                .filterIsInstance<Parent>()
                .filter { p -> p.selected }

            val outAll = outParents + mMaleAdapter.currentList
                .filterIsInstance<PollenGroup>()
                .filter { p -> p.selected }
                .map { group -> Parent(group.codeId, 1, group.name) }

            BluetoothUtil().print(requireContext(), outAll.toTypedArray())
        }
    }

    private fun getSortedParents(parents: List<Parent>, crossCounts: Map<String, Int>): List<Parent> {
        return when (currentSortType) {
            SortType.ID -> parents.sortedBy { it.codeId.lowercase() }
            SortType.CROSSES -> parents.sortedByDescending { crossCounts[it.codeId] ?: 0 }
            else -> parents.sortedBy { it.name.lowercase() }
        }
    }

    private fun getSortedGroups(groups: List<PollenGroup>, crossCounts: Map<String, Int>): List<PollenGroup> {
        return when (currentSortType) {
            SortType.ID -> groups.sortedBy { it.codeId.lowercase() }
            SortType.CROSSES -> groups.sortedByDescending { crossCounts[it.codeId] ?: 0 }
            else -> groups.sortedBy { it.name.lowercase() }
        }
    }

    private fun updateLists(parents: List<Parent>, groups: List<PollenGroup>) {
        val addedMales = ArrayList<BaseParent>()
        addedMales.addAll(getSortedGroups(groups.distinctBy { it.codeId }, maleCrossCounts))
        
        val maleParents = getSortedParents(parents.filter { p -> p.sex == 1 }.distinctBy { p -> p.codeId }, maleCrossCounts)
        mMaleAdapter.submitList(addedMales + maleParents)
        mBinding.maleRecycler.post {
            mBinding.maleRecycler.scrollToPosition(0)
        }

        val femaleParents = getSortedParents(parents.filter { p -> p.sex == 0 }.distinctBy { p -> p.codeId }, femaleCrossCounts)
        mFemaleAdapter.submitList(femaleParents)
        mBinding.femaleRecycler.post {
            mBinding.femaleRecycler.scrollToPosition(0)
        }
    }

    override fun onResume() {
        super.onResume()

        mBinding.bottomNavBar.selectedItemId = R.id.action_nav_parents
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setHasOptionsMenu(true)
    }

    override fun onOptionsItemSelected(item: MenuItem): Boolean {

        when (item.itemId) {

            R.id.action_parents_delete -> {
                mBinding.deleteParents()
            }

            R.id.action_parents_print -> {
                mBinding.printParents()
            }

            android.R.id.home -> {
                findNavController().popBackStack()
            }

            else -> true
        }

        return super.onOptionsItemSelected(item)
    }

    private fun showAddParentsDialog() {
        val importArray: Array<String?> = arrayOf(
            context?.getString(R.string.dialog_import_parents_title),
            context?.getString(R.string.add_new_female_parent),
            context?.getString(R.string.add_new_male_parent),
        )

        val icons = IntArray(importArray.size).apply {
            this[0] = R.drawable.ic_file_generic
            this[1] = R.drawable.ic_female_parent
            this[2] = R.drawable.ic_male_parent
        }

        val onItemClickListener =
            AdapterView.OnItemClickListener { _, _, position, _ ->
                when (position) {
                    0 -> { // import parents
                        context?.let {
                            ImportUtil(it, R.string.dir_parents_import, getString(R.string.dialog_import_parents_title))
                                .showImportDialog(this)
                        }
                    }
                    // create a new female parent
                    1 -> Navigation.findNavController(mBinding.root)
                        .navigate(ParentsFragmentDirections.actionParentsToCreateEvent(0))
                    // create a new male parent
                    2 -> {
                        /*
                            Go to Pollen Manager fragment for male group data-entry
                        */
                        Navigation.findNavController(mBinding.root)
                            .navigate(ParentsFragmentDirections.actionParentsToCreateEvent(1))
                    }

                }
            }

        activity?.let { fragmentActivity ->
            val dialog = context?.getString(R.string.dialog_new_parent_title)?.let {
                    dialogTitle -> ListAddDialog(fragmentActivity, dialogTitle, importArray, icons, onItemClickListener)
            }
            dialog?.show(fragmentActivity.supportFragmentManager, "ListAddDialog")
        }
    }
}
