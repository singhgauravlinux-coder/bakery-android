package com.crumbandember.app.ui.navigation

import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.navigation.NavHostController
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import androidx.navigation.navArgument
import androidx.navigation.NavType
import com.crumbandember.app.BakeryApplication
import com.crumbandember.app.ui.screens.auth.LoginScreen
import com.crumbandember.app.ui.screens.auth.RegisterScreen
import com.crumbandember.app.ui.screens.cart.CartScreen
import com.crumbandember.app.ui.screens.catalog.CatalogScreen
import com.crumbandember.app.ui.screens.catalog.ProductDetailScreen
import com.crumbandember.app.ui.screens.checkout.CheckoutScreen
import com.crumbandember.app.ui.screens.consent.LocationConsentScreen
import com.crumbandember.app.ui.screens.orders.OrderDetailScreen
import com.crumbandember.app.ui.screens.orders.OrderHistoryScreen
import com.crumbandember.app.ui.screens.profile.ProfileScreen
import com.crumbandember.app.util.Resource
import kotlinx.coroutines.launch
import kotlinx.coroutines.runBlocking

/**
 * Single NavHost for the whole app. Auth state (are we logged in?) decides
 * the start destination; every screen below auth reads userId from
 * TokenManager rather than threading it through every nav argument.
 */
@Composable
fun BakeryNavGraph(app: BakeryApplication) {
    val navController: NavHostController = rememberNavController()
    val isLoggedIn by app.authRepository.isLoggedIn.collectAsState(initial = null)
    val scope = rememberCoroutineScope()

    // Simple in-memory cart badge count, refreshed whenever we land on the catalog.
    var cartCount by remember { mutableStateOf(0) }

    // isLoggedIn starts out null until the DataStore flow emits its first
    // value; wait for that before deciding the start destination so a
    // logged-in user isn't flashed the login screen on cold start.
    if (isLoggedIn == null) return

    NavHost(
        navController = navController,
        startDestination = if (isLoggedIn == true) Screen.Catalog.route else Screen.Login.route
    ) {
        composable(Screen.Login.route) {
            LoginScreen(
                authRepository = app.authRepository,
                onLoginSuccess = {
                    // Only ever prompted once: if consent-service has no
                    // record yet (granted == null) for this user, gate on
                    // the consent screen before the catalog; otherwise skip
                    // straight there, same as any returning user.
                    scope.launch {
                        val userId = app.authRepository.currentUserId() ?: ""
                        val consentResult = app.consentRepository.getLocationConsent(userId)
                        val neverAsked = (consentResult as? Resource.Success)?.data?.granted == null
                        val destination = if (neverAsked) Screen.LocationConsent.route else Screen.Catalog.route
                        navController.navigate(destination) {
                            popUpTo(Screen.Login.route) { inclusive = true }
                        }
                    }
                },
                onNavigateToRegister = { navController.navigate(Screen.Register.route) }
            )
        }

        composable(Screen.LocationConsent.route) {
            val userId = remember { runBlocking { app.authRepository.currentUserId() } } ?: ""
            LocationConsentScreen(
                userId = userId,
                consentRepository = app.consentRepository,
                onDone = {
                    // Reached from Login (post-login gate, nothing to pop
                    // back to) vs. from Profile (revisiting the choice) need
                    // different endings — the immediate predecessor tells
                    // them apart without a second route/argument.
                    if (navController.previousBackStackEntry?.destination?.route == Screen.Profile.route) {
                        navController.popBackStack()
                    } else {
                        navController.navigate(Screen.Catalog.route) {
                            popUpTo(Screen.Login.route) { inclusive = true }
                        }
                    }
                }
            )
        }

        composable(Screen.Register.route) {
            RegisterScreen(
                authRepository = app.authRepository,
                onRegisterSuccess = { navController.popBackStack() },
                onNavigateToLogin = { navController.popBackStack() }
            )
        }

        composable(Screen.Catalog.route) {
            CatalogScreen(
                productRepository = app.productRepository,
                cartCount = cartCount,
                onProductClick = { productId ->
                    navController.navigate(Screen.ProductDetail.createRoute(productId))
                },
                onCartClick = { navController.navigate(Screen.Cart.route) },
                onProfileClick = { navController.navigate(Screen.Profile.route) }
            )
        }

        composable(
            route = Screen.ProductDetail.route,
            arguments = listOf(navArgument("productId") { type = NavType.StringType })
        ) { backStackEntry ->
            val productId = backStackEntry.arguments?.getString("productId") ?: return@composable
            val userId = remember { runBlocking { app.authRepository.currentUserId() } } ?: ""
            ProductDetailScreen(
                productId = productId,
                userId = userId,
                productRepository = app.productRepository,
                cartRepository = app.cartRepository,
                onBack = { navController.popBackStack() },
                onGoToCart = { navController.navigate(Screen.Cart.route) }
            )
            LaunchedEffect(Unit) { cartCount++ }
        }

        composable(Screen.Cart.route) {
            val userId = remember { runBlocking { app.authRepository.currentUserId() } } ?: ""
            CartScreen(
                userId = userId,
                cartRepository = app.cartRepository,
                productRepository = app.productRepository,
                onBack = { navController.popBackStack() },
                onCheckout = { navController.navigate(Screen.Checkout.route) }
            )
        }

        composable(Screen.Checkout.route) {
            val userId = remember { runBlocking { app.authRepository.currentUserId() } } ?: ""
            CheckoutScreen(
                userId = userId,
                cartRepository = app.cartRepository,
                productRepository = app.productRepository,
                orderRepository = app.orderRepository,
                onBack = { navController.popBackStack() },
                onOrderPlaced = { orderId ->
                    cartCount = 0
                    navController.navigate(Screen.OrderConfirmation.createRoute(orderId)) {
                        popUpTo(Screen.Catalog.route)
                    }
                }
            )
        }

        composable(
            route = Screen.OrderConfirmation.route,
            arguments = listOf(navArgument("orderId") { type = NavType.StringType })
        ) { backStackEntry ->
            val orderId = backStackEntry.arguments?.getString("orderId") ?: return@composable
            OrderDetailScreen(
                orderId = orderId,
                orderRepository = app.orderRepository,
                onDoneShopping = {
                    navController.navigate(Screen.Catalog.route) {
                        popUpTo(Screen.Catalog.route) { inclusive = true }
                    }
                }
            )
        }

        composable(Screen.Orders.route) {
            OrderHistoryScreen(
                orderRepository = app.orderRepository,
                onBack = { navController.popBackStack() },
                onOrderClick = { orderId -> navController.navigate(Screen.OrderConfirmation.createRoute(orderId)) }
            )
        }

        composable(Screen.Profile.route) {
            ProfileScreen(
                authRepository = app.authRepository,
                consentRepository = app.consentRepository,
                onBack = { navController.popBackStack() },
                onViewOrders = { navController.navigate(Screen.Orders.route) },
                onManageLocationAccess = { navController.navigate(Screen.LocationConsent.route) },
                onLoggedOut = {
                    navController.navigate(Screen.Login.route) {
                        popUpTo(0)
                    }
                }
            )
        }
    }
}
