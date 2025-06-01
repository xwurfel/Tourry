package com.xwurfel.tourry.feature.profile.domain.usecase

import com.xwurfel.tourry.feature.profile.domain.repository.UserRepository
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import javax.inject.Inject

class GetCurrentUserIdUseCase @Inject constructor(
    private val userRepository: UserRepository
) {
    operator fun invoke(): Flow<String?> {
        return userRepository.observeCurrentUser().map { user -> user?.id }
    }
}