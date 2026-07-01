package org.phenoapps.intercross.ui.settings

import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Card
import androidx.compose.material3.FilterChip
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
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
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.res.colorResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.withStyle
import androidx.compose.ui.unit.dp
import org.phenoapps.intercross.R
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.ui.res.painterResource
import org.phenoapps.intercross.ui.app.TopBarState
import org.phenoapps.intercross.ui.app.rememberPrefs
import org.phenoapps.intercross.ui.components.rememberQrBitmap
import org.phenoapps.intercross.ui.theme.AppTheme
import org.phenoapps.intercross.util.CrossIdSettings

enum class CrossIdMode {
    UUID,
    PATTERN,
    NONE,
}

@Composable
fun PatternSettingsRoute(
    onBack: () -> Unit = {},
) {
    val (prefs, _) = rememberPrefs()
    val savedSettings = remember { CrossIdSettings.load(prefs) }
    var mode by remember { mutableStateOf(CrossIdMode.UUID) }
    var prefix by remember { mutableStateOf("") }
    var suffix by remember { mutableStateOf("") }
    var number by remember { mutableStateOf("1") }
    var pad by remember { mutableStateOf("0") }
    var autoIncrement by remember { mutableStateOf(false) }
    var initialized by remember { mutableStateOf(false) }
    var showResetDialog by remember { mutableStateOf(false) }
    val uuidPreview = remember { java.util.UUID.randomUUID().toString() }

    fun persist(
        nextMode: CrossIdMode = mode,
        nextPrefix: String = prefix,
        nextSuffix: String = suffix,
        nextNumber: String = number,
        nextPad: String = pad,
        nextAutoIncrement: Boolean = autoIncrement,
    ) {
        val cleanNumber = nextNumber.toIntOrNull()?.coerceAtLeast(1) ?: 1
        val cleanPad = nextPad.toIntOrNull()?.coerceAtLeast(0) ?: 0
        CrossIdSettings.save(
            prefs,
            CrossIdSettings(
                isPattern = nextMode == CrossIdMode.PATTERN,
                isUUID = nextMode == CrossIdMode.UUID,
                isAutoIncrement = nextMode == CrossIdMode.PATTERN && nextAutoIncrement,
                pad = cleanPad,
                number = cleanNumber,
                prefix = nextPrefix,
                suffix = nextSuffix,
            ),
        )
    }

    LaunchedEffect(Unit) {
        if (!initialized) {
            mode = when {
                savedSettings.isUUID -> CrossIdMode.UUID
                savedSettings.isPattern -> CrossIdMode.PATTERN
                else -> CrossIdMode.NONE
            }
            prefix = savedSettings.prefix
            suffix = savedSettings.suffix
            number = savedSettings.number.coerceAtLeast(1).toString()
            pad = savedSettings.pad.coerceAtLeast(0).toString()
            autoIncrement = savedSettings.isAutoIncrement
            initialized = true
        }
    }

    val numericNumber = number.toIntOrNull()?.coerceAtLeast(1) ?: 1
    val numericPad = pad.toIntOrNull()?.coerceAtLeast(0) ?: 0
    val numberPreview = numericNumber.toString().padStart(numericPad, '0')
    val patternPreview = prefix + numberPreview + suffix

    PatternSettingsScreenContent(
        mode = mode,
        onModeChange = { mode = it; persist(nextMode = it) },
        uuidPreview = uuidPreview,
        patternPreview = patternPreview,
        prefix = prefix,
        onPrefixChange = { prefix = it; persist(nextPrefix = it) },
        suffix = suffix,
        onSuffixChange = { suffix = it; persist(nextSuffix = it) },
        numberPreview = numberPreview,
        autoIncrement = autoIncrement,
        onAutoIncrementChange = { autoIncrement = it; persist(nextAutoIncrement = it) },
        number = number,
        onNumberChange = { raw ->
            val cleaned = raw.filter { it.isDigit() }.take(7)
            number = cleaned
            persist(nextNumber = cleaned.ifBlank { "1" })
        },
        pad = pad,
        onPadChange = { raw ->
            val cleaned = raw.filter { it.isDigit() }.take(7)
            pad = cleaned
            persist(nextPad = cleaned.ifBlank { "0" })
        },
        showResetDialog = showResetDialog,
        onShowResetDialogChange = { showResetDialog = it },
        onResetConfirm = {
            number = "1"
            persist(nextNumber = "1")
            showResetDialog = false
        },
        onBack = onBack,
    )
}

