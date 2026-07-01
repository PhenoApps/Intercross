package org.phenoapps.intercross.ui.settings

import android.content.SharedPreferences
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
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.RadioButton
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringArrayResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.core.content.edit
import org.phenoapps.intercross.R
import androidx.compose.foundation.layout.Box
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.ui.res.painterResource
import org.phenoapps.intercross.ui.app.TopBarState
import org.phenoapps.intercross.ui.app.rememberPrefs
import org.phenoapps.intercross.ui.theme.AppTheme
import org.phenoapps.intercross.util.KeyUtil
import java.util.Locale

@Composable
fun ProfileSettingsRoute(
    onBack: () -> Unit = {},
) {
    val (prefs, keyUtil) = rememberPrefs()
    var persons by remember { mutableStateOf(loadProfilePersons(prefs, keyUtil)) }
    var selectedPerson by remember { mutableStateOf(selectedProfilePerson(prefs, keyUtil)) }
    var verificationInterval by remember {
        mutableStateOf(prefs.getString(keyUtil.personVerificationIntervalKey, "0") ?: "0")
    }
    var showSelectDialog by remember { mutableStateOf(false) }
    var showAddDialog by remember { mutableStateOf(false) }
    var showManageDialog by remember { mutableStateOf(false) }
    var showResetDialog by remember { mutableStateOf(false) }

    fun refreshProfileState() {
        persons = loadProfilePersons(prefs, keyUtil)
        selectedPerson = selectedProfilePerson(prefs, keyUtil)
        verificationInterval = prefs.getString(keyUtil.personVerificationIntervalKey, "0") ?: "0"
    }

    fun selectPerson(person: String) {
        setProfilePerson(prefs, keyUtil, person)
        refreshProfileState()
    }

    LaunchedEffect(prefs, keyUtil) {
        migrateLegacyProfilePersonIfNeeded(prefs, keyUtil)
        prefs.edit { putLong(keyUtil.lastTimeAskedKey, System.nanoTime()) }
        refreshProfileState()
    }

    val personSummary = profilePersonSummary(prefs, keyUtil).ifBlank { stringResource(R.string.profile_person_not_set) }
    val personsListSummary = if (persons.isEmpty()) stringResource(R.string.profile_person_not_set) else persons.joinToString()
    val nameRequiredText = stringResource(R.string.profile_person_name_required)

    val intervalValues = stringArrayResource(R.array.preferences_profile_verification_interval_values)
    val intervalEntries = stringArrayResource(R.array.preferences_profile_verification_interval_entries)
    val intervalSummary = intervalEntries.getOrNull(intervalValues.indexOf(verificationInterval))
        ?: intervalEntries.firstOrNull()
        ?: verificationInterval

    ProfileSettingsScreenContent(
        personSummary = personSummary,
        personsListSummary = personsListSummary,
        intervalSummary = intervalSummary,
        intervalEntries = intervalEntries.toList(),
        intervalValues = intervalValues.toList(),
        verificationInterval = verificationInterval,
        onSelectPerson = {
            if (persons.isEmpty()) showAddDialog = true else showSelectDialog = true
        },
        onAddPerson = { showAddDialog = true },
        onManagePersons = { showManageDialog = true },
        onIntervalChange = { selected ->
            verificationInterval = selected
            prefs.edit { putString(keyUtil.personVerificationIntervalKey, selected) }
        },
        onReset = { showResetDialog = true },
        showSelectDialog = showSelectDialog,
        onSelectDialogDismiss = { showSelectDialog = false },
        onSelectDialogClear = {
            selectPerson("")
            showSelectDialog = false
        },
        onSelectDialogSave = {
            selectPerson(it)
            showSelectDialog = false
        },
        selectedPerson = selectedPerson,
        persons = persons,
        showAddDialog = showAddDialog,
        onAddDialogDismiss = { showAddDialog = false },
        onAddDialogAdd = { person ->
            if (person.isBlank()) {
                nameRequiredText
            } else {
                val updated = (loadProfilePersons(prefs, keyUtil) + person)
                    .distinctBy { it.lowercase(Locale.getDefault()) }
                    .sortedBy { it.lowercase(Locale.getDefault()) }
                saveProfilePersons(prefs, keyUtil, updated)
                selectPerson(person)
                showAddDialog = false
                null
            }
        },
        showManageDialog = showManageDialog,
        onManageDialogDismiss = { showManageDialog = false },
        onManageDialogRemove = { person ->
            val updated = loadProfilePersons(prefs, keyUtil)
                .filterNot { it.equals(person, ignoreCase = true) }
            saveProfilePersons(prefs, keyUtil, updated)
            if (selectedProfilePerson(prefs, keyUtil).equals(person, ignoreCase = true)) {
                setProfilePerson(prefs, keyUtil, updated.firstOrNull().orEmpty())
            }
            refreshProfileState()
        },
        showResetDialog = showResetDialog,
        onResetDialogDismiss = { showResetDialog = false },
        onResetDialogConfirm = {
            prefs.edit {
                putStringSet(keyUtil.profilePersonListKey, emptySet())
                putString(keyUtil.profileSelectedPersonKey, "")
                putString(keyUtil.personFirstNameKey, "")
                putString(keyUtil.personLastNameKey, "")
                putBoolean(keyUtil.profileShowPersonInputKey, false)
            }
            refreshProfileState()
            showResetDialog = false
        },
        onBack = onBack,
    )
}

