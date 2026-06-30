package org.phenoapps.intercross.ui.brapi

import android.os.Bundle
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.AssistChip
import androidx.compose.material3.AssistChipDefaults
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.pluralStringResource
import androidx.compose.ui.res.stringResource

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import org.brapi.v2.model.germ.BrAPICross
import org.brapi.v2.model.germ.BrAPICrossParent
import org.brapi.v2.model.germ.BrAPIParentType
import org.phenoapps.intercross.R
import org.phenoapps.intercross.brapi.service.BrAPIServiceV2
import org.phenoapps.intercross.brapi.service.BrapiPaginationManager
import org.phenoapps.intercross.data.EventsRepository
import org.phenoapps.intercross.data.ParentsRepository
import org.phenoapps.intercross.data.models.CrossType
import org.phenoapps.intercross.data.models.Event
import org.phenoapps.intercross.data.models.Parent
import org.phenoapps.intercross.fragments.brapi.BrapiRequestException
import org.phenoapps.intercross.fragments.brapi.awaitCrosses
import androidx.compose.foundation.layout.Box
import org.phenoapps.intercross.ui.app.TopBarState
import org.phenoapps.intercross.ui.components.CrossListItem
import org.phenoapps.intercross.ui.theme.AppTheme
import org.phenoapps.intercross.ui.app.rememberDatabase
import org.phenoapps.intercross.util.DateUtil

import org.phenoapps.intercross.ui.preview.PreviewSampleData
import org.phenoapps.intercross.ui.theme.IntercrossPreviewTheme
import androidx.compose.ui.tooling.preview.Preview

@Composable
fun BrapiCrossImportRoute(
    args: Bundle,
    onBack: () -> Unit,
    onDone: () -> Unit,
    onShowMessage: (String) -> Unit,
) {
    val context = LocalContext.current
    val db = rememberDatabase()
    val service = remember(context) { BrAPIServiceV2(context) }
    val eventsRepository = remember(db) { EventsRepository.getInstance(db.eventsDao()) }
    val parentsRepository = remember(db) { ParentsRepository.getInstance(db.parentsDao()) }
    val scope = rememberCoroutineScope()
    val projectDbId = args.getString("crossingProjectDbId").orEmpty()
    val programName = args.getString("programName").orEmpty()
    var crosses by remember(args) { mutableStateOf(emptyList<BrAPICross>()) }
    var isLoading by remember { mutableStateOf(false) }
    var isImporting by remember { mutableStateOf(false) }
    var importProgress by remember { mutableIntStateOf(0) }
    var hasError by remember { mutableStateOf(false) }

    fun load() {
        if (!canReachBrapi(context, onShowMessage)) return
        isLoading = true
        hasError = false
        crosses = emptyList()
        scope.launch {
            try {
                val pagination = BrapiPaginationManager(context)
                pagination.reset()
                crosses = service.awaitCrosses(projectDbId, pagination)
                    .filter { it.crossingProjectDbId == projectDbId }
            } catch (error: BrapiRequestException) {
                hasError = true
                onShowMessage("BrAPI callback failed. ${error.code}")
            } catch (error: Exception) {
                hasError = true
                onShowMessage(error.localizedMessage ?: context.getString(R.string.brapi_planned_crosses_load_error))
            } finally {
                isLoading = false
            }
        }
    }

    LaunchedEffect(projectDbId) {
        load()
    }

    val topBarState = TopBarState(
        titleRes = R.string.brapi_cross_import_title,
        showBack = true,
        onBack = onBack
    )

    BrapiScaffold(topBarState = topBarState) { innerPadding ->
        BrapiCrossImportScreen(
            modifier = Modifier.padding(innerPadding),
            args = args,
            crosses = crosses,
            isLoading = isLoading,
            isImporting = isImporting,
            importProgress = importProgress,
            hasError = hasError,
            onRetry = ::load,
            onImport = {
                if (crosses.isEmpty()) {
                    onShowMessage(context.getString(R.string.fragment_brapi_import_no_planned_crosses_found))
                    return@BrapiCrossImportScreen
                }
                isImporting = true
                importProgress = 0
                scope.launch {
                    val total = crosses.size
                    try {
                        crosses.forEach { cross ->
                            val row = cross.toCrossRow()
                            withContext(Dispatchers.IO) {
                                val maleParent = Parent(codeId = row.maleId, sex = 1).also { it.name = row.maleName }
                                val femaleParent = Parent(codeId = row.femaleId, sex = 0).also { it.name = row.femaleName }
                                parentsRepository.insertIgnore(maleParent, femaleParent)
                                parentsRepository.updateName(maleParent, femaleParent)
                                eventsRepository.insert(
                                    Event(
                                        eventDbId = row.crossId,
                                        femaleObsUnitDbId = row.femaleId,
                                        maleObsUnitDbId = row.maleId,
                                        readableName = row.title,
                                        timestamp = DateUtil().getTime(),
                                        person = "",
                                        experiment = programName,
                                        type = determineCrossType(row.maleId, row.femaleId),
                                    ),
                                )
                            }
                            importProgress += 1
                        }
                        onShowMessage(context.getString(R.string.brapi_imported_crosses, total))
                        onDone()
                    } catch (error: Exception) {
                        onShowMessage(error.localizedMessage ?: context.getString(R.string.brapi_planned_crosses_load_error))
                    } finally {
                        isImporting = false
                    }
                }
            }
        )
    }
}

