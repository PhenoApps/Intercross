package org.phenoapps.intercross.data.models

import androidx.recyclerview.widget.DiffUtil


open class BaseParent: BaseTable() {

    companion object {

        class DiffCallback : DiffUtil.ItemCallback<BaseParent>() {

            override fun areItemsTheSame(oldItem: BaseParent, newItem: BaseParent): Boolean {
                val oldCode = (oldItem as? Parent)?.codeId ?: (oldItem as? PollenGroup)?.codeId
                val newCode = (newItem as? Parent)?.codeId ?: (newItem as? PollenGroup)?.codeId
                return oldCode == newCode
            }

            override fun areContentsTheSame(oldItem: BaseParent, newItem: BaseParent): Boolean {
                val oldSelected = (oldItem as? Parent)?.selected ?: (oldItem as? PollenGroup)?.selected
                val newSelected = (newItem as? Parent)?.selected ?: (newItem as? PollenGroup)?.selected
                return oldItem.hashCode() == newItem.hashCode() && oldSelected == newSelected
            }
        }
    }

    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (javaClass != other?.javaClass) return false
        return true
    }

    override fun hashCode(): Int {
        return javaClass.hashCode()
    }
}

