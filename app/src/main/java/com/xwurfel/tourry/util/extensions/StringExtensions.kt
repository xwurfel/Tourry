package com.xwurfel.tourry.util.extensions

fun String.capitalizeFirstLetter(): String {
    return if (isEmpty()) {
        this
    } else {
        this.first().uppercase() + this.substring(1).lowercase()
    }
}