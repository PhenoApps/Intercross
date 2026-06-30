package org.phenoapps.intercross.ui.settings

import android.app.Activity
import android.content.Context
import android.content.Intent
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringArrayResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.core.content.edit
import androidx.core.net.toUri
import net.openid.appauth.AuthorizationService
import net.openid.appauth.EndSessionRequest
import org.phenoapps.intercross.R
import org.phenoapps.intercross.activities.BrapiAuthActivity
import androidx.compose.foundation.layout.Box
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.ui.res.painterResource
import org.phenoapps.intercross.ui.app.TopBarState
import org.phenoapps.intercross.ui.app.rememberPrefs
import org.phenoapps.intercross.ui.brapi.BrapiScaffold
import org.phenoapps.intercross.ui.theme.AppTheme
import org.phenoapps.intercross.util.OpenAuthConfigurationUtil

private const val BRAPI_VERSION_KEY = "BRAPI_VERSION"
private const val BRAPI_PAGE_SIZE_KEY = "BRAPI_PAGE_SIZE"
private const val BRAPI_CHUNK_SIZE_KEY = "BRAPI_CHUNK_SIZE"
private const val BRAPI_TIMEOUT_KEY = "BRAPI_TIMEOUT"
private const val BRAPI_MAX_OBSERVATION_TRANSFER_KEY = "BRAPI_MAX_CONCURRENT_OBSERVATION_TRANSFER"

