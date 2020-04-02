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
package com.maxpilotto.kon

import com.maxpilotto.kon.protocols.Json
import com.maxpilotto.kon.util.JsonException
import java.math.BigDecimal
import java.net.URL
import java.text.DateFormat
import java.text.SimpleDateFormat
import java.util.*
import kotlin.reflect.KClass
import kotlin.reflect.full.isSubclassOf

/**
 * Wrapper for every Json value
 *
 * This can be any of the following types
 * + JsonArray
 * + JsonObject
 * + Boolean
 * + String
 * + Number (BigDecimal, Int, Short, Float, ...)
 * + Date/Calendar (stored as String or Long)
 * + Char (stored as Int or String)
 * + IntRange (stored as Int or String)
 * + URL (stored as String)
 * + Enum (stored as String)
 * + List
 * + Generic object (transform method must be implemented)
 */
class JsonValue : Json {
    /**
     * Content of this [JsonValue]
     *
     * This can be cast/parsed using the various available methods (generally starting with 'as'),
     * alternatively it can be checked using the `is` operator and than cast using `as`
     */
    var content: Any?
        private set

    constructor(value: Any? = null) {
        this.content = if (value is JsonValue) {
            value.content
        } else {
            value
        }
    }

    override fun get(key: String): JsonValue {
        return asJsonObject()[key]
    }

    override fun get(index: Int): JsonValue {
        return asJsonArray()[index]
    }

    override fun set(key: String, element: Any?) {
        asJsonObject()[key] = wrap(element)
    }

    override fun set(index: Int, element: Any?): JsonValue {
        return asJsonArray().set(index, wrap(element))
    }

    override fun toString(): String {
        return when (content){
            is String -> "\"$content\""

            else -> content.toString()
        }
    }

    override fun hashCode(): Int {
        return content?.hashCode() ?: 0
    }

    override fun equals(other: Any?): Boolean {
        return if (other is JsonValue) {
            other.content == content
        } else {
            other == content
        }
    }

    /**
     * Returns whether or not the [content] is null
     *
     * If [stringCheck] is true, this will also check if the [content]
     * is equals to "null", ignoring the case
     */
    fun isNull(stringCheck: Boolean = true): Boolean {
        if (content == null) {
            return true
        } else if (content is String && stringCheck) {
            return (content as String).toLowerCase() == "null"
        }

        return false
    }

    /**
     * Returns this value as a String, if the value is not a String
     * then the [toString] method will be called
     */
    fun asString(): String {
        return if (content is String) {
            content as String
        } else {
            content.toString()
        }
    }

    /**
     * Returns this value as a generic Number
     *
     * If the value is not a Number, the method will try to parse it using the
     * result of the [toString] call on the [content]
     */
    fun asNumber(): Number {
        return when (content) {
            is Number -> content as Number
            is String -> try {
                BigDecimal(content.toString())
            } catch (e: Exception) {
                throw JsonException("Cannot parse value as Number: ${e.message}")
            }

            else -> throw JsonException("Value is not a Number")
        }
    }

    /**
     * Returns this value as a JsonObject
     */
    fun asJsonObject(): JsonObject {
        require(content is JsonObject) {
            JsonException("Value is not a JsonObject")
        }

        return content as JsonObject
    }

    /**
     * Returns this value as a JsonArray
     */
    fun asJsonArray(): JsonArray {
        require(content is JsonArray) {
            JsonException("Value is not a JsonArray")
        }

        return content as JsonArray
    }

    /**
     * Returns this value as an Int
     */
    fun asInt(): Int {
        return asNumber().toInt()
    }

    /**
     * Returns this value as an Long
     */
    fun asLong(): Long {
        return asNumber().toLong()
    }

    /**
     * Returns this value as a Boolean
     */
    fun asBoolean(): Boolean {
        require(content is Boolean || content is String || content is Number) {
            JsonException("Value is not a Boolean or Number")
        }

        return if (content is Boolean) {
            content as Boolean
        } else {
            asInt() != 0
        }
    }

    /**
     * Returns this value as a Double
     */
    fun asDouble(): Double {
        return asNumber().toDouble()
    }

    /**
     * Returns this value as a Float
     */
    fun asFloat(): Float {
        return asNumber().toFloat()
    }

