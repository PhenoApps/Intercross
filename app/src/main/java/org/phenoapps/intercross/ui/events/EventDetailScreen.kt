package org.phenoapps.intercross.ui.events

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.onFocusChanged
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import org.phenoapps.intercross.R
import org.phenoapps.intercross.data.dao.EventsDao
import org.phenoapps.intercross.data.models.Event
import org.phenoapps.intercross.data.models.Meta
import org.phenoapps.intercross.data.models.MetadataValues
import org.phenoapps.intercross.data.models.WishlistView
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ExposedDropdownMenuBox
import androidx.compose.material3.ExposedDropdownMenuDefaults
import androidx.compose.material3.IconButton
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import org.phenoapps.intercross.ui.app.TopBarAction
import org.phenoapps.intercross.ui.app.TopBarState
import org.phenoapps.intercross.ui.components.CrossListItem
import org.phenoapps.intercross.ui.components.defaultWishTypeEmoji
import org.phenoapps.intercross.ui.preview.PreviewSampleData
import org.phenoapps.intercross.ui.theme.AppTheme
import org.phenoapps.intercross.ui.theme.IntercrossPreviewTheme

@Composable
internal fun EventDetailScreen(
    event: Event?,
    parents: EventsDao.ParentData?,
    metadata: List<EventsDao.CrossMetadataWithDefaults>,
    metaList: List<Meta>,
    metaValues: List<MetadataValues>,
    wishes: List<WishlistView>,
    allEvents: List<Event>,
    showMetadataInputs: Boolean,
    onShowMessage: (String) -> Unit,
    onSaveMetadata: (MetadataValues) -> Unit = {},
    onNavigateToEvent: (Long) -> Unit = {},
    topBarState: TopBarState? = null,
) {
    val parentEventNotExistText = stringResource(R.string.parent_event_does_not_exist)
    val minMaxWishMetTemplate = stringResource(R.string.minimum_maximum_wish_met, "%s")
    val maxWishMetTemplate = stringResource(R.string.maximum_wish_met, "%s")
    val minWishMetTemplate = stringResource(R.string.minimum_wish_met, "%s")

    var isEnteringMetadataValue by remember { mutableStateOf(false) }

    LaunchedEffect(event?.id, showMetadataInputs) {
        if (event == null || !showMetadataInputs) {
            isEnteringMetadataValue = false
        }
    }

    androidx.compose.material3.Scaffold(
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
                        topBarState.actions
                            .filterNot { action ->
                                action.id == "print_event" && (event == null || isEnteringMetadataValue)
                            }
                            .forEach { action ->
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
    ) { innerPadding ->
    Box(Modifier.fillMaxSize().padding(innerPadding).imePadding()) {
        LazyColumn(
            Modifier.fillMaxSize().padding(12.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            event?.let { current ->
                // Cross label item
                item {
                    CrossListItem(event = current, showQrCode = true)
                }

                // Parental navigation section
                item {
                    val momName = parents?.momReadableName.orEmpty()
                    val dadName = parents?.dadReadableName.orEmpty()
                    val momCode = parents?.momCode.orEmpty()
                    val dadCode = parents?.dadCode.orEmpty()

                    androidx.compose.material3.ElevatedCard(
                        modifier = Modifier.fillMaxWidth(),
                        shape = androidx.compose.foundation.shape.RoundedCornerShape(12.dp),
                        colors = androidx.compose.material3.CardDefaults.elevatedCardColors(containerColor = Color.White),
                        elevation = androidx.compose.material3.CardDefaults.elevatedCardElevation(defaultElevation = 2.dp),
                    ) {
                        Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
                            Text(
                                text = if (momName.isBlank() && dadName.isBlank())
                                    stringResource(R.string.parents_dont_exist)
                                else
                                    stringResource(R.string.parental_navigation),
                                style = MaterialTheme.typography.titleSmall,
                                fontWeight = FontWeight.SemiBold,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                            )

                            // Female parent button
                            if (momName.isNotBlank()) {
                                Row(
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.spacedBy(10.dp),
                                ) {
                                    Surface(
                                        shape = CircleShape,
                                        color = Color(0xFFE91E63).copy(alpha = 0.12f),
                                        modifier = Modifier.size(36.dp),
                                    ) {
                                        Box(contentAlignment = Alignment.Center, modifier = Modifier.fillMaxSize()) {
                                            Text("♀", fontWeight = FontWeight.Bold, color = Color(0xFFE91E63))
                                        }
                                    }
                                    Button(
                                        onClick = {
                                            val parentEvent = allEvents.find { it.eventDbId == momCode }
                                            if (parentEvent?.id != null) {
                                                onNavigateToEvent(parentEvent.id!!)
                                            } else {
                                                onShowMessage(parentEventNotExistText)
                                            }
                                        },
                                        modifier = Modifier.weight(1f),
                                        shape = androidx.compose.foundation.shape.RoundedCornerShape(10.dp),
                                        colors = ButtonDefaults.buttonColors(containerColor = Color(0xFFE91E63)),
                                    ) {
                                        Text(momName, maxLines = 1, overflow = TextOverflow.Ellipsis, color = Color.White)
                                    }
                                }
                            }

                            // Male parent button
                            if (dadName.isNotBlank()) {
                                Row(
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.spacedBy(10.dp),
                                ) {
                                    Surface(
                                        shape = CircleShape,
                                        color = Color(0xFF2196F3).copy(alpha = 0.12f),
                                        modifier = Modifier.size(36.dp),
                                    ) {
                                        Box(contentAlignment = Alignment.Center, modifier = Modifier.fillMaxSize()) {
                                            Text("♂", fontWeight = FontWeight.Bold, color = Color(0xFF2196F3))
                                        }
                                    }
                                    Button(
                                        onClick = {
                                            val parentEvent = allEvents.find { it.eventDbId == dadCode }
                                            if (parentEvent?.id != null) {
                                                onNavigateToEvent(parentEvent.id!!)
                                            } else {
                                                onShowMessage(parentEventNotExistText)
                                            }
                                        },
                                        modifier = Modifier.weight(1f),
                                        shape = androidx.compose.foundation.shape.RoundedCornerShape(10.dp),
                                        colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF2196F3)),
                                    ) {
                                        Text(dadName, maxLines = 1, overflow = TextOverflow.Ellipsis, color = Color.White)
                                    }
                                }
                            }
                        }
                    }
                }

                // Metadata section (visible based on collectAdditionalInfoKey preference)
                if (showMetadataInputs && metaList.isNotEmpty()) {
                    item {
                        Text(
                            stringResource(R.string.metaData),
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.SemiBold,
                            modifier = Modifier.padding(top = 4.dp),
                        )
                    }
                    item {
                        val relevantWishes = remember(current, wishes) {
                            wishes.filter { it.momId == current.femaleObsUnitDbId && it.dadId == current.maleObsUnitDbId }
                        }

                        MetadataEntryCard(
                            metaList = metaList,
                            metadata = metadata,
                            wishes = relevantWishes,
                            onValueEntryActiveChange = { isEnteringMetadataValue = it },
                            onSave = { prop, newValue ->
                                // Save metadata value (same logic as fragment's onMetadataUpdated)
                                val eid = current.id?.toInt() ?: return@MetadataEntryCard
                                val metaId = metaList.find { it.property == prop }?.id?.toInt() ?: return@MetadataEntryCard

                                val existingValues = metaValues.filter { it.eid == eid && it.metaId == metaId }
                                if (existingValues.isNotEmpty()) {
                                    onSaveMetadata(MetadataValues(eid, metaId, newValue, existingValues.first().id))
                                } else {
                                    onSaveMetadata(MetadataValues(eid, metaId, newValue))
                                }

                                // Check wishlist thresholds (same logic as fragment's checkWishlist)
                                newValue?.let { v ->
                                    val mom = current.femaleObsUnitDbId
                                    val dad = current.maleObsUnitDbId
                                    val relevantWishes = wishes.filter { it.momId == mom && it.dadId == dad }
                                    val propertyWishes = relevantWishes.filter { it.wishType == prop }
                                    propertyWishes.forEach { wish ->
                                        if (v >= wish.wishMax && wish.wishMax > 0) {
                                            onShowMessage(
                                                if (wish.wishMin == wish.wishMax) minMaxWishMetTemplate.replace("%s", prop)
                                                else maxWishMetTemplate.replace("%s", prop)
                                            )
                                        } else if (v >= wish.wishMin && wish.wishMin > 0) {
                                            onShowMessage(minWishMetTemplate.replace("%s", prop))
                                        }
                                    }
                                }
                            },
                        )
                    }

                    item {
                        MetadataSummaryCard(metadata = metadata, metaList = metaList)
                    }
                }
            } ?: item {
                Text(stringResource(R.string.no_crosses_text), modifier = Modifier.padding(24.dp))
            }
        }
    }
    } // Scaffold
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun MetadataEntryCard(
    metaList: List<Meta>,
    metadata: List<EventsDao.CrossMetadataWithDefaults>,
    wishes: List<WishlistView>,
    onValueEntryActiveChange: (Boolean) -> Unit = {},
    onSave: (String, Int?) -> Unit,
    modifier: Modifier = Modifier,
) {
    var expanded by remember { mutableStateOf(false) }
    var selectedMeta by remember(metaList) {
        mutableStateOf(metaList.firstOrNull())
    }

    val currentValueEntry = metadata.find { it.property == selectedMeta?.property }
    var editValue by remember(selectedMeta, currentValueEntry) {
        mutableStateOf(currentValueEntry?.value?.toString() ?: "")
    }

    val selectedWish = wishes.find { it.wishType == selectedMeta?.property }

    androidx.compose.material3.ElevatedCard(
        modifier = modifier.fillMaxWidth(),
        shape = androidx.compose.foundation.shape.RoundedCornerShape(12.dp),
        colors = androidx.compose.material3.CardDefaults.elevatedCardColors(containerColor = Color.White),
        elevation = androidx.compose.material3.CardDefaults.elevatedCardElevation(defaultElevation = 2.dp),
    ) {
        Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
            ExposedDropdownMenuBox(
                expanded = expanded,
                onExpandedChange = { expanded = !expanded },
                modifier = Modifier.fillMaxWidth()
            ) {
                OutlinedTextField(
                    value = selectedMeta?.property ?: "",
                    onValueChange = {},
                    readOnly = true,
                    label = { Text(stringResource(R.string.dialog_metadata_property_hint)) },
                    leadingIcon = selectedMeta?.let { meta ->
                        meta.icon?.takeIf { it.isNotBlank() } ?: defaultWishTypeEmoji(meta.property)
                    }?.let {
                        { Text(it, style = MaterialTheme.typography.titleMedium) }
                    },
                    trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = expanded) },
                    colors = ExposedDropdownMenuDefaults.outlinedTextFieldColors(),
                    modifier = Modifier.menuAnchor().fillMaxWidth(),
                    shape = androidx.compose.foundation.shape.RoundedCornerShape(10.dp),
                )

                ExposedDropdownMenu(
                    expanded = expanded,
                    onDismissRequest = { expanded = false }
                ) {
                    metaList.forEach { meta ->
                        val hasWish = wishes.any { it.wishType == meta.property }
                        val metaIcon = meta.icon?.takeIf { it.isNotBlank() } ?: defaultWishTypeEmoji(meta.property)
                        DropdownMenuItem(
                            text = {
                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    metaIcon?.let {
                                        Text(it, modifier = Modifier.padding(end = 8.dp))
                                    }
                                    Text(meta.property)
                                    if (hasWish) {
                                        Spacer(Modifier.weight(1f))
                                        Icon(
                                            painter = painterResource(R.drawable.ic_wishlist),
                                            contentDescription = null,
                                            modifier = Modifier.size(16.dp),
                                            tint = MaterialTheme.colorScheme.secondary
                                        )
                                    }
                                }
                            },
                            onClick = {
                                selectedMeta = meta
                                expanded = false
                            }
                        )
                    }
                }
            }

            if (selectedWish != null) {
                Row(
                    modifier = Modifier.fillMaxWidth().padding(horizontal = 4.dp),
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Icon(
                        painter = painterResource(R.drawable.ic_wishlist),
                        contentDescription = null,
                        modifier = Modifier.size(18.dp),
                        tint = MaterialTheme.colorScheme.secondary
                    )
                    Text(
                        text = stringResource(R.string.wish),
                        style = MaterialTheme.typography.labelMedium,
                        color = MaterialTheme.colorScheme.secondary,
                        fontWeight = FontWeight.Bold
                    )

                    WishInfoBadge(label = stringResource(R.string.minimum), value = selectedWish.wishMin)
                    WishInfoBadge(label = stringResource(R.string.wish_progress), value = selectedWish.wishProgress)
                    WishInfoBadge(label = stringResource(R.string.maximum), value = selectedWish.wishMax)
                }
            }

            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                OutlinedTextField(
                    value = editValue,
                    onValueChange = { editValue = it },
                    label = { Text(stringResource(R.string.metadata_edit_hint)) },
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                    modifier = Modifier
                        .weight(1f)
                        .onFocusChanged { onValueEntryActiveChange(it.isFocused) },
                    shape = androidx.compose.foundation.shape.RoundedCornerShape(10.dp),
                )

                Button(
                    onClick = {
                        selectedMeta?.property?.let { prop ->
                            onSave(prop, editValue.toIntOrNull())
                        }
                    },
                    shape = androidx.compose.foundation.shape.RoundedCornerShape(10.dp),
                ) {
                    Text(stringResource(R.string.save_text))
                }
            }
        }
    }
}

