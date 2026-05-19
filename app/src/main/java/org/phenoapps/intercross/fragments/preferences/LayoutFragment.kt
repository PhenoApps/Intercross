package org.phenoapps.intercross.fragments.preferences

import android.os.Bundle
import org.phenoapps.intercross.R

class LayoutFragment : BasePreferenceFragment(R.xml.layout_preferences) {

    override fun onResume() {
        super.onResume()
        setToolbar(getString(R.string.prefs_layout_title))
    }

    override fun onCreatePreferences(savedInstanceState: Bundle?, rootKey: String?) {
        super.onCreatePreferences(savedInstanceState, rootKey)
    }
}