@androidx.compose.ui.tooling.preview.Preview(showBackground = true, name = "PatternSettings - UUID")
@Composable
internal fun PatternSettingsUUIDPreview() {
    org.phenoapps.intercross.ui.theme.IntercrossPreviewTheme {
        PatternSettingsScreenContent(
            mode = CrossIdMode.UUID,
            onModeChange = {},
            uuidPreview = "a1b2c3d4-e5f6-7890-abcd-ef1234567890",
            patternPreview = "",
            prefix = "",
            onPrefixChange = {},
            suffix = "",
            onSuffixChange = {},
            numberPreview = "",
            autoIncrement = true,
            onAutoIncrementChange = {},
            number = "1",
            onNumberChange = {},
            pad = "4",
            onPadChange = {},
            showResetDialog = false,
            onShowResetDialogChange = {},
            onResetConfirm = {},
        )
    }
}

@androidx.compose.ui.tooling.preview.Preview(showBackground = true, name = "PatternSettings - Pattern")
@Composable
internal fun PatternSettingsPatternPreview() {
    org.phenoapps.intercross.ui.theme.IntercrossPreviewTheme {
        PatternSettingsScreenContent(
            mode = CrossIdMode.PATTERN,
            onModeChange = {},
            uuidPreview = "a1b2c3d4-e5f6-7890-abcd-ef1234567890",
            patternPreview = "CROSS-0001-A",
            prefix = "CROSS-",
            onPrefixChange = {},
            suffix = "-A",
            onSuffixChange = {},
            numberPreview = "0001",
            autoIncrement = true,
            onAutoIncrementChange = {},
            number = "1",
            onNumberChange = {},
            pad = "4",
            onPadChange = {},
            showResetDialog = false,
            onShowResetDialogChange = {},
            onResetConfirm = {},
        )
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun PatternSettingsScreenContent(
    mode: CrossIdMode,
    onModeChange: (CrossIdMode) -> Unit,
    uuidPreview: String,
    patternPreview: String,
    prefix: String,
    onPrefixChange: (String) -> Unit,
    suffix: String,
    onSuffixChange: (String) -> Unit,
    numberPreview: String,
    autoIncrement: Boolean,
    onAutoIncrementChange: (Boolean) -> Unit,
    number: String,
    onNumberChange: (String) -> Unit,
    pad: String,
    onPadChange: (String) -> Unit,
    showResetDialog: Boolean,
    onShowResetDialogChange: (Boolean) -> Unit,
    onResetConfirm: () -> Unit,
    onBack: () -> Unit = {},
) {
    val topBarState = TopBarState(titleRes = R.string.patterns_label, showBack = true, onBack = onBack)

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
        verticalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        item {
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                CrossIdMode.entries.forEach { option ->
                    FilterChip(
                        selected = mode == option,
                        onClick = { onModeChange(option) },
                        label = {
                            Text(
                                when (option) {
                                    CrossIdMode.UUID -> stringResource(R.string.uuid)
                                    CrossIdMode.PATTERN -> stringResource(R.string.template)
                                    CrossIdMode.NONE -> stringResource(R.string.none)
                                },
                            )
                        },
                    )
                }
            }
        }

        if (mode == CrossIdMode.UUID) {
            item {
                val previewText = uuidPreview
                val qrBitmap = rememberQrBitmap(previewText)
                Card(Modifier.fillMaxWidth()) {
                    Column(
                        modifier = Modifier.padding(16.dp).fillMaxWidth(),
                        verticalArrangement = Arrangement.spacedBy(10.dp),
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        Text(
                            stringResource(R.string.cross_id_preview),
                            style = MaterialTheme.typography.titleSmall,
                            modifier = Modifier.fillMaxWidth(),
                            textAlign = TextAlign.Start
                        )

                        if (qrBitmap != null) {
                            Box(
                                modifier = Modifier
                                    .size(100.dp)
                                    .background(Color.White, RoundedCornerShape(4.dp)),
                                contentAlignment = Alignment.Center
                            ) {
                                Image(
                                    bitmap = qrBitmap.asImageBitmap(),
                                    contentDescription = null,
                                    modifier = Modifier.size(92.dp)
                                )
                            }
                        }

                        Text(
                            text = previewText,
                            style = MaterialTheme.typography.headlineSmall,
                            fontFamily = FontFamily.Monospace,
                            color = MaterialTheme.colorScheme.onSurface,
                            textAlign = TextAlign.Center
                        )
                    }
                }
            }
        }

        if (mode == CrossIdMode.PATTERN) {
            item {
                PatternPreviewCard(
                    prefix = prefix,
                    number = numberPreview,
                    suffix = suffix,
                )
            }
            item {
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    OutlinedTextField(
                        value = prefix,
                        onValueChange = onPrefixChange,
                        label = { Text(stringResource(R.string.prefix)) },
                        modifier = Modifier.weight(1f),
                        singleLine = true,
                    )
                    OutlinedTextField(
                        value = suffix,
                        onValueChange = onSuffixChange,
                        label = { Text(stringResource(R.string.suffix)) },
                        modifier = Modifier.weight(1f),
                        singleLine = true,
                    )
                }
            }
            item { SettingsSectionHeader(R.string.number) }
            item {
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    FilterChip(
                        selected = autoIncrement,
                        onClick = { onAutoIncrementChange(true) },
                        label = { Text(stringResource(R.string.auto)) },
                    )
                    FilterChip(
                        selected = !autoIncrement,
                        onClick = { onAutoIncrementChange(false) },
                        label = { Text(stringResource(R.string.start_from)) },
                    )
                }
            }
            if (!autoIncrement) {
                item {
                    OutlinedTextField(
                        value = number,
                        onValueChange = onNumberChange,
                        label = { Text(stringResource(R.string.number_hint)) },
                        modifier = Modifier.fillMaxWidth(),
                        singleLine = true,
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                    )
                }
            }
            item {
                OutlinedTextField(
                    value = pad,
                    onValueChange = onPadChange,
                    label = { Text(stringResource(R.string.pad_length_hint)) },
                    modifier = Modifier.fillMaxWidth(),
                    singleLine = true,
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                )
            }
            if (autoIncrement) {
                item {
                    OutlinedButton(onClick = { onShowResetDialogChange(true) }) {
                        Text(stringResource(R.string.reset_cross_id_counter))
                    }
                }
            }
        }
    }

    if (showResetDialog) {
        AlertDialog(
            onDismissRequest = { onShowResetDialogChange(false) },
            title = { Text(stringResource(R.string.dialog_reset_cross_id_title)) },
            text = { Text(stringResource(R.string.dialog_reset_cross_id_message)) },
            confirmButton = {
                TextButton(onClick = onResetConfirm) {
                    Text(stringResource(android.R.string.ok))
                }
            },
            dismissButton = {
                TextButton(onClick = { onShowResetDialogChange(false) }) {
                    Text(stringResource(android.R.string.cancel))
                }
            },
        )
    }
    } // Box
    } // Scaffold
}

