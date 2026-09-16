package com.crumbandember.app.ui.navigation

sealed class Screen(val route: String) {
    object Splash : Screen("splash")
    object Login : Screen("login")
    object Register : Screen("register")
    object ForgotPassword : Screen("forgot-password")
    object Catalog : Screen("catalog")
    object Search : Screen("search")
    object ProductDetail : Screen("product/{productId}") {
        fun createRoute(productId: String) = "product/$productId"
    }
    object Cart : Screen("cart")
    object LocationConsent : Screen("location-consent")
    object Checkout : Screen("checkout")
    object OrderConfirmation : Screen("order/{orderId}") {
        fun createRoute(orderId: String) = "order/$orderId"
    }
    object Orders : Screen("orders")
    object Profile : Screen("profile")
}
