package com.crumbandember.app.data.api

import com.crumbandember.app.BuildConfig
import com.crumbandember.app.data.local.TokenManager
import com.crumbandember.app.data.model.RefreshRequest
import kotlinx.coroutines.runBlocking
import okhttp3.Authenticator
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.Route
import okhttp3.logging.HttpLoggingInterceptor
import retrofit2.Retrofit
import retrofit2.converter.gson.GsonConverterFactory
import java.util.concurrent.TimeUnit

/**
 * Builds the single Retrofit/OkHttp client the whole app shares.
 *
 * Two pieces of gateway-specific behaviour live here:
 *  - every authenticated request gets `Authorization: Bearer <accessToken>`
 *    from TokenManager (see auth-service's sessionEnvelope for token shape)
 *  - a 401 triggers exactly one refresh attempt against POST /auth/refresh
 *    before giving up and forcing the user back to the login screen
 */
object NetworkModule {

    // Matched against Request.url.encodedPath with the leading "/" trimmed
    // (baseUrl's "/api/" prefix means these show up as "api/auth/register" etc).
    private val NO_AUTH_HEADER_PATHS = setOf(
        "api/auth/register",
        "api/auth/login",
        "api/auth/refresh"
    )

    fun create(tokenManager: TokenManager, onSessionExpired: () -> Unit): ApiService {
        val logging = HttpLoggingInterceptor().apply {
            level = if (BuildConfig.DEBUG) HttpLoggingInterceptor.Level.BODY else HttpLoggingInterceptor.Level.NONE
        }

        val authInterceptor = okhttp3.Interceptor { chain ->
            val original = chain.request()
            val path = original.url.encodedPath.trimStart('/')
            // These endpoints are unauthenticated by design — no session
            // exists yet (register/login) or the one we have is exactly what
            // we're trying to replace (refresh). Attaching a stale, expired,
            // or malformed bearer token to them serves no purpose and risks
            // the request being mangled or rejected before it ever reaches
            // auth-service's own validation.
            val request = if (path in NO_AUTH_HEADER_PATHS) {
                original
            } else {
                val token = runBlocking { tokenManager.accessToken() }
                if (token != null) {
                    original.newBuilder().addHeader("Authorization", "Bearer $token").build()
                } else {
                    original
                }
            }
            chain.proceed(request)
        }

        val authenticator = Authenticator { _: Route?, response ->
            // Refresh is single-use server-side (see auth-service's
            // rotateSession/reuse-detection), so only ever attempt it once
            // per failing request to avoid looping on a truly dead session.
            if (response.request.header("X-Retry") != null) return@Authenticator null

            val refreshToken = runBlocking { tokenManager.refreshToken() } ?: run {
                onSessionExpired()
                return@Authenticator null
            }

            val refreshed = runBlocking {
                runCatching {
                    val plainClient = OkHttpClient.Builder().build()
                    val plainRetrofit = Retrofit.Builder()
                        .baseUrl(BuildConfig.API_BASE_URL)
                        .client(plainClient)
                        .addConverterFactory(GsonConverterFactory.create())
                        .build()
                    val plainApi = plainRetrofit.create(ApiService::class.java)
                    val res = plainApi.refresh(RefreshRequest(refreshToken))
                    if (res.isSuccessful) res.body() else null
                }.getOrNull()
            }

            if (refreshed == null) {
                onSessionExpired()
                return@Authenticator null
            }

            runBlocking { tokenManager.updateAccessToken(refreshed.token, refreshed.refreshToken) }

            response.request.newBuilder()
                .header("Authorization", "Bearer ${refreshed.token}")
                .header("X-Retry", "true")
                .build()
        }

        val client = OkHttpClient.Builder()
            .addInterceptor(authInterceptor)
            .authenticator(authenticator)
            .addInterceptor(logging)
            .connectTimeout(10, TimeUnit.SECONDS)
            .readTimeout(15, TimeUnit.SECONDS)
            .build()

        val retrofit = Retrofit.Builder()
            .baseUrl(BuildConfig.API_BASE_URL)
            .client(client)
            .addConverterFactory(GsonConverterFactory.create())
            .build()

        return retrofit.create(ApiService::class.java)
    }
}
