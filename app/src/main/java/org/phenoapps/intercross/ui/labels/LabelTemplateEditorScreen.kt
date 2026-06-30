package org.phenoapps.intercross.ui.labels

import androidx.compose.foundation.Image
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.relocation.BringIntoViewRequester
import androidx.compose.foundation.relocation.bringIntoViewRequester
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ExposedDropdownMenuBox
import androidx.compose.material3.ExposedDropdownMenuAnchorType
import androidx.compose.material3.ExposedDropdownMenuDefaults
import androidx.compose.material3.FilterChip
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
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
import androidx.compose.ui.layout.ContentScale
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.TextRange
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.input.TextFieldValue
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import org.phenoapps.intercross.R
import org.phenoapps.intercross.data.viewmodels.LabelTemplateUiState
import org.phenoapps.intercross.util.LabelTemplateConfig
import org.phenoapps.intercross.util.LabelMediaType
import org.phenoapps.intercross.util.LabelTemplateType
import org.phenoapps.intercross.util.ZplStringReplacer
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.geometry.Offset
 import androidx.compose.ui.draw.clip
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.runtime.rememberCoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import android.content.Intent
import android.graphics.Bitmap
import androidx.core.content.FileProvider
import java.io.File
import java.io.FileOutputStream
import android.net.Uri
import androidx.compose.material3.AlertDialog
import org.phenoapps.intercross.ui.theme.AppTheme
import org.phenoapps.intercross.ui.theme.colors.DefaultAppColors
import org.phenoapps.intercross.ui.theme.toMaterialColorScheme
import org.phenoapps.intercross.ui.theme.toMaterialTypography
import org.phenoapps.intercross.ui.theme.typography.CompactTypography
import kotlin.math.roundToInt
import androidx.compose.ui.graphics.drawscope.Stroke

