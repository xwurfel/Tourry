package com.xwurfel.tourry.feature.profile.domain.model

data class User(
    val id: String,
    val name: String,
    val email: String,
    val avatarUrl: String? = null,
    val stats: UserStats? = null
)
