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
import com.google.firebase.firestore.toObject
import com.xwurfel.tourry.R
import com.xwurfel.tourry.core.domain.util.DomainResult
import com.xwurfel.tourry.core.domain.util.result
import com.xwurfel.tourry.feature.profile.data.model.FirestoreUser
import com.xwurfel.tourry.feature.profile.data.model.FirestoreUserSettings
import com.xwurfel.tourry.feature.profile.data.model.FirestoreUserStats
import com.xwurfel.tourry.feature.profile.domain.model.User
import com.xwurfel.tourry.feature.profile.domain.model.UserSettings
import com.xwurfel.tourry.feature.profile.domain.model.UserStats
import com.xwurfel.tourry.feature.profile.domain.repository.UserRepository
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.Flow
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

    companion object {
        private const val USERS_COLLECTION = "users"
        private const val USER_STATS_COLLECTION = "user_stats"
        private const val USER_SETTINGS_COLLECTION = "user_settings"
    }

    override fun observeCurrentUser(): Flow<User?> = callbackFlow {
        val authStateListener = FirebaseAuth.AuthStateListener { auth ->
            val firebaseUser = auth.currentUser

            if (firebaseUser != null) {
                // Load user profile from Firestore
                firestore.collection(USERS_COLLECTION)
                    .document(firebaseUser.uid)
                    .addSnapshotListener { snapshot, error ->
                        if (error != null) {
                            Timber.e(error, "Error observing user profile")
                            trySend(null)
                            return@addSnapshotListener
                        }

                        val firestoreUser = snapshot?.toObject<FirestoreUser>()
                        val user = if (firestoreUser != null) {
                            User(
                                id = firestoreUser.id,
                                name = firestoreUser.name,
                                email = firestoreUser.email,
                                avatarUrl = firestoreUser.avatarUrl
                            )
                        } else {
                            // Create basic user from Firebase Auth if not in Firestore
                            User(
                                id = firebaseUser.uid,
                                name = firebaseUser.displayName ?: "User",
                                email = firebaseUser.email ?: "",
                                avatarUrl = firebaseUser.photoUrl?.toString()
                            )
                        }

                        trySend(user)
                    }
            } else {
                trySend(null)
            }
        }

        firebaseAuth.addAuthStateListener(authStateListener)
        awaitClose { firebaseAuth.removeAuthStateListener(authStateListener) }
    }

    override fun observeAuthenticationState(): Flow<Boolean> =
        observeCurrentUser().map { it != null }

    override fun observeUserStats(): Flow<UserStats?> = callbackFlow {
        val authStateListener = FirebaseAuth.AuthStateListener { auth ->
            val firebaseUser = auth.currentUser

            if (firebaseUser != null) {
                firestore.collection(USER_STATS_COLLECTION)
                    .document(firebaseUser.uid)
                    .addSnapshotListener { snapshot, error ->
                        if (error != null) {
                            Timber.e(error, "Error observing user stats")
                            trySend(null)
                            return@addSnapshotListener
                        }

                        val firestoreStats = snapshot?.toObject<FirestoreUserStats>()
                        val stats = firestoreStats?.let {
                            UserStats(
                                toursCreated = it.toursCreated,
                                toursJoined = it.toursJoined
                            )
                        }

                        trySend(stats)
                    }
            } else {
                trySend(null)
            }
        }

        firebaseAuth.addAuthStateListener(authStateListener)
        awaitClose { firebaseAuth.removeAuthStateListener(authStateListener) }
    }

    override fun observeUserSettings(): Flow<UserSettings> = callbackFlow {
        val authStateListener = FirebaseAuth.AuthStateListener { auth ->
            val firebaseUser = auth.currentUser

            if (firebaseUser != null) {
                firestore.collection(USER_SETTINGS_COLLECTION)
                    .document(firebaseUser.uid)
                    .addSnapshotListener { snapshot, error ->
                        if (error != null) {
                            Timber.e(error, "Error observing user settings")
                            trySend(UserSettings()) // Default settings
                            return@addSnapshotListener
                        }

                        val firestoreSettings = snapshot?.toObject<FirestoreUserSettings>()
                        val settings = if (firestoreSettings != null) {
                            UserSettings(
                                notificationsEnabled = firestoreSettings.notificationsEnabled,
                                locationPermissionGranted = firestoreSettings.locationPermissionGranted
                            )
                        } else {
                            UserSettings() // Default settings
                        }

                        trySend(settings)
                    }
            } else {
                trySend(UserSettings()) // Default settings for guests
            }
        }

        firebaseAuth.addAuthStateListener(authStateListener)
        awaitClose { firebaseAuth.removeAuthStateListener(authStateListener) }
    }

    override suspend fun signInWithGoogle(): DomainResult<User> = result {
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

            // Create or update user in Firestore
            val user = createOrUpdateUserProfile(
                uid = firebaseUser.uid,
                name = firebaseUser.displayName ?: "User",
                email = firebaseUser.email ?: "",
                avatarUrl = firebaseUser.photoUrl?.toString()
            )

            // Initialize user stats and settings if new user
            if (authResult.additionalUserInfo?.isNewUser == true) {
                initializeUserData(firebaseUser.uid)
            }

            user
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

            // Get user from Firestore or create basic profile
            getOrCreateUserProfile(firebaseUser.uid, firebaseUser.displayName, email, null)
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

            // Create user profile in Firestore
            val user = createOrUpdateUserProfile(
                uid = firebaseUser.uid,
                name = name,
                email = email,
                avatarUrl = null
            )

            // Initialize user stats and settings
            initializeUserData(firebaseUser.uid)

            user
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
            firebaseAuth.signOut()
            googleSignInClient.signOut().await()
        } catch (e: Exception) {
            Timber.e(e, "Sign out failed")
            throw Exception(context.getString(R.string.auth_sign_in_failed))
        }
    }

    override suspend fun updateUserSettings(settings: UserSettings): DomainResult<Unit> = result {
        val currentUser = firebaseAuth.currentUser ?: throw Exception("User not authenticated")

        val firestoreSettings = FirestoreUserSettings(
            notificationsEnabled = settings.notificationsEnabled,
            locationPermissionGranted = settings.locationPermissionGranted
        )

        firestore.collection(USER_SETTINGS_COLLECTION)
            .document(currentUser.uid)
            .set(firestoreSettings)
            .await()
    }

    private suspend fun createOrUpdateUserProfile(
        uid: String,
        name: String,
        email: String,
        avatarUrl: String?
    ): User {
        val firestoreUser = FirestoreUser(
            id = uid,
            name = name,
            email = email,
            avatarUrl = avatarUrl,
            createdAt = com.google.firebase.Timestamp.now(),
            updatedAt = com.google.firebase.Timestamp.now()
        )

        firestore.collection(USERS_COLLECTION)
            .document(uid)
            .set(firestoreUser)
            .await()

        return User(
            id = uid,
            name = name,
            email = email,
            avatarUrl = avatarUrl
        )
    }

    private suspend fun getOrCreateUserProfile(
        uid: String,
        displayName: String?,
        email: String,
        photoUrl: String?
    ): User {
        val userDoc = firestore.collection(USERS_COLLECTION).document(uid).get().await()

        return if (userDoc.exists()) {
            val firestoreUser = userDoc.toObject<FirestoreUser>()!!
            User(
                id = firestoreUser.id,
                name = firestoreUser.name,
                email = firestoreUser.email,
                avatarUrl = firestoreUser.avatarUrl
            )
        } else {
            createOrUpdateUserProfile(uid, displayName ?: "User", email, photoUrl)
        }
    }

    private suspend fun initializeUserData(uid: String) {
        // Initialize user stats
        val initialStats = FirestoreUserStats(
            toursCreated = 0,
            toursJoined = 0,
            toursCompleted = 0,
            totalDistance = 0f,
            totalDuration = 0,
            favoriteThemes = emptyList(),
            averageRating = 0f
        )

        firestore.collection(USER_STATS_COLLECTION)
            .document(uid)
            .set(initialStats)
            .await()

        // Initialize user settings
        val initialSettings = FirestoreUserSettings(
            notificationsEnabled = true,
            locationPermissionGranted = false,
            language = "en",
            currency = "USD",
            theme = "system"
        )

        firestore.collection(USER_SETTINGS_COLLECTION)
            .document(uid)
            .set(initialSettings)
            .await()
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