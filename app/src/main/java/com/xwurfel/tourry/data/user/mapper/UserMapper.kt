package com.xwurfel.tourry.data.user.mapper

import androidx.core.net.toUri
import com.xwurfel.tourry.data.user.entity.UserEntity
import com.xwurfel.tourry.domain.user.model.User

fun UserEntity.toDomain(): User {
    return User(
        id = id,
        email = email,
        name = name,
        bio = bio,
        profileImageUri = profileImageUriString?.toUri(),
        phoneNumber = phoneNumber,
        role = role,
        createdAt = createdAt,
        updatedAt = updatedAt
    )
}

fun User.toEntity(passwordHash: String): UserEntity {
    return UserEntity(
        id = id,
        email = email,
        passwordHash = passwordHash,
        name = name,
        bio = bio,
        profileImageUriString = profileImageUri?.toString(),
        phoneNumber = phoneNumber,
        role = role,
        createdAt = createdAt,
        updatedAt = updatedAt
    )
}