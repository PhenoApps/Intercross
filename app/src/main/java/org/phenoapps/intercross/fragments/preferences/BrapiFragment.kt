package org.phenoapps.intercross.fragments.preferences

import android.app.Activity
import android.app.AlertDialog
import android.app.Dialog
import android.content.Context
import android.content.DialogInterface
import android.content.Intent
import android.content.SharedPreferences
import android.os.Bundle
import android.util.Log
import android.view.Menu
import android.view.MenuInflater
import android.view.MenuItem
import android.widget.Toast
import androidx.core.content.edit
import androidx.core.net.toUri
import androidx.fragment.app.DialogFragment
import androidx.preference.CheckBoxPreference
import androidx.preference.EditTextPreference
import androidx.preference.ListPreference
import androidx.preference.Preference
import androidx.preference.PreferenceCategory
import androidx.preference.PreferenceFragmentCompat
import dagger.hilt.android.AndroidEntryPoint
import net.openid.appauth.AuthorizationService
import net.openid.appauth.EndSessionRequest
import org.phenoapps.intercross.R
import org.phenoapps.intercross.activities.BrapiAuthActivity
import org.phenoapps.intercross.util.KeyUtil
import org.phenoapps.intercross.util.OpenAuthConfigurationUtil
import org.phenoapps.sharedpreferences.dialogs.NeutralButtonEditTextDialog
import org.phenoapps.sharedpreferences.dialogs.NeutralButtonEditTextDialogFragmentCompat.Companion.newInstance
import javax.inject.Inject
import kotlin.jvm.java

/**
 * This preference fragment handles all BrAPI related shared preferences.
 *
 *
 * If using oidc flow, changing the base url will change the oidc url to match. If the user
 * explicitly changes the oidc url, then the automatic change will not occur anymore.
 *
 *
 * This will call the BrAPI Authentication activity to handle authentication s.a OIDC or basic.
 * Auth token is saved in the preferences, or set to null when logging out.
 */
@AndroidEntryPoint
class BrapiFragment : PreferenceFragmentCompat(), Preference.OnPreferenceChangeListener {

    @Inject
    lateinit var keyUtil: KeyUtil

    @Inject
    lateinit var preferences: SharedPreferences

    @Inject
    lateinit var authUtil: OpenAuthConfigurationUtil

    private var context: Context? = null
    private var brapiServerPrefCategory: PreferenceCategory? = null
    private var brapiLogoutButton: Preference? = null
    private var mMenu: Menu? = null
    private var brapiURLPreference: EditTextPreference? = null
    private var brapiDisplayName: EditTextPreference? = null
    private var brapiOIDCURLPreference: EditTextPreference? = null
    private var brapiClientIdPreference: EditTextPreference? = null

    private var brapiOIDCFlow: ListPreference? = null

    //old base url must be in memory now, since NeutralEditText preference updates preferences
    private var oldBaseUrl: String? = ""

    //alert dialog displays messages when oidc or brapi urls have http
    private var mBrapiHttpWarningDialog: AlertDialog? = null

    private var brapiServerInfoButton: Preference? = null

    override fun onAttach(context: Context) {
        super.onAttach(context)
        // Occurs before onCreate function. We get the context this way.
        this@BrapiFragment.context = context
    }

