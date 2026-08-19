package org.phenoapps.intercross.ui.app

import android.content.SharedPreferences
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.platform.LocalContext
import androidx.preference.PreferenceManager
import org.phenoapps.brapi.account.BrapiAccountRepository
import org.phenoapps.brapi.account.BrapiPreferenceKeys
import org.phenoapps.intercross.data.IntercrossDatabase
import org.phenoapps.intercross.util.KeyUtil

/**
 * Shared composable helpers used by all feature packages to access the database and preferences.
 */

@Composable
fun rememberDatabase(): IntercrossDatabase {
    val context = LocalContext.current.applicationContext
    return remember(context) { IntercrossDatabase.getInstance(context) }
}

@Composable
fun rememberPrefs(): Pair<SharedPreferences, KeyUtil> {
    val context = LocalContext.current
    return remember(context) {
        PreferenceManager.getDefaultSharedPreferences(context) to KeyUtil(context)
    }
}

@Composable
fun rememberBrapiAccountRepository(): BrapiAccountRepository {
    val context = LocalContext.current.applicationContext
    val (prefs, keyUtil) = rememberPrefs()
    return remember(context) {
        BrapiAccountRepository(
            context,
            prefs,
            BrapiPreferenceKeys(
                enabled = keyUtil.brapiEnabled,
                baseUrl = keyUtil.brapiUrl,
                displayName = keyUtil.brapiDisplayName,
                accessToken = keyUtil.brapiToken,
                idToken = keyUtil.brapiId,
                brapiVersion = "brapi_version",
                oidcUrl = keyUtil.brapiOidcUrl,
                oidcFlow = keyUtil.brapiOidcFlow,
                oidcClientId = keyUtil.brapiOidcClientId,
                oidcScope = keyUtil.brapiOidcScope,
            )
        )
    }
}
