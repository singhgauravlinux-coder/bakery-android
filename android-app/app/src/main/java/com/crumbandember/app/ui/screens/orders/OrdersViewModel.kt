package com.crumbandember.app.ui.screens.orders

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.crumbandember.app.data.model.Order
import com.crumbandember.app.data.repository.OrderRepository
import com.crumbandember.app.util.Resource
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch

class OrdersViewModel(private val repository: OrderRepository) : ViewModel() {

    private val _orders = MutableStateFlow<Resource<List<Order>>>(Resource.Loading)
    val orders: StateFlow<Resource<List<Order>>> = _orders

    private val _order = MutableStateFlow<Resource<Order>>(Resource.Loading)
    val order: StateFlow<Resource<Order>> = _order

    // Surfaced only as a transient inline error on the order row that
    // failed to cancel — cancelling never blocks the rest of the list.
    private val _cancelError = MutableStateFlow<String?>(null)
    val cancelError: StateFlow<String?> = _cancelError

    fun loadOrders(userId: String) {
        viewModelScope.launch {
            _orders.value = Resource.Loading
            _orders.value = repository.getOrders(userId)
        }
    }

    fun loadOrder(orderId: String) {
        viewModelScope.launch {
            _order.value = Resource.Loading
            _order.value = repository.getOrder(orderId)
        }
    }

    /** Only offered in the UI while an order is still pending_payment — see OrderHistoryScreen. */
    fun cancelOrder(orderId: String, userId: String) {
        viewModelScope.launch {
            when (val result = repository.cancelOrder(orderId)) {
                is Resource.Success -> {
                    _cancelError.value = null
                    loadOrders(userId)
                }
                is Resource.Error -> _cancelError.value = result.message
                else -> Unit
            }
        }
    }

    fun clearCancelError() {
        _cancelError.value = null
    }
}
