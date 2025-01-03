package org.phenoapps.intercross.fragments.preferences

import android.os.Bundle
import android.view.View
import androidx.appcompat.app.AppCompatActivity
import androidx.navigation.fragment.findNavController
import androidx.preference.PreferenceFragmentCompat
import com.google.android.material.bottomnavigation.BottomNavigationView
import org.phenoapps.intercross.R
import org.phenoapps.intercross.activities.MainActivity
import org.phenoapps.intercross.util.KeyUtil

/**
 * Generic class for other preference fragments to extend.
 * The base class takes the xml file and root key to populate the preference list.
 * This class mainly handles bottom nav bar navigation directions and toolbar.
 */
open class BasePreferenceFragment(private val xml: Int) : PreferenceFragmentCompat() {

    private var mBottomNavBar: BottomNavigationView? = null

    private val mKeyUtil by lazy {
        KeyUtil(context)
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        mBottomNavBar = view.findViewById(R.id.preferences_bottom_nav_bar)

        mBottomNavBar?.selectedItemId = R.id.action_nav_preferences

        setupBottomNavBar()

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

    override fun onResume() {
        super.onResume()

        mBottomNavBar?.selectedItemId = R.id.action_nav_preferences

    }

    override fun onCreatePreferences(savedInstanceState: Bundle?, rootKey: String?) {
        setPreferencesFromResource(xml, rootKey)
    }

    private fun setupBottomNavBar() {

        mBottomNavBar?.setOnItemSelectedListener { item ->

            when (item.itemId) {

                R.id.action_nav_home -> {

                    findNavController().navigate(R.id.global_action_to_events)
                }
                R.id.action_nav_parents -> {

                    findNavController().navigate(R.id.global_action_to_parents)
                }
                R.id.action_nav_cross_count -> {

                    findNavController().navigate(R.id.global_action_to_cross_tracker)
                }
            }

            true
        }
    }
}
