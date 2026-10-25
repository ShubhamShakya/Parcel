package com.parcel.presentation

import androidx.compose.foundation.*
import androidx.compose.animation.EnterTransition
import androidx.compose.animation.ExitTransition
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.grid.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextDecoration
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.compose.LocalLifecycleOwner
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.repeatOnLifecycle
import kotlinx.coroutines.delay
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.savedstate.read
import androidx.navigation.NavType
import androidx.navigation.compose.*
import androidx.navigation.navArgument
import com.parcel.domain.*
import org.koin.compose.koinInject

@Composable
fun ParcelApp() {
    val appearanceRepository = koinInject<AppearanceRepository>()
    val appearanceVm = viewModel { AppearanceViewModel(appearanceRepository) }
    val appearance by appearanceVm.state.collectAsStateWithLifecycle()
    val authRepository = koinInject<AuthRepository>()
    val authVm = viewModel { AuthViewModel(authRepository) }
    val auth by authVm.state.collectAsStateWithLifecycle()
    val catalog = koinInject<CatalogRepository>()
    val saved = koinInject<SavedItemsRepository>()
    val preferences = koinInject<PreferencesRepository>()
    val vm = viewModel { CatalogViewModel(catalog, saved, preferences) }
    val state by vm.state.collectAsStateWithLifecycle()
    val commerceRepository = koinInject<CommerceRepository>()
    val paymentGateway = koinInject<SandboxPaymentGateway>()
    val commerceVm = viewModel { CommerceViewModel(commerceRepository, paymentGateway) }
    val commerce by commerceVm.state.collectAsStateWithLifecycle()
    val deliveryRepository = koinInject<DeliveryRepository>()
    val deliveryVm = viewModel { DeliveryViewModel(deliveryRepository) }
    val delivery by deliveryVm.state.collectAsStateWithLifecycle()
    val lifecycle = LocalLifecycleOwner.current.lifecycle
    LaunchedEffect(lifecycle, commerce.snapshot.receipts.map { it.checkout.id }) {
        lifecycle.repeatOnLifecycle(Lifecycle.State.STARTED) {
            while (true) { deliveryVm.refresh(); delay(30_000) }
        }
    }
    ParcelTheme(dark = appearance.dark) {
        val actionError = appearance.error ?: state.actionError ?: delivery.error ?: commerce.error
        if (actionError != null) ParcelDialog(
            onDismissRequest = { appearanceVm.dismissError(); vm.dismissError(); deliveryVm.dismissError(); commerceVm.dismissError() },
            title = { Text("Let’s try that again") }, text = { Text(actionError) },
            confirmButton = { TextButton(onClick = { appearanceVm.dismissError(); vm.dismissError(); deliveryVm.dismissError(); commerceVm.dismissError() }) { Text("OK") } },
        )
        commerce.notice?.let { message -> ParcelDialog(onDismissRequest = commerceVm::dismissNotice,
            title = { Text(if (message.startsWith("Added")) "Added to cart" else "Offer update") },
            text = { Text(message) }, confirmButton = { TextButton(onClick = commerceVm::dismissNotice, modifier = Modifier.testTag("notice_ok")) { Text("Got it") } }) }
        Scaffold(containerColor = Ivory) { padding ->
            Box(Modifier.fillMaxSize().padding(padding)) {
                when {
                    state.loadError -> MessageScreen("Something didn't load", "Your saved items are safe. Try loading the shop again.", "Try again", vm::reload)
                    state.loading || auth.loading || !appearance.loaded -> Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) { CircularProgressIndicator() }
                    state.disclosureAccepted != true -> Disclosure(state.busy, vm::acceptDisclosure)
                    auth.loadError -> MessageScreen("Your profile needs a moment", "Please try loading your profile again.", "Try again", authVm::reload)
                    auth.snapshot.profile == null -> AuthScreen(auth, authVm)
                    else -> ShopNavigation(state, vm, commerce, commerceVm, delivery, deliveryVm, auth, authVm, appearance, appearanceVm)
                }
            }
        }
    }
}

