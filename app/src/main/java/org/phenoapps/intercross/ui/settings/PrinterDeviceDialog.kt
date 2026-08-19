package org.phenoapps.intercross.ui.settings

import android.Manifest
import android.annotation.SuppressLint
import android.bluetooth.BluetoothAdapter
import android.content.Context
import android.content.pm.PackageManager
import android.os.Build
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.RadioButton
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import org.phenoapps.intercross.R

fun bluetoothPrinterPermissions(): Array<String> {
    return if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
        arrayOf(Manifest.permission.BLUETOOTH_SCAN, Manifest.permission.BLUETOOTH_CONNECT)
    } else {
        arrayOf(Manifest.permission.BLUETOOTH, Manifest.permission.BLUETOOTH_ADMIN)
    }
}

fun hasBluetoothPrinterPermission(context: Context): Boolean {
    return bluetoothPrinterPermissions().all {
        context.checkSelfPermission(it) == PackageManager.PERMISSION_GRANTED
    }
}

@SuppressLint("MissingPermission")
@Composable
fun PrinterDeviceDialog(
    selectedDeviceName: String,
    onDismiss: () -> Unit,
    onSelected: (String) -> Unit,
) {
    val devices = remember { BluetoothAdapter.getDefaultAdapter()?.bondedDevices?.toList().orEmpty() }
    var pendingName by remember(selectedDeviceName, devices) {
        mutableStateOf(selectedDeviceName.ifBlank { devices.firstOrNull()?.name.orEmpty() })
    }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(stringResource(R.string.choose_bluetooth_device_title)) },
        text = {
            if (devices.isEmpty()) {
                Text(stringResource(R.string.no_device_paired))
            } else {
                LazyColumn(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                    items(devices, key = { it.address }) { device ->
                        val name = device.name.orEmpty()
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clickable { pendingName = name }
                                .padding(vertical = 8.dp),
                            verticalAlignment = Alignment.CenterVertically,
                        ) {
                            RadioButton(selected = pendingName == name, onClick = { pendingName = name })
                            Column(Modifier.padding(start = 8.dp)) {
                                Text(name)
                                Text(
                                    device.address.orEmpty(),
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                                )
                            }
                        }
                    }
                }
            }
        },
        confirmButton = {
            TextButton(
                enabled = devices.isNotEmpty() && pendingName.isNotBlank(),
                onClick = { onSelected(pendingName) },
            ) {
                Text(stringResource(android.R.string.ok))
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text(stringResource(android.R.string.cancel))
            }
        },
    )
}
