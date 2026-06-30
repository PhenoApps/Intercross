package org.phenoapps.intercross.util

import android.content.Context
import androidx.core.content.ContextCompat
import org.phenoapps.intercross.R

class WishProgressColorUtil {
    fun getProgressColor(
        context: Context,
        currentProgress: Int,
        minTarget: Int,
        maxTarget: Int
    ): Int {
        return ContextCompat.getColor(
            context,
            getProgressColorRes(currentProgress, minTarget, maxTarget)
        )
    }

    internal fun getProgressColorRes(
        currentProgress: Int,
        minTarget: Int,
        maxTarget: Int
    ): Int {
        val percentage = if (minTarget > 0) {
            (currentProgress.toFloat() / minTarget.toFloat()) * 100
        } else {
            0f
        }

        return when {
            maxTarget > 0 && currentProgress >= maxTarget -> R.color.progressMax  // dark green
            minTarget > 0 && currentProgress >= minTarget -> R.color.progressMin  // light green
            percentage >= 66 -> R.color.progressMoreThanTwoThird   // yellow
            percentage >= 33 -> R.color.progressLessThanTwoThird   // orange
            currentProgress > 0 -> R.color.progressLessThanOneThird // red
            else -> R.color.progressBlank                            // gray
        }
    }
}
