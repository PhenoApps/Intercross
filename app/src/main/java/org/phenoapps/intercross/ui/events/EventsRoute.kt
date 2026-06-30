package org.phenoapps.intercross.ui.events

import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.platform.LocalContext
import androidx.lifecycle.asFlow
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.core.content.edit
import org.phenoapps.intercross.data.EventsRepository
import org.phenoapps.intercross.data.MetaValuesRepository
import org.phenoapps.intercross.data.MetadataRepository
import org.phenoapps.intercross.data.ParentsRepository
import org.phenoapps.intercross.data.WishlistRepository
import org.phenoapps.intercross.data.dao.EventsDao
import org.phenoapps.intercross.data.viewmodels.EventListViewModel
import org.phenoapps.intercross.data.viewmodels.MetaValuesViewModel
import org.phenoapps.intercross.data.viewmodels.MetadataViewModel
import org.phenoapps.intercross.data.viewmodels.ParentsListViewModel
import org.phenoapps.intercross.data.viewmodels.WishlistViewModel
import org.phenoapps.intercross.data.viewmodels.factory.EventsListViewModelFactory
import org.phenoapps.intercross.data.viewmodels.factory.MetaValuesViewModelFactory
import org.phenoapps.intercross.data.viewmodels.factory.MetadataViewModelFactory
import org.phenoapps.intercross.data.viewmodels.factory.ParentsListViewModelFactory
import org.phenoapps.intercross.data.viewmodels.factory.WishlistViewModelFactory
import org.phenoapps.intercross.ui.app.TopBarAction
import org.phenoapps.intercross.ui.app.TopBarState
import org.phenoapps.intercross.ui.app.BottomBarState
import org.phenoapps.intercross.ui.app.rememberDatabase
import org.phenoapps.intercross.ui.app.rememberPrefs
import org.phenoapps.intercross.ui.settings.PrinterDeviceDialog
import org.phenoapps.intercross.ui.settings.bluetoothPrinterPermissions
import org.phenoapps.intercross.ui.settings.hasBluetoothPrinterPermission
import org.phenoapps.intercross.util.CrossIdSettings
import org.phenoapps.intercross.util.BluetoothUtil
import org.phenoapps.intercross.util.ZebraPrinterUtil
import androidx.compose.ui.res.stringResource
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.ui.Modifier
import org.phenoapps.intercross.R

