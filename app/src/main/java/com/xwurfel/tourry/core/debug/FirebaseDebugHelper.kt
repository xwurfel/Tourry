package com.xwurfel.tourry.core.debug

import android.content.Context
import com.google.android.gms.auth.api.signin.GoogleSignIn
import com.google.android.gms.auth.api.signin.GoogleSignInOptions
import com.google.firebase.auth.FirebaseAuth
import timber.log.Timber
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Helper class for debugging Firebase authentication setup
 */
@Singleton
class FirebaseDebugHelper @Inject constructor(
    private val firebaseAuth: FirebaseAuth
) {

    fun checkFirebaseConfiguration(context: Context) {
        Timber.d("=== Firebase Configuration Debug ===")

        // Check Firebase Auth initialization
        try {
            val currentUser = firebaseAuth.currentUser
            Timber.d("Firebase Auth initialized: ${firebaseAuth.app.name}")
            Timber.d("Current user: ${currentUser?.email ?: "None"}")
        } catch (e: Exception) {
            Timber.e(e, "Firebase Auth not properly initialized")
        }

        // Check Google Services
        checkGoogleServices(context)

        // Check Google Sign-In configuration
        checkGoogleSignInConfiguration(context)
    }

    private fun checkGoogleServices(context: Context) {
        try {
            val resourceId = context.resources.getIdentifier(
                "default_web_client_id",
                "string",
                context.packageName
            )

            if (resourceId != 0) {
                val webClientId = context.getString(resourceId)
                Timber.d("Google Web Client ID found: ${webClientId.take(20)}...")
            } else {
                Timber.e("Google Web Client ID not found - google-services.json may be missing")
            }
        } catch (e: Exception) {
            Timber.e(e, "Error checking Google Services configuration")
        }
    }

    private fun checkGoogleSignInConfiguration(context: Context) {
        try {
            val gso = GoogleSignInOptions.Builder(GoogleSignInOptions.DEFAULT_SIGN_IN)
                .requestEmail()
                .build()

            val googleSignInClient = GoogleSignIn.getClient(context, gso)
            val lastSignedInAccount = GoogleSignIn.getLastSignedInAccount(context)

            Timber.d("Google Sign-In client created successfully")
            Timber.d("Last signed in account: ${lastSignedInAccount?.email ?: "None"}")
        } catch (e: Exception) {
            Timber.e(e, "Error configuring Google Sign-In")
        }
    }

    fun testAnonymousSignIn() {
        Timber.d("Testing anonymous sign-in...")
        firebaseAuth.signInAnonymously()
            .addOnSuccessListener {
                Timber.d("Anonymous sign-in successful: ${it.user?.uid}")
            }
            .addOnFailureListener { e ->
                Timber.e(e, "Anonymous sign-in failed")
            }
    }
}