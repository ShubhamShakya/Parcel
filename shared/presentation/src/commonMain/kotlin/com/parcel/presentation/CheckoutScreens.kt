package com.parcel.presentation

import androidx.compose.foundation.*
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.ui.unit.dp
import com.parcel.domain.*

@Composable
fun ScreenHeading(title: String, back: (() -> Unit)? = null) {
    Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
        if (back != null) IconButton(onClick = back) { Symbol(Glyph.Back, "Back") }
        Text(title, style = MaterialTheme.typography.headlineSmall, modifier = Modifier.weight(1f))
    }
}

@Composable
fun CartScreen(products: List<Product>, state: CommerceState, vm: CommerceViewModel, checkout: () -> Unit, orders: () -> Unit) {
    val ads = LocalParcelAds.current
    val snapshot = state.snapshot
    Scaffold(containerColor = Ivory, contentWindowInsets = WindowInsets(0), bottomBar = {
        if (snapshot.cart.isNotEmpty() && products.isNotEmpty()) Surface(color = ParcelSurface) {
            Row(Modifier.fillMaxWidth().padding(16.dp), verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                Column {
                    Text(calculateTotals(cartLines(snapshot.cart, products), snapshot.offerCode).total.display(), style = MaterialTheme.typography.titleLarge)
                    Text("Including taxes & fees", color = Muted, style = MaterialTheme.typography.labelSmall)
                }
                Button(onClick = checkout, enabled = !state.busy && !state.loadError && !state.loading, modifier = Modifier.weight(1f).heightIn(min = 52.dp).testTag("cart_checkout")) { Text(if (snapshot.pending == null) "Checkout" else "Resume checkout") }
            }
        }
    }) { insets ->
    LazyColumn(Modifier.fillMaxSize().padding(insets).testTag("cart_list"), contentPadding = PaddingValues(20.dp), verticalArrangement = Arrangement.spacedBy(16.dp)) {
        item { ScreenHeading("Your cart") }
        item(key = "cart_banner") { ads.Banner(BannerPlacement.Cart) }
        if (state.loadError) item { Button(onClick = vm::reload) { Text("Retry loading cart") } }
        else if (state.loading) item { CircularProgressIndicator() }
        else if (snapshot.cart.isEmpty()) item { Text("Your cart is waiting for a good find.", style = MaterialTheme.typography.titleMedium) }
        items(snapshot.cart, key = { it.key }) { item ->
            val product = products.find { it.id == item.productId }
            if (product != null) Surface(shape = RoundedCornerShape(16.dp)) {
                Column(Modifier.padding(14.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    Row(horizontalArrangement = Arrangement.spacedBy(14.dp)) {
                        ProductImage(product, Modifier.size(88.dp))
                        Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(4.dp)) {
                            Text(product.title, style = MaterialTheme.typography.titleSmall)
                            Text(item.variant, color = Muted)
                            Text(product.price.display(), style = MaterialTheme.typography.titleMedium)
                        }
                    }
                    Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
                        OutlinedButton(onClick = { vm.quantity(item, item.quantity - 1) }, enabled = !state.busy && snapshot.pending == null, modifier = Modifier.testTag("decrease_${item.productId}")) { Text("−") }
                        Text("${item.quantity}", Modifier.padding(horizontal = 16.dp).testTag("quantity_${item.productId}"))
                        OutlinedButton(onClick = { vm.quantity(item, item.quantity + 1) }, enabled = !state.busy && snapshot.pending == null && item.quantity < 10, modifier = Modifier.testTag("increase_${item.productId}")) { Text("+") }
                        Spacer(Modifier.weight(1f))
                        TextButton(onClick = { vm.quantity(item, 0) }, enabled = !state.busy && snapshot.pending == null) { Text("Remove") }
                    }
                }
            }
        }
        if (snapshot.cart.isNotEmpty() && products.isNotEmpty()) {
            item { TotalsCard(calculateTotals(cartLines(snapshot.cart, products), snapshot.offerCode)) }

        }
        item { TextButton(onClick = orders) { Text("View orders (${snapshot.receipts.size})") } }
    }
}

}

