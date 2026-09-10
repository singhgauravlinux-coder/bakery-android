package com.crumbandember.app.data.repository

import com.crumbandember.app.data.api.ApiService
import com.crumbandember.app.data.model.AddCartItemRequest
import com.crumbandember.app.data.model.Cart
import com.crumbandember.app.util.Resource
import com.crumbandember.app.util.safeApiCall

class CartRepository(private val api: ApiService) {

    suspend fun getCart(userId: String): Resource<Cart> =
        safeApiCall { api.getCart(userId) }

    suspend fun addItem(userId: String, productId: String, quantity: Int): Resource<Cart> =
        safeApiCall { api.addCartItem(userId, AddCartItemRequest(productId, quantity)) }

    suspend fun removeItem(userId: String, productId: String): Resource<Cart> =
        safeApiCall { api.removeCartItem(userId, productId) }

    suspend fun clear(userId: String): Resource<Cart> =
        safeApiCall { api.clearCart(userId) }
}
