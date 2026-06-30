package org.phenoapps.intercross.ui.parents

import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.Alignment
import androidx.compose.ui.platform.LocalContext
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.ui.unit.dp
import androidx.compose.material3.AlertDialog
import androidx.compose.ui.res.stringResource
import androidx.compose.material3.RadioButton
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.core.content.edit
import androidx.lifecycle.asFlow
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import org.phenoapps.intercross.R
import org.phenoapps.intercross.data.EventsRepository
import org.phenoapps.intercross.data.ParentsRepository
import org.phenoapps.intercross.data.PollenGroupRepository
import org.phenoapps.intercross.data.models.Parent
import org.phenoapps.intercross.data.viewmodels.EventListViewModel
import org.phenoapps.intercross.data.viewmodels.ParentsListViewModel
import org.phenoapps.intercross.data.viewmodels.PollenGroupListViewModel
import org.phenoapps.intercross.data.viewmodels.factory.EventsListViewModelFactory
import org.phenoapps.intercross.data.viewmodels.factory.ParentsListViewModelFactory
import org.phenoapps.intercross.data.viewmodels.factory.PollenGroupListViewModelFactory
import org.phenoapps.intercross.ui.app.TopBarAction
import org.phenoapps.intercross.ui.app.TopBarState
import org.phenoapps.intercross.ui.app.BottomBarState
import org.phenoapps.intercross.ui.app.rememberDatabase
import org.phenoapps.intercross.ui.app.rememberPrefs
import org.phenoapps.intercross.ui.settings.PrinterDeviceDialog
import org.phenoapps.intercross.ui.settings.bluetoothPrinterPermissions
import org.phenoapps.intercross.ui.settings.hasBluetoothPrinterPermission
import org.phenoapps.intercross.util.BluetoothUtil

