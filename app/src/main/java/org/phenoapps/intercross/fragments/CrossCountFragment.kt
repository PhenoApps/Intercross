package org.phenoapps.intercross.fragments

import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.util.Log
import android.view.Menu
import android.view.MenuInflater
import android.view.MenuItem
import androidx.appcompat.app.AlertDialog
import androidx.fragment.app.viewModels
import androidx.navigation.Navigation
import androidx.navigation.fragment.findNavController
import androidx.preference.PreferenceManager
import androidx.recyclerview.widget.RecyclerView
import com.evrencoskun.tableview.listener.ITableViewListener
import com.evrencoskun.tableview.sort.ISortableModel
import com.evrencoskun.tableview.sort.SortState
import com.google.android.material.tabs.TabLayout
import org.phenoapps.intercross.activities.MainActivity
import org.phenoapps.intercross.R
import org.phenoapps.intercross.adapters.TableViewAdapter
import org.phenoapps.intercross.data.EventsRepository
import org.phenoapps.intercross.data.WishlistRepository
import org.phenoapps.intercross.data.models.Event
import org.phenoapps.intercross.data.viewmodels.EventListViewModel
import org.phenoapps.intercross.data.viewmodels.WishlistViewModel
import org.phenoapps.intercross.data.viewmodels.factory.EventsListViewModelFactory
import org.phenoapps.intercross.data.viewmodels.factory.WishlistViewModelFactory
import org.phenoapps.intercross.databinding.FragmentCrossCountBinding
import org.phenoapps.intercross.util.Dialogs
import org.phenoapps.intercross.util.KeyUtil
import java.lang.IndexOutOfBoundsException
import kotlin.collections.ArrayList

/**
 * Summary Fragment is a recycler list of currenty crosses.
 * Users can navigate to and from cross block and wishlist fragments.
 */
class CrossCountFragment : IntercrossBaseFragment<FragmentCrossCountBinding>(R.layout.fragment_cross_count), ITableViewListener {

    companion object {
        const val SORT_DELAY_MS = 500L
    }

    private val eventsModel: EventListViewModel by viewModels {
        EventsListViewModelFactory(EventsRepository.getInstance(db.eventsDao()))
    }

    private val wishModel: WishlistViewModel by viewModels {
        WishlistViewModelFactory(WishlistRepository.getInstance(db.wishlistDao()))
    }

    private var mWishlistEmpty = true

    private val mPref by lazy {
        PreferenceManager.getDefaultSharedPreferences(requireContext())
    }

    private val mKeyUtil by lazy {
        KeyUtil(context)
    }

    private var mExpandedColumns = false

    //variable to avoid 'Inconsistency detected' TableView exception that is caused if sorting is spammed.
    private var mIsSorting = false

    /**
     * Polymorphism setup to allow adapter to work with two different types of objects.
     * Wishlists and Summary data are the same but they have to be rendered differently.
     */
    open class ListEntry(open var m: String, open var f: String,
                         open var count: String, open var person: String = "",
                         open var date: String = "")

    data class CrossData(override var m: String,
                         override var f: String,
                         override var count: String,
                         override var person: String = "",
                         override var date: String = ""): ListEntry(m, f, count, person, date)

    data class CellData(val text: String, val uuid: String = "", val complete: Boolean = false): ISortableModel {

        override fun getId(): String {
            return text
        }

        override fun getContent(): Any? {
            return text.toIntOrNull() ?: text
        }

    }

    override fun FragmentCrossCountBinding.afterCreateView() {

        mPref.edit().putString("last_visited_summary", "summary").apply()

        startObservers()

        bottomNavBar.selectedItemId = R.id.action_nav_cross_count

        setupBottomNavBar()

        summaryTabLayout.getTabAt(0)?.select()

        setupTabLayout()
    }

    private fun startObservers() {

        loadCounts()

        /**
         * Keep track if wishlist repo is empty to disable options items menu
         */
        wishModel.wishlist.observe(viewLifecycleOwner, { wishes ->

            mWishlistEmpty = wishes.none { it.wishType == "cross" }
        })
    }

