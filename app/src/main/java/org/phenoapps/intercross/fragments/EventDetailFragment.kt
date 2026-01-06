package org.phenoapps.intercross.fragments

import android.content.Context
import android.content.SharedPreferences
import android.content.pm.PackageManager
import android.os.Build
import android.os.Bundle
import android.view.Menu
import android.view.MenuInflater
import android.view.MenuItem
import android.view.View
import android.view.WindowManager
import android.view.inputmethod.InputMethodManager
import android.widget.Toast
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import androidx.fragment.app.viewModels
import androidx.navigation.fragment.findNavController
import dagger.hilt.android.AndroidEntryPoint
import org.phenoapps.intercross.R
import org.phenoapps.intercross.activities.MainActivity
import org.phenoapps.intercross.adapters.MetadataAdapter
import org.phenoapps.intercross.adapters.models.MetadataModel
import org.phenoapps.intercross.data.EventsRepository
import org.phenoapps.intercross.data.MetaValuesRepository
import org.phenoapps.intercross.data.MetadataRepository
import org.phenoapps.intercross.data.WishlistRepository
import org.phenoapps.intercross.data.dao.EventsDao
import org.phenoapps.intercross.data.models.Event
import org.phenoapps.intercross.data.models.Meta
import org.phenoapps.intercross.data.models.MetadataValues
import org.phenoapps.intercross.data.models.WishlistView
import org.phenoapps.intercross.data.viewmodels.EventDetailViewModel
import org.phenoapps.intercross.data.viewmodels.EventListViewModel
import org.phenoapps.intercross.data.viewmodels.MetaValuesViewModel
import org.phenoapps.intercross.data.viewmodels.MetadataViewModel
import org.phenoapps.intercross.data.viewmodels.WishlistViewModel
import org.phenoapps.intercross.data.viewmodels.factory.EventDetailViewModelFactory
import org.phenoapps.intercross.data.viewmodels.factory.EventsListViewModelFactory
import org.phenoapps.intercross.data.viewmodels.factory.MetaValuesViewModelFactory
import org.phenoapps.intercross.data.viewmodels.factory.MetadataViewModelFactory
import org.phenoapps.intercross.data.viewmodels.factory.WishlistViewModelFactory
import org.phenoapps.intercross.databinding.FragmentEventDetailBinding
import org.phenoapps.intercross.interfaces.MetadataManager
import org.phenoapps.intercross.util.BluetoothUtil
import org.phenoapps.intercross.util.Dialogs
import org.phenoapps.intercross.util.FileUtil
import org.phenoapps.intercross.util.KeyUtil
import org.phenoapps.intercross.util.observeOnce
import javax.inject.Inject

