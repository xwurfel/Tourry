package com.xwurfel.tourry.core.domain.error

interface DomainErrorMapper {

    fun domainErrorFrom(tr: Throwable): DomainError
}