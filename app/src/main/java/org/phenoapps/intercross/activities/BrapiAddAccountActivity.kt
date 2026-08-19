package org.phenoapps.intercross.activities

import android.content.Intent
import android.os.Bundle
import androidx.activity.compose.setContent
import androidx.appcompat.app.AppCompatActivity
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.core.content.edit
import androidx.preference.PreferenceManager
import dagger.hilt.android.AndroidEntryPoint
import org.phenoapps.brapi.account.BrapiAccountRepository
import org.phenoapps.intercross.ui.settings.AddBrapiAccountStepper
import org.phenoapps.intercross.util.KeyUtil
import javax.inject.Inject

@AndroidEntryPoint
class BrapiAddAccountActivity : AppCompatActivity() {

    @Inject
    lateinit var accountRepository: BrapiAccountRepository

    @Inject
    lateinit var keyUtil: KeyUtil

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        setContent {
            Box(Modifier.fillMaxSize().padding(16.dp)) {
                AddBrapiAccountStepper(
                    onDismiss = { finish() },
                    onAuthorize = { url, displayName, oidcUrl, oidcFlow, clientId, oidcScope, version ->
                        accountRepository.addAccountConfig(
                            serverUrl = url,
                            displayName = displayName,
                            oidcUrl = oidcUrl,
                            oidcFlow = oidcFlow,
                            oidcClientId = clientId,
                            oidcScope = oidcScope,
                            brapiVersion = version,
                        )
                        accountRepository.setActiveAccount(url)
                        val prefs = PreferenceManager.getDefaultSharedPreferences(this@BrapiAddAccountActivity)
                        prefs.edit {
                            putString(keyUtil.brapiOidc, oidcUrl)
                            putString(keyUtil.brapiFlow, oidcFlow)
                            putString(keyUtil.brapiClient, clientId)
                            putString(keyUtil.brapiScope, oidcScope)
                        }
                        val intent = Intent(this@BrapiAddAccountActivity, BrapiAuthActivity::class.java).apply {
                            putExtra(BrapiAuthActivity.EXTRA_SERVER_URL, url)
                            putExtra(BrapiAuthActivity.EXTRA_OIDC_URL, oidcUrl)
                            putExtra(BrapiAuthActivity.EXTRA_OIDC_FLOW, oidcFlow)
                            putExtra(BrapiAuthActivity.EXTRA_OIDC_CLIENT_ID, clientId)
                            putExtra(BrapiAuthActivity.EXTRA_OIDC_SCOPE, oidcScope)
                            putExtra(BrapiAuthActivity.EXTRA_BRAPI_VERSION, version)
                        }
                        startActivity(intent)
                        finish()
                    },
                )
            }
        }
    }
}