@Composable
private fun Disclosure(busy: Boolean, accept: () -> Unit) {
    Column(Modifier.fillMaxSize().verticalScroll(rememberScrollState()).padding(28.dp),
        verticalArrangement = Arrangement.spacedBy(24.dp)) {
        Spacer(Modifier.height(36.dp))
        Surface(color = Lavender, shape = RoundedCornerShape(32.dp)) {
            ParcelFriend(Modifier.padding(12.dp).size(112.dp), greeting = true)
        }
        Text("Welcome to Parcel", color = Teal, style = MaterialTheme.typography.titleMedium)
        Text("All the joy.\nNone of the spending.", style = MaterialTheme.typography.headlineLarge)
        Text("Browse beautiful finds, build a wishlist and enjoy the anticipation of shopping.", style = MaterialTheme.typography.bodyLarge, color = Muted)
        Surface(shape = RoundedCornerShape(20.dp), color = ParcelSurface) {
            Column(Modifier.padding(20.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
                Text("A shopping simulator", style = MaterialTheme.typography.titleMedium)
                Text("Products, prices, ratings and reviews are provided for demonstration.")
                Text("No real money is spent. No physical items are purchased or delivered.")
                Text("Explore products, save favourites and place simulated orders. PayU uses test mode only.", color = Muted)
            }
        }
        Button(onClick = accept, enabled = !busy, modifier = Modifier.fillMaxWidth().heightIn(min = 56.dp).testTag("accept_disclosure"), shape = RoundedCornerShape(14.dp)) {
            Text(if (busy) "Saving…" else "I understand · Explore Parcel")
        }
        Text("A little profile. A lot of window shopping. No real spending.", style = MaterialTheme.typography.bodySmall, color = Muted)
    }
}

@Composable
private fun ShopNavigation(state: CatalogState, vm: CatalogViewModel, commerce: CommerceState, commerceVm: CommerceViewModel, delivery: DeliveryState, deliveryVm: DeliveryViewModel, auth: AuthState, authVm: AuthViewModel, appearance: AppearanceState, appearanceVm: AppearanceViewModel) {
    val companionRepository = koinInject<CompanionRepository>()
    val companionVm = viewModel { CompanionViewModel(companionRepository) }
    val companion by companionVm.state.collectAsStateWithLifecycle()
    val nav = rememberNavController()
    val entry by nav.currentBackStackEntryAsState()
    val route = entry?.destination?.route ?: "home"
    val tabs = listOf(Triple("home", "Home", Glyph.Home), Triple("wishlist", "Wishlist", Glyph.Heart),
        Triple("cart", "Cart (${commerce.snapshot.cart.sumOf { it.quantity }})", Glyph.Bag), Triple("purchased", "Purchased", Glyph.Shelf), Triple("about", "Profile", Glyph.Info))
    LaunchedEffect(commerce.completedId) {
        commerce.completedId?.let { id ->
            nav.navigate("receipt/$id") { popUpTo("cart") { inclusive = false }; launchSingleTop = true }
            commerceVm.consumeCompletion()
        }
    }
    Scaffold(containerColor = Ivory, contentWindowInsets = WindowInsets(0), bottomBar = {
        if (route in listOf("home", "wishlist", "cart", "purchased", "about")) NavigationBar(containerColor = ParcelSurface, tonalElevation = 0.dp, windowInsets = WindowInsets(0)) {
            tabs.forEach { (path, label, icon) ->
                NavigationBarItem(selected = route == path, onClick = {
                    nav.navigate(path) { popUpTo("home"); launchSingleTop = true }
                }, icon = { Symbol(icon) }, label = { Text(label) }, modifier = Modifier.testTag("tab_$path"))
            }
        }
    }) { padding ->
        NavHost(nav, startDestination = "home", modifier = Modifier.padding(padding),
            enterTransition = { EnterTransition.None }, exitTransition = { ExitTransition.None },
            popEnterTransition = { EnterTransition.None }, popExitTransition = { ExitTransition.None }) {
            composable("home") { HomeScreen(state, vm, commerce, commerceVm, delivery.snapshot, { nav.navigate("companion/location") }, { nav.navigate("receipt/$it") }, { nav.navigate("companion/notifications") }) { nav.navigate("product/$it") } }
            listOf("companion/{page}", "companion/{page}/{id}").forEach { path ->
                composable(path, arguments = listOf(navArgument("page") { type = NavType.StringType }) + if (path.endsWith("{id}")) listOf(navArgument("id") { type = NavType.StringType }) else emptyList()) { stack ->
                    val page = stack.arguments?.read { getString("page") }.orEmpty()
                    val id = if (path.endsWith("{id}")) stack.arguments?.read { getString("id") } else null
                    CompanionScreen(page, id, companion, companionVm, auth.snapshot.profile, commerce, commerceVm, state.products, delivery.snapshot, { nav.popBackStack() }, { nav.navigate(it) })
                }
            }
            composable("wishlist") { WishlistScreen(state, vm) { nav.navigate("product/$it") } }
            composable("purchased") { PurchasedScreen(commerce.snapshot.receipts, state.products, delivery, deliveryVm) { nav.navigate("orders") } }
            composable("about") { AboutScreen(auth, authVm, appearance, appearanceVm, { nav.navigate("orders") }, { nav.navigate("addresses") }, { nav.navigate("purchased") }, { nav.navigate("companion/$it") }) }
            composable("cart") { CartScreen(state.products, commerce, commerceVm,
                { nav.navigate(if (commerce.snapshot.pending == null) "addresses" else "checkout") }, { nav.navigate("orders") }) }
            composable("addresses") { AddressScreen(commerce, commerceVm, { nav.popBackStack() }, { nav.navigate("checkout") }) }
            composable("checkout") { CheckoutScreen(state.products, commerce, commerceVm, { nav.popBackStack() }, { nav.navigate("addresses") }) }
            composable("orders") { OrderHistoryScreen(commerce.snapshot.receipts, { nav.popBackStack() }, { nav.navigate("receipt/$it") }, delivery.snapshot) }
            composable("receipt/{receiptId}", arguments = listOf(navArgument("receiptId") { type = NavType.StringType })) { backStack ->
                val id = backStack.arguments?.read { getString("receiptId") }
                TrackingScreen(commerce.snapshot.receipts.find { it.checkout.id == id }, delivery, deliveryVm, { nav.popBackStack() }, { nav.navigate("unbox/$id") }, { nav.navigate("purchased") }, { nav.navigate("companion/$it/$id") })
            }
            composable("unbox/{receiptId}", arguments = listOf(navArgument("receiptId") { type = NavType.StringType })) { backStack ->
                val id = backStack.arguments?.read { getString("receiptId") }
                UnboxingScreen(commerce.snapshot.receipts.find { it.checkout.id == id }, state.products, delivery, deliveryVm, { nav.popBackStack() }, {
                    nav.navigate("purchased") { popUpTo("home"); launchSingleTop = true }
                })
            }
            composable("product/{productId}", arguments = listOf(navArgument("productId") { type = NavType.StringType })) { backStack ->
                val product = state.products.find { it.id == backStack.arguments?.read { getString("productId") } }
                if (product == null) MessageScreen("Product not found", "This product is unavailable.", "Go back", { nav.popBackStack() })
                else ProductScreen(product, product.id in state.savedIds, state.busy || commerce.busy, { vm.toggleSaved(product) }, { nav.popBackStack() }, { commerceVm.add(product, it) }, { commerceVm.add(product, it) { nav.navigate("cart") } }, { nav.navigate("companion/$it/${product.id}") })
            }
        }
    }
}

@Composable
private fun HomeScreen(state: CatalogState, vm: CatalogViewModel, commerce: CommerceState, commerceVm: CommerceViewModel, delivery: DeliverySnapshot, addresses: () -> Unit, track: (String) -> Unit, notifications: () -> Unit, open: (String) -> Unit) {
    val ads = LocalParcelAds.current
    var sort by rememberSaveable { mutableStateOf("Top rated") }
    var sortMenu by remember { mutableStateOf(false) }
    val visible = when (sort) {
        "Price: low to high" -> state.visibleProducts.sortedBy { it.price.minorUnits }
        "Price: high to low" -> state.visibleProducts.sortedByDescending { it.price.minorUnits }
        "Top rated" -> state.visibleProducts.sortedByDescending { it.rating.toDoubleOrNull() ?: 0.0 }
        else -> state.visibleProducts
    }
    LazyVerticalGrid(columns = GridCells.Adaptive(160.dp), contentPadding = PaddingValues(20.dp),
        horizontalArrangement = Arrangement.spacedBy(12.dp), verticalArrangement = Arrangement.spacedBy(14.dp),
        modifier = Modifier.fillMaxSize().testTag("home_grid")) {
        item(span = { GridItemSpan(maxLineSpan) }) {
            Column(verticalArrangement = Arrangement.spacedBy(16.dp)) {
                Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
                    Text("parcel", style = MaterialTheme.typography.headlineLarge, color = Teal)
                    Text(" •", color = Orange, style = MaterialTheme.typography.headlineLarge)
                    Spacer(Modifier.weight(1f))
                    TextButton(onClick = notifications, modifier = Modifier.testTag("home_notifications")) { Text("Inbox") }
                }
                val address = commerce.snapshot.addresses.find { it.id == commerce.snapshot.selectedAddressId }
                TextButton(onClick = addresses, contentPadding = PaddingValues(0.dp)) {
                    Text("⌖  Deliver to ${address?.locality ?: "Connaught Place"}  ›", color = Ink)
                }
                OutlinedTextField(value = state.query, onValueChange = vm::search,
                    placeholder = { Text("Search your next favourite") }, leadingIcon = { Symbol(Glyph.Search) },
                    trailingIcon = { if (state.query.isNotEmpty()) IconButton(onClick = { vm.search("") }, modifier = Modifier.testTag("clear_search")) { Symbol(Glyph.Close, "Clear search") } },
                    singleLine = true, shape = RoundedCornerShape(14.dp), modifier = Modifier.fillMaxWidth().testTag("search_field"))
                if (state.query.isBlank() && state.category == null) OfferCarousel(commerceVm.offers, commerce.snapshot.offerCode,
                    !commerce.busy && !commerce.loading && commerce.snapshot.pending == null, commerceVm::offer)
                Row(Modifier.fillMaxWidth().horizontalScroll(rememberScrollState()), horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                    FilterChip(selected = state.category == null, onClick = { vm.selectCategory(null) }, label = { Text("All") })
                    Category.entries.forEach { category ->
                        Column(Modifier.width(68.dp).clickable { vm.selectCategory(category) }.testTag("category_${category.name}"), horizontalAlignment = Alignment.CenterHorizontally) {
                            Surface(color = if (state.category == category) Orange else ParcelSurface, shape = RoundedCornerShape(30.dp)) {
                                val example = state.products.firstOrNull { it.category == category }
                                if (example != null) ProductImage(example, Modifier.size(58.dp)) else Text(category.symbol, Modifier.padding(18.dp))
                            }
                            Text(category.label, style = MaterialTheme.typography.labelMedium, modifier = Modifier.padding(top = 6.dp))
                        }
                    }
                }
                if (state.query.isBlank() && state.category == null) {
                    val activeOrder = delivery.orders.lastOrNull { it.stage != DeliveryStage.Delivered }
                    if (activeOrder != null) Surface(onClick = { track(activeOrder.orderId) }, shape = RoundedCornerShape(16.dp), color = ParcelSurface) {
                        Row(Modifier.fillMaxWidth().padding(16.dp), verticalAlignment = Alignment.CenterVertically) {
                            Column(Modifier.weight(1f)) { Text("Your order is on the way", style = MaterialTheme.typography.titleSmall); Text(activeOrder.stage.label, color = Muted, style = MaterialTheme.typography.bodySmall) }
                            Text("Track ›", color = Teal)
                        }
                    }
                    Text("Shop clothing", style = MaterialTheme.typography.titleLarge)
                    Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                        ClothingDepartment.entries.forEach { department ->
                            Surface(onClick = { vm.selectCategory(Category.Clothing); vm.selectDepartment(department) }, modifier = Modifier.weight(1f), color = if (department == ClothingDepartment.Men) MaterialTheme.colorScheme.secondaryContainer else Pink, shape = RoundedCornerShape(18.dp)) {
                                Column(Modifier.padding(16.dp)) {
                                    Text(department.label, style = MaterialTheme.typography.titleLarge)
                                    Text(if (department == ClothingDepartment.Men) "Shirts, jeans & winterwear" else "Tops, dresses & coats", style = MaterialTheme.typography.bodySmall, color = Muted)
                                }
                            }
                        }
                    }
                    Text("Offers worth exploring", style = MaterialTheme.typography.titleLarge)
                    Row(Modifier.horizontalScroll(rememberScrollState()), horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                        state.products.filter { it.featured }.take(5).forEach { product ->
                            Surface(onClick = { open(product.id) }, modifier = Modifier.width(152.dp), shape = RoundedCornerShape(18.dp)) {
                                Column {
                                    ProductImage(product, Modifier.fillMaxWidth().height(120.dp))
                                    Text(product.title, Modifier.padding(12.dp), maxLines = 2, style = MaterialTheme.typography.titleSmall)
                                    Text(product.price.display(), Modifier.padding(start = 12.dp, bottom = 12.dp), style = MaterialTheme.typography.titleMedium)
                                }
                            }
                        }
                    }
                }
                if (state.query.isBlank() && state.category == null) ads.NativeCard(AdPlacement.Home)
                if (state.category == Category.Clothing) ClothingControls(state, vm)
                Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
                    Text(if (state.query.isNotBlank()) "Search results" else if (state.category == Category.Clothing) "${state.clothingFilter.department.label} ${if (state.clothingFilter.winterOnly) "winterwear" else "clothing"}" else state.category?.label ?: "Picked for your everyday", style = MaterialTheme.typography.titleMedium, modifier = Modifier.weight(1f))
                    Box {
                        TextButton(onClick = { sortMenu = true }) { Text("Sort ▾") }
                        DropdownMenu(expanded = sortMenu, onDismissRequest = { sortMenu = false }) {
                            listOf("Recommended", "Top rated", "Price: low to high", "Price: high to low").forEach { choice ->
                                DropdownMenuItem(text = { Text(choice) }, onClick = { sort = choice; sortMenu = false })
                            }
                        }
                    }
                }
            }
        }
        if (state.visibleProducts.isEmpty()) item(span = { GridItemSpan(maxLineSpan) }) {
            MessageScreen("No finds just yet", "Try a different search or explore all categories.", "Reset filters", { if (state.category == Category.Clothing) vm.resetClothingFilters() else { vm.search(""); vm.selectCategory(null) } })
        }
        visible.forEachIndexed { index, product ->
            if (index == 8 && state.category != null && state.query.isBlank()) {
                item(key = "category_ad", span = { GridItemSpan(maxLineSpan) }) { ads.NativeCard(AdPlacement.Category) }
            }
            item(key = product.id) {
                ProductCard(product, product.id in state.savedIds, state.busy || commerce.busy, { vm.toggleSaved(product) }, { open(product.id) }, { if (product.clothing != null) open(product.id) else commerceVm.add(product, product.variants.first()) })
            }
        }
    }
}

