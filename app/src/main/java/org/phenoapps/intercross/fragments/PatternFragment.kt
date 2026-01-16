package org.phenoapps.intercross.fragments

import android.text.Editable
import android.text.TextWatcher
import android.view.View
import android.view.WindowManager
import androidx.fragment.app.viewModels
import org.phenoapps.intercross.R
import org.phenoapps.intercross.activities.MainActivity
import org.phenoapps.intercross.data.SettingsRepository
import org.phenoapps.intercross.data.models.Settings
import org.phenoapps.intercross.data.viewmodels.SettingsViewModel
import org.phenoapps.intercross.data.viewmodels.factory.SettingsViewModelFactory
import org.phenoapps.intercross.databinding.FragmentPatternBinding
import java.util.UUID

class PatternFragment : IntercrossBaseFragment<FragmentPatternBinding>(R.layout.fragment_pattern) {

    data class Pattern(
        val uuid: Boolean = false,
        val auto: Boolean = false,
        val suffix: String = "",
        val prefix: String = "",
        val number: Int = 0,
        val pad: Int = 3
    ) {
        val pattern = prefix + number.toString().padStart(pad, '0') + suffix
    }

    private val settingsModel: SettingsViewModel by viewModels {
        SettingsViewModelFactory(SettingsRepository.getInstance(db.settingsDao()))
    }

    override fun onPause() {
        super.onPause()

        settingsModel.insert(buildSettings())
    }

    private var mLastUsed: String = "0"

    private var mLastUUID: String = UUID.randomUUID().toString()

    override fun FragmentPatternBinding.afterCreateView() {
        setupToolbar()
        setupUI()
        setupListeners()
    }

    private fun setupToolbar() {
        (activity as MainActivity).setBackButtonToolbar()
        (activity as MainActivity).supportActionBar?.apply {
            title = getString(R.string.patterns_label)
            show()
        }
    }

    private fun setupUI() {
        settingsModel.settings.observeForever { settings ->
            settings?.let {
                mBinding.settings = it
                updateUIBasedOnSettings(it)
            }
        }
    }

    private fun updateUIBasedOnSettings(settings: Settings) {
        with(settings) {
            when {
                isUUID -> mBinding.codeTextView.text = mLastUUID
                isPattern -> {
                    mBinding.apply {
                        prefixEditText.setText(prefix)
                        suffixEditText.setText(suffix)
                        numberEditText.setText(number.toString())
                        padEditText.setText(pad.toString())

                        when {
                            startFrom -> {
                                startFromRadioButton.isChecked = true
                                autoRadioButton.isChecked = false
                            }
                            isAutoIncrement -> {
                                startFromRadioButton.isChecked = false
                                autoRadioButton.isChecked = true
                            }
                        }
                    }
                }
                else -> mBinding.codeTextView.text = ""
            }
        }
    }

    private fun setupListeners() {
        mBinding.apply {
            radioGroup2.setOnCheckedChangeListener { _, checkedId ->
                handleRadioGroup2Change(checkedId)
            }

            radioGroup.setOnCheckedChangeListener { _, checkedId ->
                handleRadioGroupChange(checkedId)
            }

            arrayOf(prefixEditText, numberEditText, suffixEditText, padEditText).forEach {
                it.addTextChangedListener(createTextWatcher())
            }
        }
    }

    private fun handleRadioGroup2Change(checkedId: Int) {
        closeKeyboard()
        mBinding.apply {
            when (checkedId) {
                R.id.uuidButton -> {
                    fragmentPatternInput.visibility = View.GONE
                    codeTextView.text = mLastUUID
                }
                R.id.patternButton -> {
                    fragmentPatternInput.visibility = View.VISIBLE
                    updateCodeTextView()
                }
                R.id.noneButton -> {
                    fragmentPatternInput.visibility = View.GONE
                    codeTextView.text = ""
                }
            }
        }
    }

    private fun handleRadioGroupChange(checkedId: Int) {
        mBinding.apply {
            when (checkedId) {
                R.id.autoRadioButton -> {
                    numberEditText.isEnabled = false
                    mLastUsed = numberEditText.text.toString()
                    numberEditText.setText("0")
                }
                else -> {
                    numberEditText.isEnabled = true
                    numberEditText.setText(mLastUsed)
                }
            }
            updateCodeTextView()
        }
    }

    private fun createTextWatcher(): TextWatcher {
        return object : TextWatcher {
            override fun afterTextChanged(s: Editable?) {}
            override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) {}
            override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) {
                updateCodeTextView()
            }
        }
    }

    private fun buildSettings() = Settings().apply {
        id = 0
        var n = mBinding.numberEditText.text.toString().ifEmpty { "0" }
        var p = mBinding.padEditText.text.toString().ifEmpty { "0" }
        isAutoIncrement = mBinding.autoRadioButton.isChecked
        isPattern = mBinding.patternButton.isChecked
        isUUID = mBinding.uuidButton.isChecked
        number = n.toInt()
        pad = p.toInt()
        prefix = mBinding.prefixEditText.text.toString()
        suffix = mBinding.suffixEditText.text.toString()
        startFrom = mBinding.startFromRadioButton.isChecked
    }

    private fun updateCodeTextView() {
        mBinding.codeTextView.text = buildPattern().pattern
    }

    private fun buildPattern(): Pattern {
        val num = mBinding.numberEditText.text.toString().trim().ifEmpty { "0" }
        val pad = mBinding.padEditText.text.toString().trim().ifEmpty { "0" }
        return Pattern(
            mBinding.uuidButton.isChecked,
            mBinding.autoRadioButton.isChecked,
            mBinding.suffixEditText.text.toString(),
            mBinding.prefixEditText.text.toString(),
            num.toInt(),
            pad.toInt()
        )
    }
}
