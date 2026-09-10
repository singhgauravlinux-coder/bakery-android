package com.crumbandember.app.ui.screens.cart

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.crumbandember.app.data.model.Product
import com.crumbandember.app.data.repository.CartRepository
import com.crumbandember.app.data.repository.ProductRepository
import com.crumbandember.app.util.Resource
import kotlinx.coroutines.async
import kotlinx.coroutines.awaitAll
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch

data class CartLine(val product: Product, val quantity: Int)

class CartViewModel(
    private val userId: String,
    private val cartRepository: CartRepository,
    private val productRepository: ProductRepository
) : ViewModel() {

    private val _lines = MutableStateFlow<Resource<List<CartLine>>>(Resource.Loading)
    val lines: StateFlow<Resource<List<CartLine>>> = _lines

    val total: StateFlow<Double> get() = _total
    private val _total = MutableStateFlow(0.0)

    init {
        load()
    }

    fun load() {
        viewModelScope.launch {
            _lines.value = Resource.Loading
            when (val cartResult = cartRepository.getCart(userId)) {
                is Resource.Success -> {
                    val items = cartResult.data.items
                    if (items.isEmpty()) {
                        _lines.value = Resource.Success(emptyList())
                        _total.value = 0.0
                        return@launch
                    }
                    // cart-service only stores productId + quantity, so each line's
                    // price/name/description is resolved from product-catalog-service.
                    val productResults = items.map { item ->
                        async { item to productRepository.getProduct(item.productId) }
                    }.awaitAll()

                    val resolved = productResults.mapNotNull { (item, result) ->
                        (result as? Resource.Success)?.data?.let { CartLine(it, item.quantity) }
                    }
                    _lines.value = Resource.Success(resolved)
                    _total.value = resolved.sumOf { it.product.price * it.quantity }
                }
                is Resource.Error -> _lines.value = Resource.Error(cartResult.message, cartResult.kind)
                else -> {}
            }
        }
    }

    fun removeItem(productId: String) {
        viewModelScope.launch {
            cartRepository.removeItem(userId, productId)
            load()
        }
    }
}
