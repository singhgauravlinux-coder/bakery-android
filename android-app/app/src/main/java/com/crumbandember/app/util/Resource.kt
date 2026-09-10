package com.crumbandember.app.util

import java.io.IOException
import java.net.ConnectException
import java.net.SocketTimeoutException
import java.net.UnknownHostException

/**
 * Why a call failed, distinct from the human-readable message, so the UI can
 * show a full dedicated screen for the failures that need one (gateway down,
 * one feature's backend down) instead of a generic inline error everywhere.
 */
enum class ErrorKind {
    /** Device has no network at all — never even left the phone. */
    OFFLINE,

    /** Device is online but api-gateway itself didn't respond — the whole
     *  app is unusable, not just one screen. */
    GATEWAY_DOWN,

    /** api-gateway responded, but the specific microservice behind this
     *  screen is down or timed out (502/503/504 — see UPSTREAMS handling in
     *  services/api-gateway/server.js). Other parts of the app may still
     *  work fine. */
    SERVICE_UNAVAILABLE,

    /** api-gateway's own bug (500), not a downstream service. */
    SERVER_ERROR,

    /** Session is dead and silent refresh already failed. */
    UNAUTHORIZED,

    /** 404 or similar — nothing wrong with the server, this just isn't there. */
    NOT_FOUND,

    /** Validation errors, unexpected shapes, anything else — keep showing
     *  the existing small inline error, no dedicated screen needed. */
    UNKNOWN
}

sealed class Resource<out T> {
    object Idle : Resource<Nothing>()
    object Loading : Resource<Nothing>()
    data class Success<T>(val data: T) : Resource<T>()
    data class Error(val message: String, val kind: ErrorKind = ErrorKind.UNKNOWN) : Resource<Nothing>()
}

/**
 * Turns a Retrofit Response into a Resource, reading the gateway's
 * {error, reason} envelope on failure and classifying *why* it failed so the
 * UI can pick the right full-screen state:
 *
 *  - no connectivity at all               -> OFFLINE
 *  - connectivity, but gateway unreachable -> GATEWAY_DOWN
 *  - gateway up, 502/503/504 from a proxy call -> SERVICE_UNAVAILABLE
 *  - gateway up, 500 of its own            -> SERVER_ERROR
 *  - 401 after silent refresh already failed -> UNAUTHORIZED
 *  - 404                                    -> NOT_FOUND
 *  - anything else                          -> UNKNOWN (small inline error)
 */
suspend fun <T> safeApiCall(call: suspend () -> retrofit2.Response<T>): Resource<T> {
    return try {
        val response = call()
        if (response.isSuccessful) {
            val body = response.body()
            if (body != null) Resource.Success(body) else Resource.Error("Empty response from server")
        } else {
            val raw = response.errorBody()?.string()
            val serverMessage = runCatching {
                com.google.gson.Gson().fromJson(raw, com.crumbandember.app.data.model.ApiError::class.java)?.error
            }.getOrNull()

            val kind = when (response.code()) {
                502, 503, 504 -> ErrorKind.SERVICE_UNAVAILABLE
                500 -> ErrorKind.SERVER_ERROR
                401 -> ErrorKind.UNAUTHORIZED
                404 -> ErrorKind.NOT_FOUND
                else -> ErrorKind.UNKNOWN
            }
            Resource.Error(serverMessage ?: "Request failed (${response.code()})", kind)
        }
    } catch (e: UnknownHostException) {
        // DNS/host resolution failed. Could be no network, could be a bad
        // host config — AppNetworkState tells the two apart.
        Resource.Error(
            if (AppNetworkState.isOnline()) "Can't reach the server" else "You're offline",
            if (AppNetworkState.isOnline()) ErrorKind.GATEWAY_DOWN else ErrorKind.OFFLINE
        )
    } catch (e: ConnectException) {
        Resource.Error("Can't reach the server", ErrorKind.GATEWAY_DOWN)
    } catch (e: SocketTimeoutException) {
        Resource.Error("The server is taking too long to respond", ErrorKind.GATEWAY_DOWN)
    } catch (e: IOException) {
        Resource.Error(
            if (AppNetworkState.isOnline()) "Can't reach the server" else "You're offline",
            if (AppNetworkState.isOnline()) ErrorKind.GATEWAY_DOWN else ErrorKind.OFFLINE
        )
    } catch (e: Exception) {
        Resource.Error(e.message ?: "Unexpected error", ErrorKind.UNKNOWN)
    }
}
