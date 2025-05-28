package com.xwurfel.tourry.ui.auth

import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.ui.platform.LocalContext
import com.google.android.gms.auth.api.signin.GoogleSignInClient
import dagger.hilt.android.EntryPointAccessors

@Composable
fun GoogleSignInHandler(
    shouldLaunch: Boolean,
    onResult: (android.content.Intent?) -> Unit,
    onLaunched: () -> Unit
) {
    val context = LocalContext.current

    val launcher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.StartActivityForResult()
    ) { result ->
        onResult(result.data)
    }

    LaunchedEffect(shouldLaunch) {
        if (shouldLaunch) {
            try {
                // Get GoogleSignInClient from Hilt
                val entryPoint = EntryPointAccessors.fromApplication(
                    context.applicationContext,
                    GoogleSignInEntryPoint::class.java
                )
                val signInIntent = entryPoint.googleSignInClient().signInIntent
                launcher.launch(signInIntent)
                onLaunched()
            } catch (e: Exception) {
                onResult(null)
            }
        }
    }
}

@dagger.hilt.EntryPoint
@dagger.hilt.InstallIn(dagger.hilt.components.SingletonComponent::class)
interface GoogleSignInEntryPoint {
    fun googleSignInClient(): GoogleSignInClient
}