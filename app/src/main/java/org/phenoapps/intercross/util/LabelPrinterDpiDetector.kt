package org.phenoapps.intercross.util

/**
 * Detects label printer properties (DPI, media type, printable width) from a paired Bluetooth
 * Zebra device. The detector communicates with the printer using the Zebra SGD API and
 * returns a structured [LabelDpiDetectionResult].
 */

import android.Manifest
import android.annotation.SuppressLint
import android.bluetooth.BluetoothManager
import android.content.Context
import android.content.pm.PackageManager
import android.os.Build
import androidx.core.content.ContextCompat
import com.zebra.sdk.comm.BluetoothConnection
import com.zebra.sdk.printer.SGD
import org.phenoapps.intercross.R

/**
 * Result object from using SGD Zebra API to query device specifications
 */
data class LabelDpiDetectionResult(
    val dpi: Int?,
    val printWidthDots: Int? = null,
    val labelLengthDots: Int? = null,
    val mediaType: String? = null,
    val deviceLanguage: String? = null,
    val printerName: String? = null,
    val message: String,
) {
    fun summary(context: Context): String {
        return listOfNotNull(
            printWidthDots?.let { context.getString(R.string.printer_summary_width, it) },
            labelLengthDots?.let { context.getString(R.string.printer_summary_length, it) },
            deviceLanguage?.takeIf { it.isNotBlank() }?.let {
                context.getString(R.string.printer_summary_language, it)
            },
        ).joinToString(" | ")
    }
}

object LabelPrinterDpiDetector {
    fun canReadBluetooth(context: Context): Boolean {
        return Build.VERSION.SDK_INT < Build.VERSION_CODES.S ||
            ContextCompat.checkSelfPermission(
                context,
                Manifest.permission.BLUETOOTH_CONNECT,
            ) == PackageManager.PERMISSION_GRANTED
    }

    @SuppressLint("MissingPermission")
    fun detect(context: Context, deviceName: String): LabelDpiDetectionResult {
        if (!canReadBluetooth(context)) {
            return LabelDpiDetectionResult(
                dpi = null,
                message = context.getString(R.string.bluetooth_permission_needed_for_dpi),
            )
        }

        if (deviceName.isBlank()) {
            return LabelDpiDetectionResult(
                dpi = null,
                message = context.getString(R.string.choose_printer_before_dpi),
            )
        }

        val bluetoothManager = context.getSystemService(Context.BLUETOOTH_SERVICE) as BluetoothManager
        val device = bluetoothManager.adapter?.bondedDevices?.firstOrNull { it.name == deviceName }
            ?: return LabelDpiDetectionResult(
                dpi = null,
                message = context.getString(R.string.saved_printer_not_found),
            )

        var connection: BluetoothConnection? = null
        return try {
            connection = BluetoothConnection(device.address)
            connection.open()
            val dpi = readSgd(connection, "head.resolution.in_dpi").toFirstInt()
            val printWidthDots = readSgd(connection, "ezpl.print_width").toFirstInt()
            val labelLengthDots = readSgd(connection, "zpl.label_length").toFirstInt()
            val mediaType = readSgd(connection, "ezpl.media_type")
            val deviceLanguage = readSgd(connection, "device.languages")
            val printerName = readSgd(connection, "device.friendly_name")

            if (dpi != null) {
                val result = LabelDpiDetectionResult(
                    dpi = dpi,
                    printWidthDots = printWidthDots,
                    labelLengthDots = labelLengthDots,
                    mediaType = mediaType,
                    deviceLanguage = deviceLanguage,
                    printerName = printerName,
                    message = context.getString(R.string.read_printer_settings_from, deviceName),
                )
                result.copy(message = result.summary(context).ifBlank { result.message })
            } else {
                LabelDpiDetectionResult(
                    dpi = null,
                    message = context.getString(R.string.printer_did_not_report_dpi),
                )
            }
        } catch (_: Exception) {
            LabelDpiDetectionResult(
                dpi = null,
                message = context.getString(R.string.could_not_detect_dpi, deviceName),
            )
        } finally {
            runCatching { connection?.close() }
        }
    }

    @SuppressLint("MissingPermission")
    fun update(context: Context, deviceName: String, mediaType: LabelMediaType): Boolean {
        if (!canReadBluetooth(context) || deviceName.isBlank()) return false

        val bluetoothManager = context.getSystemService(Context.BLUETOOTH_SERVICE) as BluetoothManager
        val device = bluetoothManager.adapter?.bondedDevices?.firstOrNull { it.name == deviceName }
            ?: return false

        var connection: BluetoothConnection? = null
        return try {
            connection = BluetoothConnection(device.address)
            connection.open()
            val sgdValue = when (mediaType) {
                LabelMediaType.GAP -> "gap/notch"
                LabelMediaType.MARK -> "mark"
                LabelMediaType.CONTINUOUS -> "continuous"
            }
            SGD.SET("ezpl.media_type", sgdValue, connection)
            true
        } catch (_: Exception) {
            false
        } finally {
            runCatching { connection?.close() }
        }
    }

    @SuppressLint("MissingPermission")
    fun updateWidth(context: Context, deviceName: String, widthDots: Float): Boolean {
        if (!canReadBluetooth(context) || deviceName.isBlank()) return false

        val bluetoothManager = context.getSystemService(Context.BLUETOOTH_SERVICE) as BluetoothManager
        val device = bluetoothManager.adapter?.bondedDevices?.firstOrNull { it.name == deviceName }
            ?: return false

        var connection: BluetoothConnection? = null
        return try {
            connection = BluetoothConnection(device.address)
            connection.open()
            SGD.SET("ezpl.print_width", widthDots.toString(), connection)
            true
        } catch (_: Exception) {
            false
        } finally {
            runCatching { connection?.close() }
        }
    }

    @SuppressLint("MissingPermission")
    fun updateHeight(context: Context, deviceName: String, heightDots: Float): Boolean {
        if (!canReadBluetooth(context) || deviceName.isBlank()) return false

        val bluetoothManager = context.getSystemService(Context.BLUETOOTH_SERVICE) as BluetoothManager
        val device = bluetoothManager.adapter?.bondedDevices?.firstOrNull { it.name == deviceName }
            ?: return false

        var connection: BluetoothConnection? = null
        return try {
            connection = BluetoothConnection(device.address)
            connection.open()
            SGD.SET("zpl.label_length", heightDots.toString(), connection)
            true
        } catch (_: Exception) {
            false
        } finally {
            runCatching { connection?.close() }
        }
    }

    private fun readSgd(connection: BluetoothConnection, setting: String): String? {
        return runCatching { SGD.GET(setting, connection)?.trim() }
            .getOrNull()
            ?.takeIf { it.isNotBlank() }
    }

    private fun String?.toFirstInt(): Int? {
        return this?.let { Regex("\\d+").find(it)?.value?.toIntOrNull() }
    }
}
