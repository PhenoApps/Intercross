package org.phenoapps.intercross.ui.brapi

import android.content.Context
import android.net.ConnectivityManager
import android.net.NetworkCapabilities
import android.os.Bundle
import android.util.Log
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
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
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.RadioButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.pluralStringResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.foundation.layout.size
import com.google.gson.Gson
import org.brapi.v2.model.germ.BrAPICrossingProject
import org.phenoapps.intercross.R
import org.phenoapps.intercross.brapi.service.BrAPIService
import org.phenoapps.intercross.brapi.service.BrAPIServiceV2
import org.phenoapps.intercross.brapi.service.BrapiPaginationManager
import org.phenoapps.intercross.fragments.brapi.BrapiRequestException
import org.phenoapps.intercross.fragments.brapi.awaitCrossingProjects
import org.phenoapps.intercross.ui.app.BrapiDetailDestination
import org.phenoapps.intercross.ui.app.BrapiMode
import org.phenoapps.intercross.ui.app.TopBarState
import org.phenoapps.intercross.ui.preview.PreviewSampleData
import org.phenoapps.intercross.ui.theme.AppTheme
import org.phenoapps.intercross.ui.theme.IntercrossPreviewTheme
import androidx.compose.ui.tooling.preview.Preview

@Composable
fun BrapiProjectsRoute(
    mode: Int,
    onBack: () -> Unit,
    onOpenDetail: (destination: BrapiDetailDestination, args: Bundle) -> Unit,
    onShowMessage: (String) -> Unit,
) {
    val context = LocalContext.current
    val service = remember(context) { BrAPIServiceV2(context) }
    var page by remember { mutableIntStateOf(0) }
    var totalPages by remember { mutableIntStateOf(1) }
    var projects by remember { mutableStateOf(emptyList<BrAPICrossingProject>()) }
    var selectedProject by remember { mutableStateOf<BrAPICrossingProject?>(null) }
    var isLoading by remember { mutableStateOf(false) }
    val serverUrl = remember(context) { BrAPIService.getBrapiUrl(context) }

    val deviceOfflineWarning = stringResource(R.string.device_offline_warning)
    val brapiConfigureUrlWarning = stringResource(R.string.brapi_must_configure_url)
    val brapiNotFoundWarning = stringResource(R.string.brapi_not_found)
    val brapiSelectProjectWarning = stringResource(R.string.brapi_warning_select_crossing_project)

    LaunchedEffect(mode, page) {
        if (!isOnline(context)) {
            onShowMessage(deviceOfflineWarning)
            return@LaunchedEffect
        }
        if (!BrAPIService.hasValidBaseUrl(context)) {
            onShowMessage(brapiConfigureUrlWarning)
            return@LaunchedEffect
        }
        isLoading = true
        try {
            val pagination = BrapiPaginationManager(page, DEFAULT_BRAPI_PROJECT_PAGE_SIZE)
            val loaded = service.awaitCrossingProjects(pagination)
                .filter { it.crossingProjectName?.isNotEmpty() == true && it.crossingProjectDbId != null }
            projects = loaded
            totalPages = pagination.totalPages.coerceAtLeast(1)
            selectedProject = selectedProject?.let { selected ->
                loaded.firstOrNull { it.crossingProjectDbId == selected.crossingProjectDbId }
            }
        } catch (error: BrapiRequestException) {
            Log.e("BrAPI", "Brapi request failed", error)
            //onShowMessage("BrAPI callback failed. ${error.code}")
        } catch (error: Exception) {
            Log.e("BrAPI", "Brapi crossing project call failed", error)
            //onShowMessage(error.localizedMessage ?: brapiNotFoundWarning)
        } finally {
            isLoading = false
        }
    }

    val topBarState = TopBarState(
        titleRes = R.string.brapi_cross_projects_title,
        showBack = true,
        onBack = onBack
    )

    BrapiScaffold(topBarState = topBarState) { innerPadding ->
        BrapiProjectsScreen(
            modifier = Modifier.padding(innerPadding),
            serverUrl = serverUrl,
            isLoading = isLoading,
            projects = projects,
            selectedProject = selectedProject,
            onSelectProject = { selectedProject = it },
            page = page,
            totalPages = totalPages,
            onPageChange = { page = it },
            mode = mode,
            onAction = {
                val project = selectedProject
                if (project == null) {
                    onShowMessage(brapiSelectProjectWarning)
                } else {
                    onOpenDetail(brapiDestinationForMode(mode), project.toDetailArgs(mode))
                }
            }
        )
    }
}

