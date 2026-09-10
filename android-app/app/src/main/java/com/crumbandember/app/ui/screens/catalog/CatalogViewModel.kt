package com.crumbandember.app.ui.screens.catalog

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.crumbandember.app.data.model.Product
import com.crumbandember.app.data.repository.ProductRepository
import com.crumbandember.app.util.Resource
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch

class CatalogViewModel(private val repository: ProductRepository) : ViewModel() {

    private val _products = MutableStateFlow<Resource<List<Product>>>(Resource.Loading)
    val products: StateFlow<Resource<List<Product>>> = _products

    private val _query = MutableStateFlow("")
    val query: StateFlow<String> = _query

    init {
        loadProducts()
    }

    fun loadProducts(category: String? = null) {
        viewModelScope.launch {
            _products.value = Resource.Loading
            _products.value = repository.getProducts(category)
        }
    }

    fun onQueryChange(newQuery: String) {
        _query.value = newQuery
    }

    fun search() {
        val q = _query.value.trim()
        if (q.isEmpty()) {
            loadProducts()
            return
        }
        viewModelScope.launch {
            _products.value = Resource.Loading
            _products.value = repository.search(q)
        }
    }
}
