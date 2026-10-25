package com.parcel.presentation

import androidx.compose.foundation.*
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.pager.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.semantics.*
import androidx.compose.ui.unit.dp
import com.parcel.domain.*
import kotlinx.coroutines.launch
import kotlinx.coroutines.delay
import androidx.compose.foundation.interaction.collectIsDraggedAsState
import androidx.compose.foundation.shape.CircleShape

@Composable
fun OfferCarousel(offers: List<Offer>, selected: String?, enabled: Boolean, onSelect: (Offer) -> Unit) {
    if (offers.isEmpty()) return
    // Center the finite implementation far from either edge: modulo indexing repeats both ways.
    val center = Int.MAX_VALUE / 2 - (Int.MAX_VALUE / 2) % offers.size
    val pager = rememberPagerState(initialPage = center, pageCount = { Int.MAX_VALUE })
    val dragged by pager.interactionSource.collectIsDraggedAsState()
    LaunchedEffect(pager.settledPage, dragged, enabled) {
        if (enabled && !dragged && offers.size > 1) {
            delay(5_000)
            if (!pager.isScrollInProgress) pager.animateScrollToPage(pager.currentPage + 1)
        }
    }
    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
        HorizontalPager(pager, pageSpacing = 12.dp, modifier = Modifier.testTag("offer_carousel")) { page ->
            val offer = offers[page % offers.size]
            Card(onClick = { onSelect(offer) }, enabled = enabled, shape = RoundedCornerShape(20.dp),
                colors = CardDefaults.cardColors(containerColor = if (page % 2 == 0) Peach else MaterialTheme.colorScheme.secondaryContainer),
                modifier = Modifier.fillMaxWidth().heightIn(min = 166.dp).testTag("offer_card_${offer.code}")) {
                Column(Modifier.padding(20.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Text(if (selected == offer.code) "SELECTED · AUTO-APPLIES AT CHECKOUT" else "EXCLUSIVE OFFER · TAP TO SELECT", color = Teal, style = MaterialTheme.typography.labelSmall)
                    Text(offer.title, color = Ink, style = MaterialTheme.typography.headlineSmall)
                    Text(offer.description, color = Ink, style = MaterialTheme.typography.bodySmall)
                    Text(offer.code, color = Teal, style = MaterialTheme.typography.labelLarge)
                    Row(Modifier.fillMaxWidth().semantics { contentDescription = "Offer ${pager.currentPage % offers.size + 1} of ${offers.size}" }.testTag("offer_dots"), horizontalArrangement = Arrangement.Center) {
                        repeat(offers.size) { index ->
                            Box(Modifier.padding(horizontal = 4.dp).size(7.dp).background(
                                if (index == pager.currentPage % offers.size) Teal else Teal.copy(alpha = .25f), CircleShape))
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun ProductGallery(product: Product) {
    val images = rememberSaveable(product.id) { randomGallery(product.imageKey) }
    val pager = rememberPagerState(pageCount = { images.size })
    val scope = rememberCoroutineScope()
    Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
        HorizontalPager(pager, modifier = Modifier.fillMaxWidth().testTag("product_gallery")) { page ->
            ProductImage(product, Modifier.fillMaxWidth().heightIn(max = 360.dp).aspectRatio(1.25f), images[page])
        }
        Text("Image ${pager.currentPage + 1} of ${images.size}", color = Muted,
            style = MaterialTheme.typography.bodySmall, modifier = Modifier.align(Alignment.CenterHorizontally).testTag("gallery_count"))
        Row(Modifier.fillMaxWidth().horizontalScroll(rememberScrollState()).padding(horizontal = 20.dp), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            images.forEachIndexed { index, image ->
                Surface(shape = RoundedCornerShape(10.dp), border = BorderStroke(if (index == pager.currentPage) 2.dp else 1.dp,
                    if (index == pager.currentPage) Teal else ParcelOutline),
                    modifier = Modifier.size(56.dp).semantics { contentDescription = "Show image ${index + 1} of ${images.size}"; selected = pager.currentPage == index }
                        .clickable { scope.launch { pager.animateScrollToPage(index) } }.testTag("thumbnail_$index")) {
                    ProductImage(product, Modifier.fillMaxSize(), image)
                }
            }
        }
    }
}
