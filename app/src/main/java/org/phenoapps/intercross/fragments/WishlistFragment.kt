package org.phenoapps.intercross.fragments

import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.util.Log
import android.view.Menu
import android.view.MenuInflater
import android.view.MenuItem
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import androidx.fragment.app.viewModels
import androidx.navigation.Navigation
import androidx.navigation.fragment.findNavController
import androidx.preference.PreferenceManager
import androidx.recyclerview.widget.RecyclerView
import com.evrencoskun.tableview.listener.ITableViewListener
import com.evrencoskun.tableview.sort.SortState
import com.google.android.material.tabs.TabLayout
import org.phenoapps.intercross.activities.MainActivity
import org.phenoapps.intercross.R
import org.phenoapps.intercross.adapters.TableViewAdapter
import org.phenoapps.intercross.data.EventsRepository
import org.phenoapps.intercross.data.MetaValuesRepository
import org.phenoapps.intercross.data.MetadataRepository
import org.phenoapps.intercross.data.WishlistRepository
import org.phenoapps.intercross.data.models.Event
import org.phenoapps.intercross.data.models.Meta
import org.phenoapps.intercross.data.models.MetadataValues
import org.phenoapps.intercross.data.viewmodels.EventListViewModel
import org.phenoapps.intercross.data.viewmodels.MetaValuesViewModel
import org.phenoapps.intercross.data.viewmodels.MetadataViewModel
import org.phenoapps.intercross.data.viewmodels.WishlistViewModel
import org.phenoapps.intercross.data.viewmodels.factory.EventsListViewModelFactory
import org.phenoapps.intercross.data.viewmodels.factory.MetaValuesViewModelFactory
import org.phenoapps.intercross.data.viewmodels.factory.MetadataViewModelFactory
import org.phenoapps.intercross.data.viewmodels.factory.WishlistViewModelFactory
import org.phenoapps.intercross.databinding.FragmentWishlistBinding
import org.phenoapps.intercross.util.Dialogs
import org.phenoapps.intercross.util.KeyUtil
import java.lang.IndexOutOfBoundsException

/**
 * Summary Fragment is a recycler list of currenty crosses.
 * Users can navigate to and from cross block and wishlist fragments.
 */
class WishlistFragment : IntercrossBaseFragment<FragmentWishlistBinding>(R.layout.fragment_wishlist), ITableViewListener {

    private val eventsModel: EventListViewModel by viewModels {
        EventsListViewModelFactory(EventsRepository.getInstance(db.eventsDao()))
    }

    private val wishModel: WishlistViewModel by viewModels {
        WishlistViewModelFactory(WishlistRepository.getInstance(db.wishlistDao()))
    }

    private val metaValuesViewModel: MetaValuesViewModel by viewModels {
        MetaValuesViewModelFactory(MetaValuesRepository.getInstance(db.metaValuesDao()))
    }

    private val metadataViewModel: MetadataViewModel by viewModels {
        MetadataViewModelFactory(MetadataRepository.getInstance(db.metadataDao()))
    }

    private val mPref by lazy {
        PreferenceManager.getDefaultSharedPreferences(requireContext())
    }

    private val mKeyUtil by lazy {
        KeyUtil(context)
    }

    private var mIsSorting = false

    private var wishlistEmpty = true

    private var mMetaValuesList: List<MetadataValues> = ArrayList()
    private var mMetaList: List<Meta> = ArrayList()
    private var mEvents: List<Event> = ArrayList()

    private var systemMenu: Menu? = null

    data class WishlistData(override var male: String,
                            override var female: String,
                            var progress: String,
                            var minimum: String,
                            var maximum: String,
                            var mid: String,
                            var fid: String): CrossTrackerFragment.ListEntry(male, female, progress)

