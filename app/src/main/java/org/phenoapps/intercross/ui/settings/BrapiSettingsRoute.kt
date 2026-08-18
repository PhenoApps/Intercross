package org.phenoapps.intercross.ui.settings

import android.accounts.Account
import android.accounts.AccountManager
import android.app.Activity
import android.content.Context
import android.content.Intent
import android.graphics.Bitmap
import android.graphics.Color
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.core.content.edit
import androidx.core.net.toUri
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import androidx.lifecycle.compose.LocalLifecycleOwner
import com.google.zxing.BarcodeFormat
import com.google.zxing.MultiFormatWriter
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import net.openid.appauth.AuthorizationService
import net.openid.appauth.EndSessionRequest
import org.json.JSONObject
import org.phenoapps.brapi.BrapiAccountConstants
import org.phenoapps.brapi.ui.BrapiServerCard
import org.phenoapps.intercross.R
import org.phenoapps.intercross.activities.BrapiAuthActivity
import org.phenoapps.intercross.ui.app.TopBarState
import org.phenoapps.intercross.ui.app.rememberBrapiAccountRepository
import org.phenoapps.intercross.ui.app.rememberPrefs
import org.phenoapps.intercross.ui.brapi.BrapiScaffold
import org.phenoapps.intercross.util.OpenAuthConfigurationUtil

