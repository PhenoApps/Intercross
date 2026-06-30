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
import androidx.compose.material3.Checkbox
import androidx.compose.material3.CheckboxDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.pluralStringResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.google.gson.Gson
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import org.brapi.v2.model.germ.BrAPICrossParent
import org.brapi.v2.model.germ.BrAPIParentType
import org.brapi.v2.model.germ.BrAPIPlannedCross
import org.phenoapps.intercross.R
import org.phenoapps.intercross.brapi.service.BrAPIServiceV2
import org.phenoapps.intercross.brapi.service.BrapiPaginationManager
import org.phenoapps.intercross.data.ParentsRepository
import org.phenoapps.intercross.data.WishlistRepository
import org.phenoapps.intercross.data.models.Parent
import org.phenoapps.intercross.data.models.Wishlist
import org.phenoapps.intercross.fragments.brapi.BrapiRequestException
import org.phenoapps.intercross.fragments.brapi.awaitPlannedCrosses
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.ui.res.painterResource
import org.phenoapps.intercross.ui.app.TopBarState
import org.phenoapps.intercross.ui.app.rememberDatabase
import org.phenoapps.intercross.data.models.Event
import org.phenoapps.intercross.ui.components.CrossListItem
import org.phenoapps.intercross.ui.theme.AppTheme

import org.phenoapps.intercross.ui.preview.PreviewSampleData
import org.phenoapps.intercross.ui.theme.IntercrossPreviewTheme
import androidx.compose.ui.tooling.preview.Preview

@Composable
fun BrapiPlannedCrossesRoute(
    args: Bundle,
    onBack: () -> Unit,
    onDone: () -> Unit,
    onShowMessage: (String) -> Unit,
) {
    val context = LocalContext.current
    val db = rememberDatabase()
    val service = remember(context) { BrAPIServiceV2(context) }
    val wishlistRepository = remember(db) { WishlistRepository.getInstance(db.wishlistDao()) }
    val parentsRepository = remember(db) { ParentsRepository.getInstance(db.parentsDao()) }
    val scope = rememberCoroutineScope()
    val projectDbId = args.getString("crossingProjectDbId").orEmpty()
    var plannedCrosses by remember(args) { mutableStateOf(emptyList<BrAPIPlannedCross>()) }
    var importParents by remember { mutableStateOf(true) }
    var isLoading by remember { mutableStateOf(false) }
    var isImporting by remember { mutableStateOf(false) }
    var importProgress by remember { mutableIntStateOf(0) }
    var hasError by remember { mutableStateOf(false) }

    val loadErrorText = stringResource(R.string.brapi_planned_crosses_load_error)
    val noCrossesFoundText = stringResource(R.string.fragment_brapi_import_no_planned_crosses_found)

    fun load() {
        if (!canReachBrapi(context, onShowMessage)) return
        isLoading = true
        hasError = false
        plannedCrosses = emptyList()
        scope.launch {
            try {
                val pagination = BrapiPaginationManager(context)
                pagination.reset()
                plannedCrosses = service.awaitPlannedCrosses(projectDbId, pagination)
                    .filter { it.crossingProjectDbId == projectDbId }
            } catch (error: BrapiRequestException) {
                hasError = true
                onShowMessage("BrAPI callback failed. ${error.code}")
            } catch (error: Exception) {
                hasError = true
                onShowMessage(error.localizedMessage ?: loadErrorText)
            } finally {
                isLoading = false
            }
        }
    }

    LaunchedEffect(projectDbId) {
        load()
    }

    val topBarState = TopBarState(
        titleRes = R.string.brapi_planned_crosses_title,
        showBack = true,
        onBack = onBack
    )

    BrapiScaffold(topBarState = topBarState) { innerPadding ->
        BrapiPlannedCrossesScreen(
            modifier = Modifier.padding(innerPadding),
            args = args,
            plannedCrosses = plannedCrosses,
            importParents = importParents,
            onImportParentsChange = { importParents = it },
            isLoading = isLoading,
            isImporting = isImporting,
            importProgress = importProgress,
            hasError = hasError,
            onRetry = ::load,
            onImport = {
                if (plannedCrosses.isEmpty()) {
                    onShowMessage(noCrossesFoundText)
                    return@BrapiPlannedCrossesScreen
                }
                isImporting = true
                importProgress = 0
                scope.launch {
                    val total = plannedCrosses.size
                    try {
                        plannedCrosses.forEach { plannedCross ->
                            val row = plannedCross.toPlannedCrossRow()
                            withContext(Dispatchers.IO) {
                                wishlistRepository.insert(
                                    Wishlist(
                                        femaleDbId = row.femaleId,
                                        maleDbId = row.maleId,
                                        femaleName = row.femaleName,
                                        maleName = row.maleName,
                                        wishType = row.wishType,
                                        wishMin = row.wishMin,
                                        wishMax = row.wishMax,
                                    ),
                                )
                                if (importParents) {
                                    val maleParent = Parent(codeId = row.maleId, sex = 1).also { it.name = row.maleName }
                                    val femaleParent = Parent(codeId = row.femaleId, sex = 0).also { it.name = row.femaleName }
                                    parentsRepository.insertIgnore(maleParent, femaleParent)
                                    parentsRepository.updateName(maleParent, femaleParent)
                                }
                            }
                            importProgress += 1
                        }
                        onShowMessage(context.getString(R.string.brapi_imported_planned_crosses, total))
                        onDone()
                    } catch (error: Exception) {
                        onShowMessage(error.localizedMessage ?: loadErrorText)
                    } finally {
                        isImporting = false
                    }
                }
            }
        )
    }
}

