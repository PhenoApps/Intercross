package org.phenoapps.intercross.ui.brapi

import android.os.Bundle
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.AssistChip
import androidx.compose.material3.AssistChipDefaults
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.pluralStringResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.google.gson.Gson
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import org.brapi.v2.model.germ.BrAPICrossParent
import org.phenoapps.intercross.R
import org.phenoapps.intercross.data.ParentsRepository
import androidx.compose.foundation.layout.Box
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.ui.res.painterResource
import org.phenoapps.intercross.ui.app.TopBarState
import org.phenoapps.intercross.ui.components.ParentListItem
import org.phenoapps.intercross.data.models.Parent
import org.phenoapps.intercross.ui.app.rememberDatabase
import org.phenoapps.intercross.ui.theme.AppTheme

import org.phenoapps.intercross.ui.preview.PreviewSampleData
import org.phenoapps.intercross.ui.theme.IntercrossPreviewTheme
import androidx.compose.ui.tooling.preview.Preview

@Composable
fun BrapiPotentialParentsRoute(
    args: Bundle,
    onBack: () -> Unit,
    onDone: () -> Unit,
    onShowMessage: (String) -> Unit,
) {
    val context = LocalContext.current
    val db = rememberDatabase()
    val parentsRepository = remember(db) { ParentsRepository.getInstance(db.parentsDao()) }
    val scope = rememberCoroutineScope()
    val gson = remember { Gson() }
    val potentialParents = remember(args) {
        (args.getStringArray("potentialParentsJson") ?: emptyArray())
            .mapNotNull { encoded -> runCatching { gson.fromJson(encoded, BrAPICrossParent::class.java) }.getOrNull() }
            .distinctBy { it.observationUnitDbId ?: it.observationUnitName }
    }
    var expandedParentIds by remember { mutableStateOf(emptySet<String>()) }
    var selectedParentIds by remember { mutableStateOf(emptySet<String>()) }
    var isImporting by remember { mutableStateOf(false) }
    val noParentsMessage = stringResource(R.string.brapi_no_potential_parents_found)
    val importedParentsTemplate = stringResource(R.string.brapi_imported_parents, 0).replace("0", "%d")

    val topBarState = TopBarState(
        titleRes = R.string.brapi_potential_parents_title,
        showBack = true,
        onBack = onBack
    )

    BrapiScaffold(topBarState = topBarState) { innerPadding ->
        BrapiPotentialParentsScreen(
            modifier = Modifier.padding(innerPadding),
            args = args,
            potentialParents = potentialParents,
            expandedParentIds = expandedParentIds,
            selectedParentIds = selectedParentIds,
            onToggleExpanded = { stableId ->
                expandedParentIds = if (stableId in expandedParentIds) {
                    expandedParentIds - stableId
                } else {
                    expandedParentIds + stableId
                }
            },
            onToggleSelected = { stableId ->
                selectedParentIds = if (stableId in selectedParentIds) {
                    selectedParentIds - stableId
                } else {
                    selectedParentIds + stableId
                }
            },
            isImporting = isImporting,
            onImport = {
                if (potentialParents.isEmpty()) {
                    onShowMessage(noParentsMessage)
                    return@BrapiPotentialParentsScreen
                }
                isImporting = true
                scope.launch {
                    val imported = withContext(Dispatchers.IO) {
                        val parentsToImport = if (selectedParentIds.isEmpty()) {
                            potentialParents
                        } else {
                            potentialParents.filter { parent ->
                                val id = parent.observationUnitDbId ?: parent.observationUnitName.orEmpty()
                                id in selectedParentIds
                            }
                        }
                        val parents = parentsToImport
                            .groupBy { it.observationUnitDbId ?: it.observationUnitName.orEmpty() }
                            .filterKeys { it.isNotBlank() }
                            .map { (codeId, items) ->
                                val first = items.first()
                                val inferredSex = when {
                                    items.any { it.parentType?.toString() == "MALE" } -> 1
                                    items.any { it.parentType?.toString() == "FEMALE" } -> 0
                                    else -> 0
                                }
                                Parent(codeId = codeId, sex = inferredSex).also {
                                    it.name = first.observationUnitName ?: codeId
                                }
                            }

                        if (parents.isNotEmpty()) {
                            parentsRepository.insertIgnore(*parents.toTypedArray())
                            parentsRepository.updateName(*parents.toTypedArray())
                        }
                        parents.size
                    }
                    isImporting = false
                    onShowMessage(importedParentsTemplate.format(imported))
                    onDone()
                }
            }
        )
    }
}

