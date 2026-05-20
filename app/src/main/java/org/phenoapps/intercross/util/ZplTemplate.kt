package org.phenoapps.intercross.util

import android.content.Context
import androidx.preference.PreferenceManager
import org.phenoapps.intercross.R

/**
 * Data class representing a predefined raw ZPL label configuration.
 *
 * New configurations use named placeholders such as {crossId}, {femaleId},
 * and {date}. Legacy saved ZPL with ^FN fields is still supported when printing.
 */
data class ZplTemplate(
    val name: String,
    val displayName: String,
    val zplCode: String,
    val type: LabelTemplateType = LabelTemplateType.CROSS,
) {
    companion object {
        fun getDefaultTemplates(context: Context): List<ZplTemplate> {
            return listOf(
                ZplTemplate(
                    name = "template_2x1",
                    displayName = context.getString(R.string.label_2x1_name),
                    zplCode = """
                        ^XA
                        ^FO0,0^A0,25,20^FD{crossId}^FS
                        ^FO140,30^BQN,2,3,H^FDHA,{crossId}^FS
                        ^FO140,170^A0,25,20^FD{date}^FS
                        ^XZ
                    """.trimIndent(),
                    type = LabelTemplateType.CROSS,
                ),
                ZplTemplate(
                    name = "template_3x2",
                    displayName = context.getString(R.string.label_3x2_name),
                    zplCode = """
                        ^XA
                        ^FO0,0^A0,35,28^FD{crossId}^FS
                        ^FO210,40^BQN,2,5,H^FDHA,{crossId}^FS
                        ^FO210,300^A0,32,24^FD{date}^FS
                        ^XZ
                    """.trimIndent(),
                    type = LabelTemplateType.CROSS,
                ),
                ZplTemplate(
                    name = "parent_template_2x1",
                    displayName = context.getString(R.string.parent_label_2x1_name),
                    zplCode = """
                        ^XA
                        ^FO0,0^A0,25,20^FD{parentId}^FS
                        ^FO140,30^BQN,2,3,H^FDHA,{parentId}^FS
                        ^FO0,150^A0,22,18^FD{parentName}^FS
                        ^XZ
                    """.trimIndent(),
                    type = LabelTemplateType.PARENT,
                ),
                ZplTemplate(
                    name = "simple_names_template_2x1",
                    displayName = context.getString(R.string.simple_names_2x1_name),
                    zplCode = """
                        ^XA
                        ^CF0,20,20
                        ^FD{femaleName}^FS
                        ^FO0,20^FD{femaleId}^FS
                        ^FO0,50^FD{maleName}^FS
                        ^FO0,70^FD{maleId}^FS
                        ^FO200,50
                        ^BQN,2,3,H
                        ^FDHA,{crossId}^FS
                        ^FO0,155^FD{date}^FS
                        ^XZ
                    """.trimIndent(),
                    type = LabelTemplateType.CROSS,
                )
            )
        }

        fun getSavedTemplates(context: Context): List<ZplTemplate> {
            val prefs = PreferenceManager.getDefaultSharedPreferences(context)
            val keyUtil = KeyUtil(context)
            return LabelTemplateStore.load(prefs, keyUtil.labelTemplatesKey)
                .map { config ->
                    ZplTemplate(
                        name = "custom_${config.name}",
                        displayName = config.name,
                        zplCode = config.toZpl(),
                        type = config.type,
                    )
                }
        }

        fun getAvailableTemplates(context: Context): List<ZplTemplate> {
            return getDefaultTemplates(context) + getSavedTemplates(context)
        }

        /**
         * Get saved or built-in ZPL by display name.
         */
        fun getTemplateByDisplayName(context: Context, displayName: String): ZplTemplate? {
            return getAvailableTemplates(context).find { it.displayName == displayName }
        }
    }
}
