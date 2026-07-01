package org.phenoapps.intercross.ui.events

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.clickable
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Print
import androidx.compose.material.icons.filled.Add
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ElevatedCard
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ExposedDropdownMenuBox
import androidx.compose.material3.ExposedDropdownMenuDefaults
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItemDefaults
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.rememberModalBottomSheetState
import com.google.mlkit.vision.barcode.common.Barcode
import org.phenoapps.intercross.ui.barcode.BarcodeScannerScreen
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.RadioButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SmallFloatingActionButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.core.content.edit
import androidx.fragment.app.FragmentActivity
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import org.phenoapps.intercross.R
import org.phenoapps.intercross.data.models.Event
import org.phenoapps.intercross.data.models.Meta
import org.phenoapps.intercross.data.models.MetadataValues
import org.phenoapps.intercross.data.models.Parent
import org.phenoapps.intercross.data.models.WishlistView
import org.phenoapps.intercross.data.viewmodels.EventListViewModel
import org.phenoapps.intercross.data.viewmodels.MetaValuesViewModel
import org.phenoapps.intercross.data.viewmodels.ParentsListViewModel
import org.phenoapps.intercross.ui.app.BottomBarState
import org.phenoapps.intercross.ui.app.BottomDestinations
import org.phenoapps.intercross.ui.app.TopBarAction
import org.phenoapps.intercross.ui.app.TopBarState
import org.phenoapps.intercross.ui.app.rememberPrefs
import org.phenoapps.intercross.ui.barcode.BARCODE_MODE_CONTINUOUS
import org.phenoapps.intercross.ui.barcode.BARCODE_MODE_SEARCH
import org.phenoapps.intercross.ui.barcode.BARCODE_MODE_SINGLE
import org.phenoapps.intercross.ui.components.CrossListItem
import org.phenoapps.intercross.ui.components.SafeIcon
import org.phenoapps.intercross.ui.preview.PreviewSampleData
import org.phenoapps.intercross.ui.theme.AppTheme
import org.phenoapps.intercross.ui.theme.IntercrossPreviewTheme
import org.phenoapps.intercross.util.CrossIdSettings
import org.phenoapps.intercross.util.CrossUtil
import java.util.UUID

