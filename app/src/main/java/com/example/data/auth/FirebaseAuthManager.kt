package com.example.data.auth

import android.app.Activity
import android.content.Context
import android.util.Log
import androidx.credentials.CredentialManager
import androidx.credentials.CustomCredential
import androidx.credentials.GetCredentialRequest
import androidx.credentials.exceptions.GetCredentialCancellationException
import com.example.data.model.UserProfile
import com.google.android.libraries.identity.googleid.GetGoogleIdOption
import com.google.android.libraries.identity.googleid.GoogleIdTokenCredential
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.FirebaseUser
import com.google.firebase.auth.GoogleAuthProvider
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.callbackFlow
import kotlinx.coroutines.tasks.await
import kotlinx.coroutines.withContext

class FirebaseAuthManager(private val context: Context) {

    private val auth: FirebaseAuth by lazy {
        FirebaseAuth.getInstance()
    }

    private val credentialManager: CredentialManager by lazy {
        CredentialManager.create(context)
    }

    companion object {
        private const val TAG = "FirebaseAuthManager"
        // OAuth Client ID configured in Firebase project intrepid-carving-69v0l
        const val SERVER_CLIENT_ID = "1052884931327-vkn34c745kgnj696auqloaibme7s6ncn.apps.googleusercontent.com"
    }

    val authStateFlow: Flow<UserProfile?> = callbackFlow {
        val listener = FirebaseAuth.AuthStateListener { firebaseAuth ->
            val user = firebaseAuth.currentUser
            trySend(user?.toUserProfile())
        }
        auth.addAuthStateListener(listener)
        // Initial state
        trySend(auth.currentUser?.toUserProfile())

        awaitClose {
            auth.removeAuthStateListener(listener)
        }
    }

    fun getCurrentUser(): UserProfile? {
        return auth.currentUser?.toUserProfile()
    }

    suspend fun signInWithGoogle(activity: Activity): Result<UserProfile> = withContext(Dispatchers.IO) {
        try {
            val googleIdOption = GetGoogleIdOption.Builder()
                .setFilterByAuthorizedAccounts(false)
                .setServerClientId(SERVER_CLIENT_ID)
                .setAutoSelectEnabled(false)
                .build()

            val request = GetCredentialRequest.Builder()
                .addCredentialOption(googleIdOption)
                .build()

            val response = credentialManager.getCredential(
                context = activity,
                request = request
            )

            val credential = response.credential
            if (credential is CustomCredential && credential.type == GoogleIdTokenCredential.TYPE_GOOGLE_ID_TOKEN_CREDENTIAL) {
                val googleIdTokenCredential = GoogleIdTokenCredential.createFrom(credential.data)
                val idToken = googleIdTokenCredential.idToken

                val authCredential = GoogleAuthProvider.getCredential(idToken, null)
                val authResult = auth.signInWithCredential(authCredential).await()
                val firebaseUser = authResult.user

                if (firebaseUser != null) {
                    Result.success(firebaseUser.toUserProfile())
                } else {
                    Result.failure(Exception("Não foi possível autenticar o usuário com o Firebase."))
                }
            } else {
                Result.failure(Exception("Credencial do Google inesperada ou não suportada."))
            }
        } catch (e: GetCredentialCancellationException) {
            Log.d(TAG, "Login cancelado pelo usuário")
            Result.failure(Exception("Login cancelado pelo usuário."))
        } catch (e: Exception) {
            Log.e(TAG, "Falha no login com Google: ${e.message}", e)
            Result.failure(Exception(e.localizedMessage ?: "Erro ao realizar login com o Google."))
        }
    }

    suspend fun signOut() = withContext(Dispatchers.IO) {
        try {
            auth.signOut()
            try {
                credentialManager.clearCredentialState(androidx.credentials.ClearCredentialStateRequest())
            } catch (e: Exception) {
                Log.w(TAG, "Erro ao limpar CredentialManager: ${e.message}")
            }
        } catch (e: Exception) {
            Log.e(TAG, "Erro ao fazer logout: ${e.message}", e)
        }
    }

    private fun FirebaseUser.toUserProfile(): UserProfile {
        return UserProfile(
            uid = uid,
            displayName = displayName ?: email?.substringBefore('@') ?: "Usuário SST",
            email = email,
            photoUrl = photoUrl?.toString(),
            isAnonymous = isAnonymous
        )
    }
}
