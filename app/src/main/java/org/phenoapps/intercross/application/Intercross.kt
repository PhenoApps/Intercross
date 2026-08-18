package org.phenoapps.intercross.application

import android.app.Application
import dagger.hilt.android.HiltAndroidApp
import org.phenoapps.brapi.account.BrapiAccountRepository
import javax.inject.Inject

@HiltAndroidApp
class Intercross : Application() {

    @Inject
    lateinit var brapiAccountRepository: BrapiAccountRepository

    override fun onCreate() {
        super.onCreate()
        // Run one-time migration from SharedPreferences to AccountManager.
        // Must run before any BrAPI UI or auth flow is accessed.
        brapiAccountRepository.migrateFromPrefsIfNeeded()
    }
}