@Composable
fun LabelTemplateEditorScreen(
    uiState: LabelTemplateUiState,
    onConfigChange: (LabelTemplateConfig) -> Unit,
    onSelectSavedTemplate: (String, LabelTemplateType) -> Unit,
    onSaveTemplate: () -> Unit,
    onExportZpl: () -> Unit = {},
    onImportZpl: () -> Unit,
    onDetectDpi: () -> Unit,
    onRenderPreview: () -> Unit,
    onMessageShown: () -> Unit,
    topBarState: org.phenoapps.intercross.ui.app.TopBarState? = null,
) {
    val snackbarHostState = remember { SnackbarHostState() }
    val exportSuccessText = stringResource(R.string.export_success)
    val context = LocalContext.current
    val coroutineScope = rememberCoroutineScope()
    var showExportDialog by remember { mutableStateOf(false) }

    val suggestedFilename = (uiState.config.name.ifBlank { "label" } + ".zpl")

    val createDocumentLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.CreateDocument("text/plain")
    ) { uri: Uri? ->
        uri?.let {
            coroutineScope.launch {
                val zpl = uiState.config.toZpl()
                withContext(Dispatchers.IO) {
                    context.contentResolver.openOutputStream(uri)?.use { out ->
                        out.write(zpl.toByteArray())
                    }
                }
                snackbarHostState.showSnackbar(exportSuccessText)
            }
        }
    }

    LaunchedEffect(uiState.message) {
        uiState.message?.let {
            snackbarHostState.showSnackbar(it)
            onMessageShown()
        }
    }

    Scaffold(
        topBar = {
            if (topBarState != null) {
                @OptIn(ExperimentalMaterial3Api::class)
                (TopAppBar(
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
                ))
            }
        },
        contentWindowInsets = WindowInsets(0, 0, 0, 0),
        snackbarHost = { SnackbarHost(snackbarHostState) },
    ) { innerPadding ->
        Column(
            modifier = Modifier
                .verticalScroll(rememberScrollState())
                .padding(innerPadding)
                .imePadding()
                .padding(start = 16.dp, top = 12.dp, end = 16.dp, bottom = 12.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            PrinterInfoCard(
                uiState = uiState,
                onDetectDpi = onDetectDpi,
            )

            LabelDimensionsSection(
                config = uiState.config,
                onConfigChange = onConfigChange,
            )

            LabelMediaTypePicker(
                selectedMedia = uiState.config.media,
                onSelect = { media -> onConfigChange(uiState.config.copy(mediaType = media.name)) },
            )

            val visibleTemplates = uiState.savedTemplates.filter { it.type == uiState.config.type }
                    SavedTemplatePicker(
                templates = visibleTemplates,
                selectedName = uiState.selectedTemplateName
                    ?.takeIf { name -> visibleTemplates.any { it.name == name } },
                onSelect = onSelectSavedTemplate,
            )

            LabelTypePicker(
                selectedType = uiState.config.type,
                onSelect = { type -> onConfigChange(uiState.config.copy(labelType = type.name)) },
            )

            RawZplEditor(
                config = uiState.config,
                onConfigChange = onConfigChange,
                onImportZpl = onImportZpl,
            )

            LabelPreviewCard(
                uiState = uiState,
                onRenderPreview = onRenderPreview,
            )

            ActionRow(
                onSaveTemplate = onSaveTemplate,
                onExportRequested = { showExportDialog = true },
            )
        }
            if (showExportDialog) {
                AlertDialog(
                    onDismissRequest = { showExportDialog = false },
                    title = { Text(stringResource(R.string.export_dialog_title)) },
                    text = { Text(stringResource(R.string.export_dialog_message)) },
                    confirmButton = {
                        TextButton(onClick = {
                            showExportDialog = false
                            // launch create document to save
                            createDocumentLauncher.launch(suggestedFilename)
                            onExportZpl()
                        }) {
                            Text(stringResource(R.string.save_text))
                        }
                    },
                    dismissButton = {
                        TextButton(onClick = {
                            // share via chooser
                            showExportDialog = false
                            coroutineScope.launch {
                                val zpl = uiState.config.toZpl()
                                val cacheFile = File(context.cacheDir, suggestedFilename)
                                withContext(Dispatchers.IO) {
                                    FileOutputStream(cacheFile).use { it.write(zpl.toByteArray()) }
                                }
                                val uri = FileProvider.getUriForFile(context, context.packageName + ".fileprovider", cacheFile)
                                val shareIntent = Intent(Intent.ACTION_SEND).apply {
                                    type = "text/plain"
                                    putExtra(Intent.EXTRA_STREAM, uri)
                                    addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
                                }
                                                context.startActivity(Intent.createChooser(shareIntent, null))
                                                onExportZpl()
                                                snackbarHostState.showSnackbar(exportSuccessText)
                            }
                        }) {
                            Text(stringResource(R.string.share_text))
                        }
                    }
                )
            }
        }
    }

