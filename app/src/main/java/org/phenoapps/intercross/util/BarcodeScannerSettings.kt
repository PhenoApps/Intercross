package org.phenoapps.intercross.util

import android.content.SharedPreferences
import com.google.mlkit.vision.barcode.common.Barcode

enum class BarcodeFormatGroup {
    ONE_D,
    TWO_D,
}

data class BarcodeFormatOption(
    val value: String,
    val label: String,
    val group: BarcodeFormatGroup,
    val mlKitFormat: Int,
)

object BarcodeScannerSettings {
    val options = listOf(
        BarcodeFormatOption("QR_CODE", "QR Code", BarcodeFormatGroup.TWO_D, Barcode.FORMAT_QR_CODE),
        BarcodeFormatOption("DATA_MATRIX", "Data Matrix", BarcodeFormatGroup.TWO_D, Barcode.FORMAT_DATA_MATRIX),
        BarcodeFormatOption("AZTEC", "Aztec", BarcodeFormatGroup.TWO_D, Barcode.FORMAT_AZTEC),
        BarcodeFormatOption("PDF_417", "PDF417", BarcodeFormatGroup.TWO_D, Barcode.FORMAT_PDF417),
        BarcodeFormatOption("CODE_128", "Code 128", BarcodeFormatGroup.ONE_D, Barcode.FORMAT_CODE_128),
        BarcodeFormatOption("CODE_39", "Code 39", BarcodeFormatGroup.ONE_D, Barcode.FORMAT_CODE_39),
        BarcodeFormatOption("EAN_13", "EAN-13", BarcodeFormatGroup.ONE_D, Barcode.FORMAT_EAN_13),
        BarcodeFormatOption("EAN_8", "EAN-8", BarcodeFormatGroup.ONE_D, Barcode.FORMAT_EAN_8),
        BarcodeFormatOption("UPC_A", "UPC-A", BarcodeFormatGroup.ONE_D, Barcode.FORMAT_UPC_A),
        BarcodeFormatOption("UPC_E", "UPC-E", BarcodeFormatGroup.ONE_D, Barcode.FORMAT_UPC_E),
    )

    val defaultValues = setOf("QR_CODE")

    fun selectedValues(prefs: SharedPreferences, key: String): Set<String> {
        return prefs.getStringSet(key, defaultValues)
            ?.takeIf { it.isNotEmpty() }
            ?: defaultValues
    }

    /**
     * Returns the combined ML Kit format int for use with BarcodeScannerOptions.
     * ML Kit formats are bitwise OR'd together.
     */
    fun selectedMlKitFormats(prefs: SharedPreferences, key: String): Int {
        val selected = selectedValues(prefs, key)
        val formats = options
            .filter { it.value in selected }
            .map { it.mlKitFormat }
            .ifEmpty { listOf(Barcode.FORMAT_QR_CODE) }
        return formats.reduce { acc, format -> acc or format }
    }

    fun summary(selectedValues: Set<String>): String {
        val selectedLabels = options
            .filter { it.value in selectedValues }
            .map { it.label }
        return if (selectedLabels.isEmpty()) {
            options.first { it.value == "QR_CODE" }.label
        } else {
            selectedLabels.joinToString()
        }
    }
}