    override fun onCreatePreferences(savedInstanceState: Bundle?, rootKey: String?) {
        mBrapiHttpWarningDialog = AlertDialog.Builder(context)
            .setTitle(R.string.act_brapi_auth_http_warning_title)
            .setMessage(R.string.act_brapi_auth_http_warning_message)
            .setPositiveButton(
                R.string.ok
            ) { dialog: DialogInterface?, _: Int ->
                startAuth()
                dialog!!.dismiss()
            }.create()

        //remove old custom fb auth if it is being used
        if (preferences?.getString(
                keyUtil.brapiFlow,
                getString(R.string.preferences_brapi_oidc_flow_oauth_implicit)
            )
            == getString(R.string.preferences_brapi_oidc_flow_old_custom)
        ) {
            preferences.edit {
                putString(
                    keyUtil.brapiFlow,
                    getString(R.string.preferences_brapi_oidc_flow_oauth_implicit)
                )
            }
        }

        setPreferencesFromResource(R.xml.preferences_brapi, rootKey)

        // Show/hide preferences and category titles based on the BRAPI_ENABLED value
        val brapiEnabledPref = findPreference<CheckBoxPreference?>(keyUtil.brapiEnabled ?: "")
        if (brapiEnabledPref != null) {
            brapiEnabledPref.onPreferenceChangeListener =
                Preference.OnPreferenceChangeListener { _, newValue ->
                    val isChecked = newValue as Boolean
                    if (!isChecked) { // on disable, reset default sources if they were set to brapi
                        // remove brapi auth token when brapi is disabled
                        preferences?.edit { remove(keyUtil.brapiToken) }
                    }
                    updatePreferencesVisibility(isChecked)
                    true
                }
            updatePreferencesVisibility(brapiEnabledPref.isChecked)
        }

        setupToolbar()
        setHasOptionsMenu(true)

        brapiServerPrefCategory = findPreference("brapi_server")
        brapiLogoutButton = findPreference("revokeBrapiAuth")

        brapiClientIdPreference = findPreference(keyUtil.brapiClient)
        brapiURLPreference = findPreference(keyUtil.brapiUrl)
        brapiDisplayName = findPreference(keyUtil.brapiDisplayName)
        
        if (brapiURLPreference != null) {
            brapiURLPreference!!.onPreferenceChangeListener = this
        }
        if (brapiDisplayName != null) {
            brapiDisplayName!!.onPreferenceChangeListener = this
        }

        brapiOIDCURLPreference = findPreference(keyUtil.brapiOidc)
        brapiOIDCFlow = findPreference(keyUtil.brapiFlow)
        
        if (brapiOIDCFlow != null) {
            brapiOIDCFlow!!.onPreferenceChangeListener = this
        }

        //set saved urls, default to the test server
        val url = preferences!!.getString(
            keyUtil.brapiUrl,
            getString(R.string.brapi_base_url_default)
        )
        val displayName = preferences!!.getString(
            keyUtil.brapiDisplayName,
            getString(R.string.brapi_edit_display_name_default)
        )
        val oidcUrl = preferences!!.getString(
            keyUtil.brapiOidc,
            getString(R.string.brapi_oidc_url_default)
        )
        oldBaseUrl = url
        brapiURLPreference!!.setText(url!!)
        brapiDisplayName!!.setText(displayName!!)
        brapiOIDCURLPreference!!.setText(oidcUrl!!)

        //set logout button
        if (brapiLogoutButton != null) {
            brapiLogoutButton!!.setOnPreferenceClickListener { _: Preference? ->
                val idToken = preferences!!.getString(keyUtil.brapiId, null)
                if (idToken != null) {
                    Toast.makeText(context, R.string.logging_out_please_wait, Toast.LENGTH_SHORT)
                        .show()

                    authUtil!!.getAuthServiceConfiguration({ config, ex ->
                        config?.let {
                            val endSessionRequest =
                                EndSessionRequest.Builder(config)
                                    .setIdTokenHint(idToken)
                                    .setPostLogoutRedirectUri(BrapiAuthActivity.REDIRECT_URI.toUri())
                                    .build()
                            val authService = AuthorizationService(requireContext())
                            val endSessionIntent =
                                authService.getEndSessionRequestIntent(endSessionRequest)
                            startActivityForResult(
                                endSessionIntent,
                                BrapiAuthActivity.END_SESSION_REQUEST_CODE
                            )
                        }
                    })
                } else {
                    preferences!!.edit { remove(keyUtil.brapiToken) }

                    setButtonView()
                }
                true
            }
        }
        
        setOidcFlowUi()

//        brapiServerInfoButton = findPreference("brapi_server_info")
//        if (brapiServerInfoButton != null) {
//            brapiServerInfoButton!!.setOnPreferenceClickListener { _: Preference? ->
//                checkServerInfo()
//                true
//            }
//        }
    }

    private fun updatePreferencesVisibility(isChecked: Boolean) {
        val preferenceScreen = getPreferenceScreen()
        for (i in 0..<preferenceScreen.preferenceCount) {
            val preferenceItem = preferenceScreen.getPreference(i)
            if (preferenceItem.key == keyUtil.brapiEnabled) { // Skip the checkbox preference itself
                continue
            }
            preferenceItem.isVisible = isChecked
        }

        // Also show/hide the BrAPI toolbar authentication option
        if (mMenu != null) {
            val brapiAutoConfigureItem = mMenu!!.findItem(R.id.action_menu_brapi_auto_configure)
            if (brapiAutoConfigureItem != null) {
                brapiAutoConfigureItem.isVisible = isChecked
            }
            val brapiPrefAuthItem = mMenu!!.findItem(R.id.action_menu_brapi_pref_auth)
            if (brapiPrefAuthItem != null) {
                brapiPrefAuthItem.isVisible = isChecked
            }
        }
    }
    