@Composable
fun TotalsCard(totals: CheckoutTotals) {
    Surface(shape = RoundedCornerShape(16.dp), modifier = Modifier.fillMaxWidth().testTag("totals_card")) {
        Column(Modifier.padding(18.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
            Text("Price details", style = MaterialTheme.typography.titleMedium)
            PriceLine("Items subtotal", totals.subtotal)
            PriceLine("Offer discount", totals.discount, minus = true)
            PriceLine("Tax · 18%", totals.tax)
            PriceLine("Convenience fee", totals.convenienceFee)
            PriceLine("Delivery", Money(0))
            HorizontalDivider()
            PriceLine("Total", totals.total)
            Text("Tax is calculated on discounted items. A convenience fee of ₹29 applies per order.", style = MaterialTheme.typography.bodySmall, color = Muted)
        }
    }
}
@Composable
private fun PriceLine(label: String, amount: Money, minus: Boolean = false) {
    Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
        Text(label, modifier = Modifier.weight(1f), style = MaterialTheme.typography.bodyMedium)
        Text((if (minus && amount.minorUnits > 0) "−" else "") + amount.display(), style = MaterialTheme.typography.titleSmall)
    }
}

@Composable
fun AddressScreen(state: CommerceState, vm: CommerceViewModel, back: () -> Unit, next: () -> Unit) {
    var editor by remember { mutableStateOf<DemoAddress?>(null) }
    var deleting by remember { mutableStateOf<DemoAddress?>(null) }
    val snapshot = state.snapshot
    val locked = state.busy || snapshot.pending != null
    LazyColumn(Modifier.fillMaxSize().testTag("address_list"), contentPadding = PaddingValues(20.dp), verticalArrangement = Arrangement.spacedBy(14.dp)) {
        item { ScreenHeading("Delivery address", back) }
        item { Text("Choose an address near Connaught Place, Delhi. Delivery is simulated; use fictional details only.", color = Muted) }
        item { OutlinedButton(onClick = { editor = DemoAddress(newLocalId(), "", "Demo shopper", "", "Connaught Place", "110001") }, enabled = !locked,
            modifier = Modifier.fillMaxWidth().testTag("add_address")) { Text("Add address") } }
        items(snapshot.addresses, key = { it.id }) { address ->
            Surface(shape = RoundedCornerShape(16.dp), border = BorderStroke(if (snapshot.selectedAddressId == address.id) 2.dp else 1.dp,
                if (snapshot.selectedAddressId == address.id) Teal else Color(0xFF64718C)), modifier = Modifier.fillMaxWidth()) {
                Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        RadioButton(selected = snapshot.selectedAddressId == address.id, onClick = { vm.selectAddress(address.id) }, enabled = !locked,
                            modifier = Modifier.testTag("select_address_${address.id}"))
                        Text(address.label, style = MaterialTheme.typography.titleMedium)
                    }
                    Text(address.recipient)
                    Text(address.display, color = Muted)
                    Row {
                        TextButton(onClick = { editor = address }, enabled = !locked, modifier = Modifier.testTag("edit_address_${address.id}")) { Text("Edit") }
                        TextButton(onClick = { deleting = address }, enabled = !locked, modifier = Modifier.testTag("delete_address_${address.id}")) { Text("Delete") }
                    }
                }
            }
        }
        if (snapshot.addresses.isEmpty()) item { Text("Add a demo address to continue.") }
        item { Button(onClick = next, enabled = !state.busy && snapshot.addresses.any { it.id == snapshot.selectedAddressId },
            modifier = Modifier.fillMaxWidth().heightIn(min = 52.dp).testTag("address_continue")) { Text("Continue to checkout") } }
    }
    editor?.let { address -> AddressEditor(address, state.busy, { editor = null }) { updated ->
        vm.saveAddress(updated); editor = null
    } }
    deleting?.let { address -> ParcelDialog(onDismissRequest = { deleting = null }, title = { Text("Delete address?") },
        text = { Text(address.label) }, confirmButton = { TextButton(onClick = { vm.deleteAddress(address.id); deleting = null }, modifier = Modifier.testTag("confirm_delete_address")) { Text("Delete") } },
        dismissButton = { TextButton(onClick = { deleting = null }) { Text("Keep") } }) }
}