    /**
     * issue 25 added a commutative cross count where order does not matter.
     */
    private fun loadCounts() {

        val isCommutativeCrossing = mPref.getBoolean(mKeyUtil.workCommutativeKey, false)

        if (isCommutativeCrossing) loadCommutativeCrossCounts()
        else loadNonCommutativeCrossCounts()
    }

    private fun showChildren(male: String, female: String) {

        val isCommutativeCrossing = mPref.getBoolean(mKeyUtil.workCommutativeKey, false)

        if (isCommutativeCrossing) showCommutativeChildren(male, female)
        else showNonCommutativeChildren(male, female)
    }

    private fun showChildren(male: String, female: String, data: List<Event>) {

        context?.let { ctx ->
            Dialogs.list(
                AlertDialog.Builder(ctx),
                getString(R.string.click_item_for_child_details),
                getString(R.string.no_child_exists),
                male, female, data, { id ->

                    findNavController()
                        .navigate(CrossCountFragmentDirections
                            .globalActionToEventDetail(id))
                }) { male, female ->

                findNavController()
                    .navigate(CrossCountFragmentDirections
                        .actionFromCrosscountToEventsList(male, female))
            }
        }
    }

    /**
     * Shows table with m/f/count
     */
    private fun setupTable(entries: List<ListEntry>) {

        val maleText = getString(R.string.male)
        val femaleText = getString(R.string.female)
        val countText = getString(R.string.crosses)

        val data = arrayListOf<List<CellData>>()
        entries.forEach {
            data.add(listOf(CellData(it.m),
                CellData(it.f),
                CellData(it.count)))
        }

        with(mBinding.fragmentCrossCountTableView) {
            isIgnoreSelectionColors = true
            tableViewListener = this@CrossCountFragment
            isShowHorizontalSeparators = true
            isShowVerticalSeparators = true
            setAdapter(TableViewAdapter())

            (adapter as? TableViewAdapter)?.setAllItems(
                listOf(CellData(maleText),
                    CellData(femaleText),
                    CellData(countText)),
                listOf(),
                data
            )

            //sort table by count
            sortColumn(2, SortState.DESCENDING)
        }
    }

    /**
     * Shows table with m/f/count/date/person
     */
    private fun setupExpandedTable(entries: List<ListEntry>) {

        val maleText = getString(R.string.male)
        val femaleText = getString(R.string.female)
        val countText = getString(R.string.crosses)
        val personText = getString(R.string.person)
        val dateText = getString(R.string.date)

        val data = arrayListOf<List<CellData>>()
        entries.forEach {
            data.add(listOf(CellData(it.m),
                CellData(it.f),
                CellData(it.count),
                CellData(it.person),
                CellData(it.date)))
        }

        with(mBinding.fragmentCrossCountTableView) {
            isIgnoreSelectionColors = true
            tableViewListener = this@CrossCountFragment
            isShowHorizontalSeparators = true
            isShowVerticalSeparators = true
            setAdapter(TableViewAdapter())

            (adapter as? TableViewAdapter)?.setAllItems(
                listOf(CellData(maleText),
                    CellData(femaleText),
                    CellData(countText),
                    CellData(personText),
                    CellData(dateText)),
                listOf(),
                data
            )

            sortColumn(2, SortState.DESCENDING)
        }
    }

    //a quick wrapper function for tab selection
    private fun tabSelected(onSelect: (TabLayout.Tab?) -> Unit) = object : TabLayout.OnTabSelectedListener {
        override fun onTabSelected(tab: TabLayout.Tab?) {
            onSelect(tab)
        }
        override fun onTabUnselected(tab: TabLayout.Tab?) {}
        override fun onTabReselected(tab: TabLayout.Tab?) {}
    }

    private fun showCommutativeChildren(male: String, female: String) {

        eventsModel.parents.observe(viewLifecycleOwner, {

            it?.let { crosses ->

                eventsModel.events.observe(viewLifecycleOwner, { data ->

                    data?.let { events ->

                        showChildren(male, female, events.filter { e ->
                            (e.maleObsUnitDbId == male && e.femaleObsUnitDbId == female)
                                    || (e.maleObsUnitDbId == female && e.femaleObsUnitDbId == male)})
                    }
                })
            }
        })
    }

