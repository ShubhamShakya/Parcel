package com.parcel.presentation
/** Show an actual calendar date and local device time, including across midnight. */
internal expect fun formatLocalTimestamp(epochMillis: Long): String
