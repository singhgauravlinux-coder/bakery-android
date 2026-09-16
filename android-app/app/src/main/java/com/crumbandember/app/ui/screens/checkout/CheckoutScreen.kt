package com.crumbandember.app.ui.screens.checkout

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.selection.selectable
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.CreditCard
import androidx.compose.material.icons.filled.Payments
import androidx.compose.material.icons.filled.QrCode
import androidx.compose.material.icons.filled.Schedule
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.crumbandember.app.data.repository.CartRepository
import com.crumbandember.app.data.repository.OrderRepository
import com.crumbandember.app.data.repository.PaymentRepository
import com.crumbandember.app.data.repository.ProductRepository
import com.crumbandember.app.ui.components.AnimatedButton
import com.crumbandember.app.ui.components.BakeryDetailTopBar
import com.crumbandember.app.ui.components.ErrorView
import com.crumbandember.app.ui.components.friendlyInlineMessage
import com.crumbandember.app.util.Resource
import com.crumbandember.app.util.ViewModelFactory

private data class PaymentOption(val value: String, val label: String, val icon: androidx.compose.ui.graphics.vector.ImageVector)

private val PAYMENT_METHODS = listOf(
    PaymentOption("card", "Card", Icons.Filled.CreditCard),
    PaymentOption("upi", "UPI", Icons.Filled.QrCode),
    PaymentOption("cod", "Cash on pickup", Icons.Filled.Payments)
)

