package org.phenoapps.intercross.dialogs

import android.app.Dialog
import android.content.Context
import android.os.Bundle
import android.widget.*
import org.phenoapps.intercross.R
import org.phenoapps.intercross.interfaces.MetadataManager

/**
 * A cancelable dialog for updating default fields in a metadata property.
 */
class MetadataDefaultEditorDialog(private val ctx: Context,
                                  private val rowId: Long,
                                  private val property: String,
                                  private val default: Int,
                                  private val listener: MetadataManager) : Dialog(ctx, R.style.Dialog) {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        setContentView(R.layout.dialog_metadata_default_updater)

        setCanceledOnTouchOutside(true)

        setTitle(R.string.dialog_metadata_manager_title)

        setupUi()

    }

    private fun setupUi() {

        val property = findViewById<TextView>(R.id.dialog_metadata_updater_property_edit_text).apply {
            text = property
        }

        val value = findViewById<EditText>(R.id.dialog_metadata_updater_value_edit_text).apply {
            setText(default.toString())
        }

        findViewById<Button>(R.id.dialog_metadata_updater_value_submit_button).setOnClickListener {

            val intVal = value.text.toString().toIntOrNull()

            if (value.text.isNotBlank() && intVal != null) {

                listener.onMetadataDefaultUpdated(rowId, property.text.toString(), intVal)

                dismiss()

            } else Toast.makeText(ctx, R.string.dialog_metadata_value_must_be_integer_or_empty, Toast.LENGTH_SHORT).show()
        }

        findViewById<Button>(R.id.dialog_metadata_updater_value_delete_button).setOnClickListener {

            listener.onMetadataLongClicked(this.rowId, this.property)

            dismiss()
        }
    }
}