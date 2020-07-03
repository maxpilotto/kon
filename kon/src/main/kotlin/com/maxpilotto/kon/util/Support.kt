package com.maxpilotto.kon.util

import java.net.URL
import java.util.*

/**
 * Returns a Locale instance using the given [tag]
 */
fun localeFor(tag: String): Locale {
    require(tag.matches(Regex("[a-zA-z]{2,}[,_-]?[a-zA-z]*"))) {
        JsonException("Locale doesn't match the supported formats")
    }

    return Locale.forLanguageTag(
        tag.replace(
            Regex("[,_]"),
            "-"
        )
    )
}

/**
 * Returns a stringified version of given [value] in a Json supported
 * format
 *
 * This will wrap strings and chars around the double quotes
 */
fun stringify(value: Any?): String {
    return when (value) {
        is IntRange,
        is Enum<*>,
        is URL,
        is String -> "\"$value\""

        else -> value.toString()
    }
}

/**
 * Returns whether or not the given [value] is null
 *
 * A value is considered null/optional when it's equal to one of the following:
 * + null
 * + "null" (not case sensitive)
 * + "" (empty string)
 */
internal fun isNull(value: Any?): Boolean {
    return if (value is String) {
        value.equals("null",true) ||
                value.isEmpty()
    } else {
        value == null
    }
}

/**
 * Creates an instance of [Calendar] with the given [time],
 * which must be expressed in milliseconds
 */
internal fun calendarOf(time: Long): Calendar {
    return Calendar.getInstance().apply {
        timeInMillis = time
    }
}

/**
 * Creates an instance of [Calendar] with the given [date]
 */
internal fun calendarOf(date: Date): Calendar {
    return Calendar.getInstance().apply {
        time = date
    }
}