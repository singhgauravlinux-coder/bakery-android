package com.crumbandember.app.data.repository

import com.crumbandember.app.data.api.ApiService
import com.crumbandember.app.data.model.LoyaltyAccount
import com.crumbandember.app.util.Resource
import com.crumbandember.app.util.safeApiCall

class LoyaltyRepository(private val api: ApiService) {

    suspend fun getAccount(userId: String): Resource<LoyaltyAccount> =
        safeApiCall { api.getLoyaltyAccount(userId) }
}
