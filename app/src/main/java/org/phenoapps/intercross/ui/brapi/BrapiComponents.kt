package org.phenoapps.intercross.ui.brapi

import android.content.Context
import android.net.ConnectivityManager
import android.net.NetworkCapabilities
import android.os.Bundle
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.material3.AssistChip
import androidx.compose.material3.AssistChipDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.pluralStringResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.google.gson.GsonBuilder
import com.google.gson.JsonParser
import org.phenoapps.intercross.R
import org.phenoapps.intercross.brapi.service.BrAPIService

import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.ui.res.painterResource
import org.phenoapps.intercross.ui.app.TopBarState
import org.phenoapps.intercross.ui.theme.AppTheme

/**
 * Shared composables and utilities used across multiple BrAPI route screens.
 */

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun BrapiScaffold(
    topBarState: TopBarState,
    content: @Composable (androidx.compose.foundation.layout.PaddingValues) -> Unit
) {
    Scaffold(
        topBar = {
            TopAppBar(
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
            )
        },
        content = content
    )
}

@Composable
fun BrapiProjectSummaryCard(args: Bundle) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = androidx.compose.foundation.shape.RoundedCornerShape(8.dp),
        colors = CardDefaults.cardColors(containerColor = Color.White),
        elevation = CardDefaults.cardElevation(defaultElevation = 3.dp),
    ) {
        Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
            Text(
                text = args.getString("crossingProjectName").orUnavailable(),
                style = MaterialTheme.typography.titleLarge,
                fontWeight = FontWeight.Bold,
                color = AppTheme.colors.text.primary,
                maxLines = 2,
                overflow = TextOverflow.Ellipsis,
            )
            Text(
                text = args.getString("crossingProjectDescription")
                    ?.ifBlank { null }
                    ?: stringResource(R.string.brapi_project_description_unavailable),
                style = MaterialTheme.typography.bodyMedium,
                color = AppTheme.colors.text.secondary,
            )
            FlowRow(
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                AssistChip(
                    onClick = {},
                    label = { Text(args.getString("crossingProjectDbId").orUnavailable()) },
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
                    label = { Text(args.getString("programName").orUnavailable()) },
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
                    label = { Text(args.getString("commonCropName").orUnavailable()) },
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
                AssistChip(
                    onClick = {},
                    label = { Text(pluralStringResource(R.plurals.brapi_parent_count, args.getInt("parentCount", 0), args.getInt("parentCount", 0))) },
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
            }
        }
    }
}

@Composable
fun BrapiDetailList(
    args: Bundle,
    isLoading: Boolean,
    hasError: Boolean,
    itemCount: Int,
    countLabel: String,
    emptyLabel: String,
    onRetry: () -> Unit,
    content: androidx.compose.foundation.lazy.LazyListScope.() -> Unit,
) {
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
                text = countLabel, 
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.SemiBold,
                color = AppTheme.colors.text.primary,
                modifier = Modifier.padding(top = 8.dp)
            )
        }
        if (isLoading) {
            item {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(vertical = 24.dp),
                    horizontalArrangement = Arrangement.Center,
                ) {
                    CircularProgressIndicator(color = AppTheme.colors.accent)
                }
            }
        }
        if (hasError) {
            item {
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.errorContainer),
                ) {
                    Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                        Text(
                            text = stringResource(R.string.brapi_planned_crosses_load_error),
                            color = MaterialTheme.colorScheme.onErrorContainer,
                        )
                        OutlinedButton(onClick = onRetry) {
                            Text(stringResource(R.string.retry))
                        }
                    }
                }
            }
        }
        if (!isLoading && !hasError) {
            content()
        }
        if (!isLoading && !hasError && itemCount == 0) {
            item {
                Text(text = emptyLabel, color = AppTheme.colors.text.secondary)
            }
        }
    }
}

@Composable
fun ParentPairText(
    femaleName: String,
    femaleId: String,
    maleName: String,
    maleId: String,
) {
    Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            Text(
                "♀", 
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold,
                color = Color(0xFFE91E63)
            )
            Column {
                Text(femaleName, style = MaterialTheme.typography.bodyMedium, color = AppTheme.colors.text.primary, fontWeight = FontWeight.SemiBold)
                Text(femaleId, style = MaterialTheme.typography.labelSmall, color = AppTheme.colors.text.secondary)
            }
        }
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            Text(
                "♂", 
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold,
                color = Color(0xFF2196F3)
            )
            Column {
                Text(maleName, style = MaterialTheme.typography.bodyMedium, color = AppTheme.colors.text.primary, fontWeight = FontWeight.SemiBold)
                Text(maleId, style = MaterialTheme.typography.labelSmall, color = AppTheme.colors.text.secondary)
            }
        }
    }
}

@Composable
fun ProgressButtonLabel(text: String) {
    Row(horizontalArrangement = Arrangement.spacedBy(8.dp), verticalAlignment = Alignment.CenterVertically) {
        CircularProgressIndicator(modifier = Modifier.size(18.dp), strokeWidth = 2.dp)
        Text(text)
    }
}

/**
 * Returns this string if non-null and non-blank, otherwise "Unknown".
 */