@Composable
private fun PatternPreviewCard(
    prefix: String,
    number: String,
    suffix: String,
) {
    val prefixColor = colorResource(R.color.patternPrefixColor)
    val suffixColor = colorResource(R.color.patternSuffixColor)
    val fallbackColor = MaterialTheme.colorScheme.onSurface
    val previewText = prefix + number + suffix
    val qrBitmap = rememberQrBitmap(previewText)
    
    val preview = buildAnnotatedString {
        withStyle(SpanStyle(color = if (prefix.isBlank()) fallbackColor else prefixColor)) {
            append(prefix)
        }
        withStyle(SpanStyle(color = fallbackColor)) {
            append(number)
        }
        withStyle(SpanStyle(color = if (suffix.isBlank()) fallbackColor else suffixColor)) {
            append(suffix)
        }
    }

    Card(Modifier.fillMaxWidth()) {
        Column(
            modifier = Modifier.padding(16.dp).fillMaxWidth(),
            verticalArrangement = Arrangement.spacedBy(10.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Text(
                stringResource(R.string.pattern),
                style = MaterialTheme.typography.titleSmall,
                modifier = Modifier.fillMaxWidth(),
                textAlign = TextAlign.Start
            )

            if (qrBitmap != null) {
                Box(
                    modifier = Modifier
                        .size(100.dp)
                        .background(Color.White, RoundedCornerShape(4.dp)),
                    contentAlignment = Alignment.Center
                ) {
                    Image(
                        bitmap = qrBitmap.asImageBitmap(),
                        contentDescription = null,
                        modifier = Modifier.size(92.dp)
                    )
                }
            }

            Text(
                text = preview,
                style = MaterialTheme.typography.headlineSmall,
                fontFamily = FontFamily.Monospace,
                textAlign = TextAlign.Center
            )
        }
    }
}
