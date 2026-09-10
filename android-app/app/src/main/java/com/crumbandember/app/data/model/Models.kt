package com.crumbandember.app.data.model

// --- auth-service: POST /auth/login, /auth/register, /auth/refresh -------

data class LoginRequest(val email: String, val password: String)

data class RegisterRequest(val email: String, val password: String, val name: String?)

data class RegisterResponse(val userId: String)

/** Mirrors auth-service's sessionEnvelope() + the fields spread alongside it. */
data class AuthSession(
    val token: String,
    val tokenType: String,
    val refreshToken: String,
    val sessionId: String,
    val expiresIn: Long,
    val expiresAt: String,
    val userId: String,
    val name: String?,
    val emailVerified: Boolean
)

data class RefreshRequest(val refreshToken: String)

// --- product-catalog-service: GET /products, GET /products/:id ----------

data class Product(
    val id: String,
    val name: String,
    val category: String?,
    val price: Double,
    val description: String?
)

// --- cart-service: GET/POST/DELETE /carts/:userId[/items[/:productId]] --

data class CartItem(
    val productId: String,
    val quantity: Int
)

data class Cart(
    val userId: String,
    val items: List<CartItem>
)

data class AddCartItemRequest(
    val productId: String,
    val quantity: Int
)

// --- order-service: POST /orders, GET /orders, GET /orders/:id ----------

data class CreateOrderRequest(
    val userId: String,
    val items: List<CartItem>,
    val pickupTime: String? = null,
    val paymentMethod: String, // "card" | "upi" | "cod" | "razorpay"
    val amount: Double?,
    val currency: String = "EUR"
)

data class Order(
    val id: String,
    val userId: String,
    val items: List<CartItem>,
    val pickupTime: String?,
    val status: String,
    val paymentMethod: String,
    val paymentStatus: String,
    val amount: Double?,
    val currency: String,
    val createdAt: String? = null,
    val updatedAt: String? = null
)

// --- consent-service: GET/POST /consent/:userId -------------------------

data class ConsentRequest(
    val consentType: String = "location",
    val granted: Boolean,
    val source: String = "app"
)

/**
 * `granted == null` means the user has never been asked yet — distinct
 * from an explicit decline — so the app knows whether to show the prompt.
 */
data class ConsentRecord(
    val userId: String,
    val consentType: String,
    val granted: Boolean?,
    val respondedAt: String?
)

// --- generic gateway error envelope --------------------------------------

data class ApiError(val error: String?, val reason: String? = null, val traceId: String? = null)
