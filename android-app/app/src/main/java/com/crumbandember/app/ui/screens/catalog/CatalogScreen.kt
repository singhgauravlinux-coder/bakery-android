package com.crumbandember.app.ui.screens.catalog

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowForward
import androidx.compose.material.icons.filled.Clear
import androidx.compose.material.icons.filled.DeliveryDining
import androidx.compose.material.icons.filled.KeyboardArrowDown
import androidx.compose.material.icons.filled.LocationOn
import androidx.compose.material.icons.filled.Search
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.crumbandember.app.data.model.Product
import com.crumbandember.app.data.repository.ProductRepository
import com.crumbandember.app.ui.components.*
import com.crumbandember.app.util.DeviceLocation
import com.crumbandember.app.util.Resource
import com.crumbandember.app.util.ViewModelFactory

private val HERO_SLIDES = listOf(
    HeroSlide(
        title = "Freshly Baked\nEveryday",
        subtitle = "Golden, flaky croissants made with the finest ingredients.",
        ctaLabel = "Explore",
        accentColors = listOf(Color(0xFF2C160A), Color(0xFF8C5A2E)),
        emoji = "🥐",
        targetCategory = "viennoiserie"
    ),
    HeroSlide(
        title = "Rich, Decadent\nChocolate Cake",
        subtitle = "Indulge in layers of pure chocolate bliss, freshly baked for your special moments.",
        ctaLabel = "Order Now",
        accentColors = listOf(Color(0xFF241207), Color(0xFFD9752B)),
        emoji = "🍫",
        targetCategory = "patisserie"
    ),
    HeroSlide(
        title = "Sweet Moments\nCupcakes",
        subtitle = "Delightful cupcakes for every celebration and little joy.",
        ctaLabel = "Shop Now",
        accentColors = listOf(Color(0xFF702040), Color(0xFFD9752B)),
        emoji = "🧁",
        targetCategory = "patisserie"
    )
)

private data class CategoryItem(val id: String?, val label: String, val icon: String)

// Ids must match product-catalog-service's real categories (bread /
// viennoiserie / patisserie) — the old list ("cakes", "cookies",
// "cupcakes", "desserts") didn't match anything in the catalog, so tapping
// those chips always rendered "No bakery items found".
private val CURATED_CATEGORIES = listOf(
    CategoryItem(null, "All", "⊞"),
    CategoryItem("bread", "Bread", "🍞"),
    CategoryItem("viennoiserie", "Viennoiserie", "🥐"),
    CategoryItem("patisserie", "Patisserie", "🍮")
)

@Composable
fun CatalogScreen(
    productRepository: ProductRepository,
    cartCount: Int,
    onProductClick: (String) -> Unit,
    onCartClick: () -> Unit,
    onProfileClick: () -> Unit,
    onOrdersClick: () -> Unit = {},
    userGreetingName: String? = null
) {
    val viewModel: CatalogViewModel = viewModel(factory = ViewModelFactory { CatalogViewModel(productRepository) })
    val state by viewModel.products.collectAsState()
    val query by viewModel.query.collectAsState()
    var searchFocused by remember { mutableStateOf(false) }

    Scaffold(
        topBar = {
            BakeryTopBar(
                title = "Crumb & Ember",
                cartCount = cartCount,
                onCartClick = onCartClick,
                onNotificationsClick = {},
                onProfileClick = onProfileClick
            )
        },
        bottomBar = {
            BakeryBottomBar(
                selected = if (searchFocused) BakeryTab.SEARCH else BakeryTab.HOME,
                cartCount = cartCount,
                onSelect = { tab ->
                    when (tab) {
                        BakeryTab.HOME -> {
                            searchFocused = false
                        }
                        BakeryTab.SEARCH -> {
                            searchFocused = true
                        }
                        BakeryTab.ORDERS -> onOrdersClick()
                        BakeryTab.CART -> onCartClick()
                        BakeryTab.PROFILE -> onProfileClick()
                    }
                }
            )
        }
    ) { padding ->
        when (val s = state) {
            is Resource.Loading, Resource.Idle -> LoadingSkeleton(modifier = Modifier.padding(padding).padding(top = 16.dp))
            is Resource.Error -> UnavailableScreen(s.kind, "our menu", onRetry = { viewModel.loadProducts() })
            is Resource.Success -> {
                CatalogContent(
                    products = s.data,
                    query = query,
                    onQueryChange = viewModel::onQueryChange,
                    onSearch = viewModel::search,
                    onProductClick = onProductClick,
                    onOrdersClick = onOrdersClick,
                    padding = padding,
                    userGreetingName = userGreetingName,
                    searchRequested = searchFocused
                )
            }
        }
    }
}

