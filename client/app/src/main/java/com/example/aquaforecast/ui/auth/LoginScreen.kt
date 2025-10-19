package com.example.aquaforecast.ui.auth


import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Email
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.material.icons.filled.Visibility
import androidx.compose.material.icons.filled.VisibilityOff
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.input.VisualTransformation
import androidx.compose.ui.unit.dp
import org.koin.androidx.compose.koinViewModel
import org.koin.compose.koinInject

private const val TAG = "LoginScreen"


/**
 * Login/Register screen with Firebase Authentication
 * Uses Credential Manager for Google Sign-In
 */
@Composable
fun LoginScreen(
    onLoginSuccess: () -> Unit,
) {

    val authViewModel: AuthViewModel = koinViewModel()
    val state = authViewModel.state.collectAsState()
    val focusManager = LocalFocusManager.current
    val context = LocalContext.current

    /**
     * Launch Google Sign-In using Credential Manager
     */
    fun launchGoogleSignIn() {
            authViewModel.signInWithGoogle(context)
    }

    // Navigate on success
    LaunchedEffect(state.value.loginSuccess) {
        if (state.value.loginSuccess) {
            onLoginSuccess()
        }
    }

    Scaffold { paddingValues ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
                .verticalScroll(rememberScrollState())
                .padding(24.dp),
            contentAlignment = Alignment.Center
        ) {
            Column(
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.spacedBy(20.dp),
                modifier = Modifier.fillMaxWidth()
            ) {
                // Header
                Column(
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Text(
                        text = "🐟",
                        style = MaterialTheme.typography.displayMedium
                    )

                    Text(
                        text = if (state.value.isLoginMode) "Welcome Back" else "Create Account",
                        style = MaterialTheme.typography.headlineMedium,
                        fontWeight = FontWeight.Bold
                    )

                    Text(
                        text = if (state.value.isLoginMode)
                            "Sign in to continue"
                        else
                            "Register to get started",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }

                Spacer(modifier = Modifier.height(8.dp))

                // Email input
                OutlinedTextField(
                    value = state.value.email,
                    onValueChange = authViewModel::onEmailChanged,
                    label = { Text("Email") },
                    placeholder = { Text("your.email@example.com") },
                    leadingIcon = {
                        Icon(
                            imageVector = Icons.Default.Email,
                            contentDescription = null
                        )
                    },
                    singleLine = true,
                    isError = state.value.emailError != null,
                    supportingText = state.value.emailError?.let {
                        { Text(it) }
                    },
                    keyboardOptions = KeyboardOptions(
                        keyboardType = KeyboardType.Email,
                        imeAction = ImeAction.Next
                    ),
                    enabled = !state.value.isLoading,
                    modifier = Modifier.fillMaxWidth()
                )

                // Password input
                OutlinedTextField(
                    value = state.value.password,
                    onValueChange = authViewModel::onPasswordChanged,
                    label = { Text("Password") },
                    placeholder = { Text("Enter your password") },
                    leadingIcon = {
                        Icon(
                            imageVector = Icons.Default.Lock,
                            contentDescription = null
                        )
                    },
                    trailingIcon = {
                        IconButton(onClick = authViewModel::togglePasswordVisibility) {
                            Icon(
                                imageVector = if (state.value.isPasswordVisible)
                                    Icons.Default.Visibility
                                else
                                    Icons.Default.VisibilityOff,
                                contentDescription = if (state.value.isPasswordVisible)
                                    "Hide password"
                                else
                                    "Show password"
                            )
                        }
                    },
                    visualTransformation = if (state.value.isPasswordVisible)
                        VisualTransformation.None
                    else
                        PasswordVisualTransformation(),
                    singleLine = true,
                    isError = state.value.passwordError != null,
                    supportingText = state.value.passwordError?.let {
                        { Text(it) }
                    },
                    keyboardOptions = KeyboardOptions(
                        keyboardType = KeyboardType.Password,
                        imeAction = ImeAction.Done
                    ),
                    keyboardActions = KeyboardActions(
                        onDone = {
                            focusManager.clearFocus()
                            if (state.value.isLoginMode) {
                                authViewModel.loginWithEmail()
                            } else {
                                authViewModel.registerWithEmail()
                            }
                        }
                    ),
                    enabled = !state.value.isLoading,
                    modifier = Modifier.fillMaxWidth()
                )

                // Login/Register button
                Button(
                    onClick = {
                        focusManager.clearFocus()
                        if (state.value.isLoginMode) {
                            authViewModel.loginWithEmail()
                        } else {
                            authViewModel.registerWithEmail()
                        }
                    },
                    enabled = !state.value.isLoading,
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(56.dp)
                ) {
                    if (state.value.isLoading) {
                        CircularProgressIndicator(
                            modifier = Modifier.size(24.dp),
                            color = MaterialTheme.colorScheme.onPrimary
                        )
                    } else {
                        Text(
                            text = if (state.value.isLoginMode) "Sign In" else "Register",
                            style = MaterialTheme.typography.titleMedium
                        )
                    }
                }

                // Switch mode button
                TextButton(
                    onClick = authViewModel::switchMode,
                    enabled = !state.value.isLoading
                ) {
                    Text(
                        text = if (state.value.isLoginMode)
                            "Don't have an account? Register"
                        else
                            "Already have an account? Sign In"
                    )
                }

                // Divider
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    HorizontalDivider(modifier = Modifier.weight(1f))
                    Text(
                        text = "OR",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    HorizontalDivider(modifier = Modifier.weight(1f))
                }

                // Google Sign-In button with Credential Manager
                OutlinedButton(
                    onClick = { launchGoogleSignIn() },
                    enabled = !state.value.isLoading,
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(56.dp)
                ) {
                    Row(
                        horizontalArrangement = Arrangement.spacedBy(12.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        // Google icon (ideally use proper Google logo drawable)
                        Text(
                            text = "G",
                            style = MaterialTheme.typography.titleLarge,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.primary
                        )
                        Text(
                            text = "Continue with Google",
                            style = MaterialTheme.typography.titleMedium
                        )
                    }
                }

                // Error message
                if (state.value.error != null) {
                    Card(
                        colors = CardDefaults.cardColors(
                            containerColor = MaterialTheme.colorScheme.errorContainer
                        ),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Text(
                            text = state.value.error ?: "",
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onErrorContainer,
                            modifier = Modifier.padding(16.dp)
                        )
                    }
                }
            }
        }
    }
}