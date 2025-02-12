package org.phenoapps.intercross.fragments.preferences

import android.content.Context
import android.content.Intent
import android.os.Bundle
import android.util.Log
import android.view.View
import androidx.appcompat.app.AlertDialog
import androidx.lifecycle.lifecycleScope
import androidx.preference.Preference
import androidx.preference.PreferenceManager
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import org.phenoapps.intercross.activities.MainActivity
import org.phenoapps.intercross.R
import org.phenoapps.intercross.activities.DefineStorageActivity
import org.phenoapps.intercross.data.IntercrossDatabase
import org.phenoapps.intercross.util.DateUtil

class DatabaseFragment : BasePreferenceFragment(R.xml.database_preferences) {

    private val mPrefs by lazy {
        PreferenceManager.getDefaultSharedPreferences(requireContext())
    }

    override fun onResume() {
        super.onResume()
        setToolbar(getString(R.string.prefs_database_title))
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        with ( findPreference<Preference>(getString(R.string.key_pref_storage_definer))) {
            this?.let {
                setOnPreferenceClickListener {
                    activity?.let { _ ->
                        startActivity(Intent(context, DefineStorageActivity::class.java))
                    }
                    true
                }
            }
        }

        with(findPreference<Preference>(getString(R.string.key_pref_db_reset))) {
            this?.let {
                setOnPreferenceClickListener {
                    showDatabaseResetDialog1()

                    true
                }
            }
        }

        with (findPreference<Preference>(getString(R.string.key_pref_db_import))) {
            this?.let {
                setOnPreferenceClickListener {
                    activity?.let { act ->
                        (act as? MainActivity)?.importDatabase?.launch("application/zip")
                    }

                    true
                }
            }
        }
        with (findPreference<Preference>(getString(R.string.key_pref_db_export))) {
            this?.let {
                setOnPreferenceClickListener {
                    activity?.let { act ->
                        (act as? MainActivity)?.exportDatabase?.launch("intercross_${DateUtil().getTime()}.zip")
                    }

                    true
                }
            }
        }
    }

    // first confirmation
    private fun showDatabaseResetDialog1() {
        context?.let {
            AlertDialog.Builder(it)
                .setTitle(getString(R.string.dialog_warning))
                .setMessage(getString(R.string.database_reset_warning1))
                .setNegativeButton(getString(R.string.dialog_no)) { dialog, _ -> dialog.dismiss() }
                .setPositiveButton(getString(R.string.dialog_yes)) { dialog, _ ->
                    dialog.dismiss()
                    showDatabaseResetDialog2()
                }
                .create()
                .show()
        }
    }

    // second confirmation
    private fun showDatabaseResetDialog2() {
        context?.let {
            AlertDialog.Builder(it)
                .setTitle(getString(R.string.dialog_warning))
                .setMessage(getString(R.string.database_reset_warning2))
                .setNegativeButton(getString(R.string.dialog_no)) { dialog, _ -> dialog.dismiss() }
                .setPositiveButton(getString(R.string.dialog_yes)) { dialog, _ ->
                    try {
                        resetDatabase(it)
                        dialog.dismiss()
                        activity?.finishAffinity()
                    } catch (e: Exception) {
                        Log.e("Intercross", e.message ?: "Error")
                    }
                }
                .create()
                .show()
        }

    }

    private fun resetDatabase(context: Context) {
        viewLifecycleOwner.lifecycleScope.launch {
            withContext(Dispatchers.IO) {
                val db = IntercrossDatabase.getInstance(context)
                db.clearAllTables()

                mPrefs.edit().clear().apply()
            }
        }
    }
}
