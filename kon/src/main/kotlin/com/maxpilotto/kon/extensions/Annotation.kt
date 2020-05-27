package com.maxpilotto.kon.extensions   //TODO This file should be move to the processor module

import com.maxpilotto.kon.annotations.JsonDate
import com.maxpilotto.kon.annotations.JsonProperty
import com.maxpilotto.kon.protocols.Json
import java.util.*

/**
 * Returns the locale of this [JsonDate]
 *
 * If the locale is empty [Locale.getDefault] is returned
 */
fun JsonDate.getLocale(): String {  //TODO Should these be internal?
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

/**
 * Returns the name of this [JsonProperty] or null if the name
 * is empty or the annotation is null
 */
fun JsonProperty?.getName(): String? {
    return this?.let {
        if (it.name.isNotEmpty()) it.name else null
    }
}

/**
 * Returns the default value of this [JsonProperty] or an empty string
 * if this annotation is null
 */
fun JsonProperty?.getDefaultValue(): String {


    return this?.defaultValue ?: ""
}