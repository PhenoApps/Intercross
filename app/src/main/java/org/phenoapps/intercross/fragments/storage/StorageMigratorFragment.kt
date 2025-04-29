package org.phenoapps.intercross.fragments.storage

import android.os.Bundle
import android.view.View
import org.phenoapps.fragments.storage.PhenoLibMigratorFragment

class StorageMigratorFragment : PhenoLibMigratorFragment() {
    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        // skip migrating
        activity?.finish()
    }
}