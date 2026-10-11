package ca.georgebrown.studygroupfinder.ui.util

import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

/** "Thu, 3:00 PM", the short format used on every session card in the mockups. */
fun formatSessionTime(millis: Long): String =
    SimpleDateFormat("EEE, h:mm a", Locale.getDefault()).format(Date(millis))

/** "Thursday, Oct 16 · 3:00 PM", the long format for the details screen. */
fun formatSessionDateLong(millis: Long): String =
    SimpleDateFormat("EEEE, MMM d · h:mm a", Locale.getDefault()).format(Date(millis))

/** "Thu, Oct 16" for date-only fields. */
fun formatDateOnly(millis: Long): String =
    SimpleDateFormat("EEE, MMM d", Locale.getDefault()).format(Date(millis))

/** "3:00 PM" for time-only fields. */
fun formatTimeOnly(millis: Long): String =
    SimpleDateFormat("h:mm a", Locale.getDefault()).format(Date(millis))
