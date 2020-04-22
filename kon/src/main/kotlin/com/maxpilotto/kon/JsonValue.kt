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

import com.maxpilotto.kon.extensions.prettify
import com.maxpilotto.kon.extensions.toJsonValue
import com.maxpilotto.kon.protocols.Json
import com.maxpilotto.kon.util.JsonException
import java.math.BigDecimal
import java.net.URL
import java.text.DateFormat
import java.util.*

/**
 * # JsonValue
 *
 * Wrapper for any value stored in a JsonObject or JsonArray,
 * this is used by the [JsonObject] and [JsonArray] get/set operators
 * so accessing through your json is easier
 *
 * This will allow you to do things like this
 *
 * ```kotlin
 *
 * val dob = json["people"][0]["data"]["dob"].asDate()
 *
 * vs.
 *
 * val dob = (json.getJsonArray("people").getValue(0) as JsonObject).getJsonObject("data").getDate("dob")    //TODO Test this
 * ```
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

    constructor(value: Any? = null) {       //TODO Limit the usage of the JsonValue to the library only
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
        asJsonObject()[key] = element.toJsonValue()
    }

    override fun set(index: Int, element: Any?): Any? {
        return asJsonArray().set(index, element)
    }

    override fun toString(): String {
        return content.toString()
    }

    override fun hashCode(): Int {
        return content?.hashCode() ?: 0
    }

    override fun equals(other: Any?): Boolean {
        return if (other is JsonValue) {
            if ((other.content == null && content == "null") ||
                (other.content == "null" && content == null)
            ) {
                true
            } else {
                other.content == content
            }
        } else {
            other == content
        }
    }

    override fun prettify(): String {
        return content.prettify()
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
        return cast(content)
    }

    /**
     * Returns this value as a generic Number
     */
    fun asNumber(): Number {
        return cast(content)
    }

    /**
     * Returns this value as a JsonObject
     */
    fun asJsonObject(): JsonObject {
        return cast(content)
    }

    /**
     * Returns this value as a JsonArray
     */
    fun asJsonArray(): JsonArray {
        return cast(content)
    }

    /**
     * Returns this value as an Int
     */
    fun asInt(): Int {
        return cast(content)
    }

    /**
     * Returns this value as an Long
     */
    fun asLong(): Long {
        return cast(content)
    }

    /**
     * Returns this value as a Boolean
     */
    fun asBoolean(): Boolean {
        return cast(content)
    }

    /**
     * Returns this value as a Double
     */
    fun asDouble(): Double {
        return cast(content)
    }

    /**
     * Returns this value as a Float
     */
    fun asFloat(): Float {
        return cast(content)
    }

    /**
     * Returns this value as a Byte
     */
    fun asByte(): Byte {
        return cast(content)
    }

    /**
     * Returns this value as a Short
     */
    fun asShort(): Short {
        return cast(content)
    }

    /**
     * Returns this value as a Char
     */
    fun asChar(): Char {
        return cast(content)
    }

    /**
     * Returns this value as a [Date]
     */
    fun asDate(): Date {
        return cast(content)
    }

    /**
     * Returns this value as a [Date] using the given [dateFormat] to parse the input
     *
     * If the value is not a String, this will just return the [Date] instance if possible
     */
    fun asDate(dateFormat: DateFormat): Date {
        return castDate(content, dateFormat)
    }

    /**
     * Returns this value as a [Date] using the given [format] and [locale] to input
     * the input
     *
     * If the value is not a String, this will just return the [Date] instance if possible
     */
    fun asDate(
        format: String,
        locale: Locale = Locale.getDefault()
    ): Date {
        return castDate(content, format, locale)
    }

    /**
     * Returns this value as a [Calendar]
     */
    fun asCalendar(): Calendar {
        return cast(content)
    }

    /**
     * Returns this value as a [Calendar] using the given [dateFormat] to parse the input
     *
     * If the value is not a String, this will just return the [Calendar] instance if possible
     */
    fun asCalendar(dateFormat: DateFormat): Calendar {
        return castDate(content, dateFormat)
    }

    /**
     * Returns this value as a [Calendar] using the given [format] and [locale] to parse
     * the input
     *
     * If the value is not a String, this will just return the [Calendar] instance if possible
     */
    fun asCalendar(
        format: String,
        locale: Locale = Locale.getDefault()
    ): Calendar {
        return castDate(content, format, locale)
    }

    /**
     * Returns this value as an [IntRange]
     */
    fun asRange(): IntRange {
        return cast(content)
    }

    /**
     * Returns this value as a [BigDecimal]
     */
    fun asBigDecimal(): BigDecimal {
        return cast(content)
    }

    /**
     * Returns this value as an [URL]
     */
    fun asURL(): URL {
        return cast(content)
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
     * Returns this value as an Enum of type [T]
     *
     * The [transform] block is used to decided how the Enum value should be
     * read and used to parse the enum
     */
    inline fun <reified T : Enum<T>> asEnum(
        transform: (String) -> String = { it.capitalize() }
    ): T {
        val clazz = T::class.java

        return if (content is T) {
            content as T
        } else if (content is Number) {
            val enums = clazz.enumConstants
            val ordinal = asInt()

            if (ordinal < enums.size) {
                enums[ordinal]
            } else {
                throw JsonException("Ordinal value out of range for enum ${clazz.simpleName}")
            }
        } else {
            java.lang.Enum.valueOf(
                clazz,
                transform(asString())
            )
        }
    }

    /**
     * Returns this value as a List of Any?
     */
    fun asList(): List<Any?> {
        return asJsonArray().toList()
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
     * Returns this value as an Array of [T], using the [transform] block
     * to parse the items
     */
    inline fun <T> asList(transform: (Any?) -> T): List<T> {
        return asJsonArray().toList(transform)
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
        @JvmStatic
        val TRUE = JsonValue(true)

        /**
         * JsonValue that represents a Boolean of value false
         *
         * This is the same as instantiating JsonValue(false)
         */
        @JvmStatic
        val FALSE = JsonValue(false)

        /**
         * JsonValue that represents a null value
         *
         * This is the same as instantiating JsonValue(null)
         */
        @JvmStatic
        val NULL = JsonValue(null)
    }
}