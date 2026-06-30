package org.phenoapps.intercross.ui.crosstracker

import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.res.stringResource
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
import org.phenoapps.intercross.ui.app.TopBarState
import org.phenoapps.intercross.ui.app.rememberDatabase
import org.phenoapps.intercross.ui.app.rememberPrefs

@Composable
fun WishlistDetailRoute(
    femaleId: String,
    maleId: String,
    femaleName: String,
    maleName: String,
    onOpenEvent: (Long) -> Unit,
    onMakeCross: () -> Unit,
    onBack: () -> Unit,
) {
    val db = rememberDatabase()
    val (prefs, keyUtil) = rememberPrefs()
    val wishModel: WishlistViewModel = viewModel(factory = WishlistViewModelFactory(WishlistRepository.getInstance(db.wishlistDao())))
    val eventsModel: EventListViewModel = viewModel(factory = EventsListViewModelFactory(EventsRepository.getInstance(db.eventsDao())))
    val metadataModel: MetadataViewModel = viewModel(factory = MetadataViewModelFactory(MetadataRepository.getInstance(db.metadataDao())))
    val metaValuesModel: MetaValuesViewModel = viewModel(factory = MetaValuesViewModelFactory(MetaValuesRepository.getInstance(db.metaValuesDao())))

    val wishes by wishModel.getByParents(femaleId, maleId).asFlow().collectAsStateWithLifecycle(initialValue = emptyList())
    val events by eventsModel.events.asFlow().collectAsStateWithLifecycle(initialValue = emptyList())
    val metadata by metadataModel.metadata.asFlow().collectAsStateWithLifecycle(initialValue = emptyList())
    val metaValues by metaValuesModel.metaValues.asFlow().collectAsStateWithLifecycle(initialValue = emptyList())

    val crosses = events.filter {
        (it.femaleObsUnitDbId == femaleId && it.maleObsUnitDbId == maleId) ||
        (prefs.getBoolean(keyUtil.commutativeCrossingKey, false) && it.femaleObsUnitDbId == maleId && it.maleObsUnitDbId == femaleId)
    }
    val crossLabel = stringResource(R.string.literal_cross)
    val typeOptions = listOf(crossLabel) + metadata.map { it.property }

    val metaIcons = remember(metadata) {
        metadata.associate { it.property to it.icon }
    }

    val wishProgressItems = remember(wishes, crosses, metadata, metaValues) {
        val eventIds = crosses.mapNotNull { it.id?.toInt() }.toSet()
        wishes.map { wish ->
            val progress = progressForWishType(
                wishType = wish.wishType,
                crossWishType = "cross",
                count = crosses.size,
                eventIds = eventIds,
                metadata = metadata,
                metaValues = metaValues
            )
            WishlistProgressItem(wish.wishType, wish.wishMin, wish.wishMax ?: 0, progress)
        }
    }

    WishlistDetailScreen(
        femaleName = femaleName.ifBlank { femaleId },
        maleName = maleName.ifBlank { maleId },
        wishes = wishes,
        wishProgress = wishProgressItems,
        crosses = crosses,
        onEditWish = { wishModel.update(it) },
        onDeleteWish = { wishModel.delete(it) },
        onAddWish = { wishModel.insert(it) },
        onMakeCross = onMakeCross,
        onOpenEvent = onOpenEvent,
        femaleId = femaleId,
        maleId = maleId,
        typeOptions = typeOptions,
        metaIcons = metaIcons,
        topBarState = TopBarState(
            titleRes = R.string.wishlist_detail_title,
            showBack = true,
            onBack = onBack,
        ),
    )
}
