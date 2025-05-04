package com.xwurfel.tourry.data.category.mapper

import com.xwurfel.tourry.data.category.entity.TourCategoryEntity
import com.xwurfel.tourry.domain.category.model.TourCategory

fun TourCategoryEntity.toDomain(): TourCategory {
    return TourCategory(
        id = id,
        name = name,
        description = description,
        iconName = iconName,
        icon = icon
    )
}

fun TourCategory.toEntity(): TourCategoryEntity {
    return TourCategoryEntity(
        id = id,
        name = name,
        description = description,
        iconName = iconName,
        icon = icon
    )
}