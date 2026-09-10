package com.crumbandember.app.ui.screens.checkout

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.crumbandember.app.data.model.CartItem
import com.crumbandember.app.data.model.CreateOrderRequest
import com.crumbandember.app.data.model.Order
import com.crumbandember.app.data.repository.CartRepository
import com.crumbandember.app.data.repository.OrderRepository
import com.crumbandember.app.data.repository.ProductRepository
import com.crumbandember.app.util.Resource
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch

class CheckoutViewModel(
    private val userId: String,
    private val cartRepository: CartRepository,
    private val productRepository: ProductRepository,
    private val orderRepository: OrderRepository
) : ViewModel() {

    private val _orderState = MutableStateFlow<Resource<Order>>(Resource.Idle)
    val orderState: StateFlow<Resource<Order>> = _orderState

    /**
     * order-service requires paymentMethod in {card, upi, cod, razorpay} and a
     * positive `amount` for anything except cod. Payment gateway integration
     * (Razorpay order + signature verify) lives behind /api/payments/razorpay/*
     * and is a good next step once this basic flow is wired up.
     */
    fun placeOrder(paymentMethod: String, pickupTime: String?) {
        viewModelScope.launch {
            _orderState.value = Resource.Loading

            val cartResult = cartRepository.getCart(userId)
            if (cartResult is Resource.Error) {
                // Surface the real reason (gateway down / cart-service down /
                // offline) instead of misreporting it as an empty cart.
                _orderState.value = Resource.Error(cartResult.message, cartResult.kind)
                return@launch
            }
            val cart = (cartResult as Resource.Success).data
            if (cart.items.isEmpty()) {
                _orderState.value = Resource.Error("Your cart is empty")
                return@launch
            }

            var amount = 0.0
            for (item in cart.items) {
                val productResult = productRepository.getProduct(item.productId)
                val price = (productResult as? Resource.Success)?.data?.price ?: 0.0
                amount += price * item.quantity
            }

            val request = CreateOrderRequest(
                userId = userId,
                items = cart.items.map { CartItem(it.productId, it.quantity) },
                pickupTime = pickupTime,
                paymentMethod = paymentMethod,
                amount = if (paymentMethod == "cod") null else amount,
                currency = "EUR"
            )

            val result = orderRepository.createOrder(request)
            _orderState.value = result
            if (result is Resource.Success) {
                cartRepository.clear(userId)
            }
        }
    }
}
