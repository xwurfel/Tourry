package com.xwurfel.tourry.core.data.error

import com.xwurfel.tourry.core.data.error.network.NetworkError
import com.xwurfel.tourry.core.domain.error.DomainError
import com.xwurfel.tourry.core.domain.error.DomainErrorMapper
import timber.log.Timber

open class DomainErrorMapperImpl : DomainErrorMapper {

    override fun domainErrorFrom(tr: Throwable): DomainError {
        val domainError = when (tr) {
            is NetworkError.ConnectionError -> DomainError.ConnectionError
            is NetworkError.StatusError -> mapFromStatusError(tr)
            is NetworkError.ServerError -> mapFromServerError(tr)
            else -> DomainError.SomethingWentWrongError()
        }
        Timber.i("convert ${tr.javaClass.simpleName} to ${domainError.javaClass.simpleName}")
        return domainError
    }

    private fun mapFromStatusError(error: NetworkError.StatusError): DomainError {
        return when (error) {
            is NetworkError.StatusError.UnauthorizedError -> DomainError.UnauthorizedError(error.message)
            else -> DomainError.SomethingWentWrongError()
        }
    }

    private fun mapFromServerError(error: NetworkError.ServerError): DomainError {
        return when (error) {
            is NetworkError.ServerError.UserHasNotAcceptedTnc -> DomainError.UserHasNotAcceptedTncError
            is NetworkError.ServerError.DuplicateEvent -> DomainError.DuplicateEventError(error.message)
        }
    }
}