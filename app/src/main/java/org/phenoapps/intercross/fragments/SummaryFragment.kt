package org.phenoapps.intercross.fragments

import android.graphics.Color
import android.os.Bundle
import android.view.View
import android.widget.TextView
import androidx.appcompat.app.AppCompatActivity
import androidx.core.content.ContextCompat
import androidx.core.view.children
import androidx.fragment.app.viewModels
import androidx.navigation.fragment.findNavController
import com.github.mikephil.charting.animation.Easing
import com.github.mikephil.charting.charts.LineChart
import com.github.mikephil.charting.charts.PieChart
import com.github.mikephil.charting.components.XAxis
import com.github.mikephil.charting.data.*
import com.github.mikephil.charting.formatter.IndexAxisValueFormatter
import com.github.mikephil.charting.formatter.ValueFormatter
import com.google.android.material.tabs.TabLayout
import org.phenoapps.intercross.R
import org.phenoapps.intercross.activities.MainActivity
import org.phenoapps.intercross.data.EventsRepository
import org.phenoapps.intercross.data.ParentsRepository
import org.phenoapps.intercross.data.WishlistRepository
import org.phenoapps.intercross.data.dao.EventsDao
import org.phenoapps.intercross.data.models.CrossType
import org.phenoapps.intercross.data.models.Event
import org.phenoapps.intercross.data.models.Parent
import org.phenoapps.intercross.data.models.Wishlist
import org.phenoapps.intercross.data.viewmodels.EventListViewModel
import org.phenoapps.intercross.data.viewmodels.ParentsListViewModel
import org.phenoapps.intercross.data.viewmodels.WishlistViewModel
import org.phenoapps.intercross.data.viewmodels.factory.EventsListViewModelFactory
import org.phenoapps.intercross.data.viewmodels.factory.ParentsListViewModelFactory
import org.phenoapps.intercross.data.viewmodels.factory.WishlistViewModelFactory
import org.phenoapps.intercross.databinding.FragmentDataSummaryBinding
import org.phenoapps.intercross.util.DateUtil
import org.phenoapps.intercross.util.observeOnce
import java.util.*

/**
 * @author: Chaney
 *
 * Summary Fragment displays accumulated data using a pie chart and recycler view.
 * Summary fragment can be chosen from the main menu (currently the nav drawer)
 * This fragment has a tab layout to select three different categories:
 * 1. Sex: this category displays total males, females and unknowns (these are crosses) while m/fs are parents
 *  a. unknowns are no longer displayed issue37b
 * 2. Type: accumulated cross types e.g Biparental, Open, Self
 * 3. Meta: accumulated meta data fields (currently only fruits, seeds and flowers)
 *
 * Meta data is now displayed using a bar chart.
 *
 * If counted data is 0 then it is not displayed. This may result in a blank tab, improvements might include a default message when there's no data.
 * TODO: the graph library allows clicking different sections of the piechart, maybe this should change the displayed data somehow
 */
class SummaryFragment : IntercrossBaseFragment<FragmentDataSummaryBinding>(R.layout.fragment_data_summary) {

    private val eventsModel: EventListViewModel by viewModels {
        EventsListViewModelFactory(EventsRepository.getInstance(db.eventsDao()))
    }

    private val parentsModel: ParentsListViewModel by viewModels {
        ParentsListViewModelFactory(ParentsRepository.getInstance(db.parentsDao()))
    }

    private val wishModel: WishlistViewModel by viewModels {
        WishlistViewModelFactory(WishlistRepository.getInstance(db.wishlistDao()))
    }

    private val MAX_LABEL_LENGTH = 16

    private lateinit var mEvents: List<Event>
    private lateinit var mParents: List<Parent>
    private lateinit var mWishlist: List<Wishlist>
    private lateinit var mMetadata: List<EventsDao.CrossMetadata>

    private lateinit var graphColors: Array<Int>

    //a quick wrapper function for tab selection
    private fun tabSelected(onSelect: (TabLayout.Tab?) -> Unit) = object : TabLayout.OnTabSelectedListener {
        override fun onTabSelected(tab: TabLayout.Tab?) {
           onSelect(tab)
        }
        override fun onTabUnselected(tab: TabLayout.Tab?) {}
        override fun onTabReselected(tab: TabLayout.Tab?) {}
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        graphColors = arrayOf(
            ContextCompat.getColor(requireContext(), R.color.colorAccentTransparent),
            ContextCompat.getColor(requireContext(), R.color.colorAccent),
            ContextCompat.getColor(requireContext(), R.color.progressBlank),
            ContextCompat.getColor(requireContext(), R.color.progressStart),
            ContextCompat.getColor(requireContext(), R.color.progressLessThanTwoThird),
        )

        setHasOptionsMenu(true)
    }

