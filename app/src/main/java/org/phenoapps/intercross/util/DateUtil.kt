package org.phenoapps.intercross.util

import android.os.Build
import java.text.SimpleDateFormat
import java.time.LocalDateTime
import java.time.format.DateTimeFormatter
import java.util.*

class DateUtil {

    private val parser = SimpleDateFormat("yyyy-MM-dd'_'HH'_'mm'_'ss'_'SSS", Locale.getDefault())
    private val timeStamp = SimpleDateFormat("yyyy-MM-dd-hh-mm-ss", Locale.getDefault())
    private val date = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault())

    //attempted to return Date time similar in format to ISO 8601 with underscores
    //Date and time in UTC	2020-06-03T16:31:15+00:00
    //2020-06-03T16:31:15Z
    //20200603T163115Z
    fun getTime(): String = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
        LocalDateTime.now().format(DateTimeFormatter.ofPattern(
                "yyyy-MM-dd_HH_mm_ss_SSS"))
    } else {
        Calendar.getInstance().time.toString()
    }

    fun getFormattedDate(dateString: String): String {
        val dateObject = parser.parse(dateString)
        return dateObject?.let { date.format(it) } ?: dateString
    }
}