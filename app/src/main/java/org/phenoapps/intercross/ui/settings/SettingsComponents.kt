package org.phenoapps.intercross.ui.settings

import android.content.SharedPreferences
import androidx.annotation.DrawableRes
import androidx.annotation.StringRes
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.RowScope
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Checkbox
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.RadioButton
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.MutableState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import org.phenoapps.intercross.R
import org.phenoapps.intercross.ui.theme.IntercrossPreviewTheme

@Composable
fun SettingsSectionHeader(@StringRes titleRes: Int) {
    Text(
        text = stringResource(titleRes),
        style = MaterialTheme.typography.titleSmall,
        color = MaterialTheme.colorScheme.primary,
        modifier = Modifier.padding(top = 8.dp, bottom = 2.dp),
    )
}

@Composable
fun SettingsSingleChoiceRow(
    @StringRes titleRes: Int,
    summary: String,
    @DrawableRes iconRes: Int,
    entries: List<String>,
    values: List<String>,
    selectedValue: String,
    onSelected: (String) -> Unit,
) {
    var showDialog by remember { mutableStateOf(false) }

    SettingsActionRow(
        titleRes = titleRes,
        summary = summary,
        iconRes = iconRes,
        onClick = { showDialog = true },
    )

    if (showDialog) {
        var pendingValue by remember(selectedValue) { mutableStateOf(selectedValue) }
        AlertDialog(
            onDismissRequest = { showDialog = false },
            title = { Text(stringResource(titleRes)) },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                    entries.forEachIndexed { index, entry ->
                        val value = values.getOrNull(index).orEmpty()
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clickable { pendingValue = value }
                                .padding(vertical = 8.dp),
                            verticalAlignment = Alignment.CenterVertically,
                        ) {
                            RadioButton(
                                selected = pendingValue == value,
                                onClick = { pendingValue = value },
                            )
                            Text(entry, modifier = Modifier.padding(start = 8.dp))
                        }
                    }
                }
            },
            confirmButton = {
                TextButton(
                    onClick = {
                        onSelected(pendingValue)
                        showDialog = false
                    },
                ) {
                    Text(stringResource(R.string.dialog_save))
                }
            },
            dismissButton = {
                TextButton(onClick = { showDialog = false }) {
                    Text(stringResource(R.string.dialog_cancel))
                }
            },
        )
    }
}

@Composable
fun SettingsGroupedMultiChoiceRow(
    @StringRes titleRes: Int,
    summary: String,
    @DrawableRes iconRes: Int,
    groups: List<Pair<String, List<Pair<String, String>>>>,
    selectedValues: Set<String>,
    onSelected: (Set<String>) -> Unit,
) {
    var showDialog by remember { mutableStateOf(false) }

    SettingsActionRow(
        titleRes = titleRes,
        summary = summary,
        iconRes = iconRes,
        onClick = { showDialog = true },
    )

    if (showDialog) {
        var pendingValues by remember(selectedValues) { mutableStateOf(selectedValues) }
        AlertDialog(
            onDismissRequest = { showDialog = false },
            title = { Text(stringResource(titleRes)) },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                    groups.forEach { (groupName, options) ->
                        Text(
                            text = groupName,
                            style = MaterialTheme.typography.titleSmall,
                            color = MaterialTheme.colorScheme.primary,
                            modifier = Modifier.padding(top = 8.dp),
                        )
                        options.forEach { (value, label) ->
                            val checked = value in pendingValues
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .clickable {
                                        pendingValues = if (checked) {
                                            pendingValues - value
                                        } else {
                                            pendingValues + value
                                        }
                                    }
                                    .padding(vertical = 6.dp),
                                verticalAlignment = Alignment.CenterVertically,
                            ) {
                                Checkbox(
                                    checked = checked,
                                    onCheckedChange = { isChecked ->
                                        pendingValues = if (isChecked) {
                                            pendingValues + value
                                        } else {
                                            pendingValues - value
                                        }
                                    },
                                )
                                Text(label, modifier = Modifier.padding(start = 8.dp))
                            }
                        }
                    }
                }
            },
            confirmButton = {
                TextButton(
                    onClick = {
                        onSelected(pendingValues)
                        showDialog = false
                    },
                ) {
                    Text(stringResource(R.string.dialog_save))
                }
            },
            dismissButton = {
                TextButton(onClick = { showDialog = false }) {
                    Text(stringResource(R.string.dialog_cancel))
                }
            },
        )
    }
}

