package com.parcel.presentation

import androidx.compose.foundation.*
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalClipboardManager
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.unit.dp
import com.parcel.domain.*
import kotlinx.coroutines.delay
import kotlin.time.Clock

@Composable internal fun CompanionPage(title: String, back: () -> Unit, content: @Composable ColumnScope.() -> Unit) {
    Column(Modifier.fillMaxSize().verticalScroll(rememberScrollState()).padding(20.dp).testTag("companion_page"), verticalArrangement = Arrangement.spacedBy(16.dp)) {
        ScreenHeading(title, back)
        content()
        Spacer(Modifier.height(16.dp))
    }
}
@Composable internal fun CompanionCard(title: String, subtitle: String, action: (() -> Unit)? = null) {
    Surface(shape = RoundedCornerShape(18.dp), color = ParcelSurface, modifier = Modifier.fillMaxWidth().then(if (action != null) Modifier.clickable(onClick = action) else Modifier)) {
        Row(Modifier.padding(18.dp), verticalAlignment = Alignment.CenterVertically) {
            Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(6.dp)) {
                Text(title, style = MaterialTheme.typography.titleMedium)
                Text(subtitle, style = MaterialTheme.typography.bodyMedium, color = Muted)
            }
            if (action != null) Text(" ›", style = MaterialTheme.typography.titleLarge)
        }
    }
}
@Composable private fun Choices(options: List<String>, selected: String, choose: (String) -> Unit) {
    Row(Modifier.horizontalScroll(rememberScrollState()), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
        options.forEach { FilterChip(selected == it, onClick = { choose(it) }, label = { Text(it) }) }
    }
}

