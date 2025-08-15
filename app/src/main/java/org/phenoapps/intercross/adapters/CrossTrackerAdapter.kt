package org.phenoapps.intercross.adapters

import android.content.Context
import android.view.LayoutInflater
import android.view.MotionEvent
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import android.widget.LinearLayout
import android.widget.TextView
import androidx.cardview.widget.CardView
import androidx.constraintlayout.helper.widget.Flow
import androidx.constraintlayout.widget.ConstraintLayout
import androidx.core.content.ContextCompat
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView
import com.google.android.material.chip.Chip
import com.google.android.material.progressindicator.LinearProgressIndicator
import org.phenoapps.intercross.R
import org.phenoapps.intercross.fragments.CrossTrackerFragment
import org.phenoapps.intercross.fragments.CrossTrackerFragment.ListEntry
import org.phenoapps.intercross.fragments.CrossTrackerFragment.PlannedCrossData
import org.phenoapps.intercross.interfaces.CrossController
import org.phenoapps.intercross.util.DateUtil
import org.phenoapps.intercross.util.WishProgressColorUtil
import androidx.core.view.isVisible

class CrossTrackerAdapter(
    private val crossController: CrossController,
) : ListAdapter<ListEntry, CrossTrackerAdapter.ViewHolder>(
        DiffCallback()
    ) {

    inner class ViewHolder(view: View) : RecyclerView.ViewHolder(view) {
        val crossCount: TextView = view.findViewById(R.id.cross_count)
        val femaleParent: TextView = view.findViewById(R.id.female_parent)
        val maleParent: TextView = view.findViewById(R.id.male_parent)
        val personChip: Chip = view.findViewById(R.id.person_chip)
        val dateChip: Chip = view.findViewById(R.id.date_chip)

        val progressSection: LinearLayout = view.findViewById(R.id.wish_progress_section)
        val progressStatusIcon: ImageView = view.findViewById(R.id.wish_progress_status)
        val progressBar: LinearProgressIndicator = view.findViewById(R.id.wish_progress_bar)

        init {
            val crossCardView: CardView = view.findViewById(R.id.cross_card)
            var isLongPress = false

            // intercept touch events to prevent person, date and wishlist progress chip ripple
            crossCardView.setOnTouchListener{ _, event ->
                if (event.action == MotionEvent.ACTION_UP) {
                    if (!isLongPress) {
                        view.performClick()
                        handleClick(layoutPosition)
                    }
                    isLongPress = false
                }
                true
            }
        }
    }

    private fun handleClick(position: Int) {
        if (position != RecyclerView.NO_POSITION) {
            when (val item = currentList[position]) {
                is PlannedCrossData -> {
                    crossController.onCrossClicked(item.maleId, item.femaleId)
                }
                else -> {
                    crossController.onCrossClicked(item.male, item.female)
                }
            }
        }
    }

    // Create new views (invoked by the layout manager)
    override fun onCreateViewHolder(viewGroup: ViewGroup, viewType: Int): ViewHolder {
        // Create a new view, which defines the UI of the list item
        val view = LayoutInflater.from(viewGroup.context)
            .inflate(R.layout.list_item_cross_tracker, viewGroup, false)

        return ViewHolder(view)
    }

    // Replace the contents of a view (invoked by the layout manager)
    override fun onBindViewHolder(viewHolder: ViewHolder, position: Int) {

        // Get element from your dataset at this position and replace the
        // contents of the view with that element
        with(currentList[position]) {
            viewHolder.apply {
                crossCount.text = count
                femaleParent.text = female
                maleParent.text = male

                personChip.apply {
                    text = if (persons.isNotEmpty()) { // show only the first person if it exists
                        if (persons.size == 1 ) persons.first().name else ""
                    } else ""
                    setChipIconResource(
                        if (persons.size > 1) R.drawable.ic_person_multiple
                        else R.drawable.ic_person
                    )
                    visibility = if (persons.isNotEmpty()) View.VISIBLE else View.GONE
                    setOnClickListener {
                        crossController.onPersonChipClicked(persons, (crossCount.text as String).toInt())
                    }
                }

                dateChip.apply { // show only the first date if it exists
                    text = if (dates.isNotEmpty()) {
                        if (dates.size == 1) DateUtil().getFormattedDate(dates.first().date) else ""
                    } else ""
                    setChipIconResource(
                        if (dates.size > 1) R.drawable.ic_calendar_multiple
                        else R.drawable.ic_calendar
                    )
                    visibility = if (dates.isNotEmpty()) View.VISIBLE else View.GONE
                    setOnClickListener {
                        crossController.onDateChipClicked(dates)
                    }
                }
            }

            when (this) {
                is CrossTrackerFragment.UnplannedCrossData -> hideProgressBar(viewHolder)
                is PlannedCrossData -> setWishlistProgress(viewHolder, this)
            }
        }
    }

    private fun hideProgressBar(viewHolder: ViewHolder) {
        viewHolder.apply {
            progressSection.visibility = View.GONE
        }
        clearWishChipsFromFlow(viewHolder)
        updateWishChips(viewHolder, null)
    }

    private fun setWishlistProgress(viewHolder: ViewHolder, planned: PlannedCrossData) {
        val ctx = viewHolder.itemView.context
        val wishes = planned.wishes

        clearWishChipsFromFlow(viewHolder)

        if (wishes.isEmpty()) {
            hideProgressBar(viewHolder)
            updateWishChips(viewHolder, null)
            return
        }

        viewHolder.itemView.findViewById<Flow>(R.id.chips_flow)
        val constraintLayout = viewHolder.itemView.findViewById<ConstraintLayout>(R.id.root_constraint_layout)

        val chipIds = mutableListOf<Int>()

        // add wish chips
        wishes.forEachIndexed { index, wish ->
            val chipId = View.generateViewId()
            val chip = Chip(ctx).apply {
                id = chipId
                text = "${wish.progress}/${wish.min}"
                setChipIconResource(getWishTypeIcon(wish.wishType, ctx))
                isClickable = true
                isCheckable = false
                layoutParams = ViewGroup.LayoutParams(
                    ViewGroup.LayoutParams.WRAP_CONTENT,
                    ViewGroup.LayoutParams.WRAP_CONTENT
                )
                setOnClickListener { crossController.onWishlistProgressChipClicked(wish) }
            }

            constraintLayout.addView(chip)
            chipIds.add(chipId)
        }

        updateWishChips(viewHolder, chipIds)

        // find WishlistItem with highest progress in terms of %
        val highestProgressWish = wishes.maxBy { it.progress.toDouble() / it.min.toDouble() }
        val bestPercent = ((highestProgressWish.progress.toDouble() / highestProgressWish.min.toDouble()) * 100).toInt().coerceAtMost(100)

        val color = WishProgressColorUtil().getProgressColor(ctx, highestProgressWish.progress, highestProgressWish.min, highestProgressWish.max)

        viewHolder.apply {
            progressSection.visibility = View.VISIBLE

            // set progress icon if ANY wish meets min
            val anyComplete = planned.wishes.any { it.progress >= it.min }
            progressStatusIcon.setImageDrawable(
                ContextCompat.getDrawable(
                    ctx,
                    if (anyComplete) R.drawable.ic_wishes_complete else R.drawable.ic_wishes_incomplete
                )
            )

            progressBar.apply {
                max = 100
                progress = bestPercent
                setIndicatorColor(color)
                visibility = View.VISIBLE
            }
        }
    }

    private fun updateWishChips(viewHolder: ViewHolder, wishChipIds: List<Int>?) {
        val flowLayout = viewHolder.itemView.findViewById<Flow>(R.id.chips_flow)

        val allIds = mutableListOf<Int>()

        if (viewHolder.personChip.isVisible) allIds.add(R.id.person_chip)

        if (viewHolder.dateChip.isVisible) allIds.add(R.id.date_chip)

        wishChipIds?.let { allIds.addAll(wishChipIds) }

        flowLayout.setReferencedIds(allIds.toIntArray())
    }

    private fun clearWishChipsFromFlow(viewHolder: ViewHolder) {
        val constraintLayout = viewHolder.itemView.findViewById<ConstraintLayout>(R.id.root_constraint_layout)

        // remove any previously added wish chips
        val childrenToRemove = mutableListOf<View>()
        for (i in 0 until constraintLayout.childCount) {
            val child = constraintLayout.getChildAt(i)
            if (child is Chip && child.id != R.id.person_chip && child.id != R.id.date_chip) {
                childrenToRemove.add(child)
            }
        }

        childrenToRemove.forEach { constraintLayout.removeView(it) }
    }

    private fun getWishTypeIcon(wishType: String, context: Context): Int {
        return when (wishType) {
            context.getString(R.string.metadata_fruits) -> R.drawable.ic_fruit
            context.getString(R.string.metadata_seeds) -> R.drawable.ic_seed
            context.getString(R.string.metadata_flowers) -> R.drawable.ic_flower
            context.getString(R.string.literal_cross) -> R.drawable.ic_cross
            else -> R.drawable.ic_sprout
        }
    }

    class DiffCallback : DiffUtil.ItemCallback<ListEntry>() {

        override fun areItemsTheSame(oldItem: ListEntry, newItem: ListEntry) =
            oldItem.male == newItem.male && oldItem.female == newItem.female && oldItem.getType() == newItem.getType()

        override fun areContentsTheSame(oldItem: ListEntry, newItem: ListEntry): Boolean {
            if (oldItem.count != newItem.count) return false
            if (oldItem.persons != newItem.persons) return false
            if (oldItem.dates != newItem.dates) return false
            if (oldItem is PlannedCrossData && newItem is PlannedCrossData) {
                if (oldItem.wishes.sortedBy { it.wishType } != newItem.wishes.sortedBy { it.wishType }) return false
            }
            return true
        }
    }
}