@Composable
private fun ProductCard(product: Product, isSaved: Boolean, busy: Boolean, save: () -> Unit, open: () -> Unit, quickAdd: () -> Unit = open) {
    Card(onClick = open, shape = RoundedCornerShape(16.dp), colors = CardDefaults.cardColors(containerColor = ParcelSurface),
        modifier = Modifier.fillMaxWidth().testTag("product_${product.id}")) {
        Box {
            ProductImage(product, Modifier.fillMaxWidth().aspectRatio(1f))
            val discount = if (product.comparisonPrice.minorUnits == 0L) 0 else (100 - product.price.minorUnits * 100 / product.comparisonPrice.minorUnits).toInt()
            if (discount > 0) Surface(color = Teal, shape = RoundedCornerShape(6.dp), modifier = Modifier.padding(8.dp)) {
                Text("$discount% off", Modifier.padding(7.dp, 3.dp), color = OnAccent, style = MaterialTheme.typography.labelSmall)
            }
            IconButton(onClick = save, enabled = !busy, modifier = Modifier.align(Alignment.TopEnd).testTag("save_${product.id}")) {
                Symbol(Glyph.Heart, if (isSaved) "Remove ${product.title} from wishlist" else "Save ${product.title}", color = if (isSaved) Teal else Muted, filled = isSaved)
            }
        }
        Column(Modifier.padding(12.dp), verticalArrangement = Arrangement.spacedBy(5.dp)) {
            Text((product.clothing?.type?.label ?: product.category.label).uppercase(), style = MaterialTheme.typography.labelSmall, color = Teal)
            Text(product.title, style = MaterialTheme.typography.titleSmall, minLines = 2)
            Text("★ ${product.rating} · ${product.reviews.size} reviews", style = MaterialTheme.typography.bodySmall, color = Muted)
            Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
                Column(Modifier.weight(1f)) {
                    Text(product.price.display(), style = MaterialTheme.typography.titleMedium)
                    Text(product.comparisonPrice.display(), style = MaterialTheme.typography.bodySmall, textDecoration = TextDecoration.LineThrough, color = Muted)
                }
                FilledIconButton(onClick = quickAdd, enabled = !busy, modifier = Modifier.size(36.dp)) { Text("+", style = MaterialTheme.typography.titleLarge) }
            }
        }
    }
}

