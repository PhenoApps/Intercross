package org.phenoapps.intercross.util

import android.content.Context
import android.graphics.Color
import androidx.core.content.ContextCompat
import org.phenoapps.intercross.R

class WishProgressColorUtil {
    fun getProgressColor(
        context: Context,
        currentProgress: Int,
        minTarget: Int,
        maxTarget: Int
    ): Int {

        val percentage = (currentProgress.toFloat() / minTarget.toFloat()) * 100

        return ContextCompat.getColor(context, when {
            currentProgress >= maxTarget -> R.color.progressMax  // dark green
            currentProgress >= minTarget -> R.color.progressMin  // light green
            percentage >= 66 -> R.color.progressMoreThanTwoThird   // yellow
            percentage >= 33 -> R.color.progressLessThanTwoThird   // orange
            currentProgress > 0 -> R.color.progressLessThanOneThird // red
            else -> R.color.progressBlank                            // gray
        })
    }
}