@Composable
private fun CatalogContent(
    products: List<Product>,
    query: String,
    onQueryChange: (String) -> Unit,
    onSearch: () -> Unit,
    onProductClick: (String) -> Unit,
    onOrdersClick: () -> Unit,
    padding: PaddingValues,
    userGreetingName: String?,
    searchRequested: Boolean
) {
    var selectedCategoryId by remember { mutableStateOf<String?>(null) }

    val filtered = remember(products, selectedCategoryId) {
        if (selectedCategoryId == null) products
        else products.filter { it.category?.trim()?.lowercase()?.contains(selectedCategoryId!!) == true }
    }

    val context = LocalContext.current
    var locationLabel by remember { mutableStateOf<String?>("Locating…") }
    LaunchedEffect(Unit) {
        locationLabel = DeviceLocation.currentCityState(context) ?: "Bangalore, Karnataka"
    }

    LazyColumnWithGrid(padding = padding) {
        item {
            // Location and User Greeting Header
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 20.dp, vertical = 10.dp)
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier.clickable { /* location picker */ }
                ) {
                    Icon(
                        Icons.Filled.LocationOn,
                        contentDescription = "Location",
                        tint = MaterialTheme.colorScheme.primary,
                        modifier = Modifier.size(16.dp)
                    )
                    Spacer(Modifier.width(4.dp))
                    Text(
                        locationLabel ?: "Bangalore, Karnataka",
                        style = MaterialTheme.typography.bodySmall,
                        fontWeight = FontWeight.Medium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    Icon(
                        Icons.Filled.KeyboardArrowDown,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.size(16.dp)
                    )
                }

                Spacer(Modifier.height(6.dp))

                Text(
                    if (!userGreetingName.isNullOrBlank()) "Good Morning, $userGreetingName 👋" else "Good Morning, Baker 👋",
                    style = MaterialTheme.typography.headlineSmall,
                    fontWeight = FontWeight.Bold
                )
            }

            // Search Bar
            OutlinedTextField(
                value = query,
                onValueChange = onQueryChange,
                placeholder = { Text("Search for cakes, pastries, bread…") },
                singleLine = true,
                shape = RoundedCornerShape(50),
                leadingIcon = { Icon(Icons.Filled.Search, contentDescription = "Search icon") },
                trailingIcon = {
                    if (query.isNotBlank()) {
                        IconButton(onClick = { onQueryChange("") }) {
                            Icon(Icons.Filled.Clear, contentDescription = "Clear search")
                        }
                    } else {
                        IconButton(onClick = onSearch) {
                            Icon(Icons.Filled.Search, contentDescription = "Search button")
                        }
                    }
                },
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 20.dp)
            )

            // Active Order Tracking Card (when present)
            Spacer(Modifier.height(14.dp))
            Surface(
                shape = RoundedCornerShape(18.dp),
                color = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.65f),
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 20.dp)
                    .clickable(onClick = onOrdersClick)
            ) {
                Row(
                    modifier = Modifier.padding(horizontal = 16.dp, vertical = 12.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.weight(1f)) {
                        Text("🔥", fontSize = 20.sp)
                        Spacer(Modifier.width(10.dp))
                        Column {
                            Text(
                                "Order in the Oven",
                                style = MaterialTheme.typography.labelSmall,
                                color = MaterialTheme.colorScheme.primary,
                                fontWeight = FontWeight.Bold
                            )
                            Text(
                                "Fresh Sourdough is currently Baking",
                                style = MaterialTheme.typography.bodySmall,
                                fontWeight = FontWeight.Medium
                            )
                        }
                    }
                    Text(
                        "Track ›",
                        style = MaterialTheme.typography.labelMedium,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.primary
                    )
                }
            }

            // 3D Rolling Hero Carousel
            Spacer(Modifier.height(16.dp))
            Hero3DCarousel(
                slides = HERO_SLIDES,
                onCtaClick = { slide ->
                    // Route to a product in the slide's target category. The old
                    // check only ever looked for a "cake" category — which no
                    // product in the catalog has — so every CTA silently did
                    // nothing. We now fall back through category match, then a
                    // loose keyword match against the slide's own copy, then
                    // simply the first available product, so the button always
                    // takes the user somewhere.
                    val byCategory = products.firstOrNull {
                        it.category?.equals(slide.targetCategory, ignoreCase = true) == true
                    }
                    val keywords = (slide.title + " " + slide.subtitle).lowercase()
                    val byKeyword = products.firstOrNull { p ->
                        keywords.split(Regex("\\W+")).filter { it.length > 3 }
                            .any { word -> p.name.contains(word, ignoreCase = true) || p.description?.contains(word, ignoreCase = true) == true }
                    }
                    val target = byCategory ?: byKeyword ?: products.firstOrNull()
                    target?.let { onProductClick(it.id) }
                },
                modifier = Modifier.fillMaxWidth()
            )

            // Categories Section with Circular Icons
            Spacer(Modifier.height(20.dp))
            SectionHeader(title = "Categories")
            LazyRow(
                contentPadding = PaddingValues(horizontal = 20.dp),
                horizontalArrangement = Arrangement.spacedBy(14.dp)
            ) {
                items(CURATED_CATEGORIES) { cat ->
                    val isSelected = selectedCategoryId == cat.id
                    Column(
                        horizontalAlignment = Alignment.CenterHorizontally,
                        modifier = Modifier.clickable {
                            selectedCategoryId = if (isSelected && cat.id != null) null else cat.id
                        }
                    ) {
                        Box(
                            modifier = Modifier
                                .size(56.dp)
                                .clip(CircleShape)
                                .background(
                                    if (isSelected) MaterialTheme.colorScheme.primary
                                    else MaterialTheme.colorScheme.surfaceVariant
                                ),
                            contentAlignment = Alignment.Center
                        ) {
                            Text(
                                cat.icon,
                                fontSize = 24.sp
                            )
                        }
                        Spacer(Modifier.height(6.dp))
                        Text(
                            cat.label,
                            style = MaterialTheme.typography.labelSmall,
                            fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal,
                            color = if (isSelected) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurface
                        )
                    }
                }
            }

            Spacer(Modifier.height(18.dp))
            SectionHeader(
                title = if (selectedCategoryId != null) {
                    CURATED_CATEGORIES.firstOrNull { it.id == selectedCategoryId }?.label ?: "Popular Today"
                } else "Popular Today",
                actionLabel = "View all",
                onAction = { selectedCategoryId = null }
            )
        }

        // Product Grid
        if (filtered.isEmpty()) {
            item {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(40.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Text("🧁", fontSize = 36.sp)
                        Spacer(Modifier.height(8.dp))
                        Text(
                            "No bakery items found",
                            style = MaterialTheme.typography.titleMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
            }
        } else {
            items(filtered.chunked(2)) { pair ->
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 20.dp, vertical = 8.dp),
                    horizontalArrangement = Arrangement.spacedBy(14.dp)
                ) {
                    pair.forEach { product ->
                        ProductCard(
                            product = product,
                            onClick = { onProductClick(product.id) },
                            modifier = Modifier.weight(1f)
                        )
                    }
                    if (pair.size == 1) Spacer(Modifier.weight(1f))
                }
            }
        }

        // Special Festive Collection Card
        item {
            Spacer(Modifier.height(16.dp))
            Surface(
                shape = RoundedCornerShape(24.dp),
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 20.dp)
            ) {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .background(
                            Brush.linearGradient(
                                listOf(
                                    Color(0xFF2C160A),
                                    Color(0xFF7A5233)
                                )
                            )
                        )
                        .padding(20.dp)
                ) {
                    Column(modifier = Modifier.fillMaxWidth(0.72f)) {
                        Surface(
                            shape = RoundedCornerShape(50),
                            color = Color(0xFFD9752B),
                            modifier = Modifier.padding(bottom = 8.dp)
                        ) {
                            Text(
                                "FESTIVE EDITION",
                                style = MaterialTheme.typography.labelSmall,
                                color = Color.White,
                                fontWeight = FontWeight.Bold,
                                modifier = Modifier.padding(horizontal = 10.dp, vertical = 4.dp)
                            )
                        }
                        Text(
                            "Special Festive Collection",
                            style = MaterialTheme.typography.titleMedium,
                            color = Color.White,
                            fontWeight = FontWeight.Bold
                        )
                        Spacer(Modifier.height(4.dp))
                        Text(
                            "Premium artisanal cakes and spiced loaves for your joyous moments.",
                            style = MaterialTheme.typography.bodySmall,
                            color = Color.White.copy(alpha = 0.85f)
                        )
                        Spacer(Modifier.height(12.dp))
                        Surface(
                            shape = RoundedCornerShape(50),
                            color = Color.White,
                            modifier = Modifier.clickable {
                                products.firstOrNull()?.let { onProductClick(it.id) }
                            }
                        ) {
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                modifier = Modifier.padding(horizontal = 14.dp, vertical = 6.dp)
                            ) {
                                Text(
                                    "Shop Now",
                                    style = MaterialTheme.typography.labelMedium,
                                    fontWeight = FontWeight.Bold,
                                    color = Color(0xFF2C160A)
                                )
                                Spacer(Modifier.width(4.dp))
                                Icon(
                                    Icons.Filled.ArrowForward,
                                    contentDescription = null,
                                    tint = Color(0xFF2C160A),
                                    modifier = Modifier.size(14.dp)
                                )
                            }
                        }
                    }
                    Text(
                        "🎂",
                        fontSize = 52.sp,
                        modifier = Modifier.align(Alignment.CenterEnd)
                    )
                }
            }
        }

        // Recommended For You section
        if (products.size > 2) {
            item {
                Spacer(Modifier.height(20.dp))
                SectionHeader(title = "Recommended For You")
                LazyRow(
                    contentPadding = PaddingValues(horizontal = 20.dp),
                    horizontalArrangement = Arrangement.spacedBy(14.dp)
                ) {
                    items(products.reversed().take(5)) { product ->
                        ProductCard(
                            product = product,
                            onClick = { onProductClick(product.id) },
                            modifier = Modifier.width(180.dp)
                        )
                    }
                }
            }
        }

        item { Spacer(Modifier.height(28.dp)) }
    }
}

/** Small wrapper so the section above can mix header items and 2-up product rows in one scroll. */
@Composable
private fun LazyColumnWithGrid(
    padding: PaddingValues,
    content: androidx.compose.foundation.lazy.LazyListScope.() -> Unit
) {
    androidx.compose.foundation.lazy.LazyColumn(
        modifier = Modifier.padding(padding).fillMaxSize(),
        content = content
    )
}
