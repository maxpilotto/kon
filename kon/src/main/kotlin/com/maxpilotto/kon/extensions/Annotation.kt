package com.maxpilotto.kon.extensions

import com.maxpilotto.kon.annotations.JsonDate
import com.maxpilotto.kon.protocols.Json
import java.util.*

fun JsonDate.getLocale(): Locale {  //TODO Return a string
    return if (locale.isEmpty()) {
        Locale.getDefault()
    } else {
        Locale.forLanguageTag(locale)
    }
}

fun JsonDate.getFormat(): String {
    return if (format.isEmpty()) {
        Json.DATE_FORMAT
    } else {
        format
    }
}