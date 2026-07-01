package org.phenoapps.intercross.ui.parents

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Print
import androidx.compose.material3.Card
import androidx.compose.material3.Checkbox
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SmallFloatingActionButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Tab
import androidx.compose.material3.TabRow
import androidx.compose.material3.TabRowDefaults
import androidx.compose.material3.TabRowDefaults.tabIndicatorOffset
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import org.phenoapps.intercross.R
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.IconButton
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.NavigationBarItemDefaults
import org.phenoapps.intercross.ui.app.TopBarAction
import org.phenoapps.intercross.ui.app.TopBarState
import org.phenoapps.intercross.ui.app.BottomBarState
import org.phenoapps.intercross.ui.app.BottomDestinations
import org.phenoapps.intercross.ui.components.ParentListItem
import org.phenoapps.intercross.ui.components.SafeIcon
import org.phenoapps.intercross.ui.preview.PreviewSampleData
import org.phenoapps.intercross.ui.theme.AppTheme
import org.phenoapps.intercross.ui.theme.IntercrossPreviewTheme

@Composable
fun ParentsScreen(
    rows: List<ParentListRow>,
    tab: Int,
    sortType: ParentSortType,
    onTabChange: (Int) -> Unit,
    onSortTypeChange: (ParentSortType) -> Unit,
    onCreateParent: (Int) -> Unit,
    onDeleteSelected: () -> Unit,
    onToggleSelection: (ParentListRow) -> Unit,
    onShowMessage: (String) -> Unit,
    onSelectAll: () -> Unit = {},
    onPrintSelected: () -> Unit = {},
    showSortDialog: Boolean = false,
    onDismissSortDialog: () -> Unit = {},
    topBarState: TopBarState? = null,
    bottomBarState: BottomBarState? = null,
) {
    val selectedCount = rows.count { it.selected }

    // Sort dialog
    if (showSortDialog) {
        androidx.compose.material3.AlertDialog(
            onDismissRequest = onDismissSortDialog,
            title = { Text(stringResource(R.string.sort_by)) },
            text = {
                Column {
                    ParentSortType.entries.forEach { option ->
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clickable {
                                    onSortTypeChange(option)
                                    onDismissSortDialog()
                                }
                                .padding(vertical = 12.dp),
                            verticalAlignment = Alignment.CenterVertically,
                        ) {
                            androidx.compose.material3.RadioButton(
                                selected = sortType == option,
                                onClick = {
                                    onSortTypeChange(option)
                                    onDismissSortDialog()
                                },
                            )
                            Text(
                                text = option.name.lowercase().replaceFirstChar { it.uppercase() },
                                modifier = Modifier.padding(start = 8.dp),
                            )
                        }
                    }
                }
            },
            confirmButton = {},
        )
    }

    Scaffold(
        topBar = {
            if (topBarState != null) {
                @OptIn(ExperimentalMaterial3Api::class)
                (TopAppBar(
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
                ))
            }
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
        floatingActionButton = {
            Column(horizontalAlignment = Alignment.End, verticalArrangement = Arrangement.spacedBy(12.dp)) {
                // Show delete and print mini-FABs when items are selected
                AnimatedVisibility(visible = selectedCount > 0) {
                    Column(horizontalAlignment = Alignment.End, verticalArrangement = Arrangement.spacedBy(12.dp)) {
                        SmallFloatingActionButton(
                            onClick = onPrintSelected,
                            shape = CircleShape,
                            containerColor = AppTheme.colors.accent,
                        ) {
                            Icon(Icons.Default.Print, contentDescription = stringResource(R.string.print), tint = Color.White, modifier = Modifier.size(20.dp))
                        }
                        SmallFloatingActionButton(
                            onClick = onDeleteSelected,
                            shape = CircleShape,
                            containerColor = AppTheme.colors.accent,
                        ) {
                            Icon(Icons.Default.Delete, contentDescription = stringResource(R.string.delete), tint = Color.White, modifier = Modifier.size(20.dp))
                        }
                    }
                }
                // Main add FAB
                FloatingActionButton(
                    onClick = { onCreateParent(if (tab == 2) 1 else 0) },
                    shape = CircleShape,
                    containerColor = AppTheme.colors.accent,
                ) {
                    Icon(Icons.Default.Add, contentDescription = stringResource(R.string.new_parent), tint = Color.White)
                }
            }
        }
    ) { innerPadding ->
        Column(
            Modifier.fillMaxSize().padding(innerPadding),
        ) {
            TabRow(
                selectedTabIndex = tab,
                containerColor = AppTheme.colors.primary,
                contentColor = Color.White,
                indicator = { tabPositions ->
                    if (tabPositions.isNotEmpty()) {
                        TabRowDefaults.SecondaryIndicator(
                            Modifier.tabIndicatorOffset(tabPositions[tab]),
                            color = Color.White,
                        )
                    }
                },
                modifier = Modifier.testTag("primary_tab_row"),
            ) {
                val tabIcons = listOf(R.drawable.ic_nv_parents_tab, R.drawable.ic_female_black_24dp, R.drawable.ic_male_black_24dp)
                listOf(R.string.all_parents, R.string.female, R.string.male).forEachIndexed { index, label ->
                    Tab(
                        selected = tab == index,
                        onClick = { onTabChange(index) },
                        modifier = Modifier.testTag("white_content_tabs"),
                        text = {
                            Text(
                                stringResource(label),
                                color = if (tab == index) Color.White else Color.White.copy(alpha = 0.7f),
                            )
                        },
                        icon = {
                            Icon(
                                painter = painterResource(tabIcons[index]),
                                contentDescription = null,
                                tint = if (tab == index) Color.White else Color.White.copy(alpha = 0.7f),
                            )
                        },
                    )
                }
            }
            LazyColumn(
                modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp),
            ) {
                if (rows.isEmpty()) item { Text(stringResource(R.string.parents_table_empty), Modifier.padding(24.dp)) }
                items(rows, key = { "${it.isGroup}:${it.codeId}" }) { row ->
                    ParentListItem(
                        name = row.name,
                        codeId = row.codeId,
                        sex = row.sex,
                        selected = row.selected,
                        onToggleSelection = { onToggleSelection(row) },
                        isGroup = row.isGroup,
                        crossCount = row.crossCount
                    )
                }
            }
        }
    }
}

