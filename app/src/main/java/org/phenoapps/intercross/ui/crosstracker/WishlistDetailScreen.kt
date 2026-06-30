package org.phenoapps.intercross.ui.crosstracker

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import org.phenoapps.intercross.R
import org.phenoapps.intercross.data.models.Event
import org.phenoapps.intercross.data.models.Wishlist
import org.phenoapps.intercross.ui.app.TopBarState
import org.phenoapps.intercross.ui.components.CrossListItem
import org.phenoapps.intercross.ui.components.WishTypeIcon
import org.phenoapps.intercross.ui.preview.PreviewSampleData
import org.phenoapps.intercross.ui.theme.AppTheme
import org.phenoapps.intercross.ui.theme.IntercrossPreviewTheme
import androidx.compose.ui.tooling.preview.Preview

@Composable
fun WishlistDetailScreen(
    femaleName: String,
    maleName: String,
    wishes: List<Wishlist>,
    wishProgress: List<WishlistProgressItem> = emptyList(),
    crosses: List<Event>,
    onEditWish: (Wishlist) -> Unit,
    onDeleteWish: (Wishlist) -> Unit,
    onAddWish: (Wishlist) -> Unit,
    onMakeCross: () -> Unit,
    onOpenEvent: (Long) -> Unit,
    femaleId: String = "",
    maleId: String = "",
    typeOptions: List<String> = emptyList(),
    metaIcons: Map<String, String?> = emptyMap(),
    topBarState: TopBarState? = null,
) {
    var editingWish by remember { mutableStateOf<Wishlist?>(null) }
    var deleteConfirm by remember { mutableStateOf<Wishlist?>(null) }
    var showAddDialog by remember { mutableStateOf(false) }

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
    ) { innerPadding ->
    Column(Modifier.fillMaxSize().padding(innerPadding).padding(12.dp)) {
        // Header
        ElevatedCard(
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(12.dp),
            colors = CardDefaults.elevatedCardColors(containerColor = Color.White),
            elevation = CardDefaults.elevatedCardElevation(defaultElevation = 2.dp),
        ) {
            Row(
                modifier = Modifier.padding(16.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(8.dp),
            ) {
                Surface(shape = CircleShape, color = Color(0xFFE91E63).copy(alpha = 0.12f)) {
                    Text("♀", modifier = Modifier.padding(horizontal = 8.dp, vertical = 2.dp), color = Color(0xFFE91E63), fontWeight = FontWeight.Bold)
                }
                Text(femaleName, style = MaterialTheme.typography.titleMedium, modifier = Modifier.weight(1f))
                Text("×", color = MaterialTheme.colorScheme.onSurfaceVariant)
                Surface(shape = CircleShape, color = Color(0xFF2196F3).copy(alpha = 0.12f)) {
                    Text("♂", modifier = Modifier.padding(horizontal = 8.dp, vertical = 2.dp), color = Color(0xFF2196F3), fontWeight = FontWeight.Bold)
                }
                Text(maleName, style = MaterialTheme.typography.titleMedium, modifier = Modifier.weight(1f))
            }
        }

        Spacer(Modifier.height(12.dp))

        LazyColumn(
            modifier = Modifier.weight(1f),
            verticalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            // Wish types section
            item {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    Text(stringResource(R.string.wishlist_targets), style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.SemiBold)
                    FilledTonalButton(
                        onClick = { showAddDialog = true },
                        contentPadding = PaddingValues(horizontal = 12.dp, vertical = 4.dp),
                    ) {
                        Icon(Icons.Default.Add, contentDescription = null, modifier = Modifier.size(16.dp))
                        Spacer(Modifier.width(4.dp))
                        Text(stringResource(R.string.action_add), style = MaterialTheme.typography.labelMedium)
                    }
                }
            }
            if (wishes.isEmpty()) {
                item {
                    Text(
                        stringResource(R.string.no_wishlist_items),
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.padding(16.dp),
                    )
                }
            }
            items(wishes, key = { "wish_${it.id ?: it.wishType.hashCode()}" }) { wish ->
                val progress = wishProgress.find { it.wishType == wish.wishType }
                ElevatedCard(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(12.dp),
                    colors = CardDefaults.elevatedCardColors(containerColor = Color.White),
                    elevation = CardDefaults.elevatedCardElevation(defaultElevation = 2.dp),
                ) {
                    Column(Modifier.padding(12.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(8.dp),
                        ) {
                            WishTypeIcon(
                                wishType = wish.wishType,
                                customIcon = metaIcons[wish.wishType],
                                modifier = Modifier.size(24.dp),
                                textStyle = MaterialTheme.typography.titleLarge,
                            )
                            Column(Modifier.weight(1f)) {
                                Text(wish.wishType, style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Medium)
                                Text(
                                    "Min: ${wish.wishMin}  •  Max: ${wish.wishMax ?: "—"}",
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                                )
                            }
                            IconButton(onClick = { editingWish = wish }) {
                                Icon(Icons.Default.Edit, contentDescription = stringResource(R.string.action_edit), tint = MaterialTheme.colorScheme.primary)
                            }
                            IconButton(onClick = { deleteConfirm = wish }) {
                                Icon(Icons.Default.Delete, contentDescription = stringResource(R.string.delete), tint = MaterialTheme.colorScheme.error)
                            }
                        }

                        if (progress != null) {
                            WishProgressBar(wish = progress, icon = metaIcons[wish.wishType])
                        }
                    }
                }
            }

            // Crosses section
            item { Spacer(Modifier.height(8.dp)) }
            item {
                Text(stringResource(R.string.crosses_count, crosses.size), style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.SemiBold)
            }
            if (crosses.isEmpty()) {
                item {
                    Text(
                        stringResource(R.string.no_child_exists),
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.padding(16.dp),
                    )
                }
            }
            items(crosses, key = { "event_${it.id ?: it.eventDbId.hashCode()}" }) { event ->
                CrossListItem(event = event, onClick = { event.id?.let(onOpenEvent) })
            }
        }

        Spacer(Modifier.height(12.dp))

        // Make cross button
        Button(
            onClick = onMakeCross,
            modifier = Modifier.fillMaxWidth().height(48.dp),
            shape = RoundedCornerShape(12.dp),
            colors = ButtonDefaults.buttonColors(containerColor = AppTheme.colors.accent),
        ) {
            Icon(Icons.Default.Add, contentDescription = null, tint = Color.White, modifier = Modifier.size(20.dp))
            Spacer(Modifier.width(8.dp))
            Text(stringResource(R.string.make_cross_option), fontWeight = FontWeight.SemiBold, color = Color.White)
        }
    }

    // Edit dialog
    editingWish?.let { wish ->
        WishEditDialog(
            wish = wish,
            onDismiss = { editingWish = null },
            onSave = { updated ->
                onEditWish(updated)
                editingWish = null
            },
        )
    }

    // Delete confirmation
    deleteConfirm?.let { wish ->
        val isLast = wishes.size == 1
        AlertDialog(
            onDismissRequest = { deleteConfirm = null },
            title = {
                Text(
                    if (isLast) stringResource(R.string.delete_last_wish_title)
                    else stringResource(R.string.delete_wish_title)
                )
            },
            text = {
                Text(
                    if (isLast) stringResource(R.string.delete_last_wish_confirm)
                    else stringResource(R.string.delete_wish_confirm, wish.wishType)
                )
            },
            confirmButton = {
                TextButton(onClick = { onDeleteWish(wish); deleteConfirm = null }) {
                    Text(stringResource(R.string.delete), color = MaterialTheme.colorScheme.error)
                }
            },
            dismissButton = {
                TextButton(onClick = { deleteConfirm = null }) { Text(stringResource(R.string.dialog_cancel)) }
            },
        )
    }

    // Add new target dialog
    if (showAddDialog) {
        WishAddDialog(
            typeOptions = typeOptions,
            metaIcons = metaIcons,
            onDismiss = { showAddDialog = false },
            onSave = { type, min, max ->
                onAddWish(Wishlist(femaleId, maleId, femaleName, maleName, type, min, max))
                showAddDialog = false
            },
        )
    }
    } // Scaffold
}

