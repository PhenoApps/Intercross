package org.phenoapps.intercross.dialogs

import android.app.Dialog
import android.content.Context
import android.os.Bundle
import android.widget.*
import org.phenoapps.intercross.R
import org.phenoapps.intercross.interfaces.MetadataManager

/**
 * A cancelable dialog for creating a new metadata property with a default value.
 * Metadata property must not be blank, while metadata default value can be an integer or null (empty string)
 * Updating the database with the new properties is handled by the MetadataManager implemented in the accompanied fragment.
 */
class MetadataCreatorDialog(private val ctx: Context, private val listener: MetadataManager) : Dialog(ctx, R.style.Dialog) {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        setContentView(R.layout.dialog_metadata_creator)

        setCanceledOnTouchOutside(true)

        setTitle(R.string.dialog_metadata_title)

        setupUi()

    }

    private fun setupUi() {

        val property = findViewById<EditText>(R.id.dialog_metadata_property_edit_text)

        val value = findViewById<EditText>(R.id.dialog_metadata_value_edit_text)

        findViewById<Button>(R.id.dialog_metadata_value_submit_button).setOnClickListener {

            val propertyText = property.text.toString()
            val valueText = value.text.toString()

            when {
                propertyText.isBlank() -> Toast.makeText(ctx, R.string.dialog_metadata_property_must_not_be_empty, Toast.LENGTH_SHORT).show()
                propertyText.contains(",") -> Toast.makeText(ctx, R.string.dialog_metadata_property_must_not_have_comma, Toast.LENGTH_SHORT).show()
                valueText.isNotBlank() && valueText.toIntOrNull() == null -> Toast.makeText(ctx, R.string.dialog_metadata_value_must_be_integer_or_empty, Toast.LENGTH_SHORT).show()
                else -> { // valueText was blank OR toInt() was a valid int
                    listener.onMetadataCreated(propertyText, valueText)
                    dismiss()
                }
            }
        }
    }
}