package org.phenoapps.intercross.activities

import android.app.PendingIntent
import android.content.ActivityNotFoundException
import android.content.Context
import android.content.Intent
import android.content.SharedPreferences
import android.net.Uri
import android.os.Bundle
import android.util.Log
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import dagger.hilt.android.AndroidEntryPoint
import net.openid.appauth.AppAuthConfiguration
import net.openid.appauth.AuthorizationException
import net.openid.appauth.AuthorizationRequest
import net.openid.appauth.AuthorizationResponse
import net.openid.appauth.AuthorizationService
import net.openid.appauth.AuthorizationServiceConfiguration
import net.openid.appauth.ResponseTypeValues
import org.phenoapps.intercross.R
import org.phenoapps.intercross.util.KeyUtil
import org.phenoapps.intercross.util.OpenAuthConfigurationUtil
import javax.inject.Inject
import androidx.core.content.edit
import androidx.core.net.toUri

@AndroidEntryPoint
class BrapiAuthActivity : AppCompatActivity() {

    @Inject
    lateinit var keyUtil: KeyUtil

    @Inject
    lateinit var preferences: SharedPreferences

    @Inject
    lateinit var authUtil: OpenAuthConfigurationUtil

    private var activityStarting = false

    public override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        setContentView(R.layout.activity_brapi_auth)

//        val toolbar: Toolbar? = findViewById(R.id.toolbar)
//        setSupportActionBar(toolbar)
//        if (getSupportActionBar() != null) {
//            getSupportActionBar().setTitle(null)
//            getSupportActionBar().setDisplayHomeAsUpEnabled(true)
//            getSupportActionBar().setHomeButtonEnabled(true)
//        }

//        val rootView: View? = findViewById(R.id.content)
//        InsetHandler.INSTANCE.setupStandardInsets(rootView, toolbar)

        activityStarting = true

        // Start our login process
        //when coming back from deep link this check keeps app from auto-re-authenticating
        if (intent != null && intent.data == null) {
            val flow: String = preferences!!.getString(keyUtil.brapiOidc, "")!!
            if (flow == getString(R.string.preferences_brapi_oidc_flow_old_custom)) {
                authorizeBrAPI_OLD(preferences!!, this)
            } else {
                authorizeBrAPI(preferences!!, this)
            }
        }

