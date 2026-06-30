package org.phenoapps.intercross.ui.settings

import android.content.Context
import android.content.Intent
import android.net.Uri
import androidx.appcompat.app.AppCompatActivity
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.material3.Card
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import com.michaelflisar.changelog.ChangelogBuilder
import com.michaelflisar.changelog.classes.ImportanceChangelogSorter
import com.mikepenz.aboutlibraries.LibsBuilder
import org.phenoapps.intercross.BuildConfig
import org.phenoapps.intercross.R
import androidx.compose.foundation.layout.Box
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.tooling.preview.Preview
import org.phenoapps.intercross.ui.app.TopBarState
import org.phenoapps.intercross.ui.theme.AppTheme
import androidx.core.net.toUri

@Composable
fun AboutSettingsRoute(
    onBack: () -> Unit = {},
) {
    val context = LocalContext.current

    val developerEmail = stringResource(R.string.about_developer_trife_email)
    val librariesTitle = stringResource(R.string.libraries_title)
    val emailTitle = stringResource(R.string.about_email_title)

    AboutSettingsScreenContent(
        versionName = BuildConfig.VERSION_NAME,
        onChangelog = { showChangelog(context) },
        onRate = { openAppStore(context, "org.phenoapps.intercross") },
        developerName = stringResource(R.string.about_developer_trife),
        developerLocation = stringResource(R.string.about_developer_trife_location),
        developerEmail = developerEmail,
        onEmail = {
            val intent = Intent(Intent.ACTION_SENDTO).apply {
                data = "mailto:$developerEmail".toUri()
                putExtra(Intent.EXTRA_SUBJECT, "Intercross Question")
            }
            startSafely(context, Intent.createChooser(intent, emailTitle))
        },
        onContributors = { openUri(context, "https://github.com/PhenoApps/Intercross#contributors") },
        onFunding = { openUri(context, "https://github.com/PhenoApps/Intercross#-funding") },
        onPhenoApps = { openUri(context, "http://phenoapps.org/") },
        onFieldBook = { openAppOrStore(context, "com.fieldbook.tracker") },
        onCoordinate = { openAppOrStore(context, "org.wheatgenetics.coordinate") },
        onGitHub = { openUri(context, "https://github.com/PhenoApps/Intercross") },
        onLibraries = {
            LibsBuilder()
                .withActivityTitle(librariesTitle)
                .withLicenseShown(true)
                .withVersionShown(true)
                .start(context)
        },
        onBack = onBack,
    )
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AboutSettingsScreenContent(
    versionName: String,
    onChangelog: () -> Unit,
    onRate: () -> Unit,
    developerName: String,
    developerLocation: String,
    developerEmail: String,
    onEmail: () -> Unit,
    onContributors: () -> Unit,
    onFunding: () -> Unit,
    onPhenoApps: () -> Unit,
    onFieldBook: () -> Unit,
    onCoordinate: () -> Unit,
    onGitHub: () -> Unit,
    onLibraries: () -> Unit,
    onBack: () -> Unit = {},
) {
    val topBarState = TopBarState(titleRes = R.string.preferences_about_title, showBack = true, onBack = onBack)

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Text(topBarState.title ?: topBarState.titleRes?.let { stringResource(it) }.orEmpty())
                },
                navigationIcon = {
                    if (topBarState.showBack) {
                        IconButton(onClick = { topBarState.onBack?.invoke() }) {
                            Icon(painterResource(R.drawable.arrow_back_24px), contentDescription = stringResource(R.string.navigate_back))
                        }
                    }
                },
                actions = {
                    topBarState.actions.forEach { action ->
                        if (action.iconRes != null) {
                            IconButton(onClick = { action.onClick?.invoke() }) {
                                Icon(painterResource(action.iconRes), contentDescription = stringResource(action.labelRes))
                            }
                        } else {
                            TextButton(onClick = { action.onClick?.invoke() }) {
                                Text(stringResource(action.labelRes))
                            }
                        }
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = AppTheme.colors.primary,
                    titleContentColor = AppTheme.colors.surface.topBarContentColor,
                    actionIconContentColor = AppTheme.colors.surface.topBarContentColor,
                    navigationIconContentColor = AppTheme.colors.surface.topBarContentColor,
                ),
            )
        },
    ) { innerPadding ->
    Box(Modifier.padding(innerPadding)) {
    LazyColumn(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(8.dp),
    ) {
        item {
            Card(Modifier.fillMaxWidth()) {
                Row(
                    modifier = Modifier.padding(16.dp),
                    horizontalArrangement = Arrangement.spacedBy(14.dp),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    SettingsIcon(R.drawable.ic_about_info)
                    Column(Modifier.weight(1f)) {
                        Text(stringResource(R.string.app_name), style = MaterialTheme.typography.headlineSmall)
                        Text(
                            "${stringResource(R.string.about_version_title)} $versionName",
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                        )
                    }
                }
            }
        }
        item {
            SettingsActionRow(
                titleRes = R.string.changelog_title,
                iconRes = R.drawable.ic_about_changelog,
                onClick = onChangelog,
            )
        }
        item {
            SettingsActionRow(
                titleRes = R.string.about_rate,
                iconRes = R.drawable.ic_about_rate,
                onClick = onRate,
            )
        }
        item { SettingsSectionHeader(R.string.about_project_lead_title) }
        item {
            SettingsActionRow(
                title = developerName,
                summary = developerLocation,
                iconRes = R.drawable.ic_nv_about,
                onClick = {},
            )
        }
        item {
            SettingsActionRow(
                titleRes = R.string.about_email_title,
                summary = developerEmail,
                iconRes = R.drawable.ic_about_email,
                onClick = onEmail,
            )
        }
        item { SettingsSectionHeader(R.string.about_support_title) }
        item {
            SettingsActionRow(
                titleRes = R.string.about_contributors_title,
                iconRes = R.drawable.ic_about_contributors,
                onClick = onContributors,
            )
        }
        item {
            SettingsActionRow(
                titleRes = R.string.about_contributors_funding_title,
                iconRes = R.drawable.ic_about_funding,
                onClick = onFunding,
            )
        }
        item { SettingsSectionHeader(R.string.about_title_other_apps) }
        item {
            SettingsActionRow(
                title = "PhenoApps.org",
                iconRes = R.drawable.ic_about_website,
                onClick = onPhenoApps,
            )
        }
        item {
            SettingsPreferenceCard(onClick = onFieldBook) {
                Icon(
                    painter = painterResource(R.drawable.other_ic_field_book),
                    contentDescription = null,
                    tint = Color.Unspecified,
                    modifier = Modifier.size(24.dp)
                )
                SettingsText(
                    title = "Field Book",
                    summary = null,
                    modifier = Modifier.weight(1f)
                )
            }
        }
        item {
            SettingsPreferenceCard(onClick = onCoordinate) {
                Icon(
                    painter = painterResource(R.drawable.other_ic_coordinate),
                    contentDescription = null,
                    tint = Color.Unspecified,
                    modifier = Modifier.size(24.dp)
                )
                SettingsText(
                    title = "Coordinate",
                    summary = null,
                    modifier = Modifier.weight(1f)
                )
            }
        }
        item { SettingsSectionHeader(R.string.about_technical_title) }
        item {
            SettingsActionRow(
                titleRes = R.string.about_github_title,
                iconRes = R.drawable.ic_about_github,
                onClick = onGitHub,
            )
        }
        item {
            SettingsActionRow(
                titleRes = R.string.about_libraries_title,
                iconRes = R.drawable.ic_about_libraries,
                onClick = onLibraries,
            )
        }
    }
    } // Box
    } // Scaffold
}

private fun showChangelog(context: Context) {
    val activity = context as? AppCompatActivity ?: return
    ChangelogBuilder()
        .withUseBulletList(true)
        .withManagedShowOnStart(false)
        .withRateButton(false)
        .withSummary(false, true)
        .withTitle(context.getString(R.string.changelog_title))
        .withOkButtonLabel("OK")
        .withSorter(ImportanceChangelogSorter())
        .buildAndShowDialog(activity, false)
}

@Preview(showBackground = true, name = "About Settings")
@Composable
private fun AboutSettingsPreview() {
    org.phenoapps.intercross.ui.theme.IntercrossPreviewTheme {
        AboutSettingsScreenContent(
            versionName = "1.0.0",
            onChangelog = {},
            onRate = {},
            developerName = "Trife",
            developerLocation = "Kansas State University",
            developerEmail = "trife@ksu.edu",
            onEmail = {},
            onContributors = {},
            onFunding = {},
            onPhenoApps = {},
            onFieldBook = {},
            onCoordinate = {},
            onGitHub = {},
            onLibraries = {},
            onBack = {},
        )
    }
}
