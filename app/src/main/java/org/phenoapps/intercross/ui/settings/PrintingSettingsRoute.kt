package org.phenoapps.intercross.ui.settings

import android.content.Context
import android.content.SharedPreferences
import android.content.pm.PackageManager
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.core.content.edit
import org.phenoapps.intercross.R
import androidx.compose.foundation.layout.Box
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.RadioButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.ui.res.painterResource
import org.phenoapps.intercross.ui.app.TopBarState
import org.phenoapps.intercross.ui.app.rememberPrefs
import org.phenoapps.intercross.ui.theme.AppTheme
import org.phenoapps.intercross.util.KeyUtil
import org.phenoapps.intercross.util.LabelTemplateConfig
import org.phenoapps.intercross.util.LabelTemplateStore
import org.phenoapps.intercross.util.LabelTemplateType
import org.phenoapps.intercross.util.ZplTemplate
import java.util.Locale

@Composable
fun PrintingSettingsRoute(
    onOpenLabelDesigner: () -> Unit,
    onBack: () -> Unit = {},
) {
    val context = LocalContext.current
    val (prefs, keyUtil) = rememberPrefs()
    var selectedDeviceName by remember { mutableStateOf(prefs.getString(keyUtil.printerDeviceNameKey, "").orEmpty()) }
    var showDeviceDialog by remember { mutableStateOf(false) }
    var showCrossTemplateDialog by remember { mutableStateOf(false) }
    var showParentTemplateDialog by remember { mutableStateOf(false) }
    var message by remember { mutableStateOf<String?>(null) }
    val noBluetoothPermissionText = stringResource(R.string.error_no_bluetooth_permission)
    val permissionLauncher = rememberLauncherForActivityResult(ActivityResultContracts.RequestMultiplePermissions()) { granted ->
        if (granted.values.any { !it }) {
            message = noBluetoothPermissionText
        } else {
            showDeviceDialog = true
        }
    }

    fun requestBluetoothOrShowDevices() {
        val permissions = bluetoothPrinterPermissions()
        val missing = permissions.filter {
            context.checkSelfPermission(it) != PackageManager.PERMISSION_GRANTED
        }
        if (missing.isEmpty()) {
            showDeviceDialog = true
        } else {
            permissionLauncher.launch(missing.toTypedArray())
        }
    }

    val crossTemplateName = prefs.getString(templatePreferenceKey(keyUtil, LabelTemplateType.CROSS), "").orEmpty()
    val parentTemplateName = prefs.getString(templatePreferenceKey(keyUtil, LabelTemplateType.PARENT), "").orEmpty()

    val crossTemplates = templatesFor(context, prefs, keyUtil, LabelTemplateType.CROSS)
    val parentTemplates = templatesFor(context, prefs, keyUtil, LabelTemplateType.PARENT)

    PrintingSettingsScreenContent(
        selectedDeviceName = selectedDeviceName,
        onConnectPrinter = { requestBluetoothOrShowDevices() },
        onOpenZebraSetup = { openAppOrStore(context, "com.zebra.printersetup") },
        onOpenLabelDesigner = onOpenLabelDesigner,
        crossTemplateName = crossTemplateName,
        parentTemplateName = parentTemplateName,
        onShowCrossTemplateDialog = { showCrossTemplateDialog = true },
        onShowParentTemplateDialog = { showParentTemplateDialog = true },
        onRequestLabel = { openUri(context, "https://github.com/PhenoApps/Intercross/issues") },
        showDeviceDialog = showDeviceDialog,
        onDeviceDialogDismiss = { showDeviceDialog = false },
        onDeviceSelected = { name ->
            selectedDeviceName = name
            prefs.edit { putString(keyUtil.printerDeviceNameKey, name) }
            showDeviceDialog = false
        },
        showCrossTemplateDialog = showCrossTemplateDialog,
        crossTemplates = crossTemplates,
        onCrossTemplateDialogDismiss = { showCrossTemplateDialog = false },
        onCrossTemplateSelected = { template ->
            setActiveTemplate(prefs, keyUtil, template)
            showCrossTemplateDialog = false
        },
        showParentTemplateDialog = showParentTemplateDialog,
        parentTemplates = parentTemplates,
        onParentTemplateDialogDismiss = { showParentTemplateDialog = false },
        onParentTemplateSelected = { template ->
            setActiveTemplate(prefs, keyUtil, template)
            showParentTemplateDialog = false
        },
        message = message,
        onMessageDismiss = { message = null },
        onBack = onBack,
    )
}

@androidx.compose.ui.tooling.preview.Preview(showBackground = true, name = "PrintingSettings - Default")
@Composable
internal fun PrintingSettingsPreview() {
    org.phenoapps.intercross.ui.theme.IntercrossPreviewTheme {
        PrintingSettingsScreenContent(
            selectedDeviceName = "Zebra ZQ620",
            onConnectPrinter = {},
            onOpenZebraSetup = {},
            onOpenLabelDesigner = {},
            crossTemplateName = "Standard 2x1",
            parentTemplateName = "Parent Label",
            onShowCrossTemplateDialog = {},
            onShowParentTemplateDialog = {},
            onRequestLabel = {},
            showDeviceDialog = false,
            onDeviceDialogDismiss = {},
            onDeviceSelected = {},
            showCrossTemplateDialog = false,
            crossTemplates = emptyList(),
            onCrossTemplateDialogDismiss = {},
            onCrossTemplateSelected = {},
            showParentTemplateDialog = false,
            parentTemplates = emptyList(),
            onParentTemplateDialogDismiss = {},
            onParentTemplateSelected = {},
            message = null,
            onMessageDismiss = {},
        )
    }
}

