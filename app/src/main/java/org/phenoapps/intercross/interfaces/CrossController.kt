package org.phenoapps.intercross.interfaces

import org.phenoapps.intercross.fragments.CrossTrackerFragment
import org.phenoapps.intercross.fragments.CrossTrackerFragment.PlannedCrossData
import org.phenoapps.intercross.fragments.CrossTrackerFragment.WishlistItem

interface CrossController {
    fun onCrossClicked(male: String, female: String)
    fun onPersonChipClicked(persons: List<CrossTrackerFragment.PersonCount>, crossCount: Int)
    fun onDateChipClicked(dates: List<CrossTrackerFragment.DateCount>)
    fun onWishlistProgressChipClicked(wishlistItem: WishlistItem)
    fun getWishItemProgress(plannedCrossData: PlannedCrossData, wishType: String): Int
}