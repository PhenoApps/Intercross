package org.phenoapps.intercross.util

import android.content.SharedPreferences
import androidx.core.content.edit

/**
 * Replaces the Settings Room entity. All cross ID generation settings are now in SharedPreferences.
 */
data class CrossIdSettings(
    val isPattern: Boolean = false,
    val isUUID: Boolean = true,
    val isAutoIncrement: Boolean = false,
    val pad: Int = 0,
    val number: Int = 1,
    val prefix: String = "",
    val suffix: String = "",
) {
    val pattern: String get() = prefix + number.toString().padStart(pad, '0') + suffix

    companion object {
        private const val KEY_IS_PATTERN = "crossid.isPattern"
        private const val KEY_IS_UUID = "crossid.isUUID"
        private const val KEY_IS_AUTO_INCREMENT = "crossid.isAutoIncrement"
        private const val KEY_PAD = "crossid.pad"
        private const val KEY_NUMBER = "crossid.number"
        private const val KEY_PREFIX = "crossid.prefix"
        private const val KEY_SUFFIX = "crossid.suffix"

        fun load(prefs: SharedPreferences): CrossIdSettings {
            return CrossIdSettings(
                isPattern = prefs.getBoolean(KEY_IS_PATTERN, false),
                isUUID = prefs.getBoolean(KEY_IS_UUID, true),
                isAutoIncrement = prefs.getBoolean(KEY_IS_AUTO_INCREMENT, false),
                pad = prefs.getInt(KEY_PAD, 0),
                number = prefs.getInt(KEY_NUMBER, 1),
                prefix = prefs.getString(KEY_PREFIX, "") ?: "",
                suffix = prefs.getString(KEY_SUFFIX, "") ?: "",
            )
        }

        fun save(prefs: SharedPreferences, settings: CrossIdSettings) {
            prefs.edit {
                putBoolean(KEY_IS_PATTERN, settings.isPattern)
                    .putBoolean(KEY_IS_UUID, settings.isUUID)
                    .putBoolean(KEY_IS_AUTO_INCREMENT, settings.isAutoIncrement)
                    .putInt(KEY_PAD, settings.pad)
                    .putInt(KEY_NUMBER, settings.number)
                    .putString(KEY_PREFIX, settings.prefix)
                    .putString(KEY_SUFFIX, settings.suffix)
            }
        }

        fun incrementNumber(prefs: SharedPreferences) {
            val current = prefs.getInt(KEY_NUMBER, 1)
            prefs.edit { putInt(KEY_NUMBER, current + 1) }
        }
    }
}
