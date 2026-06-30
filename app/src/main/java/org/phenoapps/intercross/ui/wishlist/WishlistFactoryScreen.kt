package org.phenoapps.intercross.ui.wishlist

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
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
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowForward
import androidx.compose.material.icons.filled.Check
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ElevatedCard
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.RadioButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.snapshots.SnapshotStateList
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import org.phenoapps.intercross.R
import org.phenoapps.intercross.data.models.Wishlist
import org.phenoapps.intercross.ui.components.WishTypeIcon
import org.phenoapps.intercross.ui.preview.PreviewSampleData
import org.phenoapps.intercross.ui.theme.AppTheme
import org.phenoapps.intercross.ui.theme.IntercrossPreviewTheme

internal data class WishParentOption(
    val codeId: String,
    val name: String,
)

internal data class WishDraft(
    val type: String,
    val min: String = "",
    val max: String = "",
)

@Composable
internal fun ParentChoiceStep(
    summary: String,
    options: List<WishParentOption>,
    selected: WishParentOption?,
    onSelected: (WishParentOption) -> Unit,
    onNext: () -> Unit,
    onBack: (() -> Unit)? = null,
) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp),
    ) {
        // Step header
        Text(
            text = summary,
            style = MaterialTheme.typography.titleLarge,
            fontWeight = FontWeight.SemiBold,
        )
        Text(
            text = "Select one parent from the list below",
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier.padding(top = 4.dp),
        )

        Spacer(Modifier.height(12.dp))

        LazyColumn(
            modifier = Modifier.weight(1f),
            verticalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            items(options) { option ->
                val isSelected = selected?.codeId == option.codeId
                ElevatedCard(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clickable { onSelected(option) },
                    shape = RoundedCornerShape(12.dp),
                    colors = CardDefaults.elevatedCardColors(containerColor = Color.White),
                    elevation = CardDefaults.elevatedCardElevation(defaultElevation = 2.dp),
                ) {
                    Row(
                        modifier = Modifier.padding(16.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(12.dp),
                    ) {
                        RadioButton(
                            selected = isSelected,
                            onClick = { onSelected(option) },
                        )
                        Column(Modifier.weight(1f)) {
                            Text(
                                option.name,
                                style = MaterialTheme.typography.titleMedium,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis,
                            )
                            Text(
                                option.codeId,
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                            )
                        }
                    }
                }
            }
        }

        Spacer(Modifier.height(12.dp))

        // Bottom buttons
        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            onBack?.let {
                OutlinedButton(
                    onClick = it,
                    modifier = Modifier.weight(1f),
                    shape = RoundedCornerShape(12.dp),
                ) {
                    Text(stringResource(R.string.dialog_back))
                }
            }
            Button(
                onClick = onNext,
                modifier = Modifier.weight(1f).height(48.dp),
                shape = RoundedCornerShape(12.dp),
                enabled = selected != null,
                colors = ButtonDefaults.buttonColors(containerColor = AppTheme.colors.primary),
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(6.dp),
                ) {
                    Text(stringResource(R.string.next), fontWeight = FontWeight.SemiBold)
                    Icon(Icons.Default.ArrowForward, contentDescription = null, modifier = Modifier.size(18.dp))
                }
            }
        }
    }
}