    private fun showNonCommutativeChildren(male: String, female: String) {

        eventsModel.parents.observe(viewLifecycleOwner, {

            it?.let { crosses ->

                eventsModel.events.observe(viewLifecycleOwner, { data ->

                    data?.let { events ->

                        showChildren(male, female, events.filter { e -> e.maleObsUnitDbId == male
                                && e.femaleObsUnitDbId == female })
                    }
                })
            }
        })
    }

    private fun loadCommutativeCrossCounts() {

        eventsModel.parents.observe(viewLifecycleOwner, {

            it?.let { crosses ->

                val crossData = ArrayList<CrossData>()

                eventsModel.events.observe(viewLifecycleOwner, { data ->

                    data?.let { events ->

                        crosses.forEach { parentrow ->

                            crossData.add(
                                CrossData(
                                    parentrow.dad,
                                    parentrow.mom,
                                    parentrow.count.toString(),
                                    parentrow.person,
                                    parentrow.date)
                            )
                        }

                        (crossData.groupBy { cross ->
                            if (cross.m < cross.f) "${cross.m}${cross.f}".hashCode()
                            else "${cross.f}${cross.m}".hashCode()
                        }.map { entry ->
                            if (entry.value.size == 1) {
                                entry.value[0]
                            } else {
                                val actualCount = entry.value
                                    .sumBy { match -> match.count.toInt() }.toString()
                                with(entry.value[0]) {
                                    CrossData(m, f, actualCount, person, date)
                                }
                            }
                        } as List<ListEntry>?)?.let { data ->
                            if (mExpandedColumns) setupExpandedTable(data)
                            else setupTable(data)
                        }
                    }
                })
            }
        })
    }

    private fun loadNonCommutativeCrossCounts() {

        eventsModel.parents.observe(viewLifecycleOwner, {

            it?.let { crosses ->

                val crossData = ArrayList<CrossData>()

                eventsModel.events.observe(viewLifecycleOwner, { data ->

                    data?.let { events ->

                        crosses.forEach { parentrow ->

                            crossData.add(
                                CrossData(
                                    parentrow.dad,
                                    parentrow.mom,
                                    parentrow.count.toString(),
                                    parentrow.person,
                                    parentrow.date)
                            )

                        }

                        if (mExpandedColumns) setupExpandedTable(crossData)
                        else setupTable(crossData)

                    }
                })
            }
        })
    }

    private fun FragmentCrossCountBinding.setupTabLayout() {

        summaryTabLayout.addOnTabSelectedListener(tabSelected { tab ->

            when (tab?.position) {
                2 -> {

                    if (!mWishlistEmpty) {

                        Navigation.findNavController(mBinding.root)
                            .navigate(CrossCountFragmentDirections.actionToCrossblock())
                    } else {

                        Dialogs.notify(AlertDialog.Builder(requireContext()), getString(R.string.wishlist_is_empty))
                        summaryTabLayout.getTabAt(0)?.select()

                    }
                }

                3 ->
                    Navigation.findNavController(mBinding.root)
                        .navigate(CrossCountFragmentDirections.actionToSummary())

                1 ->
                    Navigation.findNavController(mBinding.root)
                        .navigate(CrossCountFragmentDirections.actionToWishlist())
            }
        })
    }

    override fun onResume() {
        super.onResume()

        (activity as MainActivity).supportActionBar?.show()

        mBinding.bottomNavBar.selectedItemId = R.id.action_nav_cross_count

    }

    override fun onCreate(savedInstanceState: Bundle?) {

        super.onCreate(savedInstanceState)


        setHasOptionsMenu(true)
    }

    override fun onCreateOptionsMenu(menu: Menu, inflater: MenuInflater) {

        inflater.inflate(R.menu.cross_count_toolbar, menu)

        super.onCreateOptionsMenu(menu, inflater)
    }

