package com.xwurfel.tourry.feature.profile.domain.model

data class UserSettings(
    val notificationsEnabled: Boolean = true,
    val locationPermissionGranted: Boolean = false
)