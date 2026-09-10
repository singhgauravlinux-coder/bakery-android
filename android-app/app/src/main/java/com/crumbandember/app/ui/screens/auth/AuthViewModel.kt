package com.crumbandember.app.ui.screens.auth

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.crumbandember.app.data.repository.AuthRepository
import com.crumbandember.app.util.Resource
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch

class AuthViewModel(private val repository: AuthRepository) : ViewModel() {

    private val _loginState = MutableStateFlow<Resource<Unit>>(Resource.Idle)
    val loginState: StateFlow<Resource<Unit>> = _loginState

    private val _registerState = MutableStateFlow<Resource<Unit>>(Resource.Idle)
    val registerState: StateFlow<Resource<Unit>> = _registerState

    fun login(email: String, password: String) {
        if (email.isBlank() || password.isBlank()) {
            _loginState.value = Resource.Error("Enter your email and password")
            return
        }
        viewModelScope.launch {
            _loginState.value = Resource.Loading
            _loginState.value = repository.login(email.trim(), password)
        }
    }

    fun register(email: String, password: String, name: String) {
        if (email.isBlank() || password.length < 8) {
            _registerState.value = Resource.Error("Email is required and password needs 8+ characters")
            return
        }
        viewModelScope.launch {
            _registerState.value = Resource.Loading
            _registerState.value = repository.register(email.trim(), password, name.ifBlank { null })
        }
    }

    fun resetRegisterState() {
        _registerState.value = Resource.Idle
    }
}
