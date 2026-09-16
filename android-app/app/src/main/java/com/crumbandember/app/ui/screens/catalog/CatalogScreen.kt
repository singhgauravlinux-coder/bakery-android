package com.crumbandember.app.ui.screens.catalog

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.LocationOn
import androidx.compose.material.icons.filled.Search
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.crumbandember.app.data.model.Product
import com.crumbandember.app.data.repository.ProductRepository
import com.crumbandember.app.ui.components.*
import com.crumbandember.app.util.Resource
import com.crumbandember.app.util.ViewModelFactory

private val HERO_SLIDES = listOf(
    HeroSlide(
        title = "Freshly Baked\nToday",
        subtitle = "Cakes, pastries, bread & more",
        ctaLabel = "Order Now",
        accentColors = listOf(Color(0xFF241207), Color(0xFF7A5233)),
        emoji = "🍰"
    ),
    HeroSlide(
        title = "Rich, Decadent\nChocolate Cake",
        subtitle = "Layers of pure chocolate bliss",
        ctaLabel = "Explore",
        accentColors = listOf(Color(0xFF3B2417), Color(0xFFD9752B)),
        emoji = "🍫"
    ),
    HeroSlide(
        title = "Sweet Moments\nCupcakes",
        subtitle = "Delightful bakes for every celebration",
        ctaLabel = "Shop Now",
        accentColors = listOf(Color(0xFFB3547A), Color(0xFFD9752B)),
        emoji = "🧁"
    )
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

    Scaffold(
        topBar = {
            BakeryTopBar(
                title = "Crumb & Ember",
                cartCount = cartCount,
                onCartClick = onCartClick,
                onNotificationsClick = {}
            )
        },
        bottomBar = {
            BakeryBottomBar(
                selected = BakeryTab.HOME,
                cartCount = cartCount,
                onSelect = { tab ->
                    when (tab) {
                        BakeryTab.HOME -> {}
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
                    padding = padding,
                    userGreetingName = userGreetingName
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
    padding: PaddingValues,
    userGreetingName: String?
) {
    val categories = remember(products) {
        products.mapNotNull { it.category?.trim() }.filter { it.isNotBlank() }.distinct().take(8)
    }
    var selectedCategory by remember { mutableStateOf<String?>(null) }
    val filtered = remember(products, selectedCategory) {
        if (selectedCategory == null) products else products.filter { it.category?.trim() == selectedCategory }
    }

    LazyColumnWithGrid(padding = padding) {
        item {
            Column(modifier = Modifier.fillMaxWidth().padding(horizontal = 20.dp, vertical = 12.dp)) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(Icons.Filled.LocationOn, contentDescription = null, tint = MaterialTheme.colorScheme.primary, modifier = Modifier.size(16.dp))
                    Spacer(Modifier.width(4.dp))
                    Text("Bangalore, Karnataka", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                }
                Spacer(Modifier.height(6.dp))
                Text(
                    if (userGreetingName != null) "Good Morning, $userGreetingName 👋" else "Good Morning 👋",
                    style = MaterialTheme.typography.headlineSmall
                )
            }

            OutlinedTextField(
                value = query,
                onValueChange = onQueryChange,
                label = { Text("Search for cakes, pastries, bread…") },
                singleLine = true,
                shape = RoundedCornerShape(50),
                leadingIcon = { Icon(Icons.Filled.Search, contentDescription = null) },
                trailingIcon = { IconButton(onClick = onSearch) { Icon(Icons.Filled.Search, contentDescription = "Search") } },
                modifier = Modifier.fillMaxWidth().padding(horizontal = 20.dp)
            )

            Spacer(Modifier.height(18.dp))
            Hero3DCarousel(slides = HERO_SLIDES, onCtaClick = {}, modifier = Modifier.fillMaxWidth())
            Spacer(Modifier.height(20.dp))

            if (categories.isNotEmpty()) {
                SectionHeader(title = "Categories")
                LazyRow(
                    contentPadding = PaddingValues(horizontal = 20.dp),
                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    items(categories) { category ->
                        val selected = selectedCategory == category
                        FilterChip(
                            selected = selected,
                            onClick = { selectedCategory = if (selected) null else category },
                            label = { Text(category) },
                            shape = RoundedCornerShape(50)
                        )
                    }
                }
                Spacer(Modifier.height(12.dp))
            }

            SectionHeader(title = if (selectedCategory != null) selectedCategory!! else "Popular Today")
        }

        if (filtered.isEmpty()) {
            item {
                Box(modifier = Modifier.fillMaxWidth().padding(32.dp), contentAlignment = Alignment.Center) {
                    Text("No products found", color = MaterialTheme.colorScheme.onSurfaceVariant)
                }
            }
        } else {
            items(filtered.chunked(2)) { pair ->
                Row(
                    modifier = Modifier.fillMaxWidth().padding(horizontal = 20.dp, vertical = 8.dp),
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

        item { Spacer(Modifier.height(24.dp)) }
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
