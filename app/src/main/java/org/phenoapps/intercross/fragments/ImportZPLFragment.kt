package org.phenoapps.intercross.fragments

import android.content.SharedPreferences
import androidx.activity.result.contract.ActivityResultContracts
import dagger.hilt.android.AndroidEntryPoint
import org.phenoapps.intercross.R
import org.phenoapps.intercross.activities.MainActivity
import org.phenoapps.intercross.databinding.FragmentImportZplBinding
import org.phenoapps.intercross.util.KeyUtil
import java.io.InputStreamReader
import javax.inject.Inject

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

            mPref.edit().putString(mKeyUtil.zplCodeKey, text).apply()

        }
    }

    override fun FragmentImportZplBinding.afterCreateView() {

        (activity as MainActivity).setBackButtonToolbar()
        (activity as MainActivity).supportActionBar?.apply{
            title = null
            show()
        }

        //import a file when button is pressed
        importButton.setOnClickListener {

            importZplFile.launch("*/*")

        }

        //set preview text to imported zpl code
        val code = mPref.getString(mKeyUtil.zplCodeKey, "") ?: ""

        if (code.isNotBlank()) codeTextView.text = code
    }
}
