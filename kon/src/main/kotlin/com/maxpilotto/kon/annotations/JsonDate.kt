package com.maxpilotto.kon.annotations

import com.maxpilotto.kon.JsonObject
import com.maxpilotto.kon.protocols.Json
import java.util.*

/**
 * Annotation used to customize a Date or Calendar type, subtype of the [JsonProperty]
 *
 * By default if no parameters are specified, the date will be read as a String and parsed using the default
 * date format value, which is [Json.DATE_FORMAT]
 *
 * @param format Format with which the Date/Calendar are written and read from and into a [JsonObject]
 * @param locale Locale used to parse the Date/Calendar, if empty [Locale.getDefault] will be used
 * @param isTimestamp If true, this Date/Calendar will be written/read as a timestamp, this will override the [format]
 */
@Target(AnnotationTarget.FIELD)
@Retention(AnnotationRetention.RUNTIME)
@MustBeDocumented
annotation class JsonDate(
    val format: String = Json.DATE_FORMAT,
    val locale: String = "",
    val isTimestamp: Boolean = false
)