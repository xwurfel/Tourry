package com.xwurfel.tourry.feature.profile.domain.usecase

import com.xwurfel.tourry.feature.profile.domain.repository.UserRepository
import kotlinx.coroutines.flow.Flow
import javax.inject.Inject

class ObserveAuthenticationStateUseCase @Inject constructor(
    private val userRepository: UserRepository
) {
    operator fun invoke(): Flow<Boolean> = userRepository.observeAuthenticationState()
}
