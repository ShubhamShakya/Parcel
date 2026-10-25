package com.parcel.domain

import kotlin.test.*

class DeliveryTest {
    @Test fun lateNightSchedulesAndInstantTimestamps() {
        val start = 22 * 3_600_000L
        val midnight = DELIVERY_DAY_MS
        val short = scheduledDelivery("short", start, "123456", true, midnight)
        val hourly = scheduledDelivery("hourly", start, "123456", false, midnight)
        assertEquals(midnight - 1, short.dueAt)
        assertEquals(start + DELIVERY_DURATION_MS, hourly.dueAt)
        DeliveryStage.entries.drop(1).forEach { stage ->
            assertEquals(stage, short.advance(short.scheduledTime(stage)).stage)
            assertEquals(DeliveryStage.entries[stage.ordinal - 1], short.advance(short.scheduledTime(stage) - 1).stage)
        }
        val instant = short.deliverNow(start + 3_600_000)
        DeliveryStage.entries.forEach { stage ->
            assertEquals(minOf(short.scheduledTime(stage), start + 3_600_000), instant.milestoneTime(stage))
        }
        assertEquals(instant.instantDeliveredAt, instant.deliverNow(midnight).instantDeliveredAt)
    }

    @Test fun exactMilestonesAdvanceHourlyAndDeliverAtFourHours() {
        val start = 1_000L
        val order = DeliveryProgress("one", start, "123456")
        DeliveryStage.entries.drop(1).forEach { stage ->
            val boundary = start + stage.afterHours * 3_600_000L
            assertEquals(DeliveryStage.entries[stage.ordinal - 1], order.advance(boundary - 1).stage)
            assertEquals(stage, order.advance(boundary).stage)
        }
        assertEquals(listOf(0, 1, 2, 3, 4), DeliveryStage.entries.map { it.afterHours })
        assertEquals(start + DELIVERY_DURATION_MS, order.dueAt)
    }
    @Test fun clockRollbackCannotRegressProgressOrRelockUnboxing() {
        val delivered = DeliveryProgress("one", 1000, "123456").advance(1000 + DELIVERY_DAY_MS).unbox(1000 + DELIVERY_DAY_MS)
        assertEquals(delivered, delivered.advance(0).unbox(0))
        assertEquals(DeliveryStage.Delivered, delivered.stage)
    }
    @Test fun instantDeliveryAndUnboxingAreIdempotentAndOpeningEarlyIsRejected() {
        val order = DeliveryProgress("one", 1000, "123456")
        assertFailsWith<IllegalStateException> { order.unbox(1001) }
        val delivered = order.deliverNow(2000)
        assertEquals(DeliveryStage.Delivered, delivered.stage)
        assertEquals(2000, delivered.instantDeliveredAt)
        assertEquals(2000, delivered.deliverNow(3000).instantDeliveredAt)
        assertEquals(2000, delivered.unbox(2000).unbox(3000).unboxedAt)
        assertTrue(DeliveryStage.entries.all { delivered.milestoneTime(it) <= 2000 })
    }
    @Test fun individualUnitsHaveStableIdsAndAllocateTheExactPaidTotal() {
        val lines = listOf(OrderLine("a", "A", "Blue", 3, Money(10001)), OrderLine("b", "B", "Pink", 2, Money(29999)))
        val receipt = Receipt(PendingCheckout("order", "demo", lines, demoAddresses.first(), calculateTotals(lines, "EVERYDAY10"), 0), "Local demo payment")
        val items = purchasedItems(receipt)
        assertEquals(5, items.size)
        assertEquals(5, items.map { it.id }.distinct().size)
        assertEquals(items, purchasedItems(receipt))
        assertEquals(receipt.checkout.totals.total.minorUnits, items.sumOf { it.paid.minorUnits })
        assertTrue(items.all { it.paid.minorUnits > 0 })
    }
}