@Composable
internal fun WishValuesStep(
    female: WishParentOption,
    male: WishParentOption,
    drafts: SnapshotStateList<WishDraft>,
    onDraftChange: (WishDraft) -> Unit,
    onBack: () -> Unit,
    onReview: (List<Wishlist>) -> Unit,
    onShowMessage: (String) -> Unit,
    metaIcons: Map<String, String?> = emptyMap(),
) {
    val minErrorMessage = stringResource(R.string.frag_wf_values_min_must_not_be_null)
    val maxErrorMessage = stringResource(R.string.frag_wf_values_max_must_not_be_null)

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp),
    ) {
        // Header with parent pair info
        ElevatedCard(
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(12.dp),
            colors = CardDefaults.elevatedCardColors(containerColor = Color.White),
            elevation = CardDefaults.elevatedCardElevation(defaultElevation = 2.dp),
        ) {
            Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(4.dp)) {
                Text(
                    text = stringResource(R.string.frag_wf_choose_type_summary, female.name, male.name),
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.SemiBold,
                )
                Row(
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    Surface(shape = CircleShape, color = Color(0xFFE91E63).copy(alpha = 0.12f)) {
                        Text("♀", modifier = Modifier.padding(horizontal = 8.dp, vertical = 2.dp), color = Color(0xFFE91E63), fontWeight = FontWeight.Bold)
                    }
                    Text(female.name, style = MaterialTheme.typography.bodyMedium)
                    Text("×", style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
                    Surface(shape = CircleShape, color = Color(0xFF2196F3).copy(alpha = 0.12f)) {
                        Text("♂", modifier = Modifier.padding(horizontal = 8.dp, vertical = 2.dp), color = Color(0xFF2196F3), fontWeight = FontWeight.Bold)
                    }
                    Text(male.name, style = MaterialTheme.typography.bodyMedium)
                }
            }
        }

        Spacer(Modifier.height(12.dp))

        Text(
            text = stringResource(R.string.set_min_max_targets),
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )

        Spacer(Modifier.height(8.dp))

        LazyColumn(
            modifier = Modifier.weight(1f),
            verticalArrangement = Arrangement.spacedBy(10.dp),
        ) {
            items(drafts, key = { it.type }) { draft ->
                WishDraftRow(
                    draft = draft,
                    onChange = onDraftChange,
                    icon = metaIcons[draft.type]
                )
            }
        }

        Spacer(Modifier.height(12.dp))

        // Bottom buttons
        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            OutlinedButton(
                onClick = onBack,
                modifier = Modifier.weight(1f),
                shape = RoundedCornerShape(12.dp),
            ) {
                Text(stringResource(R.string.dialog_back))
            }
            Button(
                modifier = Modifier.weight(1f).height(48.dp),
                shape = RoundedCornerShape(12.dp),
                colors = ButtonDefaults.buttonColors(containerColor = AppTheme.colors.primary),
                onClick = {
                    val validWishes = mutableListOf<Wishlist>()
                    for (draft in drafts) {
                        val min = draft.min.toIntOrNull()
                        val max = draft.max.toIntOrNull()
                        when {
                            min == null && draft.max.isBlank() -> Unit
                            min == null || min <= 0 -> {
                                onShowMessage("${draft.type}: $minErrorMessage")
                                return@Button
                            }
                            max != null && max < min -> {
                                onShowMessage("${draft.type}: $maxErrorMessage")
                                return@Button
                            }
                            else -> validWishes += Wishlist(female.codeId, male.codeId, female.name, male.name, draft.type, min, max)
                        }
                    }
                    if (validWishes.isEmpty()) {
                        onShowMessage(minErrorMessage)
                        return@Button
                    }
                    onReview(validWishes)
                },
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(6.dp),
                ) {
                    Icon(Icons.Default.ArrowForward, contentDescription = null, modifier = Modifier.size(18.dp))
                    Text(stringResource(R.string.next), fontWeight = FontWeight.SemiBold)
                }
            }
        }
    }
}

@Composable
internal fun WishDraftRow(
    draft: WishDraft,
    onChange: (WishDraft) -> Unit,
    icon: String? = null,
) {
    val min = draft.min.toIntOrNull()
    val max = draft.max.toIntOrNull()
    val minError = draft.min.isNotBlank() && (min == null || min <= 0)
    val maxError = draft.max.isNotBlank() && min != null && max != null && max < min

    ElevatedCard(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(12.dp),
        colors = CardDefaults.elevatedCardColors(containerColor = Color.White),
        elevation = CardDefaults.elevatedCardElevation(defaultElevation = 2.dp),
    ) {
        Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(8.dp),
            ) {
                WishTypeIcon(
                    wishType = draft.type,
                    customIcon = icon,
                    modifier = Modifier.size(20.dp),
                )
                Text(
                    draft.type,
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Medium,
                )
            }
            Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                OutlinedTextField(
                    value = draft.min,
                    onValueChange = { onChange(draft.copy(min = it.filter(Char::isDigit))) },
                    label = { Text(stringResource(R.string.minimum)) },
                    placeholder = { Text(stringResource(R.string.placeholder_min)) },
                    isError = minError,
                    supportingText = {
                        if (minError) Text(stringResource(R.string.frag_wf_values_min_must_not_be_null))
                    },
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                    singleLine = true,
                    modifier = Modifier.weight(1f),
                    shape = RoundedCornerShape(10.dp),
                )
                OutlinedTextField(
                    value = draft.max,
                    onValueChange = { onChange(draft.copy(max = it.filter(Char::isDigit))) },
                    label = { Text(stringResource(R.string.maximum)) },
                    placeholder = { Text(stringResource(R.string.placeholder_max)) },
                    isError = maxError,
                    supportingText = {
                        if (maxError) Text(stringResource(R.string.frag_wf_values_max_must_not_be_null))
                    },
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                    singleLine = true,
                    modifier = Modifier.weight(1f),
                    shape = RoundedCornerShape(10.dp),
                )
            }
        }
    }
}