    @Deprecated("Deprecated in Java")
    override fun onCreateOptionsMenu(menu: Menu, inflater: MenuInflater) {
        menu.clear()
        inflater.inflate(R.menu.menu_brapi_pref, menu)
        mMenu = menu // Store a reference to the menu
        val brapiEnabledPref = findPreference<CheckBoxPreference?>(keyUtil.brapiEnabled)
        updatePreferencesVisibility(brapiEnabledPref!!.isChecked)
        super.onCreateOptionsMenu(menu, inflater)
    }

    override fun onPrepareOptionsMenu(menu: Menu) {
        super.onPrepareOptionsMenu(menu)

        // Ensure the menu has been fully inflated before longpress listener setup
        requireView().post { setupLongPressListener() }
    }

    private fun setupLongPressListener() {
//        val menuItemView =
//            requireActivity().findViewById<View?>(R.id.action_menu_brapi_auto_configure)
//        if (menuItemView != null) {
//            menuItemView.setOnLongClickListener { v: View? ->
//                showCommunityServerListDialog()
//                true
//            }
//        }
    }

    private fun showCommunityServerListDialog() {
        val builder = AlertDialog.Builder(context)
        builder.setTitle(R.string.preferences_brapi_servers_title)

        val serverNames = resources.getStringArray(R.array.community_servers_names)
        val serverUrls = resources.getStringArray(R.array.community_servers_urls)
        val serverOidcUrls = resources.getStringArray(R.array.community_servers_oidc_urls)
        val serverGrantTypes = resources.getStringArray(R.array.community_servers_grant_types)

        // Add "Submit a server" option
        val extendedServerNames = arrayOfNulls<String>(serverNames.size + 1)
        System.arraycopy(serverNames, 0, extendedServerNames, 0, serverNames.size)
        extendedServerNames[serverNames.size] = getString(R.string.preferences_brapi_server_add)

        builder.setItems(
            extendedServerNames
        ) { dialog: DialogInterface?, which: Int ->
            if (which == serverNames.size) {
                // Handle the "Submit a server" option
                val browserIntent = Intent(
                    Intent.ACTION_VIEW,
                    "https://github.com/PhenoApps/Field-Book/issues/new?assignees=&labels=enhancement,feature+request&template=feature_request.md&title=[REQUEST]".toUri()
                )
                startActivity(browserIntent)
            } else {
                val selectedServerUrl = serverUrls[which]
                val selectedServerName = serverNames[which]
                val selectedOidcUrl = serverOidcUrls[which]
                val selectedGrantType: String? = serverGrantTypes[which]
                setServer(
                    selectedServerUrl,
                    selectedServerName,
                    selectedOidcUrl,
                    selectedGrantType
                )
            }
        }

        builder.setNegativeButton(R.string.cancel, null)
        builder.create().show()
    }


    @Deprecated("Deprecated in Java")
    override fun onOptionsItemSelected(item: MenuItem): Boolean {
//        if (item.itemId == R.id.action_menu_brapi_pref_auth) {
//            brapiAuth()
//            return true
//        }
        return super.onOptionsItemSelected(item)
    }

    override fun onResume() {
        super.onResume()
        setBaseURLSummary()
        setButtonView()
        setupToolbar()
    }

    override fun onPause() {
        super.onPause()
        if (mBrapiHttpWarningDialog!!.isShowing) mBrapiHttpWarningDialog!!.cancel()
    }

    override fun onPreferenceChange(preference: Preference, newValue: Any): Boolean {
        if (preference == brapiURLPreference) {
            updateUrls(newValue.toString())
        }

        if (preference == brapiDisplayName) {
            brapiDisplayName!!.setSummary(newValue.toString())
        }

        if (preference == brapiOIDCFlow) {
            setOidcFlowUi()
        }

        return true
    }

