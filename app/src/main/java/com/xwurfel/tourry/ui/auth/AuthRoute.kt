package com.xwurfel.tourry.ui.auth

import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Visibility
import androidx.compose.material.icons.filled.VisibilityOff
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.input.VisualTransformation
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.xwurfel.tourry.R
import com.xwurfel.tourry.core.extension.collectWithLifecycle
import com.xwurfel.tourry.ui.main.LocalSnackbarHostState

@Composable
fun AuthRoute(
    onAuthSuccess: () -> Unit,
    viewModel: AuthViewModel = hiltViewModel()
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    val snackbarHostState = LocalSnackbarHostState.current

    GoogleSignInHandler(
        shouldLaunch = uiState.shouldLaunchGoogleSignIn,
        onResult = { intent ->
            viewModel.acceptIntent(AuthIntent.GoogleSignInResult(intent))
        },
        onLaunched = {
            viewModel.acceptIntent(AuthIntent.GoogleSignInLaunched)
        }
    )

    viewModel.event.collectWithLifecycle { event ->
        when (event) {
            AuthEvent.NavigateToMain -> onAuthSuccess()
        }
    }

    // Show error messages
    LaunchedEffect(uiState.error) {
        uiState.error?.let { error ->
            snackbarHostState.showSnackbar(error)
            viewModel.acceptIntent(AuthIntent.ClearError)
        }
    }

    // Show password reset confirmation
    LaunchedEffect(uiState.passwordResetSent) {
        if (uiState.passwordResetSent) {
            snackbarHostState.showSnackbar("Password reset email sent!")
            viewModel.acceptIntent(AuthIntent.ClearError)
        }
    }

    AuthScreen(
        uiState = uiState,
        onIntent = viewModel::acceptIntent,
        snackbarHostState = snackbarHostState
    )
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AuthScreen(
    uiState: AuthUiState,
    onIntent: (AuthIntent) -> Unit,
    snackbarHostState: SnackbarHostState
) {
    var authMode by remember { mutableStateOf(AuthMode.SIGN_IN) }
    var showPasswordReset by remember { mutableStateOf(false) }

    Scaffold(
        topBar = { TopAppBar(title = { }) },
        snackbarHost = { SnackbarHost(snackbarHostState) }
    ) { paddingValues ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
        ) {
            if (uiState.isLoading) {
                CircularProgressIndicator(
                    modifier = Modifier.align(Alignment.Center)
                )
            } else {
                Column(
                    modifier = Modifier
                        .fillMaxSize()
                        .verticalScroll(rememberScrollState())
                        .padding(24.dp),
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.Center
                ) {
                    // App logo/icon
                    Image(
                        painter = painterResource(R.drawable.ic_splash_logo),
                        contentDescription = "Tourry Logo",
                        modifier = Modifier
                            .size(120.dp)
                            .clip(RoundedCornerShape(24.dp))
                    )

                    Spacer(modifier = Modifier.height(32.dp))

                    // Welcome text
                    Text(
                        text = "Welcome to Tourry",
                        style = MaterialTheme.typography.headlineMedium,
                        fontWeight = FontWeight.Bold,
                        textAlign = TextAlign.Center
                    )

                    Spacer(modifier = Modifier.height(12.dp))

                    Text(
                        text = "Discover amazing tours or create your own guided experiences",
                        style = MaterialTheme.typography.bodyLarge,
                        textAlign = TextAlign.Center,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )

                    Spacer(modifier = Modifier.height(48.dp))

                    when (authMode) {
                        AuthMode.SIGN_IN -> SignInForm(
                            onSignIn = { email, password ->
                                onIntent(AuthIntent.SignInWithEmail(email, password))
                            },
                            onGoogleSignIn = { onIntent(AuthIntent.SignInWithGoogle) },
                            onSwitchToSignUp = { authMode = AuthMode.SIGN_UP },
                            onForgotPassword = { showPasswordReset = true },
                        )

                        AuthMode.SIGN_UP -> SignUpForm(
                            onSignUp = { email, password, name ->
                                onIntent(AuthIntent.CreateAccount(email, password, name))
                            },
                            onGoogleSignIn = { onIntent(AuthIntent.SignInWithGoogle) },
                            onSwitchToSignIn = { authMode = AuthMode.SIGN_IN }
                        )
                    }
                }
            }
        }
    }

    // Password Reset Dialog
    if (showPasswordReset) {
        PasswordResetDialog(
            onDismiss = { showPasswordReset = false },
            onSendReset = { email ->
                onIntent(AuthIntent.SendPasswordReset(email))
                showPasswordReset = false
            }
        )
    }
}

