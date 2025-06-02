package com.xwurfel.tourry.feature.tours.domain.model

import com.xwurfel.tourry.feature.tours.domain.model.MyTourStatus

data class MyTour(
    val id: String,
    val title: String,
    val coverImageUrl: String?,
    val startTime: Long,
    val status: MyTourStatus,
    val participantsCount: Int = 0,
    val rating: Float? = null
)