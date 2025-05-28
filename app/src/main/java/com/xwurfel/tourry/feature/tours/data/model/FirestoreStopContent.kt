package com.xwurfel.tourry.feature.tours.data.model

import com.google.firebase.firestore.PropertyName

data class FirestoreStopContent(
    @PropertyName("text") val text: String = "",
    @PropertyName("imageUrls") val imageUrls: List<String> = emptyList(),
    @PropertyName("audioUrl") val audioUrl: String? = null,
    @PropertyName("videoUrl") val videoUrl: String? = null
)