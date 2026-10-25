package com.parcel.data

import com.parcel.domain.*
import kotlinx.coroutines.flow.flowOf

class MockCatalogRepository : CatalogRepository {
    override fun observeProducts() = flowOf(products)
    companion object {
        private fun reviews(first: String, second: String) = listOf(
            Review("Aarav", 5, first), Review("Mira", 4, second)
        )
        val products = listOf(
            Product("sound-one", "Everyday headphones", "Quiet moments, better sound", Category.Tech,
                Money(349900), Money(499900), "headphones",
                "Over-ear wireless headphones with soft cushions, fold-flat earcups and a balanced sound profile.",
                listOf("Midnight", "Sand", "Sage"), reviews("Comfortable for a long afternoon of listening.", "A lovely simple design and soft ear cushions."), true),
            Product("carry-all", "The weekend tote", "Room for a little more", Category.Style,
                Money(129900), Money(189900), "tote",
                "A roomy cotton-canvas tote with reinforced handles and a useful inner pocket. Made for weekends, bookshop stops and everyday essentials.",
                listOf("Natural", "Olive", "Ink"), reviews("Roomy enough for a book and all my essentials.", "The natural colour goes with everything."), true),
            Product("slow-morning", "Slow morning mug", "Your daily pause, reimagined", Category.Home,
                Money(64900), Money(89900), "mug",
                "A 350 ml stoneware-inspired mug with a rounded handle and a matte finish. Picture a slow morning with your favourite brew.",
                listOf("Terracotta", "Cream", "Ocean"), reviews("The shape feels so cosy for morning coffee.", "Love the soft colour and generous handle."), true),
            Product("trail-day", "Daybreak backpack", "Take the scenic route", Category.Outdoors,
                Money(249900), Money(329900), "backpack",
                "A lightweight 20-litre daypack with padded straps, a front pocket and space for your next adventure.",
                listOf("Forest", "Clay", "Black"), reviews("A practical layout for a day out.", "The forest colour looks great."), true),
            Product("desk-glow", "Arc desk light", "A warmer kind of workspace", Category.Home,
                Money(189900), Money(249900), "lamp",
                "An adjustable desk lamp with a warm glow, a weighted base and a clean silhouette. A thoughtful addition to your workspace.",
                listOf("Ochre", "White", "Charcoal"), reviews("Just the right touch for a reading corner.", "A neat design with a small footprint.")),
            Product("pocket-sound", "Pocket speaker", "Small size, good company", Category.Tech,
                Money(159900), Money(219900), "speaker",
                "A compact portable speaker with a textured fabric shell and simple controls. Perfect for a picnic playlist.",
                listOf("Slate", "Coral", "Moss"), reviews("A lovely little companion for a picnic.", "Simple controls and a nice finish.")),
        ) + clothingProducts
    }
}
