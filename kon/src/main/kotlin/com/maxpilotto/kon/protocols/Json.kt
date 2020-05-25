/*
 * Copyright 2020 Max Pilotto
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *       http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package com.maxpilotto.kon.protocols

import com.maxpilotto.kon.JsonArray
import com.maxpilotto.kon.JsonObject
import com.maxpilotto.kon.JsonValue
import com.maxpilotto.kon.util.JsonException
import java.math.BigDecimal
import java.net.URL
import java.text.DateFormat
import java.text.SimpleDateFormat
import java.util.*

/**
 * Interface containing all the access operators used by
 * Json entities like the [JsonObject] and [JsonArray]
 */
abstract class Json {
    /**
     * Returns the value wrapped in a [JsonValue] for the given [key]
     *
     * @throws JsonException If the given [key] doesn't exist
     */
    open operator fun get(key: String): JsonValue {
        throw JsonException("Operation not supported")
    }

    /**
     * Returns the value wrapped in a [JsonValue] at the given [index]
     */
    open operator fun get(index: Int): JsonValue {
        throw JsonException("Operation not supported")
    }

    /**
     * Sets the given [element] for the given [key]
     *
     * If the [element] is a [JsonValue], the value will be unwrapped
     */
    open operator fun set(key: String, element: Any?) {
        throw JsonException("Operation not supported")
    }

    /**
     * Sets the given [element] associated with the given [index] and
     * returns the item previously at the given [index]
     *
     * If the [element] is a [JsonValue], the value will be unwrapped
     */
    open operator fun set(index: Int, element: Any?): Any? {
        throw JsonException("Operation not supported")
    }

    /**
     * Sets the given [date] associated with the given [key], using the [dateFormat] to
     * format it
     */
    operator fun set(key: String, dateFormat: DateFormat, date: Date) { //TODO Put the operators in different classes, like JsonObjectOperators and JsonArrayOperators
        set(key, dateFormat.format(date))
    }

    /**
     * Sets the given [calendar] associated with the given [key], using the [dateFormat] to
     * format it
     */
    operator fun set(key: String, dateFormat: DateFormat, calendar: Calendar) {
        set(key, dateFormat, calendar.time)
    }

    /**
     * Sets the given [date] associated with the given [key], using the [format] and [locale] to
     * format it
     */
    operator fun set(key: String, format: String, locale: Locale, date: Date) {
        set(key, SimpleDateFormat(format, locale), date)
    }

    /**
     * Sets the given [calendar] associated with the given [key], using the [format] and [locale] to
     * format it
     */
    operator fun set(key: String, format: String, locale: Locale, calendar: Calendar) {
        set(key, format, locale, calendar.time)
    }

    /**
     * Sets the given [date] associated with the given [key], using the [format] and the default
     * Locale to format it
     */
    operator fun set(key: String, format: String, date: Date) {
        set(key, format, Locale.getDefault(), date)
    }

    /**
     * Sets the given [calendar] associated with the given [key], using the [format] and the default
     * Locale to format it
     */
    operator fun set(key: String, format: String, calendar: Calendar) {
        set(key, format, Locale.getDefault(), calendar.time)
    }

    /**
     * Sets the given [date] associated with the given [index], using the [dateFormat]
     * to format it and returns the item previously at the given [index]
     *
     * The returned item will never be of type [Date] or [Calendar]
     */
    operator fun set(index: Int, dateFormat: DateFormat, date: Date): Any? {
        return set(index, dateFormat.format(date))
    }

    /**
     * Sets the given [calendar] associated with the given [index], using the [dateFormat]
     * to format it and returns the item previously at the given [index]
     *
     * The returned item will never be of type [Date] or [Calendar]
     */
    operator fun set(index: Int, dateFormat: DateFormat, calendar: Calendar): Any? {
        return set(index, dateFormat, calendar.time)
    }

    /**
     * Sets the given [date] associated with the given [index], using the [format] and the [locale]
     * to format it and returns the item previously at the given [index]
     *
     * The returned item will never be of type [Date] or [Calendar]
     */
    operator fun set(index: Int, format: String, locale: Locale, date: Date): Any? {
        return set(index, SimpleDateFormat(format, locale), date)
    }

    /**
     * Sets the given [calendar] associated with the given [index], using the [format] and the [locale]
     * to format it and returns the item previously at the given [index]
     *
     * The returned item will never be of type [Date] or [Calendar]
     */
    operator fun set(index: Int, format: String, locale: Locale, calendar: Calendar): Any? {
        return set(index, format, locale, calendar.time)
    }

    /**
     * Sets the given [date] associated with the given [index], using the [format] and the default
     * locale to format it and returns the item previously at the given [index]
     *
     * The returned item will never be of type [Date] or [Calendar]
     */
    operator fun set(index: Int, format: String, date: Date): Any? {
        return set(index, format, Locale.getDefault(), date)
    }

    /**
     * Sets the given [calendar] associated with the given [index], using the [format] and the default
     * locale to format it and returns the item previously at the given [index]
     *
     * The returned item will never be of type [Date] or [Calendar]
     */
    operator fun set(index: Int, format: String, calendar: Calendar): Any? {
        return set(index, format, Locale.getDefault(), calendar.time)
    }

    /**
     * Wraps or converts the given [value] to a type supported by the [JsonObject] and [JsonArray]
     *
     * This will unwrap the [value] if it is inside a [JsonValue]
     */
    protected fun wrap(value: Any?): Any? {
        if (value == null) {
            return value
        }

        if (value::class.java.isArray) {
            return JsonArray(value)
        }

        return when (value) {
            is JsonValue -> wrap(value.content)

            is Collection<*> -> JsonArray(value)
            is Map<*, *> -> JsonObject(value)

            is JsonObject,
            is JsonArray,
            is Int,
            is Short,
            is Long,
            is Double,
            is Float,
            is Byte,
            is BigDecimal,
            is Number,
            is String -> value

            is Calendar -> value.timeInMillis
            is Date -> value.time

            is Char,
            is Boolean,
            is IntRange,
            is URL,
            is Enum<*> -> value.toString()

            else -> throw JsonException("${value::class.java.simpleName} can't be added to a JsonArray or JsonObject")
        }
    }

    /**
     * Returns this entity as a pretty printable output
     */
    //TODO Add indent option
    //TODO Add params
    // showType, shows the type of the property
    // useBraces, shows {properties} instead of JsonObject(properties), enabled by default
    abstract fun prettify(): String

    companion object {
        const val DATE_FORMAT = "yyyy-MM-dd"
    }
}