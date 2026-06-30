package org.phenoapps.intercross.util

import android.Manifest
import android.bluetooth.BluetoothDevice
import android.bluetooth.BluetoothManager
import android.content.Context
import android.content.pm.PackageManager
import android.graphics.Typeface
import android.os.Build
import android.view.Gravity
import android.widget.LinearLayout
import android.widget.RadioButton
import android.widget.ScrollView
import android.widget.TextView
import androidx.core.app.ActivityCompat
import androidx.core.content.edit
import androidx.preference.PreferenceManager
import com.google.android.material.dialog.MaterialAlertDialogBuilder
import org.phenoapps.intercross.R
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

        if (pairedDevices.isNullOrEmpty()) {
            MaterialAlertDialogBuilder(ctx)
                .setTitle(R.string.choose_bluetooth_device_title)
                .setIcon(R.drawable.ic_setting_print_connect)
                .setMessage(R.string.no_device_paired)
                .setPositiveButton(android.R.string.ok, null)
                .show()
            return
        }

        val deviceEntries = pairedDevices.entries.toList()
        var selectedIndex = deviceEntries.indexOfFirst { it.key == savedDeviceName }
            .takeIf { it >= 0 }
            ?: 0

        val choices = LinearLayout(ctx).apply {
            orientation = LinearLayout.VERTICAL
            setPadding(0, ctx.dp(4), 0, 0)
        }
        val radioButtons = mutableListOf<RadioButton>()

        fun updateSelection() {
            radioButtons.forEachIndexed { index, radioButton ->
                radioButton.isChecked = index == selectedIndex
            }
        }

        deviceEntries.forEachIndexed { index, entry ->
            val device = entry.value
            val row = LinearLayout(ctx).apply {
                orientation = LinearLayout.HORIZONTAL
                gravity = Gravity.CENTER_VERTICAL
                isClickable = true
                isFocusable = true
                background = ctx.selectableItemBackground()
                setPadding(ctx.dp(4), ctx.dp(10), ctx.dp(4), ctx.dp(10))
                setOnClickListener {
                    selectedIndex = index
                    updateSelection()
                }
            }
            val radioButton = RadioButton(ctx).apply {
                isClickable = false
                isFocusable = false
                isChecked = index == selectedIndex
            }
            radioButtons += radioButton
            val labels = LinearLayout(ctx).apply {
                orientation = LinearLayout.VERTICAL
                setPadding(ctx.dp(12), 0, 0, 0)
                layoutParams = LinearLayout.LayoutParams(0, LinearLayout.LayoutParams.WRAP_CONTENT, 1f)
            }
            labels.addView(TextView(ctx).apply {
                text = entry.key
                textSize = 16f
                typeface = Typeface.DEFAULT_BOLD
                setTextColor(ctx.themeColor(android.R.attr.textColorPrimary))
            })
            labels.addView(TextView(ctx).apply {
                text = device.address.orEmpty()
                textSize = 13f
                setTextColor(ctx.themeColor(android.R.attr.textColorSecondary))
                setPadding(0, ctx.dp(2), 0, 0)
            })
            row.addView(radioButton)
            row.addView(labels)
            choices.addView(row)
        }

        MaterialAlertDialogBuilder(ctx)
            .setTitle(ctx.getString(R.string.choose_bluetooth_device_title))
            .setIcon(R.drawable.ic_setting_print_connect)
            .setView(
                ScrollView(ctx).apply {
                    isFillViewport = false
                    setPadding(ctx.dp(8), 0, ctx.dp(8), 0)
                    addView(choices)
                },
            )
            .setNegativeButton(android.R.string.cancel, null)
            .setPositiveButton(android.R.string.ok) { _, _ ->
                val entry = deviceEntries[selectedIndex]
                pref.edit {
                    putString(keyUtil.printerDeviceNameKey, entry.key)
                }
                f(entry.value)
            }
            .show()
    }

    private fun Context.dp(value: Int): Int = (value * resources.displayMetrics.density).toInt()

    private fun Context.selectableItemBackground(): android.graphics.drawable.Drawable? {
        val attrs = intArrayOf(android.R.attr.selectableItemBackground)
        val typedArray = obtainStyledAttributes(attrs)
        return try {
            typedArray.getDrawable(0)
        } finally {
            typedArray.recycle()
        }
    }

    private fun Context.themeColor(attr: Int): Int {
        val attrs = intArrayOf(attr)
        val typedArray = obtainStyledAttributes(attrs)
        return try {
            typedArray.getColor(0, 0)
        } finally {
            typedArray.recycle()
        }
    }

    private var defaultZpl = """
        ^XA
        ^PW406
        ^LH10,10^FS
        ^FO0,0^A0,25,20^FD{crossId}^FS
        ^FO140,30^BQN,2,3,H^FDQA,{crossId}^FS
        ^FO140,170^A0,25,20^FD{date}^FS
        ^XZ
    """.trimIndent()

    private var defaultParentZpl = """
        ^XA
        ^PW406
        ^LH10,10^FS
        ^FO0,0^A0,25,20^FD{parentId}^FS
        ^FO140,30^BQN,2,3,H^FDQA,{parentId}^FS
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

    private fun findDevice(ctx: Context, deviceName: String): BluetoothDevice? {
        return getDevices(ctx)
            ?.entries
            ?.firstOrNull { it.key == deviceName }
            ?.value
    }

    fun print(ctx: Context, events: Array<ZebraPrinterUtil.CrossParentRelation>, deviceName: String): Boolean {
        val device = findDevice(ctx, deviceName) ?: return false
        resolvePrintTemplate(ctx, LabelTemplateType.CROSS) { template ->
            ZebraPrinterUtil(ctx, template, device).printEvents(events)
        }
        return true
    }

    fun print(ctx: Context, parents: Array<Parent>, deviceName: String): Boolean {
        val device = findDevice(ctx, deviceName) ?: return false
        resolvePrintTemplate(ctx, LabelTemplateType.PARENT) { template ->
            ZebraPrinterUtil(ctx, template, device).printParents(parents)
        }
        return true
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
