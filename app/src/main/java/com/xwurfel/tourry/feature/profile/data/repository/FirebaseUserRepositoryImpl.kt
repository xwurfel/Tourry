package com.xwurfel.tourry.feature.profile.data.repository

import android.content.Context
import com.google.android.gms.auth.api.signin.GoogleSignInClient
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.GoogleAuthProvider
import com.google.firebase.auth.userProfileChangeRequest
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
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class FirebaseUserRepositoryImpl @Inject constructor(
    @ApplicationContext private val context: Context,
    private val firebaseAuth: FirebaseAuth,
    private val googleSignInClient: GoogleSignInClient
) : UserRepository {

    private val _userSettings = MutableStateFlow(UserSettings())

    override fun observeCurrentUser(): Flow<User?> = callbackFlow {
        val authStateListener = FirebaseAuth.AuthStateListener { auth ->
            val firebaseUser = auth.currentUser
            val user = firebaseUser?.let { fbUser ->
                User(
                    id = fbUser.uid,
                    name = fbUser.displayName ?: "Anonymous User",
                    email = fbUser.email ?: "",
                    avatarUrl = fbUser.photoUrl?.toString(),
                    stats = null // Will be loaded separately
                )
            }
            trySend(user)
        }

        firebaseAuth.addAuthStateListener(authStateListener)
        awaitClose { firebaseAuth.removeAuthStateListener(authStateListener) }
    }

    override fun observeAuthenticationState(): Flow<Boolean> =
        observeCurrentUser().map { it != null }

    override fun observeUserStats(): Flow<UserStats?> =
        observeCurrentUser().map { user ->
            user?.let {
                // TODO: Load from Firestore or your backend
                UserStats(
                    toursCreated = 0,
                    toursJoined = 0
                )
            }
        }

    override fun observeUserSettings(): Flow<UserSettings> = _userSettings.asStateFlow()

    override suspend fun signInWithGoogle(): DomainResult<User> = result {
        try {
            val signInIntent = googleSignInClient.signInIntent
            throw UnsupportedOperationException("Requires activity context - see AuthViewModel implementation")
        } catch (e: Exception) {
            Timber.e(e, "Google sign-in failed")
            throw e
        }
    }

    override suspend fun signInWithGoogleCredential(idToken: String): DomainResult<User> = result {
        try {
            val credential = GoogleAuthProvider.getCredential(idToken, null)
            val result = firebaseAuth.signInWithCredential(credential).await()

            val firebaseUser = result.user ?: throw Exception("Sign in failed - no user")

            User(
                id = firebaseUser.uid,
                name = firebaseUser.displayName ?: "Anonymous User",
                email = firebaseUser.email ?: "",
                avatarUrl = firebaseUser.photoUrl?.toString(),
                stats = UserStats(toursCreated = 0, toursJoined = 0)
            )
        } catch (e: Exception) {
            Timber.e(e, "Google credential sign-in failed")
            throw e
        }
    }

    override suspend fun signInWithEmailAndPassword(
        email: String,
        password: String
    ): DomainResult<User> =
        result {
            try {
                val result = firebaseAuth.signInWithEmailAndPassword(email, password).await()
                val firebaseUser = result.user ?: throw Exception("Sign in failed - no user")

                User(
                    id = firebaseUser.uid,
                    name = firebaseUser.displayName ?: "User",
                    email = firebaseUser.email ?: "",
                    avatarUrl = firebaseUser.photoUrl?.toString(),
                    stats = UserStats(toursCreated = 0, toursJoined = 0)
                )
            } catch (e: Exception) {
                Timber.e(e, "Email sign-in failed")
                throw e
            }
        }

    override suspend fun createUserWithEmailAndPassword(
        email: String,
        password: String,
        name: String
    ): DomainResult<User> = result {
        try {
            val result = firebaseAuth.createUserWithEmailAndPassword(email, password).await()
            val firebaseUser = result.user ?: throw Exception("Account creation failed - no user")

            // Update the user's display name
            val profileUpdates = userProfileChangeRequest {
                displayName = name
            }
            firebaseUser.updateProfile(profileUpdates).await()

            User(
                id = firebaseUser.uid,
                name = name,
                email = email,
                avatarUrl = null,
                stats = UserStats(toursCreated = 0, toursJoined = 0)
            )
        } catch (e: Exception) {
            Timber.e(e, "Email account creation failed")
            throw e
        }
    }

    override suspend fun signOut(): DomainResult<Unit> = result {
        try {
            firebaseAuth.signOut()
            googleSignInClient.signOut().await()
            _userSettings.value = UserSettings()
        } catch (e: Exception) {
            Timber.e(e, "Sign out failed")
            throw e
        }
    }

    override suspend fun updateUserSettings(settings: UserSettings): DomainResult<Unit> = result {
        _userSettings.value = settings
        // TODO: Save to Firestore or your backend
    }

    override suspend fun sendPasswordResetEmail(email: String): DomainResult<Unit> = result {
        try {
            firebaseAuth.sendPasswordResetEmail(email).await()
        } catch (e: Exception) {
            Timber.e(e, "Password reset email failed")
            throw e
        }
    }
}