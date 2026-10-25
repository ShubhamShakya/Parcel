package com.parcel.presentation
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
internal actual fun formatLocalTimestamp(epochMillis: Long): String =
    SimpleDateFormat("dd MMM yyyy · h:mm a", Locale.getDefault()).format(Date(epochMillis))