/** All pages are backed by the existing catalog, receipts and Room companion state. */
@Composable fun CompanionScreen(page: String, id: String?, state: CompanionState, vm: CompanionViewModel,
    profile: ShopperProfile?, commerce: CommerceState, commerceVm: CommerceViewModel,
    products: List<Product>, delivery: DeliverySnapshot, back: () -> Unit, navigate: (String) -> Unit) {
    val data = state.snapshot
    val receipt = commerce.snapshot.receipts.find { it.checkout.id == id }
    val progress = delivery.orders.find { it.orderId == id }
    val product = products.find { it.id == id }
    val clipboard = LocalClipboardManager.current
    val ready = !state.loading && !state.busy
    val title = mapOf("edit-profile" to "Edit profile", "notifications" to "Notifications", "notification-settings" to "Notification settings",
        "payments" to "Payment methods", "add-card" to "Add a demo method", "coupons" to "Coupons for you", "location" to "Delivery location",
        "share" to "Share this find", "size-guide" to "Find your fit", "buy-again" to "Buy again", "returns" to "Return or exchange",
        "delivered" to "A happy delivery", "rider" to "Your delivery partner", "call" to "Demo call", "chat" to "Delivery chat",
        "help" to "How can we help?", "order-help" to "Help with an order", "support-chat" to "Parcel support", "privacy" to "Your privacy", "terms" to "Using Parcel")[page] ?: "Parcel"
    CompanionPage(title, back) {
        if (state.loading) { CircularProgressIndicator(); return@CompanionPage }
        state.error?.let { Text(it, color = MaterialTheme.colorScheme.error); TextButton(onClick = { vm.dismiss(); vm.reload() }) { Text("Dismiss and retry") } }
        if (state.loadError) return@CompanionPage
        state.message?.let { message -> ParcelDialog(onDismissRequest = vm::dismiss,
            title = { Text(if (page == "edit-profile") "Profile updated" else "All done") },
            text = { Text(message) },
            confirmButton = { Button(onClick = vm::dismiss, modifier = Modifier.testTag("companion_notice_ok")) { Text("Got it") } }) }
        when (page) {
            "edit-profile" -> {
                var first by rememberSaveable(profile?.phone) { mutableStateOf(profile?.firstName.orEmpty()) }
                var last by rememberSaveable(profile?.phone) { mutableStateOf(profile?.lastName.orEmpty()) }
                var email by rememberSaveable(profile?.phone) { mutableStateOf(data.email) }
                var birthday by rememberSaveable(profile?.phone) { mutableStateOf(data.birthday) }
                CompanionCard("Your local profile", profile?.phone.orEmpty() + " · Phone changes require signing out and verifying another number.")
                OutlinedTextField(first, { first = it }, label = { Text("First name") }, modifier = Modifier.fillMaxWidth().testTag("edit_first_name"), singleLine = true)
                OutlinedTextField(last, { last = it }, label = { Text("Last name") }, modifier = Modifier.fillMaxWidth(), singleLine = true)
                OutlinedTextField(email, { email = it }, label = { Text("Email · optional") }, modifier = Modifier.fillMaxWidth(), singleLine = true)
                OutlinedTextField(birthday, { birthday = it }, label = { Text("Birthday · YYYY-MM-DD · optional") }, modifier = Modifier.fillMaxWidth(), singleLine = true)
                Text("These details stay on this device. No verification email is sent.", color = Muted)
                Button(onClick = { vm.run { saveProfile(first, last, email, birthday) } }, enabled = ready, modifier = Modifier.fillMaxWidth().testTag("save_profile")) { Text("Save changes") }
            }
            "notifications" -> {
                var filter by rememberSaveable { mutableStateOf("All") }
                Choices(listOf("All", "Orders", "Offers", "Account"), filter) { filter = it }
                val notifications = buildList {
                    if (data.showOrders) delivery.orders.sortedByDescending { it.confirmedAt }.forEach { order ->
                        DeliveryStage.entries.filter { it.ordinal <= order.stage.ordinal }.reversed().forEach { stage ->
                            add(InboxItem("${order.orderId}:${stage.name}", "Orders", stage.label, "Order ${order.orderId.takeLast(8).uppercase()}", "receipt/${order.orderId}"))
                        }
                    }
                    if (data.showOffers) shoppingOffers.forEach { add(InboxItem("offer:${it.code}", "Offers", it.title, it.description, "companion/coupons")) }
                    add(InboxItem("welcome", "Account", "Welcome to Parcel", "Your shopping world, saved on this device.", "companion/edit-profile"))
                }
                val unread = notifications.count { it.key !in data.readNotifications }
                Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
                    Text(if (unread == 0) "You’re all caught up" else "$unread unread", Modifier.weight(1f), color = Muted)
                    TextButton(onClick = { vm.run(null) { markRead(notifications.map { it.key }.toSet()) } }, enabled = ready && unread > 0) { Text("Mark all read") }
                }
                TextButton(onClick = { navigate("companion/notification-settings") }) { Text("Manage notifications") }
                val visible = notifications.filter { filter == "All" || it.category == filter }
                if (visible.isEmpty()) CompanionCard("Nothing here yet", "Your updates will appear here.")
                visible.forEach { n -> CompanionCard((if (n.key !in data.readNotifications) "●  " else "") + n.title, n.body) {
                    vm.run(null) { markRead(setOf(n.key)) }; navigate(n.route)
                } }
            }
            "notification-settings" -> {
                CompanionCard("Your in-app inbox", "Choose what appears in Notifications. These controls do not send SMS, email or WhatsApp messages.")
                Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) { Text("Order updates", Modifier.weight(1f)); Switch(data.showOrders, { vm.run(null) { notificationSettings(it, data.showOffers) } }, enabled = ready) }
                Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) { Text("Offers and discoveries", Modifier.weight(1f)); Switch(data.showOffers, { vm.run(null) { notificationSettings(data.showOrders, it) } }, enabled = ready) }
                Text("Account messages remain available. System push notifications and quiet hours are not connected yet.", color = Muted)
            }
            "payments" -> {
                CompanionCard("Practice, without paying", "Saved methods are fictional presets for this screen. Checkout uses local confirmation or configured PayU sandbox; these presets are not sent to PayU.")
                if (data.paymentPresets.isEmpty()) Text("No demo methods saved yet.", color = Muted)
                data.paymentPresets.forEach { preset -> CompanionCard(preset, "Demo method · no financial account linked"); OutlinedButton(onClick = { vm.run { paymentPreset(preset, false) } }, enabled = ready) { Text("Remove $preset") } }
                Button(onClick = { navigate("companion/add-card") }, modifier = Modifier.fillMaxWidth()) { Text("Add demo method") }
                CompanionCard("PayU sandbox", if (commerceVm.payUConfigured) "Configured · available from checkout" else "Setup required · test merchant credentials and backend needed")
            }
            "add-card" -> {
                CompanionCard("A safe practice wallet", "Choose a fictional preset. Parcel never asks for or stores your card number, CVV, UPI PIN or bank credentials.")
                demoPaymentPresets.forEach { preset ->
                    Surface(shape = RoundedCornerShape(20.dp), color = if (preset.contains("UPI")) Mint else Teal) {
                        Column(Modifier.fillMaxWidth().padding(24.dp), verticalArrangement = Arrangement.spacedBy(18.dp)) {
                            Text("PARCEL · DEMO", color = if (preset.contains("UPI")) Ink else OnAccent)
                            Text(preset, style = MaterialTheme.typography.titleLarge, color = if (preset.contains("UPI")) Ink else OnAccent)
                        }
                    }
                    Button(onClick = { vm.run { paymentPreset(preset, true) } }, enabled = ready && preset !in data.paymentPresets) { Text(if (preset in data.paymentPresets) "Saved" else "Save demo method") }
                }
            }
            "coupons" -> {
                Text("One offer per order. Eligible savings are calculated at checkout.", color = Muted)
                shoppingOffers.forEach { offer ->
                    CompanionCard(offer.code + " · " + offer.title, offer.description + "\nMinimum spend ${Money(offer.minimum).display()}")
                    Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                        OutlinedButton(onClick = { clipboard.setText(AnnotatedString(offer.code)) }) { Text("Copy code") }
                        Button(onClick = { commerceVm.offer(offer) }, enabled = !commerce.busy && commerce.snapshot.pending == null) { Text(if (commerce.snapshot.offerCode == offer.code) "Selected" else "Use offer") }
                    }
                }
                if (commerce.snapshot.offerCode != null) TextButton(onClick = { commerceVm.offer(null) }, enabled = !commerce.busy && commerce.snapshot.pending == null) { Text("Remove selected offer") }
            }
            "location" -> {
                var pin by rememberSaveable { mutableStateOf("") }
                var checked by rememberSaveable { mutableStateOf(false) }
                CompanionCard("Around Connaught Place", "Delivery locations are simulated in New Delhi. No GPS access is needed.")
                OutlinedTextField(pin, { pin = it; checked = false }, label = { Text("Delhi pincode") }, singleLine = true, modifier = Modifier.fillMaxWidth())
                Button(onClick = { checked = true }) { Text("Check area") }
                if (checked) Text(if (Regex("110[0-9]{3}").matches(pin)) "This pincode is available for simulated delivery. Add an address below." else "Enter a six-digit Delhi pincode beginning with 110.")
                commerce.snapshot.addresses.forEach { address -> CompanionCard(address.label + if (address.id == commerce.snapshot.selectedAddressId) " · Selected" else "", address.display) { commerceVm.selectAddress(address.id) } }
                Button(onClick = { navigate("addresses") }) { Text("Manage saved addresses") }
            }
            "share" -> {
                if (product == null) Text("This product is unavailable.") else {
                    ProductImage(product, Modifier.fillMaxWidth().height(220.dp))
                    CompanionCard(product.title, product.price.display() + " · " + product.subtitle)
                    val description = "${product.title} — ${product.price.display()} in Parcel, a shopping simulator. Product reference: ${product.id}. No real purchase or delivery."
                    val share = rememberProductShare(description)
                    Button(onClick = share, modifier = Modifier.fillMaxWidth()) { Text("Share product") }
                    OutlinedButton(onClick = { clipboard.setText(AnnotatedString(description)) }) { Text("Copy description") }
                    Text("Paste the description into an app of your choice. Public shopping links and referral rewards are not available.", color = Muted)
                }
            }
            "size-guide" -> SizeGuideContent(product)
            "buy-again" -> {
                Text("Revisit your delivered finds. Current prices apply; add the original variant or choose another on its product page.", color = Muted)
                val lines = commerce.snapshot.receipts.filter { r -> delivery.orders.any { it.orderId == r.checkout.id && it.stage == DeliveryStage.Delivered } }.flatMap { it.checkout.lines }.distinctBy { it.productId to it.variant }
                if (lines.isEmpty()) CompanionCard("Your next favourite is waiting", "Delivered purchases will appear here.")
                lines.forEach { line ->
                    val current = products.find { it.id == line.productId }
                    CompanionCard(line.title, line.variant + " · " + (current?.price?.display() ?: "Unavailable")) { navigate("product/${line.productId}") }
                    Button(onClick = { current?.let { commerceVm.add(it, line.variant) } }, enabled = current != null && line.variant in current.variants && !commerce.busy && commerce.snapshot.pending == null) { Text("Add one to cart") }
                }
                OutlinedButton(onClick = { navigate("cart") }) { Text("View cart") }
            }
            "help" -> HelpContent(navigate)
            "order-help" -> {
                if (receipt == null) {
                    if (commerce.snapshot.receipts.isEmpty()) CompanionCard("No orders yet", "Place an order to see help options here.")
                    commerce.snapshot.receipts.forEach { r -> CompanionCard("Order ${r.checkout.id.takeLast(8).uppercase()}", r.checkout.lines.joinToString { it.title }) { navigate("companion/order-help/${r.checkout.id}") } }
                } else {
                    CompanionCard("Order ${id?.takeLast(8)?.uppercase()}", progress?.stage?.label ?: "Getting tracking ready")
                    listOf("Delivery", "Payment", "Missing item", "Return").forEach { issue -> CompanionCard(issue, "Save a simulated callback request") { vm.run("Demo request saved. No real callback will be made.") { requestCallback(receipt.checkout.id, issue) } } }
                    data.callbacks[id]?.let { CompanionCard("Request saved · $it", "Offline demonstration · no call is scheduled") }
                    OutlinedButton(onClick = { navigate("companion/returns/$id") }) { Text("Return or exchange") }
                    Button(onClick = { navigate("companion/support-chat") }) { Text("Chat with offline support") }
                }
            }
            "support-chat", "chat" -> {
                if (page == "chat" && receipt == null) Text("Select an order to chat about its delivery.") else {
                    val thread = if (page == "chat") "rider:$id" else "support"
                    CompanionCard("Offline conversation", "Scripted replies for the simulator. Messages are saved locally; no person receives them.")
                    data.conversations[thread].orEmpty().forEach { line ->
                        Surface(shape = RoundedCornerShape(16.dp), color = if (line.fromShopper) Mint else ParcelSurface, modifier = Modifier.fillMaxWidth().padding(start = if (line.fromShopper) 24.dp else 0.dp, end = if (line.fromShopper) 0.dp else 24.dp)) {
                            Column(Modifier.padding(14.dp)) { Text(if (line.fromShopper) "You" else "Parcel assistant", style = MaterialTheme.typography.labelMedium); Text(line.text) }
                        }
                    }
                    listOf("Where is my order?", "Help with a return", "Payment question").forEach { quick -> OutlinedButton(onClick = { vm.run(null) { sendMessage(thread, quick) } }, enabled = ready) { Text(quick) } }
                    var draft by rememberSaveable(thread) { mutableStateOf("") }
                    OutlinedTextField(draft, { draft = it.take(500) }, label = { Text("Your message") }, modifier = Modifier.fillMaxWidth().testTag("chat_message"))
                    Button(onClick = { val text = draft; vm.run(null) { sendMessage(thread, text); draft = "" } }, enabled = ready && draft.isNotBlank(), modifier = Modifier.testTag("send_chat")) { Text("Send") }
                }
            }
            "rider" -> {
                if (receipt == null) Text("Order not found.") else {
                    val names = listOf("Aarav", "Meera", "Kabir", "Anaya")
                    val name = names[(receipt.checkout.id.hashCode().toLong().let { if (it < 0) -it else it } % 4).toInt()]
                    Surface(color = Mint, shape = RoundedCornerShape(24.dp)) { Column(Modifier.fillMaxWidth().padding(28.dp), horizontalAlignment = Alignment.CenterHorizontally) { ParcelFriend(Modifier.size(110.dp)); Text(name, style = MaterialTheme.typography.headlineMedium); Text("Fictional delivery partner · New Delhi", color = Muted) } }
                    CompanionCard("Your route", progress?.stage?.label ?: "Preparing your parcel")
                    Button(onClick = { navigate("companion/chat/$id") }, modifier = Modifier.fillMaxWidth()) { Text("Message partner") }
                    OutlinedButton(onClick = { navigate("companion/call/$id") }, modifier = Modifier.fillMaxWidth()) { Text("Try a simulated call") }
                }
            }
            "call" -> DemoCallContent()
            "delivered" -> {
                if (progress?.stage != DeliveryStage.Delivered) CompanionCard("A little patience", "Rating is available once this order is delivered.") else {
                    ParcelFriend(Modifier.align(Alignment.CenterHorizontally).size(120.dp), greeting = true)
                    CompanionCard("Your parcel has arrived", "How was your simulated delivery? Your rating stays on this device.")
                    var stars by rememberSaveable(id) { mutableStateOf(data.ratings[id] ?: 5) }
                    Choices((1..5).map { "$it ★" }, "$stars ★") { stars = it.take(1).toInt() }
                    Button(onClick = { vm.run("Thanks! Your rating was saved.") { rate(progress.orderId, stars) } }, enabled = ready, modifier = Modifier.fillMaxWidth()) { Text("Save rating") }
                    OutlinedButton(onClick = { navigate("unbox/$id") }) { Text("Open your parcel") }
                    Text("No real rider or tip payment is involved.", color = Muted)
                }
            }
            "returns" -> ReturnContent(receipt, progress, products, data, ready, vm)
            "privacy" -> {
                CompanionCard("Your data stays close", "Parcel stores your profile, phone number, optional email and birthday, addresses, shopping history, chats and preferences in a local database on this device.")
                CompanionCard("Shared device", "Cart, wishlist, addresses, orders and shelf are shared across profiles on this installation. Companion settings and messages are associated with the signed-in phone number. This demo login is not secure identity verification.")
                CompanionCard("Payments and connections", "Local checkout makes no payment. If configured, PayU sandbox and its backend receive checkout information for test processing. Do not enter real financial credentials.")
                CompanionCard("Control your information", "Edit your profile, delete addresses and remove demo payment presets in the app. Clearing Parcel’s app storage in Android settings removes this installation’s local data; uninstall behaviour can depend on device backup settings.")
            }
            "terms" -> {
                CompanionCard("A shopping simulator", "Parcel is for browsing and practicing a shopping journey. Products, reviews, offers, delivery partners and interactions are illustrative. Orders do not create purchases or physical shipments.")
                CompanionCard("No real charges or refunds", "Demo methods, callbacks, return requests and delivery ratings exist only within this simulator. PayU is sandbox-only. No real payment, tip or refund is promised.")
                CompanionCard("Local demonstration", "Any four non-space OTP characters verify a demo login. Do not use this app for sensitive information. Data may be lost if local storage is cleared.")
            }
        }
    }
}
private data class InboxItem(val key: String, val category: String, val title: String, val body: String, val route: String)

