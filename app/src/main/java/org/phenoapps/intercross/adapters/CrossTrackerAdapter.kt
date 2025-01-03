package org.phenoapps.intercross.adapters

import android.graphics.Color
import android.view.LayoutInflater
import android.view.MotionEvent
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import android.widget.LinearLayout
import android.widget.TextView
import androidx.cardview.widget.CardView
import androidx.core.content.ContextCompat
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView
import com.google.android.material.chip.Chip
import com.google.android.material.progressindicator.LinearProgressIndicator
import org.phenoapps.intercross.R
import org.phenoapps.intercross.fragments.CrossTrackerFragment
import org.phenoapps.intercross.interfaces.CrossController
import org.phenoapps.intercross.util.DateUtil
import org.phenoapps.intercross.util.WishProgressColorUtil

class CrossTrackerAdapter(
    private val crossController: CrossController
) : ListAdapter<CrossTrackerFragment.ListEntry, CrossTrackerAdapter.ViewHolder>(
        DiffCallback()
    ) {

    inner class ViewHolder(view: View) : RecyclerView.ViewHolder(view) {
        val crossCount: TextView = view.findViewById(R.id.cross_count)
        val femaleParent: TextView = view.findViewById(R.id.female_parent)
        val maleParent: TextView = view.findViewById(R.id.male_parent)
        val personChip: Chip = view.findViewById(R.id.person_chip)
        val dateChip: Chip = view.findViewById(R.id.date_chip)
        val wishlistProgressChip: Chip = view.findViewById(R.id.wish_progress_chip)

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
                is CrossTrackerFragment.PlannedCrossData -> {
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
                is CrossTrackerFragment.PlannedCrossData -> setProgressBar(viewHolder, this)
            }
        }
    }

    private fun hideProgressBar(viewHolder: ViewHolder) {
        viewHolder.apply {
            wishlistProgressChip.visibility = View.GONE
            progressSection.visibility = View.GONE
        }
    }

    private fun setProgressBar(viewHolder: ViewHolder, plannedCrossData: CrossTrackerFragment.PlannedCrossData) {
        val wishProgress = plannedCrossData.progress.toIntOrNull() ?: 0
        val minTarget = plannedCrossData.wishMin.toInt()
        val maxTarget = plannedCrossData.wishMax.toInt()

        val color = WishProgressColorUtil().getProgressColor(viewHolder.itemView.context, wishProgress, minTarget, maxTarget)

        viewHolder.apply {
            wishlistProgressChip.visibility = View.VISIBLE
            progressSection.visibility = View.VISIBLE
            progressStatusIcon.setImageDrawable(
                ContextCompat.getDrawable(
                    viewHolder.itemView.context,
                    if (wishProgress >= minTarget) R.drawable.ic_wishes_complete else R.drawable.ic_wishes_incomplete
                )
            )
            wishlistProgressChip.apply {
                text = "${plannedCrossData.progress}/${plannedCrossData.wishMin}"
                setOnClickListener {
                    crossController.onWishlistProgressChipClicked(plannedCrossData)
                }
            }
            progressBar.apply {
                max = minTarget
                progress = wishProgress
                setIndicatorColor(color)
                visibility = View.VISIBLE
            }
        }
    }

    class DiffCallback : DiffUtil.ItemCallback<CrossTrackerFragment.ListEntry>() {

        override fun areItemsTheSame(oldItem: CrossTrackerFragment.ListEntry, newItem: CrossTrackerFragment.ListEntry) = oldItem == newItem

        override fun areContentsTheSame(oldItem: CrossTrackerFragment.ListEntry, newItem: CrossTrackerFragment.ListEntry) = oldItem == newItem
    }
}