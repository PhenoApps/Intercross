package org.phenoapps.intercross.ui.settings

import android.content.Context
import android.content.Intent
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.core.content.edit
import org.phenoapps.intercross.R
import org.phenoapps.intercross.activities.DefineStorageActivity
import org.phenoapps.intercross.data.IntercrossDatabase
import org.phenoapps.intercross.fragments.ImportSampleDialogFragment
import androidx.compose.foundation.layout.Box
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.ui.res.painterResource
import org.phenoapps.intercross.ui.app.TopBarState
import org.phenoapps.intercross.ui.app.rememberPrefs
import org.phenoapps.intercross.ui.theme.AppTheme
import org.phenoapps.intercross.util.DateUtil
import org.phenoapps.intercross.util.FileUtil
import org.phenoapps.utils.BaseDocumentTreeUtil
import org.phenoapps.utils.BaseDocumentTreeUtil.Companion.getStem
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

@Composable
fun DatabaseSettingsRoute(
    onBack: () -> Unit = {},
) {
    val context = LocalContext.current
    val activity = context as? androidx.fragment.app.FragmentActivity
    val scope = rememberCoroutineScope()
    val (prefs, keyUtil) = rememberPrefs()
    val fileUtil = remember(context) { FileUtil(context.applicationContext) }
    var showExportDialog by remember { mutableStateOf(false) }
    var showResetFirstDialog by remember { mutableStateOf(false) }
    var showResetSecondDialog by remember { mutableStateOf(false) }
    var showReloadDialog by remember { mutableStateOf(false) }
    var message by remember { mutableStateOf<String?>(null) }
    val importSuccessText = stringResource(R.string.database_import_success)
    val importErrorText = stringResource(R.string.database_import_error)
    val exportSuccessText = stringResource(R.string.database_export_success)
    val exportErrorText = stringResource(R.string.database_export_error)
    val resetSuccessText = stringResource(R.string.database_reset_success)
    val resetErrorText = stringResource(R.string.database_reset_error)
    val reloadErrorText = stringResource(R.string.database_reload_error)
    val backupPrefix = stringResource(R.string.default_database_backup_export_file_name)

    val databaseImportLauncher = rememberLauncherForActivityResult(ActivityResultContracts.GetContent()) { uri ->
        uri?.let {
            scope.launch {
                val success = runCatching {
                    withContext(Dispatchers.IO) {
                        fileUtil.importDatabase(it)
                    }
                }.isSuccess
                message = if (success) importSuccessText else importErrorText
            }
        }
    }

    fun resetDatabase(clearPrefs: Boolean, afterReset: suspend () -> Unit = {}) {
        scope.launch {
            val success = runCatching {
                withContext(Dispatchers.IO) {
                    runCatching {
                        val backupName = "${backupPrefix}_${DateUtil().getTime()}"
                        fileUtil.exportDatabase(backupName)
                    }
                    IntercrossDatabase.getInstance(context.applicationContext).clearAllTables()
                    if (clearPrefs) prefs.edit { clear() }
                    afterReset()
                }
            }.isSuccess

            message = when {
                success && clearPrefs -> resetSuccessText
                success -> resetSuccessText
                clearPrefs -> resetErrorText
                else -> reloadErrorText
            }

            if (success && clearPrefs) {
                activity?.finishAffinity()
            }
        }
    }

    DatabaseSettingsScreenContent(
        storageSummary = storageSummary(context),
        onOpenStorage = { startSafely(context, Intent(context, DefineStorageActivity::class.java)) },
        onImport = { databaseImportLauncher.launch("*/*") },
        onExportClick = { showExportDialog = true },
        onReloadClick = { showReloadDialog = true },
        onResetClick = { showResetFirstDialog = true },
        showExportDialog = showExportDialog,
        onExportDismiss = { showExportDialog = false },
        onExportConfirm = { fileName: String ->
            scope.launch {
                val success = runCatching {
                    withContext(Dispatchers.IO) {
                        fileUtil.exportDatabase(fileName)
                    }
                }.isSuccess
                message = if (success) exportSuccessText else exportErrorText
                showExportDialog = false
            }
        },
        showResetFirstDialog = showResetFirstDialog,
        onResetFirstDismiss = { showResetFirstDialog = false },
        onResetFirstConfirm = {
            showResetFirstDialog = false
            showResetSecondDialog = true
        },
        showResetSecondDialog = showResetSecondDialog,
        onResetSecondDismiss = { showResetSecondDialog = false },
        onResetSecondConfirm = {
            showResetSecondDialog = false
            resetDatabase(clearPrefs = true)
        },
        showReloadDialog = showReloadDialog,
        onReloadDismiss = { showReloadDialog = false },
        onReloadConfirm = {
            showReloadDialog = false
            resetDatabase(clearPrefs = false) {
                prefs.edit {
                    putBoolean(keyUtil.loadSampleWishlist, true)
                    putBoolean(keyUtil.loadSampleParents, true)
                }
                withContext(Dispatchers.Main) {
                    activity?.supportFragmentManager?.let {
                        ImportSampleDialogFragment().show(it, ImportSampleDialogFragment.TAG)
                    }
                }
            }
        },
        message = message,
        onMessageDismiss = { message = null },
        onBack = onBack,
    )
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun DatabaseSettingsScreenContent(
    storageSummary: String,
    onOpenStorage: () -> Unit,
    onImport: () -> Unit,
    onExportClick: () -> Unit,
    onReloadClick: () -> Unit,
    onResetClick: () -> Unit,
    showExportDialog: Boolean,
    onExportDismiss: () -> Unit,
    onExportConfirm: (String) -> Unit,
    showResetFirstDialog: Boolean,
    onResetFirstDismiss: () -> Unit,
    onResetFirstConfirm: () -> Unit,
    showResetSecondDialog: Boolean,
    onResetSecondDismiss: () -> Unit,
    onResetSecondConfirm: () -> Unit,
    showReloadDialog: Boolean,
    onReloadDismiss: () -> Unit,
    onReloadConfirm: () -> Unit,
    message: String?,
    onMessageDismiss: () -> Unit,
    onBack: () -> Unit = {},
) {
    val topBarState = TopBarState(titleRes = R.string.prefs_database_title, showBack = true, onBack = onBack)

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
        item {
            SettingsActionRow(
                titleRes = R.string.storage_definer_title,
                summary = storageSummary,
                iconRes = R.drawable.ic_folder_lock,
                onClick = onOpenStorage,
            )
        }
        item { SettingsSectionHeader(R.string.pref_database_title) }
        item {
            SettingsActionRow(
                titleRes = R.string.prefs_database_import_title,
                summaryRes = R.string.prefs_database_import_summary,
                iconRes = R.drawable.ic_database_import,
                onClick = onImport,
            )
        }
        item {
            SettingsActionRow(
                titleRes = R.string.prefs_database_export_title,
                summaryRes = R.string.prefs_database_export_summary,
                iconRes = R.drawable.ic_database_export,
                onClick = onExportClick,
            )
        }
        item {
            SettingsActionRow(
                titleRes = R.string.prefs_database_reload_title,
                summaryRes = R.string.prefs_database_reload_summary,
                iconRes = R.drawable.ic_database_reload,
                onClick = onReloadClick,
            )
        }
        item {
            SettingsActionRow(
                titleRes = R.string.prefs_database_reset_title,
                summaryRes = R.string.prefs_database_reset_summary,
                iconRes = R.drawable.ic_database_reset,
                onClick = onResetClick,
            )
        }
    }

    if (showExportDialog) {
        DatabaseExportDialog(
            onDismiss = onExportDismiss,
            onExport = onExportConfirm,
        )
    }

    if (showResetFirstDialog) {
        AlertDialog(
            onDismissRequest = onResetFirstDismiss,
            title = { Text(stringResource(R.string.dialog_warning)) },
            text = { Text(stringResource(R.string.database_reset_warning1)) },
            confirmButton = {
                TextButton(onClick = onResetFirstConfirm) { Text(stringResource(R.string.dialog_yes)) }
            },
            dismissButton = {
                TextButton(onClick = onResetFirstDismiss) {
                    Text(stringResource(R.string.dialog_no))
                }
            },
        )
    }

    if (showResetSecondDialog) {
        AlertDialog(
            onDismissRequest = onResetSecondDismiss,
            title = { Text(stringResource(R.string.dialog_warning)) },
            text = { Text(stringResource(R.string.database_reset_warning2)) },
            confirmButton = {
                TextButton(onClick = onResetSecondConfirm) { Text(stringResource(R.string.dialog_yes)) }
            },
            dismissButton = {
                TextButton(onClick = onResetSecondDismiss) {
                    Text(stringResource(R.string.dialog_no))
                }
            },
        )
    }

    if (showReloadDialog) {
        AlertDialog(
            onDismissRequest = onReloadDismiss,
            title = { Text(stringResource(R.string.dialog_warning)) },
            text = { Text(stringResource(R.string.database_reload_warning)) },
            confirmButton = {
                TextButton(onClick = onReloadConfirm) { Text(stringResource(R.string.dialog_yes)) }
            },
            dismissButton = {
                TextButton(onClick = onReloadDismiss) {
                    Text(stringResource(R.string.dialog_no))
                }
            },
        )
    }

    message?.let {
        AlertDialog(
            onDismissRequest = onMessageDismiss,
            text = { Text(it) },
            confirmButton = {
                TextButton(onClick = onMessageDismiss) {
                    Text(stringResource(R.string.dialog_ok))
                }
            },
        )
    }
    } // Box
    } // Scaffold
}

