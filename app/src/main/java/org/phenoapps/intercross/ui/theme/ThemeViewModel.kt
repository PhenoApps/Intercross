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

    private val prefsListener = SharedPreferences.OnSharedPreferenceChangeListener { _, key ->
        if (key == THEME_KEY) {
            _themeType.value = getThemeType()
        }
    }

    init {
        prefs.registerOnSharedPreferenceChangeListener(prefsListener)
    }

    override fun onCleared() {
        super.onCleared()
        prefs.unregisterOnSharedPreferenceChangeListener(prefsListener)
    }

    fun setThemeType(type: AppThemeType) {
        prefs.edit().putString(THEME_KEY, type.name).apply()
    }

    private fun getThemeType(): AppThemeType {
        val name = prefs.getString(THEME_KEY, AppThemeType.Default.name) ?: AppThemeType.Default.name
        return AppThemeType.entries.find { it.name == name } ?: AppThemeType.Default
    }

    private fun getTextType(): AppTextType {
        return AppTextType.MEDIUM
    }

    companion object {
        const val THEME_KEY = "org.phenoapps.intercross.APP_THEME"
    }
}