@Composable
fun TextPreferenceRow(
    @StringRes titleRes: Int,
    summary: String,
    @DrawableRes iconRes: Int,
    value: String,
    onSave: (String) -> Unit,
    @StringRes messageRes: Int? = null,
    keyboardType: KeyboardType = KeyboardType.Text,
) {
    var showDialog by remember { mutableStateOf(false) }
    SettingsActionRow(
        titleRes = titleRes,
        summary = summary,
        iconRes = iconRes,
        onClick = { showDialog = true },
    )

    if (showDialog) {
        var pendingValue by remember(value) { mutableStateOf(value) }
        AlertDialog(
            onDismissRequest = { showDialog = false },
            title = { Text(stringResource(titleRes)) },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                    messageRes?.let {
                        Text(
                            text = stringResource(it),
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                        )
                    }
                    OutlinedTextField(
                        value = pendingValue,
                        onValueChange = { pendingValue = it },
                        singleLine = true,
                        keyboardOptions = KeyboardOptions(keyboardType = keyboardType),
                        modifier = Modifier.fillMaxWidth(),
                    )
                }
            },
            confirmButton = {
                TextButton(
                    onClick = {
                        onSave(pendingValue)
                        showDialog = false
                    },
                ) {
                    Text(stringResource(R.string.dialog_save))
                }
            },
            dismissButton = {
                TextButton(onClick = { showDialog = false }) {
                    Text(stringResource(R.string.dialog_cancel))
                }
            },
        )
    }
}

@Composable
fun SettingsSwitchRow(
    @StringRes titleRes: Int,
    @StringRes summaryRes: Int? = null,
    @DrawableRes iconRes: Int,
    checked: Boolean,
    onCheckedChange: (Boolean) -> Unit,
) {
    SettingsPreferenceCard(onClick = { onCheckedChange(!checked) }) {
        SettingsIcon(iconRes)
        SettingsText(
            title = stringResource(titleRes),
            summary = summaryRes?.let { stringResource(it) },
            modifier = Modifier.weight(1f),
        )
        Switch(checked = checked, onCheckedChange = onCheckedChange)
    }
}

@Composable
fun SettingsActionRow(
    @StringRes titleRes: Int,
    @StringRes summaryRes: Int? = null,
    summary: String? = null,
    @DrawableRes iconRes: Int,
    onClick: () -> Unit,
) {
    SettingsActionRow(
        title = stringResource(titleRes),
        summary = summary ?: summaryRes?.let { stringResource(it) },
        iconRes = iconRes,
        onClick = onClick,
    )
}

@Composable
fun SettingsActionRow(
    title: String,
    summary: String? = null,
    @DrawableRes iconRes: Int,
    onClick: () -> Unit,
) {
    SettingsPreferenceCard(onClick = onClick) {
        SettingsIcon(iconRes)
        SettingsText(
            title = title,
            summary = summary,
            modifier = Modifier.weight(1f),
        )
    }
}

@Composable
fun SettingsPreferenceCard(
    onClick: () -> Unit,
    content: @Composable RowScope.() -> Unit,
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick)
            .padding(16.dp),
        horizontalArrangement = Arrangement.spacedBy(14.dp),
        verticalAlignment = Alignment.CenterVertically,
        content = content,
    )
}

@Composable
fun SettingsIcon(@DrawableRes iconRes: Int) {
    Icon(
        painter = painterResource(iconRes),
        contentDescription = null,
        tint = MaterialTheme.colorScheme.primary,
    )
}

@Composable
fun SettingsText(
    title: String,
    summary: String?,
    modifier: Modifier = Modifier,
) {
    Column(modifier = modifier) {
        Text(
            text = title,
            style = MaterialTheme.typography.titleMedium,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
        )
        if (!summary.isNullOrBlank()) {
            Text(
                text = summary,
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                maxLines = 2,
                overflow = TextOverflow.Ellipsis,
            )
        }
    }
}

@Composable
fun rememberBooleanPreference(
    prefs: SharedPreferences,
    key: String,
    defaultValue: Boolean,
): MutableState<Boolean> = remember(prefs, key) {
    mutableStateOf(prefs.getBoolean(key, defaultValue))
}


// ─── Previews ───────────────────────────────────────────────────────────────────