    override fun FragmentWishlistBinding.afterCreateView() {

        bottomNavBar.selectedItemId = R.id.action_nav_cross_count

        setupBottomNavBar()

        mPref.edit().putString("last_visited_summary", "wishlist").apply()


        eventsModel.events.observe(viewLifecycleOwner) {
            mEvents = it

            metadataViewModel.metadata.observe(viewLifecycleOwner) {
                mMetaList = it

                metaValuesViewModel.metaValues.observe(viewLifecycleOwner) {
                    mMetaValuesList = it

                    load()
                }
            }
        }

        summaryTabLayout.getTabAt(1)?.select()

        setupTabLayout()

    }

    /**
     * issue 25 added a commutative cross count where order does not matter.
     */
    private fun load() {

        val isCommutativeCrossing = mPref.getBoolean(mKeyUtil.commutativeCrossingKey, false)

        if (isCommutativeCrossing) loadCommutativeWishlist()
        else loadWishlist()
    }

    /**
     * Shows table with m/f/count
     */
    private fun setupTable(entries: List<WishlistData>) {

        val maleText = getString(R.string.male)
        val femaleText = getString(R.string.female)
        val progressText = getString(R.string.progress)
        val minimumText = getString(R.string.minimum)
        val maximumText = getString(R.string.maximum)
        val completedText = getString(R.string.completed)

        val data = arrayListOf<List<CrossTrackerFragment.CellData>>()

        entries.forEach {
            data.add(listOf(
                CrossTrackerFragment.CellData("",
                    complete = (it.progress.toIntOrNull() ?: 0) >= (it.minimum.toIntOrNull() ?: 0)
                ),
                CrossTrackerFragment.CellData(it.male, it.mid),
                CrossTrackerFragment.CellData(it.female, it.fid),
                CrossTrackerFragment.CellData(it.progress),
                CrossTrackerFragment.CellData(it.minimum),
                CrossTrackerFragment.CellData(it.maximum)
            ))
        }

        with(mBinding.fragmentWishlistTableView) {
            isIgnoreSelectionColors = true
            tableViewListener = this@WishlistFragment
            isShowHorizontalSeparators = true
            isShowVerticalSeparators = true
            setAdapter(TableViewAdapter())

            (adapter as? TableViewAdapter)?.setAllItems(
                listOf(
                    CrossTrackerFragment.CellData(completedText),
                    CrossTrackerFragment.CellData(maleText),
                    CrossTrackerFragment.CellData(femaleText),
                    CrossTrackerFragment.CellData(progressText),
                    CrossTrackerFragment.CellData(minimumText),
                    CrossTrackerFragment.CellData(maximumText)
                ),
                listOf(),
                data
            )

            //sort table by count
            sortColumn(2, SortState.DESCENDING)
        }
    }

    private fun askDeleteAll() {
        Dialogs.onOk(AlertDialog.Builder(requireContext()),
            getString(R.string.delete_all_wishlist_title),
            getString(android.R.string.cancel),
            getString(android.R.string.ok)) {

            Dialogs.onOk(AlertDialog.Builder(requireContext()),
                getString(R.string.delete_all_wishlist_title),
                getString(android.R.string.cancel),
                getString(android.R.string.ok),
                getString(R.string.dialog_cross_count_delete_all_message_2)) {


                wishModel.deleteAll()
                updateToolbarWishlistIcon()

                findNavController().navigate(WishlistFragmentDirections.globalActionToCrossTracker())
            }
        }
    }

    private fun getChildrenCommutative(momId: String, dadId: String): List<Event> = mEvents.filter {
            event -> (event.femaleObsUnitDbId == momId && event.maleObsUnitDbId == dadId)
            || (event.femaleObsUnitDbId == dadId && event.maleObsUnitDbId == momId)
    }

    private fun getChildren(momId: String, dadId: String): List<Event> = mEvents.filter {
            event -> event.femaleObsUnitDbId == momId && event.maleObsUnitDbId == dadId
    }

