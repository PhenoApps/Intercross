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
import androidx.core.net.toUri
import androidx.navigation.fragment.findNavController
import androidx.preference.ListPreference
import androidx.preference.Preference
import org.phenoapps.intercross.R
import org.phenoapps.intercross.util.LabelTemplateConfig
import org.phenoapps.intercross.util.LabelTemplateStore
import org.phenoapps.intercross.util.LabelTemplateType
import org.phenoapps.intercross.util.ZplTemplate

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

        with(findPreference<Preference>(getString(R.string.key_pref_print_label_templates))) {
            this?.let {
                setOnPreferenceClickListener {
                    findNavController().navigate(PrintingFragmentDirections.actionToLabelTemplateEditorFragment())
                    true
                }
            }
        }

        findPreference<Preference>(getString(R.string.key_pref_print_label_request))?.let {
            it.setOnPreferenceClickListener {
                val intent = Intent(Intent.ACTION_VIEW, "https://github.com/PhenoApps/Intercross/issues".toUri())
                startActivity(intent)
                true
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

        setupTemplatePreference(LabelTemplateType.CROSS)
        setupTemplatePreference(LabelTemplateType.PARENT)

        val devicePref = findPreference<Preference>(getString(R.string.key_pref_print_device_name))
        devicePref?.let {
            updateDevicePreferenceSummary(it)
            it.setOnPreferenceClickListener {
                checkBluetoothPermissionsAndShowDialog()
                true
            }
        }
    }

    private fun setupTemplatePreference(type: LabelTemplateType) {
        val pref = findPreference<ListPreference>(templatePreferenceKey(type)) ?: return
        updateTemplatePreference(pref, type)
        pref.setOnPreferenceChangeListener { _, newValue ->
            val templateName = newValue as? String ?: return@setOnPreferenceChangeListener false
            val template = templatesFor(type).firstOrNull { it.name == templateName }
                ?: return@setOnPreferenceChangeListener false

            setActiveTemplate(template)
            updateTemplatePreference(pref, type)
            false
        }
    }

    private fun updateTemplatePreference(pref: ListPreference, type: LabelTemplateType) {
        val templates = templatesFor(type)
        var selectedName = mPrefs.getString(templatePreferenceKey(type), "").orEmpty()

        if (selectedName.isBlank() && templates.isNotEmpty()) {
            val defaultTemplate = templates.first()
            setActiveTemplate(defaultTemplate)
            selectedName = defaultTemplate.name
        }

        val selectedTemplate = templates.firstOrNull { it.name == selectedName }

        pref.entries = templates.map { it.name }.toTypedArray()
        pref.entryValues = templates.map { it.name }.toTypedArray()
        pref.value = selectedTemplate?.name
        pref.isEnabled = templates.isNotEmpty()
        pref.summary = when {
            templates.isEmpty() -> getString(R.string.prefs_zpl_template_empty_summary)
            selectedName.isBlank() -> getString(R.string.no_active_zpl)
            else -> getString(R.string.prefs_zpl_template_selected_summary, selectedName)
        }
    }

    private fun templatesFor(type: LabelTemplateType): List<LabelTemplateConfig> {
        val saved = LabelTemplateStore.load(mPrefs, mKeyUtil.labelTemplatesKey)
        val builtIn = ZplTemplate.getDefaultTemplates(requireContext()).map {
            LabelTemplateConfig(
                name = it.displayName,
                rawZpl = it.zplCode,
                labelType = it.type.name,
            )
        }
        return (saved + builtIn)
            .filter { it.type == type }
            .distinctBy { it.name.lowercase() }
            .sortedBy { it.name.lowercase() }
    }

    private fun setActiveTemplate(template: LabelTemplateConfig) {
        mPrefs.edit {
            when (template.type) {
                LabelTemplateType.CROSS -> {
                    putString(mKeyUtil.crossZplTemplateKey, template.name)
                    putString(mKeyUtil.crossZplCodeKey, template.toZpl())
                }
                LabelTemplateType.PARENT -> {
                    putString(mKeyUtil.parentZplTemplateKey, template.name)
                    putString(mKeyUtil.parentZplCodeKey, template.toZpl())
                }
            }
        }
    }

    private fun templatePreferenceKey(type: LabelTemplateType): String {
        return when (type) {
            LabelTemplateType.CROSS -> mKeyUtil.crossZplTemplateKey
            LabelTemplateType.PARENT -> mKeyUtil.parentZplTemplateKey
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
            val pairedDevices = adapter.bondedDevices.toList()

            if (pairedDevices.isEmpty()) {
                AlertDialog.Builder(requireContext())
                    .setTitle(getString(R.string.choose_bluetooth_device_title))
                    .setMessage(getString(R.string.no_device_paired))
                    .setPositiveButton(android.R.string.ok, null)
                    .show()
                return
            }

            val deviceNames = pairedDevices.map { it.name }.toTypedArray()
            val currentDevice = mPrefs.getString(mKeyUtil.printerDeviceNameKey, "")
            var selectedIndex = pairedDevices.indexOfFirst { it.name == currentDevice }

            AlertDialog.Builder(requireContext())
                .setTitle(getString(R.string.choose_bluetooth_device_title))
                .setIcon(R.drawable.ic_setting_print_connect)
                .setSingleChoiceItems(deviceNames, selectedIndex) { _, which ->
                    selectedIndex = which
                }
                .setNegativeButton(android.R.string.cancel, null)
                .setPositiveButton(android.R.string.ok) { _, _ ->
                    if (selectedIndex != -1) {
                        val selectedDevice = pairedDevices[selectedIndex]
                        mPrefs.edit { putString(mKeyUtil.printerDeviceNameKey, selectedDevice.name) }
                        findPreference<Preference>(getString(R.string.key_pref_print_device_name))?.let { pref ->
                            updateDevicePreferenceSummary(pref)
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
        findPreference<ListPreference>(mKeyUtil.crossZplTemplateKey)?.let {
            updateTemplatePreference(it, LabelTemplateType.CROSS)
        }
        findPreference<ListPreference>(mKeyUtil.parentZplTemplateKey)?.let {
            updateTemplatePreference(it, LabelTemplateType.PARENT)
        }
    }
}
