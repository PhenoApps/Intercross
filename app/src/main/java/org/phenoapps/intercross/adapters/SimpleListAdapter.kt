package org.phenoapps.intercross.adapters

import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.databinding.DataBindingUtil
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView
import com.google.android.material.card.MaterialCardView
import org.phenoapps.intercross.R
import org.phenoapps.intercross.databinding.SimpleListItemBinding
import org.phenoapps.intercross.interfaces.OnSimpleItemClicked

/**
 * Simple class to use with recycler views to submit strings
 * Data supplied should be a list of pairs where the first item is a Long id and the second item is a string value
 * s.a listOf(1L to "Chaney", 2L to "Trevor")
 *
 * Uses an onclick interface to return the pair that was clicked
 */
class SimpleListAdapter(private val listener: OnSimpleItemClicked)
    : ListAdapter<Pair<String, String>, SimpleListAdapter.ViewHolder>(

    //diffutil is simply string equality of the row id or the content value
    object : DiffUtil.ItemCallback<Pair<String, String>>() {
        override fun areItemsTheSame(old: Pair<String, String>, new: Pair<String, String>) = old.first == new.first
        override fun areContentsTheSame(old: Pair<String, String>, new: Pair<String, String>) = old.second == new.second
    }) {

    private var mSelectedPosition = -1

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {

        return ViewHolder(

            DataBindingUtil.inflate(
                    LayoutInflater.from(parent.context),
                    R.layout.simple_list_item, parent, false
            )
        )
    }

    override fun onBindViewHolder(holder: ViewHolder, position: Int) {

        getItem(position).let { item ->

            with(holder) {

                holder.itemView.setOnLongClickListener {

                    listener.onItemLongClicked(item)

                    true
                }

                holder.itemView.setOnClickListener {

                    val old = mSelectedPosition

                    mSelectedPosition = position

                    notifyItemChanged(position)
                    notifyItemChanged(old)

                    this@SimpleListAdapter.listener.onItemClicked(item)
                }

                bind(item, mSelectedPosition == position)
            }
        }
    }

    inner class ViewHolder(private val binding: SimpleListItemBinding)
        : RecyclerView.ViewHolder(binding.root) {

        fun bind(pair: Pair<String, String>, isSelected: Boolean) {

            binding.name = pair.second
            (binding.root as? MaterialCardView)?.isChecked = isSelected
        }
    }
}