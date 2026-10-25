package com.parcel.presentation

import androidx.compose.foundation.*
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.unit.dp
import com.parcel.domain.*

@Composable
fun ClothingControls(state: CatalogState, vm: CatalogViewModel) {
    var showTypes by remember { mutableStateOf(false) }
    var showFilters by remember { mutableStateOf(false) }
    val filter = state.clothingFilter
    Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
        Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
            ClothingDepartment.entries.forEach { department ->
                FilterChip(selected = department == filter.department, onClick = { vm.selectDepartment(department) },
                    label = { Text(department.label) }, modifier = Modifier.weight(1f).testTag("department_${department.name}"))
            }
        }
        Text("Shop by type", style = MaterialTheme.typography.titleSmall)
        Row(Modifier.fillMaxWidth().horizontalScroll(rememberScrollState()).testTag("clothing_shortcuts"), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            FilterChip(selected = filter.typeId == null, onClick = { vm.selectClothingType(null) }, label = { Text(if (filter.winterOnly) "All winterwear" else "All") }, modifier = Modifier.testTag("clothing_type_all"))
            val shortcuts = if (filter.winterOnly) listOf("sweaters", "hoodies", "sweatshirts", "padded-jackets", "coats", "thermal-tops")
                else if (filter.department == ClothingDepartment.Men) listOf("shirts", "tshirts", "jeans", "trousers", "pyjamas")
                else listOf("tops", "tshirts", "dresses", "kurtas", "jeans", "pyjamas")
            shortcuts.forEach { id ->
                val type = clothingTypes.first { it.id == id }
                FilterChip(selected = filter.typeId == id, onClick = { vm.selectClothingType(id) }, label = { Text(if (id == "shirts") "Shirts" else type.label) }, modifier = Modifier.testTag("quick_type_$id"))
            }
        }
        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            FilterChip(selected = filter.winterOnly, onClick = { vm.selectWinter(!filter.winterOnly) }, label = { Text("Winterwear") }, modifier = Modifier.testTag("winterwear_filter"))
            OutlinedButton(onClick = { showTypes = true }, modifier = Modifier.testTag("more_clothing_types")) { Text("All types") }
            OutlinedButton(onClick = { showFilters = true }, modifier = Modifier.testTag("clothing_filters")) { Text("Filters${if (filter.attributeCount > 0) " (${filter.attributeCount})" else ""}") }
        }
        if (filter.attributeCount > 0 || filter.typeId != null || filter.winterOnly) {
            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                Text(listOfNotNull(if (filter.winterOnly) "Winterwear" else null, clothingTypes.find { it.id == filter.typeId }?.label,
                    if (filter.attributeCount > 0) "${filter.attributeCount} filters" else null).joinToString(" · "), style = MaterialTheme.typography.bodySmall, color = Teal, modifier = Modifier.weight(1f))
                TextButton(onClick = vm::resetClothingFilters, modifier = Modifier.testTag("clear_clothing_filters")) { Text("Clear") }
            }
        }
    }
    if (showTypes) ClothingTypeSheet(state, { showTypes = false }) { id -> vm.selectClothingType(id); showTypes = false }
    if (showFilters) ClothingFilterSheet(state, { showFilters = false }) { updated -> vm.applyClothingFilter(updated); showFilters = false }
}

