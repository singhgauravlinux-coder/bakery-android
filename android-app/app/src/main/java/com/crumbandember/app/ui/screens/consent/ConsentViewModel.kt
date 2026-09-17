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

    /** True once the person has made a choice in this session, whether or
     *  not it made it to consent-service. The screen navigates on this,
     *  not on [saveState], so a slow or unreachable backend never traps
     *  someone on the consent prompt. */
    private val _decided = MutableStateFlow(false)
    val decided: StateFlow<Boolean> = _decided

    fun load() {
        viewModelScope.launch {
            _consent.value = Resource.Loading
            _consent.value = consentRepository.getLocationConsent(userId)
        }
    }

    /**
     * Records the decision. The caller is responsible for actually
     * requesting the OS runtime permission when `granted` is true — this
     * only records what the person chose, it doesn't ask Android for
     * anything itself.
     *
     * Persisting to consent-service is best-effort: [_decided] flips as
     * soon as the person taps, regardless of whether the network call
     * behind it succeeds, so a down or slow consent-service never blocks
     * someone from reaching the app after they've made a choice. A failed
     * save is surfaced via [saveState] only as a quiet, non-blocking
     * signal (e.g. a snackbar) — it never gates navigation. The stale
     * value gets corrected on the next successful getLocationConsent/
     * setConsent call.
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
            _decided.value = true
        }
    }
}