@Composable
internal fun BrapiPlannedCrossesScreen(
    modifier: Modifier = Modifier,
    args: Bundle,
    plannedCrosses: List<BrAPIPlannedCross>,
    importParents: Boolean,
    onImportParentsChange: (Boolean) -> Unit,
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
        itemCount = plannedCrosses.size,
        countLabel = pluralStringResource(R.plurals.brapi_planned_cross_count, plannedCrosses.size, plannedCrosses.size),
        emptyLabel = stringResource(R.string.fragment_brapi_import_no_planned_crosses_found),
        onRetry = onRetry,
    ) {
        item {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Checkbox(
                    checked = importParents,
                    enabled = !isImporting,
                    onCheckedChange = onImportParentsChange,
                    colors = CheckboxDefaults.colors(checkedColor = AppTheme.colors.primary)
                )
                Text(stringResource(R.string.brapi_import_parents_option), color = AppTheme.colors.text.primary)
            }
        }
        items(plannedCrosses, key = { it.plannedCrossDbId ?: it.hashCode().toString() }) { plannedCross ->
            val row = remember(plannedCross) { plannedCross.toPlannedCrossRow() }
            CrossListItem(
                event = Event(
                    eventDbId = row.plannedCrossId,
                    femaleObsUnitDbId = row.femaleName,
                    maleObsUnitDbId = row.maleName,
                    readableName = row.title,
                    timestamp = "${row.wishType}: ${row.wishMin}-${row.wishMax}",
                    person = "",
                    experiment = "",
                ),
                showQrCode = false
            )
        }
        item {
            Button(
                modifier = Modifier.fillMaxWidth(),
                enabled = plannedCrosses.isNotEmpty() && !isLoading && !isImporting,
                onClick = onImport,
                colors = ButtonDefaults.buttonColors(containerColor = AppTheme.colors.accent),
            ) {
                if (isImporting) {
                    ProgressButtonLabel(text = stringResource(R.string.brapi_import_progress, importProgress, plannedCrosses.size))
                } else {
                    Text(stringResource(R.string.brapi_import_all_crosses))
                }
            }
        }
    }
    }
}

// ─── Previews ───────────────────────────────────────────────────────────────────

@Preview(showBackground = true, name = "BrapiPlannedCrosses - Populated")
@Composable
private fun BrapiPlannedCrossesPopulatedPreview() {
    IntercrossPreviewTheme {
        val args = Bundle().apply {
            putString("crossingProjectName", "Apple Breeding 2024")
            putString("crossingProjectDescription", "Developing disease-resistant cultivars")
            putString("crossingProjectDbId", "PRJ001")
            putString("programName", "Fruit Tree Improvement")
            putString("commonCropName", "Apple")
            putInt("parentCount", 12)
        }
        BrapiPlannedCrossesScreen(
            args = args,
            plannedCrosses = PreviewSampleData.brapiPlannedCrosses,
            importParents = true,
            onImportParentsChange = {},
            isLoading = false,
            isImporting = false,
            importProgress = 0,
            hasError = false,
            onRetry = {},
            onImport = {}
        )
    }
}

private data class PlannedCrossRow(
    val stableId: String,
    val title: String,
    val plannedCrossId: String,
    val maleName: String,
    val maleId: String,
    val femaleName: String,
    val femaleId: String,
    val wishType: String,
    val wishMin: Int,
    val wishMax: Int,
    val note: String,
)

private data class WishMetadata(
    val wishType: String,
    val wishMin: Int,
    val wishMax: Int,
)

private fun BrAPIPlannedCross.toPlannedCrossRow(): PlannedCrossRow {
    val orderedParents = orderedParents(parent1 ?: BrAPICrossParent(), parent2 ?: BrAPICrossParent())
    val metadata = parseWishMetadata(additionalInfo)
    val femaleName = orderedParents.second.observationUnitName ?: orderedParents.second.observationUnitDbId.orUnavailableText()
    val maleName = orderedParents.first.observationUnitName ?: orderedParents.first.observationUnitDbId.orUnavailableText()
    val title = plannedCrossName?.ifBlank { "$femaleName x $maleName" } ?: "$femaleName x $maleName"
    val rawNote = additionalInfo?.toString().orEmpty()
    return PlannedCrossRow(
        stableId = plannedCrossDbId ?: title,
        title = title,
        plannedCrossId = plannedCrossDbId ?: "",
        maleName = maleName,
        maleId = orderedParents.first.observationUnitDbId ?: "",
        femaleName = femaleName,
        femaleId = orderedParents.second.observationUnitDbId ?: "",
        wishType = metadata.wishType,
        wishMin = metadata.wishMin,
        wishMax = metadata.wishMax,
        note = formatRawJson(rawNote),
    )
}

private fun orderedParents(parent1: BrAPICrossParent, parent2: BrAPICrossParent): Pair<BrAPICrossParent, BrAPICrossParent> {
    return if (parent1.parentType == BrAPIParentType.MALE || parent1.parentType?.toString() == "MALE") {
        parent1 to parent2
    } else {
        parent2 to parent1
    }
}

private fun parseWishMetadata(additionalInfo: Any?): WishMetadata {
    val map = runCatching {
        Gson().fromJson(additionalInfo?.toString().orEmpty(), HashMap::class.java)
    }.getOrNull()
    return WishMetadata(
        wishType = map?.get("wishType")?.toString()?.ifBlank { "cross" } ?: "cross",
        wishMin = map?.get("wishMin")?.toString()?.toIntOrNull() ?: 1,
        wishMax = map?.get("wishMax")?.toString()?.toIntOrNull() ?: 10,
    )
}
