package com.crumbandember.app.ui.screens.orders

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Replay
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.crumbandember.app.data.model.Order
import com.crumbandember.app.data.repository.OrderRepository
import com.crumbandember.app.ui.components.*
import com.crumbandember.app.util.Resource
import com.crumbandember.app.util.ViewModelFactory

@Composable
fun OrderHistoryScreen(
    orderRepository: OrderRepository,
    onBack: () -> Unit,
    onOrderClick: (String) -> Unit,
    cartCount: Int = 0,
    onHomeClick: () -> Unit = onBack,
    onCartClick: () -> Unit = {},
    onProfileClick: () -> Unit = {}
) {
    val viewModel: OrdersViewModel = viewModel(factory = ViewModelFactory { OrdersViewModel(orderRepository) })
    val state by viewModel.orders.collectAsState()

    LaunchedEffect(Unit) { viewModel.loadOrders() }

    Scaffold(
        topBar = { BakeryDetailTopBar(title = "My Orders", onBack = onBack) },
        bottomBar = {
            BakeryBottomBar(
                selected = BakeryTab.ORDERS,
                cartCount = cartCount,
                onSelect = { tab ->
                    when (tab) {
                        BakeryTab.HOME -> onHomeClick()
                        BakeryTab.ORDERS -> {}
                        BakeryTab.CART -> onCartClick()
                        BakeryTab.PROFILE -> onProfileClick()
                    }
                }
            )
        }
    ) { padding ->
        when (val s = state) {
            is Resource.Loading, Resource.Idle -> LoadingSkeleton(modifier = Modifier.padding(padding).padding(top = 16.dp))
            is Resource.Error -> UnavailableScreen(s.kind, "your order history", onRetry = viewModel::loadOrders)
            is Resource.Success -> {
                if (s.data.isEmpty()) {
                    Box(Modifier.padding(padding).fillMaxSize(), contentAlignment = Alignment.Center) {
                        Column(horizontalAlignment = Alignment.CenterHorizontally) {
                            Text("📦", style = MaterialTheme.typography.displaySmall)
                            Spacer(Modifier.height(8.dp))
                            Text("No orders yet", style = MaterialTheme.typography.titleMedium)
                        }
                    }
                } else {
                    LazyColumn(
                        modifier = Modifier.padding(padding),
                        contentPadding = PaddingValues(16.dp),
                        verticalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        items(s.data, key = { it.id }) { order ->
                            OrderCard(order = order, onClick = { onOrderClick(order.id) })
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun OrderCard(order: Order, onClick: () -> Unit) {
    Surface(
        shape = RoundedCornerShape(18.dp),
        color = MaterialTheme.colorScheme.surface,
        tonalElevation = 2.dp,
        modifier = Modifier.fillMaxWidth().clickable(onClick = onClick)
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text("Order #${order.id.take(8)}", style = MaterialTheme.typography.titleMedium)
                StatusBadge(status = order.status)
            }
            Spacer(Modifier.height(6.dp))
            order.createdAt?.let {
                Text(it, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
            }
            order.amount?.let {
                Spacer(Modifier.height(4.dp))
                Text(formatPrice(it), style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.Bold)
            }
            if (order.status.equals("delivered", ignoreCase = true) || order.status.equals("completed", ignoreCase = true)) {
                Spacer(Modifier.height(10.dp))
                OutlinedButton(onClick = onClick, shape = RoundedCornerShape(50)) {
                    Icon(Icons.Filled.Replay, contentDescription = null, modifier = Modifier.size(16.dp))
                    Spacer(Modifier.width(6.dp))
                    Text("Reorder")
                }
            }
        }
    }
}

@Composable
private fun StatusBadge(status: String) {
    val color = when (status.lowercase()) {
        "delivered", "completed" -> MaterialTheme.colorScheme.tertiary
        "cancelled" -> MaterialTheme.colorScheme.error
        else -> MaterialTheme.colorScheme.primary
    }
    Surface(shape = RoundedCornerShape(50), color = color.copy(alpha = 0.15f)) {
        Text(
            status.replaceFirstChar { it.uppercase() },
            style = MaterialTheme.typography.labelSmall,
            color = color,
            fontWeight = FontWeight.Bold,
            modifier = Modifier.padding(horizontal = 10.dp, vertical = 4.dp)
        )
    }
}
