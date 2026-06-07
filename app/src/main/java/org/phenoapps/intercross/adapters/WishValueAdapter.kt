package org.phenoapps.intercross.adapters

import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.core.widget.doAfterTextChanged
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView
import org.phenoapps.intercross.R
import org.phenoapps.intercross.databinding.ListItemWishValueBinding

data class WishValueItem(
    val type: String,
    var min: Int? = null,
    var max: Int? = null
)

class WishValueAdapter : ListAdapter<WishValueItem, WishValueAdapter.ViewHolder>(
    object : DiffUtil.ItemCallback<WishValueItem>() {
        override fun areItemsTheSame(old: WishValueItem, new: WishValueItem) = old.type == new.type
        override fun areContentsTheSame(old: WishValueItem, new: WishValueItem) = old == new
    }
) {

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
        return ViewHolder(
            ListItemWishValueBinding.inflate(
                LayoutInflater.from(parent.context),
                parent,
                false
            )
        )
    }

    override fun onBindViewHolder(holder: ViewHolder, position: Int) {
        holder.bind(getItem(position))
    }

    inner class ViewHolder(private val binding: ListItemWishValueBinding) :
        RecyclerView.ViewHolder(binding.root) {

        private var currentItem: WishValueItem? = null
        private var isBinding = false

        init {
            binding.wishValueMinEt.doAfterTextChanged {
                if (!isBinding) {
                    currentItem?.min = it?.toString()?.toIntOrNull()
                    validate(currentItem)
                }
            }
            binding.wishValueMaxEt.doAfterTextChanged {
                if (!isBinding) {
                    currentItem?.max = it?.toString()?.toIntOrNull()
                    validate(currentItem)
                }
            }
        }

        fun bind(item: WishValueItem) {
            isBinding = true
            currentItem = item
            binding.name = item.type
            binding.wishValueMinEt.setText(item.min?.toString() ?: "")
            binding.wishValueMaxEt.setText(item.max?.toString() ?: "")
            validate(item)
            isBinding = false
        }

        private fun validate(item: WishValueItem?) {
            item ?: return
            val min = item.min
            val max = item.max

            if (min != null && min <= 0) {
                binding.wishValueMinTil.error = binding.root.context.getString(R.string.frag_wf_values_min_must_not_be_null)
            } else {
                binding.wishValueMinTil.error = null
            }

            if (max != null && min != null && max < min) {
                binding.wishValueMaxTil.error = binding.root.context.getString(R.string.frag_wf_values_max_must_not_be_null)
            } else {
                binding.wishValueMaxTil.error = null
            }
        }
    }
}