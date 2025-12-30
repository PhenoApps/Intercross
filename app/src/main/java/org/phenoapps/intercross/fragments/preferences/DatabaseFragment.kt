package org.phenoapps.intercross.fragments.preferences

import android.content.Context
import android.content.Intent
import android.net.Uri
import android.os.Bundle
import android.util.Log
import android.view.View
import android.widget.EditText
import android.widget.Toast
import androidx.appcompat.app.AlertDialog
import androidx.lifecycle.lifecycleScope
import androidx.preference.Preference
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import org.phenoapps.intercross.R
import org.phenoapps.intercross.activities.DefineStorageActivity
import org.phenoapps.intercross.activities.MainActivity
import org.phenoapps.intercross.data.IntercrossDatabase
import org.phenoapps.intercross.util.DateUtil
import org.phenoapps.utils.BaseDocumentTreeUtil.Companion.getRoot
import org.phenoapps.utils.BaseDocumentTreeUtil.Companion.getStem
import org.phenoapps.utils.BaseDocumentTreeUtil.Companion.isEnabled
import androidx.core.content.edit
import dagger.hilt.android.AndroidEntryPoint
import org.phenoapps.intercross.dialogs.FileExploreDialogFragment
import org.phenoapps.intercross.util.FileUtil
import org.phenoapps.utils.BaseDocumentTreeUtil.Companion.getDirectory
import javax.inject.Inject

@AndroidEntryPoint
class DatabaseFragment : BasePreferenceFragment(R.xml.database_preferences) {

    private var defaultStorageLocation: Preference? = null

    @Inject
    lateinit var fileUtil: FileUtil

    companion object {
        private const val TAG = "DatabaseFragment"
    }

