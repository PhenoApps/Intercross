package org.phenoapps.intercross.fragments

import android.content.SharedPreferences
import org.phenoapps.intercross.R
import android.os.Bundle
import android.text.Editable
import android.text.TextWatcher
import android.view.View
import androidx.fragment.app.viewModels
import androidx.navigation.fragment.findNavController
import androidx.recyclerview.widget.LinearLayoutManager
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.MainScope
import kotlinx.coroutines.launch
import org.phenoapps.intercross.activities.MainActivity
import org.phenoapps.intercross.adapters.SimpleListAdapter
import org.phenoapps.intercross.data.EventsRepository
import org.phenoapps.intercross.data.fts.CrossesDatabase
import org.phenoapps.intercross.data.fts.models.RankedCrosses
import org.phenoapps.intercross.data.fts.viewmodels.CrossesViewModel
import org.phenoapps.intercross.data.viewmodels.EventListViewModel
import org.phenoapps.intercross.data.viewmodels.factory.EventsListViewModelFactory
import org.phenoapps.intercross.databinding.FragmentSearchBinding
import org.phenoapps.intercross.interfaces.OnSimpleItemClicked
import org.phenoapps.intercross.util.KeyUtil
import org.phenoapps.intercross.util.observeOnce
import javax.inject.Inject

@AndroidEntryPoint
class SearchFragment : IntercrossBaseFragment<FragmentSearchBinding>(R.layout.fragment_search),
    CoroutineScope by MainScope(), OnSimpleItemClicked {

    private var mFtsDatabase: CrossesDatabase? = null
    private var mViewModel: CrossesViewModel? = null

    private val mScope by lazy {
        CoroutineScope(Dispatchers.IO)
    }

    private val eventsViewModel: EventListViewModel by viewModels {
        EventsListViewModelFactory(EventsRepository.getInstance(db.eventsDao()))
    }

    @Inject
    lateinit var mPref: SharedPreferences

    @Inject
    lateinit var mKeyUtil: KeyUtil

    private fun FragmentSearchBinding.startSearch(query: String) {

        val male = getString(R.string.male_parent)
        val female = getString(R.string.female_parent)

        mViewModel?.searchResult(query)?.observe(viewLifecycleOwner) {

            activity?.runOnUiThread {

                val crosses: List<RankedCrosses> = it.sortedByDescending { r -> rank(r.matchInfo.skip(4)) }

                (fragmentSearchResultsRv.adapter as SimpleListAdapter).submitList(crosses.map { ranked ->
                    ranked.cross.rowid.toString() to
                            ranked.cross.crossId +
                            "\n$female: " + ranked.cross.femaleName +
                            "\n$male: " + ranked.cross.maleName +
                            "\n ${ranked.cross.date}"
                })

                fragmentSearchResultsRv.adapter?.notifyItemRangeChanged(0, fragmentSearchResultsRv.adapter?.itemCount ?: 0)

                fragmentSearchResultsRv.scheduleLayoutAnimation()
            }
        }
    }

    //https://stackoverflow.com/questions/63654824/how-to-fix-the-error-wrong-number-of-arguments-to-function-rank-on-sqlite-an
    private fun rank(matchInfo: IntArray): Double {
        val numPhrases = matchInfo[0]
        val numColumns = matchInfo[1]

        var score = 0.0
        for (phrase in 0 until numPhrases) {
            val offset = 2 + phrase * numColumns * 3
            for (column in 0 until numColumns) {
                val numHitsInRow = matchInfo[offset + 3 * column]
                val numHitsInAllRows = matchInfo[offset + 3 * column + 1]
                if (numHitsInAllRows > 0) {
                    score += numHitsInRow.toDouble() / numHitsInAllRows.toDouble()
                }
            }
        }

        return score
    }

    fun ByteArray.skip(skipSize: Int): IntArray {
        val cleanedArr = IntArray(this.size / skipSize)
        var pointer = 0
        for (i in this.indices step skipSize) {
            cleanedArr[pointer] = this[i].toInt()
            pointer++
        }

        return cleanedArr
    }

    private fun FragmentSearchBinding.startSearchViews() {

        fragmentSearchResultsRv.adapter = SimpleListAdapter(this@SearchFragment)
        fragmentSearchResultsRv.layoutManager = LinearLayoutManager(context)
        fragmentSearchEt.addTextChangedListener(object : TextWatcher {
            override fun beforeTextChanged(
                s: CharSequence?,
                start: Int,
                count: Int,
                after: Int
            ) {}

            override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) {

                val text = s?.toString() ?: ""

                startSearch(text)
            }

            override fun afterTextChanged(s: Editable?) {}

        })
    }

    override fun onDestroy() {
        super.onDestroy()
        mFtsDatabase?.close()
    }

    override fun FragmentSearchBinding.afterCreateView() {

        val isCommutativeCrossing = mPref.getBoolean(mKeyUtil.commutativeCrossingKey, false)

        //initialize databases and populate fts tables with all cross data
        context?.let { ctx ->

            CrossesDatabase.getInMemoryInstance(ctx).let { fts ->

                mFtsDatabase = fts
                mViewModel = CrossesViewModel(fts.crossesDao(), fts.crossesFtsDao())

                eventsViewModel.events.observeOnce(viewLifecycleOwner) { events ->

                    eventsViewModel.parents.observe(viewLifecycleOwner) { parents ->

                        mScope.launch {

                            mViewModel?.insert(isCommutativeCrossing, events, parents)

                            //swap progress bar and search ui visibility
                            activity?.runOnUiThread {
                                arrayOf(fragmentSearchEt, fragmentSearchResultsRv).forEach {
                                    it.visibility = View.VISIBLE
                                }
                                fragmentSearchProgressBar.visibility = View.INVISIBLE
                            }
                        }
                    }
                }
            }
        }

        startSearchViews()
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        (activity as? MainActivity)?.setBackButtonToolbar()
        (activity as? MainActivity)?.supportActionBar?.apply {
            title = getString(R.string.search_fragment_title)
            show()
        }
    }

    override fun onItemClicked(pair: Pair<String, String>) {

        findNavController().navigate(SearchFragmentDirections
            .actionFromSearchToEventDetail(pair.first.toLong()))
    }
}
