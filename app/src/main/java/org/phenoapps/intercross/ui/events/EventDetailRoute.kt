package org.phenoapps.intercross.ui.events

import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.core.content.edit
import androidx.lifecycle.asFlow
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import org.phenoapps.intercross.R
import org.phenoapps.intercross.data.EventsRepository
import org.phenoapps.intercross.data.MetaValuesRepository
import org.phenoapps.intercross.data.MetadataRepository
import org.phenoapps.intercross.data.WishlistRepository
import org.phenoapps.intercross.data.dao.EventsDao
import org.phenoapps.intercross.data.viewmodels.EventDetailViewModel
import org.phenoapps.intercross.data.viewmodels.EventListViewModel
import org.phenoapps.intercross.data.viewmodels.MetaValuesViewModel
import org.phenoapps.intercross.data.viewmodels.MetadataViewModel
import org.phenoapps.intercross.data.viewmodels.WishlistViewModel
import org.phenoapps.intercross.data.viewmodels.factory.EventDetailViewModelFactory
import org.phenoapps.intercross.data.viewmodels.factory.EventsListViewModelFactory
import org.phenoapps.intercross.data.viewmodels.factory.MetaValuesViewModelFactory
import org.phenoapps.intercross.data.viewmodels.factory.MetadataViewModelFactory
import org.phenoapps.intercross.data.viewmodels.factory.WishlistViewModelFactory
import org.phenoapps.intercross.ui.app.TopBarAction
import org.phenoapps.intercross.ui.app.TopBarState
import org.phenoapps.intercross.ui.app.rememberDatabase
import org.phenoapps.intercross.ui.app.rememberPrefs
import org.phenoapps.intercross.ui.crosstracker.calculateActualProgress
import org.phenoapps.intercross.ui.settings.PrinterDeviceDialog
import org.phenoapps.intercross.ui.settings.bluetoothPrinterPermissions
import org.phenoapps.intercross.ui.settings.hasBluetoothPrinterPermission
import org.phenoapps.intercross.util.BluetoothUtil
import org.phenoapps.intercross.util.ZebraPrinterUtil

