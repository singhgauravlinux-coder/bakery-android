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

    fun loadOrders() {
        viewModelScope.launch {
            _orders.value = Resource.Loading
            _orders.value = repository.getOrders()
        }
    }

    fun loadOrder(orderId: String) {
        viewModelScope.launch {
            _order.value = Resource.Loading
            _order.value = repository.getOrder(orderId)
        }
    }
}
