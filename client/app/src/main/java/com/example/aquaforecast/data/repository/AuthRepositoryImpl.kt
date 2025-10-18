package com.example.aquaforecast.data.repository

import android.content.Context
import android.util.Log
import androidx.credentials.Credential
import androidx.credentials.CredentialManager
import androidx.credentials.GetCredentialRequest
import androidx.credentials.exceptions.GetCredentialCancellationException
import androidx.credentials.exceptions.GetCredentialException
import androidx.credentials.exceptions.NoCredentialException
import com.example.aquaforecast.R
import com.example.aquaforecast.domain.model.User
import com.example.aquaforecast.domain.repository.AuthRepository
import com.google.firebase.auth.FirebaseAuth
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.tasks.await
import kotlinx.coroutines.withContext
import com.example.aquaforecast.domain.repository.Result
import com.example.aquaforecast.domain.repository.asError
import com.example.aquaforecast.domain.repository.asSuccess
import com.google.android.libraries.identity.googleid.GetGoogleIdOption
import com.google.android.libraries.identity.googleid.GetSignInWithGoogleOption
import com.google.android.libraries.identity.googleid.GoogleIdTokenCredential
import com.google.android.libraries.identity.googleid.GoogleIdTokenCredential.Companion.TYPE_GOOGLE_ID_TOKEN_CREDENTIAL
import com.google.firebase.auth.FirebaseAuthException
import com.google.firebase.auth.GoogleAuthProvider
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.callbackFlow

private const val TAG = "AuthRepositoryImpl"

/**
 * Implementation of AuthRepository using Firebase Authentication
 */
