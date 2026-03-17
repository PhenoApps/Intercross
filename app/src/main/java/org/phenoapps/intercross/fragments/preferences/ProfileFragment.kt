package org.phenoapps.intercross.fragments.preferences

import android.app.AlertDialog
import android.os.Bundle
import android.view.ViewGroup
import android.widget.ArrayAdapter
import android.widget.AutoCompleteTextView
import android.widget.LinearLayout
import android.widget.ListView
import android.widget.Toast
import androidx.core.content.edit
import androidx.preference.Preference
import org.phenoapps.intercross.R
import java.util.Locale

class ProfileFragment : BasePreferenceFragment(R.xml.profile_preferences) {

    private var profilePerson: Preference? = null
    private var profileAddPerson: Preference? = null
    private var profileManagePersons: Preference? = null
    private var profileReset: Preference? = null

    override fun onResume() {
        super.onResume()
        setToolbar(getString(R.string.prefs_profile_title))
    }

    override fun onCreatePreferences(savedInstanceState: Bundle?, rootKey: String?) {
        super.onCreatePreferences(savedInstanceState, rootKey)

        mPrefs.edit { putLong(mKeyUtil.lastTimeAskedKey, System.nanoTime()) }

        profilePerson = findPreference(mKeyUtil.profilePersonKey)
        profileAddPerson = findPreference(mKeyUtil.profileAddPersonKey)
        profileManagePersons = findPreference(mKeyUtil.profileManagePersonsKey)
        profileReset = findPreference(mKeyUtil.profileResetKey)

        migrateLegacyPersonIfNeeded()
        updatePersonSummary()
        setPreferenceClickListeners()

        val updatePerson = arguments?.getBoolean(mKeyUtil.personUpdateKey, false) == true
        if (updatePerson) {
            showPersonSelectionDialog()
        }
    }

    private fun setPreferenceClickListeners() {
        profilePerson?.setOnPreferenceClickListener {
            showPersonSelectionDialog()
            true
        }

        profileAddPerson?.setOnPreferenceClickListener {
            showAddPersonDialog()
            true
        }

        profileManagePersons?.setOnPreferenceClickListener {
            showManagePersonsDialog()
            true
        }

        profileReset?.setOnPreferenceClickListener {
            showClearSettingsDialog()
            true
        }
    }

    private fun showPersonSelectionDialog() {
        val persons = loadPersons()

        if (persons.isEmpty()) {
            showAddPersonDialog()
            return
        }

        val current = selectedPerson()
        var selectedIndex = persons.indexOfFirst { it.equals(current, ignoreCase = true) }

        val listView = ListView(requireContext()).apply {
            choiceMode = ListView.CHOICE_MODE_SINGLE
            layoutParams = LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT,
                ViewGroup.LayoutParams.WRAP_CONTENT
            )
        }

        val listAdapter = ArrayAdapter(requireContext(), android.R.layout.simple_list_item_single_choice, persons.toMutableList())
        listView.adapter = listAdapter

        if (selectedIndex >= 0) {
            listView.setItemChecked(selectedIndex, true)
        }

        listView.setOnItemClickListener { _, _, position, _ ->
            selectedIndex = position
        }

        val dialog = AlertDialog.Builder(context)
            .setTitle(R.string.profile_person_select_title)
            .setView(listView)
            .setNegativeButton(getString(R.string.dialog_cancel)) { d, _ -> d.dismiss() }
            .setNeutralButton(getString(R.string.add)) { _, _ ->
                showAddPersonDialog()
            }
            .setPositiveButton(getString(R.string.dialog_save)) { _, _ ->
                if (selectedIndex in persons.indices) {
                    setSelectedPerson(persons[selectedIndex])
                }
                updatePersonSummary()
            }
            .create()

        dialog.show()

