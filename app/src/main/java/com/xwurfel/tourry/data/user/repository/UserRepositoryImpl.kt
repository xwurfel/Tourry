package com.xwurfel.tourry.data.user.repository

import android.content.Context
import androidx.core.net.toUri
import com.google.gson.Gson
import com.xwurfel.tourry.data.network.api.UserApi
import com.xwurfel.tourry.data.network.dto.UpdateRoleDto
import com.xwurfel.tourry.data.network.dto.UpdateUserDto
import com.xwurfel.tourry.data.network.util.ApiResponse
import com.xwurfel.tourry.data.network.util.NetworkUtils
import com.xwurfel.tourry.data.sync.SyncActionType
import com.xwurfel.tourry.data.sync.SyncEntity
import com.xwurfel.tourry.data.sync.dao.SyncDao
import com.xwurfel.tourry.data.upload.FileUploadService
import com.xwurfel.tourry.data.user.dao.UserDao
import com.xwurfel.tourry.data.user.mapper.toDomain
import com.xwurfel.tourry.data.user.mapper.toEntity
import com.xwurfel.tourry.di.coroutines.IoDispatcher
import com.xwurfel.tourry.domain.user.model.User
import com.xwurfel.tourry.domain.user.model.UserRole
import com.xwurfel.tourry.domain.user.repository.UserRepository
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.withContext
import java.security.MessageDigest
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class UserRepositoryImpl @Inject constructor(
    private val userApi: UserApi,
    private val userDao: UserDao,
    private val syncDao: SyncDao,
    private val fileUploadService: FileUploadService,
    @ApplicationContext private val context: Context,
    private val gson: Gson,
    @IoDispatcher private val ioDispatcher: CoroutineDispatcher
) : UserRepository {

    override fun getUserById(id: Long): Flow<User?> = flow {
        if (NetworkUtils.isNetworkAvailable(context)) {
            try {
                when (val response = NetworkUtils.safeApiCall { userApi.getUserById(id) }) {
                    is ApiResponse.Success -> {
                        val dto = response.data
                        val user = User(
                            id = dto.id,
                            email = dto.email,
                            name = dto.name,
                            bio = dto.bio,
                            profileImageUri = dto.profileImageUrl?.toUri(),
                            phoneNumber = dto.phoneNumber,
                            role = UserRole.valueOf(dto.role),
                            createdAt = dto.createdAt,
                            updatedAt = dto.updatedAt
                        )

                        // Update local cache - use a placeholder password hash
                        // since we don't have the actual password
                        userDao.insertUser(user.toEntity("placeholder_hash"))

                        emit(user)
                    }

                    is ApiResponse.Error -> {
                        val localUser = userDao.getUserById(id).first()?.toDomain()
                        emit(localUser)
                    }

                    ApiResponse.Loading -> {
                        val localUser = userDao.getUserById(id).first()?.toDomain()
                        emit(localUser)
                    }
                }
            } catch (e: Exception) {
                val localUser = userDao.getUserById(id).first()?.toDomain()
                emit(localUser)
            }
        } else {
            val localUser = userDao.getUserById(id).first()?.toDomain()
            emit(localUser)
        }
    }

    override fun getUserByEmail(email: String): Flow<User?> = flow {
        if (NetworkUtils.isNetworkAvailable(context)) {
            try {
                // The API might not have an endpoint for this specific operation
                // So we'll use the current user endpoint and filter by email
                when (val response = NetworkUtils.safeApiCall { userApi.getCurrentUser() }) {
                    is ApiResponse.Success -> {
                        val dto = response.data
                        if (dto.email == email) {
                            val user = User(
                                id = dto.id,
                                email = dto.email,
                                name = dto.name,
                                bio = dto.bio,
                                profileImageUri = dto.profileImageUrl?.toUri(),
                                phoneNumber = dto.phoneNumber,
                                role = UserRole.valueOf(dto.role),
                                createdAt = dto.createdAt,
                                updatedAt = dto.updatedAt
                            )

                            // Update local cache - use a placeholder password hash
                            userDao.insertUser(user.toEntity("placeholder_hash"))

                            emit(user)
                        } else {
                            // If current user's email doesn't match, fall back to local data
                            val localUser = userDao.getUserByEmail(email).first()?.toDomain()
                            emit(localUser)
                        }
                    }

                    is ApiResponse.Error -> {
                        val localUser = userDao.getUserByEmail(email).first()?.toDomain()
                        emit(localUser)
                    }

                    ApiResponse.Loading -> {
                        val localUser = userDao.getUserByEmail(email).first()?.toDomain()
                        emit(localUser)
                    }
                }
            } catch (e: Exception) {
                val localUser = userDao.getUserByEmail(email).first()?.toDomain()
                emit(localUser)
            }
        } else {
            val localUser = userDao.getUserByEmail(email).first()?.toDomain()
            emit(localUser)
        }
    }

    override fun getAllUsers(): Flow<List<User>> = flow {
        // This might be an admin-only feature
        // For now, we'll just return local data
        emit(userDao.getAllUsers().first().map { it.toDomain() })
    }

    override fun getUsersByRole(role: UserRole): Flow<List<User>> = flow {
        // This might be an admin-only feature
        // For now, we'll just return local data
        emit(userDao.getUsersByRole(role).first().map { it.toDomain() })
    }

    override suspend fun updateUserRole(userId: Long, role: UserRole): Unit =
        withContext(ioDispatcher) {
            if (NetworkUtils.isNetworkAvailable(context)) {
                try {
                    val roleDto = UpdateRoleDto(role.name)

                    when (val response = NetworkUtils.safeApiCall {
                        userApi.updateUserRole(userId, roleDto)
                    }) {
                        is ApiResponse.Success -> {
                            userDao.updateUserRole(userId, role)
                        }

                        is ApiResponse.Error -> {
                            userDao.updateUserRole(userId, role)

                            val syncEntity = SyncEntity(
                                entityType = "user_role",
                                entityId = userId,
                                actionType = SyncActionType.UPDATE,
                                actionData = gson.toJson(mapOf("role" to role.name))
                            )
                            syncDao.insertSyncAction(syncEntity)
                        }

                        ApiResponse.Loading -> {
                            // Should not happen with safeApiCall
                        }
                    }
                } catch (e: Exception) {
                    userDao.updateUserRole(userId, role)

                    val syncEntity = SyncEntity(
                        entityType = "user_role",
                        entityId = userId,
                        actionType = SyncActionType.UPDATE,
                        actionData = gson.toJson(mapOf("role" to role.name))
                    )
                    syncDao.insertSyncAction(syncEntity)
                }
            } else {
                userDao.updateUserRole(userId, role)

                val syncEntity = SyncEntity(
                    entityType = "user_role",
                    entityId = userId,
                    actionType = SyncActionType.UPDATE,
                    actionData = gson.toJson(mapOf("role" to role.name))
                )
                syncDao.insertSyncAction(syncEntity)
            }
        }

    override suspend fun updateUser(user: User, newPassword: String?): Unit =
        withContext(ioDispatcher) {
            var imageUrl: String? = null

            // Upload profile image if it's new or changed
            if (user.profileImageUri != null) {
                try {
                    val currentUser = userDao.getUserById(user.id).first()
                    if (currentUser == null || currentUser.profileImageUriString != user.profileImageUri.toString()) {
                        val result = fileUploadService.uploadImage(user.profileImageUri)
                        if (result.isSuccess) {
                            imageUrl = result.getOrThrow()
                        }
                    } else {
                        imageUrl = user.profileImageUri.toString()
                    }
                } catch (e: Exception) {
                    // Continue with update even if image upload fails
                    imageUrl = user.profileImageUri.toString()
                }
            }

            if (NetworkUtils.isNetworkAvailable(context)) {
                try {
                    val updateUserDto = UpdateUserDto(
                        name = user.name,
                        bio = user.bio,
                        profileImageUrl = imageUrl,
                        phoneNumber = user.phoneNumber,
                        password = newPassword
                    )

                    when (val response = NetworkUtils.safeApiCall {
                        userApi.updateUser(user.id, updateUserDto)
                    }) {
                        is ApiResponse.Success -> {
                            val updatedUser = response.data
                            val userEntity = User(
                                id = updatedUser.id,
                                email = updatedUser.email,
                                name = updatedUser.name,
                                bio = updatedUser.bio,
                                profileImageUri = updatedUser.profileImageUrl?.toUri(),
                                phoneNumber = updatedUser.phoneNumber,
                                role = UserRole.valueOf(updatedUser.role),
                                createdAt = updatedUser.createdAt,
                                updatedAt = updatedUser.updatedAt
                            )

                            // We need to preserve the password hash from the local database
                            val currentUserEntity = userDao.getUserById(user.id).first()
                            val passwordHash = if (newPassword != null) {
                                hashPassword(newPassword)
                            } else {
                                currentUserEntity?.passwordHash ?: "placeholder_hash"
                            }

                            userDao.insertUser(userEntity.toEntity(passwordHash))
                        }

                        is ApiResponse.Error -> {
                            // Update locally and mark for sync
                            val currentUserEntity = userDao.getUserById(user.id).first()
                            val passwordHash = if (newPassword != null) {
                                hashPassword(newPassword)
                            } else {
                                currentUserEntity?.passwordHash ?: "placeholder_hash"
                            }

                            userDao.insertUser(user.toEntity(passwordHash))

                            val syncEntity = SyncEntity(
                                entityType = "user",
                                entityId = user.id,
                                actionType = SyncActionType.UPDATE,
                                actionData = gson.toJson(
                                    mapOf(
                                        "name" to user.name,
                                        "bio" to user.bio,
                                        "profileImageUrl" to imageUrl,
                                        "phoneNumber" to user.phoneNumber,
                                        "password" to newPassword
                                    )
                                )
                            )
                            syncDao.insertSyncAction(syncEntity)
                        }

                        ApiResponse.Loading -> {
                            // Should not happen with safeApiCall
                        }
                    }
                } catch (e: Exception) {
                    // Update locally and mark for sync
                    val currentUserEntity = userDao.getUserById(user.id).first()
                    val passwordHash = if (newPassword != null) {
                        hashPassword(newPassword)
                    } else {
                        currentUserEntity?.passwordHash ?: "placeholder_hash"
                    }

                    userDao.insertUser(user.toEntity(passwordHash))

                    val syncEntity = SyncEntity(
                        entityType = "user",
                        entityId = user.id,
                        actionType = SyncActionType.UPDATE,
                        actionData = gson.toJson(
                            mapOf(
                                "name" to user.name,
                                "bio" to user.bio,
                                "profileImageUrl" to imageUrl,
                                "phoneNumber" to user.phoneNumber,
                                "password" to newPassword
                            )
                        )
                    )
                    syncDao.insertSyncAction(syncEntity)
                }
            } else {
                // Update locally and mark for sync
                val currentUserEntity = userDao.getUserById(user.id).first()
                val passwordHash = if (newPassword != null) {
                    hashPassword(newPassword)
                } else {
                    currentUserEntity?.passwordHash ?: "placeholder_hash"
                }

                userDao.insertUser(user.toEntity(passwordHash))

                val syncEntity = SyncEntity(
                    entityType = "user",
                    entityId = user.id,
                    actionType = SyncActionType.UPDATE,
                    actionData = gson.toJson(
                        mapOf(
                            "name" to user.name,
                            "bio" to user.bio,
                            "profileImageUrl" to imageUrl,
                            "phoneNumber" to user.phoneNumber,
                            "password" to newPassword
                        )
                    )
                )
                syncDao.insertSyncAction(syncEntity)
            }
        }

    override suspend fun deleteUser(id: Long) = withContext(ioDispatcher) {
        if (NetworkUtils.isNetworkAvailable(context)) {
            try {
                when (val response = NetworkUtils.safeApiCall { userApi.deleteUser(id) }) {
                    is ApiResponse.Success -> {
                        userDao.deleteUser(id)
                    }

                    is ApiResponse.Error -> {
                        val syncEntity = SyncEntity(
                            entityType = "user",
                            entityId = id,
                            actionType = SyncActionType.DELETE
                        )
                        syncDao.insertSyncAction(syncEntity)

                        // For UI updates, delete locally anyway
                        userDao.deleteUser(id)
                    }

                    ApiResponse.Loading -> {
                        // Should not happen with safeApiCall
                    }
                }
            } catch (e: Exception) {
                val syncEntity = SyncEntity(
                    entityType = "user",
                    entityId = id,
                    actionType = SyncActionType.DELETE
                )
                syncDao.insertSyncAction(syncEntity)

                // For UI updates, delete locally anyway
                userDao.deleteUser(id)
            }
        } else {
            val syncEntity = SyncEntity(
                entityType = "user",
                entityId = id,
                actionType = SyncActionType.DELETE
            )
            syncDao.insertSyncAction(syncEntity)

            // For UI updates, delete locally anyway
            userDao.deleteUser(id)
        }
    }

    private fun hashPassword(password: String): String {
        val bytes = password.toByteArray()
        val md = MessageDigest.getInstance("SHA-256")
        val digest = md.digest(bytes)
        return digest.fold("") { str, it -> str + "%02x".format(it) }
    }
}