package org.phenoapps.intercross.util

import android.content.SharedPreferences
import androidx.core.content.edit
import com.google.gson.Gson
import com.google.gson.reflect.TypeToken

/**
 * Simple persistence helper for saving and loading `LabelTemplateConfig` lists in
 * SharedPreferences. Templates are stored as JSON and are normalized with
 * `sanitizedForSave()` when loaded or saved.
 */
object LabelTemplateStore {
    private val gson = Gson()
    private val listType = object : TypeToken<List<LabelTemplateConfig>>() {}.type

    fun load(prefs: SharedPreferences, key: String): List<LabelTemplateConfig> {
        val json = prefs.getString(key, null).orEmpty()
        if (json.isBlank()) return emptyList()

        return runCatching {
            gson.fromJson<List<LabelTemplateConfig>>(json, listType).orEmpty()
        }.getOrDefault(emptyList())
            .map { it.sanitizedForSave() }
            .sortedBy { it.name.lowercase() }
    }

    fun save(prefs: SharedPreferences, key: String, templates: List<LabelTemplateConfig>) {
        prefs.edit {
            putString(key, gson.toJson(templates.map { it.sanitizedForSave() }))
        }
    }

    fun upsert(
        prefs: SharedPreferences,
        key: String,
        template: LabelTemplateConfig,
    ): List<LabelTemplateConfig> {
        val savedTemplate = template.sanitizedForSave()
        val updated = load(prefs, key)
            .filterNot {
                it.name.equals(savedTemplate.name, ignoreCase = true) && it.type == savedTemplate.type
            } + savedTemplate
        save(prefs, key, updated)
        return load(prefs, key)
    }
}
