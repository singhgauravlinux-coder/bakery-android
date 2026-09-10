package com.crumbandember.app.data.repository

import com.crumbandember.app.data.api.ApiService
import com.crumbandember.app.data.local.TokenManager
import com.crumbandember.app.data.model.LoginRequest
import com.crumbandember.app.data.model.RefreshRequest
import com.crumbandember.app.data.model.RegisterRequest
import com.crumbandember.app.util.Resource
import com.crumbandember.app.util.safeApiCall
import kotlinx.coroutines.flow.Flow

class AuthRepository(
    private val api: ApiService,
    private val tokenManager: TokenManager
) {
    val isLoggedIn: Flow<Boolean> = tokenManager.accessTokenFlow.let { flow ->
        kotlinx.coroutines.flow.map(flow) { it != null }
    }
    val userName: Flow<String?> = tokenManager.userNameFlow

    suspend fun login(email: String, password: String): Resource<Unit> {
        val result = safeApiCall { api.login(LoginRequest(email, password)) }
        if (result is Resource.Success) {
            tokenManager.save(result.data)
            return Resource.Success(Unit)
        }
        val error = result as? Resource.Error
        return Resource.Error(error?.message ?: "Login failed", error?.kind ?: com.crumbandember.app.util.ErrorKind.UNKNOWN)
    }

    suspend fun register(email: String, password: String, name: String?): Resource<Unit> {
        val result = safeApiCall { api.register(RegisterRequest(email, password, name)) }
        return when (result) {
            is Resource.Success -> Resource.Success(Unit)
            is Resource.Error -> Resource.Error(result.message, result.kind)
            else -> Resource.Error("Registration failed")
        }
    }

    suspend fun logout() {
        val refreshToken = tokenManager.refreshToken()
        if (refreshToken != null) {
            runCatching { api.logout(RefreshRequest(refreshToken)) }
        }
        tokenManager.clear()
    }

    suspend fun currentUserId(): String? = tokenManager.userId()
}
