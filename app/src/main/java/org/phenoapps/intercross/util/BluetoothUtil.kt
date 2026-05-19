package org.phenoapps.intercross.util

import android.Manifest
import android.bluetooth.BluetoothDevice
import android.bluetooth.BluetoothManager
import android.content.Context
import android.content.pm.PackageManager
import android.os.Build
import android.widget.RadioButton
import android.widget.RadioGroup
import androidx.appcompat.app.AlertDialog
import androidx.core.app.ActivityCompat
import androidx.preference.PreferenceManager
import androidx.core.content.edit
import org.phenoapps.intercross.R
import org.phenoapps.intercross.data.models.Event
import org.phenoapps.intercross.data.models.Parent
import kotlin.collections.forEach


//Bluetooth Utility class for printing ZPL code and choosing bluetooth devices to print from.
class BluetoothUtil {

    private fun getDevices(ctx: Context): Map<String, BluetoothDevice>? {
        val bluetoothManager = ctx.getSystemService(Context.BLUETOOTH_SERVICE) as BluetoothManager
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            if (ActivityCompat.checkSelfPermission(
                    ctx,
                    Manifest.permission.BLUETOOTH_CONNECT
                ) != PackageManager.PERMISSION_GRANTED
            ) return null
        }

        return bluetoothManager.adapter?.bondedDevices?.associate { it.name to it }
    }

    //suppressed false positive lint message, permissions is checked on runtime before thread is launched
    //operation that uses the provided context to prompt the user for a paired bluetooth device
    private fun choose(ctx: Context, f: (BluetoothDevice) -> Unit) {
        val pref = PreferenceManager.getDefaultSharedPreferences(ctx)
        val keyUtil = KeyUtil(ctx)

        val pairedDevices = getDevices(ctx)
        val savedDeviceName = pref.getString(keyUtil.printerDeviceNameKey, "") ?: ""

        if (savedDeviceName.isNotBlank()) {
            pairedDevices?.entries?.find { it.key == savedDeviceName }?.value?.let { savedDevice ->
                f(savedDevice)
            }
            return
        }

        val map = HashMap<Int, Map.Entry<String, BluetoothDevice>>()
        val input = RadioGroup(ctx)

        pairedDevices?.entries?.forEach { entry ->
            val button = RadioButton(ctx)
            button.text = entry.key
            input.addView(button)
            map[button.id] = entry
        }

        val builder = AlertDialog.Builder(ctx)
        builder.setTitle(ctx.getString(R.string.choose_bluetooth_device_title))
        builder.setView(input)
        builder.setNegativeButton(android.R.string.cancel) { _, _ -> }
        builder.setPositiveButton(android.R.string.ok) { _, _ ->
            if (input.checkedRadioButtonId != -1) {
                val entry = map[input.checkedRadioButtonId] ?: return@setPositiveButton
                pref.edit {
                    putString(keyUtil.printerDeviceNameKey, entry.key)
                }
                f(entry.value)
            }
        }
        builder.show()
    }

    private var defaultZpl = """
        ^XA
        ^PW406
        ^LH10,10^FS
        ^FO0,0^A0,25,20^FD{crossId}^FS
        ^FO140,30^BQN,2,3,H^FDHA,{crossId}^FS
        ^FO140,170^A0,25,20^FD{date}^FS
        ^XZ
    """.trimIndent()

    private var defaultParentZpl = """
        ^XA
        ^PW406
        ^LH10,10^FS
        ^FO0,0^A0,25,20^FD{parentId}^FS
        ^FO140,30^BQN,2,3,H^FDHA,{parentId}^FS
        ^XZ
    """.trimIndent()

    private fun resolvePrintTemplate(
        ctx: Context,
        type: LabelTemplateType,
        onComplete: (String) -> Unit,
    ) {
        val pref = PreferenceManager.getDefaultSharedPreferences(ctx)
        val keyUtil = KeyUtil(ctx)

        val selectedName = when (type) {
            LabelTemplateType.CROSS -> pref.getString(keyUtil.crossZplTemplateKey, "")
            LabelTemplateType.PARENT -> pref.getString(keyUtil.parentZplTemplateKey, "")
        }?.trim().orEmpty()
        val savedZpl = when (type) {
            LabelTemplateType.CROSS -> pref.getString(keyUtil.crossZplCodeKey, "")
            LabelTemplateType.PARENT -> pref.getString(keyUtil.parentZplCodeKey, "")
        }?.trim().orEmpty()

        if (savedZpl.isNotBlank()) {
            onComplete(savedZpl)
            return
        }

        ZplTemplate.getAvailableTemplates(ctx)
            .firstOrNull { it.displayName == selectedName && it.type == type }
            ?.let {
            onComplete(it.zplCode)
            return
        }

        val legacyZpl = pref.getString(keyUtil.zplCodeKey, "")?.trim().orEmpty()
        if (legacyZpl.isNotBlank()) {
            onComplete(legacyZpl)
            return
        }

        onComplete(
            when (type) {
                LabelTemplateType.CROSS -> defaultZpl
                LabelTemplateType.PARENT -> defaultParentZpl
            },
        )
    }

    fun print(ctx: Context, events: Array<ZebraPrinterUtil.CrossParentRelation>) {
        resolvePrintTemplate(ctx, LabelTemplateType.CROSS) { template ->
            choose(ctx) { device ->
                ZebraPrinterUtil(ctx, template, device).printEvents(events)
            }
        }
    }

    fun print(ctx: Context, parents: Array<Parent>) {
        resolvePrintTemplate(ctx, LabelTemplateType.PARENT) { template ->
            choose(ctx) { device ->
                ZebraPrinterUtil(ctx, template, device).printParents(parents)
            }
        }
    }
}