@Composable
fun BrapiSettingsRoute(
    onShowMessage: (String) -> Unit,
    onBack: () -> Unit = {},
) {
    val context = LocalContext.current
    val (prefs, keyUtil) = rememberPrefs()
    val authUtil = remember(context, prefs, keyUtil) {
        OpenAuthConfigurationUtil(context, keyUtil, prefs)
    }
    val brapiBaseUrlDefault = stringResource(R.string.brapi_base_url_default)
    val brapiEditDisplayNameDefault = stringResource(R.string.brapi_edit_display_name_default)
    val brapiOidcUrlDefault = stringResource(R.string.brapi_oidc_url_default)
    val brapiOidcClientIdDefault = stringResource(R.string.brapi_oidc_clientid_default)
    val brapiOidcScopeDefault = stringResource(R.string.brapi_oidc_scope_default)
    val brapiVersionV2 = stringResource(R.string.preferences_brapi_version_v2)
    val brapiMustConfigureUrl = stringResource(R.string.brapi_must_configure_url)
    val exportSourceBrapi = stringResource(R.string.export_source_brapi)
    val loggingOutWait = stringResource(R.string.logging_out_please_wait)

    var brapiEnabled by remember { mutableStateOf(prefs.getBoolean(keyUtil.brapiEnabled, false)) }
    var baseUrl by remember {
        mutableStateOf(prefs.getString(keyUtil.brapiUrl, brapiBaseUrlDefault).orEmpty())
    }
    var oldBaseUrl by remember { mutableStateOf(baseUrl) }
    var displayName by remember {
        mutableStateOf(
            prefs.getString(keyUtil.brapiDisplayName, brapiEditDisplayNameDefault).orEmpty(),
        )
    }
    var oidcUrl by remember {
        mutableStateOf(prefs.getString(keyUtil.brapiOidc, brapiOidcUrlDefault).orEmpty())
    }
    val implicitFlow = stringResource(R.string.preferences_brapi_oidc_flow_oauth_implicit)
    val oldCustomFlow = stringResource(R.string.preferences_brapi_oidc_flow_old_custom)
    var oidcFlow by remember {
        mutableStateOf(prefs.getString(keyUtil.brapiFlow, implicitFlow).orEmpty())
    }
    var clientId by remember {
        mutableStateOf(prefs.getString(keyUtil.brapiClient, brapiOidcClientIdDefault).orEmpty())
    }
    var scope by remember {
        mutableStateOf(prefs.getString(keyUtil.brapiScope, brapiOidcScopeDefault).orEmpty())
    }
    var brapiVersion by remember {
        mutableStateOf(prefs.getString(BRAPI_VERSION_KEY, brapiVersionV2).orEmpty())
    }
    var pageSize by remember { mutableStateOf(prefs.getString(BRAPI_PAGE_SIZE_KEY, "50").orEmpty()) }
    var chunkSize by remember { mutableStateOf(prefs.getString(BRAPI_CHUNK_SIZE_KEY, "500").orEmpty()) }
    var timeout by remember { mutableStateOf(prefs.getString(BRAPI_TIMEOUT_KEY, "120").orEmpty()) }
    var concurrentTransfers by remember {
        mutableStateOf(prefs.getString(BRAPI_MAX_OBSERVATION_TRANSFER_KEY, "5").orEmpty())
    }
    var authToken by remember { mutableStateOf(prefs.getString(keyUtil.brapiToken, null)) }
    var showHttpWarning by remember { mutableStateOf(false) }
    var showCommunityServers by remember { mutableStateOf(false) }

    val authLauncher = rememberLauncherForActivityResult(ActivityResultContracts.StartActivityForResult()) {
        authToken = prefs.getString(keyUtil.brapiToken, null)
    }
    val logoutLauncher = rememberLauncherForActivityResult(ActivityResultContracts.StartActivityForResult()) { result ->
        if (result.resultCode == Activity.RESULT_OK) {
            prefs.edit {
                remove(keyUtil.brapiId)
                remove(keyUtil.brapiToken)
            }
            authToken = null
        }
    }

    LaunchedEffect(implicitFlow, oldCustomFlow) {
        if (oidcFlow == oldCustomFlow) {
            oidcFlow = implicitFlow
            prefs.edit { putString(keyUtil.brapiFlow, implicitFlow) }
        }
    }

    fun launchAuth(allowHttp: Boolean) {
        startBrapiAuth(
            context = context,
            baseUrl = baseUrl,
            oidcUrl = oidcUrl,
            allowHttp = allowHttp,
            onHttpWarning = { showHttpWarning = true },
            onInvalidUrl = { onShowMessage(brapiMustConfigureUrl) },
            launch = { authLauncher.launch(it) },
        )
    }

    fun persistBaseUrl(nextUrl: String) {
        val nextDisplayName = serverDisplayName(nextUrl, exportSourceBrapi)
        val nextOidcUrl = if (prefs.getBoolean(keyUtil.brapiExplicitOidcUrl, false)) {
            oidcUrl
        } else {
            oidcUrl.replaceFirst(oldBaseUrl, nextUrl)
        }
        baseUrl = nextUrl
        oldBaseUrl = nextUrl
        displayName = nextDisplayName
        oidcUrl = nextOidcUrl
        prefs.edit {
            putString(keyUtil.brapiUrl, nextUrl)
            putString(keyUtil.brapiDisplayName, nextDisplayName)
            putString(keyUtil.brapiOidc, nextOidcUrl)
        }
        launchAuth(allowHttp = false)
    }

    fun logout() {
        val idToken = prefs.getString(keyUtil.brapiId, null)
        if (idToken == null) {
            prefs.edit { remove(keyUtil.brapiToken) }
            authToken = null
            return
        }
        onShowMessage(loggingOutWait)
        authUtil.getAuthServiceConfiguration { config, _ ->
            if (config == null) {
                prefs.edit {
                    remove(keyUtil.brapiId)
                    remove(keyUtil.brapiToken)
                }
                authToken = null
                return@getAuthServiceConfiguration
            }
            val request = EndSessionRequest.Builder(config)
                .setIdTokenHint(idToken)
                .setPostLogoutRedirectUri(BrapiAuthActivity.REDIRECT_URI.toUri())
                .build()
            logoutLauncher.launch(AuthorizationService(context).getEndSessionRequestIntent(request))
        }
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
                    if (!enabled) authToken = null
                },
            )
        }

        if (brapiEnabled) {
            item { SettingsSectionHeader(R.string.preferences_brapi_server_title) }
            item {
                TextPreferenceRow(
                    titleRes = R.string.brapi_base_url,
                    summary = baseUrl,
                    messageRes = R.string.brapi_base_url_desc,
                    iconRes = R.drawable.ic_adv_brapi_base,
                    value = baseUrl,
                    onSave = { persistBaseUrl(it) },
                )
            }
            item {
                TextPreferenceRow(
                    titleRes = R.string.brapi_display_name,
                    summary = displayName,
                    iconRes = R.drawable.ic_pref_brapi_name,
                    value = displayName,
                    onSave = {
                        val next = it.ifBlank { exportSourceBrapi }
                        displayName = next
                        prefs.edit { putString(keyUtil.brapiDisplayName, next) }
                    },
                )
            }
            item {
                SettingsActionRow(
                    titleRes = R.string.preferences_brapi_servers_title,
                    summaryRes = R.string.preferences_brapi_server_add,
                    iconRes = R.drawable.ic_pref_brapi_server_add,
                    onClick = { showCommunityServers = true },
                )
            }
            item {
                SettingsActionRow(
                    titleRes = R.string.menu_action_brapi_pref_title,
                    summary = authToken?.let { stringResource(R.string.brapi_auth_success) }
                        ?: stringResource(R.string.brapi_auth_needed_title),
                    iconRes = R.drawable.lock_reset,
                    onClick = { launchAuth(allowHttp = false) },
                )
            }
            if (authToken != null) {
                item {
                    SettingsActionRow(
                        titleRes = R.string.brapi_revoke_auth,
                        iconRes = R.drawable.ic_pref_brapi_logout,
                        onClick = { logout() },
                    )
                }
            }

            item { SettingsSectionHeader(R.string.preferences_brapi_authorization_title) }
            item {
                val flowEntries = stringArrayResource(R.array.pref_brapi_oidc_flow).toList()
                SettingsSingleChoiceRow(
                    titleRes = R.string.preferences_brapi_oidc_flow,
                    summary = oidcFlow.ifBlank { implicitFlow },
                    iconRes = R.drawable.ic_pref_brapi_version,
                    entries = flowEntries,
                    values = flowEntries,
                    selectedValue = oidcFlow.ifBlank { implicitFlow },
                    onSelected = {
                        oidcFlow = it
                        prefs.edit { putString(keyUtil.brapiFlow, it) }
                    },
                )
            }
            if (oidcFlow != oldCustomFlow) {
                item {
                    TextPreferenceRow(
                        titleRes = R.string.brapi_oidc_url,
                        summary = oidcUrl,
                        messageRes = R.string.brapi_oidc_url_desc,
                        iconRes = R.drawable.ic_adv_brapi_base,
                        value = oidcUrl,
                        onSave = {
                            oidcUrl = it
                            prefs.edit {
                                putString(keyUtil.brapiOidc, it)
                                putBoolean(keyUtil.brapiExplicitOidcUrl, true)
                            }
                        },
                    )
                }
            }
            item {
                TextPreferenceRow(
                    titleRes = R.string.brapi_oidc_clientid,
                    summary = clientId.ifBlank { stringResource(R.string.brapi_oidc_clientid_desc) },
                    messageRes = R.string.brapi_oidc_clientid_desc,
                    iconRes = R.drawable.ic_pref_brapi_client_id,
                    value = clientId,
                    onSave = {
                        clientId = it
                        prefs.edit { putString(keyUtil.brapiClient, it) }
                    },
                )
            }
            item {
                TextPreferenceRow(
                    titleRes = R.string.brapi_oidc_scope,
                    summary = scope.ifBlank { stringResource(R.string.brapi_oidc_scope_desc) },
                    messageRes = R.string.brapi_oidc_scope_desc,
                    iconRes = R.drawable.ic_pref_brapi_scope,
                    value = scope,
                    onSave = {
                        scope = it
                        prefs.edit { putString(keyUtil.brapiScope, it) }
                    },
                )
            }

            item { SettingsSectionHeader(R.string.preferences_brapi_advanced_title) }
            item {
                val versions = stringArrayResource(R.array.pref_brapi_version).toList()
                SettingsSingleChoiceRow(
                    titleRes = R.string.preferences_brapi_version,
                    summary = brapiVersion.ifBlank { brapiVersionV2 },
                    iconRes = R.drawable.ic_pref_brapi_version,
                    entries = versions,
                    values = versions,
                    selectedValue = brapiVersion.ifBlank { brapiVersionV2 },
                    onSelected = {
                        brapiVersion = it
                        prefs.edit { putString(BRAPI_VERSION_KEY, it) }
                    },
                )
            }
            item {
                TextPreferenceRow(
                    titleRes = R.string.brapi_pagination,
                    summary = pageSize,
                    iconRes = R.drawable.ic_pref_brapi_pagination,
                    value = pageSize,
                    keyboardType = KeyboardType.Number,
                    onSave = {
                        pageSize = it.ifBlank { "50" }
                        prefs.edit { putString(BRAPI_PAGE_SIZE_KEY, pageSize) }
                    },
                )
            }
            item {
                TextPreferenceRow(
                    titleRes = R.string.brapi_chunk_size,
                    summary = chunkSize,
                    iconRes = R.drawable.ic_transfer,
                    value = chunkSize,
                    keyboardType = KeyboardType.Number,
                    onSave = {
                        chunkSize = it.ifBlank { "500" }
                        prefs.edit { putString(BRAPI_CHUNK_SIZE_KEY, chunkSize) }
                    },
                )
            }
            item {
                TextPreferenceRow(
                    titleRes = R.string.brapi_timeout,
                    summary = timeout,
                    iconRes = R.drawable.ic_pref_brapi_timeout,
                    value = timeout,
                    keyboardType = KeyboardType.Number,
                    onSave = {
                        timeout = it.ifBlank { "120" }
                        prefs.edit { putString(BRAPI_TIMEOUT_KEY, timeout) }
                    },
                )
            }
            item {
                TextPreferenceRow(
                    titleRes = R.string.brapi_max_concurrent_observation_transfer,
                    summary = concurrentTransfers,
                    iconRes = R.drawable.transfer_down,
                    value = concurrentTransfers,
                    keyboardType = KeyboardType.Number,
                    onSave = {
                        concurrentTransfers = it.ifBlank { "5" }
                        prefs.edit { putString(BRAPI_MAX_OBSERVATION_TRANSFER_KEY, concurrentTransfers) }
                    },
                )
            }
        }
    }

    if (showHttpWarning) {
        AlertDialog(
            onDismissRequest = { showHttpWarning = false },
            title = { Text(stringResource(R.string.act_brapi_auth_http_warning_title)) },
            text = { Text(stringResource(R.string.act_brapi_auth_http_warning_message)) },
            confirmButton = {
                TextButton(
                    onClick = {
                        showHttpWarning = false
                        launchAuth(allowHttp = true)
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

    if (showCommunityServers) {
        CommunityServersDialog(
            onDismiss = { showCommunityServers = false },
            onSelect = { url, name, oidc, flow ->
                baseUrl = url
                oldBaseUrl = url
                displayName = name
                oidcUrl = oidc
                oidcFlow = flow ?: implicitFlow
                prefs.edit {
                    putString(keyUtil.brapiUrl, url)
                    putString(keyUtil.brapiDisplayName, name)
                    putString(keyUtil.brapiOidc, oidc)
                    putString(keyUtil.brapiFlow, oidcFlow)
                }
                showCommunityServers = false
                launchAuth(allowHttp = false)
            },
        )
    }
    } // Box
    } // Scaffold
}

@Composable
private fun CommunityServersDialog(
    onDismiss: () -> Unit,
    onSelect: (url: String, name: String, oidcUrl: String, oidcFlow: String?) -> Unit,
) {
    val context = LocalContext.current
    val names = stringArrayResource(R.array.community_servers_names).toList()
    val urls = stringArrayResource(R.array.community_servers_urls).toList()
    val oidcUrls = stringArrayResource(R.array.community_servers_oidc_urls).toList()
    val grantTypes = stringArrayResource(R.array.community_servers_grant_types).toList()
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(stringResource(R.string.preferences_brapi_servers_title)) },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                names.forEachIndexed { index, name ->
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clickable {
                                onSelect(
                                    urls.getOrElse(index) { "" },
                                    name,
                                    oidcUrls.getOrElse(index) { "" },
                                    grantTypes.getOrNull(index),
                                )
                            }
                            .padding(vertical = 10.dp),
                        verticalAlignment = Alignment.CenterVertically,
                    ) {
                        Text(name, style = MaterialTheme.typography.bodyLarge)
                    }
                }
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clickable {
                            openUri(
                                context,
                                "https://github.com/PhenoApps/Field-Book/issues/new?assignees=&labels=enhancement,feature+request&template=feature_request.md&title=[REQUEST]",
                            )
                            onDismiss()
                        }
                        .padding(vertical = 10.dp),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    Text(stringResource(R.string.preferences_brapi_server_add), style = MaterialTheme.typography.bodyLarge)
                }
            }
        },
        confirmButton = {},
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text(stringResource(R.string.dialog_cancel))
            }
        },
    )
}

// -- BrAPI helper functions --

private fun startBrapiAuth(
    context: Context,
    baseUrl: String,
    oidcUrl: String,
    allowHttp: Boolean,
    onHttpWarning: () -> Unit,
    onInvalidUrl: () -> Unit,
    launch: (Intent) -> Unit,
) {
    val baseIsHttp = baseUrl.startsWith("http://")
    val oidcIsHttp = oidcUrl.startsWith("http://")
    if (!allowHttp && (baseIsHttp || oidcIsHttp)) {
        onHttpWarning()
        return
    }
    val hasValidBaseScheme = baseUrl.startsWith("https://") || (allowHttp && baseIsHttp)
    val hasValidOidcScheme = oidcUrl.startsWith("https://") || (allowHttp && oidcIsHttp)
    if (!hasValidBaseScheme || !hasValidOidcScheme) {
        onInvalidUrl()
        return
    }
    launch(Intent(context, BrapiAuthActivity::class.java))
}

private fun serverDisplayName(url: String, fallback: String): String {
    return url
        .replace("https?://(?:www\\.)?(.*?)(?:/.*)?$".toRegex(), "$1")
        .ifBlank { fallback }
}
