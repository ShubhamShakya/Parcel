package com.parcel.presentation

import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.tween
import androidx.compose.foundation.*
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.grid.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.parcel.domain.*

@Composable
fun TrackingScreen(receipt: Receipt?, state: DeliveryState, vm: DeliveryViewModel, back: () -> Unit, unbox: () -> Unit, purchased: () -> Unit, companion: (String) -> Unit = {}) {
    val progress = state.snapshot.orders.find { it.orderId == receipt?.checkout?.id }
    LazyColumn(Modifier.fillMaxSize().testTag("receipt_list"), contentPadding = PaddingValues(20.dp), verticalArrangement = Arrangement.spacedBy(16.dp)) {
        item { ScreenHeading("Your happy delivery", back) }
        if (receipt == null) item { Text("Loading your saved order…") }
        else {
            item {
                Surface(color = Mint, shape = RoundedCornerShape(28.dp)) {
                    Row(Modifier.fillMaxWidth().padding(16.dp), verticalAlignment = Alignment.CenterVertically) {
                        Column(Modifier.weight(1f)) {
                            Text(if (progress?.stage == DeliveryStage.Delivered) "Your parcel is here!" else "Hooray, order confirmed!", style = MaterialTheme.typography.headlineSmall, color = Teal)
                            Text("Order ${receipt.checkout.id.takeLast(8).uppercase()}", style = MaterialTheme.typography.bodySmall)
                            Text(if (receipt.checkout.mode == "demo") "Payment confirmed · no charge" else "PayU test payment confirmed", style = MaterialTheme.typography.bodySmall)
                        }
                        ParcelFriend(Modifier.size(88.dp), greeting = true)
                    }
                }
            }
            if (progress == null) item { Text("Getting your tracking ready…"); TextButton(onClick = vm::reload) { Text("Retry tracking") } }
            else {
                item { DeliveryMap(progress) }
                item {
                    val hours = ((progress.dueAt - progress.observedAt).coerceAtLeast(0) + 3_599_999) / 3_600_000
                    Text(if (progress.stage == DeliveryStage.Delivered) "Delivered with a little sprinkle of joy." else "Arriving in about $hours ${if (hours == 1L) "hour" else "hours"}", style = MaterialTheme.typography.titleMedium)
                    Text(if (progress.stage == DeliveryStage.Delivered) "Delivered ${formatLocalTimestamp(progress.milestoneTime(DeliveryStage.Delivered))}" else "${if (progress.scheduleDurationMs < DELIVERY_DURATION_MS) "Shorter steps for delivery tonight" else "Next stage every hour"} · estimated delivery ${formatLocalTimestamp(progress.dueAt)}. Updates continue while you’re away.", color = Muted, style = MaterialTheme.typography.bodySmall)
                }
                item {
                    Card(modifier = Modifier.fillMaxWidth(), colors = CardDefaults.cardColors(containerColor = ParcelSurface)) {
                        Column(Modifier.padding(20.dp), verticalArrangement = Arrangement.spacedBy(14.dp)) {
                            DeliveryStage.entries.forEach { stage ->
                                val complete = progress.stage.ordinal >= stage.ordinal
                                Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(14.dp)) {
                                    Surface(color = if (complete) Teal else Lavender, shape = RoundedCornerShape(12.dp)) {
                                        Text(if (complete) "✓" else "${stage.ordinal+1}", Modifier.size(32.dp).wrapContentSize(), color = if (complete) OnAccent else Muted)
                                    }
                                    Column {
                                        Text(stage.label, color = if (complete) Teal else Muted, style = MaterialTheme.typography.titleSmall, modifier = Modifier.testTag("tracking_${stage.name}"))
                                        Text(if (progress.stage == stage) "${if (stage == DeliveryStage.Delivered) "Safely arrived" else "You are here"}" else if (complete) "Complete" else "Up next", style = MaterialTheme.typography.bodySmall, color = Muted)
                                        Text("${if (complete) "" else "Expected · "}${formatLocalTimestamp(progress.milestoneTime(stage))}", style = MaterialTheme.typography.bodySmall, color = Muted, modifier = Modifier.testTag("tracking_time_${stage.name}"))
                                    }
                                }
                            }
                        }
                    }
                }
                item {
                    Surface(color = Lavender, shape = RoundedCornerShape(24.dp)) {
                        Column(Modifier.fillMaxWidth().padding(20.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
                            Text(if (progress.stage == DeliveryStage.Delivered) "Delivery OTP · completed" else "Your delivery OTP", style = MaterialTheme.typography.titleMedium)
                            Text(progress.otp.toCharArray().joinToString(" "), style = MaterialTheme.typography.headlineMedium, color = Teal, modifier = Modifier.testTag("delivery_otp"))
                            Text("Show this to your delivery partner when your parcel arrives.", style = MaterialTheme.typography.bodyMedium)
                        }
                    }
                }
                item {
                    when {
                        progress.unboxedAt != null -> Button(onClick = purchased, modifier = Modifier.fillMaxWidth().testTag("see_purchased")) { Text("Visit your shelf") }
                        progress.stage == DeliveryStage.Delivered -> Button(onClick = unbox, modifier = Modifier.fillMaxWidth().heightIn(min = 54.dp).testTag("open_unboxing")) { Text("Unbox your parcel ✨") }
                        else -> Button(onClick = { vm.deliverNow(progress.orderId) }, enabled = !state.busy, modifier = Modifier.fillMaxWidth().heightIn(min = 54.dp).testTag("deliver_now")) { Text("Deliver it instantly ✨") }
                    }
                }
            }
            item {
                CompanionCard("Your delivery partner", "Meet your fictional rider, message or try a demo call") { companion("rider") }
                TextButton(onClick = { companion("order-help") }) { Text("Help with this order") }
                if (progress?.stage == DeliveryStage.Delivered) TextButton(onClick = { companion("delivered") }) { Text("Rate this delivery") }
            }
            item { Text("Your goodies", style = MaterialTheme.typography.titleMedium) }
            items(receipt.checkout.lines) { line -> Text("${line.title} · ${line.variant} × ${line.quantity}") }
            item { Text("Deliver to: ${receipt.checkout.address.display}", color = Muted) }
            item { TotalsCard(receipt.checkout.totals) }
        }
    }
}

@Composable
fun UnboxingScreen(receipt: Receipt?, products: List<Product>, state: DeliveryState, vm: DeliveryViewModel, back: () -> Unit, purchased: () -> Unit) {
    val progress = state.snapshot.orders.find { it.orderId == receipt?.checkout?.id }
    var opening by remember { mutableStateOf(false) }
    val animation = remember { Animatable(0f) }
    val opened = progress?.unboxedAt != null
    LaunchedEffect(opening) {
        if (opening && progress != null) {
            animation.animateTo(1f, tween(1500))
            vm.unbox(progress.orderId)
            opening = false
        }
    }
    Column(Modifier.fillMaxSize().verticalScroll(rememberScrollState()).padding(24.dp).testTag("unboxing_screen"), horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.spacedBy(20.dp)) {
        ScreenHeading("A little box of happy", back)
        Text(if (opened) "Ta-da! They’re yours." else "What’s inside?", style = MaterialTheme.typography.headlineMedium, textAlign = TextAlign.Center)
        Text(if (opened) "Your new favourites are ready for their shelf." else "Tap the parcel and let the happy out.", color = Muted, textAlign = TextAlign.Center)
        Surface(color = Lavender, shape = RoundedCornerShape(40.dp)) {
            ParcelFriend(Modifier.fillMaxWidth().height(280.dp).clickable(enabled = !opened && !opening && !state.busy && progress?.stage == DeliveryStage.Delivered) { opening = true }.testTag("unbox_parcel"), opening = if (opened) 1f else animation.value)
        }
        when {
            opened -> {
                receipt?.checkout?.lines?.forEach { line ->
                    Surface(color = Mint, shape = RoundedCornerShape(24.dp)) {
                        Row(Modifier.fillMaxWidth().padding(12.dp), verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                            products.find { it.id == line.productId }?.let { ProductImage(it, Modifier.size(76.dp).clip(RoundedCornerShape(18.dp))) }
                            Column(Modifier.weight(1f)) {
                                Text("${line.quantity} × ${line.title}", style = MaterialTheme.typography.titleMedium)
                                Text(line.variant, color = Muted, style = MaterialTheme.typography.bodySmall)
                            }
                        }
                    }
                }
                Button(onClick = purchased, modifier = Modifier.fillMaxWidth().heightIn(min = 54.dp).testTag("unboxing_to_shelf")) { Text("Give them a home on your shelf") }
            }
            progress?.stage != DeliveryStage.Delivered -> Text("Your parcel will be ready to open after delivery.", color = Muted)
            else -> Button(onClick = { opening = true }, enabled = !opening && !state.busy, modifier = Modifier.fillMaxWidth().heightIn(min = 54.dp).testTag("start_unboxing")) { Text(if (opening || state.busy) "Unwrapping the happy…" else "Open my parcel") }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun PurchasedScreen(receipts: List<Receipt>, products: List<Product>, state: DeliveryState, vm: DeliveryViewModel, orders: () -> Unit) {
    val ads = LocalParcelAds.current
    val unboxed = state.snapshot.orders.filter { it.unboxedAt != null }.map { it.orderId }.toSet()
    val items = receipts.filter { it.checkout.id in unboxed }.flatMap(::purchasedItems)
    var selectedSlot by rememberSaveable { mutableStateOf<Int?>(null) }
    LazyColumn(Modifier.fillMaxSize().testTag("purchased_list"), contentPadding = PaddingValues(20.dp), verticalArrangement = Arrangement.spacedBy(18.dp)) {
        item {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Column(Modifier.weight(1f)) { Text("My little shelf", style = MaterialTheme.typography.headlineMedium, color = Teal); Text("A home for every happy find.", color = Muted) }
                ParcelFriend(Modifier.size(76.dp), greeting = true)
            }
            Text("${items.size} unboxed · ${state.snapshot.shelf.size}/12 spaces filled", style = MaterialTheme.typography.bodySmall)
            TextButton(onClick = orders, modifier = Modifier.testTag("purchased_orders")) { Text("My orders & deliveries") }
        }
        item(key = "purchased_banner") { ads.Banner(BannerPlacement.Purchased) }
        item {
            Surface(color = Peach, shape = RoundedCornerShape(26.dp), shadowElevation = 2.dp) {
                Column(Modifier.padding(12.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
                    repeat(4) { row ->
                        Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                            repeat(3) { column ->
                                val slot = row*3+column
                                val item = items.find { it.id == state.snapshot.shelf.find { p -> p.slot == slot }?.itemId }
                                val product = products.find { it.id == item?.productId }
                                Surface(onClick = { selectedSlot = slot }, enabled = !state.loading && !state.busy, color = ParcelSurface, shape = RoundedCornerShape(16.dp), modifier = Modifier.weight(1f).height(126.dp).testTag("shelf_slot_$slot")) {
                                    Column(horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.Center, modifier = Modifier.padding(6.dp)) {
                                        if (item == null) { Text("+", style = MaterialTheme.typography.headlineMedium, color = Teal); Text("Space ${slot+1}", style = MaterialTheme.typography.labelSmall, color = Muted) }
                                        else {
                                            if (product != null) ProductImage(product, Modifier.size(68.dp).clip(RoundedCornerShape(12.dp))) else Symbol(Glyph.Bag, modifier = Modifier.size(48.dp))
                                            Text(item.title, maxLines = 2, style = MaterialTheme.typography.labelSmall, textAlign = TextAlign.Center, modifier = Modifier.testTag("shelf_item_${item.id}"))
                                        }
                                    }
                                }
                            }
                        }
                    }
                }
            }
        }
        item { Text("Tap a space to choose an unboxed item. Tap an occupied space to swap, move or put it away.", color = Muted, style = MaterialTheme.typography.bodySmall) }
        if (items.isNotEmpty()) {
            item { Text("Your unboxed finds", style = MaterialTheme.typography.titleLarge) }
            items(items.chunked(2)) { pair ->
                Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                    pair.forEach { item ->
                        Card(modifier = Modifier.weight(1f), colors = CardDefaults.cardColors(containerColor = ParcelSurface)) {
                            products.find { it.id == item.productId }?.let { ProductImage(it, Modifier.fillMaxWidth().height(110.dp)) }
                            Column(Modifier.padding(12.dp), verticalArrangement = Arrangement.spacedBy(4.dp)) {
                                Text(item.title, maxLines = 2, style = MaterialTheme.typography.titleSmall)
                                Text(item.variant, style = MaterialTheme.typography.bodySmall, color = Muted)
                                Text(item.paid.display(), color = Teal, style = MaterialTheme.typography.titleMedium)
                                Text(state.snapshot.shelf.find { it.itemId == item.id }?.let { "On shelf · space ${it.slot+1}" } ?: "Ready for your shelf", style = MaterialTheme.typography.labelSmall, color = Muted)
                            }
                        }
                    }
                    if (pair.size == 1) Spacer(Modifier.weight(1f))
                }
            }
        }
        if (items.isEmpty()) item {
            Surface(color = Mint, shape = RoundedCornerShape(24.dp)) { Column(Modifier.padding(20.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                Text("Your shelf is waiting for its first friend", style = MaterialTheme.typography.titleMedium)
                Text("Unbox a delivered parcel to bring your purchases here.", color = Muted)
                TextButton(onClick = orders) { Text("Find a parcel to open") }
            } }
        }
    }
    selectedSlot?.let { slot ->
        ModalBottomSheet(onDismissRequest = { selectedSlot = null }, sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)) {
            Column(Modifier.fillMaxHeight(.85f).padding(horizontal = 20.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
                Text("Pick a friend for space ${slot+1}", style = MaterialTheme.typography.titleLarge)
                Text("Purchase amount includes your share of discounts, taxes and fees.", style = MaterialTheme.typography.bodySmall, color = Muted)
                if (state.snapshot.shelf.any { it.slot == slot }) TextButton(onClick = { vm.clear(slot); selectedSlot = null }, modifier = Modifier.testTag("clear_shelf_slot")) { Text("Put this item away") }
                if (items.isEmpty()) Text("No unboxed items yet. Open a delivered parcel first.")
                LazyVerticalGrid(columns = GridCells.Fixed(2), modifier = Modifier.weight(1f).testTag("shelf_picker"), horizontalArrangement = Arrangement.spacedBy(12.dp), verticalArrangement = Arrangement.spacedBy(12.dp), contentPadding = PaddingValues(bottom = 24.dp)) {
                    items(items, key = { it.id }) { item ->
                        Card(onClick = { vm.place(slot, item.id); selectedSlot = null }, enabled = !state.busy, modifier = Modifier.testTag("pick_${item.id}"), colors = CardDefaults.cardColors(containerColor = ParcelSurface)) {
                            val product = products.find { it.id == item.productId }
                            if (product != null) ProductImage(product, Modifier.fillMaxWidth().height(115.dp))
                            Column(Modifier.padding(12.dp), verticalArrangement = Arrangement.spacedBy(5.dp)) {
                                Text(item.title, maxLines = 2, style = MaterialTheme.typography.titleSmall)
                                Text(item.variant, style = MaterialTheme.typography.bodySmall, color = Muted)
                                Text(item.paid.display(), color = Teal, style = MaterialTheme.typography.titleMedium)
                                state.snapshot.shelf.find { it.itemId == item.id }?.let { Text("Move from space ${it.slot+1}", style = MaterialTheme.typography.labelSmall) }
                            }
                        }
                    }
                }
            }
        }
    }
}
