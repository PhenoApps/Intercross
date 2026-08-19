package org.phenoapps.intercross.ui.crosstracker

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.lifecycle.asFlow
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import org.phenoapps.intercross.R
import org.phenoapps.intercross.data.EventsRepository
import org.phenoapps.intercross.data.MetaValuesRepository
import org.phenoapps.intercross.data.MetadataRepository
import org.phenoapps.intercross.data.WishlistRepository
import org.phenoapps.intercross.data.viewmodels.EventListViewModel
import org.phenoapps.intercross.data.viewmodels.MetaValuesViewModel
import org.phenoapps.intercross.data.viewmodels.MetadataViewModel
import org.phenoapps.intercross.data.viewmodels.WishlistViewModel
import org.phenoapps.intercross.data.viewmodels.factory.EventsListViewModelFactory
import org.phenoapps.intercross.data.viewmodels.factory.MetaValuesViewModelFactory
import org.phenoapps.intercross.data.viewmodels.factory.MetadataViewModelFactory
import org.phenoapps.intercross.data.viewmodels.factory.WishlistViewModelFactory
import org.phenoapps.intercross.ui.app.TopBarAction
import org.phenoapps.intercross.ui.app.TopBarState
import org.phenoapps.intercross.ui.app.rememberDatabase
import org.phenoapps.intercross.ui.app.rememberPrefs
import org.phenoapps.intercross.ui.components.CrossListItem
import org.phenoapps.intercross.util.WishProgressColorUtil

@Composable
fun CrossBlockRoute(
    onOpenEvent: (Long) -> Unit,
    onCreateWishlist: () -> Unit,
    onMakeCross: (femaleId: String, maleId: String) -> Unit = { _, _ -> },
    onBack: () -> Unit = {},
) {
    val db = rememberDatabase()
    val wishModel: WishlistViewModel = viewModel(factory = WishlistViewModelFactory(WishlistRepository.getInstance(db.wishlistDao())))
    val eventsModel: EventListViewModel = viewModel(factory = EventsListViewModelFactory(EventsRepository.getInstance(db.eventsDao())))
    val metadataModel: MetadataViewModel = viewModel(factory = MetadataViewModelFactory(MetadataRepository.getInstance(db.metadataDao())))
    val metaValuesModel: MetaValuesViewModel = viewModel(factory = MetaValuesViewModelFactory(MetaValuesRepository.getInstance(db.metaValuesDao())))

    val (prefs, keyUtil) = rememberPrefs()
    val context = LocalContext.current
    val source = if (prefs.getBoolean(keyUtil.commutativeCrossingKey, false)) wishModel.commutativeWishes else wishModel.wishes
    val allWishesData by source.asFlow().collectAsStateWithLifecycle(initialValue = emptyList())
    val events by eventsModel.events.asFlow().collectAsStateWithLifecycle(initialValue = emptyList())
    val metadata by metadataModel.metadata.asFlow().collectAsStateWithLifecycle(initialValue = emptyList())
    val metaValues by metaValuesModel.metaValues.asFlow().collectAsStateWithLifecycle(initialValue = emptyList())

    var selectedCell by remember { mutableStateOf<CrossBlockCell?>(null) }
    var selectedWishType by remember { mutableStateOf("cross") }

    val correctedWishes = remember(allWishesData, events, metadata, metaValues) {
        allWishesData.calculateActualProgress(
            events = events,
            metadata = metadata,
            metaValues = metaValues,
            commutative = prefs.getBoolean(keyUtil.commutativeCrossingKey, false)
        )
    }

    val filteredWishes = remember(correctedWishes, selectedWishType) {
        correctedWishes.filter { it.wishType == selectedWishType }
    }

    val wishTypes = remember(allWishesData) {
        allWishesData.map { it.wishType }.distinct().sorted()
    }

    val matrix = remember(filteredWishes) {
        buildCrossBlockMatrix(filteredWishes) {
            WishProgressColorUtil().getProgressColor(context, it.wishProgress, it.wishMin, it.wishMax)
        }
    }

    val topBarState = TopBarState(
        titleRes = R.string.cross_block_label,
        showBack = true,
        onBack = onBack,
        actions = listOf(
            TopBarAction("crossblock_add_wish", R.string.add_wishlist_item, iconRes = R.drawable.ic_add_black_24dp, onClick = onCreateWishlist),
        ),
    )

    CrossBlockScreen(
        matrix = matrix,
        selectedWishType = selectedWishType,
        wishTypes = wishTypes,
        onWishTypeChange = { selectedWishType = it },
        onCellClick = { selectedCell = it },
        topBarState = topBarState,
    )

    selectedCell?.let { cell ->
        val matches = events.filter {
            it.femaleObsUnitDbId == cell.femaleId && it.maleObsUnitDbId == cell.maleId
        }
        AlertDialog(
            onDismissRequest = { selectedCell = null },
            title = { Text(stringResource(R.string.click_item_to_open_child)) },
            text = {
                if (matches.isEmpty()) {
                    Text(stringResource(R.string.no_child_exists))
                } else {
                    LazyColumn(
                        verticalArrangement = Arrangement.spacedBy(8.dp),
                        modifier = Modifier.heightIn(max = 400.dp),
                    ) {
                        items(matches.take(12), key = { it.id ?: it.eventDbId.hashCode().toLong() }) { event ->
                            CrossListItem(
                                event = event,
                                onClick = {
                                    selectedCell = null
                                    event.id?.let(onOpenEvent)
                                },
                            )
                        }
                    }
                }
            },
            confirmButton = {
                TextButton(onClick = { selectedCell = null }) {
                    Text(stringResource(R.string.dialog_ok))
                }
            },
            dismissButton = {
                TextButton(onClick = {
                    selectedCell = null
                    onMakeCross(cell.femaleId, cell.maleId)
                }) {
                    Text(stringResource(R.string.make_cross_option))
                }
            },
        )
    }
}
