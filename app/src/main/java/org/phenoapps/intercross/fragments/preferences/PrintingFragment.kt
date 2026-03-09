package org.phenoapps.intercross.fragments.preferences

import android.annotation.SuppressLint
import android.bluetooth.BluetoothAdapter
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Build
import android.os.Bundle
import android.view.View
import android.widget.RadioButton
import android.widget.RadioGroup
import android.widget.Toast
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AlertDialog
import androidx.core.content.edit
import androidx.navigation.fragment.findNavController
import androidx.preference.Preference
import org.phenoapps.intercross.R
import androidx.core.net.toUri

class PrintingFragment : BasePreferenceFragment(R.xml.printing_preferences) {

    private val requestBluetoothPermissions = registerForActivityResult(
        ActivityResultContracts.RequestMultiplePermissions()
    ) { granted ->
        if (granted.filter { it.value == false }.isNotEmpty()) {
            Toast.makeText(context, R.string.error_no_bluetooth_permission, Toast.LENGTH_SHORT).show()
        } else {
            // Permissions granted, now show the device selection dialog
            showDeviceSelectionDialog()
        }
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        with(findPreference<Preference>(getString(R.string.key_pref_print_zpl_import))) {
            this?.let {
                setOnPreferenceClickListener {
                    findNavController().navigate(PrintingFragmentDirections.actionToImportZplFragment())
                    true
                }
            }
        }

        val printSetup = findPreference<Preference>(getString(R.string.key_pref_print_connect))
        printSetup?.setOnPreferenceClickListener {
            val intent = activity?.packageManager
                ?.getLaunchIntentForPackage("com.zebra.printersetup")
            when (intent) {
                null -> {
                    val i = Intent(Intent.ACTION_VIEW)
                    i.data =
                        "https://play.google.com/store/apps/details?id=com.zebra.printersetup".toUri()
                    startActivity(i)
                }
                else -> {
                    startActivity(intent)
                }
            }
            true
        }

        val devicePref = findPreference<Preference>(getString(R.string.key_pref_print_device_name))
        devicePref?.let {
            updateDevicePreferenceSummary(it)
            it.setOnPreferenceClickListener {
                checkBluetoothPermissionsAndShowDialog()
                true
            }
        }
    }

    private fun checkBluetoothPermissionsAndShowDialog() {
        context?.let { ctx ->
            var permit = true

            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
                if (ctx.checkSelfPermission(android.Manifest.permission.BLUETOOTH_SCAN) == PackageManager.PERMISSION_GRANTED
                    && ctx.checkSelfPermission(android.Manifest.permission.BLUETOOTH_CONNECT) == PackageManager.PERMISSION_GRANTED
                ) {
                    permit = true
                } else {
                    permit = false
                    requestBluetoothPermissions.launch(
                        arrayOf(
                            android.Manifest.permission.BLUETOOTH_SCAN,
                            android.Manifest.permission.BLUETOOTH_CONNECT
                        )
                    )
                }
            } else {
                if (ctx.checkSelfPermission(android.Manifest.permission.BLUETOOTH) == PackageManager.PERMISSION_GRANTED
                    && ctx.checkSelfPermission(android.Manifest.permission.BLUETOOTH_ADMIN) == PackageManager.PERMISSION_GRANTED
                ) {
                    permit = true
                } else {
                    permit = false
                    requestBluetoothPermissions.launch(
                        arrayOf(
                            android.Manifest.permission.BLUETOOTH,
                            android.Manifest.permission.BLUETOOTH_ADMIN
                        )
                    )
                }
            }

            if (permit) {
                showDeviceSelectionDialog()
            }
        }
    }

    @SuppressLint("MissingPermission")
    private fun showDeviceSelectionDialog() {
        val mBluetoothAdapter = BluetoothAdapter.getDefaultAdapter()

        mBluetoothAdapter?.let { adapter ->
            val pairedDevices = adapter.bondedDevices

            if (pairedDevices.isEmpty()) {
                AlertDialog.Builder(requireContext())
                    .setTitle(getString(R.string.choose_bluetooth_device_title))
                    .setMessage(getString(R.string.no_device_paired))
                    .setPositiveButton(android.R.string.ok, null)
                    .show()
                return
            }

            val deviceMap = HashMap<Int, String>()
            val input = RadioGroup(requireContext())

            pairedDevices.forEachIndexed { _, device ->
                val button = RadioButton(requireContext())
                button.text = device.name
                input.addView(button)
                deviceMap[button.id] = device.name
            }

            // Pre-select the currently saved device if it exists
            val currentDevice = mPrefs.getString(mKeyUtil.printerDeviceNameKey, "")
            pairedDevices.find { it.name == currentDevice }?.let { device ->
                val existingButton = (0 until input.childCount)
                    .map { input.getChildAt(it) as RadioButton }
                    .find { it.text == device.name }
                existingButton?.isChecked = true
            }

            AlertDialog.Builder(requireContext())
                .setTitle(getString(R.string.choose_bluetooth_device_title))
                .setView(input)
                .setNegativeButton(android.R.string.cancel, null)
                .setPositiveButton(android.R.string.ok) { _, _ ->
                    if (input.checkedRadioButtonId != -1) {
                        val selectedDevice = deviceMap[input.checkedRadioButtonId]
                        selectedDevice?.let { device ->
                            mPrefs.edit { putString(mKeyUtil.printerDeviceNameKey, device) }
                            findPreference<Preference>(getString(R.string.key_pref_print_device_name))?.let { pref ->
                                updateDevicePreferenceSummary(pref)
                            }
                        }
                    }
                }
                .show()
        }
    }

    private fun updateDevicePreferenceSummary(pref: Preference) {
        val deviceName = mPrefs.getString(mKeyUtil.printerDeviceNameKey, "")
        pref.summary = if (deviceName.isNullOrBlank()) {
            getString(R.string.prefs_zebra_device_summary)
        } else {
            getString(R.string.prefs_zebra_device_selected, deviceName)
        }
    }

    override fun onResume() {
        super.onResume()
        setToolbar(getString(R.string.prefs_printing_title))
        val devicePref = findPreference<Preference>(getString(R.string.key_pref_print_device_name))
        devicePref?.let { updateDevicePreferenceSummary(it) }
    }
}