@Composable
private fun AccentSectionCard(
    modifier: Modifier = Modifier,
    content: @Composable () -> Unit,
) {
    Card(
        modifier = modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        elevation = CardDefaults.cardElevation(defaultElevation = 0.dp),
        border = BorderStroke(
            width = 1.dp,
            color = MaterialTheme.colorScheme.secondary.copy(alpha = 0.55f),
        ),
    ) {
        content()
    }
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun LabelSizeBox(
    config: LabelTemplateConfig,
    modifier: Modifier = Modifier,
    previewBitmap: Bitmap? = null,
) {
    BoxWithConstraints(modifier = modifier, contentAlignment = Alignment.Center) {
        // Reserve a little padding inside the available space
        val padding = 12.dp
        val availableW = (maxWidth - padding * 2).coerceAtLeast(8.dp)
        val availableH = (maxHeight - padding * 2).coerceAtLeast(8.dp)

        val wIn = config.widthInches
        val hIn = config.heightInches
        val aspect = if (hIn > 0f) wIn / hIn else 1f

        val availWVal = availableW.value
        val availHVal = availableH.value

        val (targetWVal, targetHVal) = if (availWVal / availHVal > aspect) {
            // height limits
            val h = availHVal
            val w = h * aspect
            Pair(w, h)
        } else {
            // width limits
            val w = availWVal
            val h = if (aspect > 0f) w / aspect else availHVal
            Pair(w, h)
        }

        val targetW = targetWVal.dp
        val targetH = targetHVal.dp

        Box(
            modifier = Modifier
                .size(targetW, targetH)
                .background(Color.Transparent),
            contentAlignment = Alignment.Center,
        ) {
            // Base card with grid and label dimensions
            Card(
                modifier = Modifier
                    .matchParentSize(),
                shape = RoundedCornerShape(6.dp),
                elevation = CardDefaults.cardElevation(defaultElevation = 6.dp),
                colors = CardDefaults.cardColors(containerColor = Color.White),
            ) {
                Box(contentAlignment = Alignment.Center) {
                    val density = LocalDensity.current
                    val strokePx = with(density) { 0.5.dp.toPx() }
                    val gridColor = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.12f)

                    Canvas(modifier = Modifier.matchParentSize()) {
                        val cols = 4
                        val rows = 4
                        for (i in 1 until cols) {
                            val x = size.width * i / cols
                            drawLine(gridColor, Offset(x, 0f), Offset(x, size.height), strokeWidth = strokePx)
                        }
                        for (j in 1 until rows) {
                            val y = size.height * j / rows
                            drawLine(gridColor, Offset(0f, y), Offset(size.width, y), strokeWidth = strokePx)
                        }
                    }
                }
            }

            val headHeightRatio = 0.12f
            val headHeightDp = targetH * headHeightRatio

            previewBitmap?.let { bmp ->
                Image(
                    bitmap = bmp.asImageBitmap(),
                    contentDescription = null,
                    contentScale = ContentScale.Fit,
                    modifier = Modifier
                        .matchParentSize()
                        .padding(top = headHeightDp)
                        .clip(RoundedCornerShape(6.dp)),
                )
            }

            val density = LocalDensity.current
            val strokePx = with(density) { 1.dp.toPx() }
            val onSurfaceVariant = MaterialTheme.colorScheme.onSurfaceVariant

            // Draw a precise border inset by the user's margins
            Canvas(modifier = Modifier.matchParentSize()) {

                // Draw the precise border inset by the user's margins (marginX/marginY are in dots)
                // Compute scale from dots to canvas pixels using config width/height dots
                val widthDots = config.widthDots.toFloat().coerceAtLeast(1f)
                val heightDots = config.heightDots.toFloat().coerceAtLeast(1f)
                val scaleX = size.width / widthDots
                val scaleY = size.height / heightDots
                val insetLeft = config.marginX * scaleX
                val insetTop = config.marginY * scaleY

                val borderW = (size.width - insetLeft - insetLeft).coerceAtLeast(1f)
                val borderH = (size.height - insetTop - insetTop).coerceAtLeast(1f)

                drawRect(
                    color = onSurfaceVariant,
                    topLeft = Offset(insetLeft, insetTop),
                    size = androidx.compose.ui.geometry.Size(borderW, borderH),
                    style = Stroke(width = strokePx)
                )
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun SavedTemplatePicker(
    templates: List<LabelTemplateConfig>,
    selectedName: String?,
    onSelect: (String, LabelTemplateType) -> Unit,
) {
    if (templates.isEmpty()) {
        return
    }

    var expanded by remember { mutableStateOf(false) }
    ExposedDropdownMenuBox(
        expanded = expanded,
        onExpandedChange = { expanded = !expanded },
    ) {
        OutlinedTextField(
            value = selectedName.orEmpty(),
            onValueChange = {},
            readOnly = true,
            label = { Text(stringResource(R.string.saved_zpl_label)) },
            trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = expanded) },
            modifier = Modifier
                .menuAnchor(ExposedDropdownMenuAnchorType.PrimaryNotEditable, true)
                .fillMaxWidth(),
        )
        ExposedDropdownMenu(
            expanded = expanded,
            onDismissRequest = { expanded = false },
        ) {
            templates.forEach { template ->
                DropdownMenuItem(
                    text = {
                        Column {
                            Text(template.name)
                            // list variables used in the ZPL (tokens like {crossId}, {date})
                            val zpl = template.toZpl()
                            val vars = "\\{([^}]+)\\}".toRegex()
                                .findAll(zpl)
                                .map { it.groupValues[1] }
                                .distinct()
                                .joinToString(", ")
                            if (vars.isNotBlank()) {
                                Text(
                                    vars,
                                    style = MaterialTheme.typography.bodySmall.copy(fontFamily = FontFamily.Monospace),
                                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                                    maxLines = 1,
                                    overflow = TextOverflow.Ellipsis,
                                )
                            }
                        }
                    },
                    onClick = {
                        expanded = false
                        onSelect(template.name, template.type)
                    },
                )
            }
        }
    }
}

@Composable
private fun RawZplEditor(
    config: LabelTemplateConfig,
    onConfigChange: (LabelTemplateConfig) -> Unit,
    onImportZpl: () -> Unit,
) {
    AccentSectionCard {
        Column(
            modifier = Modifier.padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            var zplValue by remember {
                val zpl = config.toZpl()
                mutableStateOf(TextFieldValue(zpl, selection = TextRange(zpl.length)))
            }
            LaunchedEffect(config) {
                val zpl = config.toZpl()
                if (zpl != zplValue.text) {
                    zplValue = TextFieldValue(zpl, selection = TextRange(zpl.length))
                }
            }

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
            ) {
                Text(stringResource(R.string.zpl_string_title), style = MaterialTheme.typography.titleMedium)
                OutlinedButton(onClick = onImportZpl) {
                    Text(stringResource(R.string.import_file))
                }
            }
            OutlinedTextField(
                value = config.name,
                onValueChange = { onConfigChange(config.copy(name = it)) },
                label = { Text(stringResource(R.string.saved_name_label)) },
                singleLine = true,
                modifier = Modifier.fillMaxWidth(),
            )
            OutlinedTextField(
                value = zplValue,
                onValueChange = {
                    zplValue = it
                    onConfigChange(config.copy(rawZpl = it.text))
                },
                label = { Text(stringResource(R.string.zpl_field_label)) },
                textStyle = MaterialTheme.typography.bodySmall.copy(fontFamily = FontFamily.Monospace),
                minLines = 12,
                modifier = Modifier
                    .fillMaxWidth()
                    .heightIn(min = 220.dp),
            )
            AvailablePlaceholders(
                type = config.type,
                onInsert = { placeholder ->
                    val selectionStart = minOf(zplValue.selection.start, zplValue.selection.end)
                    val selectionEnd = maxOf(zplValue.selection.start, zplValue.selection.end)
                    val updated = zplValue.text.replaceRange(selectionStart, selectionEnd, placeholder)
                    val cursor = selectionStart + placeholder.length
                    zplValue = TextFieldValue(updated, selection = TextRange(cursor))
                    onConfigChange(config.copy(rawZpl = updated))
                },
            )
            Text(
                text = stringResource(R.string.placeholder_compatibility_note),
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
    }
}

@Composable
private fun LabelPreviewCard(
    uiState: LabelTemplateUiState,
    onRenderPreview: () -> Unit,
) {
    AccentSectionCard {
        Column(
            modifier = Modifier.padding(12.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Text(stringResource(R.string.label_preview_title), style = MaterialTheme.typography.titleSmall)
                OutlinedButton(
                    onClick = onRenderPreview,
                    enabled = !uiState.isRenderingPreview && uiState.zplPreview.isNotBlank(),
                ) {
                    Text(
                        if (uiState.isRenderingPreview) {
                            stringResource(R.string.rendering_preview)
                        } else {
                            stringResource(R.string.render_preview)
                        },
                    )
                }
            }

            // visual indication of the label physical size
            val bitmap = uiState.previewBitmap
            LabelSizeBox(
                config = uiState.config,
                modifier = Modifier
                    .fillMaxWidth()
                    .heightIn(120.dp),
                previewBitmap = bitmap,
            )
            // previewBitmap is drawn inside the LabelSizeBox when available; otherwise show message
            if (bitmap == null) {
                Text(
                    text = uiState.previewMessage ?: stringResource(R.string.label_preview_empty),
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
        }
    }
}

@Composable
private fun LabelDimensionsSection(
    config: LabelTemplateConfig,
    onConfigChange: (LabelTemplateConfig) -> Unit,
) {
    var pvWidth by remember { mutableStateOf(config.widthInches.toDisplayText()) }
    var pvHeight by remember { mutableStateOf(config.heightInches.toDisplayText()) }
    val pvWidthRequester = remember { BringIntoViewRequester() }
    val pvHeightRequester = remember { BringIntoViewRequester() }

    LaunchedEffect(config.widthInches, config.heightInches) {
        val wText = config.widthInches.toDisplayText()
        val hText = config.heightInches.toDisplayText()
        if (wText != pvWidth) pvWidth = wText
        if (hText != pvHeight) pvHeight = hText
    }

    Row(horizontalArrangement = Arrangement.spacedBy(8.dp), modifier = Modifier.fillMaxWidth()) {
        OutlinedTextField(
            value = pvWidth,
            onValueChange = {
                pvWidth = it
                it.toFloatOrNull()?.let { f ->
                    val coerced = f.coerceIn(1f, 6f)
                    if (coerced != config.widthInches) {
                        onConfigChange(config.copy(widthInches = coerced))
                    }
                }
            },
            label = { Text(stringResource(R.string.label_width_in)) },
            singleLine = true,
            modifier = Modifier
                .weight(1f)
                .bringIntoViewRequester(pvWidthRequester),
        )
        OutlinedTextField(
            value = pvHeight,
            onValueChange = {
                pvHeight = it
                it.toFloatOrNull()?.let { f ->
                    val coerced = f.coerceIn(1f, 6f)
                    if (coerced != config.heightInches) {
                        onConfigChange(config.copy(heightInches = coerced))
                    }
                }
            },
            label = { Text(stringResource(R.string.label_height_in)) },
            singleLine = true,
            modifier = Modifier
                .weight(1f)
                .bringIntoViewRequester(pvHeightRequester),
        )
    }
}

@Composable
private fun AvailablePlaceholders(
    type: LabelTemplateType,
    onInsert: (String) -> Unit,
) {
    val placeholders = when (type) {
        LabelTemplateType.CROSS -> ZplStringReplacer.eventPlaceholderHelp
        LabelTemplateType.PARENT -> ZplStringReplacer.parentPlaceholderHelp
    }
    AccentSectionCard {
        Column(
            modifier = Modifier.padding(6.dp),
            verticalArrangement = Arrangement.spacedBy(6.dp),
        ) {
            Text(stringResource(R.string.placeholders_title), style = MaterialTheme.typography.labelLarge)
            FlowRow(
                horizontalArrangement = Arrangement.spacedBy(6.dp),
                verticalArrangement = Arrangement.spacedBy(6.dp),
            ) {
                placeholders.forEach { placeholder ->
                    // show a compact chip with a shortened label; insert uses full token
                    val token = placeholder.token
                    val short = if (token.length > 14) token.take(12) + "…" else token
                    FilterChip(
                        selected = false,
                        onClick = { onInsert(token) },
                        label = { Text(short) },
                    )
                }
            }
        }
    }
}

@Composable
private fun PrinterInfoCard(
    uiState: LabelTemplateUiState,
    onDetectDpi: () -> Unit,
) {
    val config = uiState.config
    AccentSectionCard {
        Column(
            modifier = Modifier.padding(horizontal = 10.dp, vertical = 8.dp),
            verticalArrangement = Arrangement.spacedBy(4.dp),
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Column(
                    modifier = Modifier.weight(1f),
                    verticalArrangement = Arrangement.spacedBy(2.dp),
                ) {
                    Text(stringResource(R.string.printer_info_title), style = MaterialTheme.typography.titleSmall)
                    Text(
                        text = uiState.deviceName.ifBlank { stringResource(R.string.no_device_paired) },
                        style = MaterialTheme.typography.bodySmall,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                    )
                }
                TextButton(
                    onClick = onDetectDpi,
                    enabled = !uiState.isDetectingDpi,
                ) {
                    Text(
                        if (uiState.isDetectingDpi) {
                            stringResource(R.string.reading_printer)
                        } else {
                            stringResource(R.string.read_printer)
                        },
                    )
                }
            }
            Text(
                text = uiState.printerDetails
                    .ifBlank { stringResource(R.string.printer_settings_empty) },
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
            )
            Text(
                text = stringResource(
                    R.string.printer_info_values,
                    config.dpi,
                    stringResource(config.media.displayNameResId),
                    config.widthInches.toDisplayText(),
                    config.heightInches.toDisplayText(),
                ),
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
            )
        }
    }
}

@Composable
private fun LabelTypePicker(
    selectedType: LabelTemplateType,
    onSelect: (LabelTemplateType) -> Unit,
) {
    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
        LabelTemplateType.entries.forEach { type ->
            FilterChip(
                selected = selectedType == type,
                onClick = { onSelect(type) },
                label = { Text(type.typeLabel()) },
            )
        }
    }
}

@Composable
private fun LabelMediaTypePicker(
    selectedMedia: LabelMediaType,
    onSelect: (LabelMediaType) -> Unit,
) {
    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
        LabelMediaType.entries.forEach { media ->
            FilterChip(
                selected = selectedMedia == media,
                onClick = { onSelect(media) },
                label = { Text(stringResource(media.displayNameResId)) },
            )
        }
    }
}

@Composable
private fun ActionRow(
    onSaveTemplate: () -> Unit,
    onExportRequested: () -> Unit,
) {
    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
        Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
            Button(
                onClick = onSaveTemplate,
                modifier = Modifier.weight(1f),
            ) {
                Text(stringResource(R.string.save_text))
            }
            OutlinedButton(
                onClick = onExportRequested,
                modifier = Modifier.weight(1f),
            ) {
                Text(stringResource(R.string.export_zpl))
            }
        }
    }
}

