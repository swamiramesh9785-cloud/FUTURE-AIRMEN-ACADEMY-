package com.example.data.repository

import android.app.Activity
import android.content.Context
import android.util.Log
import androidx.credentials.ClearCredentialStateRequest
import androidx.credentials.CredentialManager
import androidx.credentials.CustomCredential
import androidx.credentials.GetCredentialRequest
import androidx.credentials.exceptions.GetCredentialCancellationException
import com.example.R
import com.example.data.model.UserProfile
import com.google.android.libraries.identity.googleid.GetGoogleIdOption
import com.google.android.libraries.identity.googleid.GetSignInWithGoogleOption
import com.google.android.libraries.identity.googleid.GoogleIdTokenCredential
import com.google.android.libraries.identity.googleid.GoogleIdTokenCredential.Companion.TYPE_GOOGLE_ID_TOKEN_CREDENTIAL
import com.google.firebase.Firebase
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.FirebaseUser
import com.google.firebase.auth.GoogleAuthProvider
import com.google.firebase.auth.auth
import com.google.firebase.firestore.FieldValue
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.SetOptions
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.NonCancellable
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.tasks.await
import kotlinx.coroutines.withContext

private const val TAG = "AuthRepository"
const val ADMIN_EMAIL = "swamiramesh9785@gmail.com"

class AuthRepository(private val context: Context) {
    private val auth: FirebaseAuth = Firebase.auth
    private val db: FirebaseFirestore = FirebaseFirestore.getInstance(
        context.applicationContext.getString(R.string.firestore_database_id)
    )
    private val credentialManager: CredentialManager = CredentialManager.create(context)

    private val _currentUser = MutableStateFlow<FirebaseUser?>(auth.currentUser)
    val currentUser: StateFlow<FirebaseUser?> = _currentUser.asStateFlow()

    private val _userProfile = MutableStateFlow<UserProfile?>(null)
    val userProfile: StateFlow<UserProfile?> = _userProfile.asStateFlow()

    private val _authLoading = MutableStateFlow(false)
    val authLoading: StateFlow<Boolean> = _authLoading.asStateFlow()

    private val _authError = MutableStateFlow<String?>(null)
    val authError: StateFlow<String?> = _authError.asStateFlow()

    init {
        auth.addAuthStateListener { firebaseAuth ->
            val user = firebaseAuth.currentUser
            _currentUser.value = user
            if (user != null) {
                CoroutineScope(Dispatchers.IO).launch {
                    loadOrCreateProfile(user)
                }
            } else {
                _userProfile.value = null
            }
        }
        if (auth.currentUser != null) {
            CoroutineScope(Dispatchers.IO).launch {
                loadOrCreateProfile(auth.currentUser!!)
            }
        }
    }

    private suspend fun loadOrCreateProfile(user: FirebaseUser) {
        try {
            val userDocRef = db.collection("users").document(user.uid)
            val snapshot = userDocRef.get().await()

            val isAdminUser = user.email?.trim()?.equals(ADMIN_EMAIL, ignoreCase = true) == true
            val defaultRole = if (isAdminUser) "admin" else "student"

            if (snapshot.exists()) {
                val profile = snapshot.toObject(UserProfile::class.java)
                if (profile != null) {
                    // Sync admin role if necessary
                    if (isAdminUser && profile.role != "admin") {
                        userDocRef.update("role", "admin", "updatedAt", FieldValue.serverTimestamp()).await()
                        _userProfile.value = profile.copy(role = "admin")
                    } else {
                        _userProfile.value = profile
                    }
                }
            } else {
                val newProfile = UserProfile(
                    userId = user.uid,
                    name = user.displayName ?: "Aspirant",
                    email = user.email ?: "",
                    photoUrl = user.photoUrl?.toString(),
                    role = defaultRole,
                    testsAttempted = 0,
                    bestScore = 0.0
                )
                val payload = mutableMapOf<String, Any>(
                    "userId" to newProfile.userId,
                    "name" to newProfile.name,
                    "email" to newProfile.email,
                    "role" to newProfile.role,
                    "testsAttempted" to newProfile.testsAttempted,
                    "bestScore" to newProfile.bestScore,
                    "createdAt" to FieldValue.serverTimestamp(),
                    "updatedAt" to FieldValue.serverTimestamp()
                )
                if (newProfile.photoUrl != null) {
                    payload["photoUrl"] = newProfile.photoUrl
                }

                userDocRef.set(payload, SetOptions.merge()).await()
                _userProfile.value = newProfile
            }
        } catch (e: Exception) {
            Log.e(TAG, "Error syncing user profile: ${e.message}", e)
        }
    }

