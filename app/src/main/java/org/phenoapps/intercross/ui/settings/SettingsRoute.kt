package org.phenoapps.intercross.ui.settings

import androidx.annotation.DrawableRes
import androidx.annotation.StringRes
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Search
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import org.phenoapps.intercross.R
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.IconButton
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.NavigationBarItemDefaults
import androidx.compose.ui.graphics.Color
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.ui.res.painterResource
import org.phenoapps.intercross.ui.app.TopBarState
import org.phenoapps.intercross.ui.app.BottomBarState
import org.phenoapps.intercross.ui.app.BottomDestinations
import org.phenoapps.intercross.ui.app.SettingsPage
import org.phenoapps.intercross.ui.components.SafeIcon
import org.phenoapps.intercross.ui.theme.AppTheme

data class SettingsRow(
    @param:StringRes val titleRes: Int,
    @param:DrawableRes val iconRes: Int,
    val page: String,
)

@Composable
fun SettingsRoute(
    onOpenPage: (String) -> Unit,
    bottomBarState: BottomBarState? = null,
) {
    var searchQuery by remember { mutableStateOf("") }

    val rows = remember {
        listOf(
            SettingsRow(R.string.prefs_profile_title, R.drawable.ic_account_circle_black_24dp, SettingsPage.PROFILE),
            SettingsRow(R.string.prefs_appearance_title, R.drawable.ic_pref_appearance, SettingsPage.APPEARANCE),
            SettingsRow(R.string.prefs_layout_title, R.drawable.ic_book_open, SettingsPage.LAYOUT),
            SettingsRow(R.string.prefs_behavior_title, R.drawable.ic_setting_pattern_create, SettingsPage.BEHAVIOR),
            SettingsRow(R.string.prefs_printing_title, R.drawable.ic_setting_print_connect, SettingsPage.PRINTING),
            SettingsRow(R.string.prefs_database_title, R.drawable.ic_database, SettingsPage.DATABASE),
            SettingsRow(R.string.prefs_brapi_title, R.drawable.ic_brapi, SettingsPage.BRAPI),
            SettingsRow(R.string.prefs_about_title, R.drawable.ic_about_info, SettingsPage.ABOUT),
        )
    }

    // Pre-resolve all search item titles for filtering
    val searchItemTitles = AllSettingsItems.map { item ->
        item to stringResource(item.titleRes)
    }

    SettingsScreenContent(
        rows = rows,
        searchQuery = searchQuery,
        onSearchQueryChange = { searchQuery = it },
        searchItemTitles = searchItemTitles,
        onOpenPage = onOpenPage,
        bottomBarState = bottomBarState,
    )
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SettingsScreenContent(
    rows: List<SettingsRow>,
    searchQuery: String,
    onSearchQueryChange: (String) -> Unit,
    searchItemTitles: List<Pair<SettingsSearchItem, String>>,
    onOpenPage: (String) -> Unit,
    bottomBarState: BottomBarState? = null,
) {
    val topBarState = TopBarState(titleRes = R.string.settings_label)

    androidx.compose.material3.Scaffold(
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
        bottomBar = {
            if (bottomBarState != null) {
                NavigationBar(
                    containerColor = AppTheme.colors.primary,
                ) {
                    BottomDestinations.forEach { destination ->
                        NavigationBarItem(
                            selected = bottomBarState.selectedRoute == destination.route,
                            onClick = { bottomBarState.onTabSelected(destination.route) },
                            icon = { SafeIcon(resId = destination.iconRes, contentDescription = null, tint = Color.White) },
                            label = if (bottomBarState.selectedRoute == destination.route) {
                                { Text(stringResource(destination.labelRes), color = Color.White) }
                            } else null,
                            colors = NavigationBarItemDefaults.colors(
                                indicatorColor = Color.White.copy(alpha = 0.2f),
                            ),
                        )
                    }
                }
            }
        },
    ) { innerPadding ->
    LazyColumn(
        modifier = Modifier
            .fillMaxSize()
            .padding(innerPadding)
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(8.dp),
    ) {
        item {
            OutlinedTextField(
                value = searchQuery,
                onValueChange = onSearchQueryChange,
                modifier = Modifier
                    .fillMaxWidth()
                    .testTag("settings_search"),
                placeholder = { Text(stringResource(R.string.search_settings_hint)) },
                leadingIcon = {
                    Icon(
                        imageVector = Icons.Default.Search,
                        contentDescription = null,
                    )
                },
                singleLine = true,
            )
        }

        if (searchQuery.isBlank()) {
            // Show category-level list when not searching
            items(rows, key = { it.page }) { row ->
                SettingsCategoryRow(
                    row = row,
                    onClick = { onOpenPage(row.page) },
                )
            }
        } else {
            // Search individual settings items
            val searchResults = searchItemTitles.filter { (_, title) ->
                title.contains(searchQuery, ignoreCase = true)
            }.map { it.first }
            if (searchResults.isEmpty()) {
                item {
                    Text(
                        text = stringResource(R.string.no_settings_found, searchQuery),
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.padding(16.dp),
                    )
                }
            } else {
                items(searchResults, key = { "${it.page}:${it.titleRes}" }) { result ->
                    SettingsSearchResultRow(
                        item = result,
                        onClick = { onOpenPage(result.page) },
                    )
                }
            }
        }
    }
    } // Scaffold
}

@Composable
private fun SettingsCategoryRow(
    row: SettingsRow,
    onClick: () -> Unit,
) {
    Column(modifier = Modifier.testTag("settings_flat_row")) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .clickable(onClick = onClick)
                .padding(16.dp),
            horizontalArrangement = Arrangement.spacedBy(14.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Icon(
                painter = painterResource(row.iconRes),
                contentDescription = null,
                tint = MaterialTheme.colorScheme.primary,
            )
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = stringResource(row.titleRes),
                    style = MaterialTheme.typography.titleMedium,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                )
            }
        }
        HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant)
    }
}