@Composable
fun ParentsRoute(
    bottomBarState: BottomBarState? = null,
    onCreateParent: (Int) -> Unit,
    onShowMessage: (String) -> Unit,
    onImportFile: () -> Unit = {},
    onImportBrapi: () -> Unit = {},
) {
    val db = rememberDatabase()
    val (prefs, keyUtil) = rememberPrefs()
    val parentsModel: ParentsListViewModel = viewModel(factory = ParentsListViewModelFactory(ParentsRepository.getInstance(db.parentsDao())))
    val groupModel: PollenGroupListViewModel = viewModel(factory = PollenGroupListViewModelFactory(PollenGroupRepository.getInstance(db.pollenGroupDao())))
    val eventsModel: EventListViewModel = viewModel(factory = EventsListViewModelFactory(EventsRepository.getInstance(db.eventsDao())))
    val parents by parentsModel.parents.asFlow().collectAsStateWithLifecycle(initialValue = emptyList())
    val groups by groupModel.groups.asFlow().collectAsStateWithLifecycle(initialValue = emptyList())
    val events by eventsModel.events.asFlow().collectAsStateWithLifecycle(initialValue = emptyList())
    var tab by remember { mutableIntStateOf(0) }
    var sortType by remember { mutableStateOf(ParentSortType.NAME) }

    // Local selection state for immediate reactivity
    var localSelection by remember { mutableStateOf(mapOf<String, Boolean>()) }
    var showImportDialog by remember { mutableStateOf(false) }
    var showSexDialog by remember { mutableStateOf(false) }
    var showSortDialog by remember { mutableStateOf(false) }
    var pendingParentPrint by remember { mutableStateOf<Array<Parent>?>(null) }
    var showPrinterDeviceDialog by remember { mutableStateOf(false) }

    // Build rows using local selection override for immediate UI updates
    val rows = remember(parents, groups, events, tab, sortType, localSelection) {
        buildParentRows(parents, groups, events, tab, sortType).map { row ->
            val overrideSelected = localSelection[row.codeId]
            if (overrideSelected != null) row.copy(selected = overrideSelected) else row
        }
    }

    val selectedCount = rows.count { it.selected }

    val topBarState = if (selectedCount > 0) {
        TopBarState(
            title = stringResource(R.string.selected_count, selectedCount),
            actions = listOf(
                TopBarAction("select_all_parents", R.string.sort_by, iconRes = R.drawable.ic_select_all, onClick = {
                    val allSelected = rows.isNotEmpty() && rows.all { it.selected }
                    if (allSelected) {
                        val allCodes = rows.map { it.codeId to false }.toMap()
                        localSelection = localSelection + allCodes
                        parents.filter { it.selected }.forEach { parent ->
                            parent.selected = false
                            parentsModel.update(parent)
                        }
                        groups.filter { it.selected }.forEach { group ->
                            group.selected = false
                            groupModel.update(group)
                        }
                    } else {
                        val allCodes = rows.map { it.codeId to true }.toMap()
                        localSelection = localSelection + allCodes
                        parents.filter { !it.selected }.forEach { parent ->
                            parent.selected = true
                            parentsModel.update(parent)
                        }
                        groups.filter { !it.selected }.forEach { group ->
                            group.selected = true
                            groupModel.update(group)
                        }
                    }
                }),
                TopBarAction("import_parents", R.string.import_file, iconRes = R.drawable.ic_nv_import_white, onClick = {
                    showImportDialog = true
                }),
                TopBarAction("sort_parents", R.string.sort_by, iconRes = R.drawable.sort, onClick = {
                    showSortDialog = true
                }),
            ),
        )
    } else {
        TopBarState(
            title = "",
            actions = listOf(
                TopBarAction("select_all_parents", R.string.sort_by, iconRes = R.drawable.ic_select_all, onClick = {
                    val allSelected = rows.isNotEmpty() && rows.all { it.selected }
                    if (allSelected) {
                        val allCodes = rows.map { it.codeId to false }.toMap()
                        localSelection = localSelection + allCodes
                        parents.filter { it.selected }.forEach { parent ->
                            parent.selected = false
                            parentsModel.update(parent)
                        }
                        groups.filter { it.selected }.forEach { group ->
                            group.selected = false
                            groupModel.update(group)
                        }
                    } else {
                        val allCodes = rows.map { it.codeId to true }.toMap()
                        localSelection = localSelection + allCodes
                        parents.filter { !it.selected }.forEach { parent ->
                            parent.selected = true
                            parentsModel.update(parent)
                        }
                        groups.filter { !it.selected }.forEach { group ->
                            group.selected = true
                            groupModel.update(group)
                        }
                    }
                }),
                TopBarAction("import_parents", R.string.import_file, iconRes = R.drawable.ic_nv_import_white, onClick = {
                    showImportDialog = true
                }),
                TopBarAction("sort_parents", R.string.sort_by, iconRes = R.drawable.sort, onClick = {
                    showSortDialog = true
                }),
            ),
        )
    }

    val noBluetoothPermissionText = stringResource(R.string.error_no_bluetooth_permission)
    val savedPrinterNotFoundText = stringResource(R.string.saved_printer_not_found)
    val selectAllRowsText = stringResource(R.string.SelectAllRows)
    val notDeletedReasonText = stringResource(R.string.frag_parents_parents_not_deleted_reason)

    val context = LocalContext.current
    var showDeleteConfirm by remember { mutableStateOf(false) }
    val printerPermissionLauncher = rememberLauncherForActivityResult(ActivityResultContracts.RequestMultiplePermissions()) { granted ->
        if (granted.values.any { !it }) {
            pendingParentPrint = null
            onShowMessage(noBluetoothPermissionText)
        } else {
            showPrinterDeviceDialog = true
        }
    }

    fun requestPrinterDeviceFor(selectedParents: Array<Parent>) {
        pendingParentPrint = selectedParents
        if (hasBluetoothPrinterPermission(context)) {
            showPrinterDeviceDialog = true
        } else {
            printerPermissionLauncher.launch(bluetoothPrinterPermissions())
        }
    }

    fun printParents(selectedParents: Array<Parent>) {
        val savedDeviceName = prefs.getString(keyUtil.printerDeviceNameKey, "").orEmpty()
        if (savedDeviceName.isNotBlank() && BluetoothUtil().print(context, selectedParents, savedDeviceName)) {
            return
        }
        requestPrinterDeviceFor(selectedParents)
    }

    if (showPrinterDeviceDialog) {
        PrinterDeviceDialog(
            selectedDeviceName = prefs.getString(keyUtil.printerDeviceNameKey, "").orEmpty(),
            onDismiss = {
                showPrinterDeviceDialog = false
                pendingParentPrint = null
            },
            onSelected = { name ->
                prefs.edit { putString(keyUtil.printerDeviceNameKey, name) }
                showPrinterDeviceDialog = false
                pendingParentPrint?.let { selectedParents ->
                    if (!BluetoothUtil().print(context, selectedParents, name)) {
                        onShowMessage(savedPrinterNotFoundText)
                    }
                }
                pendingParentPrint = null
            },
        )
    }

    // Delete confirmation dialog
    if (showDeleteConfirm) {
        AlertDialog(
            onDismissRequest = { showDeleteConfirm = false },
            title = { Text(stringResource(R.string.delete)) },
            text = {
                val count = rows.count { it.selected }
                Text(stringResource(R.string.delete_selected_parents, count))
            },
            confirmButton = {
                TextButton(onClick = {
                    showDeleteConfirm = false
                    val selected = rows.filter { it.selected }
                    val (used, deletable) = selected.partition { row ->
                        events.any { it.femaleObsUnitDbId == row.codeId || it.maleObsUnitDbId == row.codeId }
                    }
                    val parentCodes = deletable.filterNot { it.isGroup }.map { it.codeId }.toSet()
                    val groupCodes = deletable.filter { it.isGroup }.map { it.codeId }
                    parents.filter { it.codeId in parentCodes }.takeIf { it.isNotEmpty() }?.let {
                        parentsModel.delete(*it.toTypedArray())
                    }
                    if (groupCodes.isNotEmpty()) {
                        groupModel.deleteByCode(groupCodes)
                    }
                    localSelection = localSelection - parentCodes - groupCodes.toSet()
                    if (used.isNotEmpty()) {
                        onShowMessage(notDeletedReasonText)
                    }
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

    // Sex selection dialog (shown when on ALL tab and FAB tapped)
    if (showSexDialog) {
        AlertDialog(
            onDismissRequest = { showSexDialog = false },
            title = { Text(stringResource(R.string.frag_parents_new_parent_title)) },
            text = {
                Column {
                    TextButton(
                        onClick = { showSexDialog = false; onCreateParent(0) },
                        modifier = Modifier.fillMaxWidth(),
                    ) {
                        Text(stringResource(R.string.female))
                    }
                    TextButton(
                        onClick = { showSexDialog = false; onCreateParent(1) },
                        modifier = Modifier.fillMaxWidth(),
                    ) {
                        Text(stringResource(R.string.male))
                    }
                }
            },
            confirmButton = {},
        )
    }

    // Sort dialog
    if (showSortDialog) {
        AlertDialog(
            onDismissRequest = { showSortDialog = false },
            title = { Text(stringResource(R.string.sort_by)) },
            text = {
                Column {
                    ParentSortType.entries.forEach { option ->
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clickable {
                                    sortType = option
                                    showSortDialog = false
                                }
                                .padding(vertical = 12.dp),
                            verticalAlignment = Alignment.CenterVertically,
                        ) {
                            RadioButton(
                                selected = sortType == option,
                                onClick = {
                                    sortType = option
                                    showSortDialog = false
                                },
                            )
                            Text(
                                text = option.name.lowercase().replaceFirstChar { it.uppercase() },
                                modifier = Modifier.padding(start = 8.dp),
                            )
                        }
                    }
                }
            },
            confirmButton = {},
        )
    }

    // Import source dialog
    if (showImportDialog) {
        AlertDialog(
            onDismissRequest = { showImportDialog = false },
            title = { Text(stringResource(R.string.import_file)) },
            text = {
                Column {
                    TextButton(
                        onClick = { showImportDialog = false; onImportFile() },
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

    ParentsScreen(
        rows = rows,
        tab = tab,
        sortType = sortType,
        onTabChange = { tab = it },
        onSortTypeChange = { sortType = ParentSortType.entries[(sortType.ordinal + 1) % ParentSortType.entries.size] },
        onCreateParent = { sex ->
            if (tab == 0) {
                // ALL tab: show sex selection dialog
                showSexDialog = true
            } else {
                onCreateParent(sex)
            }
        },
        onDeleteSelected = {
            val selected = rows.filter { it.selected }
            if (selected.isEmpty()) {
                onShowMessage(selectAllRowsText)
            } else {
                showDeleteConfirm = true
            }
        },
        onToggleSelection = { row ->
            val newState = !(localSelection[row.codeId] ?: row.selected)
            localSelection = localSelection + (row.codeId to newState)
            // Also persist to DB
            if (row.isGroup) {
                groups.find { it.codeId == row.codeId }?.let { g ->
                    g.selected = newState
                    groupModel.update(g)
                }
            } else {
                parents.find { it.codeId == row.codeId }?.let { p ->
                    p.selected = newState
                    parentsModel.update(p)
                }
            }
        },
        onShowMessage = onShowMessage,
        onSelectAll = {
            // Toggle: if all are selected, deselect all; otherwise select all
            val allSelected = rows.all { it.selected }
            if (allSelected) {
                val allCodes = rows.map { it.codeId to false }.toMap()
                localSelection = localSelection + allCodes
                parents.filter { it.selected }.forEach { parent ->
                    parent.selected = false
                    parentsModel.update(parent)
                }
                groups.filter { it.selected }.forEach { group ->
                    group.selected = false
                    groupModel.update(group)
                }
            } else {
                val allCodes = rows.map { it.codeId to true }.toMap()
                localSelection = localSelection + allCodes
                parents.filter { !it.selected }.forEach { parent ->
                    parent.selected = true
                    parentsModel.update(parent)
                }
                groups.filter { !it.selected }.forEach { group ->
                    group.selected = true
                    groupModel.update(group)
                }
            }
        },
        onPrintSelected = {
            val selectedParents = rows
                .filter { it.selected && !it.isGroup }
                .mapNotNull { row -> parents.find { it.codeId == row.codeId } }
                .toTypedArray()
            if (selectedParents.isEmpty()) {
                onShowMessage(selectAllRowsText)
            } else {
                printParents(selectedParents)
            }
        },
        topBarState = topBarState,
        bottomBarState = bottomBarState,
    )
}