@Composable
internal fun BrapiPotentialParentsScreen(
    modifier: Modifier = Modifier,
    args: Bundle,
    potentialParents: List<BrAPICrossParent>,
    expandedParentIds: Set<String>,
    selectedParentIds: Set<String>,
    onToggleExpanded: (String) -> Unit,
    onToggleSelected: (String) -> Unit,
    isImporting: Boolean,
    onImport: () -> Unit,
) {
    Box(modifier) {
    LazyColumn(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(10.dp),
    ) {
        item {
            BrapiProjectSummaryCard(args = args)
        }
        item {
            Text(
                text = pluralStringResource(R.plurals.brapi_parent_count, potentialParents.size, potentialParents.size),
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.SemiBold,
                color = AppTheme.colors.text.primary,
                modifier = Modifier.padding(top = 8.dp)
            )
        }
        if (potentialParents.isEmpty()) {
            item {
                Text(
                    text = stringResource(R.string.brapi_no_potential_parents_found),
                    color = AppTheme.colors.text.secondary,
                )
            }
        }
        items(potentialParents, key = { it.observationUnitDbId ?: it.observationUnitName.orEmpty() }) { parent ->
            val stableId = parent.observationUnitDbId ?: parent.observationUnitName.orEmpty()
            val inferredSex = if (parent.parentType?.toString() == "MALE") 1 else 0
            val isSelected = stableId in selectedParentIds
            
            Column {
                ParentListItem(
                    name = parent.observationUnitName ?: stringResource(R.string.brapi_project_value_unavailable),
                    codeId = parent.observationUnitDbId ?: stringResource(R.string.brapi_project_id_unavailable),
                    sex = inferredSex,
                    selected = isSelected,
                    modifier = Modifier.padding(vertical = 4.dp),
                    onToggleSelection = { onToggleSelected(stableId) }
                )
                
                if (stableId in expandedParentIds) {
                    val rawJson = remember(parent) { Gson().toJson(parent) }
                    Card(
                        modifier = Modifier.fillMaxWidth().padding(bottom = 8.dp),
                        colors = CardDefaults.cardColors(containerColor = AppTheme.colors.background),
                        border = androidx.compose.foundation.BorderStroke(0.5.dp, AppTheme.colors.surface.border)
                    ) {
                        Text(
                            text = formatRawJson(rawJson),
                            modifier = Modifier.padding(12.dp),
                            style = MaterialTheme.typography.bodySmall,
                            color = AppTheme.colors.text.secondary,
                            fontFamily = FontFamily.Monospace,
                        )
                    }
                }
            }
        }
        item {
            Button(
                modifier = Modifier.fillMaxWidth(),
                enabled = potentialParents.isNotEmpty() && !isImporting,
                onClick = onImport,
                colors = ButtonDefaults.buttonColors(containerColor = AppTheme.colors.accent),
            ) {
                if (isImporting) {
                    ProgressButtonLabel(text = stringResource(R.string.brapi_import_progress, 0, potentialParents.size))
                } else {
                    Text(stringResource(R.string.brapi_import_potential_parents))
                }
            }
        }
    }
    }
}

// ─── Previews ───────────────────────────────────────────────────────────────────

@Preview(showBackground = true, name = "BrapiPotentialParents - Populated")
@Composable
internal fun BrapiPotentialParentsPopulatedPreview() {
    IntercrossPreviewTheme {
        val args = Bundle().apply {
            putString("crossingProjectName", "Apple Breeding 2024")
            putString("crossingProjectDescription", "Developing disease-resistant cultivars")
            putString("crossingProjectDbId", "PRJ001")
            putString("programName", "Fruit Tree Improvement")
            putString("commonCropName", "Apple")
            putInt("parentCount", 3)
        }
        BrapiPotentialParentsScreen(
            args = args,
            potentialParents = PreviewSampleData.brapiPotentialParents,
            expandedParentIds = setOf("HC001"),
            selectedParentIds = setOf("HC001", "HC002"),
            onToggleExpanded = {},
            onToggleSelected = {},
            isImporting = false,
            onImport = {}
        )
    }
}

