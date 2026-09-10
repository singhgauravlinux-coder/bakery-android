package com.crumbandember.app.data.api

import com.crumbandember.app.data.model.*
import retrofit2.Response
import retrofit2.http.*

/**
 * Talks to the API gateway (services/api-gateway), which proxies each
 * segment to its owning microservice — see UPSTREAMS in server.js. Paths
 * here are the /api/<segment>/... routes the gateway exposes, not the
 * services' own internal ports.
 */
interface ApiService {

    // --- auth-service ----------------------------------------------------
    @POST("auth/register")
    suspend fun register(@Body body: RegisterRequest): Response<RegisterResponse>

    @POST("auth/login")
    suspend fun login(@Body body: LoginRequest): Response<AuthSession>

    @POST("auth/refresh")
    suspend fun refresh(@Body body: RefreshRequest): Response<AuthSession>

    @POST("auth/logout")
    suspend fun logout(@Body body: RefreshRequest): Response<Unit>

    // --- product-catalog-service ------------------------------------------
    @GET("products")
    suspend fun getProducts(@Query("category") category: String? = null): Response<List<Product>>

    @GET("products/{id}")
    suspend fun getProduct(@Path("id") id: String): Response<Product>

    // --- cart-service ------------------------------------------------------
    @GET("carts/{userId}")
    suspend fun getCart(@Path("userId") userId: String): Response<Cart>

    @POST("carts/{userId}/items")
    suspend fun addCartItem(
        @Path("userId") userId: String,
        @Body body: AddCartItemRequest
    ): Response<Cart>

    @DELETE("carts/{userId}/items/{productId}")
    suspend fun removeCartItem(
        @Path("userId") userId: String,
        @Path("productId") productId: String
    ): Response<Cart>

    @DELETE("carts/{userId}")
    suspend fun clearCart(@Path("userId") userId: String): Response<Cart>

    // --- order-service -------------------------------------------------------
    @POST("orders")
    suspend fun createOrder(@Body body: CreateOrderRequest): Response<Order>

    @GET("orders/{id}")
    suspend fun getOrder(@Path("id") id: String): Response<Order>

    @GET("orders")
    suspend fun getOrders(): Response<List<Order>>

    // --- search-service ------------------------------------------------------
    @GET("search")
    suspend fun search(@Query("q") query: String): Response<List<Product>>

    // --- consent-service -------------------------------------------------------
    @GET("consent/{userId}")
    suspend fun getConsent(
        @Path("userId") userId: String,
        @Query("type") consentType: String = "location"
    ): Response<ConsentRecord>

    @POST("consent/{userId}")
    suspend fun postConsent(
        @Path("userId") userId: String,
        @Body body: ConsentRequest
    ): Response<ConsentRecord>
}
