package com.xwurfel.tourry.domain.auth.model

import com.xwurfel.tourry.domain.user.model.User

data class AuthState(
    val isAuthenticated: Boolean = false,
    val currentUser: User? = null,
    val error: String? = null
)

