package com.parcel.domain

enum class ClothingDepartment(val label: String) { Men("Men's"), Women("Women's") }
enum class ClothingGroup(val label: String) {
    Topwear("Topwear"), Bottomwear("Bottomwear"), Outfits("Sets & outfits"), Ethnic("Indian & ethnic wear"),
    Activewear("Sportswear"), Sleepwear("Sleepwear & loungewear"), Innerwear("Innerwear"), Winterwear("Winterwear")
}
data class ClothingType(val id: String, val label: String, val group: ClothingGroup, val departments: Set<ClothingDepartment>)
val clothingTypes = listOf(
    ClothingType("shirts", "Casual shirts", ClothingGroup.Topwear, setOf(ClothingDepartment.Men, ClothingDepartment.Women)),
    ClothingType("formal-shirts", "Formal shirts", ClothingGroup.Topwear, setOf(ClothingDepartment.Men)),
    ClothingType("tshirts", "T-shirts", ClothingGroup.Topwear, setOf(ClothingDepartment.Men, ClothingDepartment.Women)),
    ClothingType("polos", "Polo shirts", ClothingGroup.Topwear, setOf(ClothingDepartment.Men)),
    ClothingType("tank-tops", "Tank tops", ClothingGroup.Topwear, setOf(ClothingDepartment.Men)),
    ClothingType("overshirts", "Overshirts", ClothingGroup.Topwear, setOf(ClothingDepartment.Men)),
    ClothingType("tops", "Tops", ClothingGroup.Topwear, setOf(ClothingDepartment.Women)),
    ClothingType("blouses", "Blouses", ClothingGroup.Topwear, setOf(ClothingDepartment.Women)),
    ClothingType("crop-tops", "Crop tops", ClothingGroup.Topwear, setOf(ClothingDepartment.Women)),
    ClothingType("tunics", "Tunics", ClothingGroup.Topwear, setOf(ClothingDepartment.Women)),
    ClothingType("camisoles", "Camisoles", ClothingGroup.Topwear, setOf(ClothingDepartment.Women)),
    ClothingType("jeans", "Jeans", ClothingGroup.Bottomwear, setOf(ClothingDepartment.Men, ClothingDepartment.Women)),
    ClothingType("trousers", "Trousers", ClothingGroup.Bottomwear, setOf(ClothingDepartment.Men, ClothingDepartment.Women)),
    ClothingType("chinos", "Chinos", ClothingGroup.Bottomwear, setOf(ClothingDepartment.Men)),
    ClothingType("cargo-pants", "Cargo pants", ClothingGroup.Bottomwear, setOf(ClothingDepartment.Men, ClothingDepartment.Women)),
    ClothingType("shorts", "Shorts", ClothingGroup.Bottomwear, setOf(ClothingDepartment.Men, ClothingDepartment.Women)),
    ClothingType("joggers", "Joggers", ClothingGroup.Bottomwear, setOf(ClothingDepartment.Men, ClothingDepartment.Women)),
    ClothingType("track-pants", "Track pants", ClothingGroup.Bottomwear, setOf(ClothingDepartment.Men, ClothingDepartment.Women)),
    ClothingType("jeggings", "Jeggings", ClothingGroup.Bottomwear, setOf(ClothingDepartment.Women)),
    ClothingType("leggings", "Leggings", ClothingGroup.Bottomwear, setOf(ClothingDepartment.Women)),
    ClothingType("palazzos", "Palazzos", ClothingGroup.Bottomwear, setOf(ClothingDepartment.Women)),
    ClothingType("culottes", "Culottes", ClothingGroup.Bottomwear, setOf(ClothingDepartment.Women)),
    ClothingType("skirts", "Skirts", ClothingGroup.Bottomwear, setOf(ClothingDepartment.Women)),
    ClothingType("co-ord-sets", "Co-ord sets", ClothingGroup.Outfits, setOf(ClothingDepartment.Men, ClothingDepartment.Women)),
    ClothingType("suits", "Suits", ClothingGroup.Outfits, setOf(ClothingDepartment.Men)),
    ClothingType("tracksuits", "Tracksuits", ClothingGroup.Outfits, setOf(ClothingDepartment.Men, ClothingDepartment.Women)),
    ClothingType("dresses", "Dresses", ClothingGroup.Outfits, setOf(ClothingDepartment.Women)),
    ClothingType("jumpsuits", "Jumpsuits", ClothingGroup.Outfits, setOf(ClothingDepartment.Women)),
    ClothingType("playsuits", "Playsuits", ClothingGroup.Outfits, setOf(ClothingDepartment.Women)),
    ClothingType("kurtas", "Kurtas", ClothingGroup.Ethnic, setOf(ClothingDepartment.Men, ClothingDepartment.Women)),
    ClothingType("kurta-pyjama-sets", "Kurta-pyjama sets", ClothingGroup.Ethnic, setOf(ClothingDepartment.Men)),
    ClothingType("sherwanis", "Sherwanis", ClothingGroup.Ethnic, setOf(ClothingDepartment.Men)),
    ClothingType("nehru-jackets", "Nehru jackets", ClothingGroup.Ethnic, setOf(ClothingDepartment.Men)),
    ClothingType("dhotis", "Dhotis", ClothingGroup.Ethnic, setOf(ClothingDepartment.Men)),
    ClothingType("kurtis", "Kurtis", ClothingGroup.Ethnic, setOf(ClothingDepartment.Women)),
    ClothingType("kurta-sets", "Kurta sets", ClothingGroup.Ethnic, setOf(ClothingDepartment.Women)),
    ClothingType("salwar-suits", "Salwar suits", ClothingGroup.Ethnic, setOf(ClothingDepartment.Women)),
    ClothingType("churidars", "Churidars", ClothingGroup.Ethnic, setOf(ClothingDepartment.Women)),
    ClothingType("sarees", "Sarees", ClothingGroup.Ethnic, setOf(ClothingDepartment.Women)),
    ClothingType("saree-blouses", "Saree blouses", ClothingGroup.Ethnic, setOf(ClothingDepartment.Women)),
    ClothingType("lehengas", "Lehengas", ClothingGroup.Ethnic, setOf(ClothingDepartment.Women)),
    ClothingType("dupattas", "Dupattas", ClothingGroup.Ethnic, setOf(ClothingDepartment.Women)),
    ClothingType("workout-tshirts", "Workout T-shirts", ClothingGroup.Activewear, setOf(ClothingDepartment.Men)),
    ClothingType("workout-tops", "Workout tops", ClothingGroup.Activewear, setOf(ClothingDepartment.Women)),
    ClothingType("training-shorts", "Training shorts", ClothingGroup.Activewear, setOf(ClothingDepartment.Men, ClothingDepartment.Women)),
    ClothingType("sports-jackets", "Sports jackets", ClothingGroup.Activewear, setOf(ClothingDepartment.Men, ClothingDepartment.Women)),
    ClothingType("swimwear", "Swimwear", ClothingGroup.Activewear, setOf(ClothingDepartment.Men, ClothingDepartment.Women)),
    ClothingType("sports-bras", "Sports bras", ClothingGroup.Activewear, setOf(ClothingDepartment.Women)),
    ClothingType("workout-leggings", "Workout leggings", ClothingGroup.Activewear, setOf(ClothingDepartment.Women)),
    ClothingType("pyjamas", "Pyjamas", ClothingGroup.Sleepwear, setOf(ClothingDepartment.Men, ClothingDepartment.Women)),
    ClothingType("night-suits", "Night suits", ClothingGroup.Sleepwear, setOf(ClothingDepartment.Men, ClothingDepartment.Women)),
    ClothingType("lounge-pants", "Lounge pants", ClothingGroup.Sleepwear, setOf(ClothingDepartment.Men)),
    ClothingType("lounge-shorts", "Lounge shorts", ClothingGroup.Sleepwear, setOf(ClothingDepartment.Men)),
    ClothingType("nightdresses", "Nightdresses", ClothingGroup.Sleepwear, setOf(ClothingDepartment.Women)),
    ClothingType("nightshirts", "Nightshirts", ClothingGroup.Sleepwear, setOf(ClothingDepartment.Women)),
    ClothingType("lounge-sets", "Lounge sets", ClothingGroup.Sleepwear, setOf(ClothingDepartment.Women)),
    ClothingType("robes", "Robes", ClothingGroup.Sleepwear, setOf(ClothingDepartment.Women)),
    ClothingType("briefs", "Briefs", ClothingGroup.Innerwear, setOf(ClothingDepartment.Men, ClothingDepartment.Women)),
    ClothingType("trunks", "Trunks", ClothingGroup.Innerwear, setOf(ClothingDepartment.Men)),
    ClothingType("boxers", "Boxers", ClothingGroup.Innerwear, setOf(ClothingDepartment.Men)),
    ClothingType("undershirts", "Undershirts", ClothingGroup.Innerwear, setOf(ClothingDepartment.Men)),
    ClothingType("vests", "Vests", ClothingGroup.Innerwear, setOf(ClothingDepartment.Men)),
    ClothingType("bras", "Bras", ClothingGroup.Innerwear, setOf(ClothingDepartment.Women)),
    ClothingType("slips", "Slips", ClothingGroup.Innerwear, setOf(ClothingDepartment.Women)),
    ClothingType("shapewear", "Shapewear", ClothingGroup.Innerwear, setOf(ClothingDepartment.Women)),
    ClothingType("sweaters", "Sweaters", ClothingGroup.Winterwear, setOf(ClothingDepartment.Men, ClothingDepartment.Women)),
    ClothingType("cardigans", "Cardigans", ClothingGroup.Winterwear, setOf(ClothingDepartment.Men, ClothingDepartment.Women)),
    ClothingType("sweatshirts", "Sweatshirts", ClothingGroup.Winterwear, setOf(ClothingDepartment.Men, ClothingDepartment.Women)),
    ClothingType("hoodies", "Hoodies", ClothingGroup.Winterwear, setOf(ClothingDepartment.Men, ClothingDepartment.Women)),
    ClothingType("fleece-jackets", "Fleece jackets", ClothingGroup.Winterwear, setOf(ClothingDepartment.Men, ClothingDepartment.Women)),
    ClothingType("padded-jackets", "Padded jackets", ClothingGroup.Winterwear, setOf(ClothingDepartment.Men, ClothingDepartment.Women)),
    ClothingType("coats", "Coats", ClothingGroup.Winterwear, setOf(ClothingDepartment.Men, ClothingDepartment.Women)),
    ClothingType("thermal-tops", "Thermal tops", ClothingGroup.Winterwear, setOf(ClothingDepartment.Men, ClothingDepartment.Women)),
    ClothingType("thermal-bottoms", "Thermal bottoms", ClothingGroup.Winterwear, setOf(ClothingDepartment.Men, ClothingDepartment.Women)),
    ClothingType("thermal-sets", "Thermal sets", ClothingGroup.Winterwear, setOf(ClothingDepartment.Men, ClothingDepartment.Women)),
    ClothingType("shrugs", "Shrugs", ClothingGroup.Winterwear, setOf(ClothingDepartment.Women)),
    ClothingType("shawls", "Shawls", ClothingGroup.Winterwear, setOf(ClothingDepartment.Women)),
)
data class ClothingDetails(
    val department: ClothingDepartment, val typeId: String, val colors: List<String>, val sizes: List<String>,
    val fabric: String, val fit: String, val pattern: String, val occasion: String, val season: String,
    val winterWear: Boolean, val sleeve: String? = null, val neckline: String? = null, val rise: String? = null,
) {
    init {
        require(clothingTypes.any { it.id == typeId && department in it.departments })
        require(colors.isNotEmpty() && sizes.isNotEmpty())
    }
    val type: ClothingType get() = clothingTypes.first { it.id == typeId }
    fun variant(color: String, size: String): String {
        require(color in colors && size in sizes); return "$color / $size"
    }
}

