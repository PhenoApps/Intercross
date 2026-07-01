package org.phenoapps.intercross.ui.parents

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Check
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ElevatedCard
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import org.phenoapps.intercross.R
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.IconButton
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import org.phenoapps.intercross.ui.app.TopBarState
import org.phenoapps.intercross.ui.theme.AppTheme
import org.phenoapps.intercross.ui.theme.IntercrossPreviewTheme

@Composable
fun ParentCreatorScreen(
    mode: Int,
    code: String,
    name: String,
    bulk: Boolean,
    onCodeChange: (String) -> Unit,
    onNameChange: (String) -> Unit,
    onBulkChange: () -> Unit,
    onSave: () -> Unit,
    topBarState: TopBarState? = null,
) {
    val isFemale = mode == 0
    val accentColor = if (isFemale) Color(0xFFE91E63) else Color(0xFF2196F3)
    val sexSymbol = if (isFemale) "♀" else "♂"
    val title = if (isFemale) stringResource(R.string.add_female) else stringResource(R.string.add_male)

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
        modifier = Modifier
            .fillMaxSize()
            .padding(innerPadding)
            .padding(16.dp)
            .imePadding(),
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        // Header with sex indicator
        Spacer(Modifier.height(16.dp))
        Surface(
            shape = RoundedCornerShape(50),
            color = accentColor.copy(alpha = 0.12f),
            modifier = Modifier.size(64.dp),
        ) {
            Box(contentAlignment = Alignment.Center) {
                Text(
                    text = sexSymbol,
                    style = MaterialTheme.typography.headlineLarge,
                    color = accentColor,
                    fontWeight = FontWeight.Bold,
                )
            }
        }
        Spacer(Modifier.height(12.dp))
        Text(
            text = title,
            style = MaterialTheme.typography.titleLarge,
            fontWeight = FontWeight.SemiBold,
        )
        Spacer(Modifier.height(24.dp))

        // Form card
        ElevatedCard(
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(16.dp),
            colors = CardDefaults.elevatedCardColors(containerColor = Color.White),
            elevation = CardDefaults.elevatedCardElevation(defaultElevation = 4.dp),
        ) {
            Column(
                modifier = Modifier.padding(20.dp),
                verticalArrangement = Arrangement.spacedBy(16.dp),
            ) {
                OutlinedTextField(
                    value = code,
                    onValueChange = onCodeChange,
                    label = { Text(stringResource(R.string.Code)) },
                    placeholder = { Text(stringResource(R.string.placeholder_code_id)) },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(12.dp),
                )
                OutlinedTextField(
                    value = name,
                    onValueChange = onNameChange,
                    label = { Text(stringResource(R.string.Name)) },
                    placeholder = { Text(stringResource(R.string.placeholder_parent_name_hint)) },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(12.dp),
                )
                if (mode == 1) {
                    FilterChip(
                        selected = bulk,
                        onClick = onBulkChange,
                        label = { Text(stringResource(R.string.add_male_group)) },
                        leadingIcon = if (bulk) {
                            { Icon(Icons.Default.Check, contentDescription = null, modifier = Modifier.size(18.dp)) }
                        } else null,
                        colors = FilterChipDefaults.filterChipColors(
                            selectedContainerColor = accentColor.copy(alpha = 0.12f),
                            selectedLabelColor = accentColor,
                            selectedLeadingIconColor = accentColor,
                        ),
                    )
                }
            }
        }

        Spacer(Modifier.weight(1f))

        // Save button pinned to bottom
        Button(
            onClick = onSave,
            modifier = Modifier
                .fillMaxWidth()
                .height(52.dp),
            shape = RoundedCornerShape(12.dp),
            colors = ButtonDefaults.buttonColors(containerColor = accentColor),
            enabled = code.isNotBlank(),
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(8.dp),
            ) {
                Icon(
                    painter = painterResource(
                        if (isFemale) R.drawable.ic_female_black_24dp
                        else R.drawable.ic_male_black_24dp
                    ),
                    contentDescription = null,
                    tint = Color.White,
                    modifier = Modifier.size(20.dp),
                )
                Text(
                    text = title,
                    fontWeight = FontWeight.SemiBold,
                    color = Color.White,
                )
            }
        }
        Spacer(Modifier.height(8.dp))
    }
    } // Scaffold
}

// ─── Previews ───────────────────────────────────────────────────────────────────

@Preview(showBackground = true, name = "ParentCreatorScreen - Female")
@Composable
internal fun ParentCreatorScreenFemalePreview() {
    IntercrossPreviewTheme {
        ParentCreatorScreen(
            mode = 0,
            code = "HC001",
            name = "Honeycrisp",
            bulk = false,
            onCodeChange = {},
            onNameChange = {},
            onBulkChange = {},
            onSave = {},
            topBarState = TopBarState(
                titleRes = R.string.parent_creator_label,
                showBack = true,
            ),
        )
    }
}

@Preview(showBackground = true, name = "ParentCreatorScreen - Male")
@Composable
internal fun ParentCreatorScreenMalePreview() {
    IntercrossPreviewTheme {
        ParentCreatorScreen(
            mode = 1,
            code = "FJ003",
            name = "Fuji",
            bulk = false,
            onCodeChange = {},
            onNameChange = {},
            onBulkChange = {},
            onSave = {},
            topBarState = TopBarState(
                titleRes = R.string.parent_creator_label,
                showBack = true,
            ),
        )
    }
}

@Preview(showBackground = true, name = "ParentCreatorScreen - Male Bulk")
@Composable
internal fun ParentCreatorScreenMaleBulkPreview() {
    IntercrossPreviewTheme {
        ParentCreatorScreen(
            mode = 1,
            code = "PG001",
            name = "Spring Pollen Mix",
            bulk = true,
            onCodeChange = {},
            onNameChange = {},
            onBulkChange = {},
            onSave = {},
            topBarState = TopBarState(
                titleRes = R.string.parent_creator_label,
                showBack = true,
            ),
        )
    }
}

@Preview(showBackground = true, name = "ParentCreatorScreen - Empty")
@Composable
internal fun ParentCreatorScreenEmptyPreview() {
    IntercrossPreviewTheme {
        ParentCreatorScreen(
            mode = 0,
            code = "",
            name = "",
            bulk = false,
            onCodeChange = {},
            onNameChange = {},
            onBulkChange = {},
            onSave = {},
            topBarState = TopBarState(
                titleRes = R.string.parent_creator_label,
                showBack = true,
            ),
        )
    }
}
