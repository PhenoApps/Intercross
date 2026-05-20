package org.phenoapps.intercross.util

/**
 * Represents the configuration for a label template.
 *
 * This data class contains both user-editable values (name, rawZpl) and derived
 * properties (widthDots, heightDots, media, type). Call sanitizedForSave before
 * persisting to ensure values are clamped to reasonable ranges and ZPL is normalized.
 */

import androidx.annotation.StringRes
import org.phenoapps.intercross.R
import kotlin.math.roundToInt

enum class LabelMediaType(
    @StringRes val displayNameResId: Int,
    val zplCode: String,
) {
    GAP(R.string.label_media_type_gap, "Y"),
    MARK(R.string.label_media_type_mark, "M"),
    CONTINUOUS(R.string.label_media_type_continuous, "N");

    companion object {
        fun fromName(name: String): LabelMediaType =
            entries.firstOrNull { it.name == name } ?: GAP

        fun fromSgd(value: String?): LabelMediaType {
            return when (value?.lowercase()?.trim()) {
                "continuous" -> CONTINUOUS
                "mark" -> MARK
                "non-continuous" -> GAP
                else -> GAP
            }
        }
    }
}

enum class LabelTemplateType {
    CROSS,
    PARENT;

    companion object {
        fun fromName(name: String): LabelTemplateType =
            entries.firstOrNull { it.name == name } ?: CROSS
    }
}

object ZplFormatter {
    fun readable(zpl: String): String {
        val normalized = zpl.trim()
            .replace("\r\n", "\n")
            .replace("\r", "\n")
        if (normalized.isBlank()) return ""

        return normalized
            .replace(Regex("(?<!\\n)(\\^XA)"), "\n$1")
            .replace(Regex("(\\^FS)(?!\\n)"), "$1\n")
            .replace(Regex("(?<!\\n)(\\^XZ)"), "\n$1")
            .lineSequence()
            .map { it.trim() }
            .filter { it.isNotBlank() }
            .joinToString("\n")
    }
}

data class LabelTemplateConfig(
    val name: String = "Field label",
    val rawZpl: String? = null,
    val labelType: String = LabelTemplateType.CROSS.name,
    val mediaType: String = LabelMediaType.GAP.name,
    val dpi: Int = 203,
    val widthInches: Float = 2f,
    val heightInches: Float = 1f,
    val marginX: Int = 10,
    val marginY: Int = 10,
    val idX: Int = 0,
    val idY: Int = 0,
    val barcodeX: Int = 140,
    val barcodeY: Int = 30,
    val dateX: Int = 140,
    val dateY: Int = 170,
    val textHeight: Int = 25,
    val textWidth: Int = 20,
    val barcodeMagnification: Int = 3,
    val updatedAt: Long = System.currentTimeMillis(),
) {
    val media: LabelMediaType
        get() = LabelMediaType.fromName(mediaType)

    val type: LabelTemplateType
        get() = LabelTemplateType.fromName(labelType)

    val widthDots: Float
        get() = (widthInches * dpi).coerceAtLeast(1f)

    val heightDots: Float
        get() = (heightInches * dpi).coerceAtLeast(1f)

    fun toZpl(): String {
        rawZpl?.trim()?.takeIf { it.isNotBlank() }?.let { return it }
        return toGeneratedZpl()
    }

    private fun toGeneratedZpl(): String {
        return listOf(
            "^XA",
            "^MN${media.zplCode}",
            "^PW$widthDots",
            "^LL$heightDots",
            "^LH$marginX,$marginY^FS",
            "^FO$idX,$idY^A0,$textHeight,$textWidth^FD${primaryIdPlaceholder()}^FS",
            "^FO$barcodeX,$barcodeY^BQN,2,$barcodeMagnification,H^FDHA,${primaryIdPlaceholder()}^FS",
            "^FO$dateX,$dateY^A0,$textHeight,$textWidth^FD${footerPlaceholder()}^FS",
            "^XZ",
        ).joinToString("\n")
    }

    private fun primaryIdPlaceholder(): String {
        return when (type) {
            LabelTemplateType.CROSS -> "{crossId}"
            LabelTemplateType.PARENT -> "{parentId}"
        }
    }

    private fun footerPlaceholder(): String {
        return when (type) {
            LabelTemplateType.CROSS -> "{date}"
            LabelTemplateType.PARENT -> "{parentName}"
        }
    }

    fun sanitizedForSave(): LabelTemplateConfig {
        val cleanedName = name.trim().ifBlank { "Field label" }
        val cleanedZpl = ZplFormatter.readable(toZpl())
        return copy(
            name = cleanedName,
            rawZpl = cleanedZpl,
            labelType = type.name,
            dpi = dpi.coerceIn(152, 600),
            widthInches = widthInches.coerceIn(0.1f, 15f),
            heightInches = heightInches.coerceIn(0.1f, 15f),
            marginX = marginX.coerceAtLeast(0),
            marginY = marginY.coerceAtLeast(0),
            idX = idX.coerceAtLeast(0),
            idY = idY.coerceAtLeast(0),
            barcodeX = barcodeX.coerceAtLeast(0),
            barcodeY = barcodeY.coerceAtLeast(0),
            dateX = dateX.coerceAtLeast(0),
            dateY = dateY.coerceAtLeast(0),
            textHeight = textHeight.coerceIn(8, 120),
            textWidth = textWidth.coerceIn(8, 120),
            barcodeMagnification = barcodeMagnification.coerceIn(1, 10),
            updatedAt = System.currentTimeMillis(),
        )
    }
}
