package org.phenoapps.intercross.adapters

import android.view.LayoutInflater
import android.view.ViewGroup
import android.view.inputmethod.InputMethodManager
import androidx.core.widget.addTextChangedListener
import androidx.databinding.DataBindingUtil
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView
import org.phenoapps.intercross.R
import org.phenoapps.intercross.adapters.models.MetadataModel
import org.phenoapps.intercross.databinding.ListItemMetadataBinding
import org.phenoapps.intercross.interfaces.MetadataManager


class MetadataAdapter(val imm: InputMethodManager?, val listener: MetadataManager) :
    ListAdapter<MetadataModel, RecyclerView.ViewHolder>(MetadataModel.Companion.DiffCallback()) {

    private var mFirstLoad = true

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): RecyclerView.ViewHolder {

        return MetadataViewHolder(DataBindingUtil.inflate(
                        LayoutInflater.from(parent.context),
                        R.layout.list_item_metadata, parent, false))
    }

    override fun onBindViewHolder(holder: RecyclerView.ViewHolder, position: Int) {

        getItem(position).let { meta ->

            with(holder as MetadataViewHolder) {

                bind(meta)

            }
        }
    }

    inner class MetadataViewHolder(private val binding: ListItemMetadataBinding) : RecyclerView.ViewHolder(binding.root) {

        fun bind(meta: MetadataModel) {

            with(binding) {
                fun validateInputForMaxInt(newInput: String, previousValue: String): String {
                    if (newInput.isNotEmpty()) {
                        val longValue = newInput.toLongOrNull()

                        if (longValue != null && longValue > Int.MAX_VALUE) { // retain old value
                            listItemMetadataEditText.setText(previousValue)
                            listItemMetadataEditText.setSelection(previousValue.length)
                            listItemMetadataTextLayout.error = listItemMetadataTextLayout.context.getString(
                                R.string.error_value_exceeds_max,
                                Int.MAX_VALUE
                            )
                            return previousValue
                        } else { // valid input
                            listItemMetadataTextLayout.error = null
                            return newInput
                        }
                    } else { // input is empty = valid
                        listItemMetadataTextLayout.error = null
                        return ""
                    }
                }

                listItemMetadataEditText.setText(meta.value)

                if (mFirstLoad) {
                    listItemMetadataEditText.requestFocus()
                    imm?.toggleSoftInput(InputMethodManager.SHOW_FORCED, InputMethodManager.HIDE_IMPLICIT_ONLY)
                    mFirstLoad = false
                }

                listItemMetadataTextLayout.hint = meta.property

                var previousValidValue = meta.value

                listItemMetadataEditText.addTextChangedListener {

                    val currentInput = listItemMetadataEditText.text.toString()

                    // update the variable to hold validated input
                    previousValidValue = validateInputForMaxInt(currentInput, previousValidValue)

                    val intValue = previousValidValue.toIntOrNull()

                    listener.onMetadataUpdated(meta.property, intValue)

                }

                listItemMetadataEditText.setOnFocusChangeListener { _, hasFocus -> // clear any errors when focus shifts
                    if (!hasFocus) {
                        listItemMetadataTextLayout.error = null
                    }
                }

                value = if (meta.value == "null") "" else meta.value

                property = meta.property

                hint = meta.property
            }
        }
    }
}