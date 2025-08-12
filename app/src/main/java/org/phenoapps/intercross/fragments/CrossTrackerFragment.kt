package org.phenoapps.intercross.fragments

import android.os.Bundle
import android.view.Menu
import android.view.MenuInflater
import android.view.MenuItem
import android.view.View
import android.widget.AdapterView
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import androidx.fragment.app.viewModels
import androidx.navigation.fragment.findNavController
import androidx.preference.PreferenceManager
import androidx.recyclerview.widget.LinearLayoutManager
import com.evrencoskun.tableview.sort.ISortableModel
import org.phenoapps.intercross.R
import org.phenoapps.intercross.activities.MainActivity
import org.phenoapps.intercross.adapters.CrossTrackerAdapter
import org.phenoapps.intercross.data.EventsRepository
import org.phenoapps.intercross.data.MetaValuesRepository
import org.phenoapps.intercross.data.MetadataRepository
import org.phenoapps.intercross.data.WishlistRepository
import org.phenoapps.intercross.data.dao.EventsDao
import org.phenoapps.intercross.data.models.Event
import org.phenoapps.intercross.data.models.Meta
import org.phenoapps.intercross.data.models.MetadataValues
import org.phenoapps.intercross.data.models.WishlistView
import org.phenoapps.intercross.data.viewmodels.EventListViewModel
import org.phenoapps.intercross.data.viewmodels.MetaValuesViewModel
import org.phenoapps.intercross.data.viewmodels.MetadataViewModel
import org.phenoapps.intercross.data.viewmodels.WishlistViewModel
import org.phenoapps.intercross.data.viewmodels.factory.EventsListViewModelFactory
import org.phenoapps.intercross.data.viewmodels.factory.MetaValuesViewModelFactory
import org.phenoapps.intercross.data.viewmodels.factory.MetadataViewModelFactory
import org.phenoapps.intercross.data.viewmodels.factory.WishlistViewModelFactory
import org.phenoapps.intercross.databinding.FragmentCrossTrackerBinding
import org.phenoapps.intercross.dialogs.ListAddDialog
import org.phenoapps.intercross.interfaces.CrossController
import org.phenoapps.intercross.interfaces.EventClickListener
import org.phenoapps.intercross.util.DateUtil
import org.phenoapps.intercross.util.ImportUtil
import org.phenoapps.intercross.util.KeyUtil
import org.phenoapps.intercross.util.ShowChildrenDialogUtil
import androidx.core.content.edit
import org.phenoapps.intercross.data.ParentsRepository
import org.phenoapps.intercross.data.models.Parent
import org.phenoapps.intercross.data.viewmodels.ParentsListViewModel
import org.phenoapps.intercross.data.viewmodels.factory.ParentsListViewModelFactory
import kotlin.collections.component1
import kotlin.collections.component2

/**
 * Summary Fragment is a recycler list of current crosses.
 * Users can navigate to and from cross block and wishlist fragments.
 */
