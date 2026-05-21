package org.phenoapps.intercross.util

import android.Manifest
import android.content.Context
import android.os.Build
import android.os.Bundle
import android.widget.AdapterView
import androidx.core.os.bundleOf
import androidx.fragment.app.Fragment
import androidx.preference.PreferenceManager
import androidx.navigation.fragment.findNavController
import org.phenoapps.intercross.R
import org.phenoapps.intercross.activities.MainActivity
import org.phenoapps.intercross.brapi.service.BrAPIServiceV2
import org.phenoapps.intercross.dialogs.FileExploreDialogFragment
import org.phenoapps.intercross.dialogs.ListAddDialog
import org.phenoapps.utils.BaseDocumentTreeUtil.Companion.getDirectory
import pub.devrel.easypermissions.AfterPermissionGranted
import pub.devrel.easypermissions.EasyPermissions

class ImportUtil(
    private val context: Context,
    private val importDirectory: Int,
    private val importDialogTitle: String,
    private val brapiImportMode: Int = BRAPI_MODE_WISHLIST
) {

    companion object {
        const val BRAPI_MODE_WISHLIST = 0
        const val BRAPI_MODE_PARENTS = 1
        const val BRAPI_MODE_IMPORT_CROSSES = 2
        const val BRAPI_MODE_EXPORT_CROSSES = 3
        const val IMPORT_MODE_ARG = "mode"
    }

    private val prefs by lazy {
        PreferenceManager.getDefaultSharedPreferences(context)
    }

    private val keyUtil by lazy {
        KeyUtil(context)
    }

    fun showImportDialog(fragment: Fragment) {
        var importArray: Array<String?> = arrayOf(
            context.getString(R.string.import_source_local),
        )

        if (prefs.getBoolean(keyUtil.brapiEnabled, false)) {
            val displayName = prefs.getString(
                keyUtil.brapiDisplayName,
                context.getString(R.string.brapi_edit_display_name_default)
            ) ?: context.getString(R.string.brapi_edit_display_name_default)

            importArray = importArray.copyOf(importArray.size + 1).apply {
                this[1] = displayName
            }
        }

        val icons = IntArray(importArray.size).apply {
            this[0] = R.drawable.ic_file_generic
            if (importArray.size > 1) {
                this[1] = R.drawable.ic_adv_brapi
            }
        }

        val onItemClickListener =
            AdapterView.OnItemClickListener { _, _, position, _ ->
                when (position) {
                    0 -> loadLocalPermission(fragment)
                    1 -> {
                        fragment.findNavController().navigate(
                            R.id.global_action_to_wishlist_import,
                            bundleOf(IMPORT_MODE_ARG to brapiImportMode)
                        )
                    }
                }
            }

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
            val appContext: Context = context
            val importDir = getDirectory(appContext, importDirectory)
            if (importDir != null && importDir.exists()) {
                val dialogTitleKey = appContext.getString(R.string.dialog_title)
                val pathKey = appContext.getString(R.string.path)
                val includeKey = appContext.getString(R.string.include)
                val dialog = FileExploreDialogFragment()
                dialog.arguments = Bundle().apply {
                    putString(dialogTitleKey, importDialogTitle)
                    putString(pathKey, importDir.uri.toString())
                    putStringArray(includeKey, arrayOf("csv", "xls", "xlsx"))
                }
                dialog.setOnFileSelectedListener { uri ->
                    (fragment.activity as MainActivity).importFromUri(uri)
                }
                dialog.show(fragment.parentFragmentManager, FileExploreDialogFragment.TAG)
            }
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }
}