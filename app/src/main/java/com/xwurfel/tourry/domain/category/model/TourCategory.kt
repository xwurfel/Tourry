package com.xwurfel.tourry.domain.category.model

data class TourCategory(
    val id: Long = 0,
    val name: String,
    val description: String,
    val iconName: String?
)