@Composable
internal fun BrapiProjectsScreen(
    modifier: Modifier = Modifier,
    serverUrl: String,
    isLoading: Boolean,
    projects: List<BrAPICrossingProject>,
    selectedProject: BrAPICrossingProject?,
    onSelectProject: (BrAPICrossingProject) -> Unit,
    page: Int,
    totalPages: Int,
    onPageChange: (Int) -> Unit,
    mode: Int,
    onAction: () -> Unit,
) {
    Box(modifier) {
    LazyColumn(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(10.dp),
    ) {
        item {
            Text(serverUrl, style = MaterialTheme.typography.bodySmall, color = AppTheme.colors.text.secondary)
        }
        if (isLoading) {
            item {
                Row(
                    modifier = Modifier.fillMaxWidth().padding(vertical = 24.dp),
                    horizontalArrangement = Arrangement.Center,
                ) {
                    CircularProgressIndicator(color = AppTheme.colors.accent)
                }
            }
        }
        items(projects, key = { it.crossingProjectDbId ?: it.hashCode().toString() }) { project ->
            BrapiProjectCard(
                project = project,
                selected = project.crossingProjectDbId == selectedProject?.crossingProjectDbId,
                onClick = { onSelectProject(project) },
            )
        }
        item {
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                OutlinedButton(
                    enabled = page > 0 && !isLoading,
                    onClick = { onPageChange((page - 1).coerceAtLeast(0)) },
                    modifier = Modifier.weight(1f),
                ) {
                    Text(stringResource(R.string.action_previous))
                }
                Text(
                    text = "Page ${page + 1} of $totalPages",
                    modifier = Modifier.align(Alignment.CenterVertically),
                    style = MaterialTheme.typography.bodyMedium,
                )
                OutlinedButton(
                    enabled = page < totalPages - 1 && !isLoading,
                    onClick = { onPageChange((page + 1).coerceAtMost(totalPages - 1)) },
                    modifier = Modifier.weight(1f),
                ) {
                    Text(stringResource(R.string.next))
                }
            }
        }
        item {
            Button(
                modifier = Modifier.fillMaxWidth(),
                enabled = !isLoading && selectedProject != null,
                onClick = onAction,
                colors = ButtonDefaults.buttonColors(containerColor = AppTheme.colors.accent),
            ) {
                Text(
                    if (mode == BrapiMode.PARENTS) {
                        stringResource(R.string.brapi_review_parents)
                    } else {
                        stringResource(R.string.brapi_import_selected)
                    },
                )
            }
        }
    }
    }
}

@Composable
private fun BrapiProjectCard(
    project: BrAPICrossingProject,
    selected: Boolean,
    onClick: () -> Unit,
) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick),
        shape = androidx.compose.foundation.shape.RoundedCornerShape(8.dp),
        colors = CardDefaults.cardColors(
            containerColor = if (selected) AppTheme.colors.primaryTransparent.copy(alpha = 0.1f) else Color.White
        ),
        elevation = CardDefaults.cardElevation(),
        border = if (selected) androidx.compose.foundation.BorderStroke(2.dp, AppTheme.colors.primary) else null
    ) {
        Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(16.dp)) {
            Row(
                horizontalArrangement = Arrangement.spacedBy(12.dp),
                verticalAlignment = Alignment.Top,
            ) {
                RadioButton(
                    selected = selected, 
                    onClick = onClick,
                    colors = androidx.compose.material3.RadioButtonDefaults.colors(selectedColor = AppTheme.colors.primary)
                )
                Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Text(
                        text = project.crossingProjectName.orEmpty(),
                        style = MaterialTheme.typography.titleLarge,
                        fontWeight = FontWeight.Bold,
                        color = AppTheme.colors.text.primary,
                        maxLines = 2,
                        overflow = TextOverflow.Ellipsis,
                    )
                    Text(
                        text = project.crossingProjectDescription
                            ?: project.additionalInfo?.toString()
                            ?: stringResource(R.string.brapi_project_description_unavailable),
                        style = MaterialTheme.typography.bodyMedium,
                        color = AppTheme.colors.text.secondary,
                        maxLines = 2,
                        overflow = TextOverflow.Ellipsis,
                    )
                }
            }
            
            androidx.compose.foundation.layout.FlowRow(
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                AssistChip(
                    onClick = {},
                    label = { Text(project.crossingProjectDbId ?: stringResource(R.string.brapi_project_id_unavailable)) },
                    leadingIcon = {
                        Icon(
                            painter = painterResource(R.drawable.ic_tag_black_24dp),
                            contentDescription = null,
                            modifier = Modifier.size(16.dp)
                        )
                    },
                    colors = AssistChipDefaults.assistChipColors(
                        containerColor = AppTheme.colors.primaryTransparent,
                        labelColor = AppTheme.colors.text.primary,
                        leadingIconContentColor = AppTheme.colors.primary
                    ),
                    border = null
                )
                AssistChip(
                    onClick = {},
                    label = {
                        Text(
                            pluralStringResource(
                                R.plurals.brapi_parent_count,
                                project.potentialParents?.size ?: 0,
                                project.potentialParents?.size ?: 0,
                            ),
                        )
                    },
                    leadingIcon = {
                        Icon(
                            painter = painterResource(R.drawable.ic_nv_parents_tab),
                            contentDescription = null,
                            modifier = Modifier.size(16.dp)
                        )
                    },
                    colors = AssistChipDefaults.assistChipColors(
                        containerColor = AppTheme.colors.primaryTransparent,
                        labelColor = AppTheme.colors.text.primary,
                        leadingIconContentColor = AppTheme.colors.primary
                    ),
                    border = null
                )
                AssistChip(
                    onClick = {},
                    label = { Text(project.programName.orUnavailable()) },
                    leadingIcon = {
                        Icon(
                            painter = painterResource(R.drawable.ic_account_tree_black_24dp),
                            contentDescription = null,
                            modifier = Modifier.size(16.dp)
                        )
                    },
                    colors = AssistChipDefaults.assistChipColors(
                        containerColor = AppTheme.colors.primaryTransparent,
                        labelColor = AppTheme.colors.text.primary,
                        leadingIconContentColor = AppTheme.colors.primary
                    ),
                    border = null
                )
                AssistChip(
                    onClick = {},
                    label = { Text(project.commonCropName.orUnavailable()) },
                    leadingIcon = {
                        Icon(
                            painter = painterResource(R.drawable.ic_sprout),
                            contentDescription = null,
                            modifier = Modifier.size(16.dp)
                        )
                    },
                    colors = AssistChipDefaults.assistChipColors(
                        containerColor = AppTheme.colors.primaryTransparent,
                        labelColor = AppTheme.colors.text.primary,
                        leadingIconContentColor = AppTheme.colors.primary
                    ),
                    border = null
                )
            }
        }
    }
}

