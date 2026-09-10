package com.crumbandember.app.data.repository

import com.crumbandember.app.data.api.ApiService
import com.crumbandember.app.data.model.CreateOrderRequest
import com.crumbandember.app.data.model.Order
import com.crumbandember.app.util.Resource
import com.crumbandember.app.util.safeApiCall

class OrderRepository(private val api: ApiService) {

    suspend fun createOrder(request: CreateOrderRequest): Resource<Order> =
        safeApiCall { api.createOrder(request) }

    suspend fun getOrder(id: String): Resource<Order> =
        safeApiCall { api.getOrder(id) }

    suspend fun getOrders(): Resource<List<Order>> =
        safeApiCall { api.getOrders() }
}