@OptIn(ExperimentalMaterial3Api::class, ExperimentalLayoutApi::class)
@Composable
private fun ClothingTypeSheet(state: CatalogState, dismiss: () -> Unit, select: (String?) -> Unit) {
    val filter = state.clothingFilter
    val products = state.products.filter { product -> product.clothing?.let { it.department == filter.department && (!filter.winterOnly || it.winterWear) } == true }
    ModalBottomSheet(onDismissRequest = dismiss, sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)) {
        LazyColumn(Modifier.fillMaxWidth().fillMaxHeight(.85f).testTag("clothing_type_list"), contentPadding = PaddingValues(20.dp), verticalArrangement = Arrangement.spacedBy(16.dp)) {
            item { Text("${filter.department.label} clothing", style = MaterialTheme.typography.headlineSmall) }
            item { TextButton(onClick = { select(null) }) { Text(if (filter.winterOnly) "All winterwear" else "All clothing") } }
            ClothingGroup.entries.forEach { group ->
                val types = clothingTypes.filter { it.group == group && filter.department in it.departments && products.any { p -> p.clothing?.typeId == it.id } }
                if (types.isNotEmpty()) item {
                    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                        Text(group.label, style = MaterialTheme.typography.titleMedium, color = Teal)
                        FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                            types.forEach { type ->
                                val count = products.count { it.clothing?.typeId == type.id }
                                FilterChip(selected = filter.typeId == type.id, onClick = { select(type.id) }, label = { Text("${type.label} ($count)") }, modifier = Modifier.testTag("type_${type.id}"))
                            }
                        }
                    }
                }
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun ClothingFilterSheet(state: CatalogState, dismiss: () -> Unit, apply: (ClothingFilter) -> Unit) {
    var draft by remember { mutableStateOf(state.clothingFilter) }
    val options = state.products.mapNotNull { it.clothing }.filter { it.department == draft.department &&
        (draft.typeId == null || it.typeId == draft.typeId) && (!draft.winterOnly || it.winterWear) }
    ModalBottomSheet(onDismissRequest = dismiss, sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)) {
        Column(Modifier.fillMaxHeight(.9f).padding(horizontal = 20.dp)) {
            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                Text("Refine your finds", style = MaterialTheme.typography.titleLarge)
                TextButton(onClick = { draft = ClothingFilter(department = draft.department) }, modifier = Modifier.testTag("reset_clothing_sheet")) { Text("Reset") }
            }
            LazyColumn(Modifier.weight(1f).testTag("clothing_filter_list"), verticalArrangement = Arrangement.spacedBy(16.dp), contentPadding = PaddingValues(bottom = 16.dp)) {
                item { Facet("Size", options.flatMap { it.sizes }.distinct(), draft.size, "size") { draft = draft.copy(size = it) } }
                item { Facet("Colour", options.flatMap { it.colors }.distinct(), draft.color, "color") { draft = draft.copy(color = it) } }
                item { Facet("Fit", options.map { it.fit }.distinct(), draft.fit, "fit") { draft = draft.copy(fit = it) } }
                item { Facet("Fabric", options.map { it.fabric }.distinct(), draft.fabric, "fabric") { draft = draft.copy(fabric = it) } }
                item { Facet("Pattern", options.map { it.pattern }.distinct(), draft.pattern, "pattern") { draft = draft.copy(pattern = it) } }
                item { Facet("Occasion", options.map { it.occasion }.distinct(), draft.occasion, "occasion") { draft = draft.copy(occasion = it) } }
                item { Facet("Season", options.map { it.season }.distinct(), draft.season, "season") { draft = draft.copy(season = it) } }
                if (options.any { it.sleeve != null }) item { Facet("Sleeve", options.mapNotNull { it.sleeve }.distinct(), draft.sleeve, "sleeve") { draft = draft.copy(sleeve = it) } }
                if (options.any { it.neckline != null }) item { Facet("Neckline", options.mapNotNull { it.neckline }.distinct(), draft.neckline, "neckline") { draft = draft.copy(neckline = it) } }
                if (options.any { it.rise != null }) item { Facet("Rise", options.mapNotNull { it.rise }.distinct(), draft.rise, "rise") { draft = draft.copy(rise = it) } }
                item {
                    val ranges = listOf(Triple("Any price", null, null), Triple("Under ₹1,000", null, 99999L), Triple("₹1,000–1,999", 100000L, 199999L), Triple("₹2,000–2,999", 200000L, 299999L), Triple("₹3,000+", 300000L, null))
                    val selected = ranges.find { it.second == draft.minPrice && it.third == draft.maxPrice }?.first
                    Facet("Price", ranges.map { it.first }, selected, "price", showAny = false) { label ->
                        val range = ranges.firstOrNull { it.first == label } ?: ranges.first()
                        draft = draft.copy(minPrice = range.second, maxPrice = range.third)
                    }
                }
            }
            Button(onClick = { apply(draft) }, modifier = Modifier.fillMaxWidth().heightIn(min = 52.dp).padding(bottom = 8.dp).testTag("apply_clothing_filters")) {
                val count = filterProducts(state.products, state.query, Category.Clothing, draft).size
                Text("Show $count ${if (count == 1) "item" else "items"}")
            }
        }
    }
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun Facet(title: String, values: List<String>, selected: String?, key: String, showAny: Boolean = true, select: (String?) -> Unit) {
    Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
        Text(title, style = MaterialTheme.typography.titleMedium)
        FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            if (showAny) FilterChip(selected == null, onClick = { select(null) }, label = { Text("Any") }, modifier = Modifier.testTag("facet_${key}_any"))
            values.forEach { value -> FilterChip(selected == value, onClick = { select(if (selected == value && showAny) null else value) }, label = { Text(value) }, modifier = Modifier.testTag("facet_${key}_$value")) }
        }
    }
}

@Composable
fun ClothingSelectors(details: ClothingDetails, color: String, size: String?, selectColor: (String) -> Unit, selectSize: (String) -> Unit) {
    Text("Colour · $color", style = MaterialTheme.typography.titleSmall)
    Row(Modifier.fillMaxWidth().horizontalScroll(rememberScrollState()), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
        details.colors.forEach { choice -> FilterChip(color == choice, onClick = { selectColor(choice) }, label = { Text(choice) }, modifier = Modifier.testTag("clothing_color_$choice")) }
    }
    Text("${if (details.sizes.all { it.all(Char::isDigit) }) "Waist size (inches)" else "Size"} · ${size ?: "Choose a size"}", style = MaterialTheme.typography.titleSmall)
    Row(Modifier.fillMaxWidth().horizontalScroll(rememberScrollState()), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
        details.sizes.forEach { choice -> FilterChip(size == choice, onClick = { selectSize(choice) }, label = { Text(choice) }, modifier = Modifier.testTag("clothing_size_$choice")) }
    }
    Text("${details.fabric} · ${details.fit} fit · ${details.pattern}", style = MaterialTheme.typography.bodySmall, color = Muted)
}