@androidx.compose.ui.tooling.preview.Preview(showBackground = true, name = "PrintingSettings - Template Dialog")
@Composable
internal fun PrintingSettingsTemplateDialogPreview() {
    org.phenoapps.intercross.ui.theme.IntercrossPreviewTheme {
        PrintingSettingsScreenContent(
            selectedDeviceName = "Zebra ZQ620",
            onConnectPrinter = {},
            onOpenZebraSetup = {},
            onOpenLabelDesigner = {},
            crossTemplateName = "Standard 2x1",
            parentTemplateName = "Parent Label",
            onShowCrossTemplateDialog = {},
            onShowParentTemplateDialog = {},
            onRequestLabel = {},
            showDeviceDialog = false,
            onDeviceDialogDismiss = {},
            onDeviceSelected = {},
            showCrossTemplateDialog = true,
            crossTemplates = listOf(
                LabelTemplateConfig(name = "Standard 2x1", rawZpl = "", labelType = LabelTemplateType.CROSS.name),
                LabelTemplateConfig(name = "Compact 1x1", rawZpl = "", labelType = LabelTemplateType.CROSS.name),
                LabelTemplateConfig(name = "Detailed 3x2", rawZpl = "", labelType = LabelTemplateType.CROSS.name),
            ),
            onCrossTemplateDialogDismiss = {},
            onCrossTemplateSelected = {},
            showParentTemplateDialog = false,
            parentTemplates = emptyList(),
            onParentTemplateDialogDismiss = {},
            onParentTemplateSelected = {},
            message = null,
            onMessageDismiss = {},
        )
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun PrintingSettingsScreenContent(
    selectedDeviceName: String,
    onConnectPrinter: () -> Unit,
    onOpenZebraSetup: () -> Unit,
    onOpenLabelDesigner: () -> Unit,
    crossTemplateName: String,
    parentTemplateName: String,
    onShowCrossTemplateDialog: () -> Unit,
    onShowParentTemplateDialog: () -> Unit,
    onRequestLabel: () -> Unit,
    showDeviceDialog: Boolean,
    onDeviceDialogDismiss: () -> Unit,
    onDeviceSelected: (String) -> Unit,
    showCrossTemplateDialog: Boolean,
    crossTemplates: List<LabelTemplateConfig>,
    onCrossTemplateDialogDismiss: () -> Unit,
    onCrossTemplateSelected: (LabelTemplateConfig) -> Unit,
    showParentTemplateDialog: Boolean,
    parentTemplates: List<LabelTemplateConfig>,
    onParentTemplateDialogDismiss: () -> Unit,
    onParentTemplateSelected: (LabelTemplateConfig) -> Unit,
    message: String?,
    onMessageDismiss: () -> Unit,
    onBack: () -> Unit = {},
) {
    val topBarState = TopBarState(titleRes = R.string.prefs_printing_title, showBack = true, onBack = onBack)

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
        item { SettingsSectionHeader(R.string.prefs_printing_category_title) }
        item {
            SettingsActionRow(
                titleRes = R.string.prefs_zebra_device_title,
                summary = if (selectedDeviceName.isBlank()) {
                    stringResource(R.string.prefs_zebra_device_summary)
                } else {
                    stringResource(R.string.prefs_zebra_device_selected, selectedDeviceName)
                },
                iconRes = R.drawable.ic_cross_print,
                onClick = onConnectPrinter,
            )
        }
        item {
            SettingsActionRow(
                titleRes = R.string.prefs_zebra_print_connect_title,
                summaryRes = R.string.prefs_zebra_print_connect_summary,
                iconRes = R.drawable.ic_setting_print_connect,
                onClick = onOpenZebraSetup,
            )
        }
        item { SettingsSectionHeader(R.string.prefs_label_category_title) }
        item {
            SettingsActionRow(
                titleRes = R.string.zpl_label_setup_title,
                summaryRes = R.string.label_designer_summary,
                iconRes = R.drawable.ic_receipt_long_black_24dp,
                onClick = onOpenLabelDesigner,
            )
        }
        item {
            SettingsActionRow(
                titleRes = R.string.prefs_cross_zpl_template_title,
                summary = crossTemplateName.ifBlank { stringResource(R.string.prefs_zpl_template_empty_summary) }.let {
                    if (crossTemplateName.isBlank()) it else stringResource(R.string.prefs_zpl_template_selected_summary, crossTemplateName)
                },
                iconRes = R.drawable.ic_badge_black_24dp,
                onClick = onShowCrossTemplateDialog,
            )
        }
        item {
            SettingsActionRow(
                titleRes = R.string.prefs_parent_zpl_template_title,
                summary = parentTemplateName.ifBlank { stringResource(R.string.prefs_zpl_template_empty_summary) }.let {
                    if (parentTemplateName.isBlank()) it else stringResource(R.string.prefs_zpl_template_selected_summary, parentTemplateName)
                },
                iconRes = R.drawable.ic_nv_parents_tab,
                onClick = onShowParentTemplateDialog,
            )
        }
        item {
            SettingsActionRow(
                titleRes = R.string.prefs_label_request_title,
                summaryRes = R.string.prefs_label_request_summary,
                iconRes = R.drawable.ic_about_github,
                onClick = onRequestLabel,
            )
        }
    }

    if (showDeviceDialog) {
        PrinterDeviceDialog(
            selectedDeviceName = selectedDeviceName,
            onDismiss = onDeviceDialogDismiss,
            onSelected = onDeviceSelected,
        )
    }

    if (showCrossTemplateDialog) {
        LabelTemplateSelectionDialog(
            type = LabelTemplateType.CROSS,
            templates = crossTemplates,
            selectedName = crossTemplateName,
            onDismiss = onCrossTemplateDialogDismiss,
            onSelected = onCrossTemplateSelected,
        )
    }

    if (showParentTemplateDialog) {
        LabelTemplateSelectionDialog(
            type = LabelTemplateType.PARENT,
            templates = parentTemplates,
            selectedName = parentTemplateName,
            onDismiss = onParentTemplateDialogDismiss,
            onSelected = onParentTemplateSelected,
        )
    }

    message?.let {
        AlertDialog(
            onDismissRequest = onMessageDismiss,
            text = { Text(it) },
            confirmButton = {
                TextButton(onClick = onMessageDismiss) {
                    Text(stringResource(android.R.string.ok))
                }
            },
        )
    }
    } // Box
    } // Scaffold
}