data class ClothingFilter(
    val department: ClothingDepartment = ClothingDepartment.Men,
    val typeId: String? = null, val winterOnly: Boolean = false,
    val size: String? = null, val color: String? = null, val fabric: String? = null,
    val fit: String? = null, val pattern: String? = null, val occasion: String? = null,
    val season: String? = null, val sleeve: String? = null, val neckline: String? = null,
    val rise: String? = null, val minPrice: Long? = null, val maxPrice: Long? = null,
) {
    val attributeCount: Int get() = listOf(size, color, fabric, fit, pattern, occasion, season, sleeve, neckline, rise).count { it != null } +
        if (minPrice != null || maxPrice != null) 1 else 0
    fun matches(product: Product): Boolean {
        val c = product.clothing ?: return false
        return c.department == department && (typeId == null || c.typeId == typeId) &&
            (!winterOnly || c.winterWear) && (size == null || size in c.sizes) &&
            (color == null || color in c.colors) && (fabric == null || fabric == c.fabric) &&
            (fit == null || fit == c.fit) && (pattern == null || pattern == c.pattern) &&
            (occasion == null || occasion == c.occasion) && (season == null || season == c.season) &&
            (sleeve == null || sleeve == c.sleeve) && (neckline == null || neckline == c.neckline) &&
            (rise == null || rise == c.rise) && (minPrice == null || product.price.minorUnits >= minPrice) &&
            (maxPrice == null || product.price.minorUnits <= maxPrice)
    }
}
