package org.phenoapps.intercross.fragments

import android.content.SharedPreferences
import android.widget.ArrayAdapter
import androidx.activity.result.contract.ActivityResultContracts
import dagger.hilt.android.AndroidEntryPoint
import org.phenoapps.intercross.R
import org.phenoapps.intercross.activities.MainActivity
import org.phenoapps.intercross.databinding.FragmentImportZplBinding
import org.phenoapps.intercross.util.KeyUtil
import org.phenoapps.intercross.util.ZplTemplate
import java.io.InputStreamReader
import javax.inject.Inject
import androidx.core.content.edit

@AndroidEntryPoint
class ImportZPLFragment : IntercrossBaseFragment<FragmentImportZplBinding>(R.layout.fragment_import_zpl) {

    @Inject
    lateinit var mPref: SharedPreferences

    @Inject
    lateinit var mKeyUtil: KeyUtil

    private val importZplFile = registerForActivityResult(ActivityResultContracts.GetContent()) { uri ->

        uri?.let {

            val text = InputStreamReader(context?.contentResolver?.openInputStream(uri))
                .readLines()
                .joinToString("\n")

            mBinding.codeTextView.text = text

            mPref.edit {
                putString(mKeyUtil.zplTemplateKey, "None")
                putString(mKeyUtil.zplCodeKey, text)
            }

        }
    }

    override fun FragmentImportZplBinding.afterCreateView() {

        (activity as MainActivity).setBackButtonToolbar()
        (activity as MainActivity).supportActionBar?.apply{
            title = null
            show()
        }

        // Setup template spinner
        setupTemplateSpinner()

        //import a file when button is pressed
        importButton.setOnClickListener {

            importZplFile.launch("*/*")

        }

        //set preview text to imported zpl code
        val code = mPref.getString(mKeyUtil.zplCodeKey, "") ?: ""

        if (code.isNotBlank()) codeTextView.text = code
    }

    private fun FragmentImportZplBinding.setupTemplateSpinner() {

        val templates = ZplTemplate.getDefaultTemplates(requireContext())
        val templateNames = templates.map { it.displayName }.toMutableList()
        val defaultTemplate = templates.firstOrNull { it.name == "template_2x1" } ?: templates.first()

        // Add "None" option at the beginning for custom imported templates
        val spinnerItems = mutableListOf("None")
        spinnerItems.addAll(templateNames)

        val adapter = ArrayAdapter(
            requireContext(),
            android.R.layout.simple_spinner_item,
            spinnerItems
        )
        adapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item)
        templateSpinner.adapter = adapter

        val savedTemplateName = mPref.getString(mKeyUtil.zplTemplateKey, "")?.trim().orEmpty()
        val resolvedTemplateName = when {
            savedTemplateName.isBlank() -> defaultTemplate.displayName
            spinnerItems.contains(savedTemplateName) -> savedTemplateName
            else -> defaultTemplate.displayName
        }

        val selectedPosition = spinnerItems.indexOf(resolvedTemplateName)
        if (selectedPosition >= 0) {
            templateSpinner.setSelection(selectedPosition)
        }

        // Show the default template code in preview if nothing is saved yet
        if (savedTemplateName.isBlank()) {
            codeTextView.text = defaultTemplate.zplCode
        }

        // Handle template selection
        templateSpinner.onItemSelectedListener = object : android.widget.AdapterView.OnItemSelectedListener {
            override fun onItemSelected(parent: android.widget.AdapterView<*>?, view: android.view.View?, position: Int, id: Long) {
                if (position > 0) {
                    val selectedTemplate = templates[position - 1]
                    codeTextView.text = selectedTemplate.zplCode
                    mPref.edit {
                        putString(mKeyUtil.zplTemplateKey, selectedTemplate.displayName)
                        putString(mKeyUtil.zplCodeKey, selectedTemplate.zplCode)
                    }
                } else {
                    mPref.edit { putString(mKeyUtil.zplTemplateKey, "None") }
                }
            }

            override fun onNothingSelected(parent: android.widget.AdapterView<*>?) {
                // Do nothing
            }
        }
    }
}