@Composable private fun HelpContent(navigate: (String) -> Unit) {
    var query by rememberSaveable { mutableStateOf("") }
    OutlinedTextField(query, { query = it }, label = { Text("Search help") }, modifier = Modifier.fillMaxWidth(), singleLine = true)
    val faqs = listOf("When will my order arrive?" to "Simulated orders normally advance every hour and arrive in four hours. Late-night orders randomly keep that schedule or use shorter steps to arrive before midnight. Your tracking page shows the saved times. Deliver it instantly skips the wait.",
        "Will I be charged?" to "Local checkout has no charge. PayU checkout uses sandbox test mode only.", "How do returns work?" to "You can save a simulated return request within 10 days of delivery from Help with an order. No real pickup or refund takes place.",
        "How do I unbox a product?" to "Once delivered, tap Unbox your parcel on tracking. Your unboxed items appear in Purchased, ready for your shelf.", "How do coupons work?" to "Choose one coupon. Checkout calculates its minimum spend, discount, tax and fee rules.")
    val visible = faqs.filter { query.isBlank() || (it.first + it.second).contains(query, true) }
    if (visible.isEmpty()) Text("No matching answers. Try ‘delivery’, ‘payment’ or ‘return’.", color = Muted)
    visible.forEach { (q, a) -> var expanded by rememberSaveable(q) { mutableStateOf(false) }; CompanionCard(q, if (expanded) a else "Tap to read") { expanded = !expanded } }
    Button(onClick = { navigate("companion/order-help") }, modifier = Modifier.fillMaxWidth()) { Text("Help with an order") }
    OutlinedButton(onClick = { navigate("companion/support-chat") }, modifier = Modifier.fillMaxWidth()) { Text("Chat with offline support") }
}