        val params = dialog.window?.attributes
        params?.width = LinearLayout.LayoutParams.MATCH_PARENT
        dialog.window?.attributes = params
    }

    private fun showManagePersonsDialog() {
        val persons = loadPersons().toMutableList()

        val listView = ListView(requireContext()).apply {
            choiceMode = ListView.CHOICE_MODE_SINGLE
            layoutParams = LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT,
                ViewGroup.LayoutParams.WRAP_CONTENT
            )
        }

        val listAdapter = ArrayAdapter(requireContext(), android.R.layout.simple_list_item_single_choice, persons.toMutableList())
        listView.adapter = listAdapter

        var selectedIndex = persons.indexOfFirst { it.equals(selectedPerson(), ignoreCase = true) }
        if (selectedIndex >= 0) {
            listView.setItemChecked(selectedIndex, true)
        }

        listView.setOnItemClickListener { _, _, position, _ ->
            selectedIndex = position
        }

        val dialog = AlertDialog.Builder(context)
            .setTitle(R.string.profile_person_manage_title)
            .setView(listView)
            .setNegativeButton(getString(R.string.dialog_cancel)) { d, _ -> d.dismiss() }
            .setNeutralButton(getString(R.string.profile_person_remove), null)
            .create()

        fun refreshAdapter() {
            val snapshot = persons.toList()
            listAdapter.clear()
            listAdapter.addAll(snapshot)
            listAdapter.notifyDataSetChanged()
        }

        dialog.setOnShowListener {
            dialog.getButton(AlertDialog.BUTTON_NEUTRAL).setOnClickListener {
                if (selectedIndex !in persons.indices) {
                    Toast.makeText(requireContext(), getString(R.string.profile_person_not_found), Toast.LENGTH_SHORT).show()
                    return@setOnClickListener
                }

                val removedPerson = persons.removeAt(selectedIndex)
                savePersons(persons)

                if (selectedPerson().equals(removedPerson, ignoreCase = true)) {
                    setSelectedPerson(persons.firstOrNull().orEmpty())
                }

                refreshAdapter()

                if (persons.isNotEmpty()) {
                    selectedIndex = selectedIndex.coerceAtMost(persons.lastIndex)
                    listView.setItemChecked(selectedIndex, true)
                } else {
                    selectedIndex = -1
                }

                updatePersonSummary()
                Toast.makeText(requireContext(), getString(R.string.profile_person_removed), Toast.LENGTH_SHORT).show()
            }
        }

        dialog.show()
    }

    private fun showAddPersonDialog() {
        val input = AutoCompleteTextView(requireContext()).apply {
            hint = getString(R.string.profile_person_input_hint)
            isSingleLine = true
        }

        val persons = loadPersons()
        val adapter = ArrayAdapter(requireContext(), android.R.layout.simple_list_item_1, persons)
        input.setAdapter(adapter)
        input.threshold = 0
        input.setOnClickListener { input.showDropDown() }

        val dialog = AlertDialog.Builder(context)
            .setTitle(R.string.profile_add_person)
            .setView(input)
            .setNegativeButton(getString(R.string.dialog_cancel)) { d, _ -> d.dismiss() }
            .setPositiveButton(getString(R.string.add)) { _, _ ->
                val entered = input.text?.toString()?.trim().orEmpty()
                if (entered.isBlank()) {
                    Toast.makeText(requireContext(), getString(R.string.profile_person_name_required), Toast.LENGTH_SHORT).show()
                    return@setPositiveButton
                }

                val currentPersons = loadPersons().toMutableList()
                if (currentPersons.none { it.equals(entered, ignoreCase = true) }) {
                    currentPersons.add(entered)
                    currentPersons.sortBy { it.lowercase(Locale.getDefault()) }
                    savePersons(currentPersons)
                }
            }
            .create()

        dialog.show()
    }

    private fun showClearSettingsDialog() {
        val builder = AlertDialog.Builder(context)
            .setTitle(getString(R.string.profile_reset))
            .setMessage(getString(R.string.dialog_confirm))
            .setNegativeButton(getString(R.string.dialog_no)) { dialog, _ -> dialog.dismiss() }
            .setPositiveButton(getString(R.string.dialog_yes)) { dialog, _ ->
                dialog.dismiss()
                mPrefs.edit {
                    putStringSet(mKeyUtil.profilePersonListKey, emptySet())
                    putString(mKeyUtil.profileSelectedPersonKey, "")
                    putString(mKeyUtil.personFirstNameKey, "")
                    putString(mKeyUtil.personLastNameKey, "")
                    putBoolean(mKeyUtil.profileShowPersonInputKey, false)
                }
                updatePersonSummary()
            }

        builder.create().show()
    }

    private fun updatePersonSummary() {
        profilePerson?.summary = personSummary()
    }

    private fun personSummary(): String {
        val selected = selectedPerson()
        if (selected.isNotBlank()) {
            return selected
        }

        val first = mPrefs.getString(mKeyUtil.personFirstNameKey, "").orEmpty()
        val last = mPrefs.getString(mKeyUtil.personLastNameKey, "").orEmpty()
        return "$first $last".trim()
    }

    private fun selectedPerson(): String =
        mPrefs.getString(mKeyUtil.profileSelectedPersonKey, "").orEmpty().trim()

    private fun loadPersons(): List<String> {
        return mPrefs.getStringSet(mKeyUtil.profilePersonListKey, emptySet())
            .orEmpty()
            .map { it.trim() }
            .filter { it.isNotBlank() }
            .distinctBy { it.lowercase(Locale.getDefault()) }
            .sortedBy { it.lowercase(Locale.getDefault()) }
    }

    private fun savePersons(persons: List<String>) {
        mPrefs.edit {
            putStringSet(mKeyUtil.profilePersonListKey, persons.toSet())
        }
    }

    private fun setSelectedPerson(person: String) {
        val cleaned = person.trim()
        val first: String
        val last: String

        if (cleaned.isBlank()) {
            first = ""
            last = ""
        } else {
            val tokens = cleaned.split("\\s+".toRegex(), limit = 2)
            first = tokens.firstOrNull().orEmpty()
            last = if (tokens.size > 1) tokens[1] else ""
        }

        mPrefs.edit {
            putString(mKeyUtil.profileSelectedPersonKey, cleaned)
            putString(mKeyUtil.personFirstNameKey, first)
            putString(mKeyUtil.personLastNameKey, last)
        }
    }

    private fun migrateLegacyPersonIfNeeded() {
        val first = mPrefs.getString(mKeyUtil.personFirstNameKey, "").orEmpty().trim()
        val last = mPrefs.getString(mKeyUtil.personLastNameKey, "").orEmpty().trim()
        val legacyPerson = "$first $last".trim()

        if (legacyPerson.isBlank()) {
            return
        }

        val currentPersons = loadPersons().toMutableList()
        if (currentPersons.none { it.equals(legacyPerson, ignoreCase = true) }) {
            currentPersons.add(legacyPerson)
            savePersons(currentPersons)
        }

        if (selectedPerson().isBlank()) {
            setSelectedPerson(legacyPerson)
        }
    }
}