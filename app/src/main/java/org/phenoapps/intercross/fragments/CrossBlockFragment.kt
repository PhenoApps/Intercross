package org.phenoapps.intercross.fragments

import android.content.SharedPreferences
import android.graphics.Color
import android.os.Bundle
import android.view.*
import androidx.fragment.app.viewModels
import androidx.navigation.fragment.findNavController
import androidx.recyclerview.widget.RecyclerView
import com.evrencoskun.tableview.listener.ITableViewListener
import dagger.hilt.android.AndroidEntryPoint
import org.phenoapps.intercross.activities.MainActivity
import org.phenoapps.intercross.R
import org.phenoapps.intercross.adapters.CrossBlockTableViewAdapter
import org.phenoapps.intercross.data.EventsRepository
import org.phenoapps.intercross.data.WishlistRepository
import org.phenoapps.intercross.data.models.Event
import org.phenoapps.intercross.data.models.WishlistView
import org.phenoapps.intercross.data.viewmodels.EventListViewModel
import org.phenoapps.intercross.data.viewmodels.WishlistViewModel
import org.phenoapps.intercross.data.viewmodels.factory.EventsListViewModelFactory
import org.phenoapps.intercross.data.viewmodels.factory.WishlistViewModelFactory
import org.phenoapps.intercross.databinding.FragmentCrossBlockBinding
import org.phenoapps.intercross.interfaces.EventClickListener
import org.phenoapps.intercross.util.KeyUtil
import org.phenoapps.intercross.util.ShowChildrenDialogUtil
import org.phenoapps.intercross.util.WishProgressColorUtil
import javax.inject.Inject
import androidx.core.content.edit