        //getOnBackPressedDispatcher().addCallback(this, standardBackCallback())
    }

    protected override fun onNewIntent(intent: Intent) {
        super.onNewIntent(intent)
        setIntent(intent)
        //getIntent() should always return the last received intent
    }

    public override fun onResume() {
        super.onResume()

        if (activityStarting) {
            // If the activity has just started, ignore the onResume code
            activityStarting = false
        } else {
            val ex = AuthorizationException.fromIntent(intent)
            val data: Uri? = intent.data

            if (data != null) {
                // authorization completed
                val flow: String = preferences.getString(keyUtil.brapiOidc, "")!!
                if (flow == getString(R.string.preferences_brapi_oidc_flow_old_custom)) {
                    checkBrapiAuth_OLD(data)
                } else {
                    checkBrapiAuth(data)
                }
            } else if (ex != null) {
                // authorization completed in error

                authError(ex)
            } else { //returning from deep link with null data should finish activity
                //otherwise the progress bar hangs

                intent.data = null

                finish()
            }
        }
    }

    fun authorizeBrAPI(sharedPreferences: SharedPreferences, context: Context?) {

        sharedPreferences.edit {
            putString(keyUtil.brapiToken, null)
        }

        val flow: String = sharedPreferences.getString(keyUtil.brapiFlow, "")!!
        val responseType =
            if (flow == getString(R.string.preferences_brapi_oidc_flow_oauth_implicit)) ResponseTypeValues.TOKEN else ResponseTypeValues.CODE

        try {
            val clientId: String =
                sharedPreferences.getString(keyUtil.brapiClient, "fieldbook")!!
            val scope: String = sharedPreferences.getString(keyUtil.brapiScope, "")!!

            // Authorization code flow works better with custom URL scheme fieldbook://app/auth
            // https://github.com/openid/AppAuth-Android/issues?q=is%3Aissue+intent+null
            val redirectURI =
                if (flow == getString(R.string.preferences_brapi_oidc_flow_oauth_implicit))
                    "https://phenoapps.org/field-book".toUri()
                else "fieldbook://app/auth".toUri()

            authUtil.getAuthServiceConfiguration({ authorizationServiceConfiguration, ex ->
                if (ex != null) {
                    Log.e("BrAPIService", "failed to fetch configuration", ex)
                    authError(ex)
                    finish()
                }
                try {
                    authorizationServiceConfiguration?.let { config ->
                        requestAuthorization(
                            config,
                            clientId,
                            responseType,
                            redirectURI,
                            scope,
                            context
                        )
                    }
                } catch (e: IllegalArgumentException) {
                    e.printStackTrace()

//                    Toast.makeText(
//                        context,
//                        R.string.oauth_configured_incorrectly,
//                        Toast.LENGTH_LONG
//                    ).show()

                    finish()
                }
            })
        } catch (ex: Exception) {
            authError(ex)
        }
    }

    private fun requestAuthorization(
        serviceConfig: AuthorizationServiceConfiguration,
        clientId: String,
        responseType: String,
        redirectURI: Uri,
        scope: String,
        context: Context?
    ) {
        val authRequestBuilder =
            AuthorizationRequest.Builder(
                serviceConfig,  // the authorization service configuration
                clientId,  // the client ID, typically pre-registered and static
                responseType,  // the response_type value: token or code
                redirectURI
            ) // the redirect URI to which the auth response is sent

        if (!scope.trim { it <= ' ' }.isEmpty()) {
            authRequestBuilder.setScope("$scope openid")
        } else {
            authRequestBuilder.setScopes("openid")
        }

        val authRequest = authRequestBuilder.setPrompt("login").build()

        val authService = this.authorizationService

        val responseIntent = Intent(context, BrapiAuthActivity::class.java)
        responseIntent.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP)

        authService.performAuthorizationRequest(
            authRequest,
            PendingIntent.getActivity(context, 0, responseIntent, PendingIntent.FLAG_MUTABLE)
        )
    }

    fun authorizeBrAPI_OLD(sharedPreferences: SharedPreferences, context: Context) {

        sharedPreferences.edit {
            putString(keyUtil.brapiToken, null)
        }

        try {
            val url = sharedPreferences.getString(
                keyUtil.brapiUrl,
                ""
            ) + "/brapi/authorize?display_name=Field Book&return_url=fieldbook://"
            try {
                // Go to url with the default browser
                val uri = url.toUri()
                val i = Intent(Intent.ACTION_VIEW, uri)
                i.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                i.addFlags(Intent.FLAG_ACTIVITY_NO_HISTORY)
                context.startActivity(i)
            } catch (ex: ActivityNotFoundException) {
                Log.e("BrAPI", "Error starting BrAPI auth", ex)
                authError(ex)
            }
        } catch (ex: Exception) {
            Log.e("BrAPI", "Error starting BrAPI auth", ex)
            authError(ex)
        }
    }

    private fun authError(ex: Exception?) {
        // Clear our data from our deep link so the app doesn't think it is
        // coming from a deep link if it is coming from deep link on pause and resume.

        intent.data = null

        Log.e("BrAPI", "Error starting BrAPI auth", ex)
        Toast.makeText(this, R.string.brapi_auth_error_starting, Toast.LENGTH_LONG).show()
        setResult(RESULT_CANCELED)
        finish()
    }

    private fun authSuccess(accessToken: String?, idToken: String?) {
        val editor = preferences!!.edit()
        editor.putString(keyUtil.brapiToken, accessToken)
        editor.putString(keyUtil.brapiId, idToken).apply()
        editor.apply()

        // Clear our data from our deep link so the app doesn't think it is
        // coming from a deep link if it is coming from deep link on pause and resume.
        intent.data = null

        Log.d("BrAPI", "Auth successful")
        Toast.makeText(this, R.string.brapi_auth_success, Toast.LENGTH_LONG).show()
        setResult(RESULT_OK)
        finish()
    }

    fun checkBrapiAuth_OLD(data: Uri) {
        val status = data.getQueryParameter("status")!!.toInt()

        // Check that we actually have the data. If not return failure.
        if (status == null) {
            authError(null)
            return
        }

        if (status == 200) {
            val token = data.getQueryParameter("token")

            // Check that we received a token.
            if (token == null) {
                authError(null)
                return
            }
            authSuccess(token, null)
        } else {
            authError(null)
        }
    }

    private val authorizationService: AuthorizationService
        /**
         * Create an instance of AuthorizationService with custom connection builder.
         * @return Configured auth service
         */
        get() {
            val builder = AppAuthConfiguration.Builder()
            builder.setConnectionBuilder(authUtil!!.getConnectionBuilder())
            return AuthorizationService(this, builder.build())
        }

    fun checkBrapiAuth(data: Uri) {
        var data = data
        val authService = this.authorizationService
        val ex = AuthorizationException.fromIntent(intent)
        val response = AuthorizationResponse.fromIntent(intent)

        if (ex != null) {
            authError(ex)
            return
        }

        if (response != null && response.authorizationCode != null) {
            authService.performTokenRequest(
                response.createTokenExchangeRequest()
            ) { response, ex ->
                if (response != null && response.accessToken != null) {
                    authSuccess(response.accessToken, response.idToken)
                } else {
                    authError(null)
                }
            }
            return
        }

        if (response != null && response.accessToken != null) {
            authSuccess(response.accessToken, null)
            return
        }

        // Original check for access_token
        data = data.toString().replaceFirst("#".toRegex(), "?").toUri()
        var token = data.getQueryParameter("access_token")
        // Check that we received a token.
        if (token == null) {
            authError(null)
            return
        }

        if (token.startsWith("Bearer ")) {
            token = token.replaceFirst("Bearer ".toRegex(), "")
        }

        authSuccess(token, null)
    }

    companion object {
        //first number that came to Pete's head --IRRI hackathon '25
        var END_SESSION_REQUEST_CODE: Int = 456

        var REDIRECT_URI: String = "fieldbook://app/auth"
    }
}