package org.phenoapps.intercross.fragments

import android.content.Context
import android.os.Bundle
import android.os.Handler
import android.text.Editable
import android.text.TextWatcher
import android.util.Log
import android.view.Menu
import android.view.MenuInflater
import android.view.MenuItem
import android.view.View
import android.view.WindowManager
import android.view.inputmethod.EditorInfo
import android.widget.TextView
import androidx.appcompat.app.AlertDialog
import androidx.core.content.ContextCompat
import androidx.core.widget.addTextChangedListener
import androidx.fragment.app.activityViewModels
import androidx.fragment.app.viewModels
import androidx.lifecycle.Observer
import androidx.navigation.fragment.findNavController
import androidx.preference.PreferenceManager
import androidx.recyclerview.widget.ItemTouchHelper
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import kotlinx.android.synthetic.main.fragment_events.*
import kotlinx.android.synthetic.main.fragment_events.recyclerView
import kotlinx.android.synthetic.main.fragment_events.view.*
import org.phenoapps.intercross.BuildConfig
import org.phenoapps.intercross.R
import org.phenoapps.intercross.adapters.EventsAdapter
import org.phenoapps.intercross.data.EventsRepository
import org.phenoapps.intercross.data.ParentsRepository
import org.phenoapps.intercross.data.SettingsRepository
import org.phenoapps.intercross.data.WishlistRepository
import org.phenoapps.intercross.data.models.Event
import org.phenoapps.intercross.data.models.Parent
import org.phenoapps.intercross.data.models.Settings
import org.phenoapps.intercross.data.models.WishlistView
import org.phenoapps.intercross.data.viewmodels.CrossSharedViewModel
import org.phenoapps.intercross.data.viewmodels.EventListViewModel
import org.phenoapps.intercross.data.viewmodels.ParentsListViewModel
import org.phenoapps.intercross.data.viewmodels.SettingsViewModel
import org.phenoapps.intercross.data.viewmodels.WishlistViewModel
import org.phenoapps.intercross.data.viewmodels.factory.EventsListViewModelFactory
import org.phenoapps.intercross.data.viewmodels.factory.ParentsListViewModelFactory
import org.phenoapps.intercross.data.viewmodels.factory.SettingsViewModelFactory
import org.phenoapps.intercross.data.viewmodels.factory.WishlistViewModelFactory
import org.phenoapps.intercross.databinding.FragmentEventsBinding
import org.phenoapps.intercross.util.CrossUtil
import org.phenoapps.intercross.util.Dialogs
import org.phenoapps.intercross.util.FileUtil
import org.phenoapps.intercross.util.SnackbarQueue
import java.util.*

class EventsFragment : IntercrossBaseFragment<FragmentEventsBinding>(R.layout.fragment_events) {

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

    private var mParents: List<Parent> = ArrayList()

    private var mSettings: Settings = Settings()

    private var mEvents: List<Event> = ArrayList()

    private var mEventsEmpty = true

    private val mSharedViewModel: CrossSharedViewModel by activityViewModels()

    private var mWishlistProgress: List<WishlistView> = ArrayList()

    private var mFocused: View? = null

    private fun getFirstOrder(context: Context): String {

        val maleFirst = PreferenceManager.getDefaultSharedPreferences(context)
                .getBoolean(SettingsFragment.ORDER, false)

        return if (maleFirst) context.getString(R.string.MaleID) else context.getString(R.string.FemaleID)
    }

    private fun getSecondOrder(context: Context): String {

        val maleFirst = PreferenceManager.getDefaultSharedPreferences(context)
                .getBoolean(SettingsFragment.ORDER, false)

        return if (maleFirst) context.getString(R.string.FemaleID) else context.getString(R.string.MaleID)

    }

    override fun FragmentEventsBinding.afterCreateView() {

        activity?.window?.setSoftInputMode(WindowManager.LayoutParams.SOFT_INPUT_ADJUST_RESIZE)

        val pref = PreferenceManager.getDefaultSharedPreferences(requireContext())

        if ("demo" in BuildConfig.BUILD_TYPE) {

            pref.edit().putString("org.phenoapps.intercross.PERSON", "Developer").apply()

        }

        if (pref.getBoolean("first_load", true)) {

            settingsModel.insert(mSettings
                    .apply {
                        isUUID = true
                    })

            pref.edit().putBoolean("first_load", false).apply()
        }

        recyclerView.adapter = EventsAdapter(this@EventsFragment, viewModel)

        recyclerView.layoutManager = LinearLayoutManager(context)

        firstHint = getFirstOrder(requireContext())

        secondHint = getSecondOrder(requireContext())

        startObservers()

        activity?.window?.setSoftInputMode(WindowManager.LayoutParams.SOFT_INPUT_ADJUST_PAN)

        setupUI()

    }