@AndroidEntryPoint
class CrossBlockFragment : IntercrossBaseFragment<FragmentCrossBlockBinding>(R.layout.fragment_cross_block),
    ITableViewListener,
    EventClickListener {

    private val eventsModel: EventListViewModel by viewModels {
        EventsListViewModelFactory(EventsRepository.getInstance(db.eventsDao()))
    }

    private val wishModel: WishlistViewModel by viewModels {
        WishlistViewModelFactory(WishlistRepository.getInstance(db.wishlistDao()))
    }

    //cell data contains a text field for headers
    //fid/mid for clicking on the cell so we can query the wishlistview row
    //and progressColor that colors the cell depending on progress (defined in legend)
    data class CellData(val text: String = "", val fid: String = "", val mid: String = "", val progressColor: Int = Color.GRAY)

    private lateinit var mWishlist: List<WishlistView>

    private var mEvents: List<Event> = ArrayList()

    @Inject
    lateinit var mPref: SharedPreferences

    @Inject
    lateinit var mKeyUtil: KeyUtil

    private val mShowChildrenDialogUtil by lazy {
        ShowChildrenDialogUtil(
            this,
            context,
            { male, female ->
                findNavController()
                    .navigate(CrossBlockFragmentDirections
                        .actionFromCrossblockToEventsList(male, female))
            },
            eventsModel,
            this,
            mPref.getBoolean(mKeyUtil.commutativeCrossingKey, false)
        )
    }


    override fun FragmentCrossBlockBinding.afterCreateView() {

        (activity as MainActivity).setBackButtonToolbar()
        (activity as MainActivity).supportActionBar?.apply {
            title = getString(R.string.crossblock)
            show()
        }

        setHasOptionsMenu(true)

        mPref.edit { putString("last_visited_summary", "crossblock") }

        val isCommutative = mPref.getBoolean(mKeyUtil.commutativeCrossingKey, false)

        /**
         * list for events model, disable options menu for summary if the list is empty
         */
        eventsModel.events.observe(viewLifecycleOwner) {

            it?.let {

                mEvents = it

                if (isCommutative) {

                    wishModel.commutativeCrossblock.observe(viewLifecycleOwner) { block ->

                        mWishlist = block

                        setupTable(mWishlist)

                    }

                } else {

                    wishModel.crossblock.observe(viewLifecycleOwner) { block ->

                        mWishlist = block

                        setupTable(mWishlist)

                    }
                }
            }
        }
    }

    /**
     * Displays cross block table.
     */
    private fun setupTable(entries: List<WishlistView>) {

        val maleColumnHeaders = entries.map { CellData(it.dadName, it.momId, it.dadId) }
            .distinctBy { it.mid }.sortedBy { it.mid }
        val femaleRowHeaders = entries.map { CellData(it.momName, it.momId, it.dadId) }
            .distinctBy { it.fid }.sortedBy { it.fid }

        val data = arrayListOf<List<CellData>>()
        for (female in femaleRowHeaders) {
            val row = arrayListOf<CellData>()
            for (male in maleColumnHeaders) {
                val wish = entries.find { it.momId == female.fid && it.dadId == male.mid }
                if (wish == null) {
                    row.add(CellData())
                } else {
                    context?.let {
                        val color = WishProgressColorUtil().getProgressColor(it, wish.wishProgress, wish.wishMin, wish.wishMax)
                        row.add(CellData(fid = wish.momId, mid = wish.dadId, progressColor = color))
                    }
                }
            }
            data.add(row)
        }

        with(mBinding.fragmentCrossblockTableView) {
            isIgnoreSelectionColors = true
            tableViewListener = this@CrossBlockFragment
            isShowHorizontalSeparators = true
            isShowVerticalSeparators = true

            setAdapter(CrossBlockTableViewAdapter())

            (adapter as? CrossBlockTableViewAdapter)?.setAllItems(
                maleColumnHeaders,
                femaleRowHeaders,
                data
            )
        }
    }

    override fun onOptionsItemSelected(item: MenuItem): Boolean {
        when (item.itemId) {
            R.id.action_parents_toolbar_initiate_wf -> {

                findNavController().navigate(CrossBlockFragmentDirections
                    .actionFromCrossblockToWishFactory())
            }
        }
        return super.onOptionsItemSelected(item)
    }

    override fun onCreateOptionsMenu(menu: Menu, inflater: MenuInflater) {
        super.onCreateOptionsMenu(menu, inflater)

        inflater.inflate(R.menu.crossblock_toolbar, menu)

    }

    override fun onCreate(savedInstanceState: Bundle?) {

        super.onCreate(savedInstanceState)

        setHasOptionsMenu(true)
    }

    override fun onCellClicked(cellView: RecyclerView.ViewHolder, column: Int, row: Int) {
        mBinding.fragmentCrossblockTableView.adapter?.getCellItem(column, row)?.let { r ->

            val cell = (r as? CellData)
            val mid = cell?.mid
            val fid = cell?.fid

            mid?.let { maleId ->
                fid?.let { femaleId ->
                    mShowChildrenDialogUtil.showChildren(mEvents, maleId, femaleId)
                }
            }
        }
    }

    override fun onEventClick(eventId: Long) {
        mShowChildrenDialogUtil.dismiss()
        findNavController()
            .navigate(CrossBlockFragmentDirections
                .actionToEventDetail(eventId))
    }

    override fun onColumnHeaderClicked(columnHeaderView: RecyclerView.ViewHolder, column: Int) {}
    override fun onCellDoubleClicked(cellView: RecyclerView.ViewHolder, column: Int, row: Int) {}
    override fun onCellLongPressed(cellView: RecyclerView.ViewHolder, column: Int, row: Int) {}
    override fun onColumnHeaderDoubleClicked(columnHeaderView: RecyclerView.ViewHolder, column: Int) {}
    override fun onColumnHeaderLongPressed(columnHeaderView: RecyclerView.ViewHolder, column: Int) {}
    override fun onRowHeaderClicked(rowHeaderView: RecyclerView.ViewHolder, row: Int) {}
    override fun onRowHeaderDoubleClicked(rowHeaderView: RecyclerView.ViewHolder, row: Int) {}
    override fun onRowHeaderLongPressed(rowHeaderView: RecyclerView.ViewHolder, row: Int) {}
}