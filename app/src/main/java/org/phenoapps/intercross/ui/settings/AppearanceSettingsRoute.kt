package org.phenoapps.intercross.ui.settings

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import org.phenoapps.intercross.R
import androidx.compose.foundation.layout.Box
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.ui.res.painterResource
import org.phenoapps.intercross.ui.app.TopBarState
import org.phenoapps.intercross.ui.theme.AppTheme
import org.phenoapps.intercross.ui.theme.ThemeViewModel
import org.phenoapps.intercross.ui.theme.enums.AppThemeType

@Composable
fun AppearanceSettingsRoute(
    themeViewModel: ThemeViewModel = hiltViewModel(),
    onBack: () -> Unit = {},
) {
    val currentTheme by themeViewModel.themeType.collectAsState()

    val themeEntries = listOf(
        stringResource(R.string.appearance_theme_default),
        stringResource(R.string.appearance_theme_green),
        stringResource(R.string.appearance_theme_high_contrast),
        stringResource(R.string.appearance_theme_sky),
        stringResource(R.string.appearance_theme_autumn),
    )
    val themeValues = listOf(
        AppThemeType.Default.name,
        AppThemeType.Green.name,
        AppThemeType.HighContrast.name,
        AppThemeType.Sky.name,
        AppThemeType.Autumn.name,
    )

    AppearanceSettingsScreenContent(
        themeEntries = themeEntries,
        themeValues = themeValues,
        currentThemeName = currentTheme.name,
        onThemeSelected = { name ->
            val theme = AppThemeType.entries.find { it.name == name } ?: AppThemeType.Default
            themeViewModel.setThemeType(theme)
        },
        onBack = onBack,
    )
}

@androidx.compose.ui.tooling.preview.Preview(showBackground = true, name = "AppearanceSettings - Default")
@Composable
internal fun AppearanceSettingsPreview() {
    org.phenoapps.intercross.ui.theme.IntercrossPreviewTheme {
        AppearanceSettingsScreenContent(
            themeEntries = listOf("Default", "Nature (Green)", "High Contrast"),
            themeValues = listOf("Default", "Green", "HighContrast"),
            currentThemeName = "Default",
            onThemeSelected = {},
        )
    }
}

@androidx.compose.ui.tooling.preview.Preview(showBackground = true, name = "AppearanceSettings - Green")
@Composable
internal fun AppearanceSettingsGreenPreview() {
    org.phenoapps.intercross.ui.theme.IntercrossPreviewTheme {
        AppearanceSettingsScreenContent(
            themeEntries = listOf("Default", "Nature (Green)", "High Contrast"),
            themeValues = listOf("Default", "Green", "HighContrast"),
            currentThemeName = "Nature (Green)",
            onThemeSelected = {},
        )
    }
}

@androidx.compose.ui.tooling.preview.Preview(showBackground = true, name = "AppearanceSettings - High Contrast")
@Composable
internal fun AppearanceSettingsHighContrastPreview() {
    org.phenoapps.intercross.ui.theme.IntercrossPreviewTheme {
        AppearanceSettingsScreenContent(
            themeEntries = listOf("Default", "Nature (Green)", "High Contrast"),
            themeValues = listOf("Default", "Green", "HighContrast"),
            currentThemeName = "HighContrast",
            onThemeSelected = {},
        )
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AppearanceSettingsScreenContent(
    themeEntries: List<String>,
    themeValues: List<String>,
    currentThemeName: String,
    onThemeSelected: (String) -> Unit,
    onBack: () -> Unit = {},
) {
    val currentIndex = themeValues.indexOf(currentThemeName).coerceAtLeast(0)

    val topBarState = TopBarState(titleRes = R.string.prefs_appearance_title, showBack = true, onBack = onBack)

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
            SettingsSingleChoiceRow(
                titleRes = R.string.appearance_theme_title,
                summary = themeEntries[currentIndex],
                iconRes = R.drawable.ic_pref_appearance,
                entries = themeEntries,
                values = themeValues,
                selectedValue = currentThemeName,
                onSelected = onThemeSelected,
            )
        }
    }
    } // Box
    } // Scaffold
}
