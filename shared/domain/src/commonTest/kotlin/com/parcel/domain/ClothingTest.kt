package com.parcel.domain

import kotlin.test.*

class ClothingTest {
    private fun product(id: String, department: ClothingDepartment, type: String, winter: Boolean = false, price: Long = 100000) = Product(
        id, "$id cotton clothing", "Comfortable everyday wear", Category.Clothing, Money(price), Money(price + 50000), id,
        "Description", listOf("Navy / M", "Navy / L"), emptyList(), clothing = ClothingDetails(department, type,
            listOf("Navy"), listOf("M", "L"), "Cotton", "Regular", "Solid", "Casual", if (winter) "Winter" else "All season", winter))
    private val menShirt = product("men-shirt", ClothingDepartment.Men, "shirts")
    private val womenShirt = product("women-shirt", ClothingDepartment.Women, "shirts")
    private val menHoodie = product("men-hoodie", ClothingDepartment.Men, "hoodies", true, 200000)
    private val womenSportsJacket = product("women-sports", ClothingDepartment.Women, "sports-jackets", true)
    private val products = listOf(menShirt, womenShirt, menHoodie, womenSportsJacket)

    @Test fun departmentAndTypeIntersectWithoutLeakingTheOtherDepartment() {
        assertEquals(listOf(menShirt), filterProducts(products, "", Category.Clothing, ClothingFilter(typeId = "shirts")))
        assertEquals(listOf(womenShirt), filterProducts(products, "", Category.Clothing, ClothingFilter(ClothingDepartment.Women, "shirts")))
    }
    @Test fun winterCollectionReusesTheSameSportsProductIdentity() {
        val result = filterProducts(products, "", Category.Clothing, ClothingFilter(ClothingDepartment.Women, winterOnly = true))
        assertEquals(listOf(womenSportsJacket), result)
        assertEquals(ClothingGroup.Activewear, result.single().clothing!!.type.group)
        assertEquals("women-sports", result.single().id)
    }
    @Test fun sizeColourFabricAndPriceApplyTogether() {
        assertTrue(ClothingFilter(size = "M", color = "Navy", fabric = "Cotton", minPrice = 100000, maxPrice = 199999).matches(menShirt))
        assertFalse(ClothingFilter(size = "XS").matches(menShirt))
        assertFalse(ClothingFilter(color = "Rose").matches(menShirt))
        assertFalse(ClothingFilter(maxPrice = 199999).matches(menHoodie))
        assertEquals(3, ClothingFilter(size = "M", fabric = "Cotton", maxPrice = 199999).attributeCount)
    }
    @Test fun genderSearchDoesNotMatchMenInsideWomen() {
        assertEquals(listOf(menShirt, menHoodie), filterProducts(products, "men cotton", null))
        assertEquals(listOf(womenShirt, womenSportsJacket), filterProducts(products, "women cotton", null))
        assertEquals(listOf(womenShirt, womenSportsJacket), filterProducts(products, "women's", null))
    }
    @Test fun clothingOptionsOnlyGenerateValidCartVariants() {
        val details = menShirt.clothing!!
        assertEquals("Navy / M", details.variant("Navy", "M"))
        assertFailsWith<IllegalArgumentException> { details.variant("Rose", "M") }
        assertFailsWith<IllegalArgumentException> { details.variant("Navy", "XXL") }
    }
}
