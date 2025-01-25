package org.phenoapps.intercross.util

import android.content.Context
import android.util.Log
import android.view.LayoutInflater
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AlertDialog
import androidx.documentfile.provider.DocumentFile
import androidx.preference.PreferenceManager
import com.google.android.material.dialog.MaterialAlertDialogBuilder
import dagger.hilt.android.qualifiers.ActivityContext
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import org.phenoapps.intercross.R
import org.phenoapps.intercross.data.models.Event
import org.phenoapps.intercross.data.models.Meta
import org.phenoapps.intercross.data.models.MetadataValues
import org.phenoapps.intercross.data.models.Parent
import org.phenoapps.intercross.data.models.PollenGroup
import org.phenoapps.intercross.data.viewmodels.EventListViewModel
import org.phenoapps.utils.BaseDocumentTreeUtil
import java.io.OutputStreamWriter
import javax.inject.Inject

class ExportUtil@Inject constructor(@ActivityContext private val context: Context) {

    companion object {
        private const val TAG = "ExportUtil"
    }

    private val ioScope = CoroutineScope(Dispatchers.IO)
    private var progressDialog: AlertDialog? = null

    /**
     * Parent's input file header fields. These are queried from the strings XML.
     */
    private val parentsCodeIdHeader: String by lazy { context.getString(R.string.parents_header_code_id) }
    private val parentsNameHeader: String by lazy { context.getString(R.string.parents_header_readable_name) }
    private val parentsSexHeader: String by lazy { context.getString(R.string.parents_header_sex) }
    private val parentHeaders = setOf(parentsCodeIdHeader, parentsNameHeader, parentsSexHeader)

    /**
     * Wishlist input file header fields. These are queried from the strings XML.
     */
    private val wishlistFemaleIdHeader: String by lazy { context.getString(R.string.wishlist_header_female_id) }
    private val wishlistMaleIdHeader: String by lazy { context.getString(R.string.wishlist_header_male_id) }
    private val wishlistMaleNameHeader: String by lazy { context.getString(R.string.wishlist_header_male_name) }
    private val wishlistFemaleNameHeader: String by lazy { context.getString(R.string.wishlist_header_female_name) }
    private val wishlistTypeHeader: String by lazy { context.getString(R.string.wishlist_header_type) }
    private val wishlistMinHeader: String by lazy { context.getString(R.string.wishlist_header_min) }
    private val wishlistMaxHeader: String by lazy { context.getString(R.string.wishlist_header_max) }

    private val wishlistHeaders = setOf(
        wishlistFemaleIdHeader,
        wishlistMaleIdHeader,
        wishlistMaleNameHeader,
        wishlistFemaleIdHeader,
        wishlistFemaleNameHeader,
        wishlistTypeHeader,
        wishlistMinHeader,
        wishlistMaxHeader
    )

    /**
     * Cross table export file header fields.
     */

    private val crossIdHeader: String by lazy { context.getString(R.string.crosses_export_id_header) }
    private val crossMomHeader: String by lazy { context.getString(R.string.crosses_export_mom_header) }
    private val crossDadHeader: String by lazy { context.getString(R.string.crosses_export_dad_header) }
    private val crossTimestampHeader: String by lazy { context.getString(R.string.crosses_export_date_header) }
    private val crossPersonHeader: String by lazy { context.getString(R.string.crosses_export_person_header) }
    private val crossExperimentHeader: String by lazy { context.getString(R.string.crosses_export_experiment_header) }
    private val crossTypeHeader: String by lazy { context.getString(R.string.crosses_export_type_header) }

    private val crossHeaders = setOf(crossIdHeader, crossMomHeader, crossDadHeader,
        crossTimestampHeader, crossPersonHeader, crossExperimentHeader,
        crossTypeHeader)

    private val eventModelHeaderString by lazy {
        crossHeaders.joinToString(",")
    }

    private val mPref by lazy {
        PreferenceManager.getDefaultSharedPreferences(context)
    }

    sealed class ExportResult {
        data class Success(val file: DocumentFile): ExportResult()
        data class Failure(val error: Throwable): ExportResult()
        data object NoData: ExportResult()
    }