@Composable
internal fun WishSummaryStep(
    wishes: List<Wishlist>,
    femaleName: String,
    maleName: String,
    onBack: () -> Unit,
    onConfirm: () -> Unit,
    metaIcons: Map<String, String?> = emptyMap(),
) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp),
    ) {
        // Header
        Text(
            text = "Review Wishlist Items",
            style = MaterialTheme.typography.titleLarge,
            fontWeight = FontWeight.SemiBold,
        )
        Text(
            text = "${wishes.size} item(s) will be created",
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier.padding(top = 4.dp),
        )

        Spacer(Modifier.height(12.dp))

        // Parent pair card
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
                Text(femaleName, style = MaterialTheme.typography.bodyMedium)
                Text("×", color = MaterialTheme.colorScheme.onSurfaceVariant)
                Surface(shape = CircleShape, color = Color(0xFF2196F3).copy(alpha = 0.12f)) {
                    Text("♂", modifier = Modifier.padding(horizontal = 8.dp, vertical = 2.dp), color = Color(0xFF2196F3), fontWeight = FontWeight.Bold)
                }
                Text(maleName, style = MaterialTheme.typography.bodyMedium)
            }
        }

        Spacer(Modifier.height(12.dp))

        // Wish items list
        LazyColumn(
            modifier = Modifier.weight(1f),
            verticalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            items(wishes, key = { it.wishType }) { wish ->
                ElevatedCard(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(12.dp),
                    colors = CardDefaults.elevatedCardColors(containerColor = Color.White),
                    elevation = CardDefaults.elevatedCardElevation(defaultElevation = 2.dp),
                ) {
                    Row(
                        modifier = Modifier.padding(16.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(12.dp),
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
                        Icon(Icons.Default.Check, contentDescription = null, tint = Color(0xFF4CAF50), modifier = Modifier.size(20.dp))
                    }
                }
            }
        }

        Spacer(Modifier.height(12.dp))

        // Bottom buttons
        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            OutlinedButton(
                onClick = onBack,
                modifier = Modifier.weight(1f),
                shape = RoundedCornerShape(12.dp),
            ) {
                Text(stringResource(R.string.action_edit))
            }
            Button(
                onClick = onConfirm,
                modifier = Modifier.weight(1f).height(48.dp),
                shape = RoundedCornerShape(12.dp),
                colors = ButtonDefaults.buttonColors(containerColor = AppTheme.colors.primary),
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(6.dp),
                ) {
                    Icon(Icons.Default.Check, contentDescription = null, modifier = Modifier.size(18.dp))
                    Text(stringResource(R.string.action_confirm), fontWeight = FontWeight.SemiBold)
                }
            }
        }
    }
}

// ─── Previews ───────────────────────────────────────────────────────────────────

@Preview(showBackground = true, name = "ParentChoiceStep - Default")
@Composable
private fun ParentChoiceStepPreview() {
    IntercrossPreviewTheme {
        ParentChoiceStep(
            summary = "Choose female parent",
            options = PreviewSampleData.wishParentOptions,
            selected = PreviewSampleData.wishParentOptions.first(),
            onSelected = {},
            onNext = {},
            onBack = {},
        )
    }
}

@Preview(showBackground = true, name = "WishDraftRow - Valid")
@Composable
private fun WishDraftRowValidPreview() {
    IntercrossPreviewTheme {
        WishDraftRow(
            draft = WishDraft(type = "Cross", min = "5", max = "10"),
            onChange = {},
        )
    }
}