    override fun FragmentDataSummaryBinding.afterCreateView() {

        (activity as MainActivity).applyFragmentInsets(root, fragSummaryTb)

        //initialize pie chart parameters, this is mostly taken from the github examples
        setupPieChart(typeSummaryPieChart)
        setupLineChart(crossesOverTimeChart)
        setupBarChart()

        //listen to events and parents once and then displays the data
        //if somehow events are injected after afterCreateView, the graph won't update
        //but that could only happen if someone used the database inspector
        startObservers()

        bottomNavBar.selectedItemId = R.id.action_nav_summary

        setupBottomNavBar()
    }

    // override fun onOptionsItemSelected(item: MenuItem): Boolean {
    //
    //     when(item.itemId) {
    //         R.id.action_to_crossblock -> {
    //             findNavController().navigate(SummaryFragmentDirections.actionToCrossblock())
    //         }
    //     }
    //     return super.onOptionsItemSelected(item)
    // }


    override fun onResume() {
        super.onResume()

        (activity as? AppCompatActivity)?.setSupportActionBar(mBinding.fragSummaryTb)

        mBinding.bottomNavBar.menu.findItem(R.id.action_nav_summary).isEnabled = false

        mBinding.bottomNavBar.selectedItemId = R.id.action_nav_summary

        mBinding.bottomNavBar.menu.findItem(R.id.action_nav_summary).isEnabled = true

    }

    //used to load label/value pair data into the adapter's view holder
    open class ListEntry(open var label: String, open var value: Float)

    private fun FragmentDataSummaryBinding.setupBottomNavBar() {

        bottomNavBar.setOnNavigationItemSelectedListener { item ->

            when (item.itemId) {

                R.id.action_nav_home -> {

                    findNavController().navigate(SummaryFragmentDirections.globalActionToEvents())
                }
                R.id.action_nav_preferences -> {

                    findNavController().navigate(SummaryFragmentDirections.globalActionToPreferencesFragment())
                }
                R.id.action_nav_parents -> {

                    findNavController().navigate(SummaryFragmentDirections.globalActionToParents())

                }
                R.id.action_nav_crosses -> {

                    findNavController().navigate(SummaryFragmentDirections.globalActionToCrossTracker())

                }
            }

            true
        }
    }

    /**
     * Cascade-load event and parent data, once the data is loaded trigger the first tab data to load.
     */
    private fun FragmentDataSummaryBinding.startObservers() {

        eventsModel.events.observeOnce(viewLifecycleOwner) { data ->

            mEvents = data

            eventsModel.metadata.observeOnce(viewLifecycleOwner) { metadata ->
                mMetadata = metadata
                setData(setMetaData())
            }

            val (dataset, xAxisLabels) = setCrossesOverTimeData()
            setData(crossesOverTimeChart, crossesNoDataText, dataset, xAxisLabels)

            setData(typeSummaryPieChart, typeNoDataText, setTypeData())
        }

        wishModel.wishlist.observe(viewLifecycleOwner) { wishes ->

            mWishlist = wishes.filter { it.wishType == "cross" }
        }
    }

    //initializes view parameters s.a recycler view and pie chart
    private fun setupPieChart(dataSummaryPieChart: PieChart) {

        dataSummaryPieChart.setUsePercentValues(false)
        dataSummaryPieChart.description.isEnabled = false
        dataSummaryPieChart.setExtraOffsets(5f, 10f, 5f, 5f)

        dataSummaryPieChart.dragDecelerationFrictionCoef = 0.95f

        //chart.setCenterTextTypeface(tfLight)
        //chart.centerText = generateCenterSpannableText()

        dataSummaryPieChart.isDrawHoleEnabled = true
        dataSummaryPieChart.setHoleColor(Color.WHITE)

        dataSummaryPieChart.setTransparentCircleColor(Color.WHITE)
        dataSummaryPieChart.setTransparentCircleAlpha(110)

        dataSummaryPieChart.holeRadius = 58f
        dataSummaryPieChart.transparentCircleRadius = 61f

        dataSummaryPieChart.setDrawCenterText(true)

        dataSummaryPieChart.rotationAngle = 0f
        // enable rotation of the chart by touch
        // enable rotation of the chart by touch
        dataSummaryPieChart.isRotationEnabled = true
        dataSummaryPieChart.isHighlightPerTapEnabled = true

        // add a selection listener
        //chart.setOnChartValueSelectedListener(this)

        //issue 37b remove legend
//        val l: Legend = dataSummaryPieChart.legend
//        l.verticalAlignment = Legend.LegendVerticalAlignment.TOP
//        l.horizontalAlignment = Legend.LegendHorizontalAlignment.RIGHT
//        l.orientation = Legend.LegendOrientation.VERTICAL
//        l.setDrawInside(false)
//        l.xEntrySpace = 7f
//        l.yEntrySpace = 0f
//        l.yOffset = 0f

        dataSummaryPieChart.legend.isEnabled = false

        // entry label styling
        dataSummaryPieChart.setEntryLabelColor(Color.BLACK)
        //chart.setEntryLabelTypeface(tfRegular)
        dataSummaryPieChart.setEntryLabelTextSize(12f)
    }

