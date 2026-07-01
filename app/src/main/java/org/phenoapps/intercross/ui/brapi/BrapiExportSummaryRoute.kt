package org.phenoapps.intercross.ui.brapi

import android.os.Bundle
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
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
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.lifecycle.asFlow
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import com.google.gson.JsonObject
import kotlinx.coroutines.launch
import org.brapi.v2.model.BrAPIExternalReference
import org.brapi.v2.model.germ.BrAPICross
import org.brapi.v2.model.germ.BrAPICrossParent
import org.brapi.v2.model.germ.BrAPICrossType
import org.brapi.v2.model.germ.BrAPIParentType
import org.phenoapps.intercross.R
import org.phenoapps.intercross.brapi.service.BrAPIServiceV2
import org.phenoapps.intercross.data.EventsRepository
import org.phenoapps.intercross.data.ParentsRepository
import org.phenoapps.intercross.data.models.CrossType
import org.phenoapps.intercross.data.models.Event
import org.phenoapps.intercross.data.models.Parent
import org.phenoapps.intercross.data.viewmodels.EventListViewModel
import org.phenoapps.intercross.data.viewmodels.ParentsListViewModel
import org.phenoapps.intercross.data.viewmodels.factory.EventsListViewModelFactory
import org.phenoapps.intercross.data.viewmodels.factory.ParentsListViewModelFactory
import org.phenoapps.intercross.fragments.brapi.BrapiRequestException
import org.phenoapps.intercross.fragments.brapi.awaitCrossingProject
import org.phenoapps.intercross.fragments.brapi.awaitPostCrosses
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
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
import org.phenoapps.intercross.ui.components.CrossListItem
import org.phenoapps.intercross.ui.preview.PreviewSampleData
import org.phenoapps.intercross.ui.theme.AppTheme
import org.phenoapps.intercross.ui.theme.IntercrossPreviewTheme

@Composable
fun BrapiExportSummaryRoute(
    args: Bundle,
    onBack: () -> Unit,
    onDone: () -> Unit,
    onShowMessage: (String) -> Unit,
) {
    val context = LocalContext.current
    val db = rememberDatabase()
    val service = remember(context) { BrAPIServiceV2(context) }
    val eventsModel: EventListViewModel = viewModel(
        factory = EventsListViewModelFactory(EventsRepository.getInstance(db.eventsDao())),
    )
    val parentsModel: ParentsListViewModel = viewModel(
        factory = ParentsListViewModelFactory(ParentsRepository.getInstance(db.parentsDao())),
    )
    val events by eventsModel.events.asFlow().collectAsStateWithLifecycle(initialValue = emptyList())
    val parents by parentsModel.parents.asFlow().collectAsStateWithLifecycle(initialValue = emptyList())
    val scope = rememberCoroutineScope()
    val projectDbId = args.getString("crossingProjectDbId").orEmpty()
    val projectName = args.getString("crossingProjectName").orEmpty()
    var maleParentIds by remember { mutableStateOf(emptySet<String>()) }
    var femaleParentIds by remember { mutableStateOf(emptySet<String>()) }
    var isLoadingProject by remember { mutableStateOf(false) }
    var isExporting by remember { mutableStateOf(false) }
    var hasError by remember { mutableStateOf(false) }

    fun loadProject() {
        if (!canReachBrapi(context, onShowMessage)) return
        isLoadingProject = true
        hasError = false
        scope.launch {
            try {
                val project = service.awaitCrossingProject(projectDbId).firstOrNull()
                val potentialParents = project?.potentialParents ?: emptyList()
                maleParentIds = potentialParents
                    .filter { it.parentType == BrAPIParentType.MALE }
                    .mapNotNull { it.observationUnitDbId }
                    .toSet()
                femaleParentIds = potentialParents
                    .filter { it.parentType == BrAPIParentType.FEMALE }
                    .mapNotNull { it.observationUnitDbId }
                    .toSet()
            } catch (error: BrapiRequestException) {
                hasError = true
                onShowMessage("BrAPI callback failed. ${error.code}")
            } catch (error: Exception) {
                hasError = true
                onShowMessage(error.localizedMessage ?: context.getString(R.string.fragment_brapi_export_crosses_failed))
            } finally {
                isLoadingProject = false
            }
        }
    }

    LaunchedEffect(projectDbId) {
        loadProject()
    }

    val exportableEvents = remember(events, maleParentIds, femaleParentIds) {
        events.filter { it.maleObsUnitDbId in maleParentIds && it.femaleObsUnitDbId in femaleParentIds }
    }

    val topBarState = TopBarState(
        titleRes = R.string.brapi_export_summary_title,
        showBack = true,
        onBack = onBack
    )

    BrapiScaffold(topBarState = topBarState) { innerPadding ->
        BrapiExportSummaryScreen(
            modifier = Modifier.padding(innerPadding),
            args = args,
            exportableEvents = exportableEvents,
            isLoading = isLoadingProject || isExporting,
            hasError = hasError,
            onRetry = ::loadProject,
            onExport = {
                isExporting = true
                scope.launch {
                    try {
                        val brapiCrosses = exportableEvents.map { event ->
                            event.toBrapiCross(
                                projectDbId = projectDbId,
                                projectName = projectName,
                                parents = parents,
                            )
                        }
                        service.awaitPostCrosses(brapiCrosses)
                        onShowMessage(context.getString(R.string.brapi_exported_crosses, exportableEvents.size))
                        onDone()
                    } catch (error: BrapiRequestException) {
                        val message = when (error.code) {
                            403 -> context.getString(R.string.brapi_error_forbidden)
                            else -> context.getString(R.string.fragment_brapi_export_crosses_failed)
                        }
                        onShowMessage(message)
                    } catch (error: Exception) {
                        onShowMessage(error.localizedMessage ?: context.getString(R.string.fragment_brapi_export_crosses_failed))
                    } finally {
                        isExporting = false
                    }
                }
            }
        )
    }
}