    /**
     * This is required to use a dialog as a preference, used to add a barcode scan button to the
     * url preferences utilizing the NeutralButtonEditTextDialog
     * @param preference this is the base url or the oidc url preferences
     */
    override fun onDisplayPreferenceDialog(preference: Preference) {
        // check if dialog is already showing
        if (fragmentManager != null && requireFragmentManager().findFragmentByTag(
                DIALOG_FRAGMENT_TAG
            ) != null
        ) {
            return
        }

        val f: DialogFragment?

        if (preference is NeutralButtonEditTextDialog) {
            /*
             * Takes three callbacks, on neutral (barcode scan), positive (save to prefs), negative (do nothing)
            */

            f = newInstance(preference.key, { dialog: Dialog?, text: String? ->
                var text = text
                //neutral edit text callback
                //change request code for brapi url vs oidc url
                if (preference.key == brapiURLPreference!!.key) {

                } else if (preference.key == brapiDisplayName!!.key) {
                    text =
                        oldBaseUrl!!.replace("https?://(?:www\\.)?(.*?)(?:/.*)?$".toRegex(), "$1")
                    brapiDisplayName!!.setText(text)
                    onPreferenceChange(brapiDisplayName!!, text)
                } else {
                    preferences?.edit {
                        putBoolean(keyUtil.brapiExplicitOidcUrl, true)
                    }
                    //startBarcodeScan(REQUEST_BARCODE_SCAN_OIDC_URL)
                }

                dialog?.dismiss()
            }, { dialog: Dialog?, text: String? ->
                var text = text
                //positive edit text callback
                if (preference.key == brapiURLPreference!!.key) {
                    brapiURLPreference!!.setText(text!!)
                    if (oldBaseUrl != text) { // skip updates if url hasn't actually changed
                        onPreferenceChange(brapiURLPreference!!, text)
                    }
                    brapiAuth()
                } else if (preference.key == brapiDisplayName!!.key) {
                    text =
                        if (text.isNullOrEmpty()) getString(R.string.export_source_brapi) else text
                    brapiDisplayName!!.setText(text)
                    onPreferenceChange(brapiDisplayName!!, text)
                } else if (preference.key == brapiClientIdPreference!!.key) {
                    //pass to ensure oidc isn't updated as well, client id is automatically handled by xml definition
                } else {
                    preferences?.edit {
                        putBoolean(keyUtil.brapiExplicitOidcUrl, true)
                    }
                    brapiOIDCURLPreference!!.setText(text!!)
                }

                dialog?.dismiss()
            }, { dialog: Dialog?, _: String? ->

                //negative edit text callback
                dialog?.dismiss()
            })
        } else {
            f = null
        }

        if (f != null) {
            f.setTargetFragment(this, 0)
            f.show(requireFragmentManager(), DIALOG_FRAGMENT_TAG)
        } else {
            super.onDisplayPreferenceDialog(preference)
        }
    }

    private fun setupToolbar() {
//        val act: Activity? = activity
//        if (act != null) {
//            val bar: ActionBar? = (this.activity as PreferencesActivity).getSupportActionBar()
//            if (bar != null) {
//                bar.title = getString(R.string.brapi_info_title)
//            }
//        }
    }

    private fun setBaseURLSummary() {
        val url: String = preferences!!.getString(
            keyUtil.brapiUrl,
            "https://test-server.brapi.org"
        )!!
        val displayName: String = preferences!!.getString(
            keyUtil.brapiDisplayName,
            getString(R.string.brapi_edit_display_name_default)
        )!!
        brapiURLPreference!!.setSummary(url)
        brapiDisplayName!!.setSummary(displayName)
    }

    //should only be called from brapi auth or the warning dialog
    private fun startAuth() {
        val brapiHost = preferences!!.getString(keyUtil.brapiUrl, null)
        if (brapiHost != null) {
            val intent = Intent()
            intent.setClassName(requireContext(), BrapiAuthActivity::class.java.name)
            startActivityForResult(intent, AUTH_REQUEST_CODE)
        }
    }

    //checks the uri scheme, uris without http/https causes auth to crash
    private fun brapiAuth() {

        if (brapiOIDCURLPreference!!.text?.startsWith("http://") == true
            || brapiURLPreference!!.text?.startsWith("http://") == true
        ) {
            if (mBrapiHttpWarningDialog != null
                && !mBrapiHttpWarningDialog!!.isShowing
            ) {
                mBrapiHttpWarningDialog!!.show()
            }
        } else if (brapiOIDCURLPreference!!.text?.startsWith("https://") == true
            && brapiURLPreference!!.text?.startsWith("https://") == true
        ) {
            //without this check, urls without https:// or http:// were causing brapi auth to crash

            startAuth()
        }
    }
    
    private fun setServer(url: String, displayName: String, oidcUrl: String, oidcFlow: String?) {
        oldBaseUrl = url
        brapiURLPreference!!.setText(url)
        brapiDisplayName!!.setText(displayName)
        brapiOIDCURLPreference!!.setText(oidcUrl)
        preferences.edit { putString(keyUtil.brapiOidc, oidcUrl) }
        if (oidcFlow != null) brapiOIDCFlow!!.setValue(oidcFlow)

        setOidcFlowUi()
        brapiAuth()
    }