@Composable
private fun WishInfoBadge(label: String, value: Int) {
    Surface(
        color = MaterialTheme.colorScheme.secondaryContainer,
        shape = androidx.compose.foundation.shape.RoundedCornerShape(4.dp),
    ) {
        Row(
            modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(4.dp)
        ) {
            Text(
                text = label,
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.onSecondaryContainer
            )
            Text(
                text = value.toString(),
                style = MaterialTheme.typography.labelSmall,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.onSecondaryContainer
            )
        }
    }
}

@Composable
private fun MetadataSummaryCard(
    metadata: List<EventsDao.CrossMetadataWithDefaults>,
    metaList: List<Meta>,
    modifier: Modifier = Modifier,
) {
    val entriesWithValues = metadata.filter { it.value != null }
    if (entriesWithValues.isEmpty()) return

    androidx.compose.material3.ElevatedCard(
        modifier = modifier.fillMaxWidth(),
        shape = androidx.compose.foundation.shape.RoundedCornerShape(12.dp),
        colors = androidx.compose.material3.CardDefaults.elevatedCardColors(containerColor = Color.White),
        elevation = androidx.compose.material3.CardDefaults.elevatedCardElevation(defaultElevation = 2.dp),
    ) {
        Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
            Text(
                stringResource(R.string.summary),
                style = MaterialTheme.typography.titleSmall,
                fontWeight = FontWeight.SemiBold
            )
            entriesWithValues.forEach { entry ->
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.weight(1f)) {
                        val meta = metaList.find { it.property == entry.property }
                        val metaIcon = meta?.icon?.takeIf { it.isNotBlank() } ?: defaultWishTypeEmoji(entry.property)
                        if (metaIcon != null) {
                            Text(metaIcon, modifier = Modifier.padding(end = 8.dp))
                        }
                        Text(entry.property, style = MaterialTheme.typography.bodyMedium)
                    }
                    Text(entry.value.toString(), style = MaterialTheme.typography.bodyMedium, fontWeight = FontWeight.Bold)
                }
            }
        }
    }
}

