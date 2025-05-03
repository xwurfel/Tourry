package com.xwurfel.tourry.domain.user.model

import android.net.Uri
import java.time.LocalDateTime

data class User(
    val id: Long = 0,
    val email: String,
    val name: String,
    val bio: String?,
    val profileImageUri: Uri?,
    val phoneNumber: String?,
    val role: UserRole,
    val createdAt: LocalDateTime = LocalDateTime.now(),
    val updatedAt: LocalDateTime = LocalDateTime.now()
)