    /**
     * Check the explicit flag.
     * @param newValue the newly changed url
     */
    private fun updateUrls(newValue: String) {
        val oldOidcUrl: String = preferences!!.getString(keyUtil.brapiOidc, "")!!

        // remove scheme and subdomain for initial display name
        val displayName = newValue.replace("https?://(?:www\\.)?(.*?)(?:/.*)?$".toRegex(), "$1")

        Log.d(TAG, "$oldBaseUrl to $oldOidcUrl")

        if (!preferences!!.getBoolean(keyUtil.brapiExplicitOidcUrl, false)) {
            //regex replace old base within oidc to new value
            //this might lead to invalid urls if the user forgets a '/' and other cases
            //where oidc is explicitly changed first (fixed with preference flag)

            val newOidcUrl = oldOidcUrl.replaceFirst(oldBaseUrl!!.toRegex(), newValue)

            Log.d(TAG, newOidcUrl)

            setServer(newValue, displayName, newOidcUrl, null)
        }

        oldBaseUrl = newValue
    }

    /**
     * Handles the UI state for authorize button.
     * When there is a valid host, the authorize button is visible.
     * If there is a token, the logout and reauthorize buttons appear.
     */
    fun setButtonView() {
        val brapiToken = preferences.getString(keyUtil.brapiToken, null)
        val brapiHost = preferences.getString(keyUtil.brapiUrl, null)

        if (brapiHost != null) {  // && !brapiHost.equals(getString(R.string.brapi_base_url_default))) {

            if (brapiToken != null) {
                // Show if our logout button if it is not shown already
                brapiServerPrefCategory!!.addPreference(brapiLogoutButton!!)
            } else {
                brapiServerPrefCategory!!.removePreference(brapiLogoutButton!!)
            }
        } else {
            brapiServerPrefCategory!!.removePreference(brapiLogoutButton!!)
        }
    }

    private fun setOidcFlowUi() {
        val preferenceCategory = findPreference<PreferenceCategory?>("brapi_oidc_settings")

        if (preferenceCategory != null) {
            if (preferences.getString(
                    keyUtil.brapiFlow,
                    getString(R.string.preferences_brapi_oidc_flow_oauth_implicit)
                )
                != getString(R.string.preferences_brapi_oidc_flow_old_custom)
            ) {
                preferenceCategory.addPreference(brapiOIDCURLPreference!!)
            } else {
                preferenceCategory.removePreference(brapiOIDCURLPreference!!)
            }
        }

        if (brapiOIDCFlow != null) {
            Log.d(TAG, brapiOIDCFlow!!.value)
        }
    }

//    private fun isValidUrl(url: String): Boolean {
//        try {
//            val obj = URL(url)
//            return obj.protocol == "http" || obj.protocol == "https"
//        } catch (e: MalformedURLException) {
//            Log.e(TAG, "Invalid URL: " + url, e)
//            return false
//        }
//    }
//
//    private fun showErrorDialog(message: String?) {
//        AlertDialog.Builder(this.activity, R.style.AppAlertDialog)
//            .setTitle(R.string.preferences_brapi_server_scan_error)
//            .setMessage(message)
//            .setPositiveButton(R.string.ok, null)
//            .setIcon(R.drawable.ic_dialog_alert)
//            .show()
//    }

//    /**
//     * Check server information and log supported calls
//     */
//    private fun checkServerInfo() {
//        val act = activity
//        if (act != null) {
//            if (Utils.isConnected(act)) {
//                act.supportFragmentManager
//                    .beginTransaction()
//                    .replace(R.id.prefs_container, BrapiServerInfoFragment())
//                    .addToBackStack(null)
//                    .commit()
//            } else {
//                Toast.makeText(act, R.string.device_offline_warning, Toast.LENGTH_SHORT).show()
//            }
//        }
//    }

    @Deprecated("Deprecated in Java")
    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)
        if (resultCode == Activity.RESULT_OK) {
           if (requestCode == BrapiAuthActivity.END_SESSION_REQUEST_CODE) {
                preferences?.edit {remove(keyUtil.brapiId) }
                preferences?.edit { remove(keyUtil.brapiToken) }
                setButtonView()
            }

            when (requestCode) {
                AUTH_REQUEST_CODE -> {}
            }
        }
    }

    companion object {
        private val TAG: String = BrapiFragment::class.java.simpleName
        private const val AUTH_REQUEST_CODE = 123
        private const val DIALOG_FRAGMENT_TAG =
            "com.tracker.fieldbook.preferences.BRAPI_DIALOG_FRAGMENT"
    }
}