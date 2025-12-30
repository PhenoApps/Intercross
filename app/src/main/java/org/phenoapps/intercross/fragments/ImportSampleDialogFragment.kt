package org.phenoapps.intercross.fragments

import android.app.AlertDialog
import android.app.Dialog
import android.os.Bundle
import android.widget.TextView
import android.widget.Toast
import androidx.fragment.app.DialogFragment
import androidx.preference.PreferenceManager
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import org.phenoapps.intercross.R
import org.phenoapps.intercross.activities.MainActivity
import org.phenoapps.intercross.util.KeyUtil
import org.phenoapps.utils.BaseDocumentTreeUtil
import javax.inject.Inject

@AndroidEntryPoint
class ImportSampleDialogFragment : DialogFragment() {

    @Inject
    lateinit var mKeyUtil: KeyUtil

    override fun onCreateDialog(savedInstanceState: Bundle?): Dialog {
        val inflater = activity?.layoutInflater
        val view = inflater?.inflate(R.layout.dialog_loading, null)

        val loadingTextView: TextView? = view?.findViewById(R.id.loading_text_view)
        loadingTextView?.text = getString(R.string.dialog_import_loading_sample)

        val builder = AlertDialog.Builder(context)
            .setView(view)

        startImportSample()

        return builder.create()
    }

    private fun startImportSample() {
        val mPref = context?.let { PreferenceManager.getDefaultSharedPreferences(it) }
        val loadSampleWishlist = mPref?.getBoolean(mKeyUtil.loadSampleWishlist, false)
        val loadSampleParents = mPref?.getBoolean(mKeyUtil.loadSampleParents, false)

        CoroutineScope(Dispatchers.IO).launch {
            try {
                context?.let { ctx ->
                    val wishlistDir =
                        BaseDocumentTreeUtil.getDirectory(ctx, R.string.dir_wishlist_import)
                    val parentsDir =
                        BaseDocumentTreeUtil.getDirectory(ctx, R.string.dir_parents_import)

                    if (wishlistDir != null && parentsDir != null) {
                        if (loadSampleWishlist == true) {
                            val wishlistFile = BaseDocumentTreeUtil.getFile(
                                ctx,
                                R.string.dir_wishlist_import,
                                "wishlist_sample.csv"
                            )
                            if (wishlistFile != null) {
                                if (wishlistFile.exists()) {
                                    (activity as? MainActivity)?.importFromUri(wishlistFile.uri)
                                }
                            }
                        }

                        if (loadSampleParents == true) {
                            val parentsFile = BaseDocumentTreeUtil.getFile(
                                ctx,
                                R.string.dir_parents_import,
                                "parents_sample.csv"
                            )
                            if (parentsFile != null) {
                                if (parentsFile.exists()) {
                                    (activity as? MainActivity)?.importFromUri(parentsFile.uri)
                                }
                            }
                        }

                        withContext(Dispatchers.Main) {
                            Toast.makeText(ctx, getString(R.string.load_sample_success_toast), Toast.LENGTH_SHORT).show()
                            dismiss()
                        }
                    }
                }
            } catch (e: Exception) {
                e.printStackTrace()
                withContext(Dispatchers.Main) {
                    context?.let { ctx ->
                        Toast.makeText(ctx, R.string.load_sample_failed_toast, Toast.LENGTH_SHORT)
                            .show()
                        dismiss()
                    }
                }
            }
        }
    }
}