@Composable
fun BrapiSettingsRoute(
    onShowMessage: (String) -> Unit,
    onBack: () -> Unit = {},
    onNavigateToAdvancedSettings: () -> Unit = {},
    onOpenScanner: () -> Unit = {},
    scanResult: String? = null,
    onScanResultConsumed: () -> Unit = {},
) {
    val context = LocalContext.current
    val (prefs, keyUtil) = rememberPrefs()
    val brapiAccountRepository = rememberBrapiAccountRepository()
    val authUtil = remember(context, prefs, keyUtil) {
        OpenAuthConfigurationUtil(context, keyUtil, prefs)
    }
    val brapiOidcUrlDefault = stringResource(R.string.brapi_oidc_url_default)
    val brapiMustConfigureUrl = stringResource(R.string.brapi_must_configure_url)
    val loggingOutWait = stringResource(R.string.logging_out_please_wait)
    val redirectUrl = stringResource(R.string.brapi_redirect_uri)

    var brapiEnabled by remember { mutableStateOf(prefs.getBoolean(keyUtil.brapiEnabled, false)) }
    var activeUrl by remember { mutableStateOf(prefs.getString(keyUtil.brapiUrl, "") ?: "") }

    // OIDC URL needed for auth launch validation
    val oidcUrl = prefs.getString(keyUtil.brapiOidc, brapiOidcUrlDefault).orEmpty()

    // Account list state
    val am = remember { AccountManager.get(context) }
    var accounts by remember { mutableStateOf(brapiAccountRepository.getAllAccounts()) }
    // Counter to force recomposition of cards when token state changes
    var tokenStateVersion by remember { mutableStateOf(0) }
    fun refreshAccounts() {
        accounts = brapiAccountRepository.getAllAccounts()
        activeUrl = prefs.getString(keyUtil.brapiUrl, "") ?: ""
        tokenStateVersion++
    }

    // Refresh accounts whenever the composable resumes (e.g. after navigation back)
    val lifecycleOwner = LocalLifecycleOwner.current
    DisposableEffect(lifecycleOwner) {
        val observer = LifecycleEventObserver { _, event ->
            if (event == Lifecycle.Event.ON_RESUME) {
                accounts = brapiAccountRepository.getAllAccounts()
            }
        }
        lifecycleOwner.lifecycle.addObserver(observer)
        onDispose { lifecycleOwner.lifecycle.removeObserver(observer) }
    }

    // Dialog states
    var showHttpWarning by remember { mutableStateOf(false) }
    var showCommunityServers by remember { mutableStateOf(false) }
    var showAddAccountStepper by remember { mutableStateOf(false) }
    var accountToDelete by remember { mutableStateOf<Account?>(null) }
    // Track pending scan type: "base_url" or "config"
    var pendingScanType by remember { mutableStateOf<String?>(null) }
    // Track which card is expanded
    var expandedAccountUrl by remember { mutableStateOf<String?>(null) }
    // Track URL for HTTP warning retry
    var pendingHttpWarningUrl by remember { mutableStateOf("") }
    // Track URL being logged out for the launcher callback
    var pendingLogoutUrl by remember { mutableStateOf("") }
    var compatibilityMessage by remember { mutableStateOf<String?>(null) }
    var qrCodeBitmap by remember { mutableStateOf<Bitmap?>(null) }
    var accountToEdit by remember { mutableStateOf<Account?>(null) }
    var pendingChooseAccount by remember { mutableStateOf<Account?>(null) }
    val scope = rememberCoroutineScope()

    val authLauncher = rememberLauncherForActivityResult(ActivityResultContracts.StartActivityForResult()) {
        // Refresh accounts after auth completes so auth status chip updates
        refreshAccounts()
    }
    val logoutLauncher = rememberLauncherForActivityResult(ActivityResultContracts.StartActivityForResult()) { result ->
        if (result.resultCode == Activity.RESULT_OK) {
            brapiAccountRepository.clearToken(pendingLogoutUrl)
            refreshAccounts()
        }
    }
    val accountChooserLauncher = rememberLauncherForActivityResult(ActivityResultContracts.StartActivityForResult()) { result ->
        val pending = pendingChooseAccount
        pendingChooseAccount = null

        if (result.resultCode == Activity.RESULT_OK && result.data != null) {
            val accountName = result.data?.getStringExtra(AccountManager.KEY_ACCOUNT_NAME)
            val accountType = result.data?.getStringExtra(AccountManager.KEY_ACCOUNT_TYPE)
                ?: BrapiAccountConstants.ACCOUNT_TYPE
            if (accountName != null && accountType == BrapiAccountConstants.ACCOUNT_TYPE) {
                val selected = Account(accountName, accountType)
                val accountToUse = pending ?: selected
                if (pending != null && (pending.name != selected.name || pending.type != selected.type)) return@rememberLauncherForActivityResult

                if (brapiAccountRepository.canAccessAccount(accountToUse)) {
                    brapiAccountRepository.grantSelectedAccount(accountToUse)
                    if (brapiAccountRepository.canUseToken(accountToUse)) {
                        val url = am.getUserData(accountToUse, BrapiAccountConstants.KEY_SERVER_URL) ?: accountToUse.name
                        brapiAccountRepository.setActiveAccount(url)
                        refreshAccounts()
                    }
                }
            }
        }
    }

    fun launchAuth(serverUrl: String, allowHttp: Boolean = false) {
        pendingHttpWarningUrl = serverUrl
        // Read per-account OIDC config from AccountManager
        val account = brapiAccountRepository.getAccountByUrl(serverUrl)

        if (account != null && !brapiAccountRepository.canUseToken(account)) {
            pendingChooseAccount = account
            accountChooserLauncher.launch(brapiAccountRepository.buildChooseAccountIntent(account))
            return
        }

        val accountOidcUrl = account?.let {
            am.getUserData(it, BrapiAccountConstants.KEY_OIDC_URL)
        }?.takeIf { it.isNotEmpty() } ?: oidcUrl
        val accountOidcFlow = account?.let {
            am.getUserData(it, BrapiAccountConstants.KEY_OIDC_FLOW)
        } ?: ""
        val accountOidcClientId = account?.let {
            am.getUserData(it, BrapiAccountConstants.KEY_OIDC_CLIENT_ID)
        } ?: ""
        val accountOidcScope = account?.let {
            am.getUserData(it, BrapiAccountConstants.KEY_OIDC_SCOPE)
        } ?: ""
        val accountBrapiVersion = account?.let {
            am.getUserData(it, BrapiAccountConstants.KEY_BRAPI_VERSION)
        } ?: ""

        // Validate URL schemes before launching
        val baseIsHttp = serverUrl.startsWith("http://")
        val oidcIsHttp = accountOidcUrl.startsWith("http://")
        if (!allowHttp && (baseIsHttp || oidcIsHttp)) {
            showHttpWarning = true
            return
        }
        val hasValidBaseScheme = serverUrl.startsWith("https://") || (allowHttp && baseIsHttp)
        val hasValidOidcScheme = accountOidcUrl.startsWith("https://") || (allowHttp && oidcIsHttp)
        if (!hasValidBaseScheme || !hasValidOidcScheme) {
            onShowMessage(brapiMustConfigureUrl)
            return
        }

        // Launch with per-account extras
        authLauncher.launch(
            Intent(context, BrapiAuthActivity::class.java).apply {
                putExtra(BrapiAuthActivity.EXTRA_SERVER_URL, serverUrl)
                putExtra(BrapiAuthActivity.EXTRA_OIDC_URL, accountOidcUrl)
                putExtra(BrapiAuthActivity.EXTRA_OIDC_FLOW, accountOidcFlow)
                putExtra(BrapiAuthActivity.EXTRA_OIDC_CLIENT_ID, accountOidcClientId)
                putExtra(BrapiAuthActivity.EXTRA_OIDC_SCOPE, accountOidcScope)
                putExtra(BrapiAuthActivity.EXTRA_BRAPI_VERSION, accountBrapiVersion)
            }
        )
    }

    fun logout(serverUrl: String) {
        pendingLogoutUrl = serverUrl
        val idToken = brapiAccountRepository.peekIdToken()
        if (idToken == null) {
            brapiAccountRepository.clearToken(serverUrl)
            refreshAccounts()
            return
        }
        onShowMessage(loggingOutWait)
        authUtil.getAuthServiceConfiguration { config, _ ->
            if (config == null) {
                brapiAccountRepository.clearToken(serverUrl)
                refreshAccounts()
                return@getAuthServiceConfiguration
            }
            val request = EndSessionRequest.Builder(config)
                .setIdTokenHint(idToken)
                .setPostLogoutRedirectUri(redirectUrl.toUri())
                .build()
            logoutLauncher.launch(AuthorizationService(context).getEndSessionRequestIntent(request))
        }
    }

    fun checkServerCompatibility(account: Account) {
        val url = am.getUserData(account, BrapiAccountConstants.KEY_SERVER_URL) ?: account.name
        scope.launch {
            val message = withContext(Dispatchers.IO) {
                runCatching {
                    val serverInfoUrl = url.trimEnd('/') + "/brapi/v2/serverinfo"
                    val conn = java.net.URL(serverInfoUrl).openConnection() as java.net.HttpURLConnection
                    conn.connectTimeout = 5000
                    conn.readTimeout = 5000
                    if (conn.responseCode != 200) return@runCatching null
                    val result = JSONObject(conn.inputStream.bufferedReader().readText())
                        .optJSONObject("result")
                    val name = result?.optString("serverName")?.takeIf { it.isNotEmpty() } ?: url
                    val calls = result?.optJSONArray("calls")?.length()
                    if (calls != null) "$name\n$calls BrAPI calls advertised" else name
                }.getOrNull()
            }
            compatibilityMessage = message ?: context.getString(R.string.brapi_server_returned_empty_response)
        }
    }

    fun shareAccountSettings(account: Account) {
        val serverUrl = am.getUserData(account, BrapiAccountConstants.KEY_SERVER_URL) ?: account.name
        val displayName = am.getUserData(account, BrapiAccountConstants.KEY_DISPLAY_NAME) ?: account.name
        val oidcUrl = am.getUserData(account, BrapiAccountConstants.KEY_OIDC_URL) ?: ""
        val oidcFlow = am.getUserData(account, BrapiAccountConstants.KEY_OIDC_FLOW) ?: ""
        val clientId = am.getUserData(account, BrapiAccountConstants.KEY_OIDC_CLIENT_ID) ?: ""
        val oidcScope = am.getUserData(account, BrapiAccountConstants.KEY_OIDC_SCOPE) ?: ""
        val version = am.getUserData(account, BrapiAccountConstants.KEY_BRAPI_VERSION) ?: ""

        val jsonConfig = JSONObject().apply {
            put("url", serverUrl)
            put("name", displayName)
            put("version", version)
            put("authFlow", oidcFlow)
            put("oidcUrl", oidcUrl)
            put("clientId", clientId)
            put("scope", oidcScope)
            put("pageSize", prefs.getString("BRAPI_PAGE_SIZE", "50"))
            put("serverTimeoutMilli", prefs.getString("BRAPI_TIMEOUT", "120"))
        }.toString()

        val size = (context.resources.displayMetrics.widthPixels * 0.8f).toInt().coerceAtLeast(320)
        val matrix = MultiFormatWriter().encode(jsonConfig, BarcodeFormat.QR_CODE, size, size)
        val bitmap = Bitmap.createBitmap(size, size, Bitmap.Config.RGB_565)
        for (x in 0 until size) {
            for (y in 0 until size) {
                bitmap.setPixel(x, y, if (matrix[x, y]) Color.BLACK else Color.WHITE)
            }
        }
        qrCodeBitmap = bitmap
    }

    val topBarState = TopBarState(titleRes = R.string.prefs_brapi_title, showBack = true, onBack = onBack)

    BrapiScaffold(topBarState = topBarState) { innerPadding ->
    Box(Modifier.padding(innerPadding)) {
    LazyColumn(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(8.dp),
    ) {
        // BrAPI enable switch
        item {
            SettingsSwitchRow(
                titleRes = R.string.preferences_brapi_enable_title,
                summaryRes = R.string.preferences_brapi_enable_summary,
                iconRes = R.drawable.ic_adv_brapi,
                checked = brapiEnabled,
                onCheckedChange = { enabled ->
                    brapiEnabled = enabled
                    prefs.edit {
                        putBoolean(keyUtil.brapiEnabled, enabled)
                        if (!enabled) remove(keyUtil.brapiToken)
                    }
                    if (!enabled) refreshAccounts()
                },
            )
        }

        if (brapiEnabled) {
            val (owned, shared) = accounts.partition { brapiAccountRepository.isOwnAccount(it) }
            val activeAccount = accounts.find {
                (am.getUserData(it, BrapiAccountConstants.KEY_SERVER_URL) ?: it.name) == activeUrl
            }
            val availableOwned = owned.filter {
                (am.getUserData(it, BrapiAccountConstants.KEY_SERVER_URL) ?: it.name) != activeUrl
            }

            // BrAPI Actions (Add Account, Shared Servers, Advanced)
            item {
                SettingsActionRow(
                    titleRes = org.phenoapps.brapi.provider.R.string.pheno_brapi_add_account,
                    summaryRes = R.string.brapi_no_accounts_empty_state,
                    iconRes = R.drawable.pheno_brapi_ic_key,
                    onClick = { showCommunityServers = true },
                )
            }
            item {
                SettingsActionRow(
                    titleRes = org.phenoapps.brapi.provider.R.string.pheno_brapi_shared_servers,
                    summaryRes = R.string.brapi_server_info_pref_summary,
                    iconRes = R.drawable.pheno_brapi_ic_share,
                    onClick = {
                        pendingChooseAccount = null
                        accountChooserLauncher.launch(brapiAccountRepository.buildChooseAccountIntent())
                    },
                )
            }
            item {
                SettingsActionRow(
                    titleRes = R.string.brapi_advanced_settings_title,
                    summaryRes = R.string.brapi_advanced_settings_summary,
                    iconRes = R.drawable.cog,
                    onClick = { onNavigateToAdvancedSettings() },
                )
            }

            // Active Server category
            if (activeAccount != null) {
                item { SettingsSectionHeader(R.string.brapi_active_server_category) }
                item {
                    val serverUrl = am.getUserData(activeAccount, BrapiAccountConstants.KEY_SERVER_URL) ?: activeAccount.name
                    val displayName = am.getUserData(activeAccount, BrapiAccountConstants.KEY_DISPLAY_NAME) ?: activeAccount.name
                    @Suppress("UNUSED_EXPRESSION") tokenStateVersion
                    val hasToken = !brapiAccountRepository.peekTokenForAccount(activeAccount).isNullOrEmpty()
                    val isExpanded = expandedAccountUrl == serverUrl
                    BrapiServerCard(
                        displayName = displayName,
                        serverUrl = serverUrl,
                        isActive = true,
                        hasToken = hasToken,
                        isExpanded = isExpanded,
                        onToggleExpanded = {
                            expandedAccountUrl = if (isExpanded) null else serverUrl
                        },
                        onEnable = {
                            if (!brapiAccountRepository.canUseToken(activeAccount)) {
                                pendingChooseAccount = activeAccount
                                accountChooserLauncher.launch(brapiAccountRepository.buildChooseAccountIntent(activeAccount))
                            } else {
                                brapiAccountRepository.setActiveAccount(serverUrl)
                                refreshAccounts()
                                if (!hasToken) {
                                    launchAuth(serverUrl)
                                }
                            }
                        },
                        onAuthorize = {
                            brapiAccountRepository.setActiveAccount(serverUrl)
                            refreshAccounts()
                            launchAuth(serverUrl)
                        },
                        onLogOut = {
                            logout(serverUrl)
                        },
                        onCheckCompatibility = { checkServerCompatibility(activeAccount) },
                        onShareSettings = { shareAccountSettings(activeAccount) },
                        onEdit = { accountToEdit = activeAccount },
                        onRemove = { accountToDelete = activeAccount },
                        onRequestSwitchServer = {
                            brapiAccountRepository.setActiveAccount(serverUrl)
                            refreshAccounts()
                            launchAuth(serverUrl)
                        },
                    )
                }
            }

            // Available Servers category
            if (availableOwned.isNotEmpty()) {
                item { SettingsSectionHeader(R.string.brapi_available_servers_category) }
                items(availableOwned.size) { index ->
                    val account = availableOwned[index]
                    val serverUrl = am.getUserData(account, BrapiAccountConstants.KEY_SERVER_URL) ?: account.name
                    val displayName = am.getUserData(account, BrapiAccountConstants.KEY_DISPLAY_NAME) ?: account.name
                    @Suppress("UNUSED_EXPRESSION") tokenStateVersion
                    val hasToken = !brapiAccountRepository.peekTokenForAccount(account).isNullOrEmpty()
                    val isExpanded = expandedAccountUrl == serverUrl
                    BrapiServerCard(
                        displayName = displayName,
                        serverUrl = serverUrl,
                        isActive = false,
                        hasToken = hasToken,
                        isExpanded = isExpanded,
                        onToggleExpanded = {
                            expandedAccountUrl = if (isExpanded) null else serverUrl
                        },
                        onEnable = {
                            if (!brapiAccountRepository.canUseToken(account)) {
                                pendingChooseAccount = account
                                accountChooserLauncher.launch(brapiAccountRepository.buildChooseAccountIntent(account))
                            } else {
                                brapiAccountRepository.setActiveAccount(serverUrl)
                                refreshAccounts()
                                if (!hasToken) {
                                    launchAuth(serverUrl)
                                }
                            }
                        },
                        onAuthorize = {
                            brapiAccountRepository.setActiveAccount(serverUrl)
                            refreshAccounts()
                            launchAuth(serverUrl)
                        },
                        onLogOut = {
                            logout(serverUrl)
                        },
                        onCheckCompatibility = { checkServerCompatibility(account) },
                        onShareSettings = { shareAccountSettings(account) },
                        onEdit = { accountToEdit = account },
                        onRemove = { accountToDelete = account },
                        onRequestSwitchServer = {
                            brapiAccountRepository.setActiveAccount(serverUrl)
                            refreshAccounts()
                            launchAuth(serverUrl)
                        },
                    )
                }
            }

            // Shared Accounts category
            if (shared.isNotEmpty()) {
                item { SettingsSectionHeader(R.string.brapi_shared_accounts_category) }

                items(shared.size) { index ->
                    val account = shared[index]
                    val serverUrl = am.getUserData(account, BrapiAccountConstants.KEY_SERVER_URL) ?: account.name
                    val displayName = am.getUserData(account, BrapiAccountConstants.KEY_DISPLAY_NAME) ?: account.name
                    val ownerPkg = am.getUserData(account, BrapiAccountConstants.KEY_OWNER_PACKAGE) ?: ""
                    val ownerLabel = stringResource(
                        org.phenoapps.brapi.provider.R.string.pheno_brapi_shared_account_from,
                        BrapiAccountConstants.displayNameForPackage(ownerPkg),
                    )
                    val isActive = (am.getUserData(account, BrapiAccountConstants.KEY_SERVER_URL) ?: account.name) == activeUrl
                    @Suppress("UNUSED_EXPRESSION") tokenStateVersion
                    val hasToken = !brapiAccountRepository.peekTokenForAccount(account).isNullOrEmpty()
                    val isExpanded = expandedAccountUrl == serverUrl
                    BrapiServerCard(
                        displayName = displayName,
                        serverUrl = serverUrl,
                        isActive = isActive,
                        hasToken = hasToken,
                        isExpanded = isExpanded,
                        ownerLabel = ownerLabel,
                        onToggleExpanded = {
                            expandedAccountUrl = if (isExpanded) null else serverUrl
                        },
                        onEnable = {
                            if (!brapiAccountRepository.canUseToken(account)) {
                                pendingChooseAccount = account
                                accountChooserLauncher.launch(brapiAccountRepository.buildChooseAccountIntent(account))
                            } else {
                                brapiAccountRepository.setActiveAccount(serverUrl)
                                refreshAccounts()
                                if (!hasToken) {
                                    launchAuth(serverUrl)
                                }
                            }
                        },
                        onAuthorize = {
                            brapiAccountRepository.setActiveAccount(serverUrl)
                            refreshAccounts()
                            launchAuth(serverUrl)
                        },
                        onLogOut = {
                            logout(serverUrl)
                        },
                        onCheckCompatibility = { checkServerCompatibility(account) },
                        onShareSettings = { shareAccountSettings(account) },
                        onEdit = null, // Can't edit foreign accounts
                        onRemove = { accountToDelete = account },
                        onRequestSwitchServer = {
                            brapiAccountRepository.setActiveAccount(serverUrl)
                            refreshAccounts()
                            launchAuth(serverUrl)
                        },
                    )
                }
            }
        }
    }

    // Delete confirmation dialog
    accountToDelete?.let { account ->
        val deleteDisplayName = am.getUserData(account, BrapiAccountConstants.KEY_DISPLAY_NAME) ?: account.name
        val deleteServerUrl = am.getUserData(account, BrapiAccountConstants.KEY_SERVER_URL) ?: account.name
        AlertDialog(
            onDismissRequest = { accountToDelete = null },
            title = { Text(stringResource(R.string.dialog_confirm_delete_account_title)) },
            text = { Text("Remove $deleteDisplayName?") },
            confirmButton = {
                TextButton(onClick = {
                    brapiAccountRepository.removeAccount(deleteServerUrl)
                    refreshAccounts()
                    accountToDelete = null
                }) { Text(stringResource(R.string.dialog_ok)) }
            },
            dismissButton = {
                TextButton(onClick = { accountToDelete = null }) { Text(stringResource(R.string.dialog_cancel)) }
            },
        )
    }

    // HTTP warning dialog
    if (showHttpWarning) {
        AlertDialog(
            onDismissRequest = { showHttpWarning = false },
            title = { Text(stringResource(R.string.act_brapi_auth_http_warning_title)) },
            text = { Text(stringResource(R.string.act_brapi_auth_http_warning_message)) },
            confirmButton = {
                TextButton(
                    onClick = {
                        showHttpWarning = false
                        launchAuth(pendingHttpWarningUrl, allowHttp = true)
                    },
                ) {
                    Text(stringResource(R.string.ok))
                }
            },
            dismissButton = {
                TextButton(onClick = { showHttpWarning = false }) {
                    Text(stringResource(R.string.dialog_cancel))
                }
            },
        )
    }

    // Compatibility dialog
    compatibilityMessage?.let { message ->
        AlertDialog(
            onDismissRequest = { compatibilityMessage = null },
            title = { Text(stringResource(R.string.brapi_compatibility)) },
            text = { Text(message) },
            confirmButton = {
                TextButton(onClick = { compatibilityMessage = null }) {
                    Text(stringResource(R.string.dialog_ok))
                }
            }
        )
    }

    // QR Share dialog
    qrCodeBitmap?.let { bitmap ->
        AlertDialog(
            onDismissRequest = { qrCodeBitmap = null },
            title = { Text(stringResource(R.string.preferences_brapi_barcode_config_dialog_title)) },
            text = {
                Image(
                    bitmap = bitmap.asImageBitmap(),
                    contentDescription = null,
                    modifier = Modifier.fillMaxWidth(),
                    contentScale = ContentScale.FillWidth
                )
            },
            confirmButton = {
                TextButton(onClick = { qrCodeBitmap = null }) {
                    Text(stringResource(R.string.ok))
                }
            }
        )
    }

    // Add Account stepper form
    if (showCommunityServers || showAddAccountStepper || accountToEdit != null) {
        AddBrapiAccountStepper(
            onDismiss = {
                showCommunityServers = false
                showAddAccountStepper = false
                accountToEdit = null
            },
            initialAccount = accountToEdit,
            onScanBaseUrl = {
                pendingScanType = "base_url"
                onOpenScanner()
            },
            onScanConfig = {
                pendingScanType = "config"
                onOpenScanner()
            },
            scanResult = scanResult,
            onScanResultConsumed = onScanResultConsumed,
            onAuthorize = { url, displayName, oidc, flow, clientId, oidcScope ->
                // Add the account via repository
                brapiAccountRepository.addAccountConfig(
                    serverUrl = url,
                    displayName = displayName,
                    oidcUrl = oidc,
                    oidcFlow = flow,
                    oidcClientId = clientId,
                    oidcScope = oidcScope,
                )
                brapiAccountRepository.setActiveAccount(url)
                prefs.edit {
                    putString(keyUtil.brapiOidc, oidc)
                    putString(keyUtil.brapiFlow, flow)
                    putString(keyUtil.brapiClient, clientId)
                    putString(keyUtil.brapiScope, oidcScope)
                }
                refreshAccounts()
                showCommunityServers = false
                showAddAccountStepper = false
                accountToEdit = null
                launchAuth(url)
            },
        )
    }
    } // Box
    } // Scaffold
}
