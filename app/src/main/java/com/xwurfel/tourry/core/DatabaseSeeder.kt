package com.xwurfel.tourry.core

import com.xwurfel.tourry.di.coroutines.IoDispatcher
import com.xwurfel.tourry.domain.category.model.TourCategory
import com.xwurfel.tourry.domain.category.repository.TourCategoryRepository
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class DatabaseSeeder @Inject constructor(
    private val categoryRepository: TourCategoryRepository,
    @IoDispatcher private val ioDispatcher: CoroutineDispatcher
) {
    private val defaultCategories = listOf(
        TourCategory(
            name = "Adventure",
            description = "Exciting outdoor experiences and thrilling activities",
            iconName = "hiking"
        ),
        TourCategory(
            name = "Cultural",
            description = "Historical sites, museums, and cultural experiences",
            iconName = "museum"
        ),
        TourCategory(
            name = "Food & Drinks",
            description = "Culinary tours, wine tastings, and food experiences",
            iconName = "restaurant"
        ),
        TourCategory(
            name = "Nature",
            description = "Explore parks, wildlife, and natural wonders",
            iconName = "landscape"
        ),
        TourCategory(
            name = "Urban",
            description = "City tours, architecture, and urban exploration",
            iconName = "location_city"
        ),
        TourCategory(
            name = "Relaxation",
            description = "Spa experiences, wellness activities, and relaxing environments",
            iconName = "spa"
        )
    )

    fun seed() {
        CoroutineScope(ioDispatcher).launch {
            seedCategories()
        }
    }

    private suspend fun seedCategories() {
        val existingCategories = categoryRepository.getAllCategories().first()

        if (existingCategories.isEmpty()) {
            defaultCategories.forEach { category ->
                categoryRepository.saveCategory(category)
            }
        }
    }
}