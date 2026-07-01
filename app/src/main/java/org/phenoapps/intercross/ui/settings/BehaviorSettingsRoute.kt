package org.phenoapps.intercross.ui.settings

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.core.content.edit
import org.phenoapps.intercross.R
import androidx.compose.foundation.layout.Box
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.ui.res.painterResource
import org.phenoapps.intercross.ui.app.TopBarState
import org.phenoapps.intercross.ui.app.rememberPrefs
import org.phenoapps.intercross.ui.theme.AppTheme
import org.phenoapps.intercross.util.BarcodeFormatGroup
import org.phenoapps.intercross.util.BarcodeScannerSettings
import org.phenoapps.intercross.util.CrossIdSettings

@Composable
fun BehaviorSettingsRoute(
    onOpenPattern: () -> Unit,
    onOpenMetadata: () -> Unit,
    onBack: () -> Unit = {},
) {
    val (prefs, keyUtil) = rememberPrefs()
    val crossIdSettings = remember { CrossIdSettings.load(prefs) }
    var blankMale by rememberBooleanPreference(prefs, keyUtil.blankMaleKey, false)
    var crossOrder by rememberBooleanPreference(prefs, keyUtil.crossOrderKey, false)
    //var collectAdditionalInfo by rememberBooleanPreference(prefs, keyUtil.collectAdditionalInfoKey, false)
    var soundNotifications by rememberBooleanPreference(prefs, keyUtil.soundNotificationKey, false)
    var openCrossAfterCreate by rememberBooleanPreference(prefs, keyUtil.openCrossAfterCreateKey, false)
    var commutativeCrossing by rememberBooleanPreference(prefs, keyUtil.commutativeCrossingKey, false)
    var barcodeFormats by remember(prefs, keyUtil.barcodeFormatsKey) {
        androidx.compose.runtime.mutableStateOf(
            BarcodeScannerSettings.selectedValues(prefs, keyUtil.barcodeFormatsKey),
        )
    }

    val crossIdSummary = when {
        crossIdSettings.isPattern -> stringResource(R.string.pattern)
        !crossIdSettings.isUUID && !crossIdSettings.isPattern -> stringResource(R.string.none)
        else -> stringResource(R.string.uuid)
    }

    BehaviorSettingsScreenContent(
        blankMale = blankMale,
        onBlankMaleChange = { blankMale = it; prefs.edit { putBoolean(keyUtil.blankMaleKey, it) } },
        crossOrder = crossOrder,
        onCrossOrderChange = { crossOrder = it; prefs.edit { putBoolean(keyUtil.crossOrderKey, it) } },
        barcodeFormats = barcodeFormats,
        barcodeFormatSummary = BarcodeScannerSettings.summary(barcodeFormats),
        onBarcodeFormatsChange = {
            val normalizedFormats = it.ifEmpty { BarcodeScannerSettings.defaultValues }
            barcodeFormats = normalizedFormats
            prefs.edit { putStringSet(keyUtil.barcodeFormatsKey, normalizedFormats) }
        },
        crossIdSummary = crossIdSummary,
        onOpenPattern = onOpenPattern,
        onOpenMetadata = onOpenMetadata,
        soundNotifications = soundNotifications,
        onSoundNotificationsChange = { soundNotifications = it; prefs.edit { putBoolean(keyUtil.soundNotificationKey, it) } },
        openCrossAfterCreate = openCrossAfterCreate,
        onOpenCrossAfterCreateChange = { openCrossAfterCreate = it; prefs.edit { putBoolean(keyUtil.openCrossAfterCreateKey, it) } },
        commutativeCrossing = commutativeCrossing,
        onCommutativeCrossingChange = { commutativeCrossing = it; prefs.edit { putBoolean(keyUtil.commutativeCrossingKey, it) } },
        onBack = onBack,
    )
}

