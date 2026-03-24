package org.phenoapps.intercross.util

import android.content.Context
import org.phenoapps.intercross.R

/**
 * Data class representing a predefined ZPL label template
 *
 * Templates use Zebra field variables:
 * FN1 = Cross/Parent ID (barcode and text)
 * FN2 = Cross/Parent ID (display text)
 * FN3 = Female parent ID
 * FN4 = Male parent ID
 * FN5 = Timestamp
 * FN6 = Person who made the cross
 *
 * All templates must start with ^XA^DFR:TEMPLATE and end with ^XZ
 * to match PrintThread's sendCommand() expectations.
 */
data class ZplTemplate(
    val name: String,
    val displayName: String,
    val zplCode: String,
) {
    companion object {
        fun getDefaultTemplates(context: Context): List<ZplTemplate> {
            return listOf(
                ZplTemplate(
                    name = "template_2x1",
                    displayName = context.getString(R.string.label_2x1_name),
                    zplCode = "^XA^DFR:TEMPLATE^FS^PW406^LH10,10^FS^FO0,0^A0,25,20^FN1^FS^FO140,30^BQN,2,3,H,^FN2^FS^FO140,170^A0,25,20^FN5^FS^XZ",
                ),
                ZplTemplate(
                    name = "template_3x2",
                    displayName = context.getString(R.string.label_3x2_name),
                    zplCode = "^XA^DFR:TEMPLATE^FS^PW609^LH10,10^FS^FO0,0^A0,35,28^FN1^FS^FO210,40^BQN,2,5,H,^FN2^FS^FO210,300^A0,32,24^FN5^FS^XZ",
                )
            )
        }

        /**
         * Get template by display name
         */
        fun getTemplateByDisplayName(context: Context, displayName: String): ZplTemplate? {
            return getDefaultTemplates(context).find { it.displayName == displayName }
        }
    }
}