package com.xwurfel.tourry.data.category.repository

import com.xwurfel.tourry.data.category.dao.TourCategoryDao
import com.xwurfel.tourry.data.category.mapper.toDomain
import com.xwurfel.tourry.data.category.mapper.toEntity
import com.xwurfel.tourry.domain.category.model.TourCategory
import com.xwurfel.tourry.domain.category.repository.TourCategoryRepository
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import javax.inject.Inject

class TourCategoryRepositoryImpl @Inject constructor(
    private val categoryDao: TourCategoryDao
) : TourCategoryRepository {

    override suspend fun saveCategory(category: TourCategory): Long {
        return categoryDao.insertCategory(category.toEntity())
    }

    override fun getAllCategories(): Flow<List<TourCategory>> {
        return categoryDao.getAllCategories().map { entities ->
            entities.map { it.toDomain() }
        }
    }

    override fun getCategoryById(id: Long): Flow<TourCategory?> {
        return categoryDao.getCategoryById(id).map { it?.toDomain() }
    }

    override fun searchCategories(query: String): Flow<List<TourCategory>> {
        return categoryDao.searchCategories(query).map { entities ->
            entities.map { it.toDomain() }
        }
    }

    override suspend fun deleteCategory(id: Long) {
        categoryDao.deleteCategory(id)
    }
}