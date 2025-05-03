package com.xwurfel.tourry.data.user.repository

import com.xwurfel.tourry.data.user.dao.UserDao
import com.xwurfel.tourry.data.user.mapper.toDomain
import com.xwurfel.tourry.data.user.mapper.toEntity
import com.xwurfel.tourry.domain.user.model.User
import com.xwurfel.tourry.domain.user.model.UserRole
import com.xwurfel.tourry.domain.user.repository.UserRepository
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.firstOrNull
import kotlinx.coroutines.flow.map
import java.security.MessageDigest
import javax.inject.Inject

class UserRepositoryImpl @Inject constructor(
    private val userDao: UserDao
) : UserRepository {

    override suspend fun registerUser(user: User, password: String): Long {
        val passwordHash = hashPassword(password)
        return userDao.insertUser(user.toEntity(passwordHash))
    }

    override suspend fun authenticateUser(email: String, password: String): User? {
        val passwordHash = hashPassword(password)
        return userDao.authenticateUser(email, passwordHash)?.toDomain()
    }

    override fun getUserById(id: Long): Flow<User?> {
        return userDao.getUserById(id).map { it?.toDomain() }
    }

    override fun getUserByEmail(email: String): Flow<User?> {
        return userDao.getUserByEmail(email).map { it?.toDomain() }
    }

    override fun getAllUsers(): Flow<List<User>> {
        return userDao.getAllUsers().map { entities ->
            entities.map { it.toDomain() }
        }
    }

    override fun getUsersByRole(role: UserRole): Flow<List<User>> {
        return userDao.getUsersByRole(role).map { entities ->
            entities.map { it.toDomain() }
        }
    }

    override suspend fun updateUserRole(userId: Long, role: UserRole) {
        userDao.updateUserRole(userId, role)
    }

    override suspend fun updateUser(user: User, newPassword: String?) {
        val currentUserFlow = userDao.getUserById(user.id)
        val currentUser = currentUserFlow.map { it }.firstOrNull()

        currentUser?.let {
            val passwordHash = if (newPassword != null) {
                hashPassword(newPassword)
            } else {
                it.passwordHash
            }

            userDao.insertUser(user.toEntity(passwordHash))
        }
    }

    override suspend fun deleteUser(id: Long) {
        userDao.deleteUser(id)
    }

    private fun hashPassword(password: String): String {
        val bytes = password.toByteArray()
        val md = MessageDigest.getInstance("SHA-256")
        val digest = md.digest(bytes)
        return digest.fold("") { str, it -> str + "%02x".format(it) }
    }
}