@Composable
private fun AddressEditor(address: DemoAddress, busy: Boolean, dismiss: () -> Unit, save: (DemoAddress) -> Unit) {
    var label by rememberSaveable(address.id) { mutableStateOf(address.label) }
    var recipient by rememberSaveable(address.id) { mutableStateOf(address.recipient) }
    var line by rememberSaveable(address.id) { mutableStateOf(address.line) }
    var locality by rememberSaveable(address.id) { mutableStateOf(address.locality) }
    var postal by rememberSaveable(address.id) { mutableStateOf(address.postalCode) }
    var error by remember { mutableStateOf<String?>(null) }
    ParcelDialog(onDismissRequest = dismiss, title = { Text("Edit address") }, text = {
        Column(Modifier.imePadding(), verticalArrangement = Arrangement.spacedBy(10.dp)) {
            Text("Use fictional details only. City: New Delhi.", style = MaterialTheme.typography.bodySmall)
            OutlinedTextField(label, { label = it.take(150) }, label = { Text("Label") }, singleLine = true, modifier = Modifier.testTag("address_label"))
            OutlinedTextField(recipient, { recipient = it.take(150) }, label = { Text("Recipient") }, singleLine = true, modifier = Modifier.testTag("address_recipient"))
            OutlinedTextField(line, { line = it.take(150) }, label = { Text("Building / street") }, modifier = Modifier.testTag("address_line"))
            OutlinedTextField(locality, { locality = it.take(150) }, label = { Text("Locality") }, singleLine = true, modifier = Modifier.testTag("address_locality"))
            OutlinedTextField(postal, { postal = it.filter(Char::isDigit).take(6) }, label = { Text("Delhi PIN code") }, singleLine = true,
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number), modifier = Modifier.testTag("address_postal"))
            error?.let { Text(it, color = MaterialTheme.colorScheme.error) }
        }
    }, confirmButton = { TextButton(onClick = {
        val updated = address.copy(label = label.trim(), recipient = recipient.trim(), line = line.trim(), locality = locality.trim(), postalCode = postal)
        error = updated.validationError()
        if (error == null) save(updated)
    }, enabled = !busy, modifier = Modifier.testTag("save_address")) { Text("Save address") } },
        dismissButton = { TextButton(onClick = dismiss) { Text("Cancel") } })
}

@Composable
fun CheckoutScreen(products: List<Product>, state: CommerceState, vm: CommerceViewModel, back: () -> Unit, addresses: () -> Unit) {
    val snapshot = state.snapshot
    val pending = snapshot.pending
    val address = pending?.address ?: snapshot.addresses.find { it.id == snapshot.selectedAddressId }
    val totals = pending?.totals ?: calculateTotals(cartLines(snapshot.cart, products), snapshot.offerCode)
    var acknowledged by rememberSaveable { mutableStateOf(false) }
    var method by rememberSaveable { mutableStateOf("demo") }
    Scaffold(containerColor = Ivory, contentWindowInsets = WindowInsets(0), bottomBar = {
        if (pending == null) Surface(color = ParcelSurface) {
            Column(Modifier.padding(16.dp)) {
                Row(Modifier.fillMaxWidth().padding(bottom = 8.dp), horizontalArrangement = Arrangement.SpaceBetween) {
                    Text("Total to confirm", color = Muted); Text(totals.total.display(), style = MaterialTheme.typography.titleMedium)
                }
            Button(onClick = { if (method == "demo") vm.placeDemo() else vm.payU() },
                enabled = acknowledged && address != null && snapshot.cart.isNotEmpty() && !state.busy && (method == "demo" || vm.payUConfigured),
                modifier = Modifier.fillMaxWidth().heightIn(min = 56.dp).testTag("place_order")) {
                Text(if (state.busy) "Confirming…" else if (method == "demo") "Place order" else "Continue to PayU test checkout")
            }
            }
        }
    }) { insets ->
    LazyColumn(Modifier.fillMaxSize().padding(insets).testTag("checkout_list"), contentPadding = PaddingValues(20.dp), verticalArrangement = Arrangement.spacedBy(16.dp)) {
        item { ScreenHeading("Review & checkout", back) }
        item { SimulationNote(Modifier.fillMaxWidth()) }
        item { Surface(modifier = Modifier.fillMaxWidth(), shape = RoundedCornerShape(16.dp)) { Column(Modifier.padding(18.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
            Text("Delivery address", style = MaterialTheme.typography.titleMedium)
            Text(address?.display ?: "Select an address to continue.")
            TextButton(onClick = addresses, enabled = pending == null && !state.busy) { Text("Change address") }
        } } }
        item { Surface(modifier = Modifier.fillMaxWidth(), shape = RoundedCornerShape(16.dp)) { Column(Modifier.padding(18.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
            Text("Offer", style = MaterialTheme.typography.titleMedium)
            if (totals.offerCode == null) Text("Tap an offer banner on Home to apply it here.", color = Muted)
            else {
                Text("${totals.offerCode} · ${if (totals.offerEligible) "Applied automatically" else "Minimum spend not met"}", color = Teal)
                shoppingOffers.find { it.code == totals.offerCode }?.let { Text(it.description, color = Muted) }
                TextButton(onClick = { vm.offer(null) }, enabled = pending == null && !state.busy) { Text("Remove offer") }
            }
        } } }
        item { TotalsCard(totals) }
        if (pending != null) {
            item { Text("A ${if (pending.mode == "payu") "PayU sandbox" else "local demo"} checkout is pending. Your cart and address are locked until it is resolved.", color = Teal) }
            item { Button(onClick = vm::reconcile, enabled = !state.busy, modifier = Modifier.fillMaxWidth().testTag("resume_payment")) { Text(if (state.busy) "Checking…" else "Resume / check payment") } }
        } else {
            item { Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                Text("Payment method", style = MaterialTheme.typography.titleMedium)
                Row(Modifier.fillMaxWidth().clickable { method = "demo" }.testTag("method_demo"), verticalAlignment = Alignment.CenterVertically) { RadioButton(method == "demo", { method = "demo" }); Text("Instant checkout · no payment required") }
                Row(Modifier.fillMaxWidth().clickable { method = "payu" }.testTag("method_payu"), verticalAlignment = Alignment.CenterVertically) { RadioButton(method == "payu", { method = "payu" }); Text("PayU sandbox · UPI & cards") }
                if (method == "payu") {
                    Text(if (vm.payUConfigured) "Uses PayU's test environment. Use official test details only." else "Setup required: configure the sandbox backend URL. PayU checkout is unavailable until configured.", color = Muted, modifier = Modifier.testTag("payu_setup_status"))
                    Text("Use PayU's official UAT simulator for UPI intent testing. Available UPI/card methods depend on your test merchant account.", style = MaterialTheme.typography.bodySmall, color = Muted)
                }
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Checkbox(acknowledged, { acknowledged = it }, modifier = Modifier.testTag("checkout_acknowledge"))
                    Text("I understand this is simulated shopping. No physical items will be shipped. Use only demo/test payment details.", style = MaterialTheme.typography.bodySmall)
                }
            } }

        }
    }
}

}

