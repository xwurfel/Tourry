package com.xwurfel.tourry.feature.profile.domain.usecase

import com.xwurfel.tourry.core.domain.util.DomainResult
import com.xwurfel.tourry.feature.profile.domain.model.User
import com.xwurfel.tourry.feature.profile.domain.repository.UserRepository
import javax.inject.Inject

class CreateAccountUseCase @Inject constructor(
    private val userRepository: UserRepository
) {
    suspend operator fun invoke(email: String, password: String, name: String): DomainResult<User> =
        userRepository.createUserWithEmailAndPassword(email, password, name)
}