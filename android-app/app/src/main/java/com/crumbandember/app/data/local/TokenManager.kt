package com.crumbandember.app.data.local

import android.content.Context
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import com.crumbandember.app.data.model.AuthSession
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.map

private val Context.dataStore by preferencesDataStore(name = "bakery_session")

/**
 * Persists the access/refresh token pair issued by auth-service so the user
 * stays logged in across app restarts. Mirrors what the web storefront keeps
 * in memory + localStorage.
 */
class TokenManager(private val context: Context) {

    private object Keys {
        val ACCESS_TOKEN = stringPreferencesKey("access_token")
        val REFRESH_TOKEN = stringPreferencesKey("refresh_token")
        val USER_ID = stringPreferencesKey("user_id")
        val USER_NAME = stringPreferencesKey("user_name")
    }

    val accessTokenFlow: Flow<String?> = context.dataStore.data.map { it[Keys.ACCESS_TOKEN] }
    val userIdFlow: Flow<String?> = context.dataStore.data.map { it[Keys.USER_ID] }
    val userNameFlow: Flow<String?> = context.dataStore.data.map { it[Keys.USER_NAME] }

    suspend fun accessToken(): String? = accessTokenFlow.first()
    suspend fun refreshToken(): String? = context.dataStore.data.map { it[Keys.REFRESH_TOKEN] }.first()
    suspend fun userId(): String? = userIdFlow.first()

    suspend fun save(session: AuthSession) {
        context.dataStore.edit { prefs ->
            prefs[Keys.ACCESS_TOKEN] = session.token
            prefs[Keys.REFRESH_TOKEN] = session.refreshToken
            prefs[Keys.USER_ID] = session.userId
            prefs[Keys.USER_NAME] = session.name ?: session.userId
        }
    }

    suspend fun updateAccessToken(newAccessToken: String, newRefreshToken: String) {
        context.dataStore.edit { prefs ->
            prefs[Keys.ACCESS_TOKEN] = newAccessToken
            prefs[Keys.REFRESH_TOKEN] = newRefreshToken
        }
    }

    suspend fun clear() {
        context.dataStore.edit { it.clear() }
    }
}
