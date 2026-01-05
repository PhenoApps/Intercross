package org.phenoapps.intercross.ui.theme

import android.content.SharedPreferences
import androidx.lifecycle.ViewModel
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import org.phenoapps.intercross.ui.theme.enums.AppTextType
import org.phenoapps.intercross.ui.theme.enums.AppThemeType
import javax.inject.Inject

/**
 * Provides reactive state theming updates (colors and text sizes) based on user preferences.
 */
@HiltViewModel
class ThemeViewModel @Inject constructor(
    private val prefs: SharedPreferences,
) : ViewModel() {

    private val _themeType = MutableStateFlow(getThemeType())
    val themeType: StateFlow<AppThemeType> = _themeType.asStateFlow()

    private val _textType = MutableStateFlow(getTextType())
    val textType: StateFlow<AppTextType> = _textType.asStateFlow()

    private fun getThemeType(): AppThemeType {
        // val themeIndex = prefs.getString(PreferenceKeys.THEME, "0")?.toInt() ?: 0
        // return when (themeIndex) {
        //     0 -> AppThemeType.Default
        //     else -> AppThemeType.Default
        // }
        return AppThemeType.Default
    }

    private fun getTextType(): AppTextType {
        // val textIndex = prefs.getString(PreferenceKeys.TEXT_THEME, "1")?.toInt() ?: 1
        // return AppTextType.entries.find { it.index == textIndex } ?: AppTextType.MEDIUM
        return AppTextType.MEDIUM
    }
}