@Preview(showBackground = true, name = "SettingsSectionHeader")
@Composable
private fun SettingsSectionHeaderPreview() {
    IntercrossPreviewTheme {
        SettingsSectionHeader(titleRes = R.string.prefs_profile_title)
    }
}

@Preview(showBackground = true, name = "SettingsSingleChoiceRow - Collapsed")
@Preview(showBackground = true, name = "SettingsSingleChoiceRow - Large Font", fontScale = 1.5f)
@Composable
private fun SettingsSingleChoiceRowCollapsedPreview() {
    IntercrossPreviewTheme {
        SettingsSingleChoiceRow(
            titleRes = R.string.preferences_brapi_version,
            summary = "v2.0",
            iconRes = R.drawable.ic_pref_brapi_version,
            entries = listOf("v1.3", "v2.0"),
            values = listOf("v1.3", "v2.0"),
            selectedValue = "v2.0",
            onSelected = {},
        )
    }
}

@Preview(showBackground = true, name = "SettingsSingleChoiceRow - Expanded")
@Composable
private fun SettingsSingleChoiceRowExpandedPreview() {
    IntercrossPreviewTheme {
        // Render the dialog content directly to show expanded state
        Column {
            SettingsSingleChoiceRow(
                titleRes = R.string.preferences_brapi_version,
                summary = "v2.0",
                iconRes = R.drawable.ic_pref_brapi_version,
                entries = listOf("v1.3", "v2.0"),
                values = listOf("v1.3", "v2.0"),
                selectedValue = "v2.0",
                onSelected = {},
            )
            // Show radio options inline to represent expanded state visually
            Column(modifier = Modifier.padding(16.dp)) {
                listOf("v1.3", "v2.0").forEach { entry ->
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(vertical = 8.dp),
                        verticalAlignment = Alignment.CenterVertically,
                    ) {
                        RadioButton(
                            selected = entry == "v2.0",
                            onClick = {},
                        )
                        Text(entry, modifier = Modifier.padding(start = 8.dp))
                    }
                }
            }
        }
    }
}

@Preview(showBackground = true, name = "TextPreferenceRow")
@Composable
private fun TextPreferenceRowPreview() {
    IntercrossPreviewTheme {
        TextPreferenceRow(
            titleRes = R.string.brapi_display_name,
            summary = "My BrAPI Server",
            iconRes = R.drawable.ic_pref_brapi_name,
            value = "My BrAPI Server",
            onSave = {},
        )
    }
}

@Preview(showBackground = true, name = "SettingsSwitchRow")
@Preview(showBackground = true, name = "SettingsSwitchRow - Large Font", fontScale = 1.5f)
@Composable
private fun SettingsSwitchRowPreview() {
    IntercrossPreviewTheme {
        SettingsSwitchRow(
            titleRes = R.string.preferences_brapi_enable_title,
            summaryRes = R.string.preferences_brapi_enable_summary,
            iconRes = R.drawable.ic_adv_brapi,
            checked = true,
            onCheckedChange = {},
        )
    }
}

@Preview(showBackground = true, name = "SettingsActionRow")
@Preview(showBackground = true, name = "SettingsActionRow - Large Font", fontScale = 1.5f)
@Composable
private fun SettingsActionRowPreview() {
    IntercrossPreviewTheme {
        SettingsActionRow(
            titleRes = R.string.prefs_database_export_title,
            summaryRes = R.string.prefs_database_export_summary,
            iconRes = R.drawable.ic_database_export,
            onClick = {},
        )
    }
}

@Preview(showBackground = true, name = "SettingsPreferenceCard")
@Composable
private fun SettingsPreferenceCardPreview() {
    IntercrossPreviewTheme {
        SettingsPreferenceCard(onClick = {}) {
            SettingsIcon(R.drawable.ic_account_circle_black_24dp)
            SettingsText(
                title = "Sample Preference",
                summary = "Tap to change this setting",
                modifier = Modifier.weight(1f),
            )
        }
    }
}

@Preview(showBackground = true, name = "SettingsIcon")
@Composable
private fun SettingsIconPreview() {
    IntercrossPreviewTheme {
        SettingsIcon(R.drawable.ic_database)
    }
}

@Preview(showBackground = true, name = "SettingsText")
@Composable
private fun SettingsTextPreview() {
    IntercrossPreviewTheme {
        SettingsText(
            title = "Appearance",
            summary = "Change the look and feel of the app",
        )
    }
}
