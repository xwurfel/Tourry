package com.xwurfel.tourry.data.category.repository

import android.content.Context
import com.google.gson.Gson
import com.xwurfel.tourry.data.category.dao.TourCategoryDao
import com.xwurfel.tourry.data.category.mapper.toDomain
import com.xwurfel.tourry.data.category.mapper.toEntity
import com.xwurfel.tourry.data.network.api.CategoryApi
import com.xwurfel.tourry.data.network.dto.CategoryCreateDto
import com.xwurfel.tourry.data.network.util.ApiResponse
import com.xwurfel.tourry.data.network.util.NetworkUtils
import com.xwurfel.tourry.data.sync.SyncActionType
import com.xwurfel.tourry.data.sync.SyncEntity
import com.xwurfel.tourry.data.sync.dao.SyncDao
import com.xwurfel.tourry.di.coroutines.IoDispatcher
import com.xwurfel.tourry.domain.category.model.TourCategory
import com.xwurfel.tourry.domain.category.repository.TourCategoryRepository
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.withContext
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class TourCategoryRepositoryImpl @Inject constructor(
    private val categoryApi: CategoryApi,
    private val categoryDao: TourCategoryDao,
    private val syncDao: SyncDao,
    @ApplicationContext private val context: Context,
    private val gson: Gson,
    @IoDispatcher private val ioDispatcher: CoroutineDispatcher
) : TourCategoryRepository {

    override suspend fun saveCategory(category: TourCategory): Long = withContext(ioDispatcher) {
        if (NetworkUtils.isNetworkAvailable(context)) {
            val categoryDto = CategoryCreateDto(
                name = category.name,
                description = category.description,
                iconName = category.iconName,
                iconUrl = category.icon
            )

            try {
                val response = if (category.id == 0L) {
                    NetworkUtils.safeApiCall { categoryApi.createCategory(categoryDto) }
                } else {
                    NetworkUtils.safeApiCall {
                        categoryApi.updateCategory(
                            category.id,
                            categoryDto
                        )
                    }
                }

                when (response) {
                    is ApiResponse.Success -> {
                        val savedCategory = response.data
                        val categoryEntity = TourCategory(
                            id = savedCategory.id,
                            name = savedCategory.name,
                            description = savedCategory.description,
                            iconName = savedCategory.iconName,
                            icon = savedCategory.iconUrl
                        ).toEntity()

                        categoryDao.insertCategory(categoryEntity)
                        return@withContext savedCategory.id
                    }

                    is ApiResponse.Error -> {
                        val localId = categoryDao.insertCategory(category.toEntity())

                        val syncEntity = SyncEntity(
                            entityType = "category",
                            entityId = localId,
                            actionType = if (category.id == 0L) SyncActionType.CREATE else SyncActionType.UPDATE,
                            actionData = gson.toJson(category.copy(id = localId))
                        )
                        syncDao.insertSyncAction(syncEntity)

                        return@withContext localId
                    }

                    ApiResponse.Loading -> {
                        throw Exception("Request is still loading")
                    }
                }
            } catch (_: Exception) {
                val localId = categoryDao.insertCategory(category.toEntity())

                val syncEntity = SyncEntity(
                    entityType = "category",
                    entityId = localId,
                    actionType = if (category.id == 0L) SyncActionType.CREATE else SyncActionType.UPDATE,
                    actionData = gson.toJson(category.copy(id = localId))
                )
                syncDao.insertSyncAction(syncEntity)

                return@withContext localId
            }
        } else {
            val localId = categoryDao.insertCategory(category.toEntity())

            val syncEntity = SyncEntity(
                entityType = "category",
                entityId = localId,
                actionType = if (category.id == 0L) SyncActionType.CREATE else SyncActionType.UPDATE,
                actionData = gson.toJson(category.copy(id = localId))
            )
            syncDao.insertSyncAction(syncEntity)

            return@withContext localId
        }
    }

    override fun getAllCategories(): Flow<List<TourCategory>> = flow {
        if (NetworkUtils.isNetworkAvailable(context)) {
            try {
                when (val response = NetworkUtils.safeApiCall { categoryApi.getAllCategories() }) {
                    is ApiResponse.Success -> {
                        val categories = response.data.map { dto ->
                            TourCategory(
                                id = dto.id,
                                name = dto.name,
                                description = dto.description,
                                iconName = dto.iconName,
                                icon = dto.iconUrl
                            )
                        }

                        categoryDao.deleteAllCategories()
                        categoryDao.insertCategories(categories.map { it.toEntity() })

                        emit(categories)
                    }

                    is ApiResponse.Error -> {
                        val localCategories =
                            categoryDao.getAllCategories().first().map { it.toDomain() }
                        emit(localCategories)
                    }

                    ApiResponse.Loading -> {
                        val localCategories =
                            categoryDao.getAllCategories().first().map { it.toDomain() }
                        emit(localCategories)
                    }
                }
            } catch (_: Exception) {
                val localCategories = categoryDao.getAllCategories().first().map { it.toDomain() }
                emit(localCategories)
            }
        } else {
            val localCategories = categoryDao.getAllCategories().first().map { it.toDomain() }
            emit(localCategories)
        }
    }

    override fun getCategoryById(id: Long): Flow<TourCategory?> = flow {
        if (NetworkUtils.isNetworkAvailable(context)) {
            try {
                when (val response = NetworkUtils.safeApiCall { categoryApi.getCategoryById(id) }) {
                    is ApiResponse.Success -> {
                        val dto = response.data
                        val category = TourCategory(
                            id = dto.id,
                            name = dto.name,
                            description = dto.description,
                            iconName = dto.iconName,
                            icon = dto.iconUrl
                        )

                        // Update local cache
                        categoryDao.insertCategory(category.toEntity())

                        emit(category)
                    }

                    is ApiResponse.Error -> {
                        val localCategory = categoryDao.getCategoryById(id).first()?.toDomain()
                        emit(localCategory)
                    }

                    ApiResponse.Loading -> {
                        val localCategory = categoryDao.getCategoryById(id).first()?.toDomain()
                        emit(localCategory)
                    }
                }
            } catch (_: Exception) {
                val localCategory = categoryDao.getCategoryById(id).first()?.toDomain()
                emit(localCategory)
            }
        } else {
            val localCategory = categoryDao.getCategoryById(id).first()?.toDomain()
            emit(localCategory)
        }
    }

    override fun searchCategories(query: String): Flow<List<TourCategory>> = flow {
        if (NetworkUtils.isNetworkAvailable(context)) {
            try {
                when (val response =
                    NetworkUtils.safeApiCall { categoryApi.searchCategories(query) }) {
                    is ApiResponse.Success -> {
                        val categories = response.data.map { dto ->
                            TourCategory(
                                id = dto.id,
                                name = dto.name,
                                description = dto.description,
                                iconName = dto.iconName,
                                icon = dto.iconUrl
                            )
                        }

                        // Cache only the search results (don't replace all categories)
                        categories.forEach { category ->
                            categoryDao.insertCategory(category.toEntity())
                        }

                        emit(categories)
                    }

                    is ApiResponse.Error -> {
                        val localCategories =
                            categoryDao.searchCategories(query).first().map { it.toDomain() }
                        emit(localCategories)
                    }

                    ApiResponse.Loading -> {
                        val localCategories =
                            categoryDao.searchCategories(query).first().map { it.toDomain() }
                        emit(localCategories)
                    }
                }
            } catch (_: Exception) {
                val localCategories =
                    categoryDao.searchCategories(query).first().map { it.toDomain() }
                emit(localCategories)
            }
        } else {
            val localCategories = categoryDao.searchCategories(query).first().map { it.toDomain() }
            emit(localCategories)
        }
    }

    override suspend fun deleteCategory(id: Long) = withContext(ioDispatcher) {
        if (NetworkUtils.isNetworkAvailable(context)) {
            try {
                when (NetworkUtils.safeApiCall { categoryApi.deleteCategory(id) }) {
                    is ApiResponse.Success -> {
                        categoryDao.deleteCategory(id)
                    }

                    is ApiResponse.Error -> {
                        val syncEntity = SyncEntity(
                            entityType = "category",
                            entityId = id,
                            actionType = SyncActionType.DELETE
                        )
                        syncDao.insertSyncAction(syncEntity)

                        // For UI updates, delete locally anyway
                        categoryDao.deleteCategory(id)
                    }

                    ApiResponse.Loading -> {
                        // Should not happen with safeApiCall
                    }
                }
            } catch (_: Exception) {
                val syncEntity = SyncEntity(
                    entityType = "category",
                    entityId = id,
                    actionType = SyncActionType.DELETE
                )
                syncDao.insertSyncAction(syncEntity)

                // For UI updates, delete locally anyway
                categoryDao.deleteCategory(id)
            }
        } else {
            val syncEntity = SyncEntity(
                entityType = "category",
                entityId = id,
                actionType = SyncActionType.DELETE
            )
            syncDao.insertSyncAction(syncEntity)

            // For UI updates, delete locally anyway
            categoryDao.deleteCategory(id)
        }
    }
}