package org.phenoapps.intercross.ui.settings

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.core.content.edit
import org.phenoapps.intercross.R
import org.phenoapps.intercross.ui.app.TopBarState
import org.phenoapps.intercross.ui.app.rememberPrefs
import org.phenoapps.intercross.ui.theme.AppTheme

@Composable
fun LayoutSettingsRoute(
    onBack: () -> Unit = {},
) {
    val (prefs, keyUtil) = rememberPrefs()
    var showPersonInput by rememberBooleanPreference(
        prefs = prefs,
        key = keyUtil.profileShowPersonInputKey,
        defaultValue = false,
    )

    LayoutSettingsScreenContent(
        showPersonInput = showPersonInput,
        onShowPersonInputChange = { checked ->
            showPersonInput = checked
            prefs.edit { putBoolean(keyUtil.profileShowPersonInputKey, checked) }
        },
        onBack = onBack,
    )
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun LayoutSettingsScreenContent(
    showPersonInput: Boolean,
    onShowPersonInputChange: (Boolean) -> Unit,
    onBack: () -> Unit = {},
) {
    val topBarState = TopBarState(titleRes = R.string.prefs_layout_title, showBack = true, onBack = onBack)

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
    ) { innerPadding ->
    Box(Modifier.padding(innerPadding)) {
    LazyColumn(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(8.dp),
    ) {
        item {
            SettingsSwitchRow(
                titleRes = R.string.profile_show_person_input,
                iconRes = R.drawable.form_dropdown,
                checked = showPersonInput,
                onCheckedChange = onShowPersonInputChange,
            )
        }
    }
    } // Box
    } // Scaffold
}
