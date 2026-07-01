package org.phenoapps.intercross.ui.pollenmanager

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Checkbox
import androidx.compose.material3.CheckboxDefaults
import androidx.compose.material3.ElevatedCard
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import org.phenoapps.intercross.R
import org.phenoapps.intercross.data.models.Parent
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.ui.res.painterResource
import org.phenoapps.intercross.ui.app.TopBarAction
import org.phenoapps.intercross.ui.app.TopBarState
import org.phenoapps.intercross.ui.theme.AppTheme
import org.phenoapps.intercross.ui.theme.IntercrossPreviewTheme

@Composable
fun PollenManagerScreen(
    normalizedCode: String,
    normalizedName: String,
    males: List<Parent>,
    selectedMaleIds: Set<Long>,
    onSelectAll: () -> Unit,
    onClearAll: () -> Unit,
    onToggleMale: (Long) -> Unit,
    onSave: () -> Unit,
    topBarState: TopBarState? = null,
) {
    val selectedCount = selectedMaleIds.size
    val totalCount = males.distinctBy { it.codeId }.size

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
    Column(
        Modifier
            .fillMaxSize()
            .padding(innerPadding)
            .padding(16.dp),
    ) {
        // Header card with group info
        ElevatedCard(
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(16.dp),
            colors = CardDefaults.elevatedCardColors(containerColor = Color.White),
            elevation = CardDefaults.elevatedCardElevation(defaultElevation = 4.dp),
        ) {
            Column(Modifier.padding(20.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(12.dp),
                ) {
                    Surface(
                        shape = CircleShape,
                        color = Color(0xFF2196F3).copy(alpha = 0.12f),
                        modifier = Modifier.size(48.dp),
                    ) {
                        Box(contentAlignment = Alignment.Center) {
                            Icon(
                                painter = painterResource(R.drawable.sack_outline),
                                contentDescription = null,
                                modifier = Modifier.size(28.dp),
                                tint = Color(0xFF2196F3),
                            )
                        }
                    }
                    Column {
                        Text(
                            normalizedName,
                            style = MaterialTheme.typography.titleLarge,
                            fontWeight = FontWeight.SemiBold,
                        )
                        Text(
                            normalizedCode,
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                        )
                    }
                }
                // Selection counter
                Surface(
                    shape = RoundedCornerShape(8.dp),
                    color = AppTheme.colors.primary.copy(alpha = 0.08f),
                ) {
                    Text(
                        text = "$selectedCount of $totalCount males selected",
                        style = MaterialTheme.typography.labelMedium,
                        color = AppTheme.colors.primary,
                        fontWeight = FontWeight.Medium,
                        modifier = Modifier.padding(horizontal = 12.dp, vertical = 6.dp),
                    )
                }
            }
        }

        Spacer(Modifier.height(12.dp))

        // Male parent list
        LazyColumn(
            modifier = Modifier.weight(1f),
            verticalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            if (males.isEmpty()) {
                item {
                    Box(
                        modifier = Modifier.fillMaxWidth().padding(48.dp),
                        contentAlignment = Alignment.Center,
                    ) {
                        Text(
                            stringResource(R.string.parents_table_empty),
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                        )
                    }
                }
            }
            items(males.distinctBy { it.codeId }, key = { it.id ?: it.codeId.hashCode().toLong() }) { male ->
                val maleId = male.id
                val selected = maleId != null && maleId in selectedMaleIds
                ElevatedCard(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clickable(enabled = maleId != null) {
                            if (maleId != null) onToggleMale(maleId)
                        },
                    shape = RoundedCornerShape(12.dp),
                    colors = CardDefaults.elevatedCardColors(containerColor = Color.White),
                    elevation = CardDefaults.elevatedCardElevation(defaultElevation = 2.dp),
                ) {
                    Row(
                        Modifier.padding(horizontal = 12.dp, vertical = 14.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(12.dp),
                    ) {
                        Checkbox(
                            checked = selected,
                            onCheckedChange = { if (maleId != null) onToggleMale(maleId) },
                            colors = CheckboxDefaults.colors(
                                checkedColor = AppTheme.colors.primary,
                            ),
                        )
                        Column(Modifier.weight(1f)) {
                            Text(
                                male.name,
                                style = MaterialTheme.typography.titleMedium,
                            )
                            Text(
                                male.codeId,
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                            )
                        }
                    }
                }
            }
        }

        Spacer(Modifier.height(12.dp))

        // Save button
        Button(
            onClick = onSave,
            modifier = Modifier
                .fillMaxWidth()
                .height(52.dp),
            shape = RoundedCornerShape(12.dp),
            colors = ButtonDefaults.buttonColors(containerColor = AppTheme.colors.primary),
            enabled = selectedCount > 0,
        ) {
            Text(
                stringResource(R.string.dialog_save),
                fontWeight = FontWeight.SemiBold,
                color = Color.White,
            )
        }
    }
    } // Scaffold
}


// ─── Previews ────────────────────────────────────────────────────────────────

@Preview(showBackground = true, name = "PollenManagerScreen - Populated")
@Composable
internal fun PollenManagerScreenPopulatedPreview() {
    val males = listOf(
        Parent("FJ003", 1).apply { name = "Fuji"; id = 3L },
        Parent("GL004", 1).apply { name = "Gala"; id = 4L },
        Parent("GS005", 1).apply { name = "Granny Smith"; id = 5L },
    )
    IntercrossPreviewTheme {
        PollenManagerScreen(
            normalizedCode = "PG001",
            normalizedName = "Spring Pollen Mix",
            males = males,
            selectedMaleIds = setOf(3L),
            onSelectAll = {},
            onClearAll = {},
            onToggleMale = {},
            onSave = {},
            topBarState = TopBarState(
                titleRes = R.string.add_male_group,
                showBack = true,
                actions = listOf(
                    TopBarAction("pollen_select_all", R.string.SelectAllRows, iconRes = R.drawable.ic_select_all),
                ),
            ),
        )
    }
}
