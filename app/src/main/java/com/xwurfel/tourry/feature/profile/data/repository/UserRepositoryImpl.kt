package com.xwurfel.tourry.feature.profile.data.repository

import com.xwurfel.tourry.core.domain.util.DomainResult
import com.xwurfel.tourry.core.domain.util.result
import com.xwurfel.tourry.feature.profile.domain.model.User
import com.xwurfel.tourry.feature.profile.domain.model.UserSettings
import com.xwurfel.tourry.feature.profile.domain.model.UserStats
import com.xwurfel.tourry.feature.profile.domain.repository.UserRepository
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.map
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class UserRepositoryImpl @Inject constructor() : UserRepository {

    private val _currentUser = MutableStateFlow<User?>(null)
    private val _isAuthenticated = MutableStateFlow(false)
    private val _userSettings = MutableStateFlow(UserSettings())

    override fun observeCurrentUser(): Flow<User?> = _currentUser.asStateFlow()

    override fun observeAuthenticationState(): Flow<Boolean> = _isAuthenticated.asStateFlow()

    override fun observeUserStats(): Flow<UserStats?> = _currentUser.map { it?.stats }

    override fun observeUserSettings(): Flow<UserSettings> = _userSettings.asStateFlow()

    override suspend fun signIn(): DomainResult<User> = result {
        delay(1000)

        val user = User(
            id = "dummy_user_${System.currentTimeMillis()}",
            name = "John Doe",
            email = "john.doe@example.com",
            avatarUrl = null,
        )

        val stats = UserStats(
            toursCreated = 3,
            toursJoined = 7
        )

        _currentUser.value = user.copy(stats = stats)
        _isAuthenticated.value = true

        user
    }

    override suspend fun signOut(): DomainResult<Unit> = result {
        delay(500)

        _currentUser.value = null
        _isAuthenticated.value = false
        _userSettings.value = UserSettings()
    }

    override suspend fun updateUserSettings(settings: UserSettings): DomainResult<Unit> = result {
        delay(300)

        _userSettings.value = settings
    }
}