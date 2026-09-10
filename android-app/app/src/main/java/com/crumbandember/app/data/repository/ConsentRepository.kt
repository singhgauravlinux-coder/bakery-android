package com.crumbandember.app.data.repository

import com.crumbandember.app.data.api.ApiService
import com.crumbandember.app.data.model.ConsentRecord
import com.crumbandember.app.data.model.ConsentRequest
import com.crumbandember.app.util.Resource
import com.crumbandember.app.util.safeApiCall

class ConsentRepository(private val api: ApiService) {

    suspend fun getLocationConsent(userId: String): Resource<ConsentRecord> =
        safeApiCall { api.getConsent(userId, "location") }

    suspend fun setLocationConsent(userId: String, granted: Boolean): Resource<ConsentRecord> =
        safeApiCall { api.postConsent(userId, ConsentRequest(consentType = "location", granted = granted)) }
}