@Preview(showBackground = true, name = "EventDetail - Populated")
@Composable
internal fun EventDetailScreenPopulatedPreview() {
    IntercrossPreviewTheme {
        EventDetailScreen(
            event = PreviewSampleData.events.first().copy(femaleObsUnitDbId = "HC001", maleObsUnitDbId = "FJ003"),
            parents = EventsDao.ParentData(
                momCode = "HC001",
                momReadableName = "Honeycrisp",
                dadCode = "FJ003",
                dadReadableName = "Fuji",
            ),
            metadata = listOf(
                EventsDao.CrossMetadataWithDefaults(
                    eid = 1,
                    property = "Seeds",
                    value = 5,
                    defaultValue = 0,
                ),
            ),
            metaList = listOf(
                Meta(property = "Seeds", defaultValue = 0, id = 1, icon = "🌱"),
                Meta(property = "Fruits", defaultValue = 0, id = 2, icon = "🍎"),
            ),
            metaValues = emptyList(),
            wishes = listOf(
                WishlistView(
                    momId = "HC001",
                    momName = "Honeycrisp",
                    dadId = "FJ003",
                    dadName = "Fuji",
                    wishMin = 10,
                    wishMax = 20,
                    wishType = "Seeds",
                    wishProgress = 10
                )
            ),
            allEvents = PreviewSampleData.events,
            showMetadataInputs = false,
            onShowMessage = {},
            topBarState = TopBarState(
                titleRes = R.string.event_detail_label,
                showBack = true,
                actions = listOf(
                    TopBarAction("toggle_metadata_edit", R.string.metaData, iconRes = R.drawable.ic_edit),
                    TopBarAction("delete_event", R.string.delete, iconRes = R.drawable.ic_menu_delete),
                ),
            ),
        )
    }
}