@Composable
private fun SettingsSearchResultRow(
    item: SettingsSearchItem,
    onClick: () -> Unit,
) {
    Column(modifier = Modifier.testTag("settings_search_result")) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .clickable(onClick = onClick)
                .padding(16.dp),
            horizontalArrangement = Arrangement.spacedBy(14.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Icon(
                painter = painterResource(item.iconRes),
                contentDescription = null,
                tint = MaterialTheme.colorScheme.primary,
            )
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = stringResource(item.titleRes),
                    style = MaterialTheme.typography.titleMedium,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                )
                Text(
                    text = stringResource(item.categoryRes),
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
        }
        HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant)
    }
}

// ─── Previews ───────────────────────────────────────────────────────────────────

@androidx.compose.ui.tooling.preview.Preview(showBackground = true, name = "Settings Hub - Category List")
@Composable
internal fun SettingsHubPreview() {
    org.phenoapps.intercross.ui.theme.IntercrossPreviewTheme {
        SettingsScreenContent(
            rows = listOf(
                SettingsRow(R.string.prefs_profile_title, R.drawable.ic_account_circle_black_24dp, SettingsPage.PROFILE),
                SettingsRow(R.string.prefs_appearance_title, R.drawable.ic_pref_appearance, SettingsPage.APPEARANCE),
                SettingsRow(R.string.prefs_layout_title, R.drawable.ic_book_open, SettingsPage.LAYOUT),
                SettingsRow(R.string.prefs_behavior_title, R.drawable.ic_setting_pattern_create, SettingsPage.BEHAVIOR),
                SettingsRow(R.string.prefs_printing_title, R.drawable.ic_setting_print_connect, SettingsPage.PRINTING),
                SettingsRow(R.string.prefs_database_title, R.drawable.ic_database, SettingsPage.DATABASE),
                SettingsRow(R.string.prefs_brapi_title, R.drawable.ic_brapi, SettingsPage.BRAPI),
                SettingsRow(R.string.prefs_about_title, R.drawable.ic_about_info, SettingsPage.ABOUT),
            ),
            searchQuery = "",
            onSearchQueryChange = {},
            searchItemTitles = emptyList(),
            onOpenPage = {},
        )
    }
}

@androidx.compose.ui.tooling.preview.Preview(showBackground = true, name = "Settings Hub - Search")
@Composable
internal fun SettingsHubSearchPreview() {
    org.phenoapps.intercross.ui.theme.IntercrossPreviewTheme {
        SettingsScreenContent(
            rows = emptyList(),
            searchQuery = "person",
            onSearchQueryChange = {},
            searchItemTitles = AllSettingsItems.take(3).map {
                it to "Profile"
            },
            onOpenPage = {},
        )
    }
}