@Composable
private fun WishEditDialog(
    wish: Wishlist,
    onDismiss: () -> Unit,
    onSave: (Wishlist) -> Unit,
) {
    var min by remember { mutableStateOf(wish.wishMin.toString()) }
    var max by remember { mutableStateOf(wish.wishMax?.toString().orEmpty()) }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(stringResource(R.string.edit_wish_type, wish.wishType)) },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                OutlinedTextField(
                    value = min,
                    onValueChange = { min = it.filter(Char::isDigit) },
                    label = { Text(stringResource(R.string.minimum)) },
                    singleLine = true,
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(10.dp),
                )
                OutlinedTextField(
                    value = max,
                    onValueChange = { max = it.filter(Char::isDigit) },
                    label = { Text(stringResource(R.string.maximum)) },
                    singleLine = true,
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(10.dp),
                )
            }
        },
        confirmButton = {
            TextButton(
                onClick = {
                    val newMin = min.toIntOrNull() ?: wish.wishMin
                    val newMax = max.toIntOrNull()
                    val updated = wish.copy(wishMin = newMin, wishMax = newMax)
                    updated.id = wish.id
                    onSave(updated)
                },
            ) { Text(stringResource(R.string.dialog_save)) }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) { Text(stringResource(R.string.dialog_cancel)) }
        },
    )
}

