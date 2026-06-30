package org.phenoapps.intercross.ui.settings

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Card
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
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.lifecycle.asFlow
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import org.phenoapps.intercross.R
import org.phenoapps.intercross.data.MetaValuesRepository
import org.phenoapps.intercross.data.MetadataRepository
import org.phenoapps.intercross.data.models.Meta
import org.phenoapps.intercross.data.viewmodels.MetaValuesViewModel
import org.phenoapps.intercross.data.viewmodels.MetadataViewModel
import org.phenoapps.intercross.data.viewmodels.factory.MetaValuesViewModelFactory
import org.phenoapps.intercross.data.viewmodels.factory.MetadataViewModelFactory
import androidx.compose.foundation.layout.Box
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.ui.res.painterResource
import org.phenoapps.intercross.ui.app.TopBarState
import org.phenoapps.intercross.ui.app.rememberDatabase
import org.phenoapps.intercross.ui.components.defaultWishTypeEmoji
import org.phenoapps.intercross.ui.theme.AppTheme
import kotlinx.coroutines.launch
import java.util.Locale

@Composable
fun MetadataSettingsRoute(
    onBack: () -> Unit = {},
) {
    val db = rememberDatabase()
    val metadataModel: MetadataViewModel = viewModel(factory = MetadataViewModelFactory(MetadataRepository.getInstance(db.metadataDao())))
    val metaValuesModel: MetaValuesViewModel = viewModel(factory = MetaValuesViewModelFactory(MetaValuesRepository.getInstance(db.metaValuesDao())))
    val metadata by metadataModel.metadata.asFlow().collectAsStateWithLifecycle(initialValue = emptyList())
    val metaValues by metaValuesModel.metaValues.asFlow().collectAsStateWithLifecycle(initialValue = emptyList())
    val scope = rememberCoroutineScope()
    var editingMeta by remember { mutableStateOf<Meta?>(null) }
    var showEditor by remember { mutableStateOf(false) }
    var pendingDelete by remember { mutableStateOf<Meta?>(null) }

    MetadataSettingsScreenContent(
        metadata = metadata,
        onAddClick = {
            editingMeta = null
            showEditor = true
        },
        onEditClick = { meta ->
            editingMeta = meta
            showEditor = true
        },
        onDeleteClick = { meta -> pendingDelete = meta },
        showEditor = showEditor,
        editingMeta = editingMeta,
        onEditorDismiss = { showEditor = false },
        onEditorSave = { property, defaultValue, icon ->
            val current = editingMeta
            if (current == null) {
                scope.launch {
                    metadataModel.insert(Meta(property, defaultValue, icon))
                }
            } else {
                metadataModel.update(
                    current.copy(property = property, defaultValue = defaultValue, icon = icon),
                )
            }
            showEditor = false
        },
        pendingDelete = pendingDelete,
        onDeleteDismiss = { pendingDelete = null },
        onDeleteConfirm = {
            val meta = pendingDelete ?: return@MetadataSettingsScreenContent
            metadataModel.delete(meta)
            meta.id?.let { metaId ->
                metaValues
                    .filter { it.metaId == metaId.toInt() }
                    .forEach { metaValuesModel.delete(it) }
            }
            pendingDelete = null
        },
        onBack = onBack,
    )
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun MetadataSettingsScreenContent(
    metadata: List<Meta>,
    onAddClick: () -> Unit,
    onEditClick: (Meta) -> Unit,
    onDeleteClick: (Meta) -> Unit,
    showEditor: Boolean,
    editingMeta: Meta?,
    onEditorDismiss: () -> Unit,
    onEditorSave: (String, Int?, String?) -> Unit,
    pendingDelete: Meta?,
    onDeleteDismiss: () -> Unit,
    onDeleteConfirm: () -> Unit,
    onBack: () -> Unit = {},
) {
    val topBarState = TopBarState(titleRes = R.string.dialog_metadata_manager_title, showBack = true, onBack = onBack)

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
        verticalArrangement = Arrangement.spacedBy(10.dp),
    ) {
        item {
            androidx.compose.material3.ElevatedCard(
                modifier = Modifier.fillMaxWidth(),
                shape = androidx.compose.foundation.shape.RoundedCornerShape(12.dp),
                colors = androidx.compose.material3.CardDefaults.elevatedCardColors(
                    containerColor = androidx.compose.ui.graphics.Color.White,
                ),
                elevation = androidx.compose.material3.CardDefaults.elevatedCardElevation(defaultElevation = 2.dp),
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(16.dp)
                        .then(Modifier.let { mod ->
                            mod
                        }),
                    horizontalArrangement = Arrangement.spacedBy(12.dp),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    SettingsIcon(R.drawable.ic_note_add_black_24dp)
                    Column(Modifier.weight(1f)) {
                        Text(stringResource(R.string.frag_new_metadata_title), style = MaterialTheme.typography.titleMedium)
                        Text(stringResource(R.string.prefs_behavior_create_metadata_summary), style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                    }
                    androidx.compose.material3.FilledTonalButton(
                        onClick = onAddClick,
                    ) {
                        Text(stringResource(R.string.metadata_add_button))
                    }
                }
            }
        }

        if (metadata.isEmpty()) {
            item {
                androidx.compose.foundation.layout.Box(
                    modifier = Modifier.fillMaxWidth().padding(48.dp),
                    contentAlignment = Alignment.Center,
                ) {
                    Text(
                        text = stringResource(R.string.fragment_settings_no_metadata_exists),
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        style = MaterialTheme.typography.bodyMedium,
                    )
                }
            }
        }

        items(metadata.sortedBy { it.property.lowercase(Locale.getDefault()) }, key = { it.id ?: it.property }) { meta ->
            androidx.compose.material3.ElevatedCard(
                modifier = Modifier.fillMaxWidth(),
                shape = androidx.compose.foundation.shape.RoundedCornerShape(12.dp),
                colors = androidx.compose.material3.CardDefaults.elevatedCardColors(
                    containerColor = androidx.compose.ui.graphics.Color.White,
                ),
                elevation = androidx.compose.material3.CardDefaults.elevatedCardElevation(defaultElevation = 2.dp),
            ) {
                Row(
                    modifier = Modifier.padding(16.dp),
                    horizontalArrangement = Arrangement.spacedBy(12.dp),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    SettingsIcon(R.drawable.ic_update_black_24dp)
                    Column(Modifier.weight(1f)) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Text(meta.property, style = MaterialTheme.typography.titleMedium)
                            (meta.icon?.takeIf { it.isNotBlank() } ?: defaultWishTypeEmoji(meta.property))?.let {
                                Text(
                                    text = it,
                                    modifier = Modifier.padding(start = 8.dp),
                                    style = MaterialTheme.typography.titleMedium
                                )
                            }
                        }
                        Text(
                            text = stringResource(R.string.metadata_default_format, meta.defaultValue?.toString() ?: stringResource(R.string.metadata_null_placeholder)),
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                        )
                    }
                    androidx.compose.material3.IconButton(
                        onClick = { onEditClick(meta) },
                    ) {
                        androidx.compose.material3.Icon(
                            painter = androidx.compose.ui.res.painterResource(R.drawable.ic_edit),
                            contentDescription = stringResource(R.string.action_edit),
                            tint = MaterialTheme.colorScheme.primary,
                        )
                    }
                    androidx.compose.material3.IconButton(onClick = { onDeleteClick(meta) }) {
                        androidx.compose.material3.Icon(
                            painter = androidx.compose.ui.res.painterResource(R.drawable.ic_delete),
                            contentDescription = stringResource(R.string.delete),
                            tint = MaterialTheme.colorScheme.error,
                        )
                    }
                }
            }
        }
    }

    if (showEditor) {
        MetadataEditorDialog(
            initial = editingMeta,
            existing = metadata,
            onDismiss = onEditorDismiss,
            onSave = onEditorSave,
        )
    }

    pendingDelete?.let { meta ->
        AlertDialog(
            onDismissRequest = onDeleteDismiss,
            title = { Text(stringResource(R.string.frag_metadata_list_delete_dialog_title)) },
            text = { Text(stringResource(R.string.frag_metadata_confirm_delete_message)) },
            confirmButton = {
                TextButton(onClick = onDeleteConfirm) {
                    Text(stringResource(android.R.string.ok))
                }
            },
            dismissButton = {
                TextButton(onClick = onDeleteDismiss) {
                    Text(stringResource(android.R.string.cancel))
                }
            },
        )
    }
    } // Box
    } // Scaffold
}

