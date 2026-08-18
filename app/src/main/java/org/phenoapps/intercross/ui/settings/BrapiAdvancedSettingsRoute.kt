package org.phenoapps.intercross.ui.settings

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringArrayResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.core.content.edit
import org.phenoapps.intercross.R
import org.phenoapps.intercross.ui.app.TopBarState
import org.phenoapps.intercross.ui.app.rememberPrefs
import org.phenoapps.intercross.ui.brapi.BrapiScaffold

private const val BRAPI_VERSION_KEY = "BRAPI_VERSION"
private const val BRAPI_PAGE_SIZE_KEY = "BRAPI_PAGE_SIZE"
private const val BRAPI_CHUNK_SIZE_KEY = "BRAPI_CHUNK_SIZE"
private const val BRAPI_TIMEOUT_KEY = "BRAPI_TIMEOUT"
private const val BRAPI_MAX_OBSERVATION_TRANSFER_KEY = "BRAPI_MAX_CONCURRENT_OBSERVATION_TRANSFER"

@Composable
fun BrapiAdvancedSettingsRoute(
    onBack: () -> Unit = {},
) {
    val context = LocalContext.current
    val (prefs, keyUtil) = rememberPrefs()

    val brapiOidcUrlDefault = stringResource(R.string.brapi_oidc_url_default)
    val brapiOidcClientIdDefault = stringResource(R.string.brapi_oidc_clientid_default)
    val brapiOidcScopeDefault = stringResource(R.string.brapi_oidc_scope_default)
    val brapiVersionV2 = stringResource(R.string.preferences_brapi_version_v2)
    val implicitFlow = stringResource(R.string.preferences_brapi_oidc_flow_oauth_implicit)
    val oldCustomFlow = stringResource(R.string.preferences_brapi_oidc_flow_old_custom)

    // Authorization (OIDC) settings
    var oidcUrl by remember {
        mutableStateOf(prefs.getString(keyUtil.brapiOidc, brapiOidcUrlDefault).orEmpty())
    }
    var oidcFlow by remember {
        mutableStateOf(prefs.getString(keyUtil.brapiFlow, implicitFlow).orEmpty())
    }
    var clientId by remember {
        mutableStateOf(prefs.getString(keyUtil.brapiClient, brapiOidcClientIdDefault).orEmpty())
    }
    var scope by remember {
        mutableStateOf(prefs.getString(keyUtil.brapiScope, brapiOidcScopeDefault).orEmpty())
    }

    // Advanced settings
    var brapiVersion by remember {
        mutableStateOf(prefs.getString(BRAPI_VERSION_KEY, brapiVersionV2).orEmpty())
    }
    var pageSize by remember { mutableStateOf(prefs.getString(BRAPI_PAGE_SIZE_KEY, "50").orEmpty()) }
    var chunkSize by remember { mutableStateOf(prefs.getString(BRAPI_CHUNK_SIZE_KEY, "500").orEmpty()) }
    var timeout by remember { mutableStateOf(prefs.getString(BRAPI_TIMEOUT_KEY, "120").orEmpty()) }
    var concurrentTransfers by remember {
        mutableStateOf(prefs.getString(BRAPI_MAX_OBSERVATION_TRANSFER_KEY, "5").orEmpty())
    }

    LaunchedEffect(implicitFlow, oldCustomFlow) {
        if (oidcFlow == oldCustomFlow) {
            oidcFlow = implicitFlow
            prefs.edit { putString(keyUtil.brapiFlow, implicitFlow) }
        }
    }

    val topBarState = TopBarState(
        titleRes = R.string.brapi_advanced_settings_title,
        showBack = true,
        onBack = onBack,
    )

    BrapiScaffold(topBarState = topBarState) { innerPadding ->
        Box(Modifier.padding(innerPadding)) {
            LazyColumn(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(16.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp),
            ) {
                // Authorization section (global OIDC settings)
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

                // Advanced settings section
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
    }
}
