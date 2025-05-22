package com.xwurfel.tourry.core.domain.error

import com.xwurfel.tourry.core.domain.util.UiText
import com.xwurfel.tourry.R

sealed class DomainError(val msg: UiText) {

    data object ConnectionError :
        DomainError(UiText.Resource(R.string.error_no_internet_connection))

    class UnauthorizedError(msg: String? = null) : DomainError(
        msg?.let { UiText.Raw(it) }
            ?: UiText.Resource(R.string.error_server_failed_to_respond)
    )

    data object UserHasNotAcceptedTncError :
        DomainError(UiText.Resource(R.string.error_server_failed_to_respond))

    class DuplicateEventError(msg: String?) : DomainError(
        msg?.let { UiText.Raw(it) }
            ?: UiText.Resource(R.string.error_server_failed_to_respond)
    )

    class SomethingWentWrongError(msg: UiText = UiText.Resource(R.string.error_server_failed_to_respond)) :
        DomainError(msg)
}