// ─── Previews ───────────────────────────────────────────────────────────────────

@Preview(showBackground = true, name = "ParentsScreen - Populated")
@Preview(showBackground = true, name = "ParentsScreen - Tablet", device = "spec:width=800dp,height=1280dp")
@Composable
internal fun ParentsScreenPopulatedPreview() {
    IntercrossPreviewTheme {
        ParentsScreen(
            rows = PreviewSampleData.parentListRows,
            tab = 0,
            sortType = ParentSortType.NAME,
            onTabChange = {},
            onSortTypeChange = {},
            onCreateParent = {},
            onDeleteSelected = {},
            onToggleSelection = {},
            onShowMessage = {},
            topBarState = TopBarState(
                title = "",
                actions = listOf(
                    TopBarAction("select_all", R.string.sort_by, iconRes = R.drawable.ic_select_all),
                    TopBarAction("import", R.string.import_file, iconRes = R.drawable.ic_nv_import_white),
                    TopBarAction("sort", R.string.sort_by, iconRes = R.drawable.sort),
                ),
            ),
            bottomBarState = BottomBarState(
                selectedRoute = "parents",
                onTabSelected = {},
            ),
        )
    }
}

@Preview(showBackground = true, name = "ParentsScreen - Selected")
@Composable
internal fun ParentsScreenSelectedPreview() {
    IntercrossPreviewTheme {
        ParentsScreen(
            rows = PreviewSampleData.parentListRows,
            tab = 0,
            sortType = ParentSortType.NAME,
            onTabChange = {},
            onSortTypeChange = {},
            onCreateParent = {},
            onDeleteSelected = {},
            onToggleSelection = {},
            onShowMessage = {},
            topBarState = TopBarState(
                title = "2 selected",
                actions = listOf(
                    TopBarAction("select_all", R.string.sort_by, iconRes = R.drawable.ic_select_all),
                    TopBarAction("import", R.string.import_file, iconRes = R.drawable.ic_nv_import_white),
                    TopBarAction("sort", R.string.sort_by, iconRes = R.drawable.sort),
                ),
            ),
            bottomBarState = BottomBarState(
                selectedRoute = "parents",
                onTabSelected = {},
            ),
        )
    }
}

@Preview(showBackground = true, name = "ParentsScreen - Empty")
@Composable
internal fun ParentsScreenEmptyPreview() {
    IntercrossPreviewTheme {
        ParentsScreen(
            rows = emptyList(),
            tab = 0,
            sortType = ParentSortType.NAME,
            onTabChange = {},
            onSortTypeChange = {},
            onCreateParent = {},
            onDeleteSelected = {},
            onToggleSelection = {},
            onShowMessage = {},
            topBarState = TopBarState(
                title = "",
                actions = listOf(
                    TopBarAction("select_all", R.string.sort_by, iconRes = R.drawable.ic_select_all),
                    TopBarAction("import", R.string.import_file, iconRes = R.drawable.ic_nv_import_white),
                    TopBarAction("sort", R.string.sort_by, iconRes = R.drawable.sort),
                ),
            ),
            bottomBarState = BottomBarState(
                selectedRoute = "parents",
                onTabSelected = {},
            ),
        )
    }
}
