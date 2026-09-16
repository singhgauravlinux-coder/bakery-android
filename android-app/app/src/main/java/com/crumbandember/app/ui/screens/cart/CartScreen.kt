package com.crumbandember.app.ui.screens.cart

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ShoppingCartCheckout
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.crumbandember.app.data.repository.CartRepository
import com.crumbandember.app.data.repository.ProductRepository
import com.crumbandember.app.ui.components.*
import com.crumbandember.app.util.Resource
import com.crumbandember.app.util.ViewModelFactory

@Composable
fun CartScreen(
    userId: String,
    cartRepository: CartRepository,
    productRepository: ProductRepository,
    onBack: () -> Unit,
    onCheckout: () -> Unit
) {
    val viewModel: CartViewModel = viewModel(
        factory = ViewModelFactory { CartViewModel(userId, cartRepository, productRepository) }
    )
    val state by viewModel.lines.collectAsState()
    val total by viewModel.total.collectAsState()

    Scaffold(
        topBar = { BakeryDetailTopBar(title = "Your Cart", onBack = onBack) },
        bottomBar = {
            if (state is Resource.Success && (state as Resource.Success).data.isNotEmpty()) {
                val deliveryCharge = 30.0
                Surface(tonalElevation = 6.dp) {
                    Column(modifier = Modifier.padding(20.dp)) {
                        PriceLine("Subtotal", formatPrice(total))
                        PriceLine("Delivery Charge", formatPrice(deliveryCharge))
                        Divider(modifier = Modifier.padding(vertical = 8.dp))
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Text("Total", style = MaterialTheme.typography.titleMedium)
                            Text(formatPrice(total + deliveryCharge), style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
                        }
                        Spacer(Modifier.height(14.dp))
                        AnimatedButton(
                            text = "Proceed to Checkout",
                            onClick = onCheckout,
                            icon = Icons.Filled.ShoppingCartCheckout,
                            modifier = Modifier.fillMaxWidth()
                        )
                    }
                }
            }
        }
    ) { padding ->
        when (val s = state) {
            is Resource.Loading, Resource.Idle -> LoadingSkeleton(modifier = Modifier.padding(padding).padding(top = 16.dp))
            is Resource.Error -> UnavailableScreen(s.kind, "your cart", onRetry = viewModel::load)
            is Resource.Success -> {
                if (s.data.isEmpty()) {
                    EmptyCart(padding = padding, onBack = onBack)
                } else {
                    LazyColumn(
                        modifier = Modifier.padding(padding),
                        contentPadding = PaddingValues(16.dp),
                        verticalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        item {
                            Text("${s.data.size} item${if (s.data.size == 1) "" else "s"}", style = MaterialTheme.typography.titleMedium)
                            Spacer(Modifier.height(4.dp))
                        }
                        items(s.data, key = { it.product.id }) { line ->
                            ProductRow(
                                product = line.product,
                                quantity = line.quantity,
                                onRemove = { viewModel.removeItem(line.product.id) }
                            )
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun PriceLine(label: String, value: String) {
    Row(modifier = Modifier.fillMaxWidth().padding(vertical = 2.dp), horizontalArrangement = Arrangement.SpaceBetween) {
        Text(label, style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
        Text(value, style = MaterialTheme.typography.bodyMedium)
    }
}

@Composable
private fun EmptyCart(padding: PaddingValues, onBack: () -> Unit) {
    Box(Modifier.padding(padding).fillMaxSize(), contentAlignment = Alignment.Center) {
        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            Text("🛒", style = MaterialTheme.typography.displaySmall)
            Spacer(Modifier.height(12.dp))
            Text("Your cart is empty", style = MaterialTheme.typography.titleMedium)
            Text(
                "Head back and pick something fresh from the oven!",
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
            Spacer(Modifier.height(20.dp))
            AnimatedButton(text = "Browse the Bakery", onClick = onBack, modifier = Modifier.fillMaxWidth(0.7f))
        }
    }
}
