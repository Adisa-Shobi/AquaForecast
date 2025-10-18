package com.example.aquaforecast.ui.splash

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.aquaforecast.domain.repository.AuthRepository
import com.example.aquaforecast.domain.repository.onError
import com.example.aquaforecast.domain.repository.onSuccess
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

/**
 * ViewModel for splash screen
 * Checks authentication status and determines navigation
 */
class SplashViewModel(
    private val authRepository: AuthRepository
) : ViewModel() {

    private val _state = MutableStateFlow<SplashState>(SplashState.Loading)
    val state = _state.asStateFlow()

    init {
        checkAuthStatus()
    }

    /**
     * Check if user is logged in and navigate accordingly
     */
    private fun checkAuthStatus() {
        viewModelScope.launch {
            // Show splash for at least 1 second for better UX
            delay(1000)

            authRepository.isLoggedIn()
                .onSuccess { isLoggedIn ->
                    if (isLoggedIn) {
                        _state.update { SplashState.NavigateToHome }
                    } else {
                        _state.update { SplashState.NavigateToLogin }
                    }
                }
                .onError {
                    // On error, assume not logged in
                    _state.update { SplashState.NavigateToLogin }
                }
        }
    }
}

/**
 * State for splash screen navigation
 */
sealed interface SplashState {
    data object Loading : SplashState
    data object NavigateToLogin : SplashState
    data object NavigateToHome : SplashState
}
