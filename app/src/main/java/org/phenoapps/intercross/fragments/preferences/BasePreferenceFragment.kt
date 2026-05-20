package org.phenoapps.intercross.fragments.preferences

import android.content.SharedPreferences
import android.os.Bundle
import android.view.View
import androidx.appcompat.app.AppCompatActivity
import androidx.preference.PreferenceFragmentCompat
import dagger.hilt.android.AndroidEntryPoint
import org.phenoapps.intercross.activities.MainActivity
import org.phenoapps.intercross.util.KeyUtil
import javax.inject.Inject

/**
 * Generic class for other preference fragments to extend.
 * The base class takes the xml file and root key to populate the preference list.
 * This class mainly handles bottom nav bar navigation directions and toolbar.
 */
@AndroidEntryPoint
open class BasePreferenceFragment(private val xml: Int) : PreferenceFragmentCompat() {

    @Inject
    lateinit var mPrefs: SharedPreferences

    @Inject
    lateinit var mKeyUtil: KeyUtil

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        setHasOptionsMenu(true)
    }

    protected fun setToolbar(toolbarTitle: String?) {
        setHasOptionsMenu(true)
        (activity as MainActivity).setBackButtonToolbar()
        (activity as AppCompatActivity).supportActionBar?.apply {
            title = toolbarTitle
            show()
        }
    }

    override fun onCreatePreferences(savedInstanceState: Bundle?, rootKey: String?) {
        setPreferencesFromResource(xml, rootKey)
    }
}