@androidx.compose.ui.tooling.preview.Preview(showBackground = true, name = "BehaviorSettings - Default")
@Composable
internal fun BehaviorSettingsPreview() {
    org.phenoapps.intercross.ui.theme.IntercrossPreviewTheme {
        BehaviorSettingsScreenContent(
            blankMale = false,
            onBlankMaleChange = {},
            crossOrder = false,
            onCrossOrderChange = {},
            barcodeFormats = setOf("QR_CODE", "CODE_128"),
            barcodeFormatSummary = "QR_CODE, CODE_128",
            onBarcodeFormatsChange = {},
            crossIdSummary = "UUID",
            onOpenPattern = {},
            onOpenMetadata = {},
            soundNotifications = false,
            onSoundNotificationsChange = {},
            openCrossAfterCreate = false,
            onOpenCrossAfterCreateChange = {},
            commutativeCrossing = false,
            onCommutativeCrossingChange = {},
        )
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun BehaviorSettingsScreenContent(
    blankMale: Boolean,
    onBlankMaleChange: (Boolean) -> Unit,
    crossOrder: Boolean,
    onCrossOrderChange: (Boolean) -> Unit,
    barcodeFormats: Set<String>,
    barcodeFormatSummary: String,
    onBarcodeFormatsChange: (Set<String>) -> Unit,
    crossIdSummary: String,
    onOpenPattern: () -> Unit,
    onOpenMetadata: () -> Unit,
    soundNotifications: Boolean,
    onSoundNotificationsChange: (Boolean) -> Unit,
    openCrossAfterCreate: Boolean,
    onOpenCrossAfterCreateChange: (Boolean) -> Unit,
    commutativeCrossing: Boolean,
    onCommutativeCrossingChange: (Boolean) -> Unit,
    onBack: () -> Unit = {},
) {
    val topBarState = TopBarState(titleRes = R.string.prefs_behavior_title, showBack = true, onBack = onBack)

    Scaffold(
        topBar = {
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
        },
    ) { innerPadding ->
    Box(Modifier.padding(innerPadding)) {
    LazyColumn(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(8.dp),
    ) {
        item { SettingsSectionHeader(R.string.prefs_behavior_naming_category) }
        item {
            SettingsSwitchRow(
                titleRes = R.string.prefs_behavior_allow_blank_male_title,
                summaryRes = R.string.prefs_behavior_allow_blank_male_summary,
                iconRes = R.drawable.ic_setting_male_blank,
                checked = blankMale,
                onCheckedChange = onBlankMaleChange,
            )
        }
        item {
            SettingsSwitchRow(
                titleRes = R.string.prefs_behavior_scan_male_first_title,
                summaryRes = R.string.prefs_behavior_scan_male_first_summary,
                iconRes = R.drawable.ic_setting_cross_order,
                checked = crossOrder,
                onCheckedChange = onCrossOrderChange,
            )
        }
        item { SettingsSectionHeader(R.string.prefs_behavior_scanning_category) }
        item {
            SettingsGroupedMultiChoiceRow(
                titleRes = R.string.prefs_behavior_barcode_formats_title,
                summary = barcodeFormatSummary,
                iconRes = R.drawable.ic_barcode_scan,
                groups = listOf(
                    stringResource(R.string.barcode_formats_2d_group) to BarcodeScannerSettings.options
                        .filter { it.group == BarcodeFormatGroup.TWO_D }
                        .map { it.value to it.label },
                    stringResource(R.string.barcode_formats_1d_group) to BarcodeScannerSettings.options
                        .filter { it.group == BarcodeFormatGroup.ONE_D }
                        .map { it.value to it.label },
                ),
                selectedValues = barcodeFormats,
                onSelected = onBarcodeFormatsChange,
            )
        }
        item {
            SettingsActionRow(
                titleRes = R.string.prefs_behavior_create_cross_id_pattern_title,
                summary = crossIdSummary,
                iconRes = R.drawable.ic_setting_pattern_create,
                onClick = onOpenPattern,
            )
        }
        item { SettingsSectionHeader(R.string.prefs_behavior_workflow_category) }
        item {
            SettingsActionRow(
                titleRes = R.string.prefs_behavior_manage_metadata_title,
                summaryRes = R.string.prefs_behavior_manage_metadata_summary,
                iconRes = R.drawable.ic_update_black_24dp,
                onClick = onOpenMetadata,
            )
        }
        item {
            SettingsSwitchRow(
                titleRes = R.string.prefs_behavior_sound_notifications_title,
                summaryRes = R.string.prefs_behavior_sound_notifications_summary,
                iconRes = R.drawable.ic_setting_sound,
                checked = soundNotifications,
                onCheckedChange = onSoundNotificationsChange,
            )
        }
        item {
            SettingsSwitchRow(
                titleRes = R.string.prefs_behavior_open_cross_title,
                summaryRes = R.string.prefs_behavior_open_cross_summary,
                iconRes = R.drawable.ic_book_open,
                checked = openCrossAfterCreate,
                onCheckedChange = onOpenCrossAfterCreateChange,
            )
        }
        item {
            SettingsSwitchRow(
                titleRes = R.string.prefs_behavior_commutative_crossing_title,
                summaryRes = R.string.prefs_behavior_commutative_crossing_summary,
                iconRes = R.drawable.ic_receipt_long_black_24dp,
                checked = commutativeCrossing,
                onCheckedChange = onCommutativeCrossingChange,
            )
        }
    }
    } // Box
    } // Scaffold
}