private fun Float.toDisplayText(): String {
    val rounded = (this * 100).roundToInt() / 100f
    return if (rounded % 1f == 0f) {
        rounded.toInt().toString()
    } else {
        rounded.toString().trimEnd('0').trimEnd('.')
    }
}

@Composable
private fun LabelTemplateType.typeLabel(): String {
    return when (this) {
        LabelTemplateType.CROSS -> stringResource(R.string.cross_label_type)
        LabelTemplateType.PARENT -> stringResource(R.string.parent_label_type)
    }
}

@Composable
private fun IntercrossPreviewTheme(content: @Composable () -> Unit) {
    MaterialTheme(
        colorScheme = DefaultAppColors.toMaterialColorScheme(),
        typography = CompactTypography.medium.toMaterialTypography(),
        content = content
    )
}

@Preview(showBackground = true)
@Composable
fun PrinterInfoCardPreview() {
    IntercrossPreviewTheme {
        PrinterInfoCard(
            uiState = LabelTemplateUiState(
                deviceName = "Zebra ZQ620",
                printerDetails = "203 dpi, 2x1 in",
                config = LabelTemplateConfig(dpi = 203, widthInches = 2f, heightInches = 1f)
            ),
            onDetectDpi = {}
        )
    }
}

@Preview(showBackground = true)
@Composable
fun SavedTemplatePickerPreview() {
    IntercrossPreviewTheme {
        SavedTemplatePicker(
            templates = listOf(
                LabelTemplateConfig(name = "Standard 2x1", rawZpl = "^XA^XZ"),
                LabelTemplateConfig(name = "Large 3x2", rawZpl = "^XA^PW609^XZ")
            ),
            selectedName = "Standard 2x1",
            onSelect = { _, _ -> }
        )
    }
}

