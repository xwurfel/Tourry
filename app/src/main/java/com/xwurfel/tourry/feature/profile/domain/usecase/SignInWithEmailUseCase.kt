package com.xwurfel.tourry.feature.profile.domain.usecase

import com.xwurfel.tourry.core.domain.util.DomainResult
import com.xwurfel.tourry.feature.profile.domain.model.User
import com.xwurfel.tourry.feature.profile.domain.repository.UserRepository
import javax.inject.Inject

class SignInWithEmailUseCase @Inject constructor(
    private val userRepository: UserRepository
) {
    suspend operator fun invoke(email: String, password: String): DomainResult<User> =
        userRepository.signInWithEmailAndPassword(email, password)
}