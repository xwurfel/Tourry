package com.xwurfel.tourry.data.network.dto

data class CategoryCreateDto(
    val name: String,
    val description: String,
    val iconName: String? = null,
    val iconUrl: String? = null
)

data class CategoryResponseDto(
    val id: Long,
    val name: String,
    val description: String,
    val iconName: String?,
    val iconUrl: String?
)