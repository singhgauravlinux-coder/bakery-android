package com.crumbandember.app.ui.screens.orders

import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.crumbandember.app.data.repository.OrderRepository
import com.crumbandember.app.ui.components.ErrorView
import com.crumbandember.app.ui.components.LoadingView
import com.crumbandember.app.ui.components.UnavailableScreen
import com.crumbandember.app.util.Resource
import com.crumbandember.app.util.ViewModelFactory

@Composable
fun OrderDetailScreen(
    orderId: String,
    orderRepository: OrderRepository,
    onDoneShopping: () -> Unit
) {
    val viewModel: OrdersViewModel = viewModel(factory = ViewModelFactory { OrdersViewModel(orderRepository) })
    val state by viewModel.order.collectAsState()

    LaunchedEffect(orderId) { viewModel.loadOrder(orderId) }

    Scaffold(topBar = { TopAppBar(title = { Text("Order confirmed") }) }) { padding ->
        when (val s = state) {
            is Resource.Loading, Resource.Idle -> LoadingView()
            is Resource.Error -> UnavailableScreen(s.kind, "your order", onRetry = { viewModel.loadOrder(orderId) })
            is Resource.Success -> {
                val order = s.data
                Column(
                    modifier = Modifier
                        .padding(padding)
                        .padding(24.dp)
                        .fillMaxSize()
                ) {
                    Text("🎉 Thanks — your order is in!", style = MaterialTheme.typography.titleLarge)
                    Spacer(Modifier.height(16.dp))
                    Text("Order #${order.id}")
                    Text("Status: ${order.status}")
                    Text("Payment: ${order.paymentMethod} (${order.paymentStatus})")
                    order.amount?.let { Text("Amount: €${"%.2f".format(it)} ${order.currency}") }
                    order.pickupTime?.let { Text("Pickup: $it") }
                    Spacer(Modifier.height(16.dp))
                    Text("Items", style = MaterialTheme.typography.titleMedium)
                    order.items.forEach { Text("• ${it.productId} × ${it.quantity}") }

                    Spacer(Modifier.weight(1f))
                    Button(onClick = onDoneShopping, modifier = Modifier.fillMaxWidth()) {
                        Text("Back to the bakery")
                    }
                }
            }
        }
    }
}
