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
                Column(
                    modifier = Modifier
                        .padding(padding)
                        .fillMaxSize()
                ) {
                    Box(modifier = Modifier.fillMaxWidth().height(280.dp).padding(16.dp)) {
                        ParallaxImage(
                            colors = listOf(
                                MaterialTheme.colorScheme.primary,
                                MaterialTheme.colorScheme.primaryContainer
                            ),
                            emoji = categoryEmoji(product.category),
                            modifier = Modifier.fillMaxSize()
                        )
                        FavoriteButton(
                            favorited = favorited,
                            onToggle = { favorited = !favorited },
                            modifier = Modifier.align(Alignment.TopEnd).padding(12.dp)
                        )
                    }

                    Column(
                        modifier = Modifier
                            .weight(1f)
                            .padding(horizontal = 24.dp)
                    ) {
                        Text(product.name, style = MaterialTheme.typography.headlineSmall)
                        product.category?.let {
                            Spacer(Modifier.height(4.dp))
                            Text(it, style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.primary)
                        }
                        Spacer(Modifier.height(12.dp))
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Text(formatPrice(product.price), style = MaterialTheme.typography.headlineSmall)
                            Spacer(Modifier.width(10.dp))
                            AssistChip(onClick = {}, label = { Text("In Stock") })
                        }
                        Spacer(Modifier.height(16.dp))
                        product.description?.let {
                            Text(it, style = MaterialTheme.typography.bodyLarge, color = MaterialTheme.colorScheme.onSurfaceVariant)
                        }

                        Spacer(Modifier.height(28.dp))
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Text("Quantity", style = MaterialTheme.typography.titleMedium)
                            Spacer(Modifier.width(20.dp))
                            QuantityStepper(
                                quantity = quantity,
                                onDecrease = { if (quantity > 1) quantity-- },
                                onIncrease = { quantity++ }
                            )
                        }
                    }

                    Surface(tonalElevation = 4.dp) {
                        Column(modifier = Modifier.padding(20.dp)) {
                            AnimatedButton(
                                text = if (addState is Resource.Loading) "Adding…" else "Add to Cart · ${formatPrice(product.price * quantity)}",
                                onClick = { viewModel.addToCart(quantity) },
                                loading = addState is Resource.Loading,
                                icon = Icons.Filled.ShoppingCartCheckout,
                                modifier = Modifier.fillMaxWidth()
                            )
                            Spacer(Modifier.height(8.dp))
                            OutlinedButton(onClick = onGoToCart, modifier = Modifier.fillMaxWidth(), shape = RoundedCornerShape(16.dp)) {
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
