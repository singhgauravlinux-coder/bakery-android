package com.crumbandember.app

import android.app.Application
import com.crumbandember.app.data.api.NetworkModule
import com.crumbandember.app.data.local.TokenManager
import com.crumbandember.app.data.repository.AuthRepository
import com.crumbandember.app.data.repository.CartRepository
import com.crumbandember.app.data.repository.ConsentRepository
import com.crumbandember.app.data.repository.OrderRepository
import com.crumbandember.app.data.repository.ProductRepository
import com.crumbandember.app.util.AppNetworkState
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.launch

/**
 * Simple hand-rolled DI container. For a bigger app, swap this for Hilt —
 * kept manual here so the whole wiring is visible in one file.
 */
class BakeryApplication : Application() {

    lateinit var tokenManager: TokenManager
        private set
    lateinit var authRepository: AuthRepository
        private set
    lateinit var productRepository: ProductRepository
        private set
    lateinit var cartRepository: CartRepository
        private set
    lateinit var orderRepository: OrderRepository
        private set
    lateinit var consentRepository: ConsentRepository
        private set

    private val appScope = CoroutineScope(SupervisorJob())

    override fun onCreate() {
        super.onCreate()

        AppNetworkState.init(this)
        tokenManager = TokenManager(this)

        val api = NetworkModule.create(tokenManager) {
            // Called by the OkHttp Authenticator when refresh fails outright.
            // Clearing here means the next screen read of isLoggedIn routes
            // the user back to the login graph.
            appScope.launch { tokenManager.clear() }
        }

        authRepository = AuthRepository(api, tokenManager)
        productRepository = ProductRepository(api)
        cartRepository = CartRepository(api)
        orderRepository = OrderRepository(api)
        consentRepository = ConsentRepository(api)
    }
}