@Composable
fun CheckoutScreen(
    userId: String,
    cartRepository: CartRepository,
    productRepository: ProductRepository,
    orderRepository: OrderRepository,
    paymentRepository: PaymentRepository,
    onBack: () -> Unit,
    onOrderPlaced: (String) -> Unit
) {
    val viewModel: CheckoutViewModel = viewModel(
        factory = ViewModelFactory { CheckoutViewModel(userId, cartRepository, productRepository, orderRepository, paymentRepository) }
    )
    val orderState by viewModel.orderState.collectAsState()
    val stage by viewModel.stage.collectAsState()

    var selectedMethod by remember { mutableStateOf(PAYMENT_METHODS.first().value) }
    var pickupTime by remember { mutableStateOf("") }
    var cardNumber by remember { mutableStateOf("") }
    var expiry by remember { mutableStateOf("") }
    var cvv by remember { mutableStateOf("") }
    var vpa by remember { mutableStateOf("") }

    LaunchedEffect(orderState, stage) {
        if (orderState is Resource.Success && stage == CheckoutStage.DONE) {
            onOrderPlaced((orderState as Resource.Success).data.id)
        }
    }

    val loading = orderState is Resource.Loading

    Scaffold(
        topBar = { BakeryDetailTopBar(title = "Checkout", onBack = onBack) }
    ) { padding ->
        Column(
            modifier = Modifier
                .padding(padding)
                .padding(20.dp)
                .fillMaxSize()
                .verticalScroll(rememberScrollState())
        ) {
            Text("Payment Method", style = MaterialTheme.typography.titleMedium)
            Spacer(Modifier.height(10.dp))
            PAYMENT_METHODS.forEach { option ->
                val selected = selectedMethod == option.value
                Surface(
                    shape = RoundedCornerShape(16.dp),
                    color = if (selected) MaterialTheme.colorScheme.primaryContainer else MaterialTheme.colorScheme.surfaceVariant,
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(vertical = 5.dp)
                        .selectable(selected = selected, onClick = { selectedMethod = option.value }, role = Role.RadioButton)
                ) {
                    Row(
                        modifier = Modifier.padding(horizontal = 16.dp, vertical = 14.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(option.icon, contentDescription = null, tint = MaterialTheme.colorScheme.primary)
                        Spacer(Modifier.width(12.dp))
                        Text(option.label, style = MaterialTheme.typography.bodyLarge, modifier = Modifier.weight(1f))
                        if (selected) Icon(Icons.Filled.Check, contentDescription = null, tint = MaterialTheme.colorScheme.primary)
                    }
                }
            }

            // payment-service validates these server-side (Luhn check on the
            // card number, MM/YY expiry, a name@bank-shaped VPA) — this is
            // real instrument capture, not a decorative form.
            when (selectedMethod) {
                "card" -> {
                    Spacer(Modifier.height(16.dp))
                    Text("Card Details", style = MaterialTheme.typography.titleMedium)
                    Spacer(Modifier.height(8.dp))
                    OutlinedTextField(
                        value = cardNumber,
                        onValueChange = { cardNumber = it },
                        label = { Text("Card number") },
                        singleLine = true,
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                        shape = RoundedCornerShape(16.dp),
                        modifier = Modifier.fillMaxWidth()
                    )
                    Spacer(Modifier.height(10.dp))
                    Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                        OutlinedTextField(
                            value = expiry,
                            onValueChange = { expiry = it },
                            label = { Text("MM/YY") },
                            singleLine = true,
                            shape = RoundedCornerShape(16.dp),
                            modifier = Modifier.weight(1f)
                        )
                        OutlinedTextField(
                            value = cvv,
                            onValueChange = { cvv = it },
                            label = { Text("CVV") },
                            singleLine = true,
                            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.NumberPassword),
                            shape = RoundedCornerShape(16.dp),
                            modifier = Modifier.weight(1f)
                        )
                    }
                }
                "upi" -> {
                    Spacer(Modifier.height(16.dp))
                    Text("UPI Details", style = MaterialTheme.typography.titleMedium)
                    Spacer(Modifier.height(8.dp))
                    OutlinedTextField(
                        value = vpa,
                        onValueChange = { vpa = it },
                        label = { Text("UPI ID (e.g. name@bank)") },
                        singleLine = true,
                        shape = RoundedCornerShape(16.dp),
                        modifier = Modifier.fillMaxWidth()
                    )
                }
            }

            Spacer(Modifier.height(20.dp))
            Text("Pickup Time", style = MaterialTheme.typography.titleMedium)
            Spacer(Modifier.height(8.dp))
            OutlinedTextField(
                value = pickupTime,
                onValueChange = { pickupTime = it },
                label = { Text("e.g. 2026-09-18T17:00 (optional)") },
                leadingIcon = { Icon(Icons.Filled.Schedule, contentDescription = null) },
                singleLine = true,
                shape = RoundedCornerShape(16.dp),
                modifier = Modifier.fillMaxWidth()
            )

            if (orderState is Resource.Error) {
                val err = orderState as Resource.Error
                Spacer(Modifier.height(12.dp))
                // Kept inline (not a full UnavailableScreen) on purpose: swapping
                // the whole screen here would wipe the payment method/pickup time
                // the person already picked. They just retry the same button —
                // the order that was already created is reused, not duplicated.
                ErrorView(friendlyInlineMessage(err.kind, err.message))
            }

            Spacer(Modifier.height(28.dp))
            val buttonLabel = when {
                !loading -> "Place Order"
                stage == CheckoutStage.CREATING_ORDER -> "Placing order…"
                stage == CheckoutStage.TAKING_PAYMENT -> "Processing payment…"
                stage == CheckoutStage.CONFIRMING -> "Confirming…"
                else -> "Please wait…"
            }
            AnimatedButton(
                text = buttonLabel,
                onClick = {
                    viewModel.placeOrder(
                        paymentMethod = selectedMethod,
                        pickupTime = pickupTime.ifBlank { null },
                        cardNumber = cardNumber.ifBlank { null },
                        expiry = expiry.ifBlank { null },
                        cvv = cvv.ifBlank { null },
                        vpa = vpa.ifBlank { null }
                    )
                },
                loading = loading,
                modifier = Modifier.fillMaxWidth()
            )
            Spacer(Modifier.height(12.dp))
        }
    }
}
