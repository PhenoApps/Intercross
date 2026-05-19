package org.phenoapps.intercross.data.viewmodels

import android.content.Context
import android.content.SharedPreferences
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import androidx.core.content.edit
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import dagger.hilt.android.lifecycle.HiltViewModel
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import org.phenoapps.intercross.R
import org.phenoapps.intercross.util.KeyUtil
import org.phenoapps.intercross.util.LabelMediaType
import org.phenoapps.intercross.util.LabelPrinterDpiDetector
import org.phenoapps.intercross.util.LabelTemplateConfig
import org.phenoapps.intercross.util.LabelTemplateStore
import org.phenoapps.intercross.util.LabelTemplateType
import org.phenoapps.intercross.util.ZplStringReplacer
import org.phenoapps.intercross.util.ZplFormatter
import org.phenoapps.intercross.util.ZplTemplate
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.RequestBody.Companion.toRequestBody
import java.util.Locale
import javax.inject.Inject

/**
 * Immutable UI state snapshot for the label template editor screen.
 *
 * All fields are serializable-friendly types; non-UI helpers such as preview bitmaps
 * are provided to the Compose UI for rendering only.
 *
 */
data class LabelTemplateUiState(
    val config: LabelTemplateConfig = LabelTemplateConfig(),
    val savedTemplates: List<LabelTemplateConfig> = emptyList(),
    val selectedTemplateName: String? = null,
    val activeTemplateName: String = "",
    val deviceName: String = "",
    val printerDetails: String = "",
    val isDetectingDpi: Boolean = false,
    val previewBitmap: Bitmap? = null,
    val isRenderingPreview: Boolean = false,
    val previewMessage: String? = null,
    val message: String? = null,
) {
    val zplPreview: String
        get() = config.sanitizedForSave().toZpl()
}