    fun signInWithGoogle(
        activity: Activity,
        scope: CoroutineScope,
        onSuccess: () -> Unit = {},
        onError: (String) -> Unit = {}
    ) {
        val clientId = try {
            context.getString(R.string.default_web_client_id)
        } catch (e: Exception) {
            val msg = "Google Client ID configuration missing"
            _authError.value = msg
            onError(msg)
            return
        }

        _authLoading.value = true
        _authError.value = null

        val signInOption = GetSignInWithGoogleOption.Builder(serverClientId = clientId).build()
        val request = GetCredentialRequest.Builder().addCredentialOption(signInOption).build()

        scope.launch {
            try {
                val result = credentialManager.getCredential(activity, request)
                val credential = result.credential
                if (credential is CustomCredential && credential.type == TYPE_GOOGLE_ID_TOKEN_CREDENTIAL) {
                    val googleIdToken = GoogleIdTokenCredential.createFrom(credential.data).idToken
                    val authCredential = GoogleAuthProvider.getCredential(googleIdToken, null)
                    val authResult = auth.signInWithCredential(authCredential).await()
                    val user = authResult.user
                    if (user != null) {
                        loadOrCreateProfile(user)
                    }
                    _authLoading.value = false
                    onSuccess()
                } else {
                    val msg = "Unexpected credential format received"
                    _authError.value = msg
                    _authLoading.value = false
                    onError(msg)
                }
            } catch (e: GetCredentialCancellationException) {
                Log.w(TAG, "Google Sign-In cancelled or dismissed: ${e.message}", e)
                _authLoading.value = false
            } catch (e: Exception) {
                Log.e(TAG, "Google Sign-In failed", e)
                val msg = e.localizedMessage ?: "Sign-in failed. Please try again."
                _authError.value = msg
                _authLoading.value = false
                onError(msg)
            }
        }
    }

    fun silentAutoSignIn(scope: CoroutineScope) {
        if (auth.currentUser != null) return

        val clientId = try {
            context.getString(R.string.default_web_client_id)
        } catch (e: Exception) {
            return
        }

        val googleIdOption = GetGoogleIdOption.Builder()
            .setFilterByAuthorizedAccounts(true)
            .setServerClientId(clientId)
            .setAutoSelectEnabled(true)
            .build()

        val request = GetCredentialRequest.Builder().addCredentialOption(googleIdOption).build()

        scope.launch {
            try {
                val result = credentialManager.getCredential(context, request)
                val credential = result.credential
                if (credential is CustomCredential && credential.type == TYPE_GOOGLE_ID_TOKEN_CREDENTIAL) {
                    val googleIdToken = GoogleIdTokenCredential.createFrom(credential.data).idToken
                    val authCredential = GoogleAuthProvider.getCredential(googleIdToken, null)
                    val authResult = auth.signInWithCredential(authCredential).await()
                    authResult.user?.let { loadOrCreateProfile(it) }
                }
            } catch (e: Exception) {
                Log.d(TAG, "Silent sign in not available: ${e.message}")
            }
        }
    }

    fun signOut(scope: CoroutineScope, onComplete: () -> Unit = {}) {
        scope.launch {
            withContext(NonCancellable) {
                try {
                    credentialManager.clearCredentialState(ClearCredentialStateRequest())
                } catch (e: Exception) {
                    Log.w(TAG, "Failed clearing credential state: ${e.message}")
                } finally {
                    auth.signOut()
                    _currentUser.value = null
                    _userProfile.value = null
                    onComplete()
                }
            }
        }
    }

    fun clearError() {
        _authError.value = null
    }
}
