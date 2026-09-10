package com.crumbandember.app.ui.screens.cart

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.crumbandember.app.data.repository.CartRepository
import com.crumbandember.app.data.repository.ProductRepository
import com.crumbandember.app.ui.components.ErrorView
import com.crumbandember.app.ui.components.LoadingView
import com.crumbandember.app.ui.components.UnavailableScreen
import com.crumbandember.app.util.Resource
import com.crumbandember.app.util.ViewModelFactory
import java.util.Locale

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
        topBar = {
            TopAppBar(
                title = { Text("Your cart") },
                navigationIcon = {
                    IconButton(onClick = onBack) { Icon(Icons.Filled.ArrowBack, contentDescription = "Back") }
                }
            )
        },
        bottomBar = {
            if (state is Resource.Success && (state as Resource.Success).data.isNotEmpty()) {
                Surface(tonalElevation = 4.dp) {
                    Column(modifier = Modifier.padding(16.dp)) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Text("Total", style = MaterialTheme.typography.titleMedium)
                            Text(
                                String.format(Locale.getDefault(), "€%.2f", total),
                                style = MaterialTheme.typography.titleMedium
                            )
                        }
                        Spacer(Modifier.height(12.dp))
                        Button(onClick = onCheckout, modifier = Modifier.fillMaxWidth()) {
                            Text("Checkout")
                        }
                    }
                }
            }
        }
    ) { padding ->
        when (val s = state) {
            is Resource.Loading, Resource.Idle -> LoadingView()
            is Resource.Error -> UnavailableScreen(s.kind, "your cart", onRetry = viewModel::load)
            is Resource.Success -> {
                if (s.data.isEmpty()) {
                    Box(Modifier.padding(padding).fillMaxSize(), contentAlignment = Alignment.Center) {
                        Text("Your cart is empty — head back and pick something fresh!")
                    }
                } else {
                    LazyColumn(
                        modifier = Modifier.padding(padding),
                        contentPadding = PaddingValues(16.dp),
                        verticalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        items(s.data, key = { it.product.id }) { line ->
                            Card(modifier = Modifier.fillMaxWidth()) {
                                Row(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .padding(12.dp),
                                    horizontalArrangement = Arrangement.SpaceBetween,
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Column {
                                        Text(line.product.name, style = MaterialTheme.typography.titleMedium)
                                        Text("Qty ${line.quantity} · €${"%.2f".format(line.product.price)} each")
                                    }
                                    IconButton(onClick = { viewModel.removeItem(line.product.id) }) {
                                        Icon(Icons.Filled.Delete, contentDescription = "Remove")
                                    }
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}
