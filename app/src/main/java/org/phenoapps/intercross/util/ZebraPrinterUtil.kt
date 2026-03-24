package org.phenoapps.intercross.util

import android.bluetooth.BluetoothDevice
import android.content.Context
import android.view.LayoutInflater
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AlertDialog
import com.zebra.sdk.comm.BluetoothConnection
import com.zebra.sdk.comm.ConnectionException
import com.zebra.sdk.printer.SGD
import com.zebra.sdk.printer.ZebraPrinterFactory
import com.zebra.sdk.printer.ZebraPrinterLanguageUnknownException
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import org.phenoapps.intercross.R
import org.phenoapps.intercross.data.models.Event
import org.phenoapps.intercross.data.models.Parent
import org.phenoapps.intercross.data.models.PollenGroup

class ZebraPrinterUtil(
    private val ctx: Context,
    private val template: String,
    private val printerDevice: BluetoothDevice
) {

    sealed class PrintMode {
        object Events : PrintMode()
        object Parents : PrintMode()
    }

    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.IO)

    private var progressDialog: AlertDialog? = null

    private suspend fun showProgress() = withContext(Dispatchers.Main) {
        val view = LayoutInflater.from(ctx).inflate(R.layout.dialog_export_progress, null)
        view.findViewById<TextView>(R.id.message).text = ctx.getString(R.string.printing)
        progressDialog = AlertDialog.Builder(ctx)
            .setView(view)
            .setCancelable(false)
            .show()
    }

    private suspend fun hideProgress() = withContext(Dispatchers.Main) {
        progressDialog?.dismiss()
        progressDialog = null
    }

    fun printEvents(events: Array<Event>) {
        scope.launch {
            runPrint(printMode = PrintMode.Events, events = events)
        }
    }

    fun printParents(parents: Array<Parent>) {
        scope.launch {
            runPrint(printMode = PrintMode.Parents, parents = parents)
        }
    }

    fun printGroup(groups: Array<PollenGroup>) {
        val parents = groups.map { group -> Parent(group.codeId, 1, group.name) }.toTypedArray()
        scope.launch {
            runPrint(printMode = PrintMode.Parents, parents = parents)
        }
    }

    private suspend fun runPrint(
        printMode: PrintMode,
        events: Array<Event> = emptyArray(),
        parents: Array<Parent> = emptyArray()
    ) {
        showProgress()
        var connection: BluetoothConnection? = null

        try {
            connection = BluetoothConnection(printerDevice.address)
            connection.open()

            val printer = ZebraPrinterFactory.getInstance(connection)
            val linkOsPrinter = ZebraPrinterFactory.createLinkOsPrinter(printer)

            linkOsPrinter?.let {
                val printerStatus = it.currentStatus

                getPrinterStatus(connection)

                if (printerStatus.isReadyToPrint) {
                    if (template.isNotBlank()) {
                        printer.sendCommand(template)
                    }

                    when (printMode) {
                        PrintMode.Events -> {
                            events.forEach { event ->
                                var timestamp = event.timestamp
                                if ("_" in timestamp) {
                                    timestamp = timestamp.split("_")[0]
                                }

                                printer.sendCommand(
                                    "^XA^XFR:TEMPLATE" +
                                        "^FN1^FD${event.eventDbId}^FS" +
                                        "^FN2^FDQA,${event.eventDbId}^FS" +
                                        "^FN3^FD${event.femaleObsUnitDbId}^FS" +
                                        "^FN4^FD${event.maleObsUnitDbId}^FS" +
                                        "^FN5^FD${timestamp}^FS" +
                                        "^FN6^FD${event.person}^FS^XZ"
                                )
                            }
                        }

                        PrintMode.Parents -> {
                            parents.forEach { parent ->
                                printer.sendCommand(
                                    "^XA^XFR:TEMPLATE" +
                                        "^FN1^FD${parent.codeId}^FS" +
                                        "^FN2^FDQA,${parent.codeId}^FS" +
                                        "^XZ"
                                )
                            }
                        }
                    }
                } else if (printerStatus.isHeadOpen) {
                    showToast(ctx.getString(R.string.printer_open))
                } else if (printerStatus.isPaused) {
                    showToast(ctx.getString(R.string.printer_paused))
                } else if (printerStatus.isPaperOut) {
                    showToast(ctx.getString(R.string.printer_empty))
                } else {
                    showToast(ctx.getString(R.string.printner_not_connected))
                }
            }
        } catch (e: ConnectionException) {
            e.printStackTrace()
        } catch (e: ZebraPrinterLanguageUnknownException) {
            e.printStackTrace()
        } finally {
            connection?.close()
            hideProgress()
        }
    }

    private suspend fun showToast(message: String) {
        withContext(Dispatchers.Main) {
            Toast.makeText(ctx, message, Toast.LENGTH_LONG).show()
        }
    }

    @Throws(ConnectionException::class)
    private fun getPrinterStatus(connection: BluetoothConnection) {
        SGD.SET("device.languages", "zpl", connection)
    }
}