@OptIn(ExperimentalMaterial3Api::class)
@Composable
internal fun EventsScreen(
    events: List<Event>,
    crossIdSettings: CrossIdSettings,
    parents: List<Parent>,
    metadata: List<Meta>,
    metaValues: List<MetadataValues>,
    wishes: List<WishlistView>,
    eventsModel: EventListViewModel,
    parentsModel: ParentsListViewModel,
    metaValuesModel: MetaValuesViewModel,
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
    topBarState: TopBarState? = null,
    bottomBarState: BottomBarState? = null,
    showArchivedEvents: Boolean = false,
    selectedEventIds: Set<Long> = emptySet(),
    onToggleEventSelection: (Event) -> Unit = {},
    onPrintSelected: () -> Unit = {},
    onDeleteSelected: () -> Unit = {},
    onArchiveSelected: () -> Unit = {},
    onUnarchiveSelected: () -> Unit = {},
    sortType: EventSortType = EventSortType.DATE,
    onSortTypeChange: (EventSortType) -> Unit = {},
    showSortDialog: Boolean = false,
    onDismissSortDialog: () -> Unit = {},
) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    val (prefs, keyUtil) = rememberPrefs()
    var firstParent by remember { mutableStateOf("") }
    var secondParent by remember { mutableStateOf("") }
    var crossId by remember { mutableStateOf("") }
    var scannerTargetField by remember { mutableStateOf<Int?>(null) }
    var showInlineScanner by remember { mutableStateOf(false) }

    val focusFirst = remember { FocusRequester() }
    val focusSecond = remember { FocusRequester() }
    val focusCrossId = remember { FocusRequester() }

    if (showSortDialog) {
        AlertDialog(
            onDismissRequest = onDismissSortDialog,
            title = { Text(stringResource(R.string.sort_by)) },
            text = {
                Column {
                    EventSortType.entries.forEach { option ->
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clickable {
                                    onSortTypeChange(option)
                                    onDismissSortDialog()
                                }
                                .padding(vertical = 12.dp),
                            verticalAlignment = Alignment.CenterVertically,
                        ) {
                            RadioButton(
                                selected = sortType == option,
                                onClick = {
                                    onSortTypeChange(option)
                                    onDismissSortDialog()
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

    LaunchedEffect(crossIdSettings.isUUID, crossIdSettings.isPattern, crossIdSettings.pattern) {
        crossId = when {
            crossIdSettings.isPattern -> crossIdSettings.pattern
            crossIdSettings.isUUID -> UUID.randomUUID().toString()
            else -> crossId
        }
    }

    val maleFirst = prefs.getBoolean(keyUtil.crossOrderKey, false)
    val blankMale = prefs.getBoolean(keyUtil.blankMaleKey, false)
    val firstLabel = if (maleFirst) stringResource(R.string.MaleID) else stringResource(R.string.FemaleID)
    val secondLabel = if (maleFirst) stringResource(R.string.FemaleID) else stringResource(R.string.MaleID)

    LaunchedEffect(prefillFemale, prefillMale) {
        if (!prefillFemale.isNullOrBlank() || !prefillMale.isNullOrBlank()) {
            if (maleFirst) {
                firstParent = prefillMale.orEmpty()
                secondParent = prefillFemale.orEmpty()
            } else {
                firstParent = prefillFemale.orEmpty()
                secondParent = prefillMale.orEmpty()
            }
            onPrefillConsumed()
        }
    }

    val femaleNameRequired = stringResource(R.string.you_must_enter_female_name)
    val maleNameRequired = stringResource(R.string.you_must_enter_male_name)
    val crossNameRequired = stringResource(R.string.you_must_enter_cross_name)
    val crossExistsEvent = stringResource(R.string.cross_id_already_exists_as_event)
    val crossExistsParent = stringResource(R.string.cross_id_already_exists_as_parent)

    fun saveEvent() {
        val female = if (maleFirst) secondParent.trim() else firstParent.trim()
        var male = if (maleFirst) firstParent.trim() else secondParent.trim()
        val name = crossId.trim()
        when {
            female.isBlank() -> onShowMessage(femaleNameRequired)
            male.isBlank() && !blankMale -> onShowMessage(maleNameRequired)
            name.isBlank() && !crossIdSettings.isPattern && !crossIdSettings.isUUID -> onShowMessage(crossNameRequired)
            events.any { it.eventDbId == name } -> onShowMessage(crossExistsEvent)
            parents.any { it.codeId == name } -> onShowMessage(crossExistsParent)
            else -> {
                if (male.isBlank()) male = "blank"
                scope.launch {
                    val eventId = withContext(Dispatchers.IO) {
                        CrossUtil(context).submitCrossEvent(
                            activity = context as? FragmentActivity,
                            female = female,
                            male = male,
                            crossName = name,
                            crossIdSettings = crossIdSettings,
                            eventsModel = eventsModel,
                            parents = parents,
                            parentModel = parentsModel,
                            wishlistProgress = wishes,
                            metaList = metadata,
                            metaValueModel = metaValuesModel,
                        )
                    }
                    firstParent = ""
                    secondParent = ""
                    crossId = when {
                        crossIdSettings.isPattern -> CrossIdSettings.load(prefs).pattern
                        crossIdSettings.isUUID -> UUID.randomUUID().toString()
                        else -> ""
                    }
                    focusFirst.requestFocus()
                    if (prefs.getBoolean(keyUtil.openCrossAfterCreateKey, false)) {
                        onOpenEvent(eventId)
                    }
                }
            }
        }
    }

    fun applyScannedCode(code: String) {
        val target = scannerTargetField
        scannerTargetField = null
        when {
            target == 0 || (target == null && firstParent.isBlank() && !(maleFirst && blankMale)) -> {
                firstParent = code
                if (crossIdSettings.isPattern || crossIdSettings.isUUID) {
                    val female = if (maleFirst) secondParent.trim() else code.trim()
                    val male = if (maleFirst) code.trim() else secondParent.trim()
                    if (female.isNotBlank() && (male.isNotBlank() || blankMale)) {
                        saveEvent()
                    } else {
                        focusSecond.requestFocus()
                    }
                } else {
                    focusSecond.requestFocus()
                }
            }
            target == 1 || (target == null && secondParent.isBlank() && !(!maleFirst && blankMale)) -> {
                secondParent = code
                if (crossIdSettings.isPattern || crossIdSettings.isUUID) {
                    val female = if (maleFirst) code.trim() else firstParent.trim()
                    val male = if (maleFirst) firstParent.trim() else code.trim()
                    if (female.isNotBlank() && (male.isNotBlank() || blankMale)) {
                        saveEvent()
                    } else {
                        focusCrossId.requestFocus()
                    }
                } else {
                    focusCrossId.requestFocus()
                }
            }
            else -> {
                crossId = code
            }
        }
    }

    LaunchedEffect(scannerResult) {
        val code = scannerResult?.takeIf { it.isNotBlank() } ?: return@LaunchedEffect
        applyScannedCode(code)
        onScannerResultConsumed()
    }

    LaunchedEffect(scannerSequenceResult) {
        val codes = scannerSequenceResult
            ?.map { it.trim() }
            ?.filter { it.isNotBlank() }
            ?: return@LaunchedEffect
        codes.forEach(::applyScannedCode)
        onScannerSequenceResultConsumed()
    }

    EventsScreenContent(
        topBarState = topBarState,
        bottomBarState = bottomBarState,
        events = events,
        firstParent = firstParent,
        secondParent = secondParent,
        crossId = crossId,
        firstLabel = firstLabel,
        secondLabel = secondLabel,
        focusFirst = focusFirst,
        focusSecond = focusSecond,
        focusCrossId = focusCrossId,
        onFirstParentChange = { firstParent = it },
        onSecondParentChange = { secondParent = it },
        onCrossIdChange = { crossId = it },
        onClear = { firstParent = ""; secondParent = "" },
        onSave = ::saveEvent,
        onOpenEvent = onOpenEvent,
        onOpenScanner = { mode ->
            if (mode == BARCODE_MODE_SINGLE) {
                if (scannerTargetField == null) {
                    scannerTargetField = when {
                        firstParent.isBlank() && !(maleFirst && blankMale) -> 0
                        secondParent.isBlank() && !(!maleFirst && blankMale) -> 1
                        else -> 2
                    }
                }
                showInlineScanner = true
            } else {
                onOpenScanner(mode)
            }
        },
        showArchivedEvents = showArchivedEvents,
        selectedEventIds = selectedEventIds,
        onToggleEventSelection = onToggleEventSelection,
        onPrintSelected = onPrintSelected,
        onDeleteSelected = onDeleteSelected,
        onArchiveSelected = onArchiveSelected,
        onUnarchiveSelected = onUnarchiveSelected,
        sortType = sortType,
        onSortTypeChange = onSortTypeChange,
        showSortDialog = showSortDialog,
        onDismissSortDialog = onDismissSortDialog,
        personDropdownContent = {
            // Person dropdown (shown when layout setting enabled)
            val showPersonInput = prefs.getBoolean(keyUtil.profileShowPersonInputKey, false)
            if (showPersonInput) {
                val personList = remember(prefs) {
                    prefs.getStringSet(keyUtil.profilePersonListKey, emptySet())
                        .orEmpty().filter { it.isNotBlank() }.sorted()
                }
                var personExpanded by remember { mutableStateOf(false) }
                var selectedPerson by remember {
                    mutableStateOf(
                        prefs.getString(keyUtil.profileSelectedPersonKey, "").orEmpty()
                    )
                }
                ExposedDropdownMenuBox(
                    expanded = personExpanded,
                    onExpandedChange = { personExpanded = it },
                ) {
                    OutlinedTextField(
                        value = selectedPerson,
                        onValueChange = {},
                        readOnly = true,
                        label = { Text(stringResource(R.string.person)) },
                        trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = personExpanded) },
                        modifier = Modifier.fillMaxWidth().menuAnchor(),
                        singleLine = true,
                    )
                    ExposedDropdownMenu(
                        expanded = personExpanded,
                        onDismissRequest = { personExpanded = false },
                    ) {
                        personList.forEach { person ->
                            DropdownMenuItem(
                                text = { Text(person) },
                                onClick = {
                                    selectedPerson = person
                                    personExpanded = false
                                    org.phenoapps.intercross.ui.settings.setProfilePerson(prefs, keyUtil, person)
                                },
                            )
                        }
                    }
                }
            }
        },
        onSetScannerTarget = { scannerTargetField = it }
    )

    if (showInlineScanner) {
        var flashEnabled by remember(prefs, keyUtil.barcodeFlashKey) {
            mutableStateOf(prefs.getBoolean(keyUtil.barcodeFlashKey, false))
        }
        Surface(
            modifier = Modifier.fillMaxSize(),
            color = MaterialTheme.colorScheme.surface
        ) {
            BarcodeScannerScreen(
                mode = BARCODE_MODE_SINGLE,
                events = events,
                parents = parents,
                mlKitFormats = remember(prefs) {
                    org.phenoapps.intercross.util.BarcodeScannerSettings.selectedMlKitFormats(prefs, keyUtil.barcodeFormatsKey)
                },
                torchEnabled = flashEnabled,
                onSingleScan = { code ->
                    applyScannedCode(code)
                    showInlineScanner = false
                },
                onOpenEvent = {
                    onOpenEvent(it)
                    showInlineScanner = false
                },
                onShowMessage = onShowMessage,
                topBarState = TopBarState(
                    titleRes = R.string.barcode_scan_label,
                    showBack = true,
                    onBack = {
                        showInlineScanner = false
                        scannerTargetField = null
                    },
                    actions = listOf(
                        TopBarAction(
                            id = "toggle_flash",
                            labelRes = if (flashEnabled) R.string.barcode_flash_off else R.string.barcode_flash_on,
                            iconRes = if (flashEnabled) R.drawable.ic_flash_off else R.drawable.ic_flash_on,
                            onClick = {
                                flashEnabled = !flashEnabled
                                prefs.edit { putBoolean(keyUtil.barcodeFlashKey, flashEnabled) }
                            },
                        ),
                    ),
                )
            )
        }
    }
}

// ─── Presentational Content (ViewModel-free, previewable) ───────────────────────

@OptIn(ExperimentalMaterial3Api::class, ExperimentalFoundationApi::class)
@Composable
internal fun EventsScreenContent(
    topBarState: TopBarState? = null,
    bottomBarState: BottomBarState? = null,
    events: List<Event>,
    firstParent: String,
    secondParent: String,
    crossId: String,
    firstLabel: String,
    secondLabel: String,
    focusFirst: FocusRequester,
    focusSecond: FocusRequester,
    focusCrossId: FocusRequester,
    onFirstParentChange: (String) -> Unit,
    onSecondParentChange: (String) -> Unit,
    onCrossIdChange: (String) -> Unit,
    onClear: () -> Unit,
    onSave: () -> Unit,
    onOpenEvent: (Long) -> Unit,
    onOpenScanner: (Int) -> Unit,
    showArchivedEvents: Boolean = false,
    selectedEventIds: Set<Long> = emptySet(),
    onToggleEventSelection: (Event) -> Unit = {},
    onPrintSelected: () -> Unit = {},
    onDeleteSelected: () -> Unit = {},
    onArchiveSelected: () -> Unit = {},
    onUnarchiveSelected: () -> Unit = {},
    sortType: EventSortType = EventSortType.DATE,
    onSortTypeChange: (EventSortType) -> Unit = {},
    showSortDialog: Boolean = false,
    onDismissSortDialog: () -> Unit = {},
    personDropdownContent: @Composable (() -> Unit)? = null,
    onSetScannerTarget: (Int) -> Unit = {},
) {
    val selectedCount = selectedEventIds.size

    Scaffold(
        topBar = {
            if (topBarState != null) {
                TopAppBar(
                    title = {
                        Text(topBarState.title ?: topBarState.titleRes?.let { stringResource(it) }.orEmpty())
                    },
                    navigationIcon = {
                        if (topBarState.showBack) {
                            IconButton(onClick = { topBarState.onBack?.invoke() }) {
                                Icon(painterResource(R.drawable.arrow_back_24px), contentDescription = stringResource(R.string.navigate_back))
                            }
                        }
                    },
                    actions = {
                        topBarState.actions.forEach { action ->
                            if (action.iconRes != null) {
                                IconButton(onClick = { action.onClick?.invoke() }) {
                                    Icon(painterResource(action.iconRes), contentDescription = stringResource(action.labelRes))
                                }
                            } else {
                                TextButton(onClick = { action.onClick?.invoke() }) {
                                    Text(stringResource(action.labelRes))
                                }
                            }
                        }
                    },
                    colors = TopAppBarDefaults.topAppBarColors(
                        containerColor = AppTheme.colors.primary,
                        titleContentColor = AppTheme.colors.surface.topBarContentColor,
                        actionIconContentColor = AppTheme.colors.surface.topBarContentColor,
                        navigationIconContentColor = AppTheme.colors.surface.topBarContentColor,
                    ),
                )
            }
        },
        bottomBar = {
            if (bottomBarState != null) {
                NavigationBar(
                    containerColor = AppTheme.colors.primary,
                ) {
                    BottomDestinations.forEach { destination ->
                        NavigationBarItem(
                            selected = bottomBarState.selectedRoute == destination.route,
                            onClick = { bottomBarState.onTabSelected(destination.route) },
                            icon = { SafeIcon(resId = destination.iconRes, contentDescription = null, tint = Color.White) },
                            label = if (bottomBarState.selectedRoute == destination.route) {
                                { Text(stringResource(destination.labelRes), color = Color.White) }
                            } else null,
                            colors = NavigationBarItemDefaults.colors(
                                indicatorColor = Color.White.copy(alpha = 0.2f),
                            ),
                        )
                    }
                }
            }
        },
    ) { innerPadding ->
    Box(modifier = Modifier.fillMaxSize().padding(innerPadding).imePadding()) {
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(horizontal = 12.dp, vertical = 8.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp),
        ) {
            if (!showArchivedEvents) {
                item {
                    ElevatedCard(
                        colors = CardDefaults.elevatedCardColors(
                            containerColor = Color.White,
                        ),
                        elevation = CardDefaults.elevatedCardElevation(defaultElevation = 4.dp),
                        shape = RoundedCornerShape(16.dp),
                    ) {
                        Column(Modifier.padding(20.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
                            OutlinedTextField(
                                value = firstParent,
                                onValueChange = onFirstParentChange,
                                label = { Text(firstLabel) },
                                singleLine = true,
                                modifier = Modifier.fillMaxWidth().focusRequester(focusFirst),
                                shape = RoundedCornerShape(12.dp),
                                keyboardOptions = KeyboardOptions(
                                    imeAction = ImeAction.Next,
                                ),
                                keyboardActions = KeyboardActions(
                                    onNext = { focusSecond.requestFocus() },
                                ),
                            )
                            OutlinedTextField(
                                value = secondParent,
                                onValueChange = onSecondParentChange,
                                label = { Text(secondLabel) },
                                singleLine = true,
                                modifier = Modifier.fillMaxWidth().focusRequester(focusSecond),
                                shape = RoundedCornerShape(12.dp),
                                keyboardOptions = KeyboardOptions(
                                    imeAction = ImeAction.Next,
                                ),
                                keyboardActions = KeyboardActions(
                                    onNext = { focusCrossId.requestFocus() },
                                ),
                            )
                            OutlinedTextField(
                                value = crossId,
                                onValueChange = onCrossIdChange,
                                label = { Text(stringResource(R.string.cross_id)) },
                                singleLine = true,
                                modifier = Modifier.fillMaxWidth().focusRequester(focusCrossId),
                                shape = RoundedCornerShape(12.dp),
                                keyboardOptions = KeyboardOptions(
                                    imeAction = ImeAction.Done,
                                ),
                                keyboardActions = KeyboardActions(
                                    onDone = { onSave() },
                                ),
                            )
                            personDropdownContent?.invoke()
                            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                                OutlinedButton(
                                    onClick = onClear,
                                    shape = RoundedCornerShape(12.dp),
                                ) {
                                    Text(stringResource(R.string.Clear))
                                }
                                Button(
                                    onClick = { onSave(); focusFirst.requestFocus() },
                                    modifier = Modifier.weight(1f).height(48.dp),
                                    colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.secondary),
                                    shape = RoundedCornerShape(12.dp),
                                ) {
                                    Text(
                                        stringResource(R.string.save_text),
                                        fontWeight = FontWeight.SemiBold,
                                    )
                                }
                            }
                        }
                    }
                }
            }
            items(events, key = { it.id ?: it.eventDbId.hashCode().toLong() }) { event ->
                val selected = event.id?.let { it in selectedEventIds } == true
                CrossListItem(
                    event = event,
                    onClick = {
                        if (selectedCount > 0) {
                            onToggleEventSelection(event)
                        } else {
                            event.id?.let(onOpenEvent)
                        }
                    },
                    onLongClick = { onToggleEventSelection(event) },
                    selected = selected,
                )
            }
        }

        AnimatedVisibility(
            visible = selectedCount > 0,
            modifier = Modifier
                .align(Alignment.BottomEnd)
                .padding(bottom = 16.dp, start = 16.dp, end = 16.dp),
        ) {
            Column(horizontalAlignment = Alignment.End, verticalArrangement = Arrangement.spacedBy(12.dp)) {
                SmallFloatingActionButton(
                    onClick = onPrintSelected,
                    shape = CircleShape,
                    containerColor = AppTheme.colors.accent,
                ) {
                    Icon(Icons.Default.Print, contentDescription = stringResource(R.string.print), tint = Color.White)
                }
                SmallFloatingActionButton(
                    onClick = if (showArchivedEvents) onUnarchiveSelected else onArchiveSelected,
                    shape = CircleShape,
                    containerColor = AppTheme.colors.accent,
                ) {
                    Icon(
                        painter = painterResource(if (showArchivedEvents) R.drawable.restore else R.drawable.ic_folder_lock),
                        contentDescription = stringResource(if (showArchivedEvents) R.string.unarchive else R.string.archive),
                        tint = Color.White,
                        modifier = Modifier.size(20.dp),
                    )
                }
                SmallFloatingActionButton(
                    onClick = onDeleteSelected,
                    shape = CircleShape,
                    containerColor = AppTheme.colors.accent,
                ) {
                    Icon(Icons.Default.Delete, contentDescription = stringResource(R.string.delete), tint = Color.White)
                }
            }
        }

        AnimatedVisibility(
            visible = selectedCount == 0 && !showArchivedEvents,
            modifier = Modifier
                .align(Alignment.BottomCenter)
                .fillMaxWidth()
                .padding(bottom = 16.dp, start = 16.dp, end = 16.dp),
        ) {
            Row(
                horizontalArrangement = Arrangement.SpaceBetween,
                modifier = Modifier.fillMaxWidth(),
            ) {
                FloatingActionButton(
                    onClick = { onOpenScanner(BARCODE_MODE_SEARCH) },
                    modifier = Modifier.size(64.dp),
                    shape = CircleShape,
                    containerColor = MaterialTheme.colorScheme.secondary,
                    contentColor = Color.White,
                ) {
                    Icon(
                        painter = painterResource(R.drawable.ic_tb_white_search),
                        contentDescription = stringResource(R.string.search_fragment_label),
                    )
                }
                Surface(
                    modifier = Modifier.size(64.dp),
                    shape = CircleShape,
                    color = MaterialTheme.colorScheme.secondary,
                    contentColor = Color.White,
                    tonalElevation = 6.dp,
                    shadowElevation = 6.dp,
                ) {
                    Box(
                        modifier = Modifier
                            .fillMaxSize()
                            .combinedClickable(
                                onClick = { onOpenScanner(BARCODE_MODE_SINGLE) },
                                onLongClick = { onOpenScanner(BARCODE_MODE_CONTINUOUS) },
                            ),
                        contentAlignment = Alignment.Center,
                    ) {
                        Icon(
                            painter = painterResource(R.drawable.ic_barcode_scan),
                            contentDescription = stringResource(R.string.barcode_scan_label),
                        )
                    }
                }
            }
        }
    }
    }
}

// ─── Previews ───────────────────────────────────────────────────────────────────

@Preview(showBackground = true, name = "EventsScreen - Populated")
@Composable
internal fun EventsScreenPopulatedPreview() {
    IntercrossPreviewTheme {
        EventsScreenContent(
            topBarState = TopBarState(
                titleRes = R.string.events_fragment_label,
                actions = listOf(
                    TopBarAction("select_all", R.string.SelectAllRows, iconRes = R.drawable.ic_select_all),
                    TopBarAction("archive", R.string.view_archived_events, iconRes = R.drawable.ic_folder_lock),
                    TopBarAction("import", R.string.import_file, iconRes = R.drawable.ic_nv_import_white),
                    TopBarAction("export", R.string.export, iconRes = R.drawable.ic_export),
                    TopBarAction("sort", R.string.sort_by, iconRes = R.drawable.sort),
                ),
            ),
            bottomBarState = BottomBarState(
                selectedRoute = "events",
                onTabSelected = {},
            ),
            events = PreviewSampleData.events,
            firstParent = "Honeycrisp",
            secondParent = "Fuji",
            crossId = "Cross-1",
            firstLabel = "Female ID",
            secondLabel = "Male ID",
            focusFirst = remember { FocusRequester() },
            focusSecond = remember { FocusRequester() },
            focusCrossId = remember { FocusRequester() },
            onFirstParentChange = {},
            onSecondParentChange = {},
            onCrossIdChange = {},
            onClear = {},
            onSave = {},
            onOpenEvent = {},
            onOpenScanner = {},
        )
    }
}

@Preview(showBackground = true, name = "EventsScreen - Empty")
@Composable
internal fun EventsScreenEmptyPreview() {
    IntercrossPreviewTheme {
        EventsScreenContent(
            topBarState = TopBarState(
                titleRes = R.string.events_fragment_label,
                actions = listOf(
                    TopBarAction("select_all", R.string.SelectAllRows, iconRes = R.drawable.ic_select_all),
                    TopBarAction("archive", R.string.view_archived_events, iconRes = R.drawable.ic_folder_lock),
                    TopBarAction("import", R.string.import_file, iconRes = R.drawable.ic_nv_import_white),
                    TopBarAction("export", R.string.export, iconRes = R.drawable.ic_export),
                    TopBarAction("sort", R.string.sort_by, iconRes = R.drawable.sort),
                ),
            ),
            bottomBarState = BottomBarState(
                selectedRoute = "events",
                onTabSelected = {},
            ),
            events = emptyList(),
            firstParent = "",
            secondParent = "",
            crossId = "",
            firstLabel = "Female ID",
            secondLabel = "Male ID",
            focusFirst = remember { FocusRequester() },
            focusSecond = remember { FocusRequester() },
            focusCrossId = remember { FocusRequester() },
            onFirstParentChange = {},
            onSecondParentChange = {},
            onCrossIdChange = {},
            onClear = {},
            onSave = {},
            onOpenEvent = {},
            onOpenScanner = {},
        )
    }
}

@Preview(showBackground = true, name = "EventsScreen - Selected")
@Composable
internal fun EventsScreenSelectedPreview() {
    val selectedIds = setOf(PreviewSampleData.events[0].id!!, PreviewSampleData.events[1].id!!)
    IntercrossPreviewTheme {
        EventsScreenContent(
            topBarState = TopBarState(
                title = "2 selected",
                actions = listOf(
                    TopBarAction("select_all", R.string.SelectAllRows, iconRes = R.drawable.ic_select_all),
                    TopBarAction("archive", R.string.view_archived_events, iconRes = R.drawable.ic_folder_lock),
                    TopBarAction("import", R.string.import_file, iconRes = R.drawable.ic_nv_import_white),
                    TopBarAction("export", R.string.export, iconRes = R.drawable.ic_export),
                    TopBarAction("sort", R.string.sort_by, iconRes = R.drawable.sort),
                ),
            ),
            bottomBarState = BottomBarState(
                selectedRoute = "events",
                onTabSelected = {},
            ),
            events = PreviewSampleData.events,
            selectedEventIds = selectedIds,
            firstParent = "",
            secondParent = "",
            crossId = "",
            firstLabel = "Female ID",
            secondLabel = "Male ID",
            focusFirst = remember { FocusRequester() },
            focusSecond = remember { FocusRequester() },
            focusCrossId = remember { FocusRequester() },
            onFirstParentChange = {},
            onSecondParentChange = {},
            onCrossIdChange = {},
            onClear = {},
            onSave = {},
            onOpenEvent = {},
            onOpenScanner = {},
        )
    }
}

@Preview(showBackground = true, name = "EventsScreen - Archived")
@Composable
internal fun EventsScreenArchivedPreview() {
    IntercrossPreviewTheme {
        EventsScreenContent(
            topBarState = TopBarState(
                title = "Archived Events",
                actions = listOf(
                    TopBarAction("select_all", R.string.SelectAllRows, iconRes = R.drawable.ic_select_all),
                    TopBarAction("archive", R.string.view_active_events, iconRes = R.drawable.ic_visibility),
                    TopBarAction("import", R.string.import_file, iconRes = R.drawable.ic_nv_import_white),
                    TopBarAction("export", R.string.export, iconRes = R.drawable.ic_export),
                    TopBarAction("sort", R.string.sort_by, iconRes = R.drawable.sort),
                ),
            ),
            bottomBarState = BottomBarState(
                selectedRoute = "events",
                onTabSelected = {},
            ),
            events = PreviewSampleData.events.take(2),
            showArchivedEvents = true,
            firstParent = "",
            secondParent = "",
            crossId = "",
            firstLabel = "Female ID",
            secondLabel = "Male ID",
            focusFirst = remember { FocusRequester() },
            focusSecond = remember { FocusRequester() },
            focusCrossId = remember { FocusRequester() },
            onFirstParentChange = {},
            onSecondParentChange = {},
            onCrossIdChange = {},
            onClear = {},
            onSave = {},
            onOpenEvent = {},
            onOpenScanner = {},
        )
    }
}

@Preview(showBackground = true, name = "EventsScreen - Archived Selected")
@Composable
internal fun EventsScreenArchivedSelectedPreview() {
    val selectedIds = setOf(PreviewSampleData.events[0].id!!)
    IntercrossPreviewTheme {
        EventsScreenContent(
            topBarState = TopBarState(
                title = "1 selected",
                actions = listOf(
                    TopBarAction("select_all", R.string.SelectAllRows, iconRes = R.drawable.ic_select_all),
                    TopBarAction("archive", R.string.view_active_events, iconRes = R.drawable.ic_visibility),
                    TopBarAction("import", R.string.import_file, iconRes = R.drawable.ic_nv_import_white),
                    TopBarAction("export", R.string.export, iconRes = R.drawable.ic_export),
                    TopBarAction("sort", R.string.sort_by, iconRes = R.drawable.sort),
                ),
            ),
            bottomBarState = BottomBarState(
                selectedRoute = "events",
                onTabSelected = {},
            ),
            events = PreviewSampleData.events.take(2),
            showArchivedEvents = true,
            selectedEventIds = selectedIds,
            firstParent = "",
            secondParent = "",
            crossId = "",
            firstLabel = "Female ID",
            secondLabel = "Male ID",
            focusFirst = remember { FocusRequester() },
            focusSecond = remember { FocusRequester() },
            focusCrossId = remember { FocusRequester() },
            onFirstParentChange = {},
            onSecondParentChange = {},
            onCrossIdChange = {},
            onClear = {},
            onSave = {},
            onOpenEvent = {},
            onOpenScanner = {},
        )
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Preview(showBackground = true, name = "EventsScreen - Person Selection")
@Composable
internal fun EventsScreenPersonSelectionPreview() {
    IntercrossPreviewTheme {
        EventsScreenContent(
            topBarState = TopBarState(
                titleRes = R.string.events_fragment_label,
                actions = listOf(
                    TopBarAction("select_all", R.string.SelectAllRows, iconRes = R.drawable.ic_select_all),
                    TopBarAction("archive", R.string.view_archived_events, iconRes = R.drawable.ic_folder_lock),
                    TopBarAction("import", R.string.import_file, iconRes = R.drawable.ic_nv_import_white),
                    TopBarAction("export", R.string.export, iconRes = R.drawable.ic_export),
                    TopBarAction("sort", R.string.sort_by, iconRes = R.drawable.sort),
                ),
            ),
            bottomBarState = BottomBarState(
                selectedRoute = "events",
                onTabSelected = {},
            ),
            events = PreviewSampleData.events,
            firstParent = "Honeycrisp",
            secondParent = "Fuji",
            crossId = "Cross-1",
            firstLabel = "Female ID",
            secondLabel = "Male ID",
            focusFirst = remember { FocusRequester() },
            focusSecond = remember { FocusRequester() },
            focusCrossId = remember { FocusRequester() },
            onFirstParentChange = {},
            onSecondParentChange = {},
            onCrossIdChange = {},
            onClear = {},
            onSave = {},
            onOpenEvent = {},
            onOpenScanner = {},
            personDropdownContent = {
                OutlinedTextField(
                    value = "Jane",
                    onValueChange = {},
                    readOnly = true,
                    label = { Text(stringResource(R.string.person)) },
                    trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = false) },
                    modifier = Modifier.fillMaxWidth(),
                    singleLine = true,
                )
            }
        )
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Preview(showBackground = true, name = "EventsScreen - Person Selection Expanded")
@Composable
internal fun EventsScreenPersonSelectionExpandedPreview() {
    IntercrossPreviewTheme {
        EventsScreenContent(
            topBarState = TopBarState(
                titleRes = R.string.events_fragment_label,
                actions = listOf(
                    TopBarAction("select_all", R.string.SelectAllRows, iconRes = R.drawable.ic_select_all),
                    TopBarAction("archive", R.string.view_archived_events, iconRes = R.drawable.ic_folder_lock),
                    TopBarAction("import", R.string.import_file, iconRes = R.drawable.ic_nv_import_white),
                    TopBarAction("export", R.string.export, iconRes = R.drawable.ic_export),
                    TopBarAction("sort", R.string.sort_by, iconRes = R.drawable.sort),
                ),
            ),
            bottomBarState = BottomBarState(
                selectedRoute = "events",
                onTabSelected = {},
            ),
            events = PreviewSampleData.events,
            firstParent = "Honeycrisp",
            secondParent = "Fuji",
            crossId = "Cross-1",
            firstLabel = "Female ID",
            secondLabel = "Male ID",
            focusFirst = remember { FocusRequester() },
            focusSecond = remember { FocusRequester() },
            focusCrossId = remember { FocusRequester() },
            onFirstParentChange = {},
            onSecondParentChange = {},
            onCrossIdChange = {},
            onClear = {},
            onSave = {},
            onOpenEvent = {},
            onOpenScanner = {},
            personDropdownContent = {
                Box {
                    OutlinedTextField(
                        value = "Jane",
                        onValueChange = {},
                        readOnly = true,
                        label = { Text(stringResource(R.string.person)) },
                        trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = true) },
                        modifier = Modifier.fillMaxWidth(),
                        singleLine = true,
                    )
                    DropdownMenu(
                        expanded = true,
                        onDismissRequest = { },
                    ) {
                        DropdownMenuItem(
                            text = { Text("Jane") },
                            onClick = { },
                        )
                        DropdownMenuItem(
                            text = { Text("John") },
                            onClick = { },
                        )
                    }
                }
            }
        )
    }
}