    override fun onResume() {
        super.onResume()
        setToolbar(getString(R.string.prefs_database_title))
        context?.let {
            if (isEnabled(it)) {
                val root = getRoot(it)
                if (root != null && root.exists()) {
                    var path = root.uri.lastPathSegment
                    if (path == null) {
                        path = root.uri.getStem(it)
                    }
                    defaultStorageLocation?.setSummary(path)
                }
            }
        }
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        defaultStorageLocation = findPreference(getString(R.string.key_pref_storage_definer))

        with (defaultStorageLocation) {
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
                    showDatabaseImportDialog()

                    true
                }
            }
        }
        with (findPreference<Preference>(getString(R.string.key_pref_db_export))) {
            this?.let {
                setOnPreferenceClickListener {
                    showDatabaseExportDialog()

                    true
                }
            }
        }
    }

    private fun showDatabaseImportDialog() {
        context?.let { ctx ->
            // make sure db directory exists
            val databaseDir = getDirectory(context, R.string.dir_database)
            if (databaseDir == null || !databaseDir.exists()) return@let

            FileExploreDialogFragment().apply {
                arguments = Bundle().apply {
                    putString(ctx.getString(R.string.dialog_title), ctx.getString(R.string.import_database_title))
                    putString(ctx.getString(R.string.path), databaseDir.uri.toString())
                    putStringArray(ctx.getString(R.string.include), arrayOf("zip"))
                }
                setOnFileSelectedListener { uri ->
                    importDatabase(uri) { importSuccess ->
                        val msg = if (importSuccess) R.string.database_import_success else R.string.database_import_error
                        Toast.makeText(ctx, msg, Toast.LENGTH_SHORT).show()
                    }
                }
            }.show(parentFragmentManager, FileExploreDialogFragment.TAG)
        }
    }

    private fun showDatabaseExportDialog() {
        val defaultFilePrefix = context?.getString(R.string.default_database_export_file_name) ?: "intercross"
        val fileName = "${defaultFilePrefix}_${DateUtil().getTime()}"

        val inflater = (activity as MainActivity).layoutInflater
        val layout = inflater.inflate(R.layout.dialog_export, null)
        val fileNameET = layout.findViewById<EditText>(R.id.file_name)

        fileNameET.setText(fileName)
        fileNameET.clearFocus()

        val dialog = AlertDialog.Builder(activity as MainActivity)
            .setTitle(R.string.database_export_title)
            .setView(layout)
            .setPositiveButton(getString(R.string.dialog_export), null)
            .setNegativeButton(getString(R.string.dialog_cancel)) { d, _ -> d.dismiss() }
            .create()

        dialog.setOnShowListener {
            dialog.getButton(AlertDialog.BUTTON_POSITIVE).setOnClickListener {
                val fileName = fileNameET.text.toString().trim()

                when {
                    fileName.isBlank() -> {
                        fileNameET.error = getString(R.string.database_file_name_blank_error)
                    }
                    else -> {
                        exportDatabase(fileName) { exportSuccess ->
                            dialog.dismiss()

                            val msg = if (exportSuccess) R.string.database_export_success else R.string.database_export_error
                            Toast.makeText(context, msg, Toast.LENGTH_SHORT).show()
                        }
                    }
                }
            }
        }

        dialog.show()
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
        context?.let { ctx ->
            AlertDialog.Builder(ctx)
                .setTitle(getString(R.string.dialog_warning))
                .setMessage(getString(R.string.database_reset_warning2))
                .setNegativeButton(getString(R.string.dialog_no)) { dialog, _ -> dialog.dismiss() }
                .setPositiveButton(getString(R.string.dialog_yes)) { dialog, _ ->
                    runCatching {
                        resetDatabase(ctx) { resetStatus ->
                            dialog.dismiss()
                            if (resetStatus) {
                                Toast.makeText(ctx, R.string.database_reset_success, Toast.LENGTH_SHORT).show()
                                activity?.finishAffinity()
                            } else {
                                Toast.makeText(ctx, R.string.database_reset_error, Toast.LENGTH_SHORT).show()
                            }
                        }
                    }
                }
                .create()
                .show()
        }

    }

    private fun resetDatabase(context: Context, successCallBack: (Boolean) -> Unit) {
        viewLifecycleOwner.lifecycleScope.launch {
            runCatching {
                withContext(Dispatchers.IO) {
                    // create a db backup
                    // any errors here will be ignored and the db will be reset
                    runCatching {
                        val defaultFilePrefix =
                            getString(R.string.default_database_backup_export_file_name)
                        val fileName = "${defaultFilePrefix}_${DateUtil().getTime()}"

                        fileUtil.exportDatabase(fileName)
                    }.onFailure { e ->
                        Log.w(TAG, "Error creating database backup: ${e.message}", e)
                    }

                    val db = IntercrossDatabase.getInstance(context)
                    db.clearAllTables()

                    mPrefs.edit { clear() }
                }
            }.onFailure { e ->
                Log.e(TAG, "Error resetting the database: ${e.message}", e)
                successCallBack(false)
            }.onSuccess {
                successCallBack(true)
            }
        }
    }

    private fun importDatabase(uri: Uri, successCallBack: (Boolean) -> Unit) {
        viewLifecycleOwner.lifecycleScope.launch {
            runCatching {
                withContext(Dispatchers.IO) {
                    fileUtil.importDatabase(uri)
                }
            }.onFailure { e ->
                Log.e(TAG, "Error importing database: ${e.message}")
                successCallBack(false)
            }.onSuccess {
                successCallBack(true)
            }
        }
    }

    private fun exportDatabase(fileName: String, successCallBack: (Boolean) -> Unit) {
        viewLifecycleOwner.lifecycleScope.launch {
            runCatching {
                withContext(Dispatchers.IO) {
                    fileUtil.exportDatabase(fileName)
                }
            }.onFailure { e ->
                Log.e(TAG, "Error exporting database: ${e.message}", e)
                successCallBack(false)
            }.onSuccess {
                successCallBack(true)
            }
        }
    }
}
