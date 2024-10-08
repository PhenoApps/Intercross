package org.phenoapps.intercross.data.models

import androidx.room.DatabaseView

/***
 * This class represents a defined view for the Intercross schema.
 * This view helps organize and output wish list progress
 */
@DatabaseView("""
SELECT DISTINCT femaleDbId as momId, femaleName as momName, maleDbId as dadId, maleName as dadName, wishMin, wishMax, wishType,
	(SELECT COUNT(*) 
	FROM events as child
	WHERE (w.femaleDbId = child.mom and ((w.maleDbId = child.dad) or (child.dad = "blank" and w.maleDbId = "-1")))) as wishProgress
from wishlist as w
""")
data class WishlistView(
        val momId: String,
        val momName: String,
        val dadId: String,
        val dadName: String,
        val wishMin: Int,
        val wishMax: Int,
        val wishType: String,
        val wishProgress: Int
)