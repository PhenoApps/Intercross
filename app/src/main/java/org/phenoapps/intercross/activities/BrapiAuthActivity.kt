package org.phenoapps.intercross.activities

import android.app.PendingIntent
import android.content.ActivityNotFoundException
import android.content.Context
import android.content.Intent
import android.content.SharedPreferences
import android.net.Uri
import android.os.Bundle
import android.util.Log
import android.view.View
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.appcompat.widget.Toolbar
import androidx.preference.PreferenceManager
import net.openid.appauth.AppAuthConfiguration
import net.openid.appauth.AuthorizationException
import net.openid.appauth.AuthorizationRequest
import net.openid.appauth.AuthorizationResponse
import net.openid.appauth.AuthorizationService
import net.openid.appauth.AuthorizationServiceConfiguration
import net.openid.appauth.ResponseTypeValues
import net.openid.appauth.TokenResponse
import androidx.core.content.edit
import androidx.core.net.toUri
import dagger.hilt.android.AndroidEntryPoint
import org.phenoapps.brapi.account.BrapiTokenStoreResult
import org.phenoapps.intercross.R
import org.phenoapps.intercross.util.BrapiAccountHelper
import org.phenoapps.intercross.util.InsetHandler
import org.phenoapps.intercross.util.KeyUtil
import org.phenoapps.intercross.util.OpenAuthConfigUtil
import javax.inject.Inject

@AndroidEntryPoint
class BrapiAuthActivity : AppCompatActivity() {

    companion object {
        private const val TAG = "BrapiAuthActivity"
        const val EXTRA_SERVER_URL = "brapi_extra_server_url"
        const val EXTRA_OIDC_URL = "brapi_extra_oidc_url"
        const val EXTRA_OIDC_FLOW = "brapi_extra_oidc_flow"
        const val EXTRA_OIDC_CLIENT_ID = "brapi_extra_oidc_client_id"
        const val EXTRA_OIDC_SCOPE = "brapi_extra_oidc_scope"
        const val EXTRA_BRAPI_VERSION = "brapi_extra_brapi_version"
    }

    @Inject
    lateinit var accountHelper: BrapiAccountHelper

    @Inject
    lateinit var keyUtil: KeyUtil

    private lateinit var redirectUri: String
    private var launchServerUrl: String = ""
    private var launchOidcUrl: String = ""
    private var launchOidcFlow: String = ""
    private var launchOidcClientId: String = ""
    private var launchOidcScope: String = ""
    private var launchBrapiVersion: String = ""

