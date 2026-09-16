package com.crumbandember.app.ui.screens.orders

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.crumbandember.app.data.repository.OrderRepository
import com.crumbandember.app.ui.components.*
import com.crumbandember.app.util.Resource
import com.crumbandember.app.util.ViewModelFactory

private val ORDER_STAGES = listOf("Ordered", "Confirmed", "Baking", "Ready", "Delivered")

private fun stagesFor(status: String): List<TimelineStep> {
    val normalized = status.lowercase()
    val currentIndex = when {
        normalized.contains("deliver") || normalized.contains("complete") -> ORDER_STAGES.lastIndex
        normalized.contains("ready") -> 3
        normalized.contains("bak") || normalized.contains("prepar") -> 2
        normalized.contains("confirm") || normalized.contains("accept") -> 1
        else -> 0
    }
    return ORDER_STAGES.mapIndexed { index, label ->
        TimelineStep(label = label, done = index < currentIndex, current = index == currentIndex)
    }
}

@Composable
fun OrderDetailScreen(
    orderId: String,
    orderRepository: OrderRepository,
    onDoneShopping: () -> Unit
) {
    val viewModel: OrdersViewModel = viewModel(factory = ViewModelFactory { OrdersViewModel(orderRepository) })
    val state by viewModel.order.collectAsState()

    LaunchedEffect(orderId) { viewModel.loadOrder(orderId) }

    Scaffold(topBar = { BakeryDetailTopBar(title = "Order Details", onBack = onDoneShopping) }) { padding ->
        when (val s = state) {
            is Resource.Loading, Resource.Idle -> LoadingSkeleton(modifier = Modifier.padding(padding).padding(top = 16.dp))
            is Resource.Error -> UnavailableScreen(s.kind, "your order", onRetry = { viewModel.loadOrder(orderId) })
            is Resource.Success -> {
                val order = s.data
                val cancelled = order.status.equals("cancelled", ignoreCase = true)
                Column(
                    modifier = Modifier
                        .padding(padding)
                        .padding(24.dp)
                        .fillMaxSize()
                ) {
                    Text("#${order.id.take(10)}", style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
                    Spacer(Modifier.height(4.dp))
                    Text(
                        if (cancelled) "This order was cancelled" else "🎉 Thanks — your order is on its way!",
                        style = MaterialTheme.typography.titleLarge
                    )

                    if (!cancelled) {
                        Spacer(Modifier.height(28.dp))
                        OrderTimeline(steps = stagesFor(order.status), modifier = Modifier.fillMaxWidth())
                    }

                    Spacer(Modifier.height(28.dp))
                    Surface(shape = RoundedCornerShape(18.dp), color = MaterialTheme.colorScheme.surfaceVariant, modifier = Modifier.fillMaxWidth()) {
                        Column(modifier = Modifier.padding(16.dp)) {
                            InfoLine("Status", order.status.replaceFirstChar { it.uppercase() })
                            InfoLine("Payment", "${order.paymentMethod} (${order.paymentStatus})")
                            order.amount?.let { InfoLine("Amount", formatPrice(it)) }
                            order.pickupTime?.let { InfoLine("Pickup", it) }
                        }
                    }

                    Spacer(Modifier.height(20.dp))
                    Text("Items", style = MaterialTheme.typography.titleMedium)
                    Spacer(Modifier.height(8.dp))
                    order.items.forEach {
                        Row(modifier = Modifier.fillMaxWidth().padding(vertical = 4.dp), horizontalArrangement = Arrangement.SpaceBetween) {
                            Text("• ${it.productId}", style = MaterialTheme.typography.bodyMedium)
                            Text("× ${it.quantity}", style = MaterialTheme.typography.bodyMedium, fontWeight = FontWeight.Bold)
                        }
                    }

                    Spacer(Modifier.weight(1f))
                    AnimatedButton(text = "Back to the Bakery", onClick = onDoneShopping, modifier = Modifier.fillMaxWidth())
                }
            }
        }
    }
}

@Composable
private fun InfoLine(label: String, value: String) {
    Row(modifier = Modifier.fillMaxWidth().padding(vertical = 3.dp), horizontalArrangement = Arrangement.SpaceBetween) {
        Text(label, style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
        Text(value, style = MaterialTheme.typography.bodyMedium, fontWeight = FontWeight.Medium)
    }
}