@androidx.compose.ui.tooling.preview.Preview(showBackground = true, name = "ProfileSettings - Default")
@Composable
internal fun ProfileSettingsPreview() {
    org.phenoapps.intercross.ui.theme.IntercrossPreviewTheme {
        ProfileSettingsScreenContent(
            personSummary = "Jane",
            personsListSummary = "Jane, Bob, Alice",
            intervalSummary = "Every 24 hours",
            intervalEntries = listOf("Never", "Every 12 hours", "Every 24 hours", "Always"),
            intervalValues = listOf("0", "12", "24", "-1"),
            verificationInterval = "24",
            onSelectPerson = {},
            onAddPerson = {},
            onManagePersons = {},
            onIntervalChange = {},
            onReset = {},
            showSelectDialog = false,
            onSelectDialogDismiss = {},
            onSelectDialogClear = {},
            onSelectDialogSave = {},
            selectedPerson = "Jane",
            persons = listOf("Jane", "Bob", "Alice"),
            showAddDialog = false,
            onAddDialogDismiss = {},
            onAddDialogAdd = { null },
            showManageDialog = false,
            onManageDialogDismiss = {},
            onManageDialogRemove = {},
            showResetDialog = false,
            onResetDialogDismiss = {},
            onResetDialogConfirm = {},
        )
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ProfileSettingsScreenContent(
    personSummary: String,
    personsListSummary: String,
    intervalSummary: String,
    intervalEntries: List<String>,
    intervalValues: List<String>,
    verificationInterval: String,
    onSelectPerson: () -> Unit,
    onAddPerson: () -> Unit,
    onManagePersons: () -> Unit,
    onIntervalChange: (String) -> Unit,
    onReset: () -> Unit,
    showSelectDialog: Boolean,
    onSelectDialogDismiss: () -> Unit,
    onSelectDialogClear: () -> Unit,
    onSelectDialogSave: (String) -> Unit,
    selectedPerson: String,
    persons: List<String>,
    showAddDialog: Boolean,
    onAddDialogDismiss: () -> Unit,
    onAddDialogAdd: (String) -> String?,
    showManageDialog: Boolean,
    onManageDialogDismiss: () -> Unit,
    onManageDialogRemove: (String) -> Unit,
    showResetDialog: Boolean,
    onResetDialogDismiss: () -> Unit,
    onResetDialogConfirm: () -> Unit,
    onBack: () -> Unit = {},
) {
    val topBarState = TopBarState(titleRes = R.string.prefs_profile_title, showBack = true, onBack = onBack)

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
                titleRes = R.string.profile_person,
                summary = personSummary,
                iconRes = R.drawable.ic_prefs_profile_person,
                onClick = onSelectPerson,
            )
        }
        item {
            SettingsActionRow(
                titleRes = R.string.profile_add_person,
                iconRes = R.drawable.ic_add_black_24dp,
                onClick = onAddPerson,
            )
        }
        item {
            SettingsActionRow(
                titleRes = R.string.profile_manage_persons,
                summary = personsListSummary,
                iconRes = R.drawable.account_group_outline,
                onClick = onManagePersons,
            )
        }
        item {
            SettingsSingleChoiceRow(
                titleRes = R.string.profile_verification_interval,
                summary = intervalSummary,
                iconRes = R.drawable.ic_hours_24,
                entries = intervalEntries,
                values = intervalValues,
                selectedValue = verificationInterval,
                onSelected = onIntervalChange,
            )
        }
        item {
            SettingsActionRow(
                titleRes = R.string.profile_reset,
                iconRes = R.drawable.ic_prefs_profile_delete,
                onClick = onReset,
            )
        }
    }

    if (showSelectDialog) {
        ProfileSelectPersonDialog(
            persons = persons,
            selectedPerson = selectedPerson,
            onDismiss = onSelectDialogDismiss,
            onClear = onSelectDialogClear,
            onSave = onSelectDialogSave,
        )
    }

    if (showAddDialog) {
        ProfileAddPersonDialog(
            onDismiss = onAddDialogDismiss,
            onAdd = onAddDialogAdd,
        )
    }

    if (showManageDialog) {
        ProfileManagePersonsDialog(
            persons = persons,
            selectedPerson = selectedPerson,
            onDismiss = onManageDialogDismiss,
            onRemove = onManageDialogRemove,
        )
    }

    if (showResetDialog) {
        AlertDialog(
            onDismissRequest = onResetDialogDismiss,
            title = { Text(stringResource(R.string.profile_reset)) },
            text = { Text(stringResource(R.string.dialog_confirm)) },
            confirmButton = {
                TextButton(onClick = onResetDialogConfirm) {
                    Text(stringResource(R.string.dialog_yes))
                }
            },
            dismissButton = {
                TextButton(onClick = onResetDialogDismiss) {
                    Text(stringResource(R.string.dialog_no))
                }
            },
        )
    }
    } // Box
    } // Scaffold
}

