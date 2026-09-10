package com.crumbandember.app.ui.screens.orders

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
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
fun OrderHistoryScreen(
    orderRepository: OrderRepository,
    onBack: () -> Unit,
    onOrderClick: (String) -> Unit
) {
    val viewModel: OrdersViewModel = viewModel(factory = ViewModelFactory { OrdersViewModel(orderRepository) })
    val state by viewModel.orders.collectAsState()

    LaunchedEffect(Unit) { viewModel.loadOrders() }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Order history") },
                navigationIcon = {
                    IconButton(onClick = onBack) { Icon(Icons.Filled.ArrowBack, contentDescription = "Back") }
                }
            )
        }
    ) { padding ->
        when (val s = state) {
            is Resource.Loading, Resource.Idle -> LoadingView()
            is Resource.Error -> UnavailableScreen(s.kind, "your order history", onRetry = viewModel::loadOrders)
            is Resource.Success -> {
                if (s.data.isEmpty()) {
                    ErrorView("No orders yet")
                } else {
                    LazyColumn(
                        modifier = Modifier.padding(padding),
                        contentPadding = PaddingValues(16.dp),
                        verticalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        items(s.data, key = { it.id }) { order ->
                            Card(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .clickable { onOrderClick(order.id) }
                            ) {
                                Column(modifier = Modifier.padding(12.dp)) {
                                    Text("Order #${order.id}", style = MaterialTheme.typography.titleMedium)
                                    Text("Status: ${order.status}")
                                    order.amount?.let { Text("€${"%.2f".format(it)} ${order.currency}") }
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}
