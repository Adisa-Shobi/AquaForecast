package com.example.aquaforecast.ui.splash

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

/**
 * ViewModel for splash screen
 * Always navigates to home since authentication is now optional
 */
class SplashViewModel : ViewModel() {

    private val _state = MutableStateFlow<SplashState>(SplashState.Loading)
    val state = _state.asStateFlow()

    init {
        navigateToHome()
    }

    /**
     * Wait briefly and navigate to home screen
     * Authentication is now optional and handled from Settings
     */
    private fun navigateToHome() {
        viewModelScope.launch {
            // Show splash for at least 1 second for better UX
            delay(1000)
            _state.update { SplashState.NavigateToHome }
        }
    }
}

/**
 * State for splash screen navigation
 */
sealed interface SplashState {
    data object Loading : SplashState
    data object NavigateToHome : SplashState
}
