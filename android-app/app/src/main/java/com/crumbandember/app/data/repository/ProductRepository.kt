package com.crumbandember.app.data.repository

import com.crumbandember.app.data.api.ApiService
import com.crumbandember.app.data.model.Product
import com.crumbandember.app.util.Resource
import com.crumbandember.app.util.safeApiCall

class ProductRepository(private val api: ApiService) {

    suspend fun getProducts(category: String? = null): Resource<List<Product>> =
        safeApiCall { api.getProducts(category) }

    suspend fun getProduct(id: String): Resource<Product> =
        safeApiCall { api.getProduct(id) }

    suspend fun search(query: String): Resource<List<Product>> =
        safeApiCall { api.search(query) }
}
