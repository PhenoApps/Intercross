package org.phenoapps.intercross.util

/**
 * Utilities for working with named placeholders embedded in ZPL templates.
 *
 * ZPL templates used by the app may contain named tokens such as `{crossId}` and
 * `{date}`. This object provides helper data describing available placeholders
 * and functions to replace tokens with concrete values for events or parents.
 *
 * Use `forEvent` and `forParent` to substitute values into a ZPL string before
 * sending it to a printer or rendering a preview.
 */

import androidx.annotation.StringRes
import org.phenoapps.intercross.R
import org.phenoapps.intercross.data.models.Event
import org.phenoapps.intercross.data.models.Parent
import kotlin.to

data class ZplPlaceholderHelp(
    val token: String,
    @StringRes val descriptionRes: Int,
)

object ZplStringReplacer {
    val eventPlaceholderHelp = listOf(
        ZplPlaceholderHelp("{crossId}", R.string.placeholder_cross_id),
        ZplPlaceholderHelp("{readableName}", R.string.placeholder_readable_name),
        ZplPlaceholderHelp("{femaleId}", R.string.placeholder_female_id),
        ZplPlaceholderHelp("{maleId}", R.string.placeholder_male_id),
        ZplPlaceholderHelp("{femaleName}", R.string.placeholder_female_name),
        ZplPlaceholderHelp("{maleName}", R.string.placeholder_male_name),
        ZplPlaceholderHelp("{date}", R.string.placeholder_date),
        ZplPlaceholderHelp("{timestamp}", R.string.placeholder_timestamp),
        ZplPlaceholderHelp("{person}", R.string.placeholder_person),
        ZplPlaceholderHelp("{experiment}", R.string.placeholder_experiment),
        ZplPlaceholderHelp("{type}", R.string.placeholder_type),
        ZplPlaceholderHelp("{qrCrossId}", R.string.placeholder_qr_cross_id),
    )

    val parentPlaceholderHelp = listOf(
        ZplPlaceholderHelp("{parentId}", R.string.placeholder_parent_id),
        ZplPlaceholderHelp("{parentName}", R.string.placeholder_parent_name),
        ZplPlaceholderHelp("{parentType}", R.string.placeholder_parent_type),
    )

    private val allPlaceholders = (eventPlaceholderHelp + parentPlaceholderHelp)
        .map { it.token }
        .toSet()

    fun hasNamedPlaceholders(zpl: String): Boolean {
        return allPlaceholders.any { placeholder -> zpl.contains(placeholder) }
    }

    fun hasLegacyFields(zpl: String): Boolean {
        return zpl.contains("^FN") || zpl.contains("^DFR:TEMPLATE") || zpl.contains("^XFR:TEMPLATE")
    }

    fun hasPlaceholdersForOtherType(zpl: String, type: LabelTemplateType): Boolean {
        val otherTypePlaceholders = when (type) {
            LabelTemplateType.CROSS -> parentPlaceholderHelp
            LabelTemplateType.PARENT -> eventPlaceholderHelp
        }.map { it.token }

        return otherTypePlaceholders.any { placeholder -> zpl.contains(placeholder) }
    }

    fun forEvent(zpl: String, event: ZebraPrinterUtil.CrossParentRelation): String {
        val timestamp = event.cross.timestamp
        val date = timestamp.substringBefore("_")
        return replace(
            zpl,
            mapOf(
                "{crossId}" to event.cross.eventDbId,
                "{femaleId}" to event.cross.femaleObsUnitDbId,
                "{maleId}" to event.cross.maleObsUnitDbId,
                "{femaleName}" to event.parents.momReadableName,
                "{maleName}" to event.parents.dadReadableName,
                "{date}" to date,
                "{timestamp}" to timestamp,
                "{person}" to event.cross.person,
                "{experiment}" to event.cross.experiment,
                "{type}" to event.cross.type.name,
                "{qrCrossId}" to "QA,${event.cross.eventDbId}",
            ),
        )
    }

    fun forParent(zpl: String, parent: Parent): String {
        return replace(
            zpl,
            mapOf(
                "{crossId}" to parent.codeId,
                "{readableName}" to parent.name,
                "{parentId}" to parent.codeId,
                "{parentName}" to parent.name,
                "{parentType}" to when (parent.sex) {
                    0 -> "female"
                    1 -> "male"
                    else -> "unknown"
                },
                "{qrCrossId}" to "QA,${parent.codeId}",
            ),
        )
    }

    private fun replace(zpl: String, values: Map<String, String>): String {
        return values.entries.fold(zpl) { current, (placeholder, value) ->
            current.replace(placeholder, value.escapeZplFieldData())
        }
    }

    private fun String.escapeZplFieldData(): String {
        return replace("^", " ")
            .replace("~", " ")
    }
}