@Composable private fun SizeGuideContent(product: Product?) {
    val clothing = product?.clothing
    if (clothing == null) { Text("This product has no clothing size guide."); return }
    var unit by rememberSaveable { mutableStateOf("cm") }
    CompanionCard(product.title, "Available sizes: ${clothing.sizes.joinToString()}. Illustrative body-measurement guide; product-specific tailoring may vary.")
    Choices(listOf("cm", "inches"), unit) { unit = it }
    val measurements = demoSizeMeasurements(clothing)
    if (measurements.isEmpty()) { Text("This item does not have a fitted size chart. See its description for fit information."); return }
    fun display(n: Double) = if (unit == "cm") n.toInt().toString() else ((n / 2.54 * 10).toInt() / 10.0).toString()
    measurements.forEach { measurement ->
        CompanionCard(measurement.label, (measurement.chestCm?.let { "${display(it)} $unit chest / bust · " } ?: "") + "${display(measurement.waistCm)} $unit waist")
    }
    Text("Measure around the fullest part of your chest and natural waist. These are demonstration measurements, not brand specifications.", color = Muted)
    var waist by rememberSaveable { mutableStateOf("") }
    var result by rememberSaveable { mutableStateOf<String?>(null) }
    OutlinedTextField(waist, { waist = it; result = null }, label = { Text("Your waist measurement in cm") }, singleLine = true, modifier = Modifier.fillMaxWidth())
    Button(onClick = {
        val number = waist.toDoubleOrNull()
        result = if (number == null || !number.isFinite() || number !in 30.0..200.0) "Enter a waist measurement between 30 and 200 cm."
        else suggestedDemoSize(measurements, number)?.let { "Try $it in this illustrative guide. Check the measurements above." } ?: "No available size matches this measurement in the demo guide."
    }) { Text("Find a suggested size") }
    result?.let { Text(it, color = Teal) }

}

