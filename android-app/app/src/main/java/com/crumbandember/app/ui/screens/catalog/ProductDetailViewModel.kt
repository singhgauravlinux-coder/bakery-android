package com.crumbandember.app.ui.screens.catalog

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.crumbandember.app.data.model.Product
import com.crumbandember.app.data.repository.CartRepository
import com.crumbandember.app.data.repository.ProductRepository
import com.crumbandember.app.util.Resource
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch

class ProductDetailViewModel(
    private val productId: String,
    private val productRepository: ProductRepository,
    private val cartRepository: CartRepository,
    private val userId: String
) : ViewModel() {

    private val _product = MutableStateFlow<Resource<Product>>(Resource.Loading)
    val product: StateFlow<Resource<Product>> = _product

    private val _addToCartState = MutableStateFlow<Resource<Unit>>(Resource.Idle)
    val addToCartState: StateFlow<Resource<Unit>> = _addToCartState

    init {
        load()
    }

    private fun load() {
        viewModelScope.launch {
            _product.value = Resource.Loading
            _product.value = productRepository.getProduct(productId)
        }
    }

    fun retry() = load()

    fun addToCart(quantity: Int) {
        viewModelScope.launch {
            _addToCartState.value = Resource.Loading
            val result = cartRepository.addItem(userId, productId, quantity)
            _addToCartState.value = when (result) {
                is Resource.Success -> Resource.Success(Unit)
                is Resource.Error -> Resource.Error(result.message, result.kind)
                else -> Resource.Idle
            }
        }
    }

    fun resetAddToCartState() {
        _addToCartState.value = Resource.Idle
    }
}
