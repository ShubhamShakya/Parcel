package com.parcel.data

import com.parcel.domain.*
import kotlin.random.Random

internal expect fun nextLocalMidnight(epochMillis: Long): Long

internal fun newDeliveryProgress(id: String, confirmedAt: Long): DeliveryProgress = scheduledDelivery(
    id, confirmedAt, Random.nextInt(100000, 1000000).toString(), Random.nextBoolean(), nextLocalMidnight(confirmedAt))
