package com.xwurfel.tourry.core.domain.util

import android.content.res.Resources
import androidx.annotation.StringRes
import androidx.compose.runtime.Composable
import androidx.compose.ui.platform.LocalContext

sealed class UiText {
    data class Raw(val value: String) : UiText()

    data class Resource(@StringRes val id: Int, val args: List<Any> = emptyList()) : UiText()

    fun asString(resources: Resources): String {
        return when (this) {
            is Raw -> value
            is Resource -> resources.getString(id, *args.toTypedArray())
        }
    }
}

@Composable
fun UiText.asString(): String {
    return when (this) {
        is UiText.Raw -> value
        is UiText.Resource -> asString(LocalContext.current.resources)
    }
}
