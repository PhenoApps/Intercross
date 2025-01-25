package org.phenoapps.intercross.fragments.app_intro

import android.Manifest
import android.content.Intent
import android.content.SharedPreferences
import android.os.Build
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import androidx.activity.result.contract.ActivityResultContracts
import androidx.core.app.ActivityCompat
import androidx.fragment.app.Fragment
import androidx.preference.PreferenceManager
import com.github.appintro.SlidePolicy
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.withContext
import org.phenoapps.androidlibrary.Utils
import org.phenoapps.fragments.storage.PhenoLibStorageDefinerFragment.AssetSample
import org.phenoapps.intercross.Constants
import org.phenoapps.utils.BaseDocumentTreeUtil
import org.phenoapps.intercross.R
import org.phenoapps.intercross.activities.AppIntroActivity
import org.phenoapps.intercross.views.RequiredSetupItem
import pub.devrel.easypermissions.EasyPermissions

class RequiredSetupPolicyFragment : Fragment(), SlidePolicy {
    private var slideTitle: String? = null
    private var slideSummary: String? = null
    private var slideBackgroundColor: Int? = null

    private var permissionsSetupItem: RequiredSetupItem? = null
    private var storageDefinerSetupItem: RequiredSetupItem? = null

    private var prefs: SharedPreferences? = null

    private val scope by lazy { CoroutineScope(Dispatchers.Main) }

    private val REQUEST_PERMISSIONS_CODE = 12

    private val samples = mapOf(
        AssetSample("wishlist_import", "wishlist_sample.csv") to R.string.dir_wishlist_import,
        AssetSample("parents_import", "parents_sample.csv") to R.string.dir_parents_import
    )

    private var directories: Array<String>? = null

    private val launcher = registerForActivityResult(ActivityResultContracts.OpenDocumentTree()) { uri ->

        uri?.let { nonNullUri ->

            val flags =
                Intent.FLAG_GRANT_READ_URI_PERMISSION or Intent.FLAG_GRANT_WRITE_URI_PERMISSION

            activity?.contentResolver?.takePersistableUriPermission(uri, flags)

            runBlocking {

                directories?.let { dirs ->

                    BaseDocumentTreeUtil.defineRootStructure(activity, nonNullUri, dirs)?.let { root ->

                        samples.entries.forEach { entry ->

                            val sampleAsset = entry.key
                            val dir = entry.value

                            BaseDocumentTreeUtil.copyAsset(activity, sampleAsset.name, sampleAsset.dir, dir)
                        }

                    }
                }
            }
        }
    }

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? = inflater.inflate(R.layout.app_intro_required_setup_slide, container, false)

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        prefs = context?.let { PreferenceManager.getDefaultSharedPreferences(it) }

        val slideTitle = view.findViewById<TextView>(R.id.slide_title)
        val slideSummary = view.findViewById<TextView>(R.id.slide_summary)

        slideTitle?.text = this.slideTitle
        slideSummary?.text = this.slideSummary

        slideBackgroundColor?.let { view.setBackgroundResource(it) }


        permissionsSetupItem = view.findViewById(R.id.permissions_setup_item)
        storageDefinerSetupItem = view.findViewById(R.id.storage_definer_setup_item)

        initSetupItems()

        //define directories that should be created in root storage
        context?.let { ctx ->
            val wishlistImport = ctx.getString(R.string.dir_wishlist_import)
            val parentsImport = ctx.getString(R.string.dir_parents_import)
            val crossesExport = ctx.getString(R.string.dir_crosses_export)
            directories = arrayOf(wishlistImport, parentsImport, crossesExport)
        }
    }

    private fun initSetupItems() {
        permissionsSetupItem?.apply {
            setIcon(R.drawable.ic_configure_white)
            setTitle(getString(R.string.app_intro_permissions_title))
            setSummary(getString(R.string.app_intro_permissions_summary))
            setOnClickListener {
                performSetup(this, {requestPermissions()}, {validatePermissions()})
            }
        }

        storageDefinerSetupItem?.apply {
            setIcon(R.drawable.ic_storage_white)
            setTitle(getString(R.string.app_intro_storage_title))
            setSummary(getString(R.string.app_intro_storage_summary))
            setOnClickListener {
                performSetup(this, {requestStorageDefiner()}, {validateStorage()})
            }
        }

        checkSetupStatus(permissionsSetupItem, validatePermissions())
        checkSetupStatus(storageDefinerSetupItem, validateStorage())
    }

    private fun performSetup(requiredSetupItem: RequiredSetupItem, setupLaunch: () -> Unit, validatorFunction: () -> Boolean) {
        scope.launch {

            setupLaunch()

            withContext(Dispatchers.Default) {
                while (!validatorFunction()) {
                    // check every 100ms if the item is set up i.e. call back is completed
                    delay(100)
                }
            }

            checkSetupStatus(requiredSetupItem, validatorFunction())

        }
    }

    private fun checkSetupStatus(setupItemView: RequiredSetupItem?, isSet: Boolean) {
        if (isSet) {
            setupItemView?.setStatus(R.drawable.ic_check_white)
        }
    }

    private fun requestPermissions() {
        var perms = Constants.permissions
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            perms += arrayOf(
                Manifest.permission.BLUETOOTH_SCAN,
                Manifest.permission.BLUETOOTH_CONNECT
            )
        }

        ActivityCompat.requestPermissions(activity as AppIntroActivity, perms, REQUEST_PERMISSIONS_CODE)
    }

    private fun requestStorageDefiner() {
        launcher.launch(null)
    }

    private fun validatePermissions(): Boolean {
        var permissionsGranted = false

        var perms = arrayOf<String?>(
            Manifest.permission.READ_EXTERNAL_STORAGE,
            Manifest.permission.CAMERA
        )
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
            perms = arrayOf(
                Manifest.permission.CAMERA
            )
        }

        context?.let {
            permissionsGranted = EasyPermissions.hasPermissions(
                it,
                *perms
            )
        }

        return permissionsGranted
    }

    private fun validateStorage(): Boolean {
        val root = BaseDocumentTreeUtil.getRoot(context)
        return (root != null && root.exists())
    }

    override val isPolicyRespected: Boolean
        get() = validateItems()

    override fun onUserIllegallyRequestedNextPage() {
        if (!validatePermissions()) {
            Utils.showShortToast(context, getString(R.string.app_intro_permissions_warning))
        } else
        if (!validateStorage()) {
            Utils.showShortToast(context, getString(R.string.app_intro_storage_warning))
        }
    }

    private fun validateItems(): Boolean {
        return validatePermissions() && validateStorage()
    }

    companion object {
        fun newInstance(
            slideTitle: String,
            slideSummary: String,
            slideBackgroundColor: Int
        ): RequiredSetupPolicyFragment {
            val fragment = RequiredSetupPolicyFragment()
            fragment.slideTitle = slideTitle
            fragment.slideSummary = slideSummary
            fragment.slideBackgroundColor = slideBackgroundColor
            return fragment
        }

    }
}