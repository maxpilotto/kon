package com.maxpilotto.kon.processor.extensions

/**
 * Returns this String wrapped with the given [delimiter]
 */
internal fun String.wrap(delimiter: String): String {
    return wrap(delimiter,delimiter)
}

/**
 * Returns this String wrapped with the given [prefix] and [suffix]
 */
internal fun String.wrap(prefix: String, suffix: String): String {
    return prefix + this + suffix
}