class CrossTrackerFragment :
    IntercrossBaseFragment<FragmentCrossTrackerBinding>(R.layout.fragment_cross_tracker),
    CrossController,
    EventClickListener {

    companion object {
        const val SORT_DELAY_MS = 500L
    }

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

    private val parentsModel: ParentsListViewModel by viewModels {
        ParentsListViewModelFactory(ParentsRepository.getInstance(db.parentsDao()))
    }

    private var mWishlistEmpty = true
    private var mEvents: List<Event> = ArrayList()
    private var mMetaValuesList: List<MetadataValues> = emptyList()
    private var mMetaList: List<Meta> = emptyList()
    private var mParents: List<Parent> = emptyList()
    private var parentLookup: Map<String, String> = emptyMap()

    private val mPref by lazy {
        PreferenceManager.getDefaultSharedPreferences(requireContext())
    }

    private val mKeyUtil by lazy {
        KeyUtil(context)
    }

    private val mShowChildrenDialogUtil by lazy {
        ShowChildrenDialogUtil(
            this,
            context,
            { male, female ->
                findNavController()
                    .navigate(CrossTrackerFragmentDirections
                        .actionFromCrossTrackerToEventsList(male, female))
            },
            eventsModel,
            this,
            mPref.getBoolean(mKeyUtil.commutativeCrossingKey, false)
        )
    }

    private var systemMenu: Menu? = null

    private val crossAdapter by lazy {
        CrossTrackerAdapter(this)
    }

    data class PersonCount(
        val name: String,
        val count: Int,
    )

    data class DateCount(
        val date: String,
        val count: Int,
    )

    /**
     * Polymorphism setup to allow adapter to work with two different types of objects.
     * Wishlists and Summary data are the same but they have to be rendered differently.
     */
    open class ListEntry(
        open var male: String,
        open var female: String,
        open var count: String,
        open var persons: List<PersonCount> = emptyList(),
        open var dates: List<DateCount> = emptyList(),
        open var maleId: String = "", open var femaleId: String = "",
    ) {
        companion object {
            const val TYPE_UNPLANNED = 0
            const val TYPE_PLANNED = 1
        }

        open fun getType(): Int = TYPE_UNPLANNED

        // used in CrossTrackerAdapter
        override fun equals(other: Any?): Boolean {
            if (this === other) return true
            if (other !is ListEntry) return false

            return male == other.male &&
                    female == other.female &&
                    count == other.count &&
                    persons == other.persons &&
                    dates == other.dates
        }

        override fun hashCode(): Int {
            var result = male.hashCode()
            result = 31 * result + female.hashCode()
            result = 31 * result + count.hashCode()
            result = 31 * result + persons.hashCode()
            result = 31 * result + dates.hashCode()
            return result
        }
    }

    data class WishlistItem(
        val wishType: String,
        val min: Int,
        val max: Int,
        val progress: Int
    )

    // for regular crosses
    data class UnplannedCrossData(
        override var male: String,
        override var female: String,
        override var count: String,
        override var persons: List<PersonCount> = emptyList(),
        override var dates: List<DateCount> = emptyList(),
    ) : ListEntry(male, female, count, persons, dates)

    // for wishlist crosses
    data class PlannedCrossData(
        override var male: String,
        override var female: String,
        override var count: String,
        override var persons: List<PersonCount> = emptyList(),
        override var dates: List<DateCount> = emptyList(),
        override var maleId: String = "",
        override var femaleId: String = "",
        val wishes: List<WishlistItem> = emptyList()
    ) : ListEntry(male, female, count, persons, dates, maleId, femaleId) {
        override fun getType(): Int = TYPE_PLANNED
    }

    data class NormalizedParents(val dad: String, val mom: String)

    /**
     * Normalizes both parent IDs to consistent format (UUID -> name)
     */
    private fun normalizeParents(dadId: String, momId: String) = NormalizedParents(normalizeParentId(dadId), normalizeParentId(momId))

    /**
     * Converts a parent ID to its display name using the parent lookup table.
     *
     * Events table contains mixed ID formats (UUIDs vs names), so we normalize all to names
     * to enable consistent matching between events and wishlist items across different formats.
     *
     * @param id The parent ID to normalize (UUID or display name)
     * @return The display name for this parent
     */
    private fun normalizeParentId(id: String) = parentLookup[id] ?: id

    data class CellData(val text: String, val uuid: String = "", val complete: Boolean = false) :
        ISortableModel {

        override fun getId(): String {
            return text
        }

        override fun getContent(): Any {
            return text.toIntOrNull() ?: text
        }

    }

    private var currentFilter = CrossFilter.ALL
    private var crossesAndWishesData: List<ListEntry> = emptyList()

    enum class CrossFilter {
        ALL,
        PLANNED,
        UNPLANNED
    }

    override fun FragmentCrossTrackerBinding.afterCreateView() {

        (activity as MainActivity).applyFragmentInsets(root, fragCrossTrackerTb)

        mPref.edit { putString("last_visited_summary", "summary") }

        startObservers()

        mBinding.crossesRecyclerView.apply {
            adapter = crossAdapter
            layoutManager = LinearLayoutManager(context)
        }

        bottomNavBar.selectedItemId = R.id.action_nav_crosses

        setupBottomNavBar()

        // summaryTabLayout.getTabAt(0)?.select()

        // setupTabLayout()

        eventsModel.events.observe(viewLifecycleOwner) {
            mEvents = it
        }


        fragmentCrossTrackerAddButton.setOnClickListener {
            showAddWishlistDialog()
            // findNavController().navigate(CrossTrackerFragmentDirections
            //     .actionFromCrossTrackerToSearch())
        }

        filterChipGroup.setOnCheckedStateChangeListener { _, checkedIds ->
            val commutativeCrossingEnabled = mPref.getBoolean(mKeyUtil.commutativeCrossingKey, false)
            when (checkedIds.firstOrNull()) {
                R.id.filter_all -> currentFilter = CrossFilter.ALL
                R.id.filter_planned -> currentFilter = CrossFilter.PLANNED
                R.id.filter_unplanned -> currentFilter = CrossFilter.UNPLANNED
            }
            val filteredData = filterResults()
            val groupedData = groupCrosses(filteredData, commutativeCrossingEnabled)
            updateNoDataTextVisibility(groupedData)
            crossAdapter.submitList(groupedData) {
                mBinding.crossesRecyclerView.scrollToPosition(0)
            }
        }
    }

    private fun startObservers() {

        loadData()

        metaValuesViewModel.metaValues.observe(viewLifecycleOwner) {
            it?.let { mMetaValuesList = it }
        }

        metadataViewModel.metadata.observe(viewLifecycleOwner) {
            it?.let { mMetaList = it }
        }

        parentsModel.parents.observe(viewLifecycleOwner) { list ->
            mParents = list ?: emptyList()
            parentLookup = mParents.associate { it.codeId to it.name }
        }

        /**
         * Keep track if wishlist repo is empty to disable options items menu
         */
        wishModel.wishlist.observe(viewLifecycleOwner) { wishes ->

            mWishlistEmpty = wishes.none { it.wishType == getString(R.string.literal_cross) }
            updateToolbarWishlistIcon()
        }
    }

    private fun loadData() {
        val commutativeCrossingEnabled = mPref.getBoolean(mKeyUtil.commutativeCrossingKey, false)

        val wishesLiveData = if (commutativeCrossingEnabled) {
            wishModel.commutativeWishes
        } else {
            wishModel.wishes
        }

        eventsModel.allParents.observe(viewLifecycleOwner) { crosses ->
            crosses?.let{
                wishesLiveData.observe(viewLifecycleOwner) { wishes ->
                    processWishesData(crosses, wishes, commutativeCrossingEnabled)
                }
            }
        }
    }

    private fun processWishesData(
        crosses: List<EventsDao.ParentCount>,
        wishes: List<WishlistView>?,
        commutativeCrossingEnabled: Boolean
    ) {
        val crossData = getCrosses(crosses, wishes, commutativeCrossingEnabled)

        // Add remaining wishes that don't have crosses
        val remainingWishes = getRemainingWishes(wishes, crossData, commutativeCrossingEnabled)

        crossesAndWishesData = crossData + remainingWishes

        val filteredData = filterResults()

        // If commutative, group the data
        val groupedData = groupCrosses(filteredData, commutativeCrossingEnabled)

        updateNoDataTextVisibility(groupedData)

        crossAdapter.submitList(groupedData)
    }

    /**
     * Returns both planned (from wishlist) and unplanned crosses
     */
    private fun getCrosses(
        crosses: List<EventsDao.ParentCount>,
        wishes: List<WishlistView>?,
        commutativeCrossingEnabled: Boolean,
    ): List<ListEntry> {
        return crosses.map { parentRow ->
            val normalizedParents = normalizeParents(parentRow.dad, parentRow.mom)

            // each cross will have a person and date with count initialized to 1 if they exist
            // these counts will then be aggregated in groupCrosses method
            val personCount = if (parentRow.person.isNotEmpty()) {
                listOf(PersonCount(parentRow.person, 1))
            } else emptyList()

            val dateCount = if (parentRow.date.isNotEmpty()) {
                listOf(DateCount(parentRow.date, 1))
            } else emptyList()

            val wish = if (commutativeCrossingEnabled) {
                wishes?.find { w ->
                    val wishParents = normalizeParents(w.dadId, w.momId)

                    (wishParents.dad == normalizedParents.dad && wishParents.mom == normalizedParents.mom) ||
                            (wishParents.dad == normalizedParents.mom && wishParents.mom == normalizedParents.dad)
                }
            } else {
                wishes?.find { w ->
                    val wishParents = normalizeParents(w.dadId, w.momId)

                    wishParents.dad == normalizedParents.dad && wishParents.mom == normalizedParents.mom
                }
            }

            if (wish == null) { // parents are not from wishlist
                UnplannedCrossData(
                    normalizedParents.dad,
                    normalizedParents.mom,
                    parentRow.count.toString(),
                    personCount,
                    dateCount
                )
            } else { // parents are in the wishlist
                PlannedCrossData(
                    wish.dadName,
                    wish.momName,
                    parentRow.count.toString(),
                    personCount,
                    dateCount,
                    wish.dadId,
                    wish.momId,
                    wishes = listOf(
                        WishlistItem(
                            wishType = wish.wishType,
                            min = wish.wishMin,
                            max = wish.wishMax,
                            progress = wish.wishProgress
                        )
                    )
                )
            }
        }
    }

    private fun getRemainingWishes(
        wishes: List<WishlistView>?,
        crossData: List<ListEntry>,
        commutativeCrossingEnabled: Boolean,
    ): List<PlannedCrossData> {
        return wishes?.filter { wish ->
            val wishParents = normalizeParents(wish.dadId, wish.momId)

            if (commutativeCrossingEnabled) {
                crossData.none { cross ->
                    val crossParents = normalizeParents(cross.male, cross.female)

                    (crossParents.dad == wishParents.dad && crossParents.mom == wishParents.mom) ||
                            (crossParents.dad == wishParents.mom && crossParents.mom == wishParents.dad)
                }
            } else {
                crossData.none { cross ->
                    val crossParents = normalizeParents(cross.male, cross.female)

                    crossParents.dad == wishParents.dad && crossParents.mom == wishParents.mom
                }
            }
        }?.map { wish ->
            PlannedCrossData(
                wish.dadName,
                wish.momName,
                "0",
                emptyList(),
                emptyList(),
                wish.dadId,
                wish.momId,
                wishes = listOf(
                    WishlistItem(
                        wishType = wish.wishType,
                        min = wish.wishMin,
                        max = wish.wishMax,
                        progress = wish.wishProgress
                    )
                )
            )
        } ?: emptyList()
    }

    private fun filterResults(): List<ListEntry> {
        return when (currentFilter) {
            CrossFilter.ALL -> crossesAndWishesData
            CrossFilter.PLANNED -> crossesAndWishesData.filterIsInstance<PlannedCrossData>()
            CrossFilter.UNPLANNED -> crossesAndWishesData.filterIsInstance<UnplannedCrossData>()
        }
    }

    /**
     * Merges wishlist items from multiple planned crosses, combining by wish type
     */
    private fun mergeWishlistItems(entries: List<PlannedCrossData>): List<WishlistItem> {
        return entries.flatMap { it.wishes }
            .groupBy { it.wishType }
            .map { (_, list) ->
                // combine progress across duplicates; MIN/MAX typically same per type,
                // but to be safe, keep min/min and max/max; sum progress
                WishlistItem(
                    wishType = list[0].wishType,
                    min = list.minOf { it.min },
                    max = list.maxOf { it.max },
                    progress = list.sumOf { it.progress }
                )
            }
    }

    /**
     * When user chooses to hide completed wishlist items
     * The planned wish item who's progress is at least meeting the wishMin are filtered out (hidden)
     */
    private fun filterCrossDataVisibility(data: List<ListEntry>, showCompleted: Boolean): List<ListEntry> {
        return if (!showCompleted) {
            data.filter { entry ->
                when (entry) {
                    is PlannedCrossData -> entry.wishes.any { it.progress < it.min } // keep if ANY wish type is still incomplete (progress < min)
                    else -> true // keep all unplanned crosses
                }
            }
        } else data // don't hide any CrossData
    }

    /**
     * Generates a hash for a grouped CrossData
     */
    private fun generateGroupKey(cross: ListEntry, commutativeCrossingEnabled: Boolean): Int {
        return if (commutativeCrossingEnabled) {
            if (cross.male < cross.female) "${cross.male}${cross.female}".hashCode()
            else "${cross.female}${cross.male}".hashCode()
        } else {
            "${cross.male}${cross.female}".hashCode()
        }
    }

    private fun groupCrosses(
        filteredData: List<ListEntry>,
        commutativeCrossingEnabled: Boolean,
    ): List<ListEntry> {
        val showCompleted = mPref.getBoolean(mKeyUtil.showCompletedWishlistItems, true)

        val groupedData = filteredData.groupBy { generateGroupKey(it, commutativeCrossingEnabled) }.map { entry ->
            if (entry.value.size == 1) entry.value[0]
            else {
                val totalCount = entry.value.sumOf { it.count.toInt() }.toString()
                val persons = aggregatePersonCounts(entry.value)
                val dates = aggregateDateCounts(entry.value)

                when (val firstCross = entry.value[0]) {
                    is UnplannedCrossData -> firstCross.copy(
                        count = totalCount,
                        persons = persons,
                        dates = dates
                    )
                    is PlannedCrossData -> {
                        // Merge wish lists by type; keep the max progress/min/max where appropriate
                        val mergedWishes = mergeWishlistItems(entry.value.filterIsInstance<PlannedCrossData>())

                        firstCross.copy(
                            count = totalCount,
                            persons = persons,
                            dates = dates,
                            wishes = mergedWishes
                        )
                    }
                    else -> firstCross // this will never be executed
                }
            }
        }

        // update progress of PlannedCrossData and filter visibility
        return filterCrossDataVisibility(updateWishProgress(groupedData), showCompleted)
    }

    /**
     *  Updates the wishProgress of each PlannedCross' WishlistItems once grouping is done
     */
    private fun updateWishProgress(listEntry: List<ListEntry>): List<ListEntry> {
        return listEntry.map { entry ->
            if (entry is PlannedCrossData) {
                val computed = entry.wishes.map { wish ->
                    val p = getWishItemProgress(entry, wish.wishType)
                    wish.copy(progress = p)
                }
                entry.copy(wishes = computed)
            } else entry
        }
    }

    // private fun FragmentCrossTrackerBinding.setupTabLayout() {
    //
    //     summaryTabLayout.addOnTabSelectedListener(tabSelected { tab ->
    //
    //         when (tab?.position) {
    //             1 ->
    //                 Navigation.findNavController(mBinding.root)
    //                     .navigate(CrossTrackerFragmentDirections.actionToSummary())
    //         }
    //     })
    // }

    override fun onResume() {
        super.onResume()

        (activity as? AppCompatActivity)?.setSupportActionBar(mBinding.fragCrossTrackerTb)

        updateToolbarWishlistIcon()

        mBinding.bottomNavBar.selectedItemId = R.id.action_nav_crosses

    }

    override fun onCreate(savedInstanceState: Bundle?) {

        super.onCreate(savedInstanceState)

        setHasOptionsMenu(true)
    }

    override fun onCreateOptionsMenu(menu: Menu, inflater: MenuInflater) {

        inflater.inflate(R.menu.cross_tracker_toolbar, menu)

        systemMenu = menu

        systemMenu?.findItem(R.id.action_toggle_completed_wishlist_visibility)?.let { menuItem ->
            // set icon based on preference
            val currentValue = mPref.getBoolean(mKeyUtil.showCompletedWishlistItems, true)
            setToggleVisibilityIcon(menuItem, currentValue)
        }

        super.onCreateOptionsMenu(menu, inflater)
    }

    private fun updateToolbarWishlistIcon() {
        systemMenu?.findItem(R.id.action_to_crossblock)?.isVisible = !mWishlistEmpty
    }

    private fun updateNoDataTextVisibility(data: List<ListEntry>) {
        mBinding.noDataText.visibility = if (data.isEmpty()) View.VISIBLE else View.GONE
    }

    private fun setToggleVisibilityIcon(menuItem: MenuItem, newValue: Boolean) {
        mPref.edit { putBoolean(mKeyUtil.showCompletedWishlistItems, newValue) }

        menuItem.setIcon(if (newValue) R.drawable.ic_show_wishlist_completed else R.drawable.ic_hide_wishlist_completed)
        menuItem.setTitle(if (newValue) R.string.crosses_toolbar_hide_completed else R.string.crosses_toolbar_show_completed)
    }

    override fun onOptionsItemSelected(item: MenuItem): Boolean {

        when(item.itemId) {
            R.id.action_toggle_completed_wishlist_visibility -> {
                val currentValue = mPref.getBoolean(mKeyUtil.showCompletedWishlistItems, true)
                val newValue = !currentValue

                setToggleVisibilityIcon(item, newValue)

                val commutativeCrossingEnabled = mPref.getBoolean(mKeyUtil.commutativeCrossingKey, false)
                val grouped = groupCrosses(filterResults(), commutativeCrossingEnabled)

                crossAdapter.submitList(grouped) {
                    mBinding.crossesRecyclerView.scrollToPosition(0)
                }
            }
            R.id.action_to_crossblock -> {
                findNavController().navigate(CrossTrackerFragmentDirections.actionToCrossblock())
            }
            // R.id.action_cross_count_delete_all -> {
            //     val deleteFilter = when (currentFilter) {
            //         CrossFilter.ALL -> getString(R.string.dialog_cross_count_delete_both)
            //         CrossFilter.PLANNED -> getString(R.string.dialog_cross_count_delete_planned)
            //         else -> getString(R.string.dialog_cross_count_delete_unplanned)
            //     }
            //     context?.let { ctx ->
            //         Dialogs.onOk(AlertDialog.Builder(ctx),
            //             getString(R.string.menu_cross_count_delete_all_title),
            //             getString(android.R.string.cancel),
            //             getString(android.R.string.ok),
            //             String.format(getString(R.string.dialog_cross_count_delete_all_message), deleteFilter)) {
            //
            //             Dialogs.onOk(AlertDialog.Builder(ctx),
            //                 getString(R.string.menu_cross_count_delete_all_title),
            //                 getString(android.R.string.cancel),
            //                 getString(android.R.string.ok),
            //                 getString(R.string.dialog_cross_count_delete_all_message_2)) {
            //
            //                 if (currentFilter == CrossFilter.ALL || currentFilter == CrossFilter.UNPLANNED) {
            //                     eventsModel.deleteAll()
            //                 }
            //                 if (currentFilter == CrossFilter.ALL || currentFilter == CrossFilter.PLANNED) {
            //                     wishModel.deleteAll()
            //                 }
            //
            //                 findNavController().popBackStack()
            //             }
            //         }
            //     }
            // }
        }

        return super.onOptionsItemSelected(item)
    }

    private fun FragmentCrossTrackerBinding.setupBottomNavBar() {

        bottomNavBar.setOnNavigationItemSelectedListener { item ->

            when (item.itemId) {

                R.id.action_nav_preferences -> {

                    findNavController().navigate(CrossTrackerFragmentDirections.globalActionToPreferencesFragment())
                }
                R.id.action_nav_summary -> {

                    findNavController().navigate(CrossTrackerFragmentDirections.actionToSummary())

                }
                R.id.action_nav_parents -> {

                    findNavController().navigate(CrossTrackerFragmentDirections.globalActionToParents())

                }
                R.id.action_nav_home -> {

                    findNavController().navigate(CrossTrackerFragmentDirections.globalActionToEvents())

                }
            }

            true
        }
    }

    override fun onCrossClicked(male: String, female: String) {
        mShowChildrenDialogUtil.showChildren(mEvents, male, female)
    }

    override fun onPersonChipClicked(persons: List<PersonCount>, crossCount: Int) {
        var message = ""
        var totalCollectorCount = 0
        persons.forEach {
            totalCollectorCount += it.count
            message += "${it.name.trim()}: ${it.count}\n"
        }
        if (totalCollectorCount < crossCount) {
            val remaining = crossCount - totalCollectorCount
            message += String.format(getString(R.string.dialog_crosses_by_person_known_message), remaining)
        }
        context?.let {
            AlertDialog.Builder(it)
                .setTitle(getString(R.string.dialog_crosses_by_persons))
                .setMessage(message)
                .setPositiveButton(getString(R.string.dialog_ok)) { d, _ -> d.dismiss() }
                .show()
        }
    }

    override fun onDateChipClicked(dates: List<DateCount>) {
        var message = ""
        dates.forEach {
            message += "${DateUtil().getFormattedDate(it.date)}: ${it.count}\n"
        }
        context?.let {
            AlertDialog.Builder(it)
                .setTitle(getString(R.string.dialog_crosses_by_dates))
                .setMessage(message)
                .setPositiveButton(getString(R.string.dialog_ok)) { d, _ -> d.dismiss() }
                .show()
        }
    }

    override fun onWishlistProgressChipClicked(wishlistItem: WishlistItem) {
        val wishType = wishlistItem.wishType
        val min = wishlistItem.min.toString()
        val max = wishlistItem.max.toString()
        val progress = wishlistItem.progress.toString()
        val message = String.format(getString(R.string.dialog_wish_progress_message), wishType, min, max, progress)

        context?.let {
            AlertDialog.Builder(it)
                .setTitle(getString(R.string.dialog_wish_progress_title))
                .setMessage(message)
                .setPositiveButton(getString(R.string.dialog_ok)) { d, _ -> d.dismiss() }
                .show()
        }
    }

    override fun onEventClick(eventId: Long) {
        mShowChildrenDialogUtil.dismiss()
        findNavController().navigate(CrossTrackerFragmentDirections.globalActionToEventDetail(eventId))
    }

    override fun getWishItemProgress(entry: PlannedCrossData, wishType: String): Int {
        // for "cross" just return the count
        if (wishType == getString(R.string.literal_cross)) return entry.count.toIntOrNull() ?: 0

        val isCommutative = mPref.getBoolean(mKeyUtil.commutativeCrossingKey, false)

        val normalizedEntryParents = normalizeParents(entry.maleId, entry.femaleId)

        val eventIds: Set<Int> = mEvents.asSequence()
            .filter { e ->
                val normalizedEventParents = normalizeParents(e.maleObsUnitDbId, e.femaleObsUnitDbId)

                if (isCommutative) {
                    (normalizedEventParents.dad == normalizedEntryParents.dad && normalizedEventParents.mom == normalizedEntryParents.mom) ||
                            (normalizedEventParents.dad == normalizedEntryParents.mom && normalizedEventParents.mom == normalizedEntryParents.dad)
                } else {
                    normalizedEventParents.dad == normalizedEntryParents.dad && normalizedEventParents.mom == normalizedEntryParents.mom
                }
            }
            .mapNotNull { it.id?.toInt() }
            .toSet()

        if (eventIds.isEmpty()) return 0

        val metaId = mMetaList.find { it.property == wishType }?.id?.toInt() ?: return 0

        // find cumulative count for this metadata across the events
        return mMetaValuesList.asSequence()
            .filter { mv -> mv.metaId == metaId && eventIds.contains(mv.eid) }
            .sumOf { it.value ?: 0 }
    }

    private fun showAddWishlistDialog() {
        val importArray: Array<String?> = arrayOf(
            context?.getString(R.string.dialog_import_wishlist_title),
            context?.getString(R.string.add_wishlist_item),
        )

        val icons = IntArray(importArray.size).apply {
            this[0] = R.drawable.ic_file_generic
            this[1] = R.drawable.ic_wishlist_add
        }

        val onItemClickListener =
            AdapterView.OnItemClickListener { _, _, position, _ ->
                when (position) {
                    0 -> { // import wishlist
                        context?.let {
                            ImportUtil(it, R.string.dir_wishlist_import, getString(R.string.dialog_import_wishlist_title))
                                .showImportDialog(this)
                        }
                    }
                    // create new wishlist item
                    1 -> findNavController().navigate(CrossTrackerFragmentDirections.actionFromCrossTrackerToWishFactory())
                }
            }

        activity?.let { fragmentActivity ->
            val dialog = context?.getString(R.string.dialog_new_wishlist_title)?.let {
                dialogTitle -> ListAddDialog(fragmentActivity, dialogTitle, importArray, icons, onItemClickListener)
            }
            dialog?.show(fragmentActivity.supportFragmentManager, "ListAddDialog")
        }
    }

    /**
     * Aggregates person counts across multiple list entries
     */
    private fun aggregatePersonCounts(entries: List<ListEntry>): List<PersonCount> {
        return entries.flatMap { it.persons }
            .groupBy { it.name }
            .map { (name, counts) ->
                PersonCount(name, counts.sumOf { it.count })
            }
    }

    /**
     * Aggregates date counts across multiple list entries, formatting dates consistently
     * Format we want to show in: yyyy-MM-dd
     */
    private fun aggregateDateCounts(entries: List<ListEntry>): List<DateCount> {
        return entries.flatMap { it.dates }
            .groupBy { DateUtil().getFormattedDate(it.date) }
            .map { (_, dateCounts) ->
                DateCount(dateCounts.first().date, dateCounts.sumOf { it.count })
            }
    }
}