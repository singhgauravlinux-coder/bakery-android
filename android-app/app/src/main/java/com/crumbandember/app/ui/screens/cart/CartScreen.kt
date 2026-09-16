package com.crumbandember.app.ui.screens.cart

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ConfirmationNumber
import androidx.compose.material.icons.filled.Discount
import androidx.compose.material.icons.filled.ShoppingCartCheckout
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
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

    var promoApplied by remember { mutableStateOf(true) }
    var promoCodeInput by remember { mutableStateOf("CRUMB20") }
    val discount = if (promoApplied) 50.0 else 0.0
    val deliveryCharge = 30.0

    Scaffold(
        topBar = { BakeryDetailTopBar(title = "Your Cart", onBack = onBack) },
        bottomBar = {
            if (state is Resource.Success && (state as Resource.Success).data.isNotEmpty()) {
                val effectiveTotal = (total + deliveryCharge - discount).coerceAtLeast(0.0)
                Surface(tonalElevation = 6.dp) {
                    Column(modifier = Modifier.padding(20.dp)) {
                        PriceLine("Subtotal", formatPrice(total))
                        PriceLine("Delivery Charge", formatPrice(deliveryCharge))
                        if (discount > 0) {
                            PriceLine("Discount (CRUMB20)", "- ${formatPrice(discount)}", isHighlight = true)
                        }
                        Divider(modifier = Modifier.padding(vertical = 8.dp))
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Text("Total", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
                            Text(
                                formatPrice(effectiveTotal),
                                style = MaterialTheme.typography.titleLarge,
                                fontWeight = FontWeight.Bold,
                                color = MaterialTheme.colorScheme.primary
                            )
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
                        verticalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        item {
                            Text(
                                "${s.data.size} item${if (s.data.size == 1) "" else "s"}",
                                style = MaterialTheme.typography.titleMedium,
                                fontWeight = FontWeight.Bold
                            )
                        }

                        items(s.data, key = { it.product.id }) { line ->
                            ProductRow(
                                product = line.product,
                                quantity = line.quantity,
                                onRemove = { viewModel.removeItem(line.product.id) }
                            )
                        }

                        // Promo code surface
                        item {
                            Spacer(Modifier.height(4.dp))
                            Surface(
                                shape = RoundedCornerShape(16.dp),
                                color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.6f),
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .clickable { promoApplied = !promoApplied }
                            ) {
                                Row(
                                    modifier = Modifier.padding(14.dp),
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.SpaceBetween
                                ) {
                                    Row(verticalAlignment = Alignment.CenterVertically) {
                                        Icon(
                                            Icons.Filled.Discount,
                                            contentDescription = null,
                                            tint = MaterialTheme.colorScheme.primary,
                                            modifier = Modifier.size(20.dp)
                                        )
                                        Spacer(Modifier.width(10.dp))
                                        Column {
                                            Text(
                                                if (promoApplied) "CRUMB20 Applied" else "Apply Promo Code",
                                                style = MaterialTheme.typography.bodyMedium,
                                                fontWeight = FontWeight.Bold
                                            )
                                            Text(
                                                if (promoApplied) "₹50 instant discount applied" else "Tap to apply voucher",
                                                style = MaterialTheme.typography.bodySmall,
                                                color = MaterialTheme.colorScheme.onSurfaceVariant
                                            )
                                        }
                                    }
                                    Text(
                                        if (promoApplied) "Remove" else "Apply",
                                        style = MaterialTheme.typography.labelMedium,
                                        fontWeight = FontWeight.Bold,
                                        color = if (promoApplied) MaterialTheme.colorScheme.error else MaterialTheme.colorScheme.primary
                                    )
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun PriceLine(label: String, value: String, isHighlight: Boolean = false) {
    Row(
        modifier = Modifier.fillMaxWidth().padding(vertical = 2.dp),
        horizontalArrangement = Arrangement.SpaceBetween
    ) {
        Text(
            label,
            style = MaterialTheme.typography.bodyMedium,
            color = if (isHighlight) Color(0xFF2E7D32) else MaterialTheme.colorScheme.onSurfaceVariant
        )
        Text(
            value,
            style = MaterialTheme.typography.bodyMedium,
            fontWeight = if (isHighlight) FontWeight.Bold else FontWeight.Normal,
            color = if (isHighlight) Color(0xFF2E7D32) else MaterialTheme.colorScheme.onSurface
        )
    }
}

@Composable
private fun EmptyCart(padding: PaddingValues, onBack: () -> Unit) {
    Box(Modifier.padding(padding).fillMaxSize(), contentAlignment = Alignment.Center) {
        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            Text("🛒", style = MaterialTheme.typography.displaySmall)
            Spacer(Modifier.height(12.dp))
            Text("Your cart is empty", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
            Spacer(Modifier.height(4.dp))
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