@Composable
fun EventDetailRoute(
    eventId: Long,
    onBack: () -> Unit,
    onShowMessage: (String) -> Unit,
    onNavigateToEvent: (Long) -> Unit = {},
) {
    val db = rememberDatabase()
    val (prefs, keyUtil) = rememberPrefs()
    val context = LocalContext.current
    val detailModel: EventDetailViewModel = viewModel(factory = EventDetailViewModelFactory(EventsRepository.getInstance(db.eventsDao()), eventId))
    val eventsModel: EventListViewModel = viewModel(factory = EventsListViewModelFactory(EventsRepository.getInstance(db.eventsDao())))
    val metaValuesModel: MetaValuesViewModel = viewModel(factory = MetaValuesViewModelFactory(MetaValuesRepository.getInstance(db.metaValuesDao())))
    val metadataModel: MetadataViewModel = viewModel(factory = MetadataViewModelFactory(MetadataRepository.getInstance(db.metadataDao())))
    val wishlistModel: WishlistViewModel = viewModel(factory = WishlistViewModelFactory(WishlistRepository.getInstance(db.wishlistDao())))

    val event by detailModel.event.asFlow().collectAsStateWithLifecycle(initialValue = null)
    val parents by detailModel.parents.asFlow().collectAsStateWithLifecycle(initialValue = null)
    val savedMetadata by detailModel.metadata.asFlow().collectAsStateWithLifecycle(initialValue = emptyList())
    val allEvents by eventsModel.events.asFlow().collectAsStateWithLifecycle(initialValue = emptyList())
    val metaValues by metaValuesModel.metaValues.asFlow().collectAsStateWithLifecycle(initialValue = emptyList())
    val metaList by metadataModel.metadata.asFlow().collectAsStateWithLifecycle(initialValue = emptyList())
    val wishesSource = if (prefs.getBoolean(keyUtil.commutativeCrossingKey, false)) wishlistModel.commutativeWishes else wishlistModel.wishes
    val allWishesData by wishesSource.asFlow().collectAsStateWithLifecycle(initialValue = emptyList())

    val correctedWishes = remember(allWishesData, allEvents, metaList, metaValues) {
        allWishesData.calculateActualProgress(
            events = allEvents,
            metadata = metaList,
            metaValues = metaValues,
            commutative = prefs.getBoolean(keyUtil.commutativeCrossingKey, false)
        )
    }

    // Merge metadata definitions with saved values (same logic as old fragment's refreshMetadata)
    val eid = event?.id?.toInt() ?: -1
    val metadata = remember(metaList, savedMetadata, metaValues, eid) {
        metaList.map { meta ->
            val savedValue = savedMetadata.find { it.property == meta.property && it.eid == eid }
            if (savedValue != null) {
                savedValue
            } else {
                // Check metaValues directly for any value for this event + property
                val metaId = meta.id?.toInt() ?: -1
                val directValue = metaValues.find { it.eid == eid && it.metaId == metaId }
                org.phenoapps.intercross.data.dao.EventsDao.CrossMetadataWithDefaults(
                    eid = eid,
                    property = meta.property,
                    value = directValue?.value,
                    defaultValue = meta.defaultValue ?: 0,
                )
            }
        }.sortedBy { it.property }
    }

    // Metadata toggle state
    var showMetadataInputs by remember {
        mutableStateOf(prefs.getBoolean(keyUtil.collectAdditionalInfoKey, false))
    }
    var showDeleteConfirm by remember { mutableStateOf(false) }
    var pendingPrintRelation by remember { mutableStateOf<ZebraPrinterUtil.CrossParentRelation?>(null) }
    var showPrinterDeviceDialog by remember { mutableStateOf(false) }
    val noBluetoothPermissionText = stringResource(R.string.error_no_bluetooth_permission)
    val savedPrinterNotFoundText = stringResource(R.string.saved_printer_not_found)

    val printerPermissionLauncher = rememberLauncherForActivityResult(ActivityResultContracts.RequestMultiplePermissions()) { granted ->
        if (granted.values.any { !it }) {
            pendingPrintRelation = null
            onShowMessage(noBluetoothPermissionText)
        } else {
            showPrinterDeviceDialog = true
        }
    }

    fun printRelation(relation: ZebraPrinterUtil.CrossParentRelation) {
        val savedDeviceName = prefs.getString(keyUtil.printerDeviceNameKey, "").orEmpty()
        if (savedDeviceName.isNotBlank() && BluetoothUtil().print(context, arrayOf(relation), savedDeviceName)) {
            return
        }
        pendingPrintRelation = relation
        if (hasBluetoothPrinterPermission(context)) {
            showPrinterDeviceDialog = true
        } else {
            printerPermissionLauncher.launch(bluetoothPrinterPermissions())
        }
    }

    if (showPrinterDeviceDialog) {
        PrinterDeviceDialog(
            selectedDeviceName = prefs.getString(keyUtil.printerDeviceNameKey, "").orEmpty(),
            onDismiss = {
                showPrinterDeviceDialog = false
                pendingPrintRelation = null
            },
            onSelected = { name ->
                prefs.edit { putString(keyUtil.printerDeviceNameKey, name) }
                showPrinterDeviceDialog = false
                pendingPrintRelation?.let { relation ->
                    if (!BluetoothUtil().print(context, arrayOf(relation), name)) {
                        onShowMessage(savedPrinterNotFoundText)
                    }
                }
                pendingPrintRelation = null
            },
        )
    }

    if (showDeleteConfirm) {
        AlertDialog(
            onDismissRequest = { showDeleteConfirm = false },
            title = { Text(stringResource(R.string.delete_cross_entry_title)) },
            text = { Text(stringResource(R.string.dialog_confirm)) },
            confirmButton = {
                TextButton(onClick = {
                    showDeleteConfirm = false
                    event?.id?.let { id ->
                        eventsModel.deleteById(id)
                        onBack()
                    }
                }) { Text(stringResource(android.R.string.ok)) }
            },
            dismissButton = {
                TextButton(onClick = { showDeleteConfirm = false }) {
                    Text(stringResource(R.string.dialog_cancel))
                }
            },
        )
    }

    val topBarState = TopBarState(
        titleRes = R.string.event_detail_label,
        showBack = true,
        onBack = onBack,
        actions = listOf(
            TopBarAction("print_event", R.string.print, iconRes = R.drawable.ic_cross_print, onClick = {
                event?.let { current ->
                    val parentData = EventsDao.ParentData(
                        momCode = current.femaleObsUnitDbId,
                        momReadableName = parents?.momReadableName ?: current.femaleObsUnitDbId,
                        dadCode = current.maleObsUnitDbId,
                        dadReadableName = parents?.dadReadableName ?: current.maleObsUnitDbId,
                    )
                    printRelation(ZebraPrinterUtil.CrossParentRelation(cross = current, parents = parentData))
                }
            }),
            TopBarAction("delete_event", R.string.delete, iconRes = R.drawable.ic_menu_delete, onClick = {
                showDeleteConfirm = true
            }),
        ),
    )

    EventDetailScreen(
        event = event,
        parents = parents,
        metadata = metadata,
        metaList = metaList,
        metaValues = metaValues,
        wishes = correctedWishes,
        allEvents = allEvents,
        showMetadataInputs = showMetadataInputs,
        onShowMessage = onShowMessage,
        onSaveMetadata = { mv ->
            if (mv.id != null) metaValuesModel.update(mv) else metaValuesModel.insert(mv)
        },
        onNavigateToEvent = onNavigateToEvent,
        topBarState = topBarState,
    )
}
