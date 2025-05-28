package com.xwurfel.tourry.feature.profile.data.model

import com.google.firebase.database.PropertyName

data class FirestoreUserSettings(
    @PropertyName("notificationsEnabled") val notificationsEnabled: Boolean = true,
    @PropertyName("locationPermissionGranted") val locationPermissionGranted: Boolean = false,
    @PropertyName("language") val language: String = "en",
    @PropertyName("currency") val currency: String = "USD",
    @PropertyName("theme") val theme: String = "system" // light, dark, system
)