@Composable
fun SignInForm(
    onSignIn: (String, String) -> Unit,
    onGoogleSignIn: () -> Unit,
    onSwitchToSignUp: () -> Unit,
    onForgotPassword: () -> Unit,
) {
    var email by remember { mutableStateOf("") }
    var password by remember { mutableStateOf("") }
    var showPassword by remember { mutableStateOf(false) }

    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        // Email field
        OutlinedTextField(
            value = email,
            onValueChange = { email = it },
            label = { Text("Email") },
            modifier = Modifier.fillMaxWidth(),
            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Email),
            singleLine = true
        )

        // Password field
        OutlinedTextField(
            value = password,
            onValueChange = { password = it },
            label = { Text("Password") },
            modifier = Modifier.fillMaxWidth(),
            visualTransformation = if (showPassword) VisualTransformation.None else PasswordVisualTransformation(),
            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Password),
            trailingIcon = {
                IconButton(onClick = { showPassword = !showPassword }) {
                    Icon(
                        imageVector = if (showPassword) Icons.Default.Visibility else Icons.Default.VisibilityOff,
                        contentDescription = if (showPassword) "Hide password" else "Show password"
                    )
                }
            },
            singleLine = true
        )

        // Forgot Password
        TextButton(onClick = onForgotPassword) {
            Text("Forgot Password?")
        }

        // Sign In Button
        Button(
            onClick = { onSignIn(email, password) },
            modifier = Modifier.fillMaxWidth(),
            enabled = email.isNotBlank() && password.isNotBlank()
        ) {
            Text("Sign In")
        }

        // Google Sign In
        OutlinedButton(
            onClick = onGoogleSignIn,
            modifier = Modifier.fillMaxWidth()
        ) {
            Icon(
                painter = painterResource(R.drawable.ic_launcher_foreground), // TODO: Replace with Google icon
                contentDescription = null,
                modifier = Modifier.size(20.dp)
            )
            Spacer(modifier = Modifier.size(8.dp))
            Text("Continue with Google")
        }

        // Switch to Sign Up
        TextButton(onClick = onSwitchToSignUp) {
            Text("Don't have an account? Sign Up")
        }
    }
}

@Composable
fun SignUpForm(
    onSignUp: (String, String, String) -> Unit,
    onGoogleSignIn: () -> Unit,
    onSwitchToSignIn: () -> Unit
) {
    var name by remember { mutableStateOf("") }
    var email by remember { mutableStateOf("") }
    var password by remember { mutableStateOf("") }
    var showPassword by remember { mutableStateOf(false) }

    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        // Name field
        OutlinedTextField(
            value = name,
            onValueChange = { name = it },
            label = { Text("Full Name") },
            modifier = Modifier.fillMaxWidth(),
            singleLine = true
        )

        // Email field
        OutlinedTextField(
            value = email,
            onValueChange = { email = it },
            label = { Text("Email") },
            modifier = Modifier.fillMaxWidth(),
            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Email),
            singleLine = true
        )

        // Password field
        OutlinedTextField(
            value = password,
            onValueChange = { password = it },
            label = { Text("Password") },
            modifier = Modifier.fillMaxWidth(),
            visualTransformation = if (showPassword) VisualTransformation.None else PasswordVisualTransformation(),
            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Password),
            trailingIcon = {
                IconButton(onClick = { showPassword = !showPassword }) {
                    Icon(
                        imageVector = if (showPassword) Icons.Default.Visibility else Icons.Default.VisibilityOff,
                        contentDescription = if (showPassword) "Hide password" else "Show password"
                    )
                }
            },
            singleLine = true
        )

        // Sign Up Button
        Button(
            onClick = { onSignUp(email, password, name) },
            modifier = Modifier.fillMaxWidth(),
            enabled = email.isNotBlank() && password.isNotBlank() && name.isNotBlank()
        ) {
            Text("Create Account")
        }

        // Google Sign In
        OutlinedButton(
            onClick = onGoogleSignIn,
            modifier = Modifier.fillMaxWidth()
        ) {
            Icon(
                painter = painterResource(R.drawable.ic_launcher_foreground), // TODO: Replace with Google icon
                contentDescription = null,
                modifier = Modifier.size(20.dp)
            )
            Spacer(modifier = Modifier.size(8.dp))
            Text("Continue with Google")
        }

        // Switch to Sign In
        TextButton(onClick = onSwitchToSignIn) {
            Text("Already have an account? Sign In")
        }
    }
}

@Composable
fun PasswordResetDialog(
    onDismiss: () -> Unit,
    onSendReset: (String) -> Unit
) {
    var email by remember { mutableStateOf("") }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Reset Password") },
        text = {
            Column {
                Text("Enter your email address and we'll send you a link to reset your password.")
                Spacer(modifier = Modifier.height(16.dp))
                OutlinedTextField(
                    value = email,
                    onValueChange = { email = it },
                    label = { Text("Email") },
                    modifier = Modifier.fillMaxWidth(),
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Email),
                    singleLine = true
                )
            }
        },
        confirmButton = {
            Button(
                onClick = { onSendReset(email) },
                enabled = email.isNotBlank()
            ) {
                Text("Send Reset Email")
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("Cancel")
            }
        }
    )
}

private enum class AuthMode {
    SIGN_IN,
    SIGN_UP
}