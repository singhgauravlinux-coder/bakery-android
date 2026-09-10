package com.crumbandember.app.ui.screens.catalog

import androidx.compose.foundation.layout.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
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
import com.crumbandember.app.ui.components.friendlyInlineMessage
import com.crumbandember.app.util.Resource
import com.crumbandember.app.util.ViewModelFactory
import kotlinx.coroutines.launch
import java.util.Locale

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
    val snackbarHostState = remember { SnackbarHostState() }
    val scope = rememberCoroutineScope()

    LaunchedEffect(addState) {
        if (addState is Resource.Success) {
            scope.launch { snackbarHostState.showSnackbar("Added to cart") }
            viewModel.resetAddToCartState()
        } else if (addState is Resource.Error) {
            val err = addState as Resource.Error
            scope.launch { snackbarHostState.showSnackbar(friendlyInlineMessage(err.kind, err.message)) }
        }
    }

    Scaffold(
        snackbarHost = { SnackbarHost(snackbarHostState) },
        topBar = {
            TopAppBar(
                title = { Text("Product") },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.Filled.ArrowBack, contentDescription = "Back")
                    }
                }
            )
        }
    ) { padding ->
        when (val s = productState) {
            is Resource.Loading, Resource.Idle -> LoadingView()
            is Resource.Error -> UnavailableScreen(s.kind, "this product", onRetry = viewModel::retry)
            is Resource.Success -> {
                val product = s.data
                Column(
                    modifier = Modifier
                        .padding(padding)
                        .padding(24.dp)
                        .fillMaxSize()
                ) {
                    Text(product.name, style = MaterialTheme.typography.headlineMedium)
                    product.category?.let {
                        Spacer(Modifier.height(4.dp))
                        Text(it, color = MaterialTheme.colorScheme.secondary)
                    }
                    Spacer(Modifier.height(12.dp))
                    Text(
                        String.format(Locale.getDefault(), "€%.2f", product.price),
                        style = MaterialTheme.typography.titleLarge
                    )
                    Spacer(Modifier.height(16.dp))
                    product.description?.let { Text(it, style = MaterialTheme.typography.bodyLarge) }

                    Spacer(Modifier.height(24.dp))
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Text("Quantity", style = MaterialTheme.typography.titleMedium)
                        Spacer(Modifier.width(16.dp))
                        IconButton(onClick = { if (quantity > 1) quantity-- }) { Text("−", style = MaterialTheme.typography.titleLarge) }
                        Text(quantity.toString(), style = MaterialTheme.typography.titleMedium)
                        IconButton(onClick = { quantity++ }) { Text("+", style = MaterialTheme.typography.titleLarge) }
                    }

                    Spacer(Modifier.weight(1f))
                    Button(
                        onClick = { viewModel.addToCart(quantity) },
                        enabled = addState !is Resource.Loading,
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Text(if (addState is Resource.Loading) "Adding…" else "Add to cart")
                    }
                    Spacer(Modifier.height(8.dp))
                    OutlinedButton(onClick = onGoToCart, modifier = Modifier.fillMaxWidth()) {
                        Text("Go to cart")
                    }
                }
            }
        }
    }
}
