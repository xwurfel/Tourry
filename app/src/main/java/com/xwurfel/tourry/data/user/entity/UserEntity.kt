package com.xwurfel.tourry.data.user.entity

import androidx.room.Entity
import androidx.room.PrimaryKey
import com.xwurfel.tourry.domain.user.model.UserRole
import java.time.LocalDateTime

@Entity(tableName = "users")
data class UserEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val email: String,
    val passwordHash: String,
    val name: String,
    val bio: String?,
    val profileImageUriString: String?,
    val phoneNumber: String?,
    val role: UserRole,
    val createdAt: LocalDateTime = LocalDateTime.now(),
    val updatedAt: LocalDateTime = LocalDateTime.now()
)