private fun brapiDestinationForMode(mode: Int): BrapiDetailDestination {
    return when (mode) {
        BrapiMode.PARENTS -> BrapiDetailDestination.PotentialParents
        BrapiMode.IMPORT_CROSSES -> BrapiDetailDestination.CrossImport
        BrapiMode.EXPORT_CROSSES -> BrapiDetailDestination.ExportSummary
        else -> BrapiDetailDestination.PlannedCrosses
    }
}

private fun BrAPICrossingProject.toDetailArgs(mode: Int): Bundle {
    return Bundle().apply {
        putString("crossingProjectDbId", crossingProjectDbId ?: "")
        putString("crossingProjectName", crossingProjectName ?: "")
        putString("crossingProjectDescription", crossingProjectDescription ?: "")
        putString("programName", programName ?: "")
        putString("commonCropName", commonCropName ?: "")
        putInt("parentCount", potentialParents?.size ?: 0)
        if (mode == BrapiMode.PARENTS) {
            putString("programDbId", programDbId ?: "")
            putStringArray(
                "potentialParentsJson",
                potentialParents
                    ?.mapNotNull { parent -> runCatching { Gson().toJson(parent) }.getOrNull() }
                    ?.toTypedArray()
                    ?: emptyArray(),
            )
        }
    }
}

private fun isOnline(context: Context): Boolean {
    val connectivityManager = context.getSystemService(Context.CONNECTIVITY_SERVICE) as ConnectivityManager
    val network = connectivityManager.activeNetwork ?: return false
    val capabilities = connectivityManager.getNetworkCapabilities(network) ?: return false
    return capabilities.hasCapability(NetworkCapabilities.NET_CAPABILITY_INTERNET)
}

private const val DEFAULT_BRAPI_PROJECT_PAGE_SIZE = 1000

// ─── Previews ───────────────────────────────────────────────────────────────────

@Preview(showBackground = true, name = "BrapiProjects - Populated")
@Composable
internal fun BrapiProjectsPopulatedPreview() {
    IntercrossPreviewTheme {
        BrapiProjectsScreen(
            serverUrl = "https://test-server.brapi.org",
            isLoading = false,
            projects = PreviewSampleData.brapiCrossingProjects,
            selectedProject = PreviewSampleData.brapiCrossingProjects.first(),
            onSelectProject = {},
            page = 0,
            totalPages = 1,
            onPageChange = {},
            mode = BrapiMode.IMPORT_CROSSES,
            onAction = {}
        )
    }
}

@Preview(showBackground = true, name = "BrapiProjects - Loading")
@Composable
internal fun BrapiProjectsLoadingPreview() {
    IntercrossPreviewTheme {
        BrapiProjectsScreen(
            serverUrl = "https://test-server.brapi.org",
            isLoading = true,
            projects = emptyList(),
            selectedProject = null,
            onSelectProject = {},
            page = 0,
            totalPages = 0,
            onPageChange = {},
            mode = BrapiMode.IMPORT_CROSSES,
            onAction = {}
        )
    }
}