@Composable
fun OrderHistoryScreen(receipts: List<Receipt>, back: () -> Unit, open: (String) -> Unit, delivery: DeliverySnapshot = DeliverySnapshot()) {
    var query by rememberSaveable { mutableStateOf("") }
    var filter by rememberSaveable { mutableStateOf("All") }
    val visible = receipts.filter { receipt ->
        val stage = delivery.orders.find { it.orderId == receipt.checkout.id }?.stage ?: DeliveryStage.Confirmed
        (filter == "All" || if (filter == "Delivered") stage == DeliveryStage.Delivered else stage != DeliveryStage.Delivered) &&
            (query.isBlank() || receipt.checkout.id.contains(query, true) || receipt.checkout.lines.any { it.title.contains(query, true) })
    }
    LazyColumn(Modifier.fillMaxSize(), contentPadding = PaddingValues(20.dp), verticalArrangement = Arrangement.spacedBy(16.dp)) {
        item { ScreenHeading("My orders", back) }
        item { OutlinedTextField(query, { query = it }, label = { Text("Search products or order ID") }, singleLine = true, modifier = Modifier.fillMaxWidth()) }
        item { Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) { listOf("All", "Active", "Delivered").forEach { option -> FilterChip(filter == option, { filter = option }, label = { Text(option) }) } } }
        if (receipts.isNotEmpty() && visible.isEmpty()) item { Text("No orders match your search.") }
        if (receipts.isEmpty()) item { Text("No orders yet.") }
        items(visible, key = { it.checkout.id }) { receipt -> Card(onClick = { open(receipt.checkout.id) }, modifier = Modifier.testTag("order_${receipt.checkout.id}")) {
            Column(Modifier.fillMaxWidth().padding(18.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                Text("Order ${receipt.checkout.id.takeLast(8).uppercase()}", style = MaterialTheme.typography.titleMedium)
                Text("${receipt.checkout.lines.sumOf { it.quantity }} items · ${receipt.checkout.totals.total.display()}")
                Text(delivery.orders.find { it.orderId == receipt.checkout.id }?.let { if (it.unboxedAt != null) "Unboxed · ready for your shelf" else it.stage.label } ?: "Order confirmed", color = Teal)
            }
        } }
    }
}

private fun paymentDisplayLabel(label: String): String = when (label) {
    "Local demo payment" -> "Payment confirmed · no charge"
    "PayU sandbox · verified" -> "PayU test payment confirmed"
    else -> label
}
