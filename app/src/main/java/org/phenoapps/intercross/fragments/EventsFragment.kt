package org.phenoapps.intercross.fragments

import android.app.Activity
import android.content.Context
import android.content.Intent
import android.os.Bundle
import android.os.Handler
import android.text.Editable
import android.text.TextWatcher
import android.util.TypedValue
import android.view.*
import android.view.inputmethod.EditorInfo
import android.widget.EditText
import android.widget.LinearLayout
import android.widget.TextView
import androidx.appcompat.app.AlertDialog
import androidx.core.content.ContextCompat
import androidx.core.view.MenuProvider
import androidx.core.widget.addTextChangedListener
import androidx.fragment.app.activityViewModels
import androidx.fragment.app.viewModels
import androidx.navigation.fragment.findNavController
import androidx.preference.PreferenceManager
import androidx.recyclerview.widget.ItemTouchHelper
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.*
import org.phenoapps.intercross.BuildConfig
import org.phenoapps.intercross.activities.MainActivity
import org.phenoapps.intercross.R
import org.phenoapps.intercross.adapters.EventsAdapter
import org.phenoapps.intercross.data.*
import org.phenoapps.intercross.data.models.Event
import org.phenoapps.intercross.data.models.Parent
import org.phenoapps.intercross.data.models.Meta
import org.phenoapps.intercross.data.models.MetadataValues
import org.phenoapps.intercross.data.models.PollenGroup
import org.phenoapps.intercross.data.models.Settings
import org.phenoapps.intercross.data.models.WishlistView
import org.phenoapps.intercross.data.viewmodels.*
import org.phenoapps.intercross.data.viewmodels.factory.*
import org.phenoapps.intercross.databinding.FragmentEventsBinding
import org.phenoapps.intercross.util.*
import java.util.*
import javax.inject.Inject
import kotlin.math.roundToInt