@Composable
fun EventsRoute(
    onOpenEvent: (Long) -> Unit,
    onOpenScanner: (Int) -> Unit,
    scannerResult: String?,
    onScannerResultConsumed: () -> Unit,
    scannerSequenceResult: List<String>? = null,
    onScannerSequenceResultConsumed: () -> Unit = {},
    onShowMessage: (String) -> Unit,
    prefillFemale: String? = null,
    prefillMale: String? = null,
    onPrefillConsumed: () -> Unit = {},
    onImportLocal: () -> Unit = {},
    onImportBrapi: () -> Unit = {},
    onExportLocal: () -> Unit = {},
    onExportBrapi: () -> Unit = {},
    bottomBarState: BottomBarState? = null,
) {
    val db = rememberDatabase()
    val eventsModel: EventListViewModel = viewModel(factory = EventsListViewModelFactory(EventsRepository.getInstance(db.eventsDao())))
    val parentsModel: ParentsListViewModel = viewModel(factory = ParentsListViewModelFactory(ParentsRepository.getInstance(db.parentsDao())))
    val wishModel: WishlistViewModel = viewModel(factory = WishlistViewModelFactory(WishlistRepository.getInstance(db.wishlistDao())))
    val metadataModel: MetadataViewModel = viewModel(factory = MetadataViewModelFactory(MetadataRepository.getInstance(db.metadataDao())))
    val metaValuesModel: MetaValuesViewModel = viewModel(factory = MetaValuesViewModelFactory(MetaValuesRepository.getInstance(db.metaValuesDao())))

    val eventsData by eventsModel.events.asFlow().collectAsStateWithLifecycle(initialValue = emptyList())
    val archivedEventsData by eventsModel.archivedEvents.asFlow().collectAsStateWithLifecycle(initialValue = emptyList())
    val parents by parentsModel.parents.asFlow().collectAsStateWithLifecycle(initialValue = emptyList())
    val metadata by metadataModel.metadata.asFlow().collectAsStateWithLifecycle(initialValue = emptyList())
    val metaValues by metaValuesModel.metaValues.asFlow().collectAsStateWithLifecycle(initialValue = emptyList())
    val wishes by wishModel.wishes.asFlow().collectAsStateWithLifecycle(initialValue = emptyList())

    val (prefs, keyUtil) = rememberPrefs()
    val crossIdSettings = remember(prefs) { CrossIdSettings.load(prefs) }

    var sortType by remember { mutableStateOf(EventSortType.DATE) }
    var showSortDialog by remember { mutableStateOf(false) }
    var showImportDialog by remember { mutableStateOf(false) }
    var showExportDialog by remember { mutableStateOf(false) }
    var selectedEventIds by remember { mutableStateOf(emptySet<Long>()) }
    var showDeleteConfirm by remember { mutableStateOf(false) }
    var showArchiveConfirm by remember { mutableStateOf(false) }
    var showUnarchiveConfirm by remember { mutableStateOf(false) }
    var showArchivedEvents by remember { mutableStateOf(false) }
    var pendingPrintRelations by remember { mutableStateOf<Array<ZebraPrinterUtil.CrossParentRelation>?>(null) }
    var showPrinterDeviceDialog by remember { mutableStateOf(false) }
    val context = LocalContext.current
    val noBluetoothPermissionText = stringResource(R.string.error_no_bluetooth_permission)
    val noEventsSelectedText = stringResource(R.string.no_events_selected)
    val savedPrinterNotFoundText = stringResource(R.string.saved_printer_not_found)

    val brapiEnabled = prefs.getBoolean(keyUtil.brapiEnabled, false)

    // ... (logic for sorting, selection, etc)

    val printerPermissionLauncher = rememberLauncherForActivityResult(ActivityResultContracts.RequestMultiplePermissions()) { granted ->
        if (granted.values.any { !it }) {
            pendingPrintRelations = null
            onShowMessage(noBluetoothPermissionText)
        } else {
            showPrinterDeviceDialog = true
        }
    }

    val displayedEvents = if (showArchivedEvents) archivedEventsData else eventsData
    val sortedEvents = remember(displayedEvents, sortType, wishes) {
        displayedEvents.sortEvents(sortType, wishes)
    }
    val visibleEventIds = remember(sortedEvents) { sortedEvents.mapNotNull { it.id }.toSet() }
    val selectedEvents = remember(sortedEvents, selectedEventIds) {
        sortedEvents.filter { event -> event.id?.let { it in selectedEventIds } == true }
    }
    val selectedCount = selectedEvents.size

    LaunchedEffect(visibleEventIds) {
        selectedEventIds = selectedEventIds intersect visibleEventIds
    }

    fun toggleSelectAll() {
        selectedEventIds = if (visibleEventIds.isNotEmpty() && selectedEventIds.containsAll(visibleEventIds)) {
            emptySet()
        } else {
            visibleEventIds
        }
    }

    fun requestPrinterDeviceFor(relations: Array<ZebraPrinterUtil.CrossParentRelation>) {
        pendingPrintRelations = relations
        if (hasBluetoothPrinterPermission(context)) {
            showPrinterDeviceDialog = true
        } else {
            printerPermissionLauncher.launch(bluetoothPrinterPermissions())
        }
    }

    fun printRelations(relations: Array<ZebraPrinterUtil.CrossParentRelation>) {
        val savedDeviceName = prefs.getString(keyUtil.printerDeviceNameKey, "").orEmpty()
        if (savedDeviceName.isNotBlank() && BluetoothUtil().print(context, relations, savedDeviceName)) {
            return
        }
        requestPrinterDeviceFor(relations)
    }

    fun printSelectedEvents() {
        if (selectedEvents.isEmpty()) {
            onShowMessage(noEventsSelectedText)
            return
        }
        val parentNames = parents.associate { it.codeId to it.name }
        val relations = selectedEvents.map { event ->
            ZebraPrinterUtil.CrossParentRelation(
                cross = event,
                parents = EventsDao.ParentData(
                    momCode = event.femaleObsUnitDbId,
                    momReadableName = parentNames[event.femaleObsUnitDbId] ?: event.femaleObsUnitDbId,
                    dadCode = event.maleObsUnitDbId,
                    dadReadableName = parentNames[event.maleObsUnitDbId] ?: event.maleObsUnitDbId,
                ),
            )
        }
        printRelations(relations.toTypedArray())
    }

    if (showPrinterDeviceDialog) {
        PrinterDeviceDialog(
            selectedDeviceName = prefs.getString(keyUtil.printerDeviceNameKey, "").orEmpty(),
            onDismiss = {
                showPrinterDeviceDialog = false
                pendingPrintRelations = null
            },
            onSelected = { name ->
                prefs.edit { putString(keyUtil.printerDeviceNameKey, name) }
                showPrinterDeviceDialog = false
                pendingPrintRelations?.let { relations ->
                    if (!BluetoothUtil().print(context, relations, name)) {
                        onShowMessage(savedPrinterNotFoundText)
                    }
                }
                pendingPrintRelations = null
            },
        )
    }

    if (showDeleteConfirm) {
        AlertDialog(
            onDismissRequest = { showDeleteConfirm = false },
            title = { Text(stringResource(R.string.delete_selected_events_title)) },
            text = { Text(stringResource(R.string.delete_selected_events_message, selectedCount)) },
            confirmButton = {
                TextButton(onClick = {
                    showDeleteConfirm = false
                    eventsModel.delete(*selectedEvents.toTypedArray())
                    selectedEventIds = emptySet()
                }) {
                    Text(stringResource(R.string.dialog_ok))
                }
            },
            dismissButton = {
                TextButton(onClick = { showDeleteConfirm = false }) {
                    Text(stringResource(R.string.dialog_cancel))
                }
            },
        )
    }

    if (showArchiveConfirm) {
        AlertDialog(
            onDismissRequest = { showArchiveConfirm = false },
            title = { Text(stringResource(R.string.archive_selected_events_title)) },
            text = { Text(stringResource(R.string.archive_selected_events_message, selectedCount)) },
            confirmButton = {
                TextButton(onClick = {
                    showArchiveConfirm = false
                    eventsModel.archiveByIds(selectedEventIds.toList())
                    selectedEventIds = emptySet()
                }) {
                    Text(stringResource(R.string.dialog_ok))
                }
            },
            dismissButton = {
                TextButton(onClick = { showArchiveConfirm = false }) {
                    Text(stringResource(R.string.dialog_cancel))
                }
            },
        )
    }

    if (showUnarchiveConfirm) {
        AlertDialog(
            onDismissRequest = { showUnarchiveConfirm = false },
            title = { Text(stringResource(R.string.unarchive_selected_events_title)) },
            text = { Text(stringResource(R.string.unarchive_selected_events_message, selectedCount)) },
            confirmButton = {
                TextButton(onClick = {
                    showUnarchiveConfirm = false
                    eventsModel.unarchiveByIds(selectedEventIds.toList())
                    selectedEventIds = emptySet()
                }) {
                    Text(stringResource(R.string.dialog_ok))
                }
            },
            dismissButton = {
                TextButton(onClick = { showUnarchiveConfirm = false }) {
                    Text(stringResource(R.string.dialog_cancel))
                }
            },
        )
    }

    val topBarState = TopBarState(
        titleRes = if (selectedCount > 0 || showArchivedEvents) null else R.string.events_fragment_label,
        title = when {
            selectedCount > 0 -> stringResource(R.string.selected_count_title, selectedCount)
            showArchivedEvents -> stringResource(R.string.archived_events_title)
            else -> null
        },
        actions = listOf(
            TopBarAction("select_all_events", R.string.SelectAllRows, iconRes = R.drawable.ic_select_all, onClick = {
                toggleSelectAll()
            }),
            TopBarAction(
                "toggle_archived_events",
                if (showArchivedEvents) R.string.view_active_events else R.string.view_archived_events,
                iconRes = if (showArchivedEvents) R.drawable.ic_visibility else R.drawable.ic_folder_lock,
                onClick = {
                    selectedEventIds = emptySet()
                    showArchivedEvents = !showArchivedEvents
                },
            ),
            TopBarAction("import", R.string.import_file, iconRes = R.drawable.ic_nv_import_white, onClick = {
                if (brapiEnabled) {
                    showImportDialog = true
                } else {
                    onImportLocal()
                }
            }),
            TopBarAction("export", R.string.export, iconRes = R.drawable.ic_export, onClick = {
                if (brapiEnabled) {
                    showExportDialog = true
                } else {
                    onExportLocal()
                }
            }),
            TopBarAction("sort", R.string.sort_by, iconRes = R.drawable.sort, onClick = { showSortDialog = true }),
        ),
    )

    if (showImportDialog) {
        AlertDialog(
            onDismissRequest = { showImportDialog = false },
            title = { Text(stringResource(R.string.import_file)) },
            text = {
                Column {
                    TextButton(
                        onClick = { showImportDialog = false; onImportLocal() },
                        modifier = Modifier.fillMaxWidth(),
                    ) {
                        Text(stringResource(R.string.import_source_local))
                    }
                    TextButton(
                        onClick = { showImportDialog = false; onImportBrapi() },
                        modifier = Modifier.fillMaxWidth(),
                    ) {
                        Text(stringResource(R.string.import_source_brapi))
                    }
                }
            },
            confirmButton = {},
        )
    }

    if (showExportDialog) {
        AlertDialog(
            onDismissRequest = { showExportDialog = false },
            title = { Text(stringResource(R.string.dialog_export_title)) },
            text = {
                Column {
                    TextButton(
                        onClick = { showExportDialog = false; onExportLocal() },
                        modifier = Modifier.fillMaxWidth(),
                    ) {
                        Text(stringResource(R.string.dialog_export_option_local))
                    }
                    TextButton(
                        onClick = { showExportDialog = false; onExportBrapi() },
                        modifier = Modifier.fillMaxWidth(),
                    ) {
                        Text(stringResource(R.string.dialog_export_option_brapi_export))
                    }
                }
            },
            confirmButton = {},
        )
    }

    EventsScreen(
        events = sortedEvents,
        crossIdSettings = crossIdSettings,
        parents = parents,
        metadata = metadata,
        metaValues = metaValues,
        wishes = wishes,
        eventsModel = eventsModel,
        parentsModel = parentsModel,
        metaValuesModel = metaValuesModel,
        onOpenEvent = onOpenEvent,
        onOpenScanner = onOpenScanner,
        scannerResult = scannerResult,
        onScannerResultConsumed = onScannerResultConsumed,
        scannerSequenceResult = scannerSequenceResult,
        onScannerSequenceResultConsumed = onScannerSequenceResultConsumed,
        onShowMessage = onShowMessage,
        prefillFemale = prefillFemale,
        prefillMale = prefillMale,
        onPrefillConsumed = onPrefillConsumed,
        topBarState = topBarState,
        bottomBarState = bottomBarState,
        showArchivedEvents = showArchivedEvents,
        selectedEventIds = selectedEventIds,
        onToggleEventSelection = { event ->
            event.id?.let { id ->
                selectedEventIds = if (id in selectedEventIds) selectedEventIds - id else selectedEventIds + id
            }
        },
        onPrintSelected = ::printSelectedEvents,
        onDeleteSelected = {
            if (selectedEvents.isEmpty()) onShowMessage(noEventsSelectedText) else showDeleteConfirm = true
        },
        onArchiveSelected = {
            if (selectedEvents.isEmpty()) onShowMessage(noEventsSelectedText) else showArchiveConfirm = true
        },
        onUnarchiveSelected = {
            if (selectedEvents.isEmpty()) onShowMessage(noEventsSelectedText) else showUnarchiveConfirm = true
        },
        sortType = sortType,
        onSortTypeChange = { sortType = it },
        showSortDialog = showSortDialog,
        onDismissSortDialog = { showSortDialog = false },
    )
}
