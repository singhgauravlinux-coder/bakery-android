package com.crumbandember.app.ui.navigation

sealed class Screen(val route: String) {
    object Login : Screen("login")
    object Register : Screen("register")
    object Catalog : Screen("catalog")
    object ProductDetail : Screen("product/{productId}") {
        fun createRoute(productId: String) = "product/$productId"
    }
    object Cart : Screen("cart")
    object LocationConsent : Screen("location-consent")    object Checkout : Screen("checkout")
    object OrderConfirmation : Screen("order/{orderId}") {
        fun createRoute(orderId: String) = "order/$orderId"
    }
    object Orders : Screen("orders")
    object Profile : Screen("profile")
}
