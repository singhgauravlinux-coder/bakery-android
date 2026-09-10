package com.crumbandember.app.ui.screens.consent

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.crumbandember.app.data.model.ConsentRecord
import com.crumbandember.app.data.repository.ConsentRepository
import com.crumbandember.app.util.Resource
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch

class ConsentViewModel(
    private val userId: String,
    private val consentRepository: ConsentRepository
) : ViewModel() {

    private val _consent = MutableStateFlow<Resource<ConsentRecord>>(Resource.Loading)
    val consent: StateFlow<Resource<ConsentRecord>> = _consent

    private val _saveState = MutableStateFlow<Resource<Unit>>(Resource.Idle)
    val saveState: StateFlow<Resource<Unit>> = _saveState

    fun load() {
        viewModelScope.launch {
            _consent.value = Resource.Loading
            _consent.value = consentRepository.getLocationConsent(userId)
        }
    }

    /**
     * Persists the decision to consent-service. The caller is responsible
     * for actually requesting the OS runtime permission when `granted` is
     * true — this only records what the person chose, it doesn't ask
     * Android for anything itself.
     */
    fun setConsent(granted: Boolean) {
        viewModelScope.launch {
            _saveState.value = Resource.Loading
            val result = consentRepository.setLocationConsent(userId, granted)
            _saveState.value = when (result) {
                is Resource.Success -> {
                    _consent.value = result
                    Resource.Success(Unit)
                }
                is Resource.Error -> Resource.Error(result.message, result.kind)
                else -> Resource.Idle
            }
        }
    }
}