@Preview(showBackground = true)
@Composable
fun LabelTypePickerPreview() {
    IntercrossPreviewTheme {
        LabelTypePicker(
            selectedType = LabelTemplateType.CROSS,
            onSelect = {}
        )
    }
}

@Preview(showBackground = true)
@Composable
fun RawZplEditorPreview() {
    IntercrossPreviewTheme {
        RawZplEditor(
            config = LabelTemplateConfig(name = "Custom Template", rawZpl = "^XA\n^FO50,50^A0,30,30^FDHello World^FS\n^XZ"),
            onConfigChange = {},
            onImportZpl = {}
        )
    }
}

@Preview(showBackground = true)
@Composable
fun LabelPreviewCardPreview() {
    IntercrossPreviewTheme {
        LabelPreviewCard(
            uiState = LabelTemplateUiState(
                config = LabelTemplateConfig(widthInches = 2f, heightInches = 1f),
                previewMessage = "Click render to see the label"
            ),
            onRenderPreview = {}
        )
    }
}

@Preview(showBackground = true)
@Composable
fun LabelDimensionsSectionPreview() {
    IntercrossPreviewTheme {
        LabelDimensionsSection(
            config = LabelTemplateConfig(widthInches = 2f, heightInches = 1f),
            onConfigChange = {}
        )
    }
}

