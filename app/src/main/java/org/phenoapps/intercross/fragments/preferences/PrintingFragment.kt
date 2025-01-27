package org.phenoapps.intercross.fragments.preferences

import android.content.Intent
import android.net.Uri
import android.os.Bundle
import android.view.View
import androidx.appcompat.app.AppCompatActivity
import androidx.navigation.fragment.findNavController
import androidx.preference.Preference
import org.phenoapps.intercross.R
import org.phenoapps.intercross.activities.MainActivity
import org.phenoapps.intercross.util.KeyUtil

class PrintingFragment : BasePreferenceFragment(R.xml.printing_preferences) {

    private val mKeyUtil by lazy {
        KeyUtil(context)
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
                    i.data = Uri.parse(
                        "https://play.google.com/store/apps/details?id=com.zebra.printersetup")
                    startActivity(i)
                }
                else -> {
                    startActivity(intent)
                }
            }
            true
        }
    }

    override fun onResume() {
        super.onResume()
        setToolbar(getString(R.string.prefs_printing_title))
    }
}
