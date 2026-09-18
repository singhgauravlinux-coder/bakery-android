package com.crumbandember.app.data.repository

import com.crumbandember.app.data.api.ApiService
import com.crumbandember.app.data.model.ConfirmOrderRequest
import com.crumbandember.app.data.model.CreateOrderRequest
import com.crumbandember.app.data.model.Order
import com.crumbandember.app.data.model.UpdateOrderStatusRequest
import com.crumbandember.app.util.Resource
import com.crumbandember.app.util.safeApiCall

class OrderRepository(private val api: ApiService) {

    suspend fun createOrder(request: CreateOrderRequest): Resource<Order> =
        safeApiCall { api.createOrder(request) }

    /** Confirms a pending_payment order once a succeeded payment exists for it. */
    suspend fun confirmOrder(orderId: String, paymentId: String): Resource<Order> =
        safeApiCall { api.confirmOrder(orderId, ConfirmOrderRequest(paymentId)) }

    suspend fun updateStatus(orderId: String, status: String): Resource<Order> =
        safeApiCall { api.updateOrderStatus(orderId, UpdateOrderStatusRequest(status)) }

    /** Cancelling is only offered in the UI while an order is still pending_payment. */
    suspend fun cancelOrder(orderId: String): Resource<Order> = updateStatus(orderId, "cancelled")

    suspend fun getOrder(id: String): Resource<Order> =
        safeApiCall { api.getOrder(id) }

    suspend fun getOrders(userId: String): Resource<List<Order>> =
        safeApiCall { api.getOrders(userId) }
}