@Composable
private fun LabelTemplateSelectionDialog(
    type: LabelTemplateType,
    templates: List<LabelTemplateConfig>,
    selectedName: String,
    onDismiss: () -> Unit,
    onSelected: (LabelTemplateConfig) -> Unit,
) {
    var pendingName by remember(selectedName, templates) {
        mutableStateOf(selectedName.ifBlank { templates.firstOrNull()?.name.orEmpty() })
    }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = {
            Text(
                stringResource(
                    if (type == LabelTemplateType.CROSS) R.string.prefs_cross_zpl_template_title
                    else R.string.prefs_parent_zpl_template_title,
                ),
            )
        },
        text = {
            if (templates.isEmpty()) {
                Text(stringResource(R.string.prefs_zpl_template_empty_summary))
            } else {
                LazyColumn(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                    items(templates, key = { it.name }) { template ->
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clickable { pendingName = template.name }
                                .padding(vertical = 8.dp),
                            verticalAlignment = Alignment.CenterVertically,
                        ) {
                            RadioButton(selected = pendingName == template.name, onClick = { pendingName = template.name })
                            Column(Modifier.padding(start = 8.dp)) {
                                Text(template.name)
                                Text(
                                    template.type.name,
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                                )
                            }
                        }
                    }
                }
            }
        },
        confirmButton = {
            TextButton(
                enabled = pendingName.isNotBlank(),
                onClick = {
                    templates.firstOrNull { it.name == pendingName }?.let(onSelected)
                },
            ) {
                Text(stringResource(R.string.dialog_save))
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text(stringResource(R.string.dialog_cancel))
            }
        },
    )
}

// -- Printing helper functions --

private fun templatesFor(
    context: Context,
    prefs: SharedPreferences,
    keyUtil: KeyUtil,
    type: LabelTemplateType,
): List<LabelTemplateConfig> {
    val saved = LabelTemplateStore.load(prefs, keyUtil.labelTemplatesKey)
    val builtIn = ZplTemplate.getDefaultTemplates(context).map {
        LabelTemplateConfig(
            name = it.displayName,
            rawZpl = it.zplCode,
            labelType = it.type.name,
        )
    }
    return (saved + builtIn)
        .filter { it.type == type }
        .distinctBy { it.name.lowercase(Locale.getDefault()) }
        .sortedBy { it.name.lowercase(Locale.getDefault()) }
}

private fun setActiveTemplate(
    prefs: SharedPreferences,
    keyUtil: KeyUtil,
    template: LabelTemplateConfig,
) {
    prefs.edit {
        when (template.type) {
            LabelTemplateType.CROSS -> {
                putString(keyUtil.crossZplTemplateKey, template.name)
                putString(keyUtil.crossZplCodeKey, template.toZpl())
            }
            LabelTemplateType.PARENT -> {
                putString(keyUtil.parentZplTemplateKey, template.name)
                putString(keyUtil.parentZplCodeKey, template.toZpl())
            }
        }
    }
}

private fun templatePreferenceKey(
    keyUtil: KeyUtil,
    type: LabelTemplateType,
): String {
    return when (type) {
        LabelTemplateType.CROSS -> keyUtil.crossZplTemplateKey
        LabelTemplateType.PARENT -> keyUtil.parentZplTemplateKey
    }
}
