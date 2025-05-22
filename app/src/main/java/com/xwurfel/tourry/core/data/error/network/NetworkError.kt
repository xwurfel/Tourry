package com.xwurfel.tourry.core.data.error.network

import java.io.IOException

sealed class NetworkError : IOException() {

    data object ConnectionError : NetworkError()

    data object UnknownError : NetworkError()

    sealed class StatusError : NetworkError() {

        data object BadRequestError : StatusError()

        data object UnauthorizedError : StatusError()

        data object ForbiddenError : StatusError()

        data object NotFoundError : StatusError()

        data object InternalSererErrorError : StatusError()
    }

    sealed class ServerError : NetworkError() {

        data object UserHasNotAcceptedTnc : ServerError()

        data object DuplicateEvent : ServerError()
    }
}