class AuthRepositoryImpl(
    private val firebaseAuth: FirebaseAuth
) : AuthRepository {

    override suspend fun loginWithEmail(email: String, password: String): Result<User> =
        withContext(Dispatchers.IO) {
            try {
                val result = firebaseAuth.signInWithEmailAndPassword(email, password).await()
                val firebaseUser = result.user

                if (firebaseUser != null) {
                    User(
                        uid = firebaseUser.uid,
                        email = firebaseUser.email,
                        displayName = firebaseUser.displayName,
                        photoUrl = firebaseUser.photoUrl?.toString()
                    ).asSuccess()
                } else {
                    "Login failed: User is null".asError()
                }
            } catch (e: FirebaseAuthException) {
                handleFirebaseAuthException(e).asError()
            } catch (e: Exception) {
                (e.message ?: "Login failed").asError()
            }
        }

    override suspend fun registerWithEmail(email: String, password: String): Result<User> =
        withContext(Dispatchers.IO) {
            try {
                val result = firebaseAuth.createUserWithEmailAndPassword(email, password).await()
                val firebaseUser = result.user

                if (firebaseUser != null) {
                    User(
                        uid = firebaseUser.uid,
                        email = firebaseUser.email,
                        displayName = firebaseUser.displayName,
                        photoUrl = firebaseUser.photoUrl?.toString()
                    ).asSuccess()
                } else {
                    "Registration failed: User is null".asError()
                }
            } catch (e: FirebaseAuthException) {
                handleFirebaseAuthException(e).asError()
            } catch (e: Exception) {
                (e.message ?: "Registration failed").asError()
            }
        }

    override suspend fun signInWithGoogle(context: Context): Result<User> =
        withContext(Dispatchers.IO) {
            try {
                Log.d(TAG, "Starting Google Sign-In")
                val credentialManager = CredentialManager.create(context)

                val signInWithGoogleOption = GetSignInWithGoogleOption.Builder(
                    serverClientId = context.getString(R.string.default_web_client_id)
                ).build()

                val request = GetCredentialRequest.Builder()
                    .addCredentialOption(signInWithGoogleOption)
                    .build()

                try {
                    Log.d(TAG, "Requesting credentials")
                    val result = credentialManager.getCredential(
                        context = context,
                        request = request
                    )

                    Log.d(TAG, "Credentials received, handling sign-in")
                    handleSignIn(result.credential)
                } catch (e: GetCredentialCancellationException) {
                    // User cancelled
                    Log.d(TAG, "User cancelled sign-in")
                    "Sign-in cancelled".asError()
                } catch (e: GetCredentialException) {
                    Log.e(TAG, "Credential error: ${e.errorMessage}", e)
                    (e.localizedMessage ?: "Failed to get credentials").asError()
                }
            } catch (e: Exception) {
                Log.e(TAG, "Unexpected error during Google Sign-In", e)
                (e.message ?: "Google sign-in failed").asError()
            }
        }

    /**
     * Handle the Google credential and sign in to Firebase
     */
    private suspend fun handleSignIn(credential: Credential): Result<User> =
        withContext(Dispatchers.IO) {
            try {
                Log.d(TAG, "Processing credential type: ${credential.type}")

                // Verify credential type
                if (credential.type != TYPE_GOOGLE_ID_TOKEN_CREDENTIAL) {
                    Log.e(TAG, "Unknown credential type: ${credential.type}")
                    return@withContext "Unknown credential type".asError()
                }

                // Extract Google ID token
                val googleIdTokenCredential = GoogleIdTokenCredential.createFrom(credential.data)
                val idToken = googleIdTokenCredential.idToken

                Log.d(TAG, "Got ID token, signing in to Firebase")

                // Create Firebase credential from Google token
                val firebaseCredential = GoogleAuthProvider.getCredential(idToken, null)

                // Sign in to Firebase
                val result = firebaseAuth.signInWithCredential(firebaseCredential).await()
                val firebaseUser = result.user

                if (firebaseUser != null) {
                    Log.d(TAG, "Firebase sign-in successful: ${firebaseUser.uid}")
                    User(
                        uid = firebaseUser.uid,
                        email = firebaseUser.email,
                        displayName = firebaseUser.displayName,
                        photoUrl = firebaseUser.photoUrl?.toString()
                    ).asSuccess()
                } else {
                    Log.e(TAG, "Firebase user is null after sign-in")
                    "Google sign-in failed: User is null".asError()
                }
            } catch (e: FirebaseAuthException) {
                Log.e(TAG, "Firebase auth error: ${e.errorCode}", e)
                handleFirebaseAuthException(e).asError()
            } catch (e: Exception) {
                Log.e(TAG, "Error handling credential", e)
                (e.message ?: "Google sign-in failed").asError()
            }
        }

    override suspend fun getCurrentUser(): Result<User?> = withContext(Dispatchers.IO) {
        try {
            val firebaseUser = firebaseAuth.currentUser

            val user = firebaseUser?.let {
                User(
                    uid = it.uid,
                    email = it.email,
                    displayName = it.displayName,
                    photoUrl = it.photoUrl?.toString()
                )
            }

            user.asSuccess()
        } catch (e: Exception) {
            (e.message ?: "Failed to get current user").asError()
        }
    }

    override fun observeAuthState(): Flow<User?> = callbackFlow {
        val authStateListener = FirebaseAuth.AuthStateListener { auth ->
            val firebaseUser = auth.currentUser
            val user = firebaseUser?.let {
                User(
                    uid = it.uid,
                    email = it.email,
                    displayName = it.displayName,
                    photoUrl = it.photoUrl?.toString()
                )
            }
            trySend(user)
        }

        firebaseAuth.addAuthStateListener(authStateListener)

        awaitClose {
            firebaseAuth.removeAuthStateListener(authStateListener)
        }
    }

    override suspend fun isLoggedIn(): Result<Boolean> = withContext(Dispatchers.IO) {
        try {
            (firebaseAuth.currentUser != null).asSuccess()
        } catch (e: Exception) {
            (e.message ?: "Failed to check login status").asError()
        }
    }

    override suspend fun logout(): Result<Unit> = withContext(Dispatchers.IO) {
        try {
            firebaseAuth.signOut()
            Unit.asSuccess()
        } catch (e: Exception) {
            (e.message ?: "Logout failed").asError()
        }
    }

    override suspend fun sendPasswordResetEmail(email: String): Result<Unit> =
        withContext(Dispatchers.IO) {
            try {
                firebaseAuth.sendPasswordResetEmail(email).await()
                Unit.asSuccess()
            } catch (e: FirebaseAuthException) {
                handleFirebaseAuthException(e).asError()
            } catch (e: Exception) {
                (e.message ?: "Failed to send password reset email").asError()
            }
        }

    /**
     * Handle Firebase Auth exceptions and return user-friendly messages
     */
    private fun handleFirebaseAuthException(exception: FirebaseAuthException): String {
        return when (exception.errorCode) {
            "ERROR_INVALID_EMAIL" -> "Invalid email address"
            "ERROR_WRONG_PASSWORD" -> "Incorrect password"
            "ERROR_USER_NOT_FOUND" -> "No account found with this email"
            "ERROR_USER_DISABLED" -> "This account has been disabled"
            "ERROR_TOO_MANY_REQUESTS" -> "Too many attempts. Please try again later"
            "ERROR_EMAIL_ALREADY_IN_USE" -> "Email is already registered"
            "ERROR_WEAK_PASSWORD" -> "Password is too weak"
            "ERROR_INVALID_CREDENTIAL" -> "Invalid credentials"
            else -> exception.message ?: "Authentication failed"
        }
    }
}