    private fun loadWishlist() {

        wishModel.wishes.observe(viewLifecycleOwner) {

            it?.let { block ->

                val data = ArrayList<WishlistData>()

                //get cross wishes which are automatically counted in the wishlist view
                val crosses = block.filter { wish -> wish.wishType == "cross" }.map { res ->
                    WishlistData(
                        res.dadName, res.momName,
                        res.wishProgress.toString(),
                        res.wishMin.toString(),
                        res.wishMax.toString(),
                        res.dadId, res.momId
                    )
                }

                data.addAll(crosses)

                //get all other wish types and use metadata tables to populate the wish progress
                val otherWishes = block.filter { it.wishType != "cross" }.map { row ->

                    //get children to accumulate their metadata
                    val children = getChildren(row.momId, row.dadId)
                    var progress = 0
                    val metadata = mMetaList.filter { it.property == row.wishType }
                    if (metadata.isNotEmpty()) {
                        val meta = metadata.first()
                        progress = mMetaValuesList.filter {
                            it.metaId == meta.id?.toInt()
                                    && it.eid in children.map { it.id?.toInt() }
                        }
                            .map { it.value ?: 0 }.sum()
                    }
                    WishlistData(
                        row.dadName, row.momName, progress.toString(),
                        row.wishMin.toString(), row.wishMax.toString(), row.dadId, row.momId
                    )
                }

                data.addAll(otherWishes)

                wishlistEmpty = crosses.isEmpty()

                updateToolbarWishlistIcon()

                setupTable(data)

            }
        }
    }

