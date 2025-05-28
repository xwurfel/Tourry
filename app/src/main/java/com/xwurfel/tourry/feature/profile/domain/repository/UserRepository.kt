package com.xwurfel.tourry.feature.profile.domain.repository

import com.xwurfel.tourry.core.domain.util.DomainResult
import com.xwurfel.tourry.feature.profile.domain.model.User
import com.xwurfel.tourry.feature.profile.domain.model.UserSettings
import com.xwurfel.tourry.feature.profile.domain.model.UserStats
import kotlinx.coroutines.flow.Flow

interface UserRepository {
    fun observeCurrentUser(): Flow<User?>
    fun observeAuthenticationState(): Flow<Boolean>
    fun observeUserStats(): Flow<UserStats?>
    fun observeUserSettings(): Flow<UserSettings>

    suspend fun signIn(): DomainResult<User>
    suspend fun signOut(): DomainResult<Unit>
    suspend fun updateUserSettings(settings: UserSettings): DomainResult<Unit>
}