    private lateinit var authUtil: OpenAuthConfigUtil
    private var activityStarting = false

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_brapi_auth)

        val toolbar = findViewById<Toolbar>(R.id.toolbar)
        setSupportActionBar(toolbar)
        supportActionBar?.apply {
            title = getString(R.string.brapi_auth_title)
            setDisplayHomeAsUpEnabled(true)
            setHomeButtonEnabled(true)
        }

        val rootView = findViewById<View>(android.R.id.content)
        InsetHandler.setupStandardInsets(rootView, toolbar)

        val prefs = PreferenceManager.getDefaultSharedPreferences(this)
        authUtil = OpenAuthConfigUtil(this, prefs)

        redirectUri = getString(R.string.brapi_redirect_uri)
        if (savedInstanceState != null) {
            launchServerUrl = savedInstanceState.getString(EXTRA_SERVER_URL, "")
            launchOidcUrl = savedInstanceState.getString(EXTRA_OIDC_URL, "")
            launchOidcFlow = savedInstanceState.getString(EXTRA_OIDC_FLOW, "")
            launchOidcClientId = savedInstanceState.getString(EXTRA_OIDC_CLIENT_ID, "")
            launchOidcScope = savedInstanceState.getString(EXTRA_OIDC_SCOPE, "")
            launchBrapiVersion = savedInstanceState.getString(EXTRA_BRAPI_VERSION, "")
        } else {
            launchServerUrl = intent?.getStringExtra(EXTRA_SERVER_URL)
                ?: prefs.getString(keyUtil.brapiUrl, "") ?: ""
            launchOidcUrl = intent?.getStringExtra(EXTRA_OIDC_URL)
                ?: prefs.getString(keyUtil.brapiOidc, "") ?: ""
            launchOidcFlow = intent?.getStringExtra(EXTRA_OIDC_FLOW)
                ?: prefs.getString(keyUtil.brapiFlow, "") ?: ""
            launchOidcClientId = intent?.getStringExtra(EXTRA_OIDC_CLIENT_ID)
                ?: prefs.getString(keyUtil.brapiClient, getString(R.string.brapi_oidc_clientid_default)) ?: ""
            launchOidcScope = intent?.getStringExtra(EXTRA_OIDC_SCOPE)
                ?: prefs.getString(keyUtil.brapiScope, "") ?: ""
            launchBrapiVersion = intent?.getStringExtra(EXTRA_BRAPI_VERSION) ?: ""
        }
        if (launchOidcClientId.isEmpty()) launchOidcClientId = getString(R.string.brapi_oidc_clientid_default)
        if (launchOidcFlow.isEmpty()) launchOidcFlow = getString(R.string.pref_brapi_oidc_flow_implicit)
        activityStarting = true

        // Start auth only when not returning from a deep link or AppAuth result.
        if (!hasAuthResult()) {
            if (isImplicitFlow(launchOidcFlow)) {
                authorizeBrAPIImplicit(prefs)
            } else {
                authorizeBrAPICode(prefs)
            }
        }
    }

    override fun onNewIntent(intent: Intent) {
        super.onNewIntent(intent)
        setIntent(intent)
    }

    override fun onSaveInstanceState(outState: Bundle) {
        super.onSaveInstanceState(outState)
        outState.putString(EXTRA_SERVER_URL, launchServerUrl)
        outState.putString(EXTRA_OIDC_URL, launchOidcUrl)
        outState.putString(EXTRA_OIDC_FLOW, launchOidcFlow)
        outState.putString(EXTRA_OIDC_CLIENT_ID, launchOidcClientId)
        outState.putString(EXTRA_OIDC_SCOPE, launchOidcScope)
        outState.putString(EXTRA_BRAPI_VERSION, launchBrapiVersion)
    }

    override fun onResume() {
        super.onResume()

        if (activityStarting) {
            activityStarting = false
            handleAuthResultIfPresent()
            return
        }

        if (!handleAuthResultIfPresent()) {
            intent?.data = null
            finish()
        }
    }

    private fun authorizeBrAPIImplicit(prefs: SharedPreferences) {
        prefs.edit { putString(keyUtil.brapiToken, null) }

        val clientId = launchOidcClientId.ifEmpty { getString(R.string.brapi_oidc_clientid_default) }
        val scope = launchOidcScope
        val implicitRedirectUri = getString(R.string.brapi_implicit_redirect_uri).toUri()

        try {
            authUtil.getAuthServiceConfiguration({ config, err ->
                if (err != null || config == null) {
                    Log.e(TAG, "Failed to fetch OIDC config", err)
                    authError(err ?: Exception("No config"))
                    return@getAuthServiceConfiguration
                }
                try {
                    requestAuthorization(config, clientId, ResponseTypeValues.TOKEN, implicitRedirectUri, scope)
                } catch (e: Exception) {
                    authError(e)
                }
            }, launchOidcUrl)
        } catch (e: Exception) {
            authError(e)
        }
    }

    private fun authorizeBrAPICode(prefs: SharedPreferences) {
        prefs.edit { putString(keyUtil.brapiToken, null) }

        val clientId = launchOidcClientId.ifEmpty { getString(R.string.brapi_oidc_clientid_default) }
        val scope = launchOidcScope
        val codeRedirectUri = redirectUri.toUri()

        try {
            authUtil.getAuthServiceConfiguration({ config, err ->
                if (err != null || config == null) {
                    Log.e(TAG, "Failed to fetch OIDC config", err)
                    authError(err ?: Exception("No config"))
                    return@getAuthServiceConfiguration
                }
                try {
                    requestAuthorization(config, clientId, ResponseTypeValues.CODE, codeRedirectUri, scope)
                } catch (e: Exception) {
                    authError(e)
                }
            }, launchOidcUrl)
        } catch (e: Exception) {
            authError(e)
        }
    }

    private fun requestAuthorization(
        serviceConfig: AuthorizationServiceConfiguration,
        clientId: String,
        responseType: String,
        redirectUri: Uri,
        scope: String,
    ) {
        val authRequestBuilder = AuthorizationRequest.Builder(
            serviceConfig,
            clientId,
            responseType,
            redirectUri,
        )

        if (scope.trim().isNotEmpty()) {
            authRequestBuilder.setScope("$scope openid")
        } else {
            authRequestBuilder.setScopes("openid")
        }

        val authRequest = authRequestBuilder.setPrompt("login").build()
        val authService = getAuthorizationService()

        val responseIntent = Intent(this, BrapiAuthActivity::class.java).apply {
            addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP)
            putExtra(EXTRA_SERVER_URL, launchServerUrl)
            putExtra(EXTRA_OIDC_URL, launchOidcUrl)
            putExtra(EXTRA_OIDC_FLOW, launchOidcFlow)
            putExtra(EXTRA_OIDC_CLIENT_ID, launchOidcClientId)
            putExtra(EXTRA_OIDC_SCOPE, launchOidcScope)
            putExtra(EXTRA_BRAPI_VERSION, launchBrapiVersion)
        }

        authService.performAuthorizationRequest(
            authRequest,
            PendingIntent.getActivity(
                this,
                0,
                responseIntent,
                PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_MUTABLE,
            ),
        )
    }

    /**
     * Best available human-readable cause for an auth failure, or empty when nothing was reported.
     *
     * AppAuth puts the provider's own wording in errorDescription and the OAuth error code in
     * error; both are absent for transport-level failures, where the exception message is all
     * there is.
     */
    private fun describeAuthFailure(ex: Exception?): String {
        if (ex == null) return ""

        if (ex is AuthorizationException) {
            val errorDesc: String? = ex.errorDescription
            if (!errorDesc.isNullOrEmpty()) {
                return errorDesc
            }
            val errorCode: String? = ex.error
            if (!errorCode.isNullOrEmpty()) {
                return errorCode
            }
            val cause = ex.cause
            if (cause != null && cause.message != null) {
                return cause.message!!
            }
        }

        return ex.message ?: ""
    }

    private fun authError(ex: Exception?) {
        // Clear our data from our deep link so the app doesn't think it is
        // coming from a deep link if it is coming from deep link on pause and resume.

        intent.data = null

        Log.e("BrAPI", "Error starting BrAPI auth", ex)

        val reason = describeAuthFailure(ex)
        val message = if (reason.isEmpty()) {
            getString(R.string.brapi_auth_error_starting)
        } else {
            getString(R.string.brapi_auth_error_starting_reason, reason)
        }

        Toast.makeText(this, message, Toast.LENGTH_LONG).show()
        setResult(RESULT_CANCELED)
        finish()
    }

    private fun authSuccess(accessToken: String?, idToken: String?) {
        if (accessToken == null) {
            authError(null)
            return
        }
        val serverUrl = launchServerUrl.ifEmpty { getString(R.string.brapi_base_url_default) }
        val stored = accountHelper.storeToken(serverUrl, accessToken, idToken)

        // Clear our data from our deep link so the app doesn't think it is
        // coming from a deep link if it is coming from deep link on pause and resume.
        intent.data = null

        // The provider signed us in either way, but only STORED means the server now has an
        // account of its own here. Saying "authorization successful" for the other outcomes would
        // promise a server card that never appears.
        val message = when (stored) {
            BrapiTokenStoreResult.STORED -> R.string.brapi_auth_success
            BrapiTokenStoreResult.ALREADY_SHARED -> R.string.brapi_auth_success_already_shared
            BrapiTokenStoreResult.ACCOUNT_UNAVAILABLE -> R.string.brapi_auth_success_no_account
        }

        Log.d("BrAPI", "Auth successful, token stored: $stored")
        Toast.makeText(this, message, Toast.LENGTH_LONG).show()
        setResult(RESULT_OK)
        finish()
    }

    private fun getAuthorizationService(): AuthorizationService {
        val builder = AppAuthConfiguration.Builder()
        builder.setConnectionBuilder(authUtil.getConnectionBuilder())
        return AuthorizationService(this, builder.build())
    }

    private fun handleAuthResultIfPresent(): Boolean {
        val ex = AuthorizationException.fromIntent(intent)
        val response = AuthorizationResponse.fromIntent(intent)
        val data = intent?.data

        return when {
            ex != null -> {
                authError(ex)
                true
            }
            response != null || data != null -> {
                checkBrapiAuth(data)
                true
            }
            else -> false
        }
    }

    private fun hasAuthResult(): Boolean =
        intent?.data != null ||
                AuthorizationException.fromIntent(intent) != null ||
                AuthorizationResponse.fromIntent(intent) != null

    fun checkBrapiAuth(data: Uri?) {
        val authService = getAuthorizationService()
        val ex = AuthorizationException.fromIntent(intent)
        val response = AuthorizationResponse.fromIntent(intent)

        if (ex != null) {
            authError(ex)
            return
        }

        if (response?.authorizationCode != null) {
            authService.performTokenRequest(response.createTokenExchangeRequest()) { tokenResponse: TokenResponse?, tokenEx: AuthorizationException? ->
                if (tokenResponse?.accessToken != null) {
                    authSuccess(tokenResponse.accessToken!!, tokenResponse.idToken)
                } else {
                    authError(tokenEx)
                }
            }
            return
        }

        if (response?.accessToken != null) {
            authSuccess(response.accessToken!!, null)
            return
        }

        // Fallback: parse access_token from fragment
        if (data == null) {
            authError(null)
            return
        }
        val modifiedData = data.toString().replaceFirst("#", "?").toUri()
        var token = modifiedData.getQueryParameter("access_token")
        if (token == null) {
            authError(null)
            return
        }
        if (token.startsWith("Bearer ")) {
            token = token.removePrefix("Bearer ")
        }
        authSuccess(token, null)
    }

    private fun isImplicitFlow(flow: String): Boolean =
        flow == getString(R.string.pref_brapi_oidc_flow_implicit)
                || flow.contains("implicit", ignoreCase = true)
}