@Composable
private fun WishlistScreen(state: CatalogState, vm: CatalogViewModel, open: (String) -> Unit) {
    val ads = LocalParcelAds.current
    LazyVerticalGrid(columns = GridCells.Adaptive(160.dp), contentPadding = PaddingValues(20.dp),
        horizontalArrangement = Arrangement.spacedBy(12.dp), verticalArrangement = Arrangement.spacedBy(14.dp), modifier = Modifier.fillMaxSize()) {
        item(span = { GridItemSpan(maxLineSpan) }) {
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                Text("Your wishlist", style = MaterialTheme.typography.headlineMedium)
                Text("A place for all your someday favourites.", color = Muted)
            }
        }
        item(key = "wishlist_banner", span = { GridItemSpan(maxLineSpan) }) { ads.Banner(BannerPlacement.Wishlist) }
        if (state.savedProducts.isEmpty()) item(span = { GridItemSpan(maxLineSpan) }) {
            Column(Modifier.fillMaxWidth().padding(vertical = 48.dp), horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.spacedBy(12.dp)) {
                Symbol(Glyph.Heart, modifier = Modifier.size(48.dp), color = Teal)
                Text("Make room for a favourite", style = MaterialTheme.typography.titleMedium)
                Text("Tap a heart on any product to save it here.", color = Muted)
            }
        }
        items(state.savedProducts, key = { it.id }) { product ->
            ProductCard(product, true, state.busy, { vm.toggleSaved(product) }, { open(product.id) })
        }
    }
}