    private fun loadCommutativeWishlist() {

        wishModel.commutativeWishes.observe(viewLifecycleOwner) {

            it?.let { block ->

                val data = ArrayList<WishlistData>()

                //get cross wishes which are automatically counted in the wishlist view
                val crosses = block.filter { wish -> wish.wishType == "cross" }.map { res ->
                    WishlistData(
                        res.dadName, res.momName,
                        res.wishProgress.toString(),
                        res.wishMin.toString(),
                        res.wishMax.toString(),
                        res.dadId, res.momId
                    )
                }

                data.addAll(crosses)

                //get all other wish types and use metadata tables to populate the wish progress
                val otherWishes = block.filter { it.wishType != "cross" }.map { row ->

                    //get children to accumulate their metadata
                    val children = getChildrenCommutative(row.momId, row.dadId)
                    var progress = 0
                    val metadata = mMetaList.filter { it.property == row.wishType }
                    if (metadata.isNotEmpty()) {
                        val meta = metadata.first()
                        progress = mMetaValuesList.filter {
                            it.metaId == meta.id?.toInt()
                                    && it.eid in children.map { it.id?.toInt() }
                        }
                            .map { it.value ?: 0 }.sum()
                    }
                    WishlistData(
                        row.dadName, row.momName, progress.toString(),
                        row.wishMin.toString(), row.wishMax.toString(),
                        row.dadId, row.momId
                    )
                }

                data.addAll(otherWishes)

                wishlistEmpty = crosses.isEmpty()

                updateToolbarWishlistIcon()

                setupTable(data)
            }
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

    private fun FragmentWishlistBinding.setupTabLayout() {

        summaryTabLayout.addOnTabSelectedListener(tabSelected { tab ->

            when (tab?.position) {
                2 -> {

                    if (mEvents.isNotEmpty()) {

                        Navigation.findNavController(mBinding.root)
                            .navigate(WishlistFragmentDirections.actionToSummary())
                    } else {

                        Dialogs.notify(AlertDialog.Builder(requireContext()), getString(R.string.crosses_empty))
                        summaryTabLayout.getTabAt(1)?.select()

                    }
                }

                0 ->
                    Navigation.findNavController(mBinding.root)
                        .navigate(WishlistFragmentDirections.actionToCrossTracker())
            }
        })
    }

    private fun FragmentWishlistBinding.setupBottomNavBar() {

        bottomNavBar.setOnNavigationItemSelectedListener { item ->

            when (item.itemId) {

                R.id.action_nav_home -> {

                    findNavController().navigate(WishlistFragmentDirections.globalActionToEvents())
                }
                R.id.action_nav_preferences -> {

                    findNavController().navigate(WishlistFragmentDirections.globalActionToPreferencesFragment())
                }
                R.id.action_nav_parents -> {

                    findNavController().navigate(WishlistFragmentDirections.globalActionToParents())

                }
                R.id.action_nav_cross_count -> {

                    findNavController().navigate(WishlistFragmentDirections.globalActionToCrossTracker())

                }
            }

            true
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {

        super.onCreate(savedInstanceState)

        setHasOptionsMenu(true)
    }

    override fun onResume() {
        super.onResume()

        (activity as? AppCompatActivity)?.setSupportActionBar(mBinding.fragWishlistTb)

        updateToolbarWishlistIcon()

        mBinding.summaryTabLayout.getTabAt(1)?.select()

        mBinding.bottomNavBar.menu.findItem(R.id.action_nav_cross_count).isEnabled = false

        mBinding.bottomNavBar.selectedItemId = R.id.action_nav_cross_count

        mBinding.bottomNavBar.menu.findItem(R.id.action_nav_cross_count).isEnabled = true

    }

    override fun onCreateOptionsMenu(menu: Menu, inflater: MenuInflater) {

        inflater.inflate(R.menu.wishlist_toolbar, menu)

        systemMenu = menu

        super.onCreateOptionsMenu(menu, inflater)
    }

    private fun updateToolbarWishlistIcon() {
        systemMenu?.findItem(R.id.action_to_crossblock)?.isVisible = !wishlistEmpty
    }

    override fun onOptionsItemSelected(item: MenuItem): Boolean {

        when(item.itemId) {

            R.id.action_wishlist_delete_all -> {
                askDeleteAll()
            }
            R.id.action_parents_toolbar_initiate_wf -> {

                findNavController().navigate(WishlistFragmentDirections
                    .actionFromWishlistToWishFactory())
            }
            R.id.action_to_crossblock -> {
                findNavController().navigate(WishlistFragmentDirections.actionToCrossblock())
            }
            R.id.action_import -> {

                (activity as MainActivity).launchImport()

                findNavController().navigate(R.id.wishlist_fragment)
            }
        }

        return super.onOptionsItemSelected(item)
    }

    private fun showChildren(male: String, female: String) {

        val isCommutativeCrossing = mPref.getBoolean(mKeyUtil.commutativeCrossingKey, false)

        val children = if (isCommutativeCrossing) getChildrenCommutative(female, male)
        else getChildren(female, male)

        context?.let { ctx ->
            Dialogs.listAndBuildCross(
                AlertDialog.Builder(ctx),
                getString(R.string.click_item_for_child_details),
                getString(R.string.no_child_exists),
                male, female, children,
                { id ->

                    findNavController().navigate(WishlistFragmentDirections
                        .globalActionToEventDetail(id))
                }) { male, female ->

                findNavController().navigate(WishlistFragmentDirections
                    .actionFromWishlistToEventsList(male, female))
            }
        }
    }

    /**
     * When a cell is clicked, grab the data within each column of that row.
     * This assumes the row will always be m/f/count or m/f/count/person/date
     * m/f will be used to query for children and display to the user to choose which to navigate to.
     * If there is just one, navigate to it automatically.
     */
    override fun onCellClicked(cellView: RecyclerView.ViewHolder, column: Int, row: Int) {
        mBinding.fragmentWishlistTableView.adapter?.getCellRowItems(row)?.let { r ->

            try {

                val male = (r[1] as? CrossTrackerFragment.CellData)?.uuid
                val female = (r[2] as? CrossTrackerFragment.CellData)?.uuid

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
            with (mBinding.fragmentWishlistTableView) {
                sortColumn(column, when (getSortingStatus(column)) {
                    SortState.DESCENDING -> SortState.ASCENDING
                    else -> SortState.DESCENDING
                })
                scrollToRowPosition(0)
            }

            Handler(Looper.getMainLooper()).postDelayed({
                mIsSorting = false
            }, 10000)
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