package org.phenoapps.intercross.util

import android.Manifest
import android.content.Context
import android.os.Build
import android.os.Bundle
import android.widget.AdapterView
import androidx.fragment.app.Fragment
import androidx.preference.PreferenceManager
import org.phenoapps.intercross.R
import org.phenoapps.intercross.activities.MainActivity
import org.phenoapps.intercross.dialogs.FileExploreDialogFragment
import org.phenoapps.intercross.dialogs.ListAddDialog
import org.phenoapps.utils.BaseDocumentTreeUtil.Companion.getDirectory
import pub.devrel.easypermissions.AfterPermissionGranted
import pub.devrel.easypermissions.EasyPermissions

class ImportUtil (private val context: Context, private val importDirectory: Int, private val importDialogTitle: String) {

    private val mPref by lazy {
        PreferenceManager.getDefaultSharedPreferences(context)
    }

    private val mKeyUtil by lazy {
        KeyUtil(context)
    }

    fun showImportDialog(fragment: Fragment) {
        var importArray: Array<String?> = arrayOf(
            context.getString(R.string.import_source_local),
            // context.getString(R.string.import_source_cloud),
        )

        if (mPref.getBoolean(mKeyUtil.brapiEnabled, false)) {
            val displayName = mPref.getString(
                mKeyUtil.brapiDisplayName,
                context.getString(R.string.brapi_edit_display_name_default)
            ) ?: context.getString(R.string.brapi_edit_display_name_default)

            importArray = importArray.copyOf(importArray.size + 1).apply {
                this[1] = displayName
            }
        }

        val icons = IntArray(importArray.size).apply {
            this[0] = R.drawable.ic_file_generic
            // this[1] = R.drawable.ic_file_cloud
            if (importArray.size > 1) {
                this[1] = R.drawable.ic_adv_brapi
            }
        }

        val onItemClickListener =
            AdapterView.OnItemClickListener { _, _, position, _ ->
                when (position) {
                    0 -> loadLocalPermission(fragment)
                    // 1 -> loadCloud()
                    // 2 ->loadBrAPI()
                }
            }

        // TODO: remove this array size checking when BrAPI is added in the app
        if (importArray.size == 1) loadLocalPermission(fragment)
        else fragment.activity?.let {
            val dialog = ListAddDialog(it, context.getString(R.string.import_file), importArray, icons, onItemClickListener)
            dialog.show(it.supportFragmentManager, "ListAddDialog")
        }
    }

    @AfterPermissionGranted(1)
    private fun loadLocalPermission(fragment: Fragment) {
        fragment.activity?.let {
            if (Build.VERSION.SDK_INT <= Build.VERSION_CODES.P) {
                val perms = arrayOf(Manifest.permission.READ_EXTERNAL_STORAGE)
                if (EasyPermissions.hasPermissions(it, *perms)) {
                    loadLocal(fragment)
                } else {
                    EasyPermissions.requestPermissions(
                        it,
                        it.getString(R.string.permission_rationale_storage_import),
                        123,
                        *perms
                    )
                }
            } else loadLocal(fragment)
        }
    }

    private fun loadLocal(fragment: Fragment) {
        try {
            fragment.let {
                val importDir = getDirectory(context, importDirectory)
                if (importDir != null && importDir.exists()) {
                    FileExploreDialogFragment().apply {
                        arguments = Bundle().apply {
                            putString("dialogTitle", importDialogTitle)
                            putString("path", importDir.uri.toString())
                            putStringArray("include", arrayOf("csv", "xls", "xlsx"))
                        }
                        setOnFileSelectedListener { uri ->
                            (it.activity as MainActivity).importFromUri(uri)
                        }
                    }.show(it.parentFragmentManager, "FileExploreDialogFragment")
                }
            }
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }
}