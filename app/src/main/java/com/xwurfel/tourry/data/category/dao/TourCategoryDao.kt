package com.xwurfel.tourry.data.category.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import com.xwurfel.tourry.data.category.entity.TourCategoryEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface TourCategoryDao {
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertCategories(categories: List<TourCategoryEntity>)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertCategory(categoryEntity: TourCategoryEntity): Long

    @Query("SELECT * FROM tour_categories")
    fun getAllCategories(): Flow<List<TourCategoryEntity>>

    @Query("SELECT * FROM tour_categories WHERE id = :id")
    fun getCategoryById(id: Long): Flow<TourCategoryEntity?>

    @Query("SELECT * FROM tour_categories WHERE name LIKE '%' || :query || '%'")
    fun searchCategories(query: String): Flow<List<TourCategoryEntity>>

    @Query("DELETE FROM tour_categories WHERE id = :id")
    suspend fun deleteCategory(id: Long)

    @Query("DELETE FROM tour_categories")
    suspend fun deleteAllCategories()
}