@OptIn(androidx.compose.material3.ExperimentalMaterial3Api::class)
@Composable
private fun WishAddDialog(
    typeOptions: List<String> = emptyList(),
    metaIcons: Map<String, String?> = emptyMap(),
    onDismiss: () -> Unit,
    onSave: (type: String, min: Int, max: Int?) -> Unit,
) {
    var type by remember { mutableStateOf("") }
    var min by remember { mutableStateOf("") }
    var max by remember { mutableStateOf("") }
    var error by remember { mutableStateOf<String?>(null) }
    var expanded by remember { mutableStateOf(false) }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(stringResource(R.string.add_wishlist_target)) },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                if (typeOptions.isNotEmpty()) {
                    ExposedDropdownMenuBox(
                        expanded = expanded,
                        onExpandedChange = { expanded = it },
                    ) {
                        OutlinedTextField(
                            value = type,
                            onValueChange = { type = it; error = null },
                            label = { Text(stringResource(R.string.wish_type_label)) },
                            singleLine = true,
                            modifier = Modifier.fillMaxWidth().menuAnchor(),
                            shape = RoundedCornerShape(10.dp),
                            readOnly = false,
                            trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = expanded) },
                            leadingIcon = {
                                if (type.isNotBlank()) {
                                    WishTypeIcon(
                                        wishType = type,
                                        customIcon = metaIcons[type],
                                        modifier = Modifier.size(20.dp),
                                    )
                                }
                            },
                        )
                        ExposedDropdownMenu(
                            expanded = expanded,
                            onDismissRequest = { expanded = false },
                        ) {
                            typeOptions.forEach { option ->
                                DropdownMenuItem(
                                    text = { Text(option) },
                                    leadingIcon = {
                                        WishTypeIcon(
                                            wishType = option,
                                            customIcon = metaIcons[option],
                                            modifier = Modifier.size(20.dp),
                                        )
                                    },
                                    onClick = {
                                        type = option
                                        expanded = false
                                        error = null
                                    },
                                )
                            }
                        }
                    }
                } else {
                    OutlinedTextField(
                        value = type,
                        onValueChange = { type = it; error = null },
                        label = { Text(stringResource(R.string.wish_type_label_hint)) },
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(10.dp),
                    )
                }
                OutlinedTextField(
                    value = min,
                    onValueChange = { min = it.filter(Char::isDigit); error = null },
                    label = { Text(stringResource(R.string.minimum)) },
                    singleLine = true,
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(10.dp),
                )
                OutlinedTextField(
                    value = max,
                    onValueChange = { max = it.filter(Char::isDigit) },
                    label = { Text(stringResource(R.string.maximum)) },
                    singleLine = true,
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(10.dp),
                )
                error?.let {
                    Text(it, color = MaterialTheme.colorScheme.error, style = MaterialTheme.typography.bodySmall)
                }
            }
        },
        confirmButton = {
            TextButton(
                onClick = {
                    val parsedMin = min.toIntOrNull()
                    when {
                        type.isBlank() -> error = "Type is required"
                        parsedMin == null || parsedMin <= 0 -> error = "Min must be greater than 0"
                        else -> onSave(type.trim(), parsedMin, max.toIntOrNull())
                    }
                },
            ) { Text(stringResource(R.string.dialog_save)) }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) { Text(stringResource(R.string.dialog_cancel)) }
        },
    )
}

/**
 * Internal wrapper exposing [WishEditDialog] for screenshot testing.
 * The original composable retains `private` visibility for production encapsulation.
 */
@Composable
internal fun WishEditDialogPreview(
    wish: Wishlist,
    onDismiss: () -> Unit = {},
    onSave: (Wishlist) -> Unit = {},
) {
    WishEditDialog(wish = wish, onDismiss = onDismiss, onSave = onSave)
}

// ─── Previews ───────────────────────────────────────────────────────────────────

@Preview(showBackground = true, name = "WishlistDetail - Populated")
@Composable
private fun WishlistDetailPopulatedPreview() {
    IntercrossPreviewTheme {
        WishlistDetailScreen(
            femaleName = "Honeycrisp",
            maleName = "Fuji",
            femaleId = "HC001",
            maleId = "FJ003",
            wishes = listOf(
                Wishlist("HC001", "FJ003", "Honeycrisp", "Fuji", "Cross", 5, 10).apply { id = 1L },
                Wishlist("HC001", "FJ003", "Honeycrisp", "Fuji", "Seeds", 3, 8).apply { id = 2L },
            ),
            wishProgress = listOf(
                WishlistProgressItem("Cross", min = 5, max = 10, progress = 7),
                WishlistProgressItem("Seeds", min = 3, max = 8, progress = 2),
            ),
            crosses = PreviewSampleData.events.take(2),
            onEditWish = {},
            onDeleteWish = {},
            onAddWish = {},
            onMakeCross = {},
            onOpenEvent = {},
            topBarState = TopBarState(titleRes = R.string.wishlist_detail_title, showBack = true),
        )
    }
}

@Preview(showBackground = true, name = "WishlistDetail - Empty")
@Composable
private fun WishlistDetailEmptyPreview() {
    IntercrossPreviewTheme {
        WishlistDetailScreen(
            femaleName = "Honeycrisp",
            maleName = "Fuji",
            femaleId = "HC001",
            maleId = "FJ003",
            wishes = emptyList(),
            wishProgress = emptyList(),
            crosses = emptyList(),
            onEditWish = {},
            onDeleteWish = {},
            onAddWish = {},
            onMakeCross = {},
            onOpenEvent = {},
            topBarState = TopBarState(titleRes = R.string.wishlist_detail_title, showBack = true),
        )
    }
}
