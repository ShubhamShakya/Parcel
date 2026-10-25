package com.parcel.data

import java.util.Calendar

internal actual fun nextLocalMidnight(epochMillis: Long): Long = Calendar.getInstance().apply {
    timeInMillis = epochMillis
    add(Calendar.DAY_OF_MONTH, 1)
    set(Calendar.HOUR_OF_DAY, 0)
    set(Calendar.MINUTE, 0)
    set(Calendar.SECOND, 0)
    set(Calendar.MILLISECOND, 0)
}.timeInMillis
