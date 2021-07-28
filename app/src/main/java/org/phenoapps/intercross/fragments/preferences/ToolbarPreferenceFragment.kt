package org.phenoapps.intercross.fragments.preferences

import android.os.Bundle
import android.view.View
import androidx.lifecycle.LiveData
import androidx.lifecycle.Observer
import androidx.navigation.fragment.findNavController
import androidx.preference.EditTextPreference
import androidx.preference.PreferenceFragmentCompat
import androidx.preference.PreferenceManager
import com.google.android.material.bottomnavigation.BottomNavigationView
import org.phenoapps.intercross.MainActivity
import org.phenoapps.intercross.R
import org.phenoapps.intercross.util.KeyUtil

/**
 * Generic class for other preference fragments to extend.
 * The base class takes the xml file and root key to populate the preference list.
 * This class mainly handles bottom nav bar navigation directions.
 */
open class ToolbarPreferenceFragment(private val xml: Int, private val key: Int) : PreferenceFragmentCompat() {

    private var mBottomNavBar: BottomNavigationView? = null

    private val mKeyUtil by lazy {
        KeyUtil(context)
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        setPreferencesFromResource(xml, getString(key))

        mBottomNavBar = view.findViewById(R.id.preferences_bottom_nav_bar)

        mBottomNavBar?.selectedItemId = R.id.action_nav_settings

        setupBottomNavBar()

        setHasOptionsMenu(false)

        (activity as MainActivity).supportActionBar?.hide()
    }

    override fun onResume() {
        super.onResume()

        mBottomNavBar?.selectedItemId = R.id.action_nav_settings

    }

    override fun onCreatePreferences(savedInstanceState: Bundle?, rootKey: String?) {

        val askPerson = (arguments ?: Bundle())
            .getString(mKeyUtil.argProfAskPerson, "false")

        if (askPerson == "true") {
            preferenceManager.showDialog(findPreference<EditTextPreference>(mKeyUtil.profPersonKey))
        }
    }

    private fun setupBottomNavBar() {

        mBottomNavBar?.setOnNavigationItemSelectedListener { item ->

            when (item.itemId) {

                R.id.action_nav_home -> {

                    findNavController().navigate(R.id.global_action_to_events)
                }
                R.id.action_nav_parents -> {

                    findNavController().navigate(R.id.global_action_to_parents)
                }
                R.id.action_nav_export -> {

                    (activity as MainActivity).showImportOrExportDialog {

                        mBottomNavBar?.selectedItemId = R.id.action_nav_settings

                    }
                }
                R.id.action_nav_cross_count -> {

                    findNavController().navigate(R.id.global_action_to_cross_count)
                }
            }

            true
        }
    }

    //extension function for live data to only observe once when the data is not null
    protected fun <T> LiveData<T>.observeOnce(observer: Observer<T>) {
        observe(viewLifecycleOwner, object : Observer<T> {
            override fun onChanged(t: T?) {
                t?.let { data ->
                    observer.onChanged(data)
                    removeObserver(this)
                }
            }
        })
    }
}