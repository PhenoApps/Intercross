package org.phenoapps.intercross.fragments.preferences

import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.net.Uri
import android.os.Bundle
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.LinearLayout
import androidx.appcompat.app.AppCompatActivity
import androidx.core.content.ContextCompat
import androidx.lifecycle.lifecycleScope
import androidx.navigation.fragment.findNavController
import androidx.swiperefreshlayout.widget.CircularProgressDrawable
import com.danielstone.materialaboutlibrary.ConvenienceBuilder
import com.danielstone.materialaboutlibrary.MaterialAboutFragment
import com.danielstone.materialaboutlibrary.items.MaterialAboutActionItem
import com.danielstone.materialaboutlibrary.items.MaterialAboutItemOnClickAction
import com.danielstone.materialaboutlibrary.items.MaterialAboutTitleItem
import com.danielstone.materialaboutlibrary.model.MaterialAboutCard
import com.danielstone.materialaboutlibrary.model.MaterialAboutList
import com.google.android.material.bottomnavigation.BottomNavigationView
import com.michaelflisar.changelog.ChangelogBuilder
import com.michaelflisar.changelog.classes.ImportanceChangelogSorter
import com.mikepenz.aboutlibraries.LibsBuilder
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import org.json.JSONObject
import org.phenoapps.intercross.BuildConfig
import org.phenoapps.intercross.R
import org.phenoapps.intercross.activities.MainActivity
import java.net.HttpURLConnection
import java.net.URL


class AboutFragment : MaterialAboutFragment() {

    private var mBottomNavBar: BottomNavigationView? = null
    private lateinit var updateCheckItem: MaterialAboutActionItem
    private lateinit var  circularProgressDrawable: CircularProgressDrawable

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        requireContext().setTheme(R.style.AppTheme)

