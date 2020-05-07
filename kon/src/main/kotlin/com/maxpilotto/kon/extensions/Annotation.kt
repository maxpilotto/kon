package com.maxpilotto.kon.extensions

import com.maxpilotto.kon.annotations.JsonDate
import com.maxpilotto.kon.protocols.Json
import java.util.*

/**
 * Returns the locale of this [JsonDate]
 *
 * If the locale is empty [Locale.getDefault] is returned
 */
fun JsonDate.getLocale(): String {
    return if (locale.isEmpty()) {
        Locale.getDefault().toString()
    } else {
        locale
    }
}

/**
 * Returns the format of this [JsonDate]
 *
 * If the format is empty [Json.DATE_FORMAT] is returned
 */
fun JsonDate.getFormat(): String {
    return if (format.isEmpty()) {
        Json.DATE_FORMAT
    } else {
        format
    }
}