fun String?.orUnavailableText(): String = this?.ifBlank { null } ?: "Unknown"

/**
 * Composable-aware version that uses a string resource for the unavailable text.
 */
@Composable
fun String?.orUnavailable(): String {
    return this?.ifBlank { null } ?: stringResource(R.string.brapi_project_value_unavailable)
}

fun formatRawJson(rawJson: String): String {
    if (rawJson.isBlank()) return ""
    return runCatching {
        GsonBuilder().setPrettyPrinting().create().toJson(JsonParser.parseString(rawJson))
    }.getOrElse { rawJson }
}

fun canReachBrapi(context: Context, onShowMessage: (String) -> Unit): Boolean {
    if (!isOnlineForBrapi(context)) {
        onShowMessage(context.getString(R.string.device_offline_warning))
        return false
    }
    if (!BrAPIService.hasValidBaseUrl(context)) {
        onShowMessage(context.getString(R.string.brapi_must_configure_url))
        return false
    }
    return true
}

fun isOnlineForBrapi(context: Context): Boolean {
    val connectivityManager = context.getSystemService(Context.CONNECTIVITY_SERVICE) as ConnectivityManager
    val network = connectivityManager.activeNetwork ?: return false
    val capabilities = connectivityManager.getNetworkCapabilities(network) ?: return false
    return capabilities.hasCapability(NetworkCapabilities.NET_CAPABILITY_INTERNET)
}

// ─── Previews ───────────────────────────────────────────────────────────────────

@androidx.compose.ui.tooling.preview.Preview(showBackground = true, name = "BrapiProjectSummaryCard - Default")
@Composable
private fun BrapiProjectSummaryCardPreview() {
    org.phenoapps.intercross.ui.theme.IntercrossPreviewTheme {
        val args = Bundle().apply {
            putString("crossingProjectName", "Apple Breeding 2024")
            putString("crossingProjectDescription", "Developing new disease-resistant apple cultivars through controlled crosses")
            putString("crossingProjectDbId", "PRJ001")
            putString("programName", "Fruit Tree Improvement")
            putString("commonCropName", "Apple")
            putInt("parentCount", 12)
        }
        BrapiProjectSummaryCard(args = args)
    }
}

@androidx.compose.ui.tooling.preview.Preview(showBackground = true, name = "BrapiDetailList - Loading")
@Composable
private fun BrapiDetailListLoadingPreview() {
    org.phenoapps.intercross.ui.theme.IntercrossPreviewTheme {
        val args = Bundle().apply {
            putString("crossingProjectName", "Apple Breeding 2024")
            putString("crossingProjectDescription", "Developing new disease-resistant apple cultivars")
            putString("crossingProjectDbId", "PRJ001")
            putString("programName", "Fruit Tree Improvement")
            putString("commonCropName", "Apple")
            putInt("parentCount", 12)
        }
        BrapiDetailList(
            args = args,
            isLoading = true,
            hasError = false,
            itemCount = 0,
            countLabel = "Planned Crosses (0)",
            emptyLabel = "No planned crosses found.",
            onRetry = {},
        ) {}
    }
}

@androidx.compose.ui.tooling.preview.Preview(showBackground = true, name = "BrapiDetailList - Populated")
@Composable
private fun BrapiDetailListPopulatedPreview() {
    org.phenoapps.intercross.ui.theme.IntercrossPreviewTheme {
        val args = Bundle().apply {
            putString("crossingProjectName", "Apple Breeding 2024")
            putString("crossingProjectDescription", "Developing new disease-resistant apple cultivars")
            putString("crossingProjectDbId", "PRJ001")
            putString("programName", "Fruit Tree Improvement")
            putString("commonCropName", "Apple")
            putInt("parentCount", 12)
        }
        BrapiDetailList(
            args = args,
            isLoading = false,
            hasError = false,
            itemCount = 3,
            countLabel = "Planned Crosses (3)",
            emptyLabel = "No planned crosses found.",
            onRetry = {},
        ) {
            items(3) { index ->
                Card(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(vertical = 4.dp),
                ) {
                    Column(Modifier.padding(12.dp)) {
                        Text("Cross ${index + 1}: Honeycrisp × Fuji", style = MaterialTheme.typography.bodyLarge)
                        Text("Status: Pending", style = MaterialTheme.typography.bodySmall)
                    }
                }
            }
        }
    }
}

@androidx.compose.ui.tooling.preview.Preview(showBackground = true, name = "ParentPairText - Default")
@Composable
private fun ParentPairTextPreview() {
    org.phenoapps.intercross.ui.theme.IntercrossPreviewTheme {
        ParentPairText(
            femaleName = "Honeycrisp",
            femaleId = "HC001",
            maleName = "Fuji",
            maleId = "FJ003",
        )
    }
}

@androidx.compose.ui.tooling.preview.Preview(showBackground = true, name = "ProgressButtonLabel - Default")
@Composable
private fun ProgressButtonLabelPreview() {
    org.phenoapps.intercross.ui.theme.IntercrossPreviewTheme {
        ProgressButtonLabel(text = "Importing crosses…")
    }
}