    override fun onOptionsItemSelected(item: MenuItem): Boolean {

        when(item.itemId) {

            R.id.action_import -> {

                (activity as MainActivity).launchImport()

                findNavController().navigate(R.id.cross_count_fragment)
            }

            R.id.action_cross_count_delete_all -> {
                context?.let { ctx ->
                    Dialogs.onOk(AlertDialog.Builder(ctx),
                        getString(R.string.menu_cross_count_delete_all_title),
                        getString(android.R.string.cancel),
                        getString(android.R.string.ok),
                        getString(R.string.dialog_cross_count_delete_all_message)) {

                        Dialogs.onOk(AlertDialog.Builder(ctx),
                            getString(R.string.menu_cross_count_delete_all_title),
                            getString(android.R.string.cancel),
                            getString(android.R.string.ok),
                            getString(R.string.dialog_cross_count_delete_all_message_2)) {

                            eventsModel.deleteAll()

                            findNavController().popBackStack()
                        }
                    }
                }
            }
            R.id.action_cross_count_expand_columns -> {
                when (mExpandedColumns) {
                    false -> {
                        item.setIcon(R.drawable.ic_expand_less_black_24dp)
                        item.title = getString(R.string.menu_cross_count_expand_less_title)
                    }
                    else -> {
                        item.setIcon(R.drawable.ic_expand_more_black_24dp)
                        item.title = getString(R.string.menu_cross_count_expand_more_title)
                    }
                }

                mExpandedColumns = !mExpandedColumns

                loadCounts()
            }
        }

        return super.onOptionsItemSelected(item)
    }

    private fun FragmentCrossCountBinding.setupBottomNavBar() {

        bottomNavBar.setOnNavigationItemSelectedListener { item ->

            when (item.itemId) {

                R.id.action_nav_settings -> {

                    findNavController().navigate(R.id.global_action_to_settings_fragment)
                }
                R.id.action_nav_parents -> {

                    findNavController().navigate(CrossCountFragmentDirections.globalActionToParents())

                }
                R.id.action_nav_export -> {

                    (activity as MainActivity).showExportDialog {

                        bottomNavBar.selectedItemId = R.id.action_nav_cross_count
                    }
                }
                R.id.action_nav_home -> {

                    findNavController().navigate(CrossCountFragmentDirections.globalActionToEvents())

                }
            }

            true
        }
    }

    /**
     * When a cell is clicked, grab the data within each column of that row.
     * This assumes the row will always be m/f/count or m/f/count/person/date
     * m/f will be used to query for children and display to the user to choose which to navigate to.
     * If there is just one, navigate to it automatically.
     */
    override fun onCellClicked(cellView: RecyclerView.ViewHolder, column: Int, row: Int) {
        mBinding.fragmentCrossCountTableView.adapter?.getCellRowItems(row)?.let { row ->

            try {

                val male = (row[0] as? CellData)?.text
                val female = (row[1] as? CellData)?.text

                male?.let { maleId ->
                    female?.let { femaleId ->
                        showChildren(maleId, femaleId)
                    }
                }

            } catch (e: IndexOutOfBoundsException) {

                Log.d(this.tag, "Table view was clicked but did not have male/female data.")

                e.printStackTrace()
            }
        }
    }

    override fun onColumnHeaderClicked(columnHeaderView: RecyclerView.ViewHolder, column: Int) {

        if (!mIsSorting) {
            mIsSorting = true
            with (mBinding.fragmentCrossCountTableView) {
                sortColumn(column, when (getSortingStatus(column)) {
                    SortState.DESCENDING -> SortState.ASCENDING
                    else -> SortState.DESCENDING
                })
                scrollToRowPosition(0)
            }

            Handler(Looper.getMainLooper()).postDelayed({
                mIsSorting = false
            }, SORT_DELAY_MS)
        }
    }

    override fun onCellDoubleClicked(cellView: RecyclerView.ViewHolder, column: Int, row: Int) {}
    override fun onCellLongPressed(cellView: RecyclerView.ViewHolder, column: Int, row: Int) {}
    override fun onColumnHeaderDoubleClicked(columnHeaderView: RecyclerView.ViewHolder, column: Int) {}
    override fun onColumnHeaderLongPressed(columnHeaderView: RecyclerView.ViewHolder, column: Int) {}
    override fun onRowHeaderClicked(rowHeaderView: RecyclerView.ViewHolder, row: Int) {}
    override fun onRowHeaderDoubleClicked(rowHeaderView: RecyclerView.ViewHolder, row: Int) {}
    override fun onRowHeaderLongPressed(rowHeaderView: RecyclerView.ViewHolder, row: Int) {}
}