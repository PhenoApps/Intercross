package org.phenoapps.intercross.util

import android.content.Context
import androidx.appcompat.app.AlertDialog
import androidx.fragment.app.FragmentActivity
import androidx.preference.PreferenceManager
import org.phenoapps.intercross.R
import org.phenoapps.intercross.data.models.Event
import org.phenoapps.intercross.data.models.Meta
import org.phenoapps.intercross.data.models.MetadataValues
import org.phenoapps.intercross.data.models.Parent
import org.phenoapps.intercross.data.models.WishlistView
import org.phenoapps.intercross.data.viewmodels.EventListViewModel
import org.phenoapps.intercross.data.viewmodels.MetaValuesViewModel
import org.phenoapps.intercross.data.viewmodels.ParentsListViewModel
import java.util.UUID

class CrossUtil(val context: Context) {

    private val mPref by lazy {
        PreferenceManager.getDefaultSharedPreferences(context)
    }

    private val mKeyUtil by lazy {
        KeyUtil(context)
    }

    fun submitCrossEvent(activity: FragmentActivity?,
                         female: String,
                         male: String,
                         crossName: String,
                         crossIdSettings: CrossIdSettings,
                         eventsModel: EventListViewModel,
                         parents: List<Parent>,
                         parentModel: ParentsListViewModel,
                         wishlistProgress: List<WishlistView>,
                         metaList: List<Meta>,
                         metaValueModel: MetaValuesViewModel,
                         suppressDialog: Boolean = false): Long {

        var name = crossName

        if (crossIdSettings.isPattern) {

            //if this submit is from the barcode fragment the cross name is empty

            if (name.isBlank()) {

                name = crossIdSettings.pattern

            }

            //in the case the user inputs a name while in pattern mode, use the name and don't update the pattern
            if (name == crossIdSettings.pattern) {

                CrossIdSettings.incrementNumber(mPref)

            }
        }

        if (crossIdSettings.isUUID && name.isBlank()) {

            name = UUID.randomUUID().toString()

        }

        val isCommutative = mPref.getBoolean(mKeyUtil.commutativeCrossingKey, false)

        val experiment = mPref.getString(mKeyUtil.experimentNameKey, "")

        val firstName = mPref.getString(mKeyUtil.personFirstNameKey,"")
        val lastName = mPref.getString(mKeyUtil.personLastNameKey,"")
        val person = if (!firstName.isNullOrEmpty() || !lastName.isNullOrEmpty()) {
            "$firstName $lastName"
        } else {
            ""
        }

        val date = DateUtil().getTime()

        val e = Event(name,
                female,
                male,
                "",
                date,
                person,
      experiment ?: "?")

        /** Insert mom/dad cross ids only if they don't exist in the DB already **/
        if (!parents.any { p -> p.codeId == e.femaleObsUnitDbId }) {

            parentModel.insert(Parent(e.femaleObsUnitDbId, 0))

        }

        if (!parents.any { p -> p.codeId == e.maleObsUnitDbId }) {

            parentModel.insert(Parent(e.maleObsUnitDbId, 1))

        }

        val eid = eventsModel.insert(e)

        FileUtil(context).ringNotification(true)

        //TODO FirebaseCrashlytics.getInstance().log("Cross created: $name $date")

        //insert default metadata values
        metaList.forEach {
            metaValueModel.insert(
                MetadataValues(eid.toInt(), it.id?.toInt() ?: -1, it.defaultValue)
            )
        }

        activity?.runOnUiThread {
            if (!suppressDialog) {
                if (isCommutative) checkCommutativeWishlist(female, male, wishlistProgress)
                else checkWishlist(female, male, wishlistProgress)
            }
        }

        return eid
    }

    private fun checkCommutativeWishlist(f: String, m: String, wishlist: List<WishlistView>) {

        val dadId = if (m == "blank") "-1" else m

        wishlist.filter { (it.momId == f && it.dadId == dadId)
                || (it.momId == dadId && it.dadId == f) }.forEach { item ->

            checkWishProgress(item)

        }
    }

    private fun checkWishlist(f: String, m: String, wishlist: List<WishlistView>) {

        val dadId = if (m == "blank") "-1" else m

        wishlist.find { it.momId == f && it.dadId == dadId }?.let { item ->

            checkWishProgress(item)

        }
    }

    /**
     * Displays a notification through dialog if the wish progress reaches min and/or max
     */
    private fun checkWishProgress(wishItem: WishlistView) {
        val newProgress = wishItem.wishProgress + 1

        if (newProgress >= wishItem.wishMin && wishItem.wishMin != 0) {

            FileUtil(context).ringNotification(true)

            val messageRes =
                if (newProgress >= wishItem.wishMax && wishItem.wishMax != 0) {

                    if (wishItem.wishMin == wishItem.wishMax)
                        R.string.wish_min_max_complete
                    else R.string.wish_max_complete

                } else {

                    R.string.wish_min_complete

                }

            Dialogs.notify(AlertDialog.Builder(context), context.getString(messageRes))

        }
    }

}
