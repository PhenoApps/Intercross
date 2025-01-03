package org.phenoapps.intercross.adapters

import android.content.Context
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.databinding.DataBindingUtil
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView
import org.phenoapps.intercross.R
import org.phenoapps.intercross.databinding.ListItemWishlistRowBinding
import org.phenoapps.intercross.fragments.CrossTrackerFragment


/**
 * This adapter class handles CrossData and WishlistFragment.ListEntry data
 * CrossData holds information on two previously crossed samples, and their crosses.
 * ListEntry holds wishlist specific data s.a wishMax, current.
 *
 * CrossData's are not rendered with checkboxes.
 *
 */
class WishlistAdapter(val context: Context) :
        ListAdapter<CrossTrackerFragment.ListEntry, WishlistAdapter.ViewHolder>(SummaryDiffCallback()) {

    private class SummaryDiffCallback : DiffUtil.ItemCallback<CrossTrackerFragment.ListEntry>() {

        override fun areItemsTheSame(oldItem: CrossTrackerFragment.ListEntry,
                                     newItem: CrossTrackerFragment.ListEntry): Boolean {

            return (oldItem.female == newItem.female) && (oldItem.male == newItem.male)
        }

        override fun areContentsTheSame(oldItem: CrossTrackerFragment.ListEntry,
                                        newItem: CrossTrackerFragment.ListEntry): Boolean {

            return (oldItem.female == newItem.female) && (oldItem.male == newItem.male)
        }
    }

    private lateinit var mParent: ViewGroup

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {

        mParent = parent

        return ViewHolder(
                DataBindingUtil.inflate(
                        LayoutInflater.from(parent.context),
                        R.layout.list_item_wishlist_row, parent, false
                )
        )
    }

    override fun onBindViewHolder(holder: ViewHolder, position: Int) {

        getItem(position).let { wishitem ->

            with(holder) {

                itemView.tag = wishitem

                bind(wishitem)
            }
        }
    }

    inner class ViewHolder(
            private val binding: ListItemWishlistRowBinding) : RecyclerView.ViewHolder(binding.root) {

        fun bind(data: CrossTrackerFragment.ListEntry) {

            with(binding) {

                binding.numCrosses.isSelected = true

                maleParent = data.male

                femaleParent = data.female

                count = data.count

                if (data.count.contains("/")) {

                    val tokens = data.count.split("/")

                    if (tokens[0].toInt() >= tokens[1].toInt()) {

                        completed = true
                    }
                }

                //uses inner view holder to create list of crosses used for that wish item
                onClick = View.OnClickListener {



                }
            }
        }
    }
}