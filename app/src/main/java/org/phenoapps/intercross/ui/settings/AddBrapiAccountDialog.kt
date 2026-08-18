package org.phenoapps.intercross.ui.settings

import android.widget.Toast
import android.accounts.AccountManager
import android.accounts.Account
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.platform.LocalContext
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import org.phenoapps.brapi.BrapiAccountConstants
import org.phenoapps.brapi.provider.R as BrapiR
import org.phenoapps.brapi.ui.BrapiAccountUiState
import org.phenoapps.brapi.ui.BrapiStepperAccountForm
import org.phenoapps.brapi.ui.defaultBrapiAccountState
import org.phenoapps.brapi.ui.isValidBrapiUrl
import org.phenoapps.brapi.ui.parseBrapiConfig
import org.phenoapps.brapi.ui.withConfig
import org.phenoapps.brapi.ui.withUrlUpdate
import org.phenoapps.intercross.R
import androidx.compose.ui.res.stringResource

/**
 * Full-screen composable hosting the BrAPI stepper account form.
 */
@Composable
fun AddBrapiAccountStepper(
    onDismiss: () -> Unit,
    initialAccount: Account? = null,
    onScanBaseUrl: () -> Unit = {},
    onScanConfig: () -> Unit = {},
    scanResult: String? = null,
    onScanResultConsumed: () -> Unit = {},
    onAuthorize: (
        baseUrl: String,
        displayName: String,
        oidcUrl: String,
        oidcFlow: String,
        clientId: String,
        scope: String,
    ) -> Unit,
) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    val clientIdDefault = stringResource(R.string.brapi_oidc_clientid_default)
    val am = remember { AccountManager.get(context) }

    var uiState by remember {
        val initialState = if (initialAccount != null) {
            BrapiAccountUiState(
                url = am.getUserData(initialAccount, BrapiAccountConstants.KEY_SERVER_URL) ?: initialAccount.name,
                displayName = am.getUserData(initialAccount, BrapiAccountConstants.KEY_DISPLAY_NAME) ?: initialAccount.name,
                oidcUrl = am.getUserData(initialAccount, BrapiAccountConstants.KEY_OIDC_URL) ?: "",
                oidcFlow = am.getUserData(initialAccount, BrapiAccountConstants.KEY_OIDC_FLOW) ?: "",
                oidcClientId = am.getUserData(initialAccount, BrapiAccountConstants.KEY_OIDC_CLIENT_ID) ?: "",
                oidcScope = am.getUserData(initialAccount, BrapiAccountConstants.KEY_OIDC_SCOPE) ?: "",
                brapiVersion = am.getUserData(initialAccount, BrapiAccountConstants.KEY_BRAPI_VERSION) ?: "V2",
                currentStep = 2 // Jump to final step for editing
            )
        } else {
            defaultBrapiAccountState(context, clientIdDefault)
        }
        mutableStateOf(initialState)
    }

    // Handle incoming scan result
    if (scanResult != null) {
        val config = parseBrapiConfig(scanResult)
        if (config != null) {
            // Scanned a full config JSON — populate all fields and jump to step 2
            uiState = uiState.withConfig(config).copy(currentStep = 2)
        } else {
            // Scanned a plain URL — treat as base URL update and go to step 1
            uiState = uiState.withUrlUpdate(scanResult).copy(currentStep = 1)
        }
        onScanResultConsumed()
    }

    fun normalizeUrl(url: String): String {
        var normalized = url.trim()
        if (!normalized.startsWith("http://") && !normalized.startsWith("https://")) {
            normalized = "https://$normalized"
        }
        return normalized.trimEnd('/')
    }

    fun onNext() {
        when (uiState.currentStep) {
            0 -> uiState = uiState.copy(currentStep = 1)
            1 -> {
                val normalized = normalizeUrl(uiState.url)
                if (normalized.isEmpty() || !isValidBrapiUrl(normalized)) {
                    Toast.makeText(
                        context,
                        BrapiR.string.pheno_brapi_invalid_url,
                        Toast.LENGTH_LONG,
                    ).show()
                    return
                }
                uiState = uiState.copy(url = normalized, currentStep = 2)
                // Fetch display name from server
                scope.launch {
                    uiState = uiState.copy(isFetchingDisplayName = true)
                    val displayText = withContext(Dispatchers.IO) {
                        runCatching {
                            val serverInfoUrl =
                                normalized.trimEnd('/') + "/brapi/v2/serverinfo"
                            val conn = java.net.URL(serverInfoUrl)
                                .openConnection() as java.net.HttpURLConnection
                            conn.connectTimeout = 5000
                            conn.readTimeout = 5000
                            if (conn.responseCode == 200) {
                                val body = conn.inputStream.bufferedReader().readText()
                                org.json.JSONObject(body)
                                    .optJSONObject("result")
                                    ?.optString("serverName")
                                    ?.takeIf { it.isNotEmpty() }
                            } else null
                        }.getOrNull()
                            ?: runCatching { java.net.URL(normalized).host }
                                .getOrDefault("")
                    }
                    uiState = uiState.copy(
                        displayName = displayText.takeIf { it.isNotEmpty() }
                            ?: uiState.displayName,
                        isFetchingDisplayName = false,
                    )
                }
            }
        }
    }

    fun onAuthorizeClicked() {
        val url = normalizeUrl(uiState.url)
        if (url.isEmpty() || !isValidBrapiUrl(url)) {
            Toast.makeText(
                context,
                BrapiR.string.pheno_brapi_invalid_url,
                Toast.LENGTH_LONG,
            ).show()
            return
        }
        val displayName = uiState.displayName.trim().ifEmpty { url }
        onAuthorize(
            url,
            displayName,
            uiState.oidcUrl.trim(),
            uiState.oidcFlow,
            uiState.oidcClientId.trim(),
            uiState.oidcScope.trim(),
        )
    }

    BrapiStepperAccountForm(
        uiState = uiState,
        titleRes = if (initialAccount != null) BrapiR.string.pheno_brapi_edit_account_title
                   else BrapiR.string.pheno_brapi_add_account_title,
        onUrlChange = { uiState = uiState.withUrlUpdate(it) },
        onDisplayNameChange = { uiState = uiState.copy(displayName = it) },
        onOidcUrlChange = { url, isUserEdit ->
            uiState = uiState.copy(
                oidcUrl = url,
                oidcUrlExplicitlySet = if (isUserEdit) true
                    else uiState.oidcUrlExplicitlySet,
            )
        },
        onOidcClientIdChange = { uiState = uiState.copy(oidcClientId = it) },
        onOidcScopeChange = { uiState = uiState.copy(oidcScope = it) },
        onOidcFlowChange = { uiState = uiState.copy(oidcFlow = it) },
        onBrapiVersionChange = { uiState = uiState.copy(brapiVersion = it) },
        onScanBaseUrl = onScanBaseUrl,
        onScanConfig = onScanConfig,
        onNext = { onNext() },
        onBack = {
            uiState = uiState.copy(
                currentStep = (uiState.currentStep - 1).coerceAtLeast(0)
            )
        },
        onCancel = onDismiss,
        onAuthorize = { onAuthorizeClicked() },
    )
}