@Preview(showBackground = true)
@Composable
fun AvailablePlaceholdersPreview() {
    IntercrossPreviewTheme {
        AvailablePlaceholders(
            type = LabelTemplateType.CROSS,
            onInsert = {}
        )
    }
}

@Preview(showBackground = true)
@Composable
fun ActionRowPreview() {
    IntercrossPreviewTheme {
        ActionRow(
            onSaveTemplate = {},
            onExportRequested = {}
        )
    }
}

@Preview(showBackground = true)
@Composable
fun LabelMediaTypePickerPreview() {
    IntercrossPreviewTheme {
        LabelMediaTypePicker(
            selectedMedia = LabelMediaType.CONTINUOUS,
            onSelect = {}
        )
    }
}

@Preview(showBackground = true)
@Composable
fun LabelTemplateEditorScreenPreview() {
    val sampleConfig = LabelTemplateConfig(
        name = "Sample 2x1",
        rawZpl = """
            ^XA
            ^PW406
            ^LH10,10^FS
            ^FO0,0^A0,25,20^FD{crossId}^FS
            ^FO140,30^BQN,2,3^FDQA,{crossId}^FS
            ^FO140,170^A0,25,20^FD{date}^FS
            ^XZ
        """.trimIndent(),
        labelType = LabelTemplateType.CROSS.name
    )

    val sampleUiState = LabelTemplateUiState(
        config = sampleConfig,
        savedTemplates = listOf(sampleConfig),
        deviceName = "Zebra ZQ620",
        printerDetails = "203 dpi, 2x1 in"
    )

    IntercrossPreviewTheme {
        LabelTemplateEditorScreen(
            uiState = sampleUiState,
            onConfigChange = {},
            onSelectSavedTemplate = { _, _ -> },
            onSaveTemplate = {},
            onImportZpl = {},
            onDetectDpi = {},
            onRenderPreview = {},
            onMessageShown = {},
        )
    }
}

@Preview(showBackground = true)
@Composable
fun LabelTemplateEditorParentPreview() {
    val sampleConfig = LabelTemplateConfig(
        name = "Parent Label",
        rawZpl = """
            ^XA
            ^PW406
            ^FO0,0^A0,25,20^FD{parentCode}^FS
            ^FO0,30^A0,25,20^FD{parentName}^FS
            ^XZ
        """.trimIndent(),
        labelType = LabelTemplateType.PARENT.name
    )

    val sampleUiState = LabelTemplateUiState(
        config = sampleConfig,
        savedTemplates = listOf(sampleConfig),
        deviceName = "Zebra ZQ620",
        printerDetails = "203 dpi, 2x1 in"
    )

    IntercrossPreviewTheme {
        LabelTemplateEditorScreen(
            uiState = sampleUiState,
            onConfigChange = {},
            onSelectSavedTemplate = { _, _ -> },
            onSaveTemplate = {},
            onImportZpl = {},
            onDetectDpi = {},
            onRenderPreview = {},
            onMessageShown = {},
        )
    }
}
