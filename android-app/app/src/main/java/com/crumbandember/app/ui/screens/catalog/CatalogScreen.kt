package com.crumbandember.app.ui.screens.catalog

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Search
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.crumbandember.app.data.repository.ProductRepository
import com.crumbandember.app.ui.components.BakeryTopBar
import com.crumbandember.app.ui.components.ErrorView
import com.crumbandember.app.ui.components.LoadingView
import com.crumbandember.app.ui.components.ProductCard
import com.crumbandember.app.ui.components.UnavailableScreen
import com.crumbandember.app.util.Resource
import com.crumbandember.app.util.ViewModelFactory

@Composable
fun CatalogScreen(
    productRepository: ProductRepository,
    cartCount: Int,
    onProductClick: (String) -> Unit,
    onCartClick: () -> Unit,
    onProfileClick: () -> Unit
) {
    val viewModel: CatalogViewModel = viewModel(factory = ViewModelFactory { CatalogViewModel(productRepository) })
    val state by viewModel.products.collectAsState()
    val query by viewModel.query.collectAsState()

    Scaffold(
        topBar = { BakeryTopBar(title = "Crumb & Ember", cartCount = cartCount, onCartClick = onCartClick) }
    ) { padding ->
        Column(modifier = Modifier.padding(padding)) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp, vertical = 8.dp)
            ) {
                OutlinedTextField(
                    value = query,
                    onValueChange = viewModel::onQueryChange,
                    label = { Text("Search the bakery") },
                    singleLine = true,
                    trailingIcon = {
                        IconButton(onClick = viewModel::search) {
                            Icon(Icons.Filled.Search, contentDescription = "Search")
                        }
                    },
                    modifier = Modifier.fillMaxWidth()
                )
            }

            TextButton(onClick = onProfileClick, modifier = Modifier.padding(horizontal = 8.dp)) {
                Text("My account & orders")
            }

            when (val s = state) {
                is Resource.Loading, Resource.Idle -> LoadingView()
                is Resource.Error -> UnavailableScreen(s.kind, "our menu", onRetry = { viewModel.loadProducts() })
                is Resource.Success -> {
                    if (s.data.isEmpty()) {
                        ErrorView("No products found")
                    } else {
                        LazyColumn(
                            contentPadding = PaddingValues(16.dp),
                            verticalArrangement = Arrangement.spacedBy(12.dp)
                        ) {
                            items(s.data, key = { it.id }) { product ->
                                ProductCard(product = product, onClick = { onProductClick(product.id) })
                            }
                        }
                    }
                }
            }
        }
    }
}
