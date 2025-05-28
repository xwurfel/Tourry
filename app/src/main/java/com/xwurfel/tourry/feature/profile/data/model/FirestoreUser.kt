package com.xwurfel.tourry.feature.profile.data.model

import com.google.firebase.Timestamp
import com.google.firebase.database.PropertyName

data class FirestoreUser(
    @PropertyName("id") val id: String = "",
    @PropertyName("name") val name: String = "",
    @PropertyName("email") val email: String = "",
    @PropertyName("avatarUrl") val avatarUrl: String? = null,
    @PropertyName("createdAt") val createdAt: Timestamp = Timestamp.now(),
    @PropertyName("updatedAt") val updatedAt: Timestamp = Timestamp.now()
)

