package org.phenoapps.intercross.util

import android.annotation.SuppressLint
import android.bluetooth.BluetoothAdapter
import android.bluetooth.BluetoothDevice
import android.content.Context
import android.widget.RadioButton
import android.widget.RadioGroup
import androidx.appcompat.app.AlertDialog
import androidx.preference.PreferenceManager
import androidx.core.content.edit
import org.phenoapps.intercross.R
import org.phenoapps.intercross.data.models.Event
import org.phenoapps.intercross.data.models.Parent


//Bluetooth Utility class for printing ZPL code and choosing bluetooth devices to print from.
class BluetoothUtil {

    private var mBtName: String = String()

    private val mBluetoothAdapter: BluetoothAdapter? by lazy {
        BluetoothAdapter.getDefaultAdapter()
    }

    //suppressed false positive lint message, permissions is checked on runtime before thread is launched
    //operation that uses the provided context to prompt the user for a paired bluetooth device
    @SuppressLint("MissingPermission")
    private fun choose(ctx: Context, f: () -> Unit) {
        val pref = PreferenceManager.getDefaultSharedPreferences(ctx)
        val keyUtil = KeyUtil(ctx)

        // Check if device is already saved
        if (mBtName.isBlank()) {
            val savedDeviceName = pref.getString(keyUtil.printerDeviceNameKey, "") ?: ""
            if (savedDeviceName.isNotBlank()) {
                mBtName = savedDeviceName
                f()
                return
            }

            mBluetoothAdapter?.let {

                val pairedDevices = it.bondedDevices

                val map = HashMap<Int, BluetoothDevice>()

                val input = RadioGroup(ctx)

                pairedDevices.forEachIndexed { _, t ->
                    val button = RadioButton(ctx)
                    button.text = t.name
                    input.addView(button)
                    map[button.id] = t
                }

                val builder = AlertDialog.Builder(ctx)
                builder.setTitle(ctx.getString(R.string.choose_bluetooth_device_title))
                builder.setView(input)
                builder.setNegativeButton(android.R.string.cancel) { _, _ -> }
                builder.setPositiveButton(android.R.string.ok) { _, _ ->
                    if (input.checkedRadioButtonId != -1) {
                        mBtName = map[input.checkedRadioButtonId]?.name ?: ""
                        // Save the selected device to preferences
                        pref.edit {
                            putString(keyUtil.printerDeviceNameKey, mBtName)
                        }
                        f()
                    }
                }
                builder.show()
            }

        } else f()
    }

    //new smaller template
    private var template = """
        ^XA^DFR:TEMPLATE^FS
        ^PW406
        ^LH10,10^FS
        ^FO0,0^A0,25,20^FN1^FS
        ^FO140,30^BQN,2,3,H,^FN2^FS
        ^FO140,170^A0,25,20^FN5^FS
        ^XZ
    """.trimIndent()

    //qr code with magnification 5 is about 150dots which is <1in
    //ZQ510 printer is 208 dots/in, 8dots/mm
    //command to store the template format
//Old template
//    private var template = "^XA" +      //start of ZPL command
//            "^MNA^MMT,N" +              //set as non-continuous label
//            "^DFR:TEMPLATE.ZPL^FS" +    //download format as TEMPLATE.ZPL
//            "^FO75,0^BQN,2,4,H^FN1^FS" + //qr code for code id
//            "^A0N,32,32" +                 //sets font
//            "^FO250,0" +
//            "^FB300,1,1,L,0^FN2^FS" +
//            "^A0N,32,32" +                 //sets font
//            "^FO250,50" +
//            "^FB300,1,1,L,0^FN3^FS" +
//            "^A0N,32,32" +                 //sets font
//            "^FO250,100" +
//            "^FB300,1,1,L,0^FN4^FS" +
//            "^A0N,32,32" +                 //sets font
//            "^FO250,150" +
//            "^FB300,1,1,L,0^FN5^FS" +
//            "^A0N,32,32" +                 //sets font
//            "^FO250,200" +
//            "^FB300,1,1,L,0^FN1^FS" +
//            "^XZ"

    /*var template = """
        ^XA
        ^MNA
        ^MMT,N
        ^DFR:DEFAULT_INTERCROSS_SAMPLE.GRF^FS
        ^FWR
        ^FO50,25
        ^A0,20,20
        ^FN1^FS
        ^FO150,30
        ^BQ,,5,H
        ^FN2^FS
        ^FO400,25
        ^A0,25,20
        ^FN3^FS
        ^XZ"
    """*/

    private fun resolvePrintTemplate(ctx: Context, onComplete: (String) -> Unit) {
        val pref = PreferenceManager.getDefaultSharedPreferences(ctx)
        val keyUtil = KeyUtil(ctx)

        val selectedTemplateName = pref.getString(keyUtil.zplTemplateKey, "") ?: ""
        val importedZpl = pref.getString(keyUtil.zplCodeKey, "") ?: ""

        // Check if user has explicitly imported custom ZPL (not just the default template)
        val hasCustomZpl = importedZpl.isNotBlank() && selectedTemplateName.equals("None", ignoreCase = true)

        // If custom ZPL imported, use it
        if (hasCustomZpl) {
            onComplete(importedZpl)
            return
        }

        // If template already selected and it's a valid predefined template, use it
        if (selectedTemplateName.isNotBlank() && !selectedTemplateName.equals("None", ignoreCase = true)) {
            ZplTemplate.getTemplateByDisplayName(ctx, selectedTemplateName)?.let {
                onComplete(it.zplCode)
                return
            }
        }

        // If no template selected, show dialog
        val templates = ZplTemplate.getDefaultTemplates(ctx)
        val templateNames = templates.map { it.displayName }.toTypedArray()

        val builder = AlertDialog.Builder(ctx)
        builder.setTitle(ctx.getString(R.string.select_zpl_template_title))
        builder.setSingleChoiceItems(templateNames, -1) { dialog, which ->
            val selectedTemplate = templates[which]
            pref.edit {
                putString(keyUtil.zplTemplateKey, selectedTemplate.displayName)
                putString(keyUtil.zplCodeKey, selectedTemplate.zplCode)
            }
            onComplete(selectedTemplate.zplCode)
            dialog.dismiss()
        }
        builder.setNegativeButton(android.R.string.cancel) { dialog, _ ->
            dialog.dismiss()
            // Use default template if user cancels
            onComplete(template)
        }

        builder.show()
    }

    fun print(ctx: Context, events: Array<Event>) {
        resolvePrintTemplate(ctx) { template ->
            choose(ctx) {
                PrintThread(ctx, template, mBtName).printEvents(events)
            }
        }
    }

    fun print(ctx: Context, parents: Array<Parent>) {
        resolvePrintTemplate(ctx) { template ->
            choose(ctx) {
                PrintThread(ctx, template, mBtName).printParents(parents)
            }
        }
    }
}