@Composable
private fun ProductScreen(product: Product, saved: Boolean, busy: Boolean, save: () -> Unit, back: () -> Unit, add: (String) -> Unit, buy: (String) -> Unit, companion: (String) -> Unit) {
    var variant by rememberSaveable(product.id) { mutableStateOf(product.variants.first()) }
    val clothing = product.clothing
    var color by rememberSaveable(product.id) { mutableStateOf(clothing?.colors?.first().orEmpty()) }
    var size by rememberSaveable(product.id) { mutableStateOf(clothing?.sizes?.singleOrNull()) }
    val chosenVariant = if (clothing != null && size != null) clothing.variant(color, size!!) else variant
    Scaffold(containerColor = Ivory, contentWindowInsets = WindowInsets(0), bottomBar = {
        Surface(color = ParcelSurface) {
            Row(Modifier.fillMaxWidth().padding(16.dp), horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                val ready = !busy && (clothing == null || size != null)
                OutlinedButton(onClick = { add(chosenVariant) }, enabled = ready, modifier = Modifier.weight(1f).height(52.dp).testTag("details_add_cart"), shape = RoundedCornerShape(14.dp)) { Text(if (clothing != null && size == null) "Select size" else "Add to cart") }
                Button(onClick = { buy(chosenVariant) }, enabled = ready, modifier = Modifier.weight(1f).height(52.dp).testTag("details_buy_now"), shape = RoundedCornerShape(14.dp)) { Text("Buy now") }
            }
        }
    }) { insets ->
    LazyColumn(Modifier.fillMaxSize().padding(insets).testTag("product_details"), contentPadding = PaddingValues(bottom = 24.dp), verticalArrangement = Arrangement.spacedBy(16.dp)) {
        item {
            Row(Modifier.fillMaxWidth().padding(horizontal = 12.dp), verticalAlignment = Alignment.CenterVertically) {
                IconButton(onClick = back, modifier = Modifier.testTag("details_back")) { Symbol(Glyph.Back, "Back") }
                Text("Product details", style = MaterialTheme.typography.titleMedium)
                Spacer(Modifier.weight(1f))
                TextButton(onClick = { companion("share") }) { Text("Share") }
            }
        }
        item { ProductGallery(product) }
        item {
            Column(Modifier.padding(horizontal = 20.dp), verticalArrangement = Arrangement.spacedBy(14.dp)) {
                Text((product.clothing?.type?.label ?: product.category.label).uppercase(), color = Teal, style = MaterialTheme.typography.labelMedium)
                Text(product.title, style = MaterialTheme.typography.headlineMedium)
                Text(product.subtitle, color = Muted)
                Text("★ ${product.rating}  ·  ${product.reviews.size} reviews", color = Teal, style = MaterialTheme.typography.bodyMedium)
                Row(horizontalArrangement = Arrangement.spacedBy(12.dp), verticalAlignment = Alignment.CenterVertically) {
                    Text(product.price.display(), style = MaterialTheme.typography.headlineSmall)
                    Text(product.comparisonPrice.display(), color = Muted, textDecoration = TextDecoration.LineThrough)
                }
                Text("Price before taxes and fees", color = Muted, style = MaterialTheme.typography.bodySmall)
            }
        }
        item {
            Column(Modifier.padding(horizontal = 20.dp), verticalArrangement = Arrangement.spacedBy(14.dp)) {
                if (clothing != null) {
                    ClothingSelectors(clothing, color, size, { color = it }, { size = it })
                    TextButton(onClick = { companion("size-guide") }) { Text("Size guide") }
                }
                else {
                Text("Colour · $variant", style = MaterialTheme.typography.titleSmall)
                Row(Modifier.horizontalScroll(rememberScrollState()), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    product.variants.forEach { choice -> FilterChip(selected = variant == choice, onClick = { variant = choice }, label = { Text(choice) }) }
                }
                }
            }
        }
        item {
            Column(Modifier.padding(horizontal = 20.dp), verticalArrangement = Arrangement.spacedBy(14.dp)) {
                Button(onClick = save, enabled = !busy, modifier = Modifier.fillMaxWidth().heightIn(min = 52.dp).testTag("details_save"), shape = RoundedCornerShape(14.dp)) {
                    Symbol(Glyph.Heart); Spacer(Modifier.width(10.dp)); Text(if (saved) "Remove from wishlist" else "Save to wishlist")
                }
                Surface(color = ParcelSurface, shape = RoundedCornerShape(16.dp)) {
                    Column(Modifier.fillMaxWidth().padding(16.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                        Text("Arrives in about 4 hours", style = MaterialTheme.typography.titleMedium)
                        Text("Simulated delivery within 4 hours. Follow every step from your order page.", color = Muted, style = MaterialTheme.typography.bodySmall)
                        Text("Offers apply at checkout", color = Teal, style = MaterialTheme.typography.titleSmall)
                    }
                }
                Text("About this find", style = MaterialTheme.typography.titleMedium)
                Text(product.description, style = MaterialTheme.typography.bodyLarge, color = Muted)
                HorizontalDivider()
                Text("Reviews", style = MaterialTheme.typography.titleMedium)
            }
        }
        product.reviews.forEach { review -> item {
            Surface(Modifier.padding(horizontal = 20.dp).fillMaxWidth(), shape = RoundedCornerShape(16.dp)) {
                Column(Modifier.padding(18.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Text(review.author, style = MaterialTheme.typography.titleSmall)
                    Text("★".repeat(review.stars), color = Teal)
                    Text(review.body, color = Muted)
                }
            }
        } }
    }
}

}

@Composable
private fun AboutScreen(auth: AuthState, authVm: AuthViewModel, appearance: AppearanceState, appearanceVm: AppearanceViewModel, orders: () -> Unit, addresses: () -> Unit, purchased: () -> Unit, companion: (String) -> Unit) {
    var confirmLogout by rememberSaveable { mutableStateOf(false) }
    if (confirmLogout) ParcelDialog(
        onDismissRequest = { confirmLogout = false },
        title = { Text("Log out of Parcel?") },
        text = { Text("Your profile and shopping will stay saved on this device.") },
        confirmButton = { TextButton(onClick = { confirmLogout = false; authVm.signOut() }, modifier = Modifier.testTag("confirm_sign_out")) { Text("Log out") } },
        dismissButton = { TextButton(onClick = { confirmLogout = false }, modifier = Modifier.testTag("cancel_sign_out")) { Text("Stay here") } }
    )
    Column(Modifier.fillMaxSize().verticalScroll(rememberScrollState()).padding(24.dp), verticalArrangement = Arrangement.spacedBy(20.dp)) {
        Text("Your account", style = MaterialTheme.typography.headlineMedium)
        auth.snapshot.profile?.let { profile ->
            Surface(color = Lavender, shape = RoundedCornerShape(28.dp)) {
                Column(Modifier.fillMaxWidth().padding(20.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Text("${profile.firstName} ${profile.lastName}", style = MaterialTheme.typography.headlineSmall, modifier = Modifier.testTag("profile_name"))
                    Text(profile.phone, color = Teal, modifier = Modifier.testTag("profile_phone"))
                    Text("Your local shopper profile", style = MaterialTheme.typography.bodySmall)
                }
            }
        }
        Surface(shape = RoundedCornerShape(18.dp)) {
            Column {
                AccountLink("My orders", "Track deliveries and view order history", orders)
                HorizontalDivider(color = Ivory)
                AccountLink("Saved addresses", "Choose, add or edit delivery addresses", addresses)
                HorizontalDivider(color = Ivory)
                AccountLink("My collection", "Unboxed finds and your personal shelf", purchased)
            }
        }
        Surface(shape = RoundedCornerShape(18.dp)) {
            Column {
                listOf("edit-profile" to "Edit profile", "notifications" to "Notifications", "coupons" to "Coupons", "payments" to "Payment methods", "buy-again" to "Buy again", "help" to "Help centre", "privacy" to "Privacy", "terms" to "Terms of use").forEach { (page, label) ->
                    AccountLink(label, "", { companion(page) })
                    HorizontalDivider(color = Ivory)
                }
            }
        }
        Surface(shape = RoundedCornerShape(18.dp)) {
            Row(Modifier.fillMaxWidth().padding(18.dp), verticalAlignment = Alignment.CenterVertically) {
                Column(Modifier.weight(1f)) {
                    Text("Appearance", style = MaterialTheme.typography.titleMedium)
                    Text(if (appearance.dark) "Dark mode" else "Light mode", color = Muted)
                }
                Switch(checked = appearance.dark, onCheckedChange = appearanceVm::setDark,
                    enabled = !appearance.busy, modifier = Modifier.testTag("dark_mode_switch").semantics { contentDescription = "Use dark mode" })
            }
        }
        LocalParcelAds.current.PrivacyOptions()
        OutlinedButton(onClick = { confirmLogout = true }, enabled = !auth.busy, modifier = Modifier.fillMaxWidth().testTag("auth_sign_out")) { Text("Log out") }
        auth.error?.let { Text(it, color = MaterialTheme.colorScheme.error) }
        SimulationNote(Modifier.fillMaxWidth())
        Text("Parcel is a shopping simulator. Its catalog, pricing and reviews are illustrative. Purchases and deliveries are simulated.", style = MaterialTheme.typography.bodyLarge)
        Text("No real payments. No real deliveries.", style = MaterialTheme.typography.titleMedium, color = Teal)
        Text("Profiles, wishlist, orders and your shelf stay on this device. Shopping data is shared on this installation when you switch profiles.", color = Muted)
        HorizontalDivider()
        Text("Version 0.6 · Your shopping companion", style = MaterialTheme.typography.titleSmall)
        Text("Browse, order, follow your delivery and open your parcel. Give your unboxed finds a home on your 12-space shelf. Orders arrive within 4 hours, or instantly with a tap. PayU sandbox checkout requires backend setup.", color = Muted)
    }
}

@Composable
private fun MessageScreen(title: String, body: String, action: String, onAction: () -> Unit) {
    Column(Modifier.fillMaxWidth().padding(28.dp), verticalArrangement = Arrangement.spacedBy(16.dp)) {
        Text(title, style = MaterialTheme.typography.titleLarge)
        Text(body, color = Muted)
        Button(onClick = onAction) { Text(action) }
    }
}

@Composable
private fun AccountLink(title: String, subtitle: String, action: () -> Unit) {
    Row(Modifier.fillMaxWidth().clickable(onClick = action).padding(18.dp), verticalAlignment = Alignment.CenterVertically) {
        Column(Modifier.weight(1f)) { Text(title, style = MaterialTheme.typography.titleMedium); if (subtitle.isNotBlank()) Text(subtitle, color = Muted, style = MaterialTheme.typography.bodySmall) }
        Text("›", style = MaterialTheme.typography.titleLarge, color = Muted)
    }
}
