package org.phenoapps.intercross.util

import android.content.Context
import android.content.SharedPreferences
import android.net.Uri
import android.util.Log
import androidx.core.net.toUri
import net.openid.appauth.AuthorizationServiceConfiguration
import net.openid.appauth.connectivity.ConnectionBuilder
import java.net.HttpURLConnection
import java.net.URL

class OpenAuthConfigUtil(
    private val context: Context,
    private val preferences: SharedPreferences,
) {
    companion object {
        private const val HTTP = "http"
        private const val HTTPS = "https"
    }

    private val keyUtil = KeyUtil(context)

    fun getConnectionBuilder(): ConnectionBuilder {
        return ConnectionBuilder { uri: Uri ->
            val scheme = uri.scheme ?: ""
            require(scheme == HTTP || scheme == HTTPS) { "scheme of uri must be http or https" }

            var conn = URL(uri.toString()).openConnection() as HttpURLConnection
            conn.instanceFollowRedirects = true

            val status = conn.responseCode
            if (status == HttpURLConnection.HTTP_MOVED_TEMP
                || status == HttpURLConnection.HTTP_MOVED_PERM
                || status == HttpURLConnection.HTTP_SEE_OTHER
            ) {
                val newUrl = conn.getHeaderField("Location")
                val cookies = conn.getHeaderField("Set-Cookie")
                conn.disconnect()
                conn = URL(newUrl).openConnection() as HttpURLConnection
                conn.setRequestProperty("Cookie", cookies)
            } else {
                conn = URL(uri.toString()).openConnection() as HttpURLConnection
            }
            conn
        }
    }

    fun getAuthServiceConfiguration(
        onRetrieveConfiguration: (AuthorizationServiceConfiguration?, Exception?) -> Unit,
        oidcUrl: String? = null,
    ) {
        try {
            val rawOidcUrl = oidcUrl?.takeIf { it.isNotEmpty() }
                ?: (preferences.getString(keyUtil.brapiOidc, "") ?: "")
            val oidcConfigUri = rawOidcUrl.let { url ->
                if (url.startsWith(HTTP) || url.startsWith(HTTPS)) url else "$HTTPS://$url"
            }.toUri()

            val scheme = oidcConfigUri.scheme ?: ""
            if (scheme != HTTP && scheme != HTTPS) {
                onRetrieveConfiguration(
                    null,
                    IllegalArgumentException("OIDC URL must use http or https scheme, got: '${oidcConfigUri}'")
                )
                return
            }

            val builder = getConnectionBuilder()

            AuthorizationServiceConfiguration.fetchFromUrl(
                oidcConfigUri,
                { serviceConfig, ex -> onRetrieveConfiguration(serviceConfig, ex) },
                builder,
            )
        } catch (ex: Exception) {
            Log.e("OpenAuthConfigUtil", "Failed to fetch OIDC config", ex)
            onRetrieveConfiguration(null, ex)
        }
    }
}
