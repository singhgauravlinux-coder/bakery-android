package com.crumbandember.app.ui.screens.catalog

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Remove
import androidx.compose.material.icons.filled.ShoppingCartCheckout
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.crumbandember.app.data.repository.CartRepository
import com.crumbandember.app.data.repository.ProductRepository
import com.crumbandember.app.ui.components.*
import com.crumbandember.app.util.Resource
import com.crumbandember.app.util.ViewModelFactory
import kotlinx.coroutines.launch

@Composable
fun ProductDetailScreen(
    productId: String,
    userId: String,
    productRepository: ProductRepository,
    cartRepository: CartRepository,
    onBack: () -> Unit,
    onGoToCart: () -> Unit
) {
    val viewModel: ProductDetailViewModel = viewModel(
        factory = ViewModelFactory { ProductDetailViewModel(productId, productRepository, cartRepository, userId) }
    )
    val productState by viewModel.product.collectAsState()
    val addState by viewModel.addToCartState.collectAsState()
    var quantity by remember { mutableStateOf(1) }
    var favorited by remember { mutableStateOf(false) }
    val snackbarHostState = remember { SnackbarHostState() }
    val scope = rememberCoroutineScope()

    LaunchedEffect(addState) {
        if (addState is Resource.Success) {
            scope.launch { snackbarHostState.showSnackbar("Added to cart 🎉") }
            viewModel.resetAddToCartState()
        } else if (addState is Resource.Error) {
            val err = addState as Resource.Error
            scope.launch { snackbarHostState.showSnackbar(friendlyInlineMessage(err.kind, err.message)) }
        }
    }

    Scaffold(
        snackbarHost = { SnackbarHost(snackbarHostState) },
        topBar = { BakeryDetailTopBar(title = "Product Details", onBack = onBack) }
    ) { padding ->
        when (val s = productState) {
            is Resource.Loading, Resource.Idle -> LoadingSkeleton(modifier = Modifier.padding(padding).padding(top = 16.dp))
            is Resource.Error -> UnavailableScreen(s.kind, "this product", onRetry = viewModel::retry)
            is Resource.Success -> {
                val product = s.data
                var selectedSize by remember { mutableStateOf("1 kg") }
                val sizeOptions = listOf("500 g", "1 kg", "1.5 kg")
                val sizeMultiplier = when (selectedSize) {
                    "500 g" -> 0.6
                    "1.5 kg" -> 1.45
                    else -> 1.0
                }
                val effectivePrice = (product.price * sizeMultiplier).toInt().toDouble()

                Column(
                    modifier = Modifier
                        .padding(padding)
                        .fillMaxSize()
                        .verticalScroll(androidx.compose.foundation.rememberScrollState())
                ) {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(260.dp)
                            .padding(horizontal = 20.dp, vertical = 10.dp)
                    ) {
                        ParallaxImage(
                            colors = listOf(
                                Color(0xFF3B2417),
                                Color(0xFFD9752B)
                            ),
                            emoji = categoryEmoji(product.category),
                            modifier = Modifier.fillMaxSize()
                        )
                        FavoriteButton(
                            favorited = favorited,
                            onToggle = { favorited = !favorited },
                            modifier = Modifier
                                .align(Alignment.TopEnd)
                                .padding(12.dp)
                        )
                    }

                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 22.dp)
                    ) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.Top
                        ) {
                            Column(modifier = Modifier.weight(1f)) {
                                Text(
                                    product.name,
                                    style = MaterialTheme.typography.headlineSmall,
                                    fontWeight = FontWeight.Bold
                                )
                                product.category?.let {
                                    Spacer(Modifier.height(2.dp))
                                    Text(
                                        it.replaceFirstChar { c -> c.uppercase() },
                                        style = MaterialTheme.typography.bodyMedium,
                                        color = MaterialTheme.colorScheme.primary,
                                        fontWeight = FontWeight.Medium
                                    )
                                }
                            }
                            Text(
                                formatPrice(effectivePrice),
                                style = MaterialTheme.typography.headlineSmall,
                                fontWeight = FontWeight.Bold,
                                color = MaterialTheme.colorScheme.primary
                            )
                        }

                        Spacer(Modifier.height(10.dp))

                        // Rating & In-Stock Badges
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(10.dp)
                        ) {
                            Surface(
                                shape = RoundedCornerShape(50),
                                color = MaterialTheme.colorScheme.surfaceVariant
                            ) {
                                Row(
                                    verticalAlignment = Alignment.CenterVertically,
                                    modifier = Modifier.padding(horizontal = 10.dp, vertical = 4.dp)
                                ) {
                                    Text("⭐", fontSize = androidx.compose.ui.unit.sp(12))
                                    Spacer(Modifier.width(4.dp))
                                    Text(
                                        "4.8 (124 reviews)",
                                        style = MaterialTheme.typography.labelMedium,
                                        fontWeight = FontWeight.Bold
                                    )
                                }
                            }

                            Surface(
                                shape = RoundedCornerShape(50),
                                color = Color(0xFFE8F5E9)
                            ) {
                                Text(
                                    "In Stock",
                                    style = MaterialTheme.typography.labelSmall,
                                    color = Color(0xFF2E7D32),
                                    fontWeight = FontWeight.Bold,
                                    modifier = Modifier.padding(horizontal = 10.dp, vertical = 4.dp)
                                )
                            }
                        }

                        Spacer(Modifier.height(18.dp))
                        Text(
                            "Description",
                            style = MaterialTheme.typography.titleSmall,
                            fontWeight = FontWeight.Bold
                        )
                        Spacer(Modifier.height(4.dp))
                        Text(
                            product.description ?: "Handcrafted using traditional fermentation techniques and premium ingredients.",
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            lineHeight = androidx.compose.ui.unit.sp(22)
                        )

                        Spacer(Modifier.height(20.dp))
                        Text(
                            "Weight / Size",
                            style = MaterialTheme.typography.titleSmall,
                            fontWeight = FontWeight.Bold
                        )
                        Spacer(Modifier.height(8.dp))
                        Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                            sizeOptions.forEach { size ->
                                val selected = selectedSize == size
                                Surface(
                                    shape = RoundedCornerShape(50),
                                    color = if (selected) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.surfaceVariant,
                                    modifier = Modifier.clickable { selectedSize = size }
                                ) {
                                    Text(
                                        size,
                                        style = MaterialTheme.typography.labelLarge,
                                        fontWeight = if (selected) FontWeight.Bold else FontWeight.Normal,
                                        color = if (selected) MaterialTheme.colorScheme.onPrimary else MaterialTheme.colorScheme.onSurface,
                                        modifier = Modifier.padding(horizontal = 18.dp, vertical = 8.dp)
                                    )
                                }
                            }
                        }

                        Spacer(Modifier.height(22.dp))
                        Text(
                            "Quantity",
                            style = MaterialTheme.typography.titleSmall,
                            fontWeight = FontWeight.Bold
                        )
                        Spacer(Modifier.height(8.dp))
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.SpaceBetween,
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            QuantityStepper(
                                quantity = quantity,
                                onDecrease = { if (quantity > 1) quantity-- },
                                onIncrease = { quantity++ }
                            )

                            Text(
                                "Total: ${formatPrice(effectivePrice * quantity)}",
                                style = MaterialTheme.typography.titleMedium,
                                fontWeight = FontWeight.Bold
                            )
                        }

                        Spacer(Modifier.height(28.dp))
                    }

                    Surface(
                        tonalElevation = 6.dp,
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Column(modifier = Modifier.padding(20.dp)) {
                            AnimatedButton(
                                text = if (addState is Resource.Loading) "Adding…" else "Add to Cart · ${formatPrice(effectivePrice * quantity)}",
                                onClick = { viewModel.addToCart(quantity) },
                                loading = addState is Resource.Loading,
                                icon = Icons.Filled.ShoppingCartCheckout,
                                modifier = Modifier.fillMaxWidth()
                            )
                            Spacer(Modifier.height(8.dp))
                            OutlinedButton(
                                onClick = onGoToCart,
                                modifier = Modifier.fillMaxWidth(),
                                shape = RoundedCornerShape(16.dp)
                            ) {
                                Text("Go to Cart")
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun QuantityStepper(quantity: Int, onDecrease: () -> Unit, onIncrease: () -> Unit) {
    Surface(shape = RoundedCornerShape(50), color = MaterialTheme.colorScheme.surfaceVariant) {
        Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.padding(4.dp)) {
            IconButton(onClick = onDecrease, modifier = Modifier.size(36.dp)) {
                Icon(Icons.Filled.Remove, contentDescription = "Decrease")
            }
            Text(quantity.toString(), style = MaterialTheme.typography.titleMedium, modifier = Modifier.padding(horizontal = 12.dp))
            IconButton(onClick = onIncrease, modifier = Modifier.size(36.dp)) {
                Icon(Icons.Filled.Add, contentDescription = "Increase")
            }
        }
    }
}

private fun categoryEmoji(category: String?): String = when (category?.trim()?.lowercase()) {
    "cake", "cakes" -> "🍰"
    "pastry", "pastries" -> "🥐"
    "bread" -> "🍞"
    "cookies" -> "🍪"
    "cupcake", "cupcakes" -> "🧁"
    "desserts" -> "🍮"
    else -> "🥖"
}
