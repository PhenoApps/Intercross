package org.phenoapps.intercross.ui.crosstracker

import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.core.content.edit
import androidx.lifecycle.asFlow
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import org.phenoapps.intercross.data.EventsRepository
import org.phenoapps.intercross.data.MetaValuesRepository
import org.phenoapps.intercross.data.MetadataRepository
import org.phenoapps.intercross.data.ParentsRepository
import org.phenoapps.intercross.data.WishlistRepository
import org.phenoapps.intercross.data.viewmodels.EventListViewModel
import org.phenoapps.intercross.data.viewmodels.MetaValuesViewModel
import org.phenoapps.intercross.data.viewmodels.MetadataViewModel
import org.phenoapps.intercross.data.viewmodels.ParentsListViewModel
import org.phenoapps.intercross.data.viewmodels.WishlistViewModel
import org.phenoapps.intercross.data.viewmodels.factory.EventsListViewModelFactory
import org.phenoapps.intercross.data.viewmodels.factory.MetaValuesViewModelFactory
import org.phenoapps.intercross.data.viewmodels.factory.MetadataViewModelFactory
import org.phenoapps.intercross.data.viewmodels.factory.ParentsListViewModelFactory
import org.phenoapps.intercross.data.viewmodels.factory.WishlistViewModelFactory
import org.phenoapps.intercross.ui.app.TopBarAction
import org.phenoapps.intercross.ui.app.TopBarState
import org.phenoapps.intercross.ui.app.BottomBarState
import org.phenoapps.intercross.ui.app.rememberDatabase
import org.phenoapps.intercross.ui.app.rememberPrefs
import androidx.compose.ui.res.stringResource
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.foundation.layout.Column
import androidx.compose.ui.Modifier
import androidx.compose.foundation.layout.fillMaxWidth
import org.phenoapps.intercross.R

@Composable
fun CrossTrackerRoute(
    bottomBarState: BottomBarState? = null,
    onOpenEvent: (Long) -> Unit,
    onOpenCrossBlock: () -> Unit,
    onCreateWishlist: () -> Unit,
    onImportLocal: () -> Unit = {},
    onImportBrapi: () -> Unit = {},
    onMakeCross: (femaleId: String, maleId: String) -> Unit = { _, _ -> },
    onOpenWishlistDetail: (femaleId: String, maleId: String, femaleName: String, maleName: String) -> Unit = { _, _, _, _ -> },
) {
    val db = rememberDatabase()
    val eventsModel: EventListViewModel = viewModel(factory = EventsListViewModelFactory(EventsRepository.getInstance(db.eventsDao())))
    val wishModel: WishlistViewModel = viewModel(factory = WishlistViewModelFactory(WishlistRepository.getInstance(db.wishlistDao())))
    val parentsModel: ParentsListViewModel = viewModel(factory = ParentsListViewModelFactory(ParentsRepository.getInstance(db.parentsDao())))
    val metadataModel: MetadataViewModel = viewModel(factory = MetadataViewModelFactory(MetadataRepository.getInstance(db.metadataDao())))
    val metaValuesModel: MetaValuesViewModel = viewModel(factory = MetaValuesViewModelFactory(MetaValuesRepository.getInstance(db.metaValuesDao())))
    val (prefs, keyUtil) = rememberPrefs()
    var filter by remember { mutableStateOf(CrossFilter.ALL) }
    var hideCompleted by remember { mutableStateOf(!prefs.getBoolean(keyUtil.showCompletedWishlistItems, true)) }
    var showImportDialog by remember { mutableStateOf(false) }

    val brapiEnabled = prefs.getBoolean(keyUtil.brapiEnabled, false)

    val crosses by eventsModel.allParents.asFlow().collectAsStateWithLifecycle(initialValue = emptyList())
    val events by eventsModel.events.asFlow().collectAsStateWithLifecycle(initialValue = emptyList())
    val wishesSource = if (prefs.getBoolean(keyUtil.commutativeCrossingKey, false)) wishModel.commutativeWishes else wishModel.wishes
    val wishes by wishesSource.asFlow().collectAsStateWithLifecycle(initialValue = emptyList())
    val parents by parentsModel.parents.asFlow().collectAsStateWithLifecycle(initialValue = emptyList())
    val metadata by metadataModel.metadata.asFlow().collectAsStateWithLifecycle(initialValue = emptyList())

    val metaIcons = remember(metadata) {
        metadata.associate { it.property to it.icon }
    }

    val metaValues by metaValuesModel.metaValues.asFlow().collectAsStateWithLifecycle(initialValue = emptyList())
    val rows = remember(crosses, wishes, parents, events, metadata, metaValues, filter, hideCompleted) {
        buildCrossTrackerRows(
            crosses = crosses,
            wishes = wishes,
            parents = parents,
            events = events,
            metaValues = metaValues,
            metadata = metadata,
            filter = filter,
            commutative = prefs.getBoolean(keyUtil.commutativeCrossingKey, false),
            showCompleted = !hideCompleted,
            crossWishType = "cross",
        )
    }

    val hideCompletedActionLabel = if (hideCompleted) {
        R.string.crosses_toolbar_show_completed
    } else {
        R.string.crosses_toolbar_hide_completed
    }
    val hideCompletedActionIcon = if (hideCompleted) {
        R.drawable.ic_show_wishlist_completed
    } else {
        R.drawable.ic_hide_wishlist_completed
    }

    val topBarState = TopBarState(
        title = "",
        actions = listOf(
            TopBarAction("cross_block", R.string.crossblock, iconRes = R.drawable.ic_grid_view_black_24dp, onClick = onOpenCrossBlock),
            TopBarAction("import_wishlist", R.string.import_file, iconRes = R.drawable.ic_nv_import_white, onClick = {
                if (brapiEnabled) {
                    showImportDialog = true
                } else {
                    onImportLocal()
                }
            }),
            TopBarAction("hide_completed", hideCompletedActionLabel, iconRes = hideCompletedActionIcon, onClick = {
                hideCompleted = !hideCompleted
                prefs.edit { putBoolean(keyUtil.showCompletedWishlistItems, !hideCompleted) }
            }),
        ),
    )

    if (showImportDialog) {
        AlertDialog(
            onDismissRequest = { showImportDialog = false },
            title = { Text(stringResource(R.string.import_file)) },
            text = {
                Column {
                    TextButton(
                        onClick = { showImportDialog = false; onImportLocal() },
                        modifier = Modifier.fillMaxWidth(),
                    ) {
                        Text(stringResource(R.string.import_source_local))
                    }
                    TextButton(
                        onClick = { showImportDialog = false; onImportBrapi() },
                        modifier = Modifier.fillMaxWidth(),
                    ) {
                        Text(stringResource(R.string.import_source_brapi))
                    }
                }
            },
            confirmButton = {},
        )
    }

    CrossTrackerScreen(
        rows = rows,
        filter = filter,
        onFilterChange = { filter = it },
        onOpenCrossBlock = onOpenCrossBlock,
        onCreateWishlist = onCreateWishlist,
        onRowClick = { row -> onOpenWishlistDetail(row.femaleId, row.maleId, row.female, row.male) },
        hideCompleted = hideCompleted,
        onToggleHideCompleted = {
            hideCompleted = !hideCompleted
            prefs.edit { putBoolean(keyUtil.showCompletedWishlistItems, !hideCompleted) }
        },
        metaIcons = metaIcons,
        topBarState = topBarState,
        bottomBarState = bottomBarState,
    )
}
