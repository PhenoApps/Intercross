package org.phenoapps.intercross.util

import android.annotation.SuppressLint
import android.bluetooth.BluetoothAdapter
import android.bluetooth.BluetoothDevice
import android.content.Context
import android.widget.RadioButton
import android.widget.RadioGroup
import androidx.appcompat.app.AlertDialog
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


        /*Filter out some classes of bluetooth devices
        mBluetoothAdapter.bondedDevices.forEach {
            when(it?.bluetoothClass?.majorDeviceClass) {
                //BluetoothClass.Device.Major.AUDIO_VIDEO -> Log.d("BTAUDIO_VIDEO", it?.bluetoothClass.toString())
                BluetoothClass.Device.Major.COMPUTER -> Log.d("BTCOMPUTER", it.bluetoothClass.toString())
                //BluetoothClass.Device.Major.HEALTH -> Log.d("BTHEALTH", it?.bluetoothClass.toString())
                BluetoothClass.Device.Major.IMAGING -> Log.d("BTIMAGING", it.bluetoothClass.toString())
                BluetoothClass.Device.Major.MISC -> Log.d("BTMISC", it.bluetoothClass.toString())
                BluetoothClass.Device.Major.NETWORKING -> Log.d("BTNETWORKING", it.bluetoothClass.toString())
                BluetoothClass.Device.Major.PERIPHERAL -> Log.d("BTPERIPHERAL", it.bluetoothClass.toString())
                BluetoothClass.Device.Major.PHONE -> Log.d("BTPHONE", it.bluetoothClass.toString())
                //BluetoothClass.Device.Major.TOY -> Log.d("BTTOY", it?.bluetoothClass.toString())
                BluetoothClass.Device.Major.UNCATEGORIZED -> Log.d("BTUNCATEGORIZED", it.bluetoothClass.toString())
                BluetoothClass.Device.Major.WEARABLE -> Log.d("BTWEARABLE", it.bluetoothClass.toString())
            }
        }*/

        //val btId = pref.getString(SettingsActivity.BT_ID, "")

        if (mBtName.isBlank()) {

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

                val builder = AlertDialog.Builder(ctx).apply {

                    setTitle("Choose bluetooth device to print from.")

                    setView(input)

                    setNegativeButton("Cancel") { _, _ ->

                    }

                    setPositiveButton("OK") { _, _ ->

                        if (input.checkedRadioButtonId == -1) return@setPositiveButton
                        else {
                            //              PreferenceManager.getDefaultSharedPreferences(ctx).edit()
                            //                    .putString(SettingsActivity.BT_ID, map[input.checkedRadioButtonId]?.name)
                            //                  .apply()
                            mBtName = map[input.checkedRadioButtonId]?.name ?: ""
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

    private fun resolvePrintTemplate(ctx: Context): String {
        val pref = androidx.preference.PreferenceManager.getDefaultSharedPreferences(ctx)
        val keyUtil = KeyUtil(ctx)

        val selectedTemplateName = pref.getString(keyUtil.zplTemplateKey, "")?.trim().orEmpty()
        val importedZpl = pref.getString(keyUtil.zplCodeKey, "")?.trim().orEmpty()

        val selectedTemplate = if (
            selectedTemplateName.isNotBlank() &&
            !selectedTemplateName.equals("None", ignoreCase = true)
        ) {
            ZplTemplate.getTemplateByDisplayName(ctx, selectedTemplateName)
        } else {
            null
        }

        return when {
            selectedTemplate != null -> selectedTemplate.zplCode
            importedZpl.isNotBlank() -> importedZpl
            else -> template
        }
    }

    fun print(ctx: Context, events: Array<Event>) {

        choose(ctx) {
            val resolvedTemplate = resolvePrintTemplate(ctx)
            PrintThread(ctx, resolvedTemplate, mBtName).printEvents(events)
        }
    }

    fun print(ctx: Context, parents: Array<Parent>) {

        choose(ctx) {
            val resolvedTemplate = resolvePrintTemplate(ctx)
            PrintThread(ctx, resolvedTemplate, mBtName).printParents(parents)
        }
    }
}