@Preview(showBackground = true, name = "WishDraftRow - Min Error")
@Composable
private fun WishDraftRowMinErrorPreview() {
    IntercrossPreviewTheme {
        WishDraftRow(
            draft = WishDraft(type = "Cross", min = "0", max = "10"),
            onChange = {},
        )
    }
}

@Preview(showBackground = true, name = "WishDraftRow - Max Error")
@Composable
private fun WishDraftRowMaxErrorPreview() {
    IntercrossPreviewTheme {
        WishDraftRow(
            draft = WishDraft(type = "Cross", min = "5", max = "3"),
            onChange = {},
        )
    }
}

@Preview(showBackground = true, name = "WishValuesStep - Populated")
@Composable
private fun WishValuesStepPreview() {
    IntercrossPreviewTheme {
        WishValuesStepPreviewContent(
            female = WishParentOption("HC001", "Honeycrisp"),
            male = WishParentOption("GA001", "Gala"),
        )
    }
}

@Composable
internal fun WishValuesStepPreviewContent(
    female: WishParentOption,
    male: WishParentOption,
) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp),
    ) {
        ElevatedCard(
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(12.dp),
            colors = CardDefaults.elevatedCardColors(containerColor = Color.White),
            elevation = CardDefaults.elevatedCardElevation(defaultElevation = 2.dp),
        ) {
            Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(4.dp)) {
                Text(
                    text = "Honeycrisp × Gala",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.SemiBold,
                )
                Row(
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    Surface(shape = CircleShape, color = Color(0xFFE91E63).copy(alpha = 0.12f)) {
                        Text("♀", modifier = Modifier.padding(horizontal = 8.dp, vertical = 2.dp), color = Color(0xFFE91E63), fontWeight = FontWeight.Bold)
                    }
                    Text(female.name, style = MaterialTheme.typography.bodyMedium)
                    Text("×", style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
                    Surface(shape = CircleShape, color = Color(0xFF2196F3).copy(alpha = 0.12f)) {
                        Text("♂", modifier = Modifier.padding(horizontal = 8.dp, vertical = 2.dp), color = Color(0xFF2196F3), fontWeight = FontWeight.Bold)
                    }
                    Text(male.name, style = MaterialTheme.typography.bodyMedium)
                }
            }
        }

        Spacer(Modifier.height(12.dp))

        Text(
            text = "Set min/max targets for each type",
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )

        Spacer(Modifier.height(8.dp))

        Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
            WishDraftRow(
                draft = WishDraft(type = "Cross", min = "5", max = "10"),
                onChange = {},
            )
            WishDraftRow(
                draft = WishDraft(type = "Seed", min = "3", max = "8"),
                onChange = {},
            )
        }

        Spacer(Modifier.height(12.dp))

        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            OutlinedButton(
                onClick = {},
                modifier = Modifier.weight(1f),
                shape = RoundedCornerShape(12.dp),
            ) {
                Text(stringResource(R.string.cancel))
            }
            Button(
                modifier = Modifier.weight(1f).height(48.dp),
                shape = RoundedCornerShape(12.dp),
                colors = ButtonDefaults.buttonColors(containerColor = AppTheme.colors.primary),
                onClick = {},
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(6.dp),
                ) {
                    Icon(Icons.Default.ArrowForward, contentDescription = null, modifier = Modifier.size(18.dp))
                    Text(stringResource(R.string.action_next), fontWeight = FontWeight.SemiBold)
                }
            }
        }
    }
}

@Preview(showBackground = true, name = "WishSummaryStep - Populated")
@Composable
private fun WishSummaryStepPreview() {
    IntercrossPreviewTheme {
        val wishes = listOf(
            org.phenoapps.intercross.data.models.Wishlist("HC001", "GA001", "Honeycrisp", "Gala", "Cross", 5, 10),
            org.phenoapps.intercross.data.models.Wishlist("HC001", "GA001", "Honeycrisp", "Gala", "Seed", 3, 8),
        )
        WishSummaryStep(
            wishes = wishes,
            femaleName = "Honeycrisp",
            maleName = "Gala",
            onBack = {},
            onConfirm = {},
        )
    }
}
