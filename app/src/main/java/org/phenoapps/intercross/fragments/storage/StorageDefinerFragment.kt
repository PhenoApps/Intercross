package org.phenoapps.intercross.fragments.storage

import android.app.Activity
import android.content.Context
import android.content.SharedPreferences
import android.os.Bundle
import android.view.View
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.runBlocking
import org.phenoapps.fragments.storage.PhenoLibStorageDefinerFragment
import org.phenoapps.intercross.R
import org.phenoapps.security.Security
import org.phenoapps.utils.BaseDocumentTreeUtil
import javax.inject.Inject

@AndroidEntryPoint
class StorageDefinerFragment : PhenoLibStorageDefinerFragment() {

    @Inject
    lateinit var prefs: SharedPreferences

    private val advisor by Security().secureDocumentTree()

    // default root folder name if user choose an incorrect root on older devices
    override val defaultAppName: String = "intercross"

    // if this file exists the migrator will be skipped
    override val migrateChecker: String = ".intercross"

    // define sample data and where to transfer
    override val samples = mapOf(
        AssetSample("wishlist_import", "wishlist_sample.csv") to R.string.dir_wishlist_import,
        AssetSample("parents_import", "parents_sample.csv") to R.string.dir_parents_import
    )

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        // define directories that should be created in root storage
        context?.let { ctx ->
            val wishlistImport = ctx.getString(R.string.dir_wishlist_import)
            val parentsImport = ctx.getString(R.string.dir_parents_import)
            val crossesExport = ctx.getString(R.string.dir_crosses_export)
            val databaseDir = ctx.getString(R.string.dir_database)
            directories = arrayOf(wishlistImport, parentsImport, crossesExport, databaseDir)
        }

        view.visibility = View.GONE

        advisor.defineDocumentTree({ treeUri ->
            runBlocking {
                directories?.let { dirs ->
                    BaseDocumentTreeUtil.defineRootStructure(context, treeUri, dirs)?.let { root ->
                        samples.entries.forEach { entry ->
                            val sampleAsset = entry.key
                            val dir = entry.value

                            BaseDocumentTreeUtil.copyAsset(context, sampleAsset.name, sampleAsset.dir, dir)
                        }
                        activity?.setResult(Activity.RESULT_OK)
                        activity?.finish()
                    }
                }
            } },
            {
                activity?.finish()
            }
        )
    }

    override fun onAttach(context: Context) {
        super.onAttach(context)
        advisor.initialize()
    }
}