@Composable
internal fun BrapiExportSummaryScreen(
    modifier: Modifier = Modifier,
    args: Bundle,
    exportableEvents: List<Event>,
    isLoading: Boolean,
    hasError: Boolean,
    onRetry: () -> Unit,
    onExport: () -> Unit,
) {
    Box(modifier) {
    BrapiDetailList(
        args = args,
        isLoading = isLoading,
        hasError = hasError,
        itemCount = exportableEvents.size,
        countLabel = stringResource(R.string.brapi_export_ready_count, exportableEvents.size),
        emptyLabel = stringResource(R.string.fragment_brapi_import_no_planned_crosses_found),
        onRetry = onRetry,
    ) {
        items(exportableEvents, key = { it.id ?: it.eventDbId }) { event ->
            CrossListItem(event = event, showQrCode = false)
        }
        item {
            Button(
                modifier = Modifier.fillMaxWidth(),
                enabled = exportableEvents.isNotEmpty() && !isLoading,
                onClick = onExport,
                colors = ButtonDefaults.buttonColors(containerColor = AppTheme.colors.accent),
            ) {
                if (isLoading) {
                    ProgressButtonLabel(text = stringResource(R.string.brapi_export_progress_title))
                } else {
                    Text(stringResource(R.string.brapi_export_crosses))
                }
            }
        }
    }
    }
}

// ─── Previews ───────────────────────────────────────────────────────────────────

@Preview(showBackground = true, name = "BrapiExportSummary - Populated")
@Composable
internal fun BrapiExportSummaryPopulatedPreview() {
    IntercrossPreviewTheme {
        val args = Bundle().apply {
            putString("crossingProjectName", "Apple Breeding 2024")
            putString("crossingProjectDescription", "Developing disease-resistant cultivars")
            putString("crossingProjectDbId", "PRJ001")
            putString("programName", "Fruit Tree Improvement")
            putString("commonCropName", "Apple")
            putInt("parentCount", 12)
        }
        BrapiExportSummaryScreen(
            args = args,
            exportableEvents = PreviewSampleData.events,
            isLoading = false,
            hasError = false,
            onRetry = {},
            onExport = {}
        )
    }
}

private fun Event.toBrapiCross(
    projectDbId: String,
    projectName: String,
    parents: List<Parent>,
): BrAPICross = BrAPICross().apply {
    externalReferences = listOf(
        BrAPIExternalReference().apply {
            referenceID = eventDbId
            referenceId = eventDbId
            referenceSource = "Intercross"
        },
    )
    additionalInfo = JsonObject()
    crossAttributes = listOf()
    crossType = type.toBrAPICrossType()
    crossingProjectDbId = projectDbId
    crossingProjectName = projectName
    parent1 = BrAPICrossParent().apply {
        observationUnitDbId = femaleObsUnitDbId
        parents.find { it.codeId == femaleObsUnitDbId }?.let { parent ->
            observationUnitName = parent.name
            parentType = if (parent.sex == 0) BrAPIParentType.FEMALE else BrAPIParentType.MALE
        }
    }
    parent2 = BrAPICrossParent().apply {
        observationUnitDbId = maleObsUnitDbId
        parents.find { it.codeId == maleObsUnitDbId }?.let { parent ->
            observationUnitName = parent.name
            parentType = if (parent.sex == 0) BrAPIParentType.FEMALE else BrAPIParentType.MALE
        }
    }
    crossName = "$femaleObsUnitDbId x $maleObsUnitDbId"
}

private fun CrossType.toBrAPICrossType(): BrAPICrossType = when (this) {
    CrossType.BIPARENTAL -> BrAPICrossType.BIPARENTAL
    CrossType.OPEN -> BrAPICrossType.OPEN_POLLINATED
    CrossType.POLY -> BrAPICrossType.BULK_OPEN_POLLINATED
    else -> BrAPICrossType.SELF
}