@Composable
private fun ProfileSelectPersonDialog(
    persons: List<String>,
    selectedPerson: String,
    onDismiss: () -> Unit,
    onClear: () -> Unit,
    onSave: (String) -> Unit,
) {
    var pendingSelection by remember(selectedPerson, persons) {
        mutableStateOf(persons.firstOrNull { it.equals(selectedPerson, ignoreCase = true) }.orEmpty())
    }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(stringResource(R.string.profile_person_select_title)) },
        text = {
            LazyColumn(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                items(persons, key = { it }) { person ->
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clickable { pendingSelection = person }
                            .padding(vertical = 8.dp),
                        verticalAlignment = Alignment.CenterVertically,
                    ) {
                        RadioButton(
                            selected = person.equals(pendingSelection, ignoreCase = true),
                            onClick = { pendingSelection = person },
                        )
                        Text(person, modifier = Modifier.padding(start = 8.dp))
                    }
                }
            }
        },
        confirmButton = {
            TextButton(
                enabled = pendingSelection.isNotBlank(),
                onClick = { onSave(pendingSelection) },
            ) {
                Text(stringResource(R.string.dialog_save))
            }
        },
        dismissButton = {
            Row {
                TextButton(onClick = onClear) {
                    Text(stringResource(R.string.Clear))
                }
                TextButton(onClick = onDismiss) {
                    Text(stringResource(R.string.dialog_cancel))
                }
            }
        },
    )
}

@Composable
private fun ProfileAddPersonDialog(
    onDismiss: () -> Unit,
    onAdd: (String) -> String?,
) {
    var firstName by remember { mutableStateOf("") }
    var lastName by remember { mutableStateOf("") }
    var error by remember { mutableStateOf<String?>(null) }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(stringResource(R.string.profile_add_person)) },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                OutlinedTextField(
                    value = firstName,
                    onValueChange = {
                        firstName = it
                        error = null
                    },
                    label = { Text(stringResource(R.string.profile_name_first)) },
                    singleLine = true,
                )
                OutlinedTextField(
                    value = lastName,
                    onValueChange = {
                        lastName = it
                        error = null
                    },
                    label = { Text(stringResource(R.string.profile_name_last)) },
                    singleLine = true,
                )
                error?.let {
                    Text(it, color = MaterialTheme.colorScheme.error, style = MaterialTheme.typography.bodySmall)
                }
            }
        },
        confirmButton = {
            TextButton(
                onClick = {
                    val message = onAdd("$firstName $lastName".trim())
                    if (message != null) error = message
                },
            ) {
                Text(stringResource(R.string.add))
            }
        },
        dismissButton = {
            Row {
                TextButton(
                    onClick = {
                        firstName = ""
                        lastName = ""
                        error = null
                    },
                ) {
                    Text(stringResource(R.string.Clear))
                }
                TextButton(onClick = onDismiss) {
                    Text(stringResource(R.string.dialog_cancel))
                }
            }
        },
    )
}

