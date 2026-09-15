package com.crumbandember.app.data.model

import com.google.gson.Gson
import com.google.gson.JsonParser
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

/**
 * Locks the JSON key names the backend actually validates on.
 *
 * This is the source-level half of the guard that was missing when a
 * minified production APK shipped posting {"a":..,"b":..,"c":..} to
 * /auth/register and collected a 400 from auth-service. Renaming a property
 * in Models.kt breaks the contract here; renaming it *at build time* (R8) is
 * caught by scripts/verify-minified-wire-contract.sh against mapping.txt.
 */
class WireContractTest {

    private val gson = Gson()

    private fun keysOf(body: Any): Set<String> =
        JsonParser.parseString(gson.toJson(body)).asJsonObject.keySet()

    @Test
    fun registerRequest_matches_auth_service_contract() {
        // auth-service: `const { email, password, name } = req.body`
        // 400 unless both `email` and `password` are present.
        val keys = keysOf(RegisterRequest("a@b.com", "hunter2hunter2", "Test"))
        assertEquals(setOf("email", "password", "name"), keys)
    }

    @Test
    fun loginRequest_matches_auth_service_contract() {
        assertEquals(setOf("email", "password"), keysOf(LoginRequest("a@b.com", "hunter2hunter2")))
    }

    @Test
    fun refreshRequest_matches_auth_service_contract() {
        assertEquals(setOf("refreshToken"), keysOf(RefreshRequest("tok")))
    }

    @Test
    fun authSession_deserialises_from_the_session_envelope_auth_service_sends() {
        val envelope = """
            {"token":"t","tokenType":"Bearer","refreshToken":"r","sessionId":"s",
             "expiresIn":900,"expiresAt":"2026-01-01T00:00:00.000Z",
             "userId":"u-1","name":"Test","emailVerified":false}
        """.trimIndent()
        val session = gson.fromJson(envelope, AuthSession::class.java)
        assertEquals("t", session.token)
        assertEquals("r", session.refreshToken)
        assertEquals("u-1", session.userId)
        assertEquals(900L, session.expiresIn)
    }

    @Test
    fun registerResponse_deserialises_userId() {
        assertEquals("u-9", gson.fromJson("""{"userId":"u-9"}""", RegisterResponse::class.java).userId)
    }

    @Test
    fun orderAndCart_bodies_keep_their_backend_key_names() {
        assertEquals(setOf("productId", "quantity"), keysOf(AddCartItemRequest("p-1", 2)))
        val orderKeys = keysOf(
            CreateOrderRequest(
                userId = "u-1",
                items = listOf(CartItem("p-1", 1)),
                pickupTime = null,
                paymentMethod = "cod",
                amount = 9.5,
                currency = "EUR"
            )
        )
        assertTrue(orderKeys.containsAll(setOf("userId", "items", "paymentMethod", "amount", "currency")))
    }

    @Test
    fun consentRequest_keeps_its_backend_key_names() {
        assertEquals(setOf("consentType", "granted", "source"), keysOf(ConsentRequest(granted = true)))
    }
}