@HiltViewModel
class LabelTemplateViewModel @Inject constructor(
    @ApplicationContext private val context: Context,
    private val prefs: SharedPreferences,
) : ViewModel() {

    private val keyUtil = KeyUtil(context)
    private val previewClient = OkHttpClient()

    private val _uiState = MutableStateFlow(createInitialState())
    val uiState: StateFlow<LabelTemplateUiState> = _uiState.asStateFlow()

    init {
        detectDpi()
    }

    fun updateConfig(config: LabelTemplateConfig) {
        val previousConfig = _uiState.value.config
        if (previousConfig.mediaType != config.mediaType) {
            updatePrinterMediaType(config.media)
        }
        if (previousConfig.widthInches != config.widthInches) {
            updatePrinterWidth(config.widthDots)
        }
        if (previousConfig.heightInches != config.heightInches) {
            updatePrinterHeight(config.heightDots)
        }
        val updatedConfig = if (
            previousConfig.type != config.type &&
            ZplStringReplacer.hasPlaceholdersForOtherType(config.toZpl(), config.type)
        ) {
            config.copy(rawZpl = null)
        } else {
            config
        }
        _uiState.update {
            it.copy(
                config = updatedConfig,
                selectedTemplateName = selectedTemplateNameFor(updatedConfig),
                activeTemplateName = activeTemplateNameFor(updatedConfig.type),
                previewBitmap = null,
                previewMessage = null,
                message = null,
            )
        }
    }

    fun selectSavedTemplate(name: String, type: LabelTemplateType) {
        val template = _uiState.value.savedTemplates.firstOrNull { it.name == name && it.type == type }
            ?: ZplTemplate.getDefaultTemplates(context)
                .firstOrNull { it.displayName == name && it.type == type }
                ?.let {
                LabelTemplateConfig(
                    name = it.displayName,
                    rawZpl = it.zplCode,
                    labelType = it.type.name,
                )
            }
            ?: return
        _uiState.update {
            it.copy(
                config = template,
                selectedTemplateName = template.name,
                activeTemplateName = activeTemplateNameFor(template.type),
                previewBitmap = null,
                previewMessage = null,
                message = null,
            )
        }
    }

    fun saveCurrentTemplate() {
        val saved = _uiState.value.config.sanitizedForSave()
        val templates = LabelTemplateStore.upsert(prefs, keyUtil.labelTemplatesKey, saved)
        setActiveTemplate(saved)
        _uiState.update {
            it.copy(
                config = saved,
                savedTemplates = templates.withBuiltInZpl(),
                selectedTemplateName = saved.name,
                activeTemplateName = saved.name,
                previewBitmap = null,
                previewMessage = null,
                message = context.getString(R.string.label_template_saved_selected, saved.name),
            )
        }
    }

    fun useCurrentTemplate() {
        val active = _uiState.value.config.sanitizedForSave()
        setActiveTemplate(active)
        _uiState.update {
            it.copy(
                config = active,
                activeTemplateName = active.name,
                previewBitmap = null,
                previewMessage = null,
                message = context.getString(R.string.label_template_selected, active.name),
            )
        }
    }

    fun importRawZpl(rawZpl: String, displayName: String?) {
        val importedName = displayName
            ?.substringAfterLast("/")
            ?.substringBeforeLast(".")
            ?.takeIf { it.isNotBlank() }
            ?: context.getString(R.string.imported_zpl_name)
        _uiState.update {
            it.copy(
                config = it.config.copy(name = importedName, rawZpl = ZplFormatter.readable(rawZpl)),
                selectedTemplateName = importedName,
                previewBitmap = null,
                previewMessage = null,
                message = context.getString(R.string.imported_zpl_message),
            )
        }
    }

    fun renderPreview() {
        if (_uiState.value.isRenderingPreview) return

        val config = _uiState.value.config.sanitizedForSave()
        _uiState.update {
            it.copy(
                isRenderingPreview = true,
                previewBitmap = null,
                previewMessage = null,
                message = null,
            )
        }

        viewModelScope.launch {
            val result = withContext(Dispatchers.IO) {
                runCatching { renderLabelaryPreview(config) }
            }

            _uiState.update { state ->
                result.fold(
                    onSuccess = { bitmap ->
                        state.copy(
                            previewBitmap = bitmap,
                            isRenderingPreview = false,
                            previewMessage = null,
                        )
                    },
                    onFailure = {
                        state.copy(
                            isRenderingPreview = false,
                            previewMessage = context.getString(R.string.label_preview_failed),
                        )
                    },
                )
            }
        }
    }

    fun clearMessage() {
        _uiState.update { it.copy(message = null) }
    }

    fun detectDpi() {
        val deviceName = prefs.getString(keyUtil.printerDeviceNameKey, "").orEmpty()
        _uiState.update {
            it.copy(
                deviceName = deviceName,
                isDetectingDpi = true,
                message = if (deviceName.isBlank()) {
                    context.getString(R.string.choose_printer_before_dpi)
                } else {
                    null
                },
            )
        }

        if (deviceName.isBlank()) {
            _uiState.update { it.copy(isDetectingDpi = false) }
            return
        }

        viewModelScope.launch {
            val result = withContext(Dispatchers.IO) {
                LabelPrinterDpiDetector.detect(context, deviceName)
            }

            _uiState.update { state ->
                val updatedConfig = result.dpi?.let { dpi ->
                    var config = state.config.copy(
                        dpi = dpi,
                    )
                    result.printWidthDots?.let { widthDots ->
                        config = config.copy(widthInches = widthDots / dpi.toFloat())
                    }
                    result.labelLengthDots?.let { heightDots ->
                        config = config.copy(heightInches = heightDots / dpi.toFloat())
                    }
                    result.mediaType?.let { mType ->
                        config = config.copy(mediaType = LabelMediaType.fromSgd(mType).name)
                    }
                    config
                } ?: state.config
                state.copy(
                    config = updatedConfig,
                    printerDetails = result.summary(context),
                    isDetectingDpi = false,
                    message = result.message,
                )
            }
        }
    }

    fun updatePrinterMediaType(mediaType: LabelMediaType) {
        val deviceName = _uiState.value.deviceName
        if (deviceName.isBlank()) return

        viewModelScope.launch(Dispatchers.IO) {
            LabelPrinterDpiDetector.update(context, deviceName, mediaType)
        }
    }

    fun updatePrinterWidth(widthDots: Float) {
        val deviceName = _uiState.value.deviceName
        if (deviceName.isBlank()) return

        viewModelScope.launch(Dispatchers.IO) {
            LabelPrinterDpiDetector.updateWidth(context, deviceName, widthDots)
        }
    }

    fun updatePrinterHeight(heightDots: Float) {
        val deviceName = _uiState.value.deviceName
        if (deviceName.isBlank()) return

        viewModelScope.launch(Dispatchers.IO) {
            LabelPrinterDpiDetector.updateHeight(context, deviceName, heightDots)
        }
    }

    private fun setActiveTemplate(template: LabelTemplateConfig) {
        prefs.edit {
            putString(keyUtil.zplTemplateKey, template.name)
            putString(keyUtil.zplCodeKey, template.toZpl())
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

    private fun selectedTemplateNameFor(config: LabelTemplateConfig): String? {
        return config.name.takeIf { name ->
            _uiState.value.savedTemplates.any { template ->
                template.type == config.type && template.name == name
            }
        }
    }

    private fun createInitialState(): LabelTemplateUiState {
        val savedTemplates = LabelTemplateStore.load(prefs, keyUtil.labelTemplatesKey)
        val activeTemplateName = prefs.getString(keyUtil.zplTemplateKey, "").orEmpty()
        val activeZpl = prefs.getString(keyUtil.zplCodeKey, "").orEmpty()
        val defaultName = context.getString(R.string.label_designer_default_name)
        val noneName = context.getString(R.string.none)
        val initialConfig = savedTemplates.firstOrNull { it.name == activeTemplateName }
            ?: activeZpl.takeIf { it.isNotBlank() }?.let {
                LabelTemplateConfig(
                    name = activeTemplateName
                        .takeUnless { name -> name.isBlank() || name.equals(noneName, ignoreCase = true) }
                        ?: defaultName,
                    rawZpl = it,
                )
            }
            ?: savedTemplates.firstOrNull()
            ?: builtInZpl().firstOrNull()
            ?: LabelTemplateConfig(name = defaultName)

        return LabelTemplateUiState(
            config = initialConfig,
            savedTemplates = savedTemplates.withBuiltInZpl(),
            selectedTemplateName = initialConfig.name,
            activeTemplateName = activeTemplateNameFor(initialConfig.type).ifBlank { activeTemplateName },
            deviceName = prefs.getString(keyUtil.printerDeviceNameKey, "").orEmpty(),
        )
    }

    private fun builtInZpl(): List<LabelTemplateConfig> {
        return ZplTemplate.getDefaultTemplates(context).map {
            LabelTemplateConfig(
                name = it.displayName,
                rawZpl = it.zplCode,
                labelType = it.type.name,
            )
        }
    }

    private fun List<LabelTemplateConfig>.withBuiltInZpl(): List<LabelTemplateConfig> {
        return (this + builtInZpl())
            .distinctBy { "${it.type.name}:${it.name.lowercase()}" }
            .sortedBy { it.name.lowercase() }
    }

    private fun activeTemplateNameFor(type: LabelTemplateType): String {
        return when (type) {
            LabelTemplateType.CROSS -> prefs.getString(keyUtil.crossZplTemplateKey, "").orEmpty()
            LabelTemplateType.PARENT -> prefs.getString(keyUtil.parentZplTemplateKey, "").orEmpty()
        }
    }

    private fun renderLabelaryPreview(config: LabelTemplateConfig): Bitmap {
        val url = "https://api.labelary.com/v1/printers/${config.dpi.toDpmm()}dpmm" +
            "/labels/${config.widthInches.toLabelarySize()}x${config.heightInches.toLabelarySize()}/0/"
        val request = Request.Builder()
            .url(url)
            .addHeader("Accept", "image/png")
            .post(previewZpl(config).toRequestBody("application/x-www-form-urlencoded".toMediaType()))
            .build()

        previewClient.newCall(request).execute().use { response ->
            if (!response.isSuccessful) error("Preview request failed: ${response.code}")
            val bytes = response.body?.bytes() ?: ByteArray(0)
            return BitmapFactory.decodeByteArray(bytes, 0, bytes.size)
                ?: error("Preview response was not an image")
        }
    }

    private fun previewZpl(config: LabelTemplateConfig): String {
        val replacements = when (config.type) {
            LabelTemplateType.CROSS -> mapOf(
                "{crossId}" to "CROSS-001",
                "{readableName}" to "Female A x Male B",
                "{femaleId}" to "FEMALE-A",
                "{maleId}" to "MALE-B",
                "{date}" to "2026-05-18",
                "{timestamp}" to "2026-05-18_14-46-00",
                "{person}" to "Collector",
                "{experiment}" to "Experiment",
                "{type}" to "Biparental",
                "{qrCrossId}" to "QA,CROSS-001",
            )
            LabelTemplateType.PARENT -> mapOf(
                "{parentId}" to "PARENT-001",
                "{parentName}" to "Parent A",
                "{parentType}" to "female",
            )
        }

        return replacements.entries.fold(config.toZpl()) { zpl, (placeholder, value) ->
            zpl.replace(placeholder, value)
        }
    }

    private fun Int.toDpmm(): Int {
        return when {
            this >= 600 -> 24
            this >= 300 -> 12
            this >= 203 -> 8
            else -> 6
        }
    }

    private fun Float.toLabelarySize(): String {
        return String.format(Locale.US, "%.2f", this)
            .trimEnd('0')
            .trimEnd('.')
    }
}
