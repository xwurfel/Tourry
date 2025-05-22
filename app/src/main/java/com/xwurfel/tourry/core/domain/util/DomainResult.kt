package com.xwurfel.tourry.core.domain.util

import com.xwurfel.tourry.core.data.error.DomainErrorMapperImpl
import com.xwurfel.tourry.core.domain.error.DomainError
import com.xwurfel.tourry.core.domain.error.DomainErrorMapper
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.transform
import kotlin.coroutines.cancellation.CancellationException

sealed class DomainResult<out T : Any> {

    data class Success<out T : Any>(val data: T) : DomainResult<T>()
    data class Failure(val domainError: DomainError) : DomainResult<Nothing>()
}

val <T : Any> DomainResult<T>.isSuccess
    get() = this is DomainResult.Success

val <T : Any> DomainResult<T>.isFailure
    get() = this is DomainResult.Failure

inline fun <T : Any> DomainResult<T>.onSuccess(block: (T) -> Unit): DomainResult<T> {
    if (isSuccess) block((this as DomainResult.Success<T>).data)
    return this
}

inline fun <T : Any> DomainResult<T>.onFailure(block: (DomainError) -> Unit): DomainResult<T> {
    if (isFailure) block((this as DomainResult.Failure).domainError)
    return this
}

inline fun <T : Any> DomainResult<T>.getOr(block: (error: DomainResult.Failure) -> T): T {
    return when (this) {
        is DomainResult.Failure -> block(this)
        is DomainResult.Success -> this.data
    }
}

fun <T : Any> DomainResult<T>.getOrNull(): T? {
    return when (this) {
        is DomainResult.Failure -> null
        is DomainResult.Success -> this.data
    }
}

fun <R : Any, T : R> DomainResult<T>.getOrDefault(defaultValue: R): R {
    if (isFailure) return defaultValue
    return (this as DomainResult.Success<T>).data
}

inline fun <R : Any, T : Any> DomainResult<T>.map(transform: (value: T) -> R): DomainResult<R> {
    return when (this) {
        is DomainResult.Success -> DomainResult.Success(transform(data))
        is DomainResult.Failure -> DomainResult.Failure(domainError)
    }
}

/**
 * @throws CancellationException If the coroutine is cancelled while the function is executing.
 */
suspend fun <T : Any> result(
    errorMapper: DomainErrorMapper = DomainErrorMapperImpl(),
    block: suspend () -> T
): DomainResult<T> =
    try {
        DomainResult.Success(block.invoke())
    } catch (e: Throwable) {
        if (e is CancellationException) throw e
        DomainResult.Failure(errorMapper.domainErrorFrom(e))
    }

inline fun <T, R : Any> Flow<T>.toResult(
    errorMapper: DomainErrorMapper = DomainErrorMapperImpl(),
    crossinline transform: suspend (value: T) -> R
): Flow<DomainResult<R>> =
    transform { value ->
        try {
            val transformedValue = transform(value)
            emit(DomainResult.Success(transformedValue))
        } catch (e: Throwable) {
            if (e is CancellationException) throw e
            emit(DomainResult.Failure(errorMapper.domainErrorFrom(e)))
        }
    }