    /**
     * Returns this value as a Byte
     */
    fun asByte(): Byte {
        return asNumber().toByte()
    }

    /**
     * Returns this value as a Short
     */
    fun asShort(): Short {
        return asNumber().toShort()
    }

    /**
     * Returns this value as a Char
     *
     * The value must be saved either as an Int or a String
     */
    fun asChar(): Char {
        return asNumber().toChar()
    }

    /**
     * Returns this value as a [Date]
     *
     * The value must be saved as a timestamp, either in a String or a Long
     */
    fun asDate(): Date {
        return Date(asLong())
    }

    /**
     * Returns this value as a [Date], using the given [dateFormat]
     *
     * The value must be saved as a String that matches the given [dateFormat]
     */
    fun asDate(dateFormat: DateFormat): Date {
        return try {
            dateFormat.parse(asString())
        } catch (e: Exception) {
            throw JsonException(e.message)
        }
    }

    /**
     * Returns this value as a [Date], using the given [format] and [locale] to parse
     * the date
     *
     * The value must be saved as a String that matches the given [format]
     */
    fun asDate(
        format: String,
        locale: Locale = Locale.getDefault()
    ): Date {
        return asDate(SimpleDateFormat(format, locale))
    }

    /**
     * Returns this value as a [Calendar]
     *
     * The value must be saved as a timestamp, either in a String or a Long
     */
    fun asCalendar(): Calendar {
        return Calendar.getInstance().apply {
            time = asDate()
        }
    }

    /**
     * Returns this value as a [Calendar], using the given [dateFormat]
     *
     * The value must be saved as a String that matches the given [dateFormat]
     */
    fun asCalendar(dateFormat: DateFormat): Calendar {
        return Calendar.getInstance().apply {
            time = asDate(dateFormat)
        }
    }

    /**
     * Returns this value as a [Calendar], using the given [format] and [locale] to parse
     * the date
     *
     * The value must be saved as a String that matches the given [format]
     */
    fun asCalendar(
        format: String,
        locale: Locale = Locale.getDefault()
    ): Calendar {
        return asCalendar(SimpleDateFormat(format, locale))
    }

    /**
     * Returns this value as an [IntRange]
     *
     * The value must be saved as a String or as an Int, the String must be formatted as "XX..XX"
     *
     * A single value will be parsed into 0..value
     */
    fun asRange(): IntRange {
        val regex = Regex("[0-9]+\\.\\.[0-9]+")

        return try {
            IntRange(0, asInt())
        } catch (e: Exception) {
            val s = asString()

            require(s.matches(regex)) {
                JsonException("Value cannot be parsed into IntRange")
            }

            with(s.split("..")) {
                IntRange(
                    get(0).toInt(),
                    get(1).toInt()
                )
            }
        }
    }

    /**
     * Returns this value as a [BigDecimal]
     */
    fun asBigDecimal(): BigDecimal {
        return try {
            BigDecimal(asString())
        } catch (e: Exception) {
            BigDecimal(asDouble())
        }
    }

    /**
     * Returns this value as an [URL]
     */
    fun asURL(): URL {
        return URL(asString())
    }

    /**
     * Returns this value as an Enum of type [T]
     *
     * The string value retrieved from the json will be capitalized by default,
     * if your enum class values are not capitalized, you can use the
     * [transform] block to customize it
     */
    inline fun <reified T : Enum<T>> asEnum(
        transform: (String) -> String = { it.capitalize() }
    ): T {
        return java.lang.Enum.valueOf(
            T::class.java,
            transform(asString())
        )
    }

    /**
     * Returns this value as an Array of [T], using the [transform] block
     * to parse the items
     */
    inline fun <T> asList(transform: (JsonValue) -> T): List<T> {
        return asJsonArray().toList(transform)
    }

    /**
     * Returns this value as an Object of type [T], the [transform] block
     * is used to parse the value
     *
     * Note that this only works if the value is a [JsonObject]
     */
    inline fun <T> asObject(transform: (JsonObject) -> T): T {
        return transform(asJsonObject())
    }

    /**
     * Returns this value as a List of wrapped values
     */
    fun asList(): List<JsonValue> {
        return asJsonArray().toList()
    }

    /**
     * Returns this value as a List of unwrapped values
     */
    fun asValueList(): List<Any?> {
        return asJsonArray().toValueList()
    }