        circularProgressDrawable = CircularProgressDrawable(requireContext())
        circularProgressDrawable.setStyle(CircularProgressDrawable.DEFAULT)
        circularProgressDrawable.start()
    }

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        // to add bottom navigation
        // take the original view, add it to wrapper
        // create a bottom nav view and add it to the wrapper

        // original view from MaterialAboutFragment
        val originalView = super.onCreateView(inflater, container, savedInstanceState)

        // Create a wrapper layout
        val wrapper = LinearLayout(requireContext()).apply {
            orientation = LinearLayout.VERTICAL
            layoutParams = ViewGroup.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT,
                ViewGroup.LayoutParams.MATCH_PARENT
            )
        }

        // add the originalView to wrapper with weight
        originalView?.let {
            wrapper.addView(it, LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT,
                0,
                1f // take up all space except for bottom nav
            ))
        }

        mBottomNavBar = BottomNavigationView(requireContext()).apply {
            id = R.id.preferences_bottom_nav_bar
            inflateMenu(R.menu.menu_bot_nav)
            layoutParams = LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT,
                LinearLayout.LayoutParams.WRAP_CONTENT
            )
        }
        wrapper.addView(mBottomNavBar)

        (activity as MainActivity).setBackButtonToolbar()
        (activity as MainActivity).supportActionBar?.apply {
            title = getString(R.string.preferences_about_title)
            show()
        }

        return wrapper
    }

    private fun setupBottomNavBar() {
        mBottomNavBar?.selectedItemId = R.id.action_nav_preferences

        mBottomNavBar?.setOnItemSelectedListener { item ->
            when (item.itemId) {
                R.id.action_nav_home -> {
                    findNavController().navigate(R.id.global_action_to_events)
                }
                R.id.action_nav_parents -> {
                    findNavController().navigate(R.id.global_action_to_parents)
                }
                R.id.action_nav_cross_count -> {
                    findNavController().navigate(R.id.global_action_to_cross_tracker)
                }
            }
            true
        }
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        setupBottomNavBar()
        checkForUpdate()
    }

    override fun onResume() {
        super.onResume()
        mBottomNavBar?.selectedItemId = R.id.action_nav_preferences
    }

    override fun getMaterialAboutList(c: Context): MaterialAboutList {
        val appCardBuilder = MaterialAboutCard.Builder()

        appCardBuilder.addItem(MaterialAboutTitleItem.Builder()
                .text(getString(R.string.app_name))
                .icon(R.mipmap.ic_launcher)
                .build())
        appCardBuilder.addItem(ConvenienceBuilder.createVersionActionItem(c,
                ContextCompat.getDrawable(requireContext(), R.drawable.ic_about_info),
                getString(R.string.about_version_title),
                false))

        updateCheckItem = MaterialAboutActionItem.Builder()
                .text(getString(R.string.check_updates_title))
                .icon(circularProgressDrawable)
                .setOnClickAction(null) // Set initially to null, will be updated later
                .build()

        appCardBuilder.addItem(updateCheckItem)

        appCardBuilder.addItem(MaterialAboutActionItem.Builder()
            .text(getString(R.string.changelog_title))
            .icon(R.drawable.ic_about_changelog)
            .setOnClickAction { showChangelog(managedShow = false, rateButton = false) }
            .build())

        // appCardBuilder.addItem(ConvenienceBuilder.createWebsiteActionItem(c,
        //         ContextCompat.getDrawable(requireContext(), R.drawable.ic_about_website),
        //         getString(R.string.about_manual_title),
        //         false,
        //         Uri.parse("https://docs.fieldbook.phenoapps.org/en/latest/intercross.html")))

        appCardBuilder.addItem(ConvenienceBuilder.createRateActionItem(c,
                ContextCompat.getDrawable(requireContext(), R.drawable.ic_about_rate),
                getString(R.string.about_rate),
                null
        ))

        val authorCardBuilder = MaterialAboutCard.Builder()
        authorCardBuilder.title(getString(R.string.about_project_lead_title))
        authorCardBuilder.addItem(MaterialAboutActionItem.Builder()
                .text(getString(R.string.about_developer_trife))
                .subText(getString(R.string.about_developer_trife_location))
                .icon(R.drawable.ic_nv_about)
                .build())

        authorCardBuilder.addItem(ConvenienceBuilder.createEmailItem(c,
                ContextCompat.getDrawable(requireContext(), R.drawable.ic_about_email),
                getString(R.string.about_email_title),
                true,
                getString(R.string.about_developer_trife_email),
                "Intercross Question"))

        val contributorsCardBuilder = MaterialAboutCard.Builder()
        contributorsCardBuilder.title(getString(R.string.about_support_title))

        contributorsCardBuilder.addItem(ConvenienceBuilder.createWebsiteActionItem(c,
                ContextCompat.getDrawable(requireContext(), R.drawable.ic_about_contributors),
                getString(R.string.about_contributors_title),
                false,
                Uri.parse("https://github.com/PhenoApps/Intercross#contributors")))

        contributorsCardBuilder.addItem(ConvenienceBuilder.createWebsiteActionItem(c,
                ContextCompat.getDrawable(requireContext(), R.drawable.ic_about_funding),
                getString(R.string.about_contributors_funding_title),
                false,
                Uri.parse("https://github.com/PhenoApps/Intercross#-funding")))

        val technicalCardBuilder = MaterialAboutCard.Builder()
        technicalCardBuilder.title(getString(R.string.about_technical_title))

        technicalCardBuilder.addItem(MaterialAboutActionItem.Builder()
                .text(R.string.about_github_title)
                .icon(R.drawable.ic_about_github)
                .setOnClickAction(ConvenienceBuilder.createWebsiteOnClickAction(c, Uri.parse("https://github.com/PhenoApps/Intercross")))
                .build())

        technicalCardBuilder.addItem(MaterialAboutActionItem.Builder()
                .text(R.string.about_libraries_title)
                .icon(R.drawable.ic_about_libraries)
                .setOnClickAction {
                    LibsBuilder()
                            .withAutoDetect(true)
                            .withActivityTitle(getString(R.string.libraries_title))
                            .withLicenseShown(true)
                            .withVersionShown(true)
                            .start(requireContext())
                }
                .build())

        val otherAppsCardBuilder = MaterialAboutCard.Builder()
        otherAppsCardBuilder.title(getString(R.string.about_title_other_apps))

        otherAppsCardBuilder.addItem(ConvenienceBuilder.createWebsiteActionItem(c,
                ContextCompat.getDrawable(requireContext(), R.drawable.ic_about_website),
                "PhenoApps.org",
                false,
                Uri.parse("http://phenoapps.org/")))

        otherAppsCardBuilder.addItem(MaterialAboutActionItem.Builder()
                .text("Field Book")
                .icon(R.drawable.other_ic_field_book)
                .setOnClickAction(openAppOrStore("com.fieldbook.tracker", c))
                .build())

        otherAppsCardBuilder.addItem(MaterialAboutActionItem.Builder()
                .text("Coordinate")
                .icon(R.drawable.other_ic_coordinate)
                .setOnClickAction(openAppOrStore("org.wheatgenetics.coordinate", c))
                .build())

        return MaterialAboutList(
                appCardBuilder.build(),
                authorCardBuilder.build(),
                contributorsCardBuilder.build(),
                otherAppsCardBuilder.build(),
                technicalCardBuilder.build()
        )
    }

    private fun checkForUpdate() {
        val currentVersion = BuildConfig.VERSION_NAME
        val owner = "PhenoApps"
        val repo = "Intercross"
        val apiUrl = "https://api.github.com/repos/$owner/$repo/releases/latest"

        viewLifecycleOwner.lifecycleScope.launch {
            try {
                val result = withContext(Dispatchers.IO) {
                    val url = URL(apiUrl)
                    val connection = url.openConnection() as HttpURLConnection
                    connection.requestMethod = "GET"

                    if (connection.responseCode == HttpURLConnection.HTTP_OK) {
                        connection.inputStream.bufferedReader().use { it.readText() }
                    } else {
                        null
                    }
                }

                result?.let {
                    val json = JSONObject(it)
                    val latestVersion = json.getString("tag_name").removePrefix("v")
                    val downloadUrl = json.getJSONArray("assets").getJSONObject(0).getString("browser_download_url")

                    val isNewVersionAvailable = isNewerVersion(currentVersion, latestVersion)
                    showVersionStatus(isNewVersionAvailable, latestVersion, downloadUrl)
                }
            } catch (e: Exception) {
                Log.e("AboutFragment", "Error checking for updates: ${e.message}")
            }
        }
    }

    private fun showVersionStatus(isNewVersionAvailable: Boolean, latestVersion: String?, downloadUrl: String?) {
        circularProgressDrawable.stop()
        if (isNewVersionAvailable) {
            updateCheckItem.text = getString(R.string.found_updates_title)
            updateCheckItem.subText = latestVersion
            updateCheckItem.icon = ContextCompat.getDrawable(requireContext(), R.drawable.ic_about_get_update)
            updateCheckItem.setOnClickAction {
                downloadUrl?.let {
                    val browserIntent = Intent(Intent.ACTION_VIEW, Uri.parse(it))
                    startActivity(browserIntent)
                }
            }
        } else {
            updateCheckItem.text = getString(R.string.no_updates_title)
            updateCheckItem.icon = ContextCompat.getDrawable(requireContext(), R.drawable.ic_about_up_to_date)
            updateCheckItem.setOnClickAction(null)
        }
        refreshMaterialAboutList()
    }

    private fun isNewerVersion(currentVersion: String, latestVersion: String): Boolean {
        val currentVersionComponents = currentVersion.split(".")
        val latestVersionComponents = latestVersion.split(".")

        val versionComponentsToCompare = minOf(currentVersionComponents.size, latestVersionComponents.size)

        for (i in 0 until versionComponentsToCompare) {
            val currentComponent = currentVersionComponents[i].toInt()
            val latestComponent = latestVersionComponents[i].toInt()

            if (currentComponent < latestComponent) return true
            if (currentComponent > latestComponent) return false
        }

        return false
    }

    private fun openAppOrStore(packageName: String, c: Context): MaterialAboutItemOnClickAction {
        val packageManager = requireContext().packageManager
        return try {
            packageManager.getPackageInfo(packageName, 0)
            MaterialAboutItemOnClickAction {
                val launchIntent = packageManager.getLaunchIntentForPackage(packageName)
                startActivity(launchIntent)
            }
        } catch (e: PackageManager.NameNotFoundException) {
            ConvenienceBuilder.createWebsiteOnClickAction(c, Uri.parse("https://play.google.com/store/apps/details?id=$packageName"))
        }
    }

    private fun showChangelog(managedShow: Boolean, rateButton: Boolean) {
        ChangelogBuilder()
            .withUseBulletList(true) // show bullets for each changelog row
            .withManagedShowOnStart(managedShow) // show dialog only if the changelog has new infos and will only show this new info
            .withRateButton(rateButton) // enable this to show a "rate app" button in the dialog => clicking it will open the play store; the parent activity or target fragment can also implement IChangelogRateHandler to handle the button click
            .withSummary(false,true) // enable this to show a summary and a "show more" button, the second parameter describes if releases without summary items should be shown expanded or not
            .withTitle(getString(R.string.changelog_title)) // provide a custom title if desired, default one is "Changelog <VERSION>"
            .withOkButtonLabel("OK") // provide a custom ok button text if desired, default one is "OK"
            .withSorter(ImportanceChangelogSorter())
            .buildAndShowDialog(activity as AppCompatActivity?, false) // second parameter defines, if the dialog has a dark or light theme
    }
}
