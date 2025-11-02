package com.example.aquaforecast.ui.auth

import android.content.Context
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.aquaforecast.domain.repository.AuthRepository
import com.example.aquaforecast.domain.repository.onError
import com.example.aquaforecast.domain.repository.onSuccess
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

private const val TAG = "AuthViewModel"

class AuthViewModel(
    private val authRepository: AuthRepository
) : ViewModel() {

    private val _state = MutableStateFlow(AuthState())
    val state = _state.asStateFlow()

    fun onEmailChanged(email: String) {
        _state.update {
            it.copy(
                email = email.trim(),
                emailError = null
            )
        }
    }

    fun onPasswordChanged(password: String) {
        _state.update {
            it.copy(
                password = password,
                passwordError = null
            )
        }
    }

    fun togglePasswordVisibility() {
        _state.update {
            it.copy(isPasswordVisible = !it.isPasswordVisible)
        }
    }

    fun switchMode() {
        _state.update {
            it.copy(
                isLoginMode = !it.isLoginMode,
                emailError = null,
                passwordError = null,
                error = null
            )
        }
    }

    fun loginWithEmail() {
        val email = _state.value.email
        val password = _state.value.password

        val emailError = validateEmail(email)
        val passwordError = validatePassword(password)

        if (emailError != null || passwordError != null) {
            _state.update {
                it.copy(
                    emailError = emailError,
                    passwordError = passwordError
                )
            }
            return
        }

        viewModelScope.launch {
            _state.update { it.copy(isLoading = true, error = null) }

            authRepository.loginWithEmail(email, password)
                .onSuccess {
                    _state.update {
                        it.copy(
                            isLoading = false,
                            loginSuccess = true
                        )
                    }
                }
                .onError { message ->
                    _state.update {
                        it.copy(
                            isLoading = false,
                            error = message
                        )
                    }
                }
        }
    }

    fun registerWithEmail() {
        val email = _state.value.email
        val password = _state.value.password

        val emailError = validateEmail(email)
        val passwordError = validatePassword(password, isRegistration = true)

        if (emailError != null || passwordError != null) {
            _state.update {
                it.copy(
                    emailError = emailError,
                    passwordError = passwordError
                )
            }
            return
        }

        viewModelScope.launch {
            _state.update { it.copy(isLoading = true, error = null) }

            authRepository.registerWithEmail(email, password)
                .onSuccess {
                    _state.update {
                        it.copy(
                            isLoading = false,
                            loginSuccess = true
                        )
                    }
                }
                .onError { message ->
                    _state.update {
                        it.copy(
                            isLoading = false,
                            error = message
                        )
                    }
                }
        }
    }

    /**
     * Sign in with Google using Context
     */
    fun signInWithGoogle(context: Context) {
        viewModelScope.launch {
            _state.update { it.copy(isLoading = true, error = null) }

            authRepository.signInWithGoogle(context)
                .onSuccess {
                    _state.update {
                        it.copy(
                            isLoading = false,
                            loginSuccess = true
                        )
                    }
                }
                .onError { message ->
                    // Don't show error if user just cancelled
                    if (message != "Sign-in cancelled") {
                        _state.update {
                            it.copy(
                                isLoading = false,
                                error = message
                            )
                        }
                    } else {
                        _state.update {
                            it.copy(isLoading = false)
                        }
                    }
                }
        }
    }

    fun clearError() {
        _state.update { it.copy(error = null) }
    }

    private fun validateEmail(email: String): String? {
        return when {
            email.isEmpty() -> "Email is required"
            !android.util.Patterns.EMAIL_ADDRESS.matcher(email).matches() ->
                "Invalid email format"
            else -> null
        }
    }

    private fun validatePassword(password: String, isRegistration: Boolean = false): String? {
        return when {
            password.isEmpty() -> "Password is required"
            isRegistration && password.length < 6 ->
                "Password must be at least 6 characters"
            else -> null
        }
    }
}

data class AuthState(
    val email: String = "",
    val password: String = "",
    val isLoginMode: Boolean = true,
    val emailError: String? = null,
    val passwordError: String? = null,
    val isPasswordVisible: Boolean = false,
    val isLoading: Boolean = false,
    val error: String? = null,
    val loginSuccess: Boolean = false
)