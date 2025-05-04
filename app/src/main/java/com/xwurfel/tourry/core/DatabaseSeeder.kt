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
    val defaultCategories = listOf(
        TourCategory(
            name = "Adventure",
            description = "Exciting outdoor experiences and thrilling activities",
            iconName = "hiking",
            icon = "https://img.icons8.com/fluency/48/mountain.png"
        ),
        TourCategory(
            name = "Cultural",
            description = "Historical sites, museums, and cultural experiences",
            iconName = "museum",
            icon = "https://img.icons8.com/fluency/48/museum.png"
        ),
        TourCategory(
            name = "Food & Drinks",
            description = "Culinary tours, wine tastings, and food experiences",
            iconName = "restaurant",
            icon = "https://img.icons8.com/fluency/48/restaurant.png"
        ),
        TourCategory(
            name = "Nature",
            description = "Explore parks, wildlife, and natural wonders",
            iconName = "landscape",
            icon = "https://img.icons8.com/fluency/48/forest.png"
        ),
        TourCategory(
            name = "Urban",
            description = "City tours, architecture, and urban exploration",
            iconName = "location_city",
            icon = "https://img.icons8.com/fluency/48/city.png"
        ),
        TourCategory(
            name = "Relaxation",
            description = "Spa experiences, wellness activities, and relaxing environments",
            iconName = "spa",
            icon = "https://img.icons8.com/fluency/48/spa.png"
        ),
        TourCategory(
            name = "Beach",
            description = "Sun, sand, and sea experiences",
            iconName = "beach_access",
            icon = "https://img.icons8.com/fluency/48/beach.png"
        ),
        TourCategory(
            name = "Wildlife",
            description = "Safaris, zoos, and animal encounters",
            iconName = "pets",
            icon = "https://img.icons8.com/fluency/48/lion.png"
        ),
        TourCategory(
            name = "Photography",
            description = "Tours focused on capturing stunning visuals",
            iconName = "photo_camera",
            icon = "https://img.icons8.com/fluency/48/camera.png"
        ),
        TourCategory(
            name = "Cruise",
            description = "Luxury and sightseeing on water",
            iconName = "directions_boat",
            icon = "https://img.icons8.com/fluency/48/cruise-ship.png"
        ),
        TourCategory(
            name = "Festival",
            description = "Seasonal events and cultural festivals",
            iconName = "celebration",
            icon = "https://img.icons8.com/fluency/48/confetti.png"
        ),
        TourCategory(
            name = "Nightlife",
            description = "Clubbing, bars, and nightlife adventures",
            iconName = "nightlife",
            icon = "https://img.icons8.com/fluency/48/cocktail.png"
        ),
        TourCategory(
            name = "Shopping",
            description = "Market, mall, and local shopping tours",
            iconName = "shopping_bag",
            icon = "https://img.icons8.com/fluency/48/shopping-cart.png"
        ),
        TourCategory(
            name = "Sport",
            description = "Watch or play local and international sports",
            iconName = "sports_soccer",
            icon = "https://img.icons8.com/fluency/48/football2.png"
        ),
        TourCategory(
            name = "Luxury",
            description = "Premium, exclusive travel experiences",
            iconName = "star",
            icon = "https://img.icons8.com/fluency/48/vip.png"
        ),
        TourCategory(
            name = "Historical",
            description = "Sites with historical and cultural importance",
            iconName = "history_edu",
            icon = "https://img.icons8.com/fluency/48/book.png"
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