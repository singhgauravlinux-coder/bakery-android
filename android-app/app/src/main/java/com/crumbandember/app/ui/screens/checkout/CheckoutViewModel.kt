package com.crumbandember.app.ui.screens.checkout

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.crumbandember.app.data.model.CartItem
import com.crumbandember.app.data.model.CreateOrderRequest
import com.crumbandember.app.data.model.CreatePaymentRequest
import com.crumbandember.app.data.model.Order
import com.crumbandember.app.data.repository.CartRepository
import com.crumbandember.app.data.repository.OrderRepository
import com.crumbandember.app.data.repository.PaymentRepository
import com.crumbandember.app.data.repository.ProductRepository
import com.crumbandember.app.util.Resource
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch

/** What step of checkout we're currently showing/attempting, for UI copy ("Placing order…" vs "Processing payment…"). */
enum class CheckoutStage { IDLE, CREATING_ORDER, TAKING_PAYMENT, CONFIRMING, DONE }

class CheckoutViewModel(
    private val userId: String,
    private val cartRepository: CartRepository,
    private val productRepository: ProductRepository,
    private val orderRepository: OrderRepository,
    private val paymentRepository: PaymentRepository
) : ViewModel() {

    private val _orderState = MutableStateFlow<Resource<Order>>(Resource.Idle)
    val orderState: StateFlow<Resource<Order>> = _orderState

    private val _stage = MutableStateFlow(CheckoutStage.IDLE)
    val stage: StateFlow<CheckoutStage> = _stage

    // Kept across retries: if payment fails for card/upi, we retry the
    // payment against the SAME order rather than creating a duplicate
    // pending_payment order every time the person taps "Place Order" again.
    private var pendingOrder: Order? = null

    /**
     * Full flow: create the order (order-service) -> for card/upi, take a
     * payment (payment-service) -> confirm the order against that payment
     * (order-service verifies it server-side). COD orders are `received`
     * immediately and skip payment entirely.
     */
    fun placeOrder(
        paymentMethod: String,
        pickupTime: String?,
        cardNumber: String? = null,
        expiry: String? = null,
        cvv: String? = null,
        vpa: String? = null
    ) {
        viewModelScope.launch {
            _orderState.value = Resource.Loading

            val order = pendingOrder ?: run {
                _stage.value = CheckoutStage.CREATING_ORDER
                val cartResult = cartRepository.getCart(userId)
                if (cartResult is Resource.Error) {
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
                    currency = "INR"
                )
                val createResult = orderRepository.createOrder(request)
                if (createResult !is Resource.Success) {
                    _orderState.value = createResult
                    return@launch
                }
                pendingOrder = createResult.data
                createResult.data
            }

            if (paymentMethod == "cod") {
                // order-service already returned this order as `received`
                // with payment_status cod_pending — nothing else to do.
                cartRepository.clear(userId)
                _stage.value = CheckoutStage.DONE
                _orderState.value = Resource.Success(order)
                return@launch
            }

            _stage.value = CheckoutStage.TAKING_PAYMENT
            val amount = order.amount ?: 0.0
            val paymentResult = paymentRepository.pay(
                CreatePaymentRequest(
                    orderId = order.id,
                    amount = amount,
                    method = paymentMethod,
                    currency = order.currency,
                    cardNumber = cardNumber,
                    expiry = expiry,
                    cvv = cvv,
                    vpa = vpa
                )
            )
            if (paymentResult !is Resource.Success) {
                // Order stays pending_payment on the server — same order is
                // reused on the next attempt (see `pendingOrder` above), the
                // person is not charged twice or left with duplicate orders.
                _orderState.value = if (paymentResult is Resource.Error) {
                    Resource.Error(paymentResult.message, paymentResult.kind, paymentResult.debugDetail)
                } else {
                    Resource.Error("Payment failed")
                }
                return@launch
            }
            if (paymentResult.data.status != "succeeded") {
                _orderState.value = Resource.Error("Payment was not successful — please try again")
                return@launch
            }

            _stage.value = CheckoutStage.CONFIRMING
            val confirmResult = orderRepository.confirmOrder(order.id, paymentResult.data.id)
            if (confirmResult is Resource.Success) {
                cartRepository.clear(userId)
                pendingOrder = null
                _stage.value = CheckoutStage.DONE
            }
            _orderState.value = confirmResult
        }
    }
}
