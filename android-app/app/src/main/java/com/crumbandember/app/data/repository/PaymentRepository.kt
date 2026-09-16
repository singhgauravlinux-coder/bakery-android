package com.crumbandember.app.data.repository

import com.crumbandember.app.data.api.ApiService
import com.crumbandember.app.data.model.CreatePaymentRequest
import com.crumbandember.app.data.model.Payment
import com.crumbandember.app.util.Resource
import com.crumbandember.app.util.safeApiCall

/**
 * Talks to payment-service's mock provider (POST /payments). Card/UPI
 * instrument fields are validated server-side (Luhn check, expiry, VPA
 * shape) — this repository just forwards whatever the checkout form
 * collected and passes the resulting success/rejection straight through.
 */
class PaymentRepository(private val api: ApiService) {

    suspend fun pay(request: CreatePaymentRequest): Resource<Payment> =
        safeApiCall { api.createPayment(request) }
}