    fun exportCrosses(
        eventsModel: EventListViewModel,
        crosses: List<Event>,
        parents: List<Parent>,
        groups: List<PollenGroup>,
        metadata: List<Meta>,
        metaValues: List<MetadataValues>,
        fileName: String
    ) {
        if (crosses.isEmpty()) {
            handleExportResult(ExportResult.NoData)
            return
        }

        showProgressDialog()

        ioScope.launch {
            val result = withContext(Dispatchers.IO) {
                try {
                    val exportDir = BaseDocumentTreeUtil.getDirectory(context, R.string.dir_crosses_export)

                    exportDir?.let { dir ->
                        dir.exists().let {
                            val file = dir.createFile("text/csv", fileName)
                            file?.let { docFile ->
                                context.contentResolver.openOutputStream(docFile.uri)?.use { stream ->
                                    val writer = OutputStreamWriter(stream)

                                    val properties = if (metadata.isNotEmpty()) {
                                        metadata.joinToString(",", ",") { it.property }
                                    } else ""
                                    val propMap = metadata.map { it.id to it.property }

                                    // add metadata properties as headers to the export file
                                    writer.write("$eventModelHeaderString$properties\n")

                                    crosses.forEach { cross ->
                                        val values = getMetadataValues(cross, metadata, metaValues, propMap)
                                        val valueString = if (values.isNotEmpty()) {
                                            values.joinToString(",", ",") { it }
                                        } else ""

                                        if (groups.any { it.codeId == cross.maleObsUnitDbId }) {
                                            val groupName = groups.find { it.codeId == cross.maleObsUnitDbId }?.name
                                            val males = groups.filter { g -> g.codeId == cross.maleObsUnitDbId }
                                                .map { g ->
                                                    parents.find { c -> c.id == g.maleId }.let {
                                                        it?.codeId
                                                    }
                                                }.joinToString(";", "{", "}")
                                            writer.write("${cross.toPollenGroupString(males, groupName)}$valueString\n")
                                        } else {
                                            writer.write("$cross$valueString\n")
                                        }

                                    }

                                    writer.flush()
                                    ExportResult.Success(docFile)
                                }
                            }
                        }
                    } ?: throw Exception("Could not access export directory")
                } catch (e: Exception) {
                    Log.e(TAG, "Export failed: ${e.message}", e)
                    ExportResult.Failure(e)
                }
            }

            withContext(Dispatchers.Main) {
                handleExportResult(result)
                if (result is ExportResult.Success) {
                    showCrossesDeleteDialog(eventsModel)
                }
            }
        }
    }

    private fun handleExportResult(result: ExportResult) {
        progressDialog?.dismiss()

        when (result) {
            is ExportResult.Success -> {
                Toast.makeText(context, context.getString(R.string.export_success), Toast.LENGTH_SHORT).show()
            }
            is ExportResult.Failure -> {
                Toast.makeText(context, String.format(context.getString(R.string.export_failed), result.error.message), Toast.LENGTH_SHORT).show()
            }
            is ExportResult.NoData -> {
                Toast.makeText(context, context.getString(R.string.export_no_data), Toast.LENGTH_SHORT).show()
            }
        }
    }

    /**
     * Print either the actual saved values for each property or its default value
     */
    private fun getMetadataValues(
        cross: Event,
        metadata: List<Meta>,
        metaValues: List<MetadataValues>,
        propMap: List<Pair<Long?, String>>
    ): List<String> {
        return propMap.map { (metaId, property) ->
            val actuals = metaValues.filter { it.eid == cross.id?.toInt() && metaId?.toInt() == it.metaId }
            if (actuals.isNotEmpty()) {
                actuals.first().value.toString()
            } else {
                metadata.find { it.property == property }?.defaultValue?.toString() ?: "0"
            }
        }
    }

    private fun showProgressDialog() {
        val progressView = LayoutInflater.from(context).inflate(R.layout.dialog_export_progress, null)
        progressView.findViewById<TextView>(R.id.message).text = context.getString(R.string.dialog_export_message)

        progressDialog = MaterialAlertDialogBuilder(context)
            .setView(progressView)
            .setCancelable(false)
            .create()
            .also { it.show() }
    }

    /**
     * Delete crosses dialog after successful export
     */
    private fun showCrossesDeleteDialog(eventsModel: EventListViewModel) {
        AlertDialog.Builder(context)
            .setTitle(context.getString(R.string.export_dialog_delete_crosses_title))
            .setMessage(context.getString(R.string.export_dialog_delete_crosses_message))
            .setNegativeButton(context.getString(R.string.dialog_no)) { d, _ -> d.dismiss()}
            .setPositiveButton(context.getString(R.string.dialog_yes)) { d, _ ->
                d.dismiss()
                eventsModel.deleteAll()
            }
            .show()
    }
}