@Composable
internal fun BrapiCrossImportScreen(
    modifier: Modifier = Modifier,
    args: Bundle,
    crosses: List<BrAPICross>,
    isLoading: Boolean,
    isImporting: Boolean,
    importProgress: Int,
    hasError: Boolean,
    onRetry: () -> Unit,
    onImport: () -> Unit,
) {
    Box(modifier) {
    BrapiDetailList(
        args = args,
        isLoading = isLoading || isImporting,
        hasError = hasError,
        itemCount = crosses.size,
        countLabel = pluralStringResource(R.plurals.brapi_cross_count, crosses.size, crosses.size),
        emptyLabel = stringResource(R.string.fragment_brapi_import_no_planned_crosses_found),
        onRetry = onRetry,
    ) {
        items(crosses, key = { it.crossDbId ?: it.hashCode().toString() }) { cross ->
            val row = remember(cross) { cross.toCrossRow() }
            CrossListItem(
                event = Event(
                    eventDbId = row.crossId,
                    femaleObsUnitDbId = row.femaleId,
                    maleObsUnitDbId = row.maleId,
                    readableName = row.title,
                    timestamp = "",
                    person = "",
                    experiment = "",
                ),
                showQrCode = false
            )
        }
        item {
            Button(
                modifier = Modifier.fillMaxWidth(),
                enabled = crosses.isNotEmpty() && !isLoading && !isImporting,
                onClick = onImport,
                colors = ButtonDefaults.buttonColors(containerColor = AppTheme.colors.accent),
            ) {
                if (isImporting) {
                    ProgressButtonLabel(text = stringResource(R.string.brapi_import_progress, importProgress, crosses.size))
                } else {
                    Text(stringResource(R.string.brapi_import_all_crosses))
                }
            }
        }
    }
    }
}

// ─── Previews ───────────────────────────────────────────────────────────────────

@Preview(showBackground = true, name = "BrapiCrossImport - Populated")
@Composable
private fun BrapiCrossImportPopulatedPreview() {
    IntercrossPreviewTheme {
        val args = Bundle().apply {
            putString("crossingProjectName", "Apple Breeding 2024")
            putString("crossingProjectDescription", "Developing disease-resistant cultivars")
            putString("crossingProjectDbId", "PRJ001")
            putString("programName", "Fruit Tree Improvement")
            putString("commonCropName", "Apple")
            putInt("parentCount", 12)
        }
        BrapiCrossImportScreen(
            args = args,
            crosses = PreviewSampleData.brapiCrosses,
            isLoading = false,
            isImporting = false,
            importProgress = 0,
            hasError = false,
            onRetry = {},
            onImport = {}
        )
    }
}

private data class CrossRow(
    val title: String,
    val crossId: String,
    val maleName: String,
    val maleId: String,
    val femaleName: String,
    val femaleId: String,
)

private fun BrAPICross.toCrossRow(): CrossRow {
    val orderedParents = orderedParents(parent1 ?: BrAPICrossParent(), parent2 ?: BrAPICrossParent())
    val maleId = orderedParents.first.observationUnitDbId ?: ""
    val femaleId = orderedParents.second.observationUnitDbId ?: ""
    val maleName = orderedParents.first.observationUnitName ?: maleId.ifBlank { "Unknown" }
    val femaleName = orderedParents.second.observationUnitName ?: femaleId.ifBlank { "Unknown" }
    val crossId = externalReferences?.find { it.referenceSource == "Intercross" }?.let { ref ->
        ref.referenceId ?: ref.referenceID
    } ?: crossDbId ?: ""
    return CrossRow(
        title = crossName ?: "$femaleName x $maleName",
        crossId = crossId,
        maleName = maleName,
        maleId = maleId,
        femaleName = femaleName,
        femaleId = femaleId,
    )
}

private fun orderedParents(parent1: BrAPICrossParent, parent2: BrAPICrossParent): Pair<BrAPICrossParent, BrAPICrossParent> {
    return if (parent1.parentType == BrAPIParentType.MALE || parent1.parentType?.toString() == "MALE") {
        parent1 to parent2
    } else {
        parent2 to parent1
    }
}

private fun determineCrossType(maleId: String, femaleId: String): CrossType {
    return when {
        maleId == "blank" -> CrossType.OPEN
        femaleId == maleId -> CrossType.SELF
        else -> CrossType.BIPARENTAL
    }
}
