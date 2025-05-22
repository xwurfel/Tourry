package com.xwurfel.tourry.core.data.error.network

import okhttp3.Response

object NetworkErrorConverter {

    private const val STATUS_BAD_REQUEST_ERROR_CODE_400 = 400
    private const val STATUS_UNAUTHORIZED_ERROR_CODE_401 = 401
    private const val STATUS_FORBIDDEN_ERROR_CODE_403 = 403
    private const val STATUS_NOT_FOUND_ERROR_CODE_404 = 404
    private const val STATUS_INTERNAL_SERVER_ERROR_500 = 500

    fun networkErrorFrom(response: Response): NetworkError {
        return serverErrorFrom(response) ?: statusErrorFrom(response) ?: NetworkError.UnknownError
    }

    private fun serverErrorFrom(response: Response): NetworkError.ServerError? {
        return when {
            // Optionally add any server-specific errors you might have
            else -> null
        }
    }

    private fun statusErrorFrom(response: Response): NetworkError.StatusError? {
        return when (response.code) {
            STATUS_BAD_REQUEST_ERROR_CODE_400 -> NetworkError.StatusError.BadRequestError
            STATUS_UNAUTHORIZED_ERROR_CODE_401 -> NetworkError.StatusError.UnauthorizedError
            STATUS_FORBIDDEN_ERROR_CODE_403 -> NetworkError.StatusError.ForbiddenError
            STATUS_NOT_FOUND_ERROR_CODE_404 -> NetworkError.StatusError.NotFoundError
            STATUS_INTERNAL_SERVER_ERROR_500 -> NetworkError.StatusError.InternalSererErrorError
            else -> null
        }
    }
}
