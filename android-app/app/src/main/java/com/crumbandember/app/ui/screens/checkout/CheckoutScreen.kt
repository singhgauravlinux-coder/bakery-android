package com.crumbandember.app.ui.screens.checkout

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.selection.selectable
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.crumbandember.app.data.repository.CartRepository
import com.crumbandember.app.data.repository.OrderRepository
import com.crumbandember.app.data.repository.ProductRepository
import com.crumbandember.app.ui.components.ErrorView
import com.crumbandember.app.ui.components.friendlyInlineMessage
import com.crumbandember.app.util.Resource
import com.crumbandember.app.util.ViewModelFactory

private val PAYMENT_METHODS = listOf("card" to "Card", "upi" to "UPI", "cod" to "Cash on pickup")

@Composable
fun CheckoutScreen(
    userId: String,
    cartRepository: CartRepository,
    productRepository: ProductRepository,
    orderRepository: OrderRepository,
    onBack: () -> Unit,
    onOrderPlaced: (String) -> Unit
) {
    val viewModel: CheckoutViewModel = viewModel(
        factory = ViewModelFactory { CheckoutViewModel(userId, cartRepository, productRepository, orderRepository) }
    )
    val orderState by viewModel.orderState.collectAsState()
    var selectedMethod by remember { mutableStateOf(PAYMENT_METHODS.first().first) }
    var pickupTime by remember { mutableStateOf("") }

    LaunchedEffect(orderState) {
        if (orderState is Resource.Success) {
            onOrderPlaced((orderState as Resource.Success).data.id)
        }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Checkout") },
                navigationIcon = {
                    IconButton(onClick = onBack) { Icon(Icons.Filled.ArrowBack, contentDescription = "Back") }
                }
            )
        }
    ) { padding ->
        Column(modifier = Modifier.padding(padding).padding(24.dp).fillMaxSize()) {
            Text("Payment method", style = MaterialTheme.typography.titleMedium)
            Spacer(Modifier.height(8.dp))
            PAYMENT_METHODS.forEach { (value, label) ->
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .selectable(
                            selected = selectedMethod == value,
                            onClick = { selectedMethod = value },
                            role = Role.RadioButton
                        )
                        .padding(vertical = 8.dp)
                ) {
                    RadioButton(selected = selectedMethod == value, onClick = { selectedMethod = value })
                    Spacer(Modifier.width(8.dp))
                    Text(label, modifier = Modifier.align(androidx.compose.ui.Alignment.CenterVertically))
                }
            }

            Spacer(Modifier.height(16.dp))
            OutlinedTextField(
                value = pickupTime,
                onValueChange = { pickupTime = it },
                label = { Text("Pickup time (optional, e.g. 2026-09-10T17:00)") },
                singleLine = true,
                modifier = Modifier.fillMaxWidth()
            )

            if (orderState is Resource.Error) {
                val err = orderState as Resource.Error
                Spacer(Modifier.height(12.dp))
                // Kept inline (not a full UnavailableScreen) on purpose: swapping
                // the whole screen here would wipe the payment method/pickup time
                // the person already picked. They just retry the same button.
                ErrorView(friendlyInlineMessage(err.kind, err.message))
            }

            Spacer(Modifier.weight(1f))
            Button(
                onClick = { viewModel.placeOrder(selectedMethod, pickupTime.ifBlank { null }) },
                enabled = orderState !is Resource.Loading,
                modifier = Modifier.fillMaxWidth()
            ) {
                if (orderState is Resource.Loading) {
                    CircularProgressIndicator(modifier = Modifier.size(18.dp), strokeWidth = 2.dp)
                } else {
                    Text("Place order")
                }
            }
        }
    }
}
