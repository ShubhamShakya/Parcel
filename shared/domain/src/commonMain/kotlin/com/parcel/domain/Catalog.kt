package com.parcel.domain

import kotlinx.coroutines.flow.Flow
import kotlinx.serialization.Serializable

@Serializable
data class Money(val minorUnits: Long, val currency: String = "INR") {
    init { require(minorUnits >= 0); require(currency == "INR") }
    fun display(): String {
        val digits = (minorUnits / 100).toString()
        val grouped = digits.reversed().chunked(3).joinToString(",").reversed()
        val fraction = (minorUnits % 100).toString().padStart(2, '0')
        return "₹$grouped" + if (fraction == "00") "" else ".$fraction"
    }
}

enum class Category(val label: String, val symbol: String) {
    Clothing("Clothing", "◇"), Tech("Tech", "◉"), Home("Home", "⌂"), Style("Style", "◇"), Outdoors("Outdoors", "△")
}
data class Review(val author: String, val stars: Int, val body: String) {
    init { require(stars in 1..5) }
}
data class Product(
    val id: String,
    val title: String,
    val subtitle: String,
    val category: Category,
    val price: Money,
    val comparisonPrice: Money,
    val imageKey: String,
    val description: String,
    val variants: List<String>,
    val reviews: List<Review>,
    val featured: Boolean = false,
    val clothing: ClothingDetails? = null,
) {
    init {
        require(id.isNotBlank()); require(title.isNotBlank())
        require(comparisonPrice.minorUnits >= price.minorUnits)
        require(variants.isNotEmpty())
    }
    val rating: String get() {
        if (reviews.isEmpty()) return "New"
        val tenths = (reviews.sumOf { it.stars } * 10 + reviews.size / 2) / reviews.size
        return "${tenths / 10}.${tenths % 10}"
    }
}

/** Stable catalog order is preserved, including when search and category are combined. */
fun filterProducts(products: List<Product>, query: String, category: Category?, clothingFilter: ClothingFilter? = null): List<Product> {
    val terms = query.trim().split(Regex("\\s+")).filter { it.isNotBlank() }
    return products.filter { product ->
        (category == null || product.category == category) &&
            (category != Category.Clothing || clothingFilter == null || clothingFilter.matches(product)) && terms.all { term ->
            when (term.lowercase().replace("'", "").replace("’", "")) {
                "men", "mens" -> product.clothing?.department == ClothingDepartment.Men
                "women", "womens" -> product.clothing?.department == ClothingDepartment.Women
                else -> buildString {
                append("${product.title} ${product.subtitle} ${product.category.label}")
                product.clothing?.let { c -> append(" ${c.department.label} ${c.type.label} ${c.fabric} ${c.pattern} ${c.colors.joinToString(" ")} ${c.season} ${c.type.group.label} ${c.fit} ${c.occasion}") }
            }.contains(term, ignoreCase = true)
            }
        }
    }
}
interface CatalogRepository { fun observeProducts(): Flow<List<Product>> }
interface SavedItemsRepository {
    fun observeSavedIds(): Flow<Set<String>>
    suspend fun setSaved(productId: String, saved: Boolean)
}
interface PreferencesRepository {
    fun observeDisclosureAccepted(): Flow<Boolean>
    suspend fun acceptDisclosure()
}

interface AppearanceRepository {
    fun observeDarkMode(): Flow<Boolean>
    suspend fun setDarkMode(enabled: Boolean)
}