@Composable
private fun MetadataEditorDialog(
    initial: Meta?,
    existing: List<Meta>,
    onDismiss: () -> Unit,
    onSave: (String, Int?, String?) -> Unit,
) {
    val propertyEmptyError = stringResource(R.string.dialog_metadata_property_must_not_be_empty)
    val propertyCommaError = stringResource(R.string.dialog_metadata_property_must_not_have_comma)
    val valueIntegerError = stringResource(R.string.dialog_metadata_value_must_be_integer_or_empty)
    val codeExistsError = stringResource(R.string.ErrorCodeExists)

    var property by remember(initial) { mutableStateOf(initial?.property.orEmpty()) }
    var defaultValue by remember(initial) { mutableStateOf(initial?.defaultValue?.toString().orEmpty()) }
    var selectedEmoji by remember(initial) { mutableStateOf(initial?.icon) }
    var error by remember { mutableStateOf<String?>(null) }

    val plantEmojis = listOf(
        "🌱", "🌿", "☘️", "🍀", "🍃", "🍂", "🍁", "🍄", "🌾", "💐",
        "🌷", "🌹", "🥀", "🌺", "🌸", "🌼", "🌻", "🍏", "🍎", "🍐",
        "🍊", "🍋", "🍌", "🍉", "🍇", "🍓", "🫐", "🍈", "🍒", "🍑",
        "🥭", "🍍", "🥥", "🥝", "🍅", "🍆", "🥑", "🥦", "🥬", "🥒",
        "🌶️", "🫑", "🌽", "🥕", "🫒", "🧄", "🧅", "🥔", "🍠"
    )

    AlertDialog(
        onDismissRequest = onDismiss,
        title = {
            Text(
                stringResource(
                    if (initial == null) R.string.frag_new_metadata_title
                    else R.string.frag_edit_metadata_title,
                ),
            )
        },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                OutlinedTextField(
                    value = property,
                    onValueChange = {
                        property = it
                        error = null
                    },
                    label = { Text(stringResource(R.string.dialog_metadata_property_hint)) },
                    placeholder = { Text(stringResource(R.string.metadata_placeholder_property)) },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth(),
                    shape = androidx.compose.foundation.shape.RoundedCornerShape(10.dp),
                )
                OutlinedTextField(
                    value = defaultValue,
                    onValueChange = {
                        defaultValue = it.filter { char -> char.isDigit() || char == '-' }
                        error = null
                    },
                    label = { Text(stringResource(R.string.dialog_metadata_default_hint)) },
                    placeholder = { Text(stringResource(R.string.metadata_placeholder_default)) },
                    singleLine = true,
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                    modifier = Modifier.fillMaxWidth(),
                    shape = androidx.compose.foundation.shape.RoundedCornerShape(10.dp),
                )

                Text(stringResource(R.string.metadata_choose_icon), style = MaterialTheme.typography.labelMedium)

                androidx.compose.foundation.lazy.grid.LazyVerticalGrid(
                    columns = androidx.compose.foundation.lazy.grid.GridCells.Adaptive(minSize = 40.dp),
                    modifier = Modifier.height(120.dp),
                    verticalArrangement = Arrangement.spacedBy(4.dp),
                    horizontalArrangement = Arrangement.spacedBy(4.dp)
                ) {
                    items(plantEmojis.size) { index ->
                        val emoji = plantEmojis[index]
                        val isSelected = selectedEmoji == emoji
                        androidx.compose.material3.Surface(
                            onClick = { selectedEmoji = if (isSelected) null else emoji },
                            shape = androidx.compose.foundation.shape.CircleShape,
                            color = if (isSelected) MaterialTheme.colorScheme.primaryContainer else Color.Transparent,
                            modifier = Modifier.size(40.dp)
                        ) {
                            Box(contentAlignment = Alignment.Center) {
                                Text(emoji, style = MaterialTheme.typography.headlineSmall)
                            }
                        }
                    }
                }

                error?.let {
                    Text(it, color = MaterialTheme.colorScheme.error, style = MaterialTheme.typography.bodySmall)
                }
            }
        },
        confirmButton = {
            TextButton(
                onClick = {
                    val cleanedProperty = property.trim()
                    val cleanedDefault = defaultValue.trim()
                    val parsedDefault = cleanedDefault.toIntOrNull()
                    error = when {
                        cleanedProperty.isBlank() -> propertyEmptyError
                        "," in cleanedProperty -> propertyCommaError
                        cleanedDefault.isNotBlank() && parsedDefault == null -> valueIntegerError
                        existing.any {
                            it.id != initial?.id && it.property.equals(cleanedProperty, ignoreCase = true)
                        } -> codeExistsError
                        else -> null
                    }

                    if (error == null) {
                        onSave(cleanedProperty, parsedDefault, selectedEmoji)
                    }
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