@Preview(showBackground = true, name = "EventDetail - Metadata Collection")
@Composable
internal fun MetadataCollectionPreview() {
    IntercrossPreviewTheme {
        EventDetailScreen(
            event = PreviewSampleData.events.first().copy(femaleObsUnitDbId = "HC001", maleObsUnitDbId = "FJ003"),
            parents = EventsDao.ParentData(
                momCode = "HC001",
                momReadableName = "Honeycrisp",
                dadCode = "FJ003",
                dadReadableName = "Fuji",
            ),
            metadata = listOf(
                EventsDao.CrossMetadataWithDefaults(
                    eid = 1,
                    property = "Seeds",
                    value = 5,
                    defaultValue = 0,
                ),
                EventsDao.CrossMetadataWithDefaults(
                    eid = 1,
                    property = "Fruits",
                    value = 2,
                    defaultValue = 0,
                ),
            ),
            metaList = listOf(
                Meta(property = "Seeds", defaultValue = 0, id = 1, icon = "🌱"),
                Meta(property = "Fruits", defaultValue = 0, id = 2, icon = "🍎"),
            ),
            metaValues = emptyList(),
            wishes = listOf(
                WishlistView("HC001", "Honeycrisp", "FJ003", "Fuji", 0, 10, "Seeds", 5),
                WishlistView("HC001", "Honeycrisp", "FJ003", "Fuji", 0, 5, "Fruits", 2)
            ),
            allEvents = PreviewSampleData.events,
            onShowMessage = {},
            topBarState = TopBarState(
                titleRes = R.string.event_detail_label,
                showBack = true,
                actions = listOf(
                    TopBarAction("toggle_metadata_edit", R.string.metaData, iconRes = R.drawable.ic_edit),
                ),
            ),
            showMetadataInputs = true,
            onSaveMetadata = {},
            onNavigateToEvent = {},
        )
    }
}
