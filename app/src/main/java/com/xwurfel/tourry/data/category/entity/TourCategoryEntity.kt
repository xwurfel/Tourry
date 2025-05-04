package com.xwurfel.tourry.data.category.entity

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "tour_categories")
data class TourCategoryEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val name: String,
    val description: String,
    val iconName: String?,
    val icon: String?,
)