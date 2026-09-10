package com.crumbandember.app.util

import android.content.Context
import android.net.ConnectivityManager
import android.net.NetworkCapabilities

/**
 * Tiny wrapper so safeApiCall (a plain suspend function, no Context of its
 * own) can tell "this phone has no network at all" apart from "the phone is
 * online but api-gateway didn't answer". Initialized once from
 * BakeryApplication.onCreate — never holds an Activity, only the process-wide
 * ApplicationContext, so no leak risk.
 */
object AppNetworkState {
    private var appContext: Context? = null

    fun init(context: Context) {
        appContext = context.applicationContext
    }

    fun isOnline(): Boolean {
        val context = appContext ?: return true // unknown -> don't over-claim "offline"
        val cm = context.getSystemService(Context.CONNECTIVITY_SERVICE) as? ConnectivityManager
            ?: return true
        val network = cm.activeNetwork ?: return false
        val capabilities = cm.getNetworkCapabilities(network) ?: return false
        return capabilities.hasCapability(NetworkCapabilities.NET_CAPABILITY_INTERNET)
    }
}