    private fun FragmentDataSummaryBinding.setupBarChart() {

        metadataSummaryBarChart.setDrawBarShadow(false)

        //dataSummaryBarChart.setDrawValueAboveBar(true)

        metadataSummaryBarChart.description.isEnabled = false

        metadataSummaryBarChart.setPinchZoom(false)

        metadataSummaryBarChart.legend.isEnabled = false

        metadataSummaryBarChart.setDrawGridBackground(false)

        metadataSummaryBarChart.xAxis.textSize = 10f

        metadataSummaryBarChart.xAxis.textColor = Color.BLACK
    }

    private fun setSexData(): PieDataSet = PieDataSet(ArrayList<PieEntry>().apply {

        if (::mParents.isInitialized) {
            val males = mParents.count { it.sex == 1 }.toFloat()
            if (males > 0f) add(PieEntry(males, "Males"))
            val females = mParents.count { it.sex == 0 }.toFloat()
            if (females > 0f) add(PieEntry(females, "Females"))
        }
        //issue 37b remove unknown sexes from pie chart
//        if (::mEvents.isInitialized) {
//            if (::mParents.isInitialized) {
//                val unk = mEvents.size.toFloat() + mParents.count { it.sex == -1 }.toFloat()
//                if (unk > 0f) add(PieEntry(unk, "Unknown"))
//            } else {
//                val unk = mEvents.size.toFloat()
//                if (unk > 0f) add(PieEntry(unk, "Unknown"))
//            }
//        }
    },"Sex Statistics")

    private fun setTypeData(): PieDataSet = PieDataSet(ArrayList<PieEntry>().apply {

        val biparental = getString(R.string.cross_type_biparental)
        val open = getString(R.string.cross_type_open)
        val self = getString(R.string.cross_type_self)
        val unknown = getString(R.string.cross_type_unknown)
        val poly = getString(R.string.cross_type_poly)

        if (::mEvents.isInitialized)
            for (type in CrossType.entries) {
                val items = mEvents.count { it.type == type }.toFloat()
                if (items > 0f) add(
                    PieEntry(
                        items,
                        when (type) {
                            CrossType.BIPARENTAL -> biparental
                            CrossType.OPEN -> open
                            CrossType.POLY -> poly
                            CrossType.SELF -> self
                            CrossType.UNKNOWN -> unknown
                        }
                    )
                )
            }
    },"Cross Type Statistics")

    /**
     * Extension function that returns a list of property/value pairs for all metadata fields
     * For example Event eid=1 metadata={fruits=[1,2], seeds=[9,7]} would return
     * [fruits to 1, seeds to 9]
     */
    private fun toEntrySet(): List<Pair<String, Int?>> {

        return if (::mMetadata.isInitialized) {
            mMetadata.map {
                it.property to it.value
            }
        } else listOf()
    }

    private fun setMetaData(): BarDataSet = BarDataSet(ArrayList<BarEntry>().apply {

        if (::mEvents.isInitialized) {

            toEntrySet() // [(seeds, 1), (flowers, 1), (seeds, 2), ....]
                .groupBy { it.first }   //[(seeds, [1, 2]), (flowers, [1]), ...]
                .map { it.key to it.value.sumOf { values -> values.second ?: 0 } } //[(seeds, 3), (flowers, 1), ...]
                .mapIndexed { index, pair -> BarEntry(
                    index.toFloat(),
                    pair.second.toFloat(),
                    pair.first
                ) }.forEach(::add)
        }
    },"Meta Data Statistics")

    private fun setupLineChart(lineChart: LineChart) {
        lineChart.apply {
            description.isEnabled = false
            setTouchEnabled(true)
            isDragEnabled = true
            setScaleEnabled(false)

            xAxis.apply {
                position = XAxis.XAxisPosition.BOTTOM
                granularity = 1f
                setAvoidFirstLastClipping(true)
            }

            axisLeft.apply {
                granularity = 1f
            }

            axisRight.isEnabled = false

            legend.isEnabled = false
        }
    }