@AndroidEntryPoint
class EventDetailFragment:
    IntercrossBaseFragment<FragmentEventDetailBinding>(R.layout.fragment_event_detail),
    MetadataManager {

    private val requestBluetoothPermissions = registerForActivityResult(ActivityResultContracts.RequestMultiplePermissions()) { granted ->

        granted?.let { grant ->

            if (grant.filter { it.value == false }.isNotEmpty()) {

                Toast.makeText(context, R.string.error_no_bluetooth_permission, Toast.LENGTH_SHORT).show()
            }
        }
    }

    private lateinit var mEvent: Event
    private lateinit var mMetaValuesList: List<MetadataValues>
    private lateinit var mMetaList: List<Meta>
    private lateinit var mWishlist: List<WishlistView>

    private val eventsList: EventListViewModel by viewModels {
        EventsListViewModelFactory(EventsRepository.getInstance(db.eventsDao()))
    }

    private val metaValuesViewModel: MetaValuesViewModel by viewModels {
        MetaValuesViewModelFactory(MetaValuesRepository.getInstance(db.metaValuesDao()))
    }

    private val metadataViewModel: MetadataViewModel by viewModels {
        MetadataViewModelFactory(MetadataRepository.getInstance(db.metadataDao()))
    }

    private val wishList: WishlistViewModel by viewModels {
        WishlistViewModelFactory(WishlistRepository.getInstance(db.wishlistDao()))
    }

    @Inject
    lateinit var mPref: SharedPreferences

    @Inject
    lateinit var mKeyUtil: KeyUtil

    private lateinit var eventDetailViewModel: EventDetailViewModel

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        setHasOptionsMenu(true)

        activity?.window?.setSoftInputMode(WindowManager.LayoutParams.SOFT_INPUT_ADJUST_RESIZE)
    }

    private fun getMetaDataVisibility(context: Context): Int {
        val collect: Boolean = mPref.getBoolean(mKeyUtil.collectAdditionalInfoKey, false)

        return if (collect) View.VISIBLE else View.GONE

    }

    override fun FragmentEventDetailBinding.afterCreateView() {

        (activity as? MainActivity)?.applyBottomInsets(root)

        (activity as MainActivity).setBackButtonToolbar()
        (activity as AppCompatActivity).supportActionBar?.show()

        arguments?.getLong("eid")?.let { rowid ->

            if (rowid == -1L){

                FileUtil(requireContext()).ringNotification(false)

                findNavController().popBackStack()
            }

            val viewModel: EventDetailViewModel by viewModels {
                EventDetailViewModelFactory(EventsRepository.getInstance(db.eventsDao()), rowid)
            }

            eventDetailViewModel = viewModel

            metaDataVisibility = getMetaDataVisibility(requireContext())

            eventDetailMetadataRecyclerView.adapter = MetadataAdapter(this@EventDetailFragment.context
                ?.getSystemService(Context.INPUT_METHOD_SERVICE) as? InputMethodManager,
                this@EventDetailFragment)

            refreshObservers()

            printButton.setOnClickListener {
                startPrintProcess()
            }
        }
    }

    // loads the metadata into the ui
    // this doesn't listen forever to avoid circular recursive updates
    private fun refreshMetadata() {

        val eid = mEvent.id?.toInt() ?: -1
        metadataViewModel.metadata.observeOnce(viewLifecycleOwner) { metadata ->

            eventDetailViewModel.metadata.observeOnce(viewLifecycleOwner) { values ->

                // merge the metadata properties with either the default values or saved values
                val actualMeta = arrayListOf<EventsDao.CrossMetadata>()
                for (data in metadata) {
                    if (values.any { it.eid == eid && data.property == it.property }) {
                        values.find { data.property == it.property }?.let {
                            actualMeta.add(EventsDao.CrossMetadata(
                                eid, data.property, it.value
                            ))
                        }
                    } else actualMeta.add(
                        EventsDao.CrossMetadata(eid, data.property, data.defaultValue))
                }

                (mBinding.eventDetailMetadataRecyclerView.adapter as MetadataAdapter)
                    .submitList(actualMeta
                        .map { MetadataModel(it.property, it.value.toString()) }
                        .sortedBy { it.property })

                mBinding.eventDetailMetadataRecyclerView.adapter?.notifyDataSetChanged()
            }
        }
    }

    private fun FragmentEventDetailBinding.refreshObservers() {

        wishList.wishes.observe(viewLifecycleOwner) { crossblock ->
            mWishlist = crossblock
        }

        if (::eventDetailViewModel.isInitialized) {

            eventDetailViewModel.event.observe(viewLifecycleOwner) {

                it?.let {

                    mEvent = it

                    event = it

                    refreshMetadata()

                    eventDetailLayout.event = it

                    eventDetailLayout.timestamp = if ("_" in it.timestamp) {

                        it.timestamp.split("_")[0]

                    } else it.timestamp
                }
            }

            eventDetailViewModel.parents.observe(viewLifecycleOwner) { data ->

                data?.let { parents ->

                    eventDetailLayout.female = parents.momReadableName

                    eventDetailLayout.male = parents.dadReadableName

                    momName = parents.momReadableName

                    dadName = parents.dadReadableName

                    momCode = parents.momCode

                    dadCode = parents.dadCode

                    eventsList.events.observe(viewLifecycleOwner) {

                        it?.let { events ->

                            events.find { e -> e.eventDbId == parents.momCode }.let { mom ->

                                femaleName.setOnClickListener {

                                    if (mom?.id == null) {

                                        Dialogs.notify(AlertDialog.Builder(requireContext()),
                                            getString(R.string.parent_event_does_not_exist))

                                    } else {
                                        findNavController()
                                            .navigate(EventDetailFragmentDirections
                                                .actionToParentEvent(mom.id ?: -1L))
                                    }
                                }

                            }

                            events.find { e -> e.eventDbId == parents.dadCode }.let { dad ->

                                maleName.setOnClickListener {

                                    if (dad?.id == null) {

                                        Dialogs.notify(AlertDialog.Builder(requireContext()),
                                            getString(R.string.parent_event_does_not_exist))

                                    } else {
                                        findNavController().navigate(EventDetailFragmentDirections
                                            .actionToParentEvent(dad.id ?: -1L))
                                    }
                                }
                            }
                        }
                    }
                }
            }
        }

        metaValuesViewModel.metaValues.observe(viewLifecycleOwner) {
            mMetaValuesList = it
        }

        metadataViewModel.metadata.observeOnce(viewLifecycleOwner) {
            it?.let {
                mMetaList = it
            }
        }
    }

    override fun onCreateOptionsMenu(menu: Menu, inflater: MenuInflater) {

        inflater.inflate(R.menu.cross_entry_toolbar, menu)

        menu.findItem(R.id.action_metadata_collect)?.let { menuItem ->
            // set icon based on preference
            val metadataCollectEnabled = mPref.getBoolean(mKeyUtil.collectAdditionalInfoKey, false)
            menuItem.setIcon(
                if (metadataCollectEnabled) R.drawable.ic_metadata_collect
                else R.drawable.ic_metadata_collect_disabled
            )
        }


        super.onCreateOptionsMenu(menu, inflater)
    }

    override fun onOptionsItemSelected(item: MenuItem): Boolean {

        if (::mEvent.isInitialized) {

            when (item.itemId) {
                R.id.action_metadata_collect -> {
                    val currentValue = mPref.getBoolean(mKeyUtil.collectAdditionalInfoKey, false)
                    val metadataCollectEnabled = !currentValue
                    mPref.edit().putBoolean(mKeyUtil.collectAdditionalInfoKey, metadataCollectEnabled).apply()

                    // update menu icon
                    item.setIcon(
                        if (metadataCollectEnabled) R.drawable.ic_metadata_collect
                        else R.drawable.ic_metadata_collect_disabled
                    )

                    if (!metadataCollectEnabled) hideKeyboard()


                    val toastMsg = getString(if (metadataCollectEnabled) R.string.collect_metadata_toast_enabled else R.string.collect_metadata_toast_disabled)
                    Toast.makeText(context?.applicationContext, toastMsg, Toast.LENGTH_SHORT).show()

                    // update visibility of metadata section
                    mBinding.metaDataVisibility = getMetaDataVisibility(requireContext())
                }
                R.id.action_delete -> {

                    Dialogs.onOk(AlertDialog.Builder(requireContext()),
                            getString(R.string.delete_cross_entry_title),
                            getString(R.string.cancel),
                            getString(android.R.string.ok)) {

                        eventsList.deleteById(mEvent.id ?: -1L)

                        findNavController().popBackStack()

                    }
                }
            }
        }

        return super.onOptionsItemSelected(item)
    }

    private fun hideKeyboard() {
        val inputMethodManager = context?.getSystemService(Context.INPUT_METHOD_SERVICE) as? InputMethodManager
        activity?.currentFocus?.let { view ->
            inputMethodManager?.hideSoftInputFromWindow(view.windowToken, 0)
        }
    }

    // updates a single row value for the current event
    // or inserts a new metadata value row if this value has not been saved previously
    override fun onMetadataUpdated(property: String, value: Int?) {

        val eid = mEvent.id?.toInt() ?: -1

        mMetaList.find { it.property == property }?.id?.toInt()?.let { metaId ->

            val values = mMetaValuesList.filter { it.eid == eid && it.metaId == metaId }
            if (mMetaValuesList.isNotEmpty() && values.isNotEmpty()) { // update the old value

                metaValuesViewModel.update(MetadataValues(
                    eid, metaId, value, values.first().id))

            } else { // insert a new row

                metaValuesViewModel.insert(
                    MetadataValues(
                    eid,
                    metaId,
                    value
                ))
            }
        }

        value?.let { v ->

            checkWishlist(property, v)

        }
    }

    private fun checkWishlist(property: String, value: Int) {

        context?.let { ctx ->
            val mom = mEvent.femaleObsUnitDbId
            val dad = mEvent.maleObsUnitDbId
            val relevantWishes = mWishlist.filter { wish -> wish.momId == mom && wish.dadId == dad }
            val propertyWishes = relevantWishes.filter { wish -> wish.wishType == property }

            propertyWishes.forEach { wish ->
                if (value >= wish.wishMax && wish.wishMax > 0) {
                    val messageRes = if (wish.wishMin == wish.wishMax) {
                        R.string.minimum_maximum_wish_met
                    } else {
                        R.string.maximum_wish_met
                    }
                    Dialogs.notify(AlertDialog.Builder(ctx), getString(messageRes, property))
                } else if (value >= wish.wishMin && wish.wishMin > 0) {
                    Dialogs.notify(AlertDialog.Builder(ctx), getString(R.string.minimum_wish_met, property))
                }
            }

        }
    }

    private fun startPrintProcess() {
        context?.let { ctx ->

            var permit = true
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
                if (ctx.checkSelfPermission(android.Manifest.permission.BLUETOOTH_SCAN) == PackageManager.PERMISSION_GRANTED
                    && ctx.checkSelfPermission(android.Manifest.permission.BLUETOOTH_CONNECT) == PackageManager.PERMISSION_GRANTED) {
                    permit = true
                } else {
                    requestBluetoothPermissions.launch(arrayOf(
                        android.Manifest.permission.BLUETOOTH_SCAN,
                        android.Manifest.permission.BLUETOOTH_CONNECT
                    ))
                }
            } else if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
                if (ctx.checkSelfPermission(android.Manifest.permission.BLUETOOTH) == PackageManager.PERMISSION_GRANTED
                    && ctx.checkSelfPermission(android.Manifest.permission.BLUETOOTH_ADMIN) == PackageManager.PERMISSION_GRANTED) {
                    permit = true
                } else {
                    requestBluetoothPermissions.launch(arrayOf(
                        android.Manifest.permission.BLUETOOTH,
                        android.Manifest.permission.BLUETOOTH_ADMIN
                    ))
                }
            }

            if (permit) {

                BluetoothUtil().print(requireContext(), arrayOf(mEvent))

            }
        }
    }
}