    /**
     * Returns this value as a List of String
     */
    fun asStringList(): List<String> {
        return asJsonArray().toStringList()
    }

    /**
     * Returns this value as a List of Number
     */
    fun asNumberList(): List<Number> {
        return asJsonArray().toNumberList()
    }

    /**
     * Returns this value as a List of [JsonObject]
     */
    fun asJsonObjectList(): List<JsonObject> {
        return asJsonArray().toJsonObjectList()
    }

    /**
     * Returns this value as a List of Int
     */
    fun asIntList(): List<Int> {
        return asJsonArray().toIntList()
    }

    /**
     * Returns this value as a List of Long
     */
    fun asLongList(): List<Long> {
        return asJsonArray().toLongList()
    }

    /**
     * Returns this value as a List of Boolean
     */
    fun asBooleanList(): List<Boolean> {
        return asJsonArray().toBooleanList()
    }

    /**
     * Returns this value as a List of Double
     */
    fun asDoubleList(): List<Double> {
        return asJsonArray().toDoubleList()
    }

    /**
     * Returns this value as a List of Float
     */
    fun asFloatList(): List<Float> {
        return asJsonArray().toFloatList()
    }

    /**
     * Returns this value as a List of Byte
     */
    fun asByteList(): List<Byte> {
        return asJsonArray().toByteList()
    }

    /**
     * Returns this value as a List of Short
     */
    fun asShortList(): List<Short> {
        return asJsonArray().toShortList()
    }

    /**
     * Returns this value as a List of Char
     */
    fun asCharList(): List<Char> {
        return asJsonArray().toCharList()
    }

    /**
     * Returns this value as a List of [Date]
     */
    fun asDateList(): List<Date> {
        return asJsonArray().toDateList()
    }

    /**
     * Returns this value as a List of [Date]
     */
    fun asDateList(dateFormat: DateFormat): List<Date> {
        return asJsonArray().toDateList(dateFormat)
    }

    /**
     * Returns this value as a List of [Date]
     */
    fun asDateList(
        format: String,
        locale: Locale = Locale.getDefault()
    ): List<Date> {
        return asJsonArray().toDateList(format, locale)
    }

    /**
     * Returns this value as a List of [Calendar]
     */
    fun asCalendarList(): List<Calendar> {
        return asJsonArray().toCalendarList()
    }

    /**
     * Returns this value as a List of [Calendar]
     */
    fun asCalendarList(dateFormat: DateFormat): List<Calendar> {
        return asJsonArray().toCalendarList(dateFormat)
    }

    /**
     * Returns this value as a List of [Calendar]
     */
    fun asCalendarList(
        format: String,
        locale: Locale = Locale.getDefault()
    ): List<Calendar> {
        return asJsonArray().toCalendarList(format, locale)
    }

    /**
     * Returns this value as a List of [IntRange]
     */
    fun asRangeList(): List<IntRange> {
        return asJsonArray().toRangeList()
    }

    /**
     * Returns this value as a List of [BigDecimal]
     */
    fun asBigDecimalList(): List<BigDecimal> {
        return asJsonArray().toBigDecimalList()
    }

    /**
     * Returns this value as a List of [URL]
     */
    fun asURLList(): List<URL> {
        return asJsonArray().toURLList()
    }

    /**
     * Returns this value as a List of Enum of type [T]
     */
    inline fun <reified T : Enum<T>> asEnumList(
        transform: (String) -> String = { it.capitalize() }
    ): List<T> {
        return asJsonArray().toEnumList(transform)
    }

    /**
     * Returns this value as a List of objects of type [T]
     */
    inline fun <T> asObjectList(transform: (JsonObject) -> T): List<T> {
        return asJsonArray().toObjectList(transform)
    }

    companion object {
        /**
         * JsonValue that represents a Boolean of value true
         *
         * This is the same as instantiating JsonValue(true)
         */
        val TRUE = JsonValue(true)

        /**
         * JsonValue that represents a Boolean of value false
         *
         * This is the same as instantiating JsonValue(false)
         */
        val FALSE = JsonValue(false)

        /**
         * JsonValue that represents a null value
         *
         * This is the same as instantiating JsonValue(null)
         */
        val NULL = JsonValue(null)
    }
}