@AndroidEntryPoint
class EventsFragment : IntercrossBaseFragment<FragmentEventsBinding>(R.layout.fragment_events),
    CoroutineScope by MainScope() {

    @Inject
    lateinit var verifyPersonHelper: VerifyPersonHelper

    private val viewModel: EventListViewModel by viewModels {
        EventsListViewModelFactory(EventsRepository.getInstance(db.eventsDao()))
    }

    private val settingsModel: SettingsViewModel by viewModels {
        SettingsViewModelFactory(SettingsRepository.getInstance(db.settingsDao()))
    }

    private val parentsList: ParentsListViewModel by viewModels {
        ParentsListViewModelFactory(ParentsRepository.getInstance(db.parentsDao()))
    }

    private val wishStore: WishlistViewModel by viewModels {
        WishlistViewModelFactory(WishlistRepository.getInstance(db.wishlistDao()))
    }

    private val metaValuesViewModel: MetaValuesViewModel by viewModels {
        MetaValuesViewModelFactory(MetaValuesRepository.getInstance(db.metaValuesDao()))
    }

    private val metadataViewModel: MetadataViewModel by viewModels {
        MetadataViewModelFactory(MetadataRepository.getInstance(db.metadataDao()))
    }

    private val argMale: String? by lazy {
        arguments?.getString("male")
    }

    private val argFemale: String? by lazy {
        arguments?.getString("female")
    }

    private var mParents: List<Parent> = ArrayList()

    private var mSettings: Settings = Settings()

    private var mEvents: List<Event> = ArrayList()

    private var mMetadata: List<Meta> = ArrayList()

    private var mEventsEmpty = true

    private val mSharedViewModel: CrossSharedViewModel by activityViewModels()

    private var mWishlistProgress: List<WishlistView> = ArrayList()

    private var mFocused: View? = null

    private val scope = CoroutineScope(Dispatchers.IO)

    private val mPref by lazy {
        PreferenceManager.getDefaultSharedPreferences(requireContext())
    }

    private val mKeyUtil by lazy {
        KeyUtil(context)
    }

    private fun getFirstOrder(context: Context): String {

        val maleFirst = mPref.getBoolean(mKeyUtil.crossOrderKey, false)

        return if (maleFirst) context.getString(R.string.MaleID) else context.getString(R.string.FemaleID)
    }

    private fun getSecondOrder(context: Context): String {

        val maleFirst = mPref.getBoolean(mKeyUtil.crossOrderKey, false)

        return if (maleFirst) context.getString(R.string.FemaleID) else context.getString(R.string.MaleID)

    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        setMenuItems()
    }

    override fun FragmentEventsBinding.afterCreateView() {

        activity?.window?.setSoftInputMode(WindowManager.LayoutParams.SOFT_INPUT_ADJUST_RESIZE)

        if ("demo" in BuildConfig.BUILD_TYPE) {

            mPref.edit().putString("org.phenoapps.intercross.PERSON", "Developer").apply()

        }

        //if this was called from crosscount/crossblock or wishlist fragment then populate the male/female tv
        val maleFirst = mPref.getBoolean(mKeyUtil.crossOrderKey, false)

        argFemale?.let { female ->
            if (maleFirst) mBinding.secondText.setText(female)
            else mBinding.firstText.setText(female)
        }

        argMale?.let { male ->
            if (maleFirst) mBinding.firstText.setText(male)
            else mBinding.secondText.setText(male)
        }

        if (mSettings.isUUID) {

            mBinding.editTextCross.setText(UUID.randomUUID().toString())

        }

        arguments?.getString("male")?.let { male ->
            if (maleFirst) {
                mBinding.firstText.setText(male)
            } else mBinding.secondText.setText(male)
        }

        arguments?.getString("female")?.let { female ->
            if (maleFirst) {
                mBinding.secondText.setText(female)
            } else mBinding.firstText.setText(female)
        }

        recyclerView.adapter = EventsAdapter(this@EventsFragment, viewModel)

        recyclerView.layoutManager = LinearLayoutManager(context)

        firstHint = getFirstOrder(requireContext())

        secondHint = getSecondOrder(requireContext())

        startObservers()

        (activity as MainActivity).supportActionBar?.hide()

        activity?.window?.setSoftInputMode(WindowManager.LayoutParams.SOFT_INPUT_ADJUST_PAN)

        setupUI()

    }

    override fun onResume() {
        super.onResume()

        (activity as MainActivity).setToolbar()
        (activity as MainActivity).supportActionBar?.title = mPref.getString(mKeyUtil.experimentNameKey, "")

        mBinding.bottomNavBar.selectedItemId = R.id.action_nav_home

    }

    private fun setMenuItems() {
        activity?.addMenuProvider(object : MenuProvider {
            override fun onCreateMenu(menu: Menu, menuInflater: MenuInflater) {
                menuInflater.inflate(R.menu.menu_entry_fragment, menu)
            }

            override fun onMenuItemSelected(menuItem: MenuItem): Boolean {
                return when (menuItem.itemId) {
                    R.id.action_set_experiment -> {
                        showExperimentDialog()
                        true
                    }
                    R.id.action_export -> {
                        // (activity as MainActivity).showExportDialog {
                        //
                        // }
                        showCrossesExport()
                        true
                    }
                    else -> false
                }
            }
        }, viewLifecycleOwner)
    }

    private fun startObservers() {

        val error = getString(R.string.ErrorCodeExists)

        val isCommutative = mPref.getBoolean(mKeyUtil.commutativeCrossingKey, false)

        metadataViewModel.metadata.observe(viewLifecycleOwner) {
            mMetadata = it
        }

        parentsList.parents.observe(viewLifecycleOwner) {

            it?.let {

                mParents = it

            }
        }

        viewModel.events.observe(viewLifecycleOwner) {

            it?.let {

                mEvents = it

                mEventsEmpty = it.isEmpty()

                mBinding.editTextCross.addTextChangedListener {

                    val value = mBinding.editTextCross.text.toString()

                    if (value.isNotBlank()) {

                        val codes =
                            mEvents.map { event -> event.eventDbId } + mParents.map { parent -> parent.codeId }
                                .distinct()

                        if (mBinding.editTextCross.text.toString() in codes) {

                            if (mBinding.crossTextHolder.error == null) mBinding.crossTextHolder.error =
                                error

                        } else mBinding.crossTextHolder.error = null

                    } else {

                        mBinding.crossTextHolder.error = null

                    }
                }

                (mBinding.recyclerView.adapter as? EventsAdapter)?.submitList(it)
            }
        }

        settingsModel.settings.observe(viewLifecycleOwner) {

            it?.let {

                mSettings = it

                mBinding.settings = it

            }
        }

        if (isCommutative) {

            wishStore.commutativeWishes.observe(viewLifecycleOwner) {

                it?.let {

                    mWishlistProgress = it.filter { wish -> wish.wishType == "cross" }
                }

            }

        } else {

            wishStore.wishes.observe(viewLifecycleOwner) {

                it?.let {

                    mWishlistProgress = it.filter { wish -> wish.wishType == "cross" }
                }

            }
        }

        mSharedViewModel.lastScan.observe(viewLifecycleOwner) {

            it?.let {

                if (it.isNotBlank()) {

                    //Log.d("IntercrossNextScan", mFocused?.id.toString())

                    when (mFocused?.id) {

                        mBinding.firstText.id -> {

                            afterFirstText(it)

                        }

                        mBinding.secondText.id -> {

                            afterSecondText(it)

                        }

                        mBinding.editTextCross.id -> {

                            afterThirdText(it)

                        }

                        else -> {

                            /**
                             * if nothing is focused check each text field and apply current settings
                             */
                            val first = mBinding.firstText.text.toString()
                            val second = mBinding.secondText.text.toString()
                            //val third = editTextCross.text.toString()

                            val maleFirst = mPref.getBoolean(mKeyUtil.crossOrderKey, false)

                            val blank = mPref.getBoolean(mKeyUtil.blankMaleKey, false)

                            //first check first text, if male first and allow blank males then skip to second text
                            if (first.isBlank() && !(maleFirst && blank)) {

                                afterFirstText(it)

                            } else if (second.isBlank() && !(!maleFirst && blank)) {

                                afterSecondText(it)

                            } else afterThirdText(it)
                        }
                    }

                    mSharedViewModel.lastScan.value = ""
                }
            }
        }
    }

    private fun afterFirstText(value: String) {

        val maleFirst = mPref.getBoolean(mKeyUtil.crossOrderKey, false)

        val blank = mPref.getBoolean(mKeyUtil.blankMaleKey, false)

        mBinding.firstText.setText(value)

        if (!maleFirst && blank) {

            askUserNewExperimentName()

        } else mBinding.secondText.requestFocus()
    }

    private fun afterSecondText(value: String) {

        val maleFirst = mPref.getBoolean(mKeyUtil.crossOrderKey, false)

        val blank = mPref.getBoolean(mKeyUtil.blankMaleKey, false)

        mBinding.secondText.setText(value)

        //check if female first, then check if string is empty to show error message
        if (!maleFirst) {

            if (value.isEmpty() && !blank) {

                mSnackbar.push(SnackbarQueue.SnackJob(mBinding.root, getString(R.string.you_must_enter_male_name)))

                return
            }

        }

        if ((mSettings.isPattern || mSettings.isUUID)) {

            askUserNewExperimentName()

        } else mBinding.editTextCross.requestFocus()
    }

    private fun afterThirdText(value: String) {

        mBinding.editTextCross.setText(value)

        askUserNewExperimentName()
    }

    private fun FragmentEventsBinding.setupUI() {

        setupRecyclerView()

        setupTextInput()

        setupButtons()

        setupBottomNavBar()

        setHasOptionsMenu(true)

    }

    private fun FragmentEventsBinding.setupBottomNavBar() {

        bottomNavBar.setOnNavigationItemSelectedListener { item ->

            when (item.itemId) {

                R.id.action_nav_preferences -> {

                    findNavController().navigate(R.id.global_action_to_preferences_fragment)
                }
                R.id.action_nav_parents -> {

                    findNavController().navigate(EventsFragmentDirections.globalActionToParents())

                }
                R.id.action_nav_cross_count -> {

                    findNavController().navigate(EventsFragmentDirections.actionToCrossTrackerFragment())

                }
            }

            true
        }

        bottomNavBar.selectedItemId = R.id.action_nav_home

    }

    private fun FragmentEventsBinding.setupRecyclerView() {

        //setup recycler adapter
        recyclerView.adapter = EventsAdapter(this@EventsFragment, viewModel)

        val undoString = getString(R.string.undo)

        //setup on item swipe to delete
        ItemTouchHelper(object : ItemTouchHelper.SimpleCallback(0, ItemTouchHelper.RIGHT) {

            override fun onMove(recyclerView: RecyclerView, viewHolder: RecyclerView.ViewHolder, target: RecyclerView.ViewHolder): Boolean {
                return false
            }

            override fun onSwiped(viewHolder: RecyclerView.ViewHolder, direction: Int) {

                (recyclerView.adapter as EventsAdapter)
                        .currentList[viewHolder.adapterPosition].also { event ->

                    event.id?.let {

                        viewModel.deleteById(eid = it)

                        mSnackbar.push(SnackbarQueue.SnackJob(root, event.eventDbId, undoString) {

                            scope.launch {
                                CrossUtil(requireContext()).submitCrossEvent(activity,
                                    event.femaleObsUnitDbId, event.maleObsUnitDbId,
                                    event.eventDbId, mSettings, settingsModel, viewModel,
                                    mParents, parentsList, mWishlistProgress, mMetadata, metaValuesViewModel
                                )
                            }
                        })
                    }
                }
            }
        }).attachToRecyclerView(recyclerView)
    }

    private fun resetDataEntry() {

        mBinding.firstText.setText("")

        mBinding.secondText.setText("")

        mBinding.editTextCross.setText("")

        when {

            mSettings.isPattern -> {

                mBinding.editTextCross.setText(mSettings.pattern)
            }

            mSettings.isUUID -> {

                mBinding.editTextCross.setText(UUID.randomUUID().toString())
            }
        }

        mBinding.firstText.requestFocus()
    }

    private fun FragmentEventsBinding.setupTextInput() {

        //single text watcher class to check if all fields are non-empty to enable the save button
        val emptyGuard = object : TextWatcher {

            override fun beforeTextChanged(charSequence: CharSequence, i: Int, i1: Int, i2: Int) {

            }

            override fun onTextChanged(charSequence: CharSequence, i: Int, i1: Int, i2: Int) {

                saveButton.isEnabled = isInputValid()
            }

            override fun afterTextChanged(editable: Editable) {

            }
        }

        val focusListener = View.OnFocusChangeListener { v, hasFocus ->

            if (hasFocus) mFocused = v

           if (hasFocus) {
               verifyPersonHelper.checkLastOpened(null)
           }

        }

        //https://stackoverflow.com/questions/3425932/detecting-when-user-has-dismissed-the-soft-keyboard
        activity?.addKeyboardToggleListener { shown ->

            if (shown) bottomNavBar.visibility = View.GONE
            else bottomNavBar.visibility = View.VISIBLE
        }

        secondText.addTextChangedListener(emptyGuard)
        firstText.addTextChangedListener(emptyGuard)
        editTextCross.addTextChangedListener(emptyGuard)

        firstText.onFocusChangeListener = focusListener
        secondText.onFocusChangeListener = focusListener
        editTextCross.onFocusChangeListener = focusListener

        firstText.setOnEditorActionListener(TextView.OnEditorActionListener { _, i, _ ->

            if (i == EditorInfo.IME_ACTION_DONE) {

                val value = firstText.text.toString()

                afterFirstText(value)

                return@OnEditorActionListener true
            }

            false
        })

        //if auto generation is enabled save after the second text is submitted
        secondText.setOnEditorActionListener(TextView.OnEditorActionListener { _, i, _ ->

            if (i == EditorInfo.IME_ACTION_DONE) {

                val value = secondText.text.toString()

                afterSecondText(value)

                return@OnEditorActionListener true
            }

            false
        })

        editTextCross.setOnEditorActionListener(TextView.OnEditorActionListener { _, i, _ ->

            if (i == EditorInfo.IME_ACTION_DONE) {

                val value = editTextCross.text.toString()

                if (value.isNotBlank()) {

                    afterThirdText(value)

                }

                return@OnEditorActionListener true
            }

            false
        })

    }

    open class KeyboardToggleListener(
        private val root: View?,
        private val onKeyboardToggleAction: (shown: Boolean) -> Unit
    ) : ViewTreeObserver.OnGlobalLayoutListener {
        private var shown = false
        override fun onGlobalLayout() {
            root?.run {
                val heightDiff = rootView.height - height
                val thresh = 550
                val keyboardShown = heightDiff > thresh
                if (shown != keyboardShown) {
                    onKeyboardToggleAction.invoke(keyboardShown)
                    shown = keyboardShown
                }
            }
        }

        private fun View.dpToPix(dp: Float) = TypedValue.applyDimension(TypedValue.COMPLEX_UNIT_DIP, dp, resources.displayMetrics).roundToInt()

    }

    fun Activity.addKeyboardToggleListener(onKeyboardToggleAction: (shown: Boolean) -> Unit): KeyboardToggleListener? {
        val root = findViewById<View>(android.R.id.content)
        val listener = KeyboardToggleListener(root, onKeyboardToggleAction)
        return root?.viewTreeObserver?.run {
            addOnGlobalLayoutListener(listener)
            listener
        }
    }

    private fun FragmentEventsBinding.setupButtons() {

        saveButton.isEnabled = isInputValid()

        button.setOnClickListener {

            verifyPersonHelper.checkLastOpened {
                findNavController().navigate(EventsFragmentDirections.actionToBarcodeScanFragment())
            }
            // if (mPref.getString(mKeyUtil.profPersonKey, "").isNullOrBlank() ||
            //     mPref.getString(mKeyUtil.profExpKey, "").isNullOrBlank()) {
            //     askUserForPersonAndExperiment()
            // } else {
            //
            //     findNavController().navigate(EventsFragmentDirections.actionToBarcodeScanFragment())
            //
            // }
        }

        button.setOnLongClickListener {

            verifyPersonHelper.checkLastOpened {
                findNavController().navigate(EventsFragmentDirections.actionToBarcodeScanFragment(2))
            }

            // if (mPref.getString(mKeyUtil.profPersonKey, "").isNullOrBlank() ||
            //     mPref.getString(mKeyUtil.profExpKey, "").isNullOrBlank()) {
            //     askUserForPersonAndExperiment()
            // } else {
            //
            //     findNavController().navigate(EventsFragmentDirections.actionToBarcodeScanFragment(2))
            //
            // }

            true
        }

        fragmentEventsSearchButton.setOnClickListener {

            findNavController().navigate(EventsFragmentDirections.actionToBarcodeScanFragment(1))

        }

        saveButton.setOnClickListener {

            askUserNewExperimentName()
        }

        clearButton.setOnClickListener {

            firstText.setText("")

            secondText.setText("")

            val person = mPref.getString(mKeyUtil.personFirstNameKey, "") ?: ""

            if (person.isNotBlank()) firstText.requestFocus()
        }
    }

    private fun FragmentEventsBinding.isInputValid(): Boolean {

        val maleFirst = mPref.getBoolean(mKeyUtil.crossOrderKey, false)

        val blank = mPref.getBoolean(mKeyUtil.blankMaleKey, false)

        val male: String
        val female: String
        val cross: String = editTextCross.text.toString()
        if (!maleFirst) {
            female = firstText.text.toString()
            male = secondText.text.toString()
        } else {
            male = firstText.text.toString()
            female = secondText.text.toString()
        }

        //calculate how full the save button should be
        var numFilled = 0
        if (blank) numFilled++
        else if (male.isNotBlank()) numFilled++
        if (female.isNotBlank()) numFilled++
        if (cross.isNotBlank()) numFilled++

        //change save button fill percentage using corresponding xml shapes
        saveButton.background = ContextCompat.getDrawable(requireContext(),
                when (numFilled) {
                    0,1,2 -> R.drawable.button_save_empty
                    //1 -> R.drawable.button_save_third
                    //2 -> R.drawable.button_save_two_thirds
                    else -> R.drawable.button_save_full
                })

        return ((male.isNotBlank() || blank) && female.isNotBlank()
                && (cross.isNotBlank() || (mSettings.isUUID || mSettings.isPattern)))
    }

    private fun askUserNewExperimentName() {

        val value = mBinding.editTextCross.text.toString()

        val maleFirst = mPref.getBoolean(mKeyUtil.crossOrderKey, false)

        val blank = mPref.getBoolean(mKeyUtil.blankMaleKey, false)

        lateinit var male: String
        lateinit var female: String

        if (!maleFirst) {
            female = (mBinding.firstText.text ?: "").toString()
            male = (mBinding.secondText.text ?: "").toString()
        } else {
            male = (mBinding.firstText.text ?: "").toString()
            female = (mBinding.secondText.text ?: "").toString()
        }

        if (value.isNotBlank() && (male.isNotBlank() || blank) && female.isNotBlank()) {

            if (male.isBlank()) male = "blank"

            val crossIds = mEvents.map { event -> event.eventDbId }

            if (!crossIds.any { id -> id == value }) {

                val parent = mParents.find { parent -> parent.codeId == value }

                if (parent != null) {

                    Dialogs.notify(AlertDialog.Builder(requireContext()), getString(R.string.cross_id_already_exists_as_parent))

                } else {

                    scope.launch {

                        context?.let { ctx ->

                            with(CrossUtil(ctx)) {

                                val eid = submitCrossEvent(
                                        activity,
                                        female,
                                        male,
                                        value,
                                        mSettings,
                                        settingsModel,
                                        viewModel,
                                        mParents,
                                        parentsList,
                                        mWishlistProgress,
                                        mMetadata,
                                        metaValuesViewModel
                                    )

                                activity?.runOnUiThread {

                                    checkPrefToOpenCrossEvent(findNavController(),
                                                EventsFragmentDirections.actionToEventFragment(eid))
                                }
                            }
                        }
                    }
                }

                resetDataEntry()

                Handler().postDelayed({
                    mBinding.recyclerView.scrollToPosition(0)
                }, 250)

            } else Dialogs.notify(AlertDialog.Builder(requireContext()), getString(R.string.cross_id_already_exists_as_event))

        } else {

            if (female.isBlank()) {

                mSnackbar.push(SnackbarQueue.SnackJob(mBinding.root, getString(R.string.you_must_enter_female_name)))

                if (!maleFirst) {
                    mBinding.firstText.requestFocus()
                } else mBinding.secondText.requestFocus()

            } else if (value.isBlank()) {

                mSnackbar.push(SnackbarQueue.SnackJob(mBinding.root, getString(R.string.you_must_enter_cross_name)))

                mBinding.editTextCross.requestFocus()

            } else {

                mSnackbar.push(SnackbarQueue.SnackJob(mBinding.root, getString(R.string.you_must_enter_male_name)))

                //request focus on the edit text that is missing
                if (maleFirst) {
                    mBinding.firstText.requestFocus()
                } else mBinding.secondText.requestFocus()
            }

            FileUtil(requireContext()).ringNotification(false)

        }
    }

    private fun showExperimentDialog() {
        val inflater = this.layoutInflater
        val layout: View = inflater.inflate(R.layout.dialog_set_experiment, null)
        val experimentName = layout.findViewById<EditText>(R.id.experimentName)

        experimentName.setText(mPref?.getString(mKeyUtil.experimentNameKey, ""))

        experimentName.setSelectAllOnFocus(true)

        val builder = android.app.AlertDialog.Builder(context)
            .setTitle(R.string.dialog_experiment_title)
            .setCancelable(true)
            .setView(layout)
            .setNegativeButton(getString(R.string.dialog_cancel)) { dialog, _ -> dialog.dismiss() }
            .setNeutralButton(R.string.Clear) { _, _ ->
                val e = mPref?.edit()
                e?.putString(mKeyUtil.experimentNameKey, "")
                (activity as MainActivity).supportActionBar?.title = null

                e?.apply()
            }
            .setPositiveButton(getString(R.string.dialog_save)) { _, _ ->
                val e = mPref?.edit()
                e?.putString(mKeyUtil.experimentNameKey, experimentName.text.toString())
                (activity as MainActivity).supportActionBar?.title = experimentName.text

                e?.apply()
            }

        val experimentDialog = builder.create()
        experimentDialog?.show()

        val langParams = experimentDialog?.window?.attributes
        langParams?.width = LinearLayout.LayoutParams.MATCH_PARENT
        experimentDialog?.window?.attributes = langParams
    }

    private fun showCrossesExport() {
        val defaultFileNamePrefix = getString(R.string.default_crosses_export_file_name)
        val fileName = "${defaultFileNamePrefix}_${DateUtil().getTime()}"

        val inflater = (activity as MainActivity).layoutInflater
        val layout = inflater.inflate(R.layout.dialog_export, null)
        val fileNameET = layout.findViewById<EditText>(R.id.file_name)

        fileNameET.setText(fileName)

        val builder = AlertDialog.Builder(activity as MainActivity)
            .setTitle(R.string.dialog_export_title)
            .setView(layout)
            .setNegativeButton(getString(R.string.dialog_cancel)) { d, _ -> d.dismiss() }
            .setPositiveButton(getString(R.string.dialog_export)) { _, _ ->
                (activity as MainActivity).startExport(fileNameET.text.toString())
            }
        builder.create().show()
    }
}