    private fun startObservers() {

        val pref = PreferenceManager.getDefaultSharedPreferences(requireContext())

        val error = getString(R.string.ErrorCodeExists)

        parentsList.parents.observe(viewLifecycleOwner, {

            it?.let {

                mParents = it

            }
        })

        viewModel.events.observe(viewLifecycleOwner) {

            it?.let {

                mEvents = it

                mEventsEmpty = it.isEmpty()

                editTextCross.addTextChangedListener {

                    val value = editTextCross.text.toString()

                    if (value.isNotBlank()) {

                        val codes =
                            mEvents.map { event -> event.eventDbId } + mParents.map { parent -> parent.codeId }
                                .distinct()

                        if (editTextCross.text.toString() in codes) {

                            if (crossTextHolder.error == null) crossTextHolder.error = error

                        } else crossTextHolder.error = null

                    } else {

                        crossTextHolder.error = null

                    }
                }

                (recyclerView.adapter as? EventsAdapter)?.submitList(it)
            }
        }

        settingsModel.settings.observe(viewLifecycleOwner) {

            it?.let {

                mSettings = it

                mBinding.settings = it

            }
        }

        wishStore.wishes.observe(viewLifecycleOwner) {

            it?.let {

                mWishlistProgress = it.filter { wish -> wish.wishType == "cross" }
            }

        }

        mSharedViewModel.lastScan.observe(viewLifecycleOwner) {

            it?.let {

                if (it.isNotBlank()) {

                    //Log.d("IntercrossNextScan", mFocused?.id.toString())

                    when (mFocused?.id) {

                        firstText.id -> {

                            afterFirstText(it)

                        }
                        secondText.id -> {

                            afterSecondText(it)

                        }
                        editTextCross.id -> {

                            afterThirdText(it)

                        }
                        else -> {

                            /**
                             * if nothing is focused check each text field and apply current settings
                             */
                            val first = firstText.text.toString()
                            val second = secondText.text.toString()
                            //val third = editTextCross.text.toString()

                            val order = pref.getBoolean(SettingsFragment.ORDER, false)
                            val blank = pref.getBoolean(SettingsFragment.BLANK, false)

                            //first check first text, if male first and allow blank males then skip to second text
                            if (first.isBlank() && !(order && blank)) {

                                afterFirstText(it)

                            } else if (second.isBlank() && !(!order && blank)) {

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

        val pref = PreferenceManager.getDefaultSharedPreferences(requireContext())

        val order = pref.getBoolean(SettingsFragment.ORDER, false)
        val blank = pref.getBoolean(SettingsFragment.BLANK, false)

        firstText.setText(value)

        if (!order && blank) {

            askUserNewExperimentName()

        } else secondText.requestFocus()
    }

    private fun afterSecondText(value: String) {

//        val pref = PreferenceManager.getDefaultSharedPreferences(requireContext())

//        val order = pref.getBoolean(SettingsFragment.ORDER, false)
//        val blank = pref.getBoolean(SettingsFragment.BLANK, false)

        secondText.setText(value)

        if ((mSettings.isPattern || mSettings.isUUID)) {

            askUserNewExperimentName()

        } else editTextCross.requestFocus()
    }

    private fun afterThirdText(value: String) {

//        val pref = PreferenceManager.getDefaultSharedPreferences(requireContext())

//        val order = pref.getBoolean(SettingsFragment.ORDER, false)
//        val blank = pref.getBoolean(SettingsFragment.BLANK, false)

        editTextCross.setText(value)

        askUserNewExperimentName()
    }

    private fun FragmentEventsBinding.setupUI() {

        setupRecyclerView()

        setupTextInput()

        setupButtons()

        setHasOptionsMenu(true)

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

                            CrossUtil(requireContext()).submitCrossEvent(mBinding.root,
                                    event.femaleObsUnitDbId, event.maleObsUnitDbId,
                                    event.eventDbId, mSettings, settingsModel, viewModel,
                                    mParents, parentsList, mWishlistProgress
                            )

                        })
                    }
                }
            }
        }).attachToRecyclerView(recyclerView)
    }

    private fun resetDataEntry() {

        firstText.setText("")

        secondText.setText("")

        editTextCross.setText("")

        when {

            mSettings.isPattern -> {

                editTextCross.setText(mSettings.pattern)
            }

            mSettings.isUUID -> {

                editTextCross.setText(UUID.randomUUID().toString())
            }
        }

        firstText.requestFocus()
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

            if (hasFocus && (PreferenceManager.getDefaultSharedPreferences(requireContext())
                            .getString("org.phenoapps.intercross.PERSON", "") ?: "").isBlank()) {
                askUserForPerson()
            }

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

                if (value.isNotBlank()) {

                    afterFirstText(value)

                } else secondText.requestFocus()

                return@OnEditorActionListener true
            }

            false
        })

        //if auto generation is enabled save after the second text is submitted
        secondText.setOnEditorActionListener(TextView.OnEditorActionListener { _, i, _ ->

            if (i == EditorInfo.IME_ACTION_DONE) {

                val value = secondText.text.toString()

                if (value.isNotBlank()) {

                    afterSecondText(value)

                } else editTextCross.requestFocus()

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

    private fun FragmentEventsBinding.setupButtons() {

        saveButton.isEnabled = isInputValid()

        button.setOnClickListener {

            if ((PreferenceManager.getDefaultSharedPreferences(requireContext())
                            .getString("org.phenoapps.intercross.PERSON", "") ?: "").isBlank()) {
                askUserForPerson()
            } else {

                findNavController().navigate(EventsFragmentDirections.actionToBarcodeScanFragment())

            }
        }

        saveButton.setOnClickListener {

            askUserNewExperimentName()
        }

        clearButton.setOnClickListener {

            firstText.setText("")

            secondText.setText("")

            val person = PreferenceManager.getDefaultSharedPreferences(requireContext())
                    .getString("org.phenoapps.intercross.PERSON", "") ?: ""

            if (person.isNotBlank()) firstText.requestFocus()
        }
    }

    private fun FragmentEventsBinding.isInputValid(): Boolean {

        val allowBlank = PreferenceManager.getDefaultSharedPreferences(requireContext())
                .getBoolean(SettingsFragment.BLANK, false)
        val maleFirst = PreferenceManager.getDefaultSharedPreferences(requireContext())
                .getBoolean(SettingsFragment.ORDER, false)
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
        if (allowBlank) numFilled++
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

        return ((male.isNotBlank() || allowBlank) && female.isNotBlank()
                && (cross.isNotBlank() || (mSettings.isUUID || mSettings.isPattern)))
    }

    private fun askUserNewExperimentName() {

        val value = editTextCross.text.toString()

        val allowBlank = PreferenceManager.getDefaultSharedPreferences(requireContext())
                .getBoolean(SettingsFragment.BLANK, false)
        val maleFirst = PreferenceManager.getDefaultSharedPreferences(requireContext())
                .getBoolean(SettingsFragment.ORDER, false)

        lateinit var male: String
        lateinit var female: String

        if (!maleFirst) {
            female = (firstText.text ?: "").toString()
            male = (secondText.text ?: "").toString()
        } else {
            male = (firstText.text ?: "").toString()
            female = (secondText.text ?: "").toString()
        }

        if (value.isNotBlank() && (male.isNotBlank() || allowBlank) && female.isNotBlank()) {

            if (male.isBlank()) male = "blank"

            val crossIds = mEvents.map { event -> event.eventDbId }

            if (!crossIds.any { id -> id == value }) {

                val parent = mParents.find { parent -> parent.codeId == value }

                if (parent != null) {

                    Dialogs.notify(AlertDialog.Builder(requireContext()), getString(R.string.cross_id_already_exists_as_parent))

                } else {

                    CrossUtil(requireContext()).submitCrossEvent(mBinding.root, female, male, value, mSettings, settingsModel, viewModel, mParents, parentsList, mWishlistProgress)

                }

                resetDataEntry()

                Handler().postDelayed({
                    recyclerView.scrollToPosition(0)
                }, 250)

            } else Dialogs.notify(AlertDialog.Builder(requireContext()), getString(R.string.cross_id_already_exists_as_event))

        } else {

            if (female.isBlank()) {

                mSnackbar.push(SnackbarQueue.SnackJob(mBinding.root, getString(R.string.you_must_enter_female_name)))

            } else if (value.isBlank()) {

                mSnackbar.push(SnackbarQueue.SnackJob(mBinding.root, getString(R.string.you_must_enter_cross_name)))

            } else {

                mSnackbar.push(SnackbarQueue.SnackJob(mBinding.root, getString(R.string.you_must_enter_male_name)))

            }

            FileUtil(requireContext()).ringNotification(false)

        }
    }

    private fun askUserForPerson() {

        mBinding.constraintLayoutParent.requestFocus()

        val person = getString(R.string.cancel)
        val personMustBeSet = getString(R.string.person_must_be_set)
        val setPerson = getString(R.string.set_person)

        val builder = AlertDialog.Builder(requireContext()).apply {

            setNegativeButton(person) { _, _ ->
                mSnackbar.push(SnackbarQueue.SnackJob(mBinding.root, personMustBeSet))
            }

            setPositiveButton(setPerson) { _, _ ->
                findNavController().navigate(R.id.settings_fragment, Bundle().apply {
                    putString("org.phenoapps.intercross.ASK_PERSON", "true")
                })
            }
        }

        builder.setTitle(personMustBeSet)
        builder.show()
    }

    override fun onCreateOptionsMenu(menu: Menu, inflater: MenuInflater) {

        inflater.inflate(R.menu.activity_main_toolbar, menu)

        super.onCreateOptionsMenu(menu, inflater)
    }

    override fun onOptionsItemSelected(item: MenuItem): Boolean {

        when(item.itemId) {

            R.id.action_search_barcode -> {

                findNavController().navigate(EventsFragmentDirections.actionToBarcodeScanFragment(1))

            }
            R.id.action_continuous_barcode -> {

                if ((PreferenceManager.getDefaultSharedPreferences(requireContext())
                                .getString("org.phenoapps.intercross.PERSON", "") ?: "").isBlank()) {
                    askUserForPerson()
                } else {

                    findNavController().navigate(EventsFragmentDirections.actionToBarcodeScanFragment(2))

                }

            }
        }
        return super.onOptionsItemSelected(item)
    }
}