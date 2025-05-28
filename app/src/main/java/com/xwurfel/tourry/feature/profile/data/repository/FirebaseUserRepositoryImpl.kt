package com.xwurfel.tourry.feature.profile.data.repository

import android.content.Context
import com.google.android.gms.auth.api.signin.GoogleSignInClient
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.FirebaseAuthInvalidCredentialsException
import com.google.firebase.auth.FirebaseAuthInvalidUserException
import com.google.firebase.auth.FirebaseAuthUserCollisionException
import com.google.firebase.auth.GoogleAuthProvider
import com.google.firebase.auth.userProfileChangeRequest
import com.google.firebase.firestore.FirebaseFirestore
import com.xwurfel.tourry.R
import com.xwurfel.tourry.core.domain.util.DomainResult
import com.xwurfel.tourry.core.domain.util.result
import com.xwurfel.tourry.feature.profile.domain.model.User
import com.xwurfel.tourry.feature.profile.domain.model.UserSettings
import com.xwurfel.tourry.feature.profile.domain.model.UserStats
import com.xwurfel.tourry.feature.profile.domain.repository.UserRepository
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.callbackFlow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.tasks.await
import timber.log.Timber
import java.net.UnknownHostException
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class FirebaseUserRepositoryImpl @Inject constructor(
    @ApplicationContext private val context: Context,
    private val firebaseAuth: FirebaseAuth,
    private val firestore: FirebaseFirestore,
    private val googleSignInClient: GoogleSignInClient
) : UserRepository {

    private val _userSettings = MutableStateFlow(UserSettings())
    private val _userStats = MutableStateFlow<UserStats?>(null)

    override fun observeCurrentUser(): Flow<User?> = callbackFlow {
        val authStateListener = FirebaseAuth.AuthStateListener { auth ->
            val firebaseUser = auth.currentUser
            val user = firebaseUser?.let { fbUser ->
                User(
                    id = fbUser.uid,
                    name = fbUser.displayName ?: "User",
                    email = fbUser.email ?: "",
                    avatarUrl = fbUser.photoUrl?.toString(),
                    stats = _userStats.value
                )
            }
            trySend(user)
        }

        firebaseAuth.addAuthStateListener(authStateListener)

        firebaseAuth.currentUser?.let { user ->
            loadUserStats(user.uid)
        }

        awaitClose {
            firebaseAuth.removeAuthStateListener(authStateListener)
        }
    }

    override fun observeAuthenticationState(): Flow<Boolean> =
        observeCurrentUser().map { it != null }

    override fun observeUserStats(): Flow<UserStats?> = _userStats.asStateFlow()

    override fun observeUserSettings(): Flow<UserSettings> = _userSettings.asStateFlow()

    override suspend fun signInWithGoogle(): DomainResult<User> = result {
        // This method is not directly used since we handle Google Sign-In through the activity result
        // But we can provide the sign-in intent here if needed
        throw UnsupportedOperationException(
            "Use signInWithGoogleCredential after getting the ID token from Google Sign-In activity result"
        )
    }

    override suspend fun signInWithGoogleCredential(idToken: String): DomainResult<User> = result {
        try {
            val credential = GoogleAuthProvider.getCredential(idToken, null)
            val authResult = firebaseAuth.signInWithCredential(credential).await()

            val firebaseUser = authResult.user
                ?: throw Exception(context.getString(R.string.auth_sign_in_failed))

            // Load or create user stats
            loadUserStats(firebaseUser.uid)

            User(
                id = firebaseUser.uid,
                name = firebaseUser.displayName ?: "User",
                email = firebaseUser.email ?: "",
                avatarUrl = firebaseUser.photoUrl?.toString(),
                stats = _userStats.value
            )
        } catch (e: Exception) {
            Timber.e(e, "Google credential sign-in failed")
            throw mapAuthException(e)
        }
    }

    override suspend fun signInWithEmailAndPassword(
        email: String,
        password: String
    ): DomainResult<User> = result {
        try {
            val authResult = firebaseAuth.signInWithEmailAndPassword(email, password).await()
            val firebaseUser = authResult.user
                ?: throw Exception(context.getString(R.string.auth_sign_in_failed))

            // Load user stats
            loadUserStats(firebaseUser.uid)

            User(
                id = firebaseUser.uid,
                name = firebaseUser.displayName ?: "User",
                email = firebaseUser.email ?: "",
                avatarUrl = firebaseUser.photoUrl?.toString(),
                stats = _userStats.value
            )
        } catch (e: Exception) {
            Timber.e(e, "Email sign-in failed")
            throw mapAuthException(e)
        }
    }

    override suspend fun createUserWithEmailAndPassword(
        email: String,
        password: String,
        name: String
    ): DomainResult<User> = result {
        try {
            val authResult = firebaseAuth.createUserWithEmailAndPassword(email, password).await()
            val firebaseUser = authResult.user
                ?: throw Exception(context.getString(R.string.auth_account_creation_failed))

            // Update the user's display name
            val profileUpdates = userProfileChangeRequest {
                displayName = name
            }
            firebaseUser.updateProfile(profileUpdates).await()

            // Initialize user stats
            val initialStats = UserStats(toursCreated = 0, toursJoined = 0)
            _userStats.value = initialStats

            // TODO: Save initial stats to Firestore
            saveUserStats(firebaseUser.uid, initialStats)

            User(
                id = firebaseUser.uid,
                name = name,
                email = email,
                avatarUrl = null,
                stats = initialStats
            )
        } catch (e: Exception) {
            Timber.e(e, "Email account creation failed")
            throw mapAuthException(e)
        }
    }

    override suspend fun sendPasswordResetEmail(email: String): DomainResult<Unit> = result {
        try {
            firebaseAuth.sendPasswordResetEmail(email).await()
        } catch (e: Exception) {
            Timber.e(e, "Password reset email failed")
            throw mapAuthException(e)
        }
    }

    override suspend fun signOut(): DomainResult<Unit> = result {
        try {
            // Sign out from Firebase
            firebaseAuth.signOut()

            // Sign out from Google
            googleSignInClient.signOut().await()

            // Clear local data
            _userSettings.value = UserSettings()
            _userStats.value = null

        } catch (e: Exception) {
            Timber.e(e, "Sign out failed")
            throw Exception(context.getString(R.string.auth_sign_in_failed))
        }
    }

    override suspend fun updateUserSettings(settings: UserSettings): DomainResult<Unit> = result {
        try {
            _userSettings.value = settings

            // TODO: Save to Firestore
            firebaseAuth.currentUser?.let { user ->
                saveUserSettings(user.uid, settings)
            }
        } catch (e: Exception) {
            Timber.e(e, "Failed to update user settings")
            throw Exception("Failed to update settings")
        }
    }

    private fun loadUserStats(userId: String) {
        // TODO: Load from Firestore
        // For now, use mock data
        _userStats.value = UserStats(
            toursCreated = 0,
            toursJoined = 0
        )
    }

    private suspend fun saveUserStats(userId: String, stats: UserStats) {
        // TODO: Implement Firestore saving
        Timber.d("Saving user stats for $userId: $stats")
    }

    private suspend fun saveUserSettings(userId: String, settings: UserSettings) {
        // TODO: Implement Firestore saving
        Timber.d("Saving user settings for $userId: $settings")
    }

    private fun mapAuthException(exception: Throwable): Exception {
        return when (exception) {
            is FirebaseAuthInvalidCredentialsException -> {
                Exception(context.getString(R.string.auth_invalid_credentials))
            }

            is FirebaseAuthInvalidUserException -> {
                when (exception.errorCode) {
                    "ERROR_USER_DISABLED" -> Exception(context.getString(R.string.auth_user_disabled))
                    else -> Exception(context.getString(R.string.auth_invalid_credentials))
                }
            }

            is FirebaseAuthUserCollisionException -> {
                Exception(context.getString(R.string.auth_email_already_in_use))
            }

            is UnknownHostException -> {
                Exception(context.getString(R.string.auth_network_error))
            }

            else -> {
                Exception(exception.message ?: context.getString(R.string.auth_sign_in_failed))
            }
        }
    }
}