package org.phenoapps.intercross.fragments

import android.text.Editable
import android.text.Spannable
import android.text.SpannableString
import android.text.TextWatcher
import android.text.style.ForegroundColorSpan
import android.view.View
import androidx.core.content.ContextCompat
import androidx.fragment.app.viewModels
import com.google.android.material.button.MaterialButtonToggleGroup
import org.phenoapps.intercross.R
import org.phenoapps.intercross.activities.MainActivity
import org.phenoapps.intercross.data.SettingsRepository
import org.phenoapps.intercross.data.models.Settings
import org.phenoapps.intercross.data.viewmodels.SettingsViewModel
import org.phenoapps.intercross.data.viewmodels.factory.SettingsViewModelFactory
import org.phenoapps.intercross.databinding.FragmentPatternBinding
import java.util.UUID

class PatternFragment : IntercrossBaseFragment<FragmentPatternBinding>(R.layout.fragment_pattern) {

    private val settingsModel: SettingsViewModel by viewModels {
        SettingsViewModelFactory(SettingsRepository.getInstance(db.settingsDao()))
    }

    override fun onPause() {
        super.onPause()
        settingsModel.insert(buildSettings())
    }

    private var mLastUsed: String = "1"

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
        settingsModel.settings.observe(viewLifecycleOwner) { settings ->
            settings?.let { updateUIBasedOnSettings(it) }
        }
    }

    private fun updateUIBasedOnSettings(settings: Settings) {
        with(settings) {
            // Set the toggle group selection without triggering the listener logic
            val targetButtonId = when {
                isUUID -> R.id.uuidButton
                isPattern -> R.id.patternButton
                else -> R.id.noneButton
            }
            if (mBinding.idTypeToggleGroup.checkedButtonId != targetButtonId) {
                mBinding.idTypeToggleGroup.check(targetButtonId)
            }

            when {
                isUUID -> {
                    mBinding.idPreviewLayout.visibility = View.VISIBLE
                    mBinding.idPreviewEditText.setText(mLastUUID)
                    mBinding.fragmentPatternInput.visibility = View.GONE
                }
                isPattern -> {
                    mBinding.apply {
                        idPreviewLayout.visibility = View.VISIBLE
                        fragmentPatternInput.visibility = View.VISIBLE
                        prefixEditText.setText(prefix)
                        suffixEditText.setText(suffix)
                        numberEditText.setText(number.toString())
                        padEditText.setText(pad.toString())

                        when {
                            startFrom -> {
                                numberInputLayout.visibility = View.VISIBLE
                                startFromRadioButton.isChecked = true
                                autoRadioButton.isChecked = false
                            }
                            isAutoIncrement -> {
                                numberInputLayout.visibility = View.GONE
                                startFromRadioButton.isChecked = false
                                autoRadioButton.isChecked = true
                            }
                        }
                        updatePreview()
                    }
                }
                else -> {
                    mBinding.idPreviewLayout.visibility = View.GONE
                    mBinding.fragmentPatternInput.visibility = View.GONE
                }
            }
        }
    }

    private fun setupListeners() {
        mBinding.apply {
            idTypeToggleGroup.addOnButtonCheckedListener { _, checkedId, isChecked ->
                if (isChecked) handleIdTypeChange(checkedId)
            }

            numberModeRadioGroup.setOnCheckedChangeListener { _, checkedId ->
                handleNumberModeChange(checkedId)
            }

            arrayOf(prefixEditText, numberEditText, suffixEditText, padEditText).forEach {
                it.addTextChangedListener(createTextWatcher())
            }
        }
    }

    private fun handleIdTypeChange(checkedId: Int) {
        closeKeyboard()
        mBinding.apply {
            when (checkedId) {
                R.id.uuidButton -> {
                    idPreviewLayout.visibility = View.VISIBLE
                    idPreviewEditText.setText(mLastUUID)
                    fragmentPatternInput.visibility = View.GONE
                }
                R.id.patternButton -> {
                    idPreviewLayout.visibility = View.VISIBLE
                    fragmentPatternInput.visibility = View.VISIBLE
                    updatePreview()
                }
                R.id.noneButton -> {
                    idPreviewLayout.visibility = View.GONE
                    fragmentPatternInput.visibility = View.GONE
                }
            }
        }
    }

    private fun handleNumberModeChange(checkedId: Int) {
        mBinding.apply {
            when (checkedId) {
                R.id.autoRadioButton -> {
                    mLastUsed = numberEditText.text.toString().ifEmpty { "1" }
                    numberInputLayout.visibility = View.GONE
                    numberEditText.setText("0")
                }
                else -> {
                    numberInputLayout.visibility = View.VISIBLE
                    numberEditText.setText(mLastUsed)
                }
            }
            updatePreview()
        }
    }

    private fun createTextWatcher(): TextWatcher {
        return object : TextWatcher {
            override fun afterTextChanged(s: Editable?) {}
            override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) {}
            override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) {
                updatePreview()
            }
        }
    }

    private fun buildSettings() = Settings().apply {
        id = 0
        val n = mBinding.numberEditText.text.toString().ifEmpty { "1" }
        val p = mBinding.padEditText.text.toString().ifEmpty { "0" }
        isAutoIncrement = mBinding.autoRadioButton.isChecked
        isPattern = mBinding.idTypeToggleGroup.checkedButtonId == R.id.patternButton
        isUUID = mBinding.idTypeToggleGroup.checkedButtonId == R.id.uuidButton
        number = n.toInt()
        pad = p.toInt()
        prefix = mBinding.prefixEditText.text.toString()
        suffix = mBinding.suffixEditText.text.toString()
        startFrom = mBinding.startFromRadioButton.isChecked
    }

    private fun updatePreview() {
        val num = mBinding.numberEditText.text.toString().trim().ifEmpty { "1" }
        val pad = mBinding.padEditText.text.toString().trim().ifEmpty { "0" }
        val prefix = mBinding.prefixEditText.text.toString()
        val suffix = mBinding.suffixEditText.text.toString()
        val numberStr = num.toInt().toString().padStart(pad.toInt(), '0')
        val full = prefix + numberStr + suffix

        val span = SpannableString(full)
        if (prefix.isNotEmpty()) {
            span.setSpan(
                ForegroundColorSpan(ContextCompat.getColor(requireContext(), R.color.patternPrefixColor)),
                0, prefix.length,
                Spannable.SPAN_EXCLUSIVE_EXCLUSIVE
            )
        }
        if (suffix.isNotEmpty()) {
            val start = prefix.length + numberStr.length
            span.setSpan(
                ForegroundColorSpan(ContextCompat.getColor(requireContext(), R.color.patternSuffixColor)),
                start, full.length,
                Spannable.SPAN_EXCLUSIVE_EXCLUSIVE
            )
        }
        mBinding.idPreviewEditText.setText(span)
    }
}
