package com.xwurfel.tourry.domain.category.repository

import com.xwurfel.tourry.domain.category.model.TourCategory
import kotlinx.coroutines.flow.Flow

interface TourCategoryRepository {
    suspend fun saveCategory(category: TourCategory): Long

    fun getAllCategories(): Flow<List<TourCategory>>

    fun getCategoryById(id: Long): Flow<TourCategory?>

    fun searchCategories(query: String): Flow<List<TourCategory>>

    suspend fun deleteCategory(id: Long)
}