    private fun setCrossesOverTimeData(): Pair<LineDataSet, List<String>> {
        val entries = ArrayList<Entry>()
        var xAxisLabels = listOf<String>()

        if (::mEvents.isInitialized && mEvents.isNotEmpty()) {

            val sortedEvents = mEvents.sortedBy { it.timestamp }

            // group by dates
            val groupedDates = sortedEvents.groupBy { DateUtil().getFormattedDate(it.timestamp) }
            val dateCounts = groupedDates.mapValues { it.value.size }
            val uniqueDates = dateCounts.keys.sorted()

            var cumulative = 0
            val labels = mutableListOf<String>()
            uniqueDates.forEachIndexed { index, dateString ->
                cumulative += dateCounts[dateString] ?: 0
                entries.add(Entry(index.toFloat(), cumulative.toFloat()))
                labels.add(dateString)
            }

            xAxisLabels = labels
        }

        val dataSet = LineDataSet(entries, "Cumulative Crosses").apply {
            color = ContextCompat.getColor(requireContext(), R.color.colorAccent)
            lineWidth = 2f
            setCircleColor(ContextCompat.getColor(requireContext(), R.color.colorPrimaryDark))
            setDrawCircleHole(false)
            mode = LineDataSet.Mode.LINEAR
            valueFormatter = object : ValueFormatter() {
                override fun getFormattedValue(value: Float): String {
                    return value.toInt().toString()
                }
            }
        }

        return dataSet to xAxisLabels
    }

    private fun FragmentDataSummaryBinding.setData(lineChart: LineChart, chartNoDataText: TextView, dataset: LineDataSet, xAxisLabels: List<String>) {

        if (dataset.entryCount == 0) {
            chartNoDataText.visibility = View.VISIBLE
            lineChart.visibility = View.GONE
            return
        }

        chartNoDataText.visibility = View.GONE
        lineChart.visibility = View.VISIBLE

        val lineData = LineData(dataset)
        lineChart.data = lineData

        lineChart.xAxis.valueFormatter = IndexAxisValueFormatter(xAxisLabels)

        lineChart.invalidate()
    }

    //sets the accumulated data into the piechart and recycler view, along with some misc. parameter setting
    private fun FragmentDataSummaryBinding.setData(dataPieChart: PieChart, chartNoDataText: TextView, dataset: PieDataSet) {
        if (dataset.entryCount == 0) {
            chartNoDataText.visibility = View.VISIBLE
            dataPieChart.visibility = View.GONE
            return
        }

        activity?.currentFocus?.clearFocus()

        chartNoDataText.visibility = View.GONE
        dataPieChart.visibility = View.VISIBLE

        dataPieChart.animateY(1400, Easing.EaseInOutQuad)

        dataset.setDrawIcons(false)
        dataset.sliceSpace = 3f
        //dataset.iconsOffset = MPPointF(0f, 40f)
        dataset.selectionShift = 5f

        dataset.colors = graphColors.toMutableList()
        //dataSet.setSelectionShift(0f);
        val data = PieData(dataset)
        data.setValueFormatter(object : ValueFormatter() {
            override fun getFormattedValue(value: Float): String {
                return value.toInt().toString()
            }
        })
        data.setValueTextSize(11f)
        data.setValueTextColor(Color.BLACK)
        //data.setValueTypeface(tfLight)
        dataPieChart.data = data

        // undo all highlights
        dataPieChart.highlightValues(null)
        dataPieChart.invalidate()
    }

    private fun FragmentDataSummaryBinding.setData(dataset: BarDataSet) {
        if (dataset.entryCount == 0) {
            metadataSummaryCard.children
            metaNoDataText.visibility = View.VISIBLE
            metadataSummaryBarChart.visibility = View.GONE
            return
        }

        activity?.currentFocus?.clearFocus()

        metadataSummaryBarChart.visibility = View.VISIBLE

        dataset.colors = graphColors.toMutableList()
        val data = BarData(dataset)

        data.setValueTextColor(Color.TRANSPARENT)

        val labels = arrayListOf<String>()
        for (i in 0 until dataset.entryCount) {
            val label = dataset.getEntryForIndex(i).data as? String ?: "?"
            labels.add(if (label.length > MAX_LABEL_LENGTH)
                label.subSequence(0, MAX_LABEL_LENGTH).toString() else label)
        }

        //dataSummaryBarChart.setDrawValueAboveBar(true)

        metadataSummaryBarChart.xAxis.valueFormatter = IndexAxisValueFormatter(labels)

        metadataSummaryBarChart.xAxis.granularity = 1f

        metadataSummaryBarChart.data = data

        // undo all highlights
        metadataSummaryBarChart.highlightValues(null)
        metadataSummaryBarChart.invalidate()
    }
}