@Composable private fun DemoCallContent() {
    var active by rememberSaveable { mutableStateOf(false) }
    var seconds by rememberSaveable { mutableIntStateOf(0) }
    var muted by rememberSaveable { mutableStateOf(false) }
    var speaker by rememberSaveable { mutableStateOf(false) }
    LaunchedEffect(active) { while (active) { delay(1000); seconds++ } }
    ParcelFriend(Modifier.size(120.dp).fillMaxWidth(), greeting = active)
    CompanionCard(if (active) "Connected · ${seconds / 60}:${(seconds % 60).toString().padStart(2, '0')}" else "Demo delivery call", "No phone call is made. No microphone is accessed. Controls and captions demonstrate the call experience.")
    if (active) {
        CompanionCard("Live demo caption", if (seconds < 4) "Hello! I’m your fictional Parcel partner." else if (seconds < 10) "Your delivery progress is available on the tracking screen." else "Thanks for trying Parcel. You can end this demo whenever you like.")
        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) { FilterChip(muted, { muted = !muted }, label = { Text(if (muted) "Muted" else "Mute") }); FilterChip(speaker, { speaker = !speaker }, label = { Text("Speaker") }) }
    }
    Button(onClick = { active = !active; if (active) seconds = 0 }, modifier = Modifier.fillMaxWidth()) { Text(if (active) "End demo call" else "Start demo call") }
}

@Composable private fun ReturnContent(receipt: Receipt?, progress: DeliveryProgress?, products: List<Product>, data: CompanionSnapshot, ready: Boolean, vm: CompanionViewModel) {
    if (receipt == null) { Text("Choose an order from Help with an order."); return }
    val error = returnEligibility(progress, Clock.System.now().toEpochMilliseconds())
    if (error != null) { CompanionCard("Return eligibility", error); return }
    var lineIndex by rememberSaveable(receipt.checkout.id) { mutableIntStateOf(0) }
    var reason by rememberSaveable { mutableStateOf(returnReasons.first()) }
    var mode by rememberSaveable { mutableStateOf("Return") }
    var size by rememberSaveable { mutableStateOf("") }
    var review by rememberSaveable { mutableStateOf(false) }
    val line = receipt.checkout.lines[lineIndex]
    val clothing = products.find { it.id == line.productId }?.clothing
    val existing = data.returns.find { it.orderId == receipt.checkout.id && it.lineIndex == lineIndex }
    CompanionCard("10-day simulated returns", "Requests are saved locally. No real pickup, exchange or refund is arranged. Your shelf remains unchanged.")
    if (!review) {
        receipt.checkout.lines.forEachIndexed { index, item -> FilterChip(lineIndex == index, { lineIndex = index; size = ""; mode = "Return" }, label = { Text(item.title + " · " + item.variant) }) }
        if (clothing?.type?.group == ClothingGroup.Innerwear) CompanionCard("Not eligible for returns", "Innerwear is excluded from simulated returns.") else if (existing != null) CompanionCard("Request recorded", "${existing.reason} · ${existing.exchangeSize?.let { "Exchange for $it" } ?: "Return"}") else {
            Choices(returnReasons, reason) { reason = it }
            Choices(if (clothing != null) listOf("Return", "Exchange") else listOf("Return"), mode) { mode = it }
            if (mode == "Exchange") Choices(clothing?.sizes.orEmpty(), size) { size = it }
            Button(onClick = { review = true }, enabled = mode == "Return" || size.isNotBlank()) { Text("Review request") }
        }
    } else {
        CompanionCard(line.title, "$reason · $mode" + if (mode == "Exchange") " for size $size" else "")
        CompanionCard("Pickup address · simulation", receipt.checkout.address.display)
        Text("${line.quantity} item(s). No money will be refunded; the original order is a simulation.", color = Muted)
        if (existing != null) CompanionCard("Request recorded", "Your request is saved on this device.") else Button(onClick = { vm.run("Simulated request recorded. No pickup or refund is scheduled.") { requestReturn(ReturnRequest(receipt.checkout.id, lineIndex, reason, if (mode == "Exchange") size else null)) } }, enabled = ready) { Text("Confirm simulated request") }
        TextButton(onClick = { review = false }) { Text("Back to items") }
    }
}