@Composable
private fun DatabaseExportDialog(
    onDismiss: () -> Unit,
    onExport: (String) -> Unit,
) {
    val defaultPrefix = stringResource(R.string.default_database_export_file_name)
    var fileName by remember { mutableStateOf("${defaultPrefix}_${DateUtil().getTime()}") }
    var error by remember { mutableStateOf<String?>(null) }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(stringResource(R.string.database_export_title)) },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                OutlinedTextField(
                    value = fileName,
                    onValueChange = {
                        fileName = it
                        error = null
                    },
                    label = { Text(stringResource(R.string.dialog_file_name_hint)) },
                    singleLine = true,
                )
                error?.let {
                    Text(it, color = MaterialTheme.colorScheme.error, style = MaterialTheme.typography.bodySmall)
                }
            }
        },
        confirmButton = {
            val blankError = stringResource(R.string.database_file_name_blank_error)
            TextButton(
                onClick = {
                    val cleaned = fileName.trim()
                    if (cleaned.isBlank()) {
                        error = blankError
                    } else {
                        onExport(cleaned)
                    }
                },
            ) {
                Text(stringResource(R.string.dialog_export))
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text(stringResource(R.string.dialog_cancel))
            }
        },
    )
}

// -- Database helper functions --

internal fun storageSummary(context: Context): String {
    val root = BaseDocumentTreeUtil.getRoot(context)
    return if (BaseDocumentTreeUtil.isEnabled(context) && root != null && root.exists()) {
        root.uri.lastPathSegment ?: root.uri.getStem(context)
    } else {
        context.getString(R.string.storage_definer_summary)
    }
}