@Composable
private fun ProfileManagePersonsDialog(
    persons: List<String>,
    selectedPerson: String,
    onDismiss: () -> Unit,
    onRemove: (String) -> Unit,
) {
    var pendingSelection by remember(selectedPerson, persons) {
        mutableStateOf(persons.firstOrNull { it.equals(selectedPerson, ignoreCase = true) } ?: persons.firstOrNull().orEmpty())
    }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(stringResource(R.string.profile_person_manage_title)) },
        text = {
            if (persons.isEmpty()) {
                Text(stringResource(R.string.profile_person_not_set))
            } else {
                LazyColumn(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                    items(persons, key = { it }) { person ->
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clickable { pendingSelection = person }
                                .padding(vertical = 8.dp),
                            verticalAlignment = Alignment.CenterVertically,
                        ) {
                            RadioButton(
                                selected = person.equals(pendingSelection, ignoreCase = true),
                                onClick = { pendingSelection = person },
                            )
                            Text(person, modifier = Modifier.padding(start = 8.dp))
                        }
                    }
                }
            }
        },
        confirmButton = {
            TextButton(
                enabled = pendingSelection.isNotBlank(),
                onClick = { onRemove(pendingSelection) },
            ) {
                Text(stringResource(R.string.profile_person_remove))
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text(stringResource(R.string.dialog_cancel))
            }
        },
    )
}

// -- Profile helper functions --

internal fun profilePersonSummary(prefs: SharedPreferences, keyUtil: KeyUtil): String {
    val selected = selectedProfilePerson(prefs, keyUtil)
    if (selected.isNotBlank()) return selected

    val first = prefs.getString(keyUtil.personFirstNameKey, "").orEmpty()
    val last = prefs.getString(keyUtil.personLastNameKey, "").orEmpty()
    return "$first $last".trim()
}

internal fun selectedProfilePerson(prefs: SharedPreferences, keyUtil: KeyUtil): String =
    prefs.getString(keyUtil.profileSelectedPersonKey, "").orEmpty().trim()

internal fun loadProfilePersons(prefs: SharedPreferences, keyUtil: KeyUtil): List<String> {
    return prefs.getStringSet(keyUtil.profilePersonListKey, emptySet())
        .orEmpty()
        .map { it.trim() }
        .filter { it.isNotBlank() }
        .distinctBy { it.lowercase(Locale.getDefault()) }
        .sortedBy { it.lowercase(Locale.getDefault()) }
}

internal fun saveProfilePersons(
    prefs: SharedPreferences,
    keyUtil: KeyUtil,
    persons: List<String>,
) {
    prefs.edit {
        putStringSet(keyUtil.profilePersonListKey, persons.toSet())
    }
}

internal fun setProfilePerson(
    prefs: SharedPreferences,
    keyUtil: KeyUtil,
    person: String,
) {
    val cleaned = person.trim()
    val (first, last) = if (cleaned.isBlank()) {
        "" to ""
    } else {
        val tokens = cleaned.split("\\s+".toRegex(), limit = 2)
        tokens.firstOrNull().orEmpty() to tokens.getOrNull(1).orEmpty()
    }

    prefs.edit {
        putString(keyUtil.profileSelectedPersonKey, cleaned)
        putString(keyUtil.personFirstNameKey, first)
        putString(keyUtil.personLastNameKey, last)
    }
}

internal fun migrateLegacyProfilePersonIfNeeded(
    prefs: SharedPreferences,
    keyUtil: KeyUtil,
) {
    val first = prefs.getString(keyUtil.personFirstNameKey, "").orEmpty().trim()
    val last = prefs.getString(keyUtil.personLastNameKey, "").orEmpty().trim()
    val legacyPerson = "$first $last".trim()

    if (legacyPerson.isBlank()) return

    val currentPersons = loadProfilePersons(prefs, keyUtil).toMutableList()
    if (currentPersons.none { it.equals(legacyPerson, ignoreCase = true) }) {
        currentPersons.add(legacyPerson)
        saveProfilePersons(prefs, keyUtil, currentPersons)
    }

    if (selectedProfilePerson(prefs, keyUtil).isBlank()) {
        setProfilePerson(prefs, keyUtil, legacyPerson)
    }
}
