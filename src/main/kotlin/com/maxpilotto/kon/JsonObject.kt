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

import com.maxpilotto.kon.extensions.Calendar
import com.maxpilotto.kon.protocols.Json
import com.maxpilotto.kon.util.JsonException
import java.math.BigDecimal
import java.net.URL
import java.text.DateFormat
import java.util.*

class JsonObject : Json {   //TODO Add value observer
    private var map: MutableMap<String, JsonValue>

    /**
     * Returns a [MutableSet] of all key/value pairs in this object
     */
    val entries: MutableSet<MutableMap.MutableEntry<String, JsonValue>>
        get() = map.entries

    /**
     * Returns a [MutableSet] of all keys in this map
     */
    val keys: MutableSet<String>
        get() = map.keys

    /**
     * Returns a MutableCollection of all values in this map.
     *
     * Note that this collection may contain duplicate values
     */
    val values: MutableCollection<JsonValue>
        get() = map.values

    /**
     * Returns the number of key/value pairs in this object
     *
     * Note that this is the number of pairs in the root of this object
     */
    val size: Int
        get() = map.size

    constructor() : this(hashMapOf())

    constructor(json: String) : this(JsonParser(json).nextObject())

    constructor(jsonObject: JsonObject) : this(jsonObject.map)

    constructor(map: MutableMap<String, JsonValue>) {
        this.map = map
    }

    override fun toString(): String {   //TODO Add prettifier
        return map.entries.joinToString(",","{","}",transform = {
            "${it.key}:${it.value}"
        })
    }

    override fun get(key: String): JsonValue {
        return map[key] ?: throw JsonException("Key '$key' was not found")
    }

    override fun set(key: String, element: Any?) {
        map[key] = wrap(element)
    }

    /**
     * Returns an optional value for the given [key] or [default] if
     * the value is null or the [key] doesn't exist
     *
     * The [default] can be of any type and it will wrapped if needed
     */
    fun opt(
        key: String,
        default: Any?
    ): JsonValue {
        val value = map[key]

        if (value == null || value.isNull()) {
            return wrap(default)
        }

        return value
    }

    /**
     * Returns the unwrapped value for the given [key]
     *
     * @throws JsonException If the [key] doesn't exist
     */
    fun getValue(key: String): Any? {
        return unwrap(get(key))
    }

    /**
     * Returns the unwrapped optional value for the given [key] or [default] if the value
     * is null or if the [key] doesn't exist
     */
    fun optValue(
        key: String,
        default: Any?
    ): Any? {
        return unwrap(opt(key, default))     //FIXME The default is wrapped and then again unwrapped
    }

    /**
     * Returns whether or not this map is empty
     */
    fun isEmpty(): Boolean {
        return map.isEmpty()
    }

    /**
     * Removes all elements from this object
     */
    fun clear() {
        map.clear()
    }

    /**
     * Removes the specified [key] and its corresponding value from this map
     *
     * @throws JsonException if the [key] was not found
     */
    fun remove(key: String): JsonValue {
        return map.remove(key) ?: throw JsonException("Key '$key' was not found")
    }

    /**
     * Removes the key and its corresponding value at the given [path]
     *
     * The [path] must contain Int and String only
     *
     * @throws JsonException If any of the keys in the [path] do not exist
     * @throws JsonException If any of the key is not a String or an Int
     * @throws JsonException If the second-last value is not a [JsonObject] or [JsonArray]
     */
    fun remove(vararg path: Any) {
        val last = path.last()
        var current = wrap(this)

        for (i in 0 until path.lastIndex) {
            val key = path[i]

            current = when (key) {
                is String -> current[key]
                is Int -> current[key]

                else -> throw JsonException("Key '$key' is not a String or an Int")
            }
        }

        when (current.content) {
            is JsonObject -> {
                if (last is String) {
                    current.asJsonObject().remove(last)
                } else {
                    throw JsonException("Last key in path must be a String")
                }
            }
            is JsonArray -> {
                if (last is Int) {
                    current.asJsonArray().removeAt(last)
                } else {
                    throw JsonException("Last key in path must be an Int")
                }
            }

            else -> throw JsonException("The value for the key '$last' is neither a JsonObject nor a JsonArray")
        }
    }

    /**
     * Returns whether or not the given [key] exists
     */
    fun has(key: String): Boolean {
        return map.containsKey(key)
    }

    /**
     * Returns whether or not the given [value] is present
     */
    fun has(value: Any?): Boolean {
        return map.containsValue(wrap(value))
    }

    /**
     * Returns this [JsonObject] as a Map
     */
    fun toMap(): Map<String, JsonValue> {
        return map.toMap()
    }

    /**
     * Returns the String for the given [key]
     *
     * @throws JsonException If the given key doesn't exist
     */
    fun getString(key: String): String {
        return get(key).asString()
    }

    /**
     * Returns an optional Int for the given [key] or [default]
     * if the value is null or the [key] doesn't exist
     *
     * By default the [default] value is an empty String
     */
    fun optString(
        key: String,
        default: String? = ""
    ): String? {
        return opt(key, default).asString()
    }

    /**
     * Returns the Number for the given [key]
     *
     * @throws JsonException If the given key doesn't exist
     */
    fun getNumber(key: String): Number {
        return get(key).asNumber()
    }

    /**
     * Returns an optional Number for the given [key] or [default]
     * if the value is null or the [key] doesn't exist
     *
     * By default the [default] value is 0
     */
    fun optNumber(
        key: String,
        default: Number? = 0
    ): Number? {
        return opt(key, default).asNumber()
    }


    /**
     * Returns the [JsonObject] for the given [key]
     *
     * @throws JsonException If the given key doesn't exist
     */
    fun getJsonObject(key: String): JsonObject {
        return get(key).asJsonObject()
    }

    /**
     * Returns an optional [JsonObject] for the given [key] or [default]
     * if the value is null or the [key] doesn't exist
     */
    fun optJsonObject(
        key: String,
        default: JsonObject?
    ): JsonObject? {
        return opt(key, default).asJsonObject()
    }

    /**
     * Returns the [JsonArray] for the given [key]
     *
     * @throws JsonException If the given key doesn't exist
     */
    fun getJsonArray(key: String): JsonArray {
        return get(key).asJsonArray()
    }

    /**
     * Returns an optional String for the given [key] or [default]
     * if the value is null or the [key] doesn't exist
     *
     * By default the [default] value is 0
     */
    fun optJsonArray(
        key: String,
        default: JsonArray?
    ): JsonArray? {
        return opt(key, default).asJsonArray()
    }

    /**
     * Returns the Int value for the given [key]
     *
     * @throws JsonException If the given key doesn't exist
     */
    fun getInt(key: String): Int {
        return get(key).asInt()
    }

    /**
     * Returns an optional Int value for the given [key] or [default]
     * if the value is null or the [key] doesn't exist
     *
     * By default the [default] value is 0
     */
    fun optInt(
        key: String,
        default: Int? = 0
    ): Int? {
        return opt(key, default).asInt()
    }

    /**
     * Returns the Long value for the given [key]
     *
     * @throws JsonException If the given key doesn't exist
     */
    fun getLong(key: String): Long {
        return get(key).asLong()
    }

    /**
     * Returns an optional Long value for the given [key] or [default]
     * if the value is null or the [key] doesn't exist
     *
     * By default the [default] value is 0
     */
    fun optLong(
        key: String,
        default: Long? = 0L
    ): Long? {
        return opt(key, default).asLong()
    }

    /**
     * Returns the Boolean value for the given [key]
     *
     * @throws JsonException If the given key doesn't exist
     */
    fun getBoolean(key: String): Boolean {
        return get(key).asBoolean()
    }

    /**
     * Returns an optional Boolean value for the given [key] or [default]
     * if the value is null or the [key] doesn't exist
     *
     * By default the [default] value is false
     */
    fun optBoolean(
        key: String,
        default: Boolean? = false
    ): Boolean? {
        return opt(key, default).asBoolean()
    }

    /**
     * Returns the Double value for the given [key]
     *
     * @throws JsonException If the given key doesn't exist
     */
    fun getDouble(key: String): Double {
        return get(key).asDouble()
    }

    /**
     * Returns an optional Double value for the given [key] or [default]
     * if the value is null or the [key] doesn't exist
     *
     * By default the [default] value is 0.0
     */
    fun optDouble(
        key: String,
        default: Double? = 0.0
    ): Double? {
        return opt(key, default).asDouble()
    }

    /**
     * Returns the Float value for the given [key]
     *
     * @throws JsonException If the given key doesn't exist
     */
    fun getFloat(key: String): Float {
        return get(key).asFloat()
    }

    /**
     * Returns an optional Float value for the given [key] or [default]
     * if the value is null or the [key] doesn't exist
     *
     * By default the [default] value is 0.0
     */
    fun optFloat(
        key: String,
        default: Float? = 0F
    ): Float? {
        return opt(key, default).asFloat()
    }

    /**
     * Returns the Byte value for the given [key]
     *
     * @throws JsonException If the given key doesn't exist
     */
    fun getByte(key: String): Byte {
        return get(key).asByte()
    }

    /**
     * Returns an optional Byte value for the given [key] or [default]
     * if the value is null or the [key] doesn't exist
     *
     * By default the [default] value is 0
     */
    fun optByte(
        key: String,
        default: Byte? = 0
    ): Byte? {
        return opt(key, default).asByte()
    }

    /**
     * Returns the Short value for the given [key]
     *
     * @throws JsonException If the given key doesn't exist
     */
    fun getShort(key: String): Short {
        return get(key).asShort()
    }

    /**
     * Returns an optional Short value for the given [key] or [default]
     * if the value is null or the [key] doesn't exist
     *
     * By default the [default] value is 0
     */
    fun optShort(
        key: String,
        default: Short? = 0
    ): Short? {
        return opt(key, default).asShort()
    }

    /**
     * Returns the Char value for the given [key]
     *
     * @throws JsonException If the given key doesn't exist
     */
    fun getChar(key: String): Char {
        return get(key).asChar()
    }

    /**
     * Returns an optional Char value for the given [key] or [default]
     * if the value is null or the [key] doesn't exist
     */
    fun optChar(
        key: String,
        default: Char?
    ): Char? {
        return opt(key, default).asChar()
    }

    /**
     * Returns the [Date], saved as a timestamp, for the given [key]
     *
     * @throws JsonException If the given key doesn't exist
     */
    fun getDate(key: String): Date {
        return get(key).asDate()
    }

    /**
     * Returns an optional [Date], saved as a timestamp, for the given [key] or [default]
     * if the value is null or the [key] doesn't exist
     *
     * The [default] value can be a Date or any Number type
     *
     * By default the [default] value is 0
     *
     * @throws JsonException If the default value is not a [Date] or a Number type
     */
    fun optDate(
        key: String,
        default: Any? = 0
    ): Date? {
        return when (default) {
            is Date -> opt(key, default)
            is Number -> opt(key, Date(default.toLong()))

            else -> throw JsonException("The default value must be either a Date or a Number type")
        }.asDate()
    }

    /**
     * Returns the [Date] for the given [key]
     *
     * @throws JsonException If the given key doesn't exist
     */
    fun getDate(
        key: String,
        dateFormat: DateFormat
    ): Date {
        return get(key).asDate(dateFormat)
    }

    /**
     * Returns an optional [Date] for the given [key] or [default]
     * if the value is null or the [key] doesn't exist
     *
     * The [default] value can be a Date or any Number type
     *
     * By default the [default] value is 0
     *
     * @throws JsonException If the default value is not a [Date] or a Number type
     */
    fun optDate(
        key: String,
        dateFormat: DateFormat,
        default: Any? = 0
    ): Date? {
        return when (default) {
            is Date -> opt(key, default)
            is Number -> opt(key, Date(default.toLong()))

            else -> throw JsonException("The default value must be either a Date or a Number type")
        }.asDate(dateFormat)
    }

    /**
     * Returns the [Date] for the given [key]
     *
     * @throws JsonException If the given key doesn't exist
     */
    fun getDate(
        key: String,
        format: String,
        locale: Locale = Locale.getDefault()
    ): Date {
        return get(key).asDate(format, locale)
    }

    /**
     * Returns an optional [Date] saved as a timestamp, for the given [key] or [default]
     * if the value is null or the [key] doesn't exist
     *
     * The [default] value can be a Date or any Number type
     *
     * By default the [default] value is 0
     *
     * @throws JsonException If the default value is not a [Date] or a Number type
     */
    fun optDate(
        key: String,
        format: String,
        locale: Locale = Locale.getDefault(),
        default: Any? = 0
    ): Date? {
        return when (default) {
            is Date -> opt(key, default)
            is Number -> opt(key, Date(default.toLong()))

            else -> throw JsonException("The default value must be either a Date or a Number type")
        }.asDate(format, locale)
    }

    /**
     * Returns the [Calendar], saved as a timestamp, for the given [key]
     *
     * @throws JsonException If the given key doesn't exist
     */
    fun getCalendar(key: String): Calendar {
        return get(key).asCalendar()
    }

    /**
     * Returns an optional [Calendar], saved as a timestamp, for the given [key] or [default]
     * if the value is null or the [key] doesn't exist
     *
     * The [default] value can be a [Calendar] or any Number type
     *
     * By default the [default] value is 0
     *
     * @throws JsonException If the default value is not a [Calendar] or a Number type
     */
    fun optCalendar(
        key: String,
        default: Any? = 0
    ): Calendar? {
        return when (default) {
            is Calendar -> opt(key, default)
            is Number -> opt(key, Calendar(default.toLong()))

            else -> throw JsonException("The default value must be either a Calendar or a Number type")
        }.asCalendar()
    }

    /**
     * Returns the [Calendar] for the given [key]
     *
     * @throws JsonException If the given key doesn't exist
     */
    fun getCalendar(
        key: String,
        dateFormat: DateFormat
    ): Calendar {
        return get(key).asCalendar(dateFormat)
    }

    /**
     * Returns an optional [Calendar] for the given [key] or [default]
     * if the value is null or the [key] doesn't exist
     *
     * The [default] value can be a [Calendar] or any Number type
     *
     * By default the [default] value is 0
     *
     * @throws JsonException If the default value is not a [Calendar] or a Number type
     */
    fun optCalendar(
        key: String,
        dateFormat: DateFormat,
        default: Any? = 0
    ): Calendar? {
        return when (default) {
            is Calendar -> opt(key, default)
            is Number -> opt(key, Calendar(default.toLong()))

            else -> throw JsonException("The default value must be either a Calendar or a Number type")
        }.asCalendar(dateFormat)
    }

    /**
     * Returns the [Calendar] for the given [key]
     *
     * @throws JsonException If the given key doesn't exist
     */
    fun getCalendar(
        key: String,
        format: String,
        locale: Locale = Locale.getDefault()
    ): Calendar {
        return get(key).asCalendar(format, locale)
    }

    /**
     * Returns an optional [Calendar] for the given [key] or [default]
     * if the value is null or the [key] doesn't exist
     *
     * The [default] value can be a [Calendar] or any Number type
     *
     * By default the [default] value is 0
     *
     * @throws JsonException If the default value is not a [Calendar] or a Number type
     */
    fun optCalendar(
        key: String,
        format: String,
        locale: Locale = Locale.getDefault(),
        default: Any? = 0
    ): Calendar? {
        return when (default) {
            is Calendar -> opt(key, default)
            is Number -> opt(key, Calendar(default.toLong()))

            else -> throw JsonException("The default value must be either a Calendar or a Number type")
        }.asCalendar(format, locale)
    }

    /**
     * Returns the [IntRange] for the given [key]
     *
     * @throws JsonException If the given key doesn't exist
     */
    fun getRange(key: String): IntRange {
        return get(key).asRange()
    }

    /**
     * Returns an optional [IntRange] for the given [key] or [default]
     * if the value is null or the [key] doesn't exist
     */
    fun optRange(
        key: String,
        default: IntRange?
    ): IntRange? {
        return opt(key, default).asRange()
    }

    /**
     * Returns the [BigDecimal] for the given [key]
     *
     * @throws JsonException If the given key doesn't exist
     */
    fun getBigDecimal(key: String): BigDecimal {
        return get(key).asBigDecimal()
    }

    /**
     * Returns an optional [BigDecimal] for the given [key] or [default]
     * if the value is null or the [key] doesn't exist
     */
    fun optBigDecimal(
        key: String,
        default: BigDecimal?
    ): BigDecimal? {
        return opt(key, default).asBigDecimal()
    }

    /**
     * Returns the [URL] for the given [key]
     *
     * @throws JsonException If the given key doesn't exist
     */
    fun getURL(key: String): URL {
        return get(key).asURL()
    }

    /**
     * Returns an optional [IntRange] for the given [key] or [default]
     * if the value is null or the [key] doesn't exist
     */
    fun optURL(
        key: String,
        default: URL?
    ): URL? {
        return opt(key, default).asURL()
    }

    /**
     * Returns the Enum of type [T] for the given [key]
     *
     * @throws JsonException If the given key doesn't exist
     */
    inline fun <reified T : Enum<T>> getEnum(
        key: String,
        transform: (String) -> String = { it.capitalize() }
    ): T {
        return get(key).asEnum(transform)
    }

    /**
     * Returns an optional Enum of type [T] for the given [key] or [default]
     * if the value is null or the [key] doesn't exist
     *
     * THe [default] value can be an Enum of type [T] or a String
     */
    inline fun <reified T : Enum<T>> optEnum(
        key: String,
        default: Any,
        transform: (String) -> String = { it.capitalize() }
    ): T {
        require(default is T || default is String) {
            JsonException("The default value must be either an Enum value or a String")
        }

        return opt(key, default).asEnum(transform)
    }

    /**
     * Returns the List of [JsonValue] for the given [key]
     *
     * @throws JsonException If the given key doesn't exist
     */
    fun getList(key: String): List<JsonValue> {
        return get(key).asList()
    }

    /**
     * Returns an optional List of [JsonValue] for the given [key] or [default]
     * if the value is null or the [key] doesn't exist
     *
     * By default the [default] value is an empty list
     */
    fun optList(
        key: String,
        default: List<JsonValue>? = listOf()
    ): List<JsonValue>? {
        return opt(key, default).asList()
    }

    /**
     * Returns the List of unwrapped values for the given [key]
     *
     * @throws JsonException If the given key doesn't exist
     */
    fun getValueList(key: String): List<Any?> {
        return get(key).asValueList()
    }

    /**
     * Returns an optional List of unwrapped values for the given [key] or [default]
     * if the value is null or the [key] doesn't exist
     *
     * By default the [default] value is an empty list
     */
    fun optValueList(
        key: String,
        default: List<Any?>? = emptyList()
    ): List<Any?>? {
        return opt(key, default).asValueList()
    }

    /**
     * Returns the List of String for the given [key]
     *
     * @throws JsonException If the given key doesn't exist
     */
    fun getStringList(key: String): List<String> {
        return get(key).asStringList()
    }

    /**
     * Returns an optional List of String for the given [key] or [default]
     * if the value is null or the [key] doesn't exist
     *
     * By default the [default] value is an empty list
     */
    fun optStringList(
        key: String,
        default: List<String>? = emptyList()
    ): List<String>? {
        return opt(key, default).asStringList()
    }

    /**
     * Returns the List of Number for the given [key]
     *
     * @throws JsonException If the given key doesn't exist
     */
    fun getNumberList(key: String): List<Number> {
        return get(key).asNumberList()
    }

    /**
     * Returns an optional List of Number for the given [key] or [default]
     * if the value is null or the [key] doesn't exist
     *
     * By default the [default] value is an empty list
     */
    fun optNumberList(
        key: String,
        default: List<Number>? = emptyList()
    ): List<Number>? {
        return opt(key, default).asNumberList()
    }

    /**
     * Returns the List of [JsonObject] for the given [key]
     *
     * @throws JsonException If the given key doesn't exist
     */
    fun getJsonObjectList(key: String): List<JsonObject> {
        return get(key).asJsonObjectList()
    }

    /**
     * Returns the List of Int for the given [key]
     *
     * @throws JsonException If the given key doesn't exist
     */
    fun getIntList(key: String): List<Int> {
        return get(key).asIntList()
    }

    /**
     * Returns an optional List of Int for the given [key] or [default]
     * if the value is null or the [key] doesn't exist
     *
     * By default the [default] value is an empty list
     */
    fun optIntList(
        key: String,
        default: List<Int>? = emptyList()
    ): List<Int>? {
        return opt(key, default).asIntList()
    }

    /**
     * Returns the List of Long for the given [key]
     *
     * @throws JsonException If the given key doesn't exist
     */
    fun getLongList(key: String): List<Long> {
        return get(key).asLongList()
    }

    /**
     * Returns an optional List of Long for the given [key] or [default]
     * if the value is null or the [key] doesn't exist
     *
     * By default the [default] value is an empty list
     */
    fun optLongList(
        key: String,
        default: List<Long>? = emptyList()
    ): List<Long>? {
        return opt(key, default).asLongList()
    }

    /**
     * Returns the List of Boolean for the given [key]
     *
     * @throws JsonException If the given key doesn't exist
     */
    fun getBooleanList(key: String): List<Boolean> {
        return get(key).asBooleanList()
    }

    /**
     * Returns an optional List of Boolean for the given [key] or [default]
     * if the value is null or the [key] doesn't exist
     *
     * By default the [default] value is an empty list
     */
    fun optBooleanList(
        key: String,
        default: List<Boolean>? = emptyList()
    ): List<Boolean>? {
        return opt(key, default).asBooleanList()
    }

    /**
     * Returns the List of Double for the given [key]
     *
     * @throws JsonException If the given key doesn't exist
     */
    fun getDoubleList(key: String): List<Double> {
        return get(key).asDoubleList()
    }

    /**
     * Returns an optional List of Double for the given [key] or [default]
     * if the value is null or the [key] doesn't exist
     *
     * By default the [default] value is an empty list
     */
    fun optDoubleList(
        key: String,
        default: List<Double>? = emptyList()
    ): List<Double>? {
        return opt(key, default).asDoubleList()
    }

    /**
     * Returns the List of Float for the given [key]
     *
     * @throws JsonException If the given key doesn't exist
     */
    fun getFloatList(key: String): List<Float> {
        return get(key).asFloatList()
    }

    /**
     * Returns an optional List of Float for the given [key] or [default]
     * if the value is null or the [key] doesn't exist
     *
     * By default the [default] value is an empty list
     */
    fun optFloatList(
        key: String,
        default: List<Float>? = emptyList()
    ): List<Float>? {
        return opt(key, default).asFloatList()
    }

    /**
     * Returns the List of Byte for the given [key]
     *
     * @throws JsonException If the given key doesn't exist
     */
    fun getByteList(key: String): List<Byte> {
        return get(key).asByteList()
    }

    /**
     * Returns an optional List of Byte for the given [key] or [default]
     * if the value is null or the [key] doesn't exist
     *
     * By default the [default] value is an empty list
     */
    fun optByteList(
        key: String,
        default: List<Byte>? = emptyList()
    ): List<Byte>? {
        return opt(key, default).asByteList()
    }

    /**
     * Returns the List of Short for the given [key]
     *
     * @throws JsonException If the given key doesn't exist
     */
    fun getShortList(key: String): List<Short> {
        return get(key).asShortList()
    }

    /**
     * Returns an optional List of Short for the given [key] or [default]
     * if the value is null or the [key] doesn't exist
     *
     * By default the [default] value is an empty list
     */
    fun optShortList(
        key: String,
        default: List<Short>? = emptyList()
    ): List<Short>? {
        return opt(key, default).asShortList()
    }

    /**
     * Returns the List of Char for the given [key]
     *
     * @throws JsonException If the given key doesn't exist
     */
    fun getCharList(key: String): List<Char> {
        return get(key).asCharList()
    }

    /**
     * Returns an optional List of Char for the given [key] or [default]
     * if the value is null or the [key] doesn't exist
     *
     * By default the [default] value is an empty list
     */
    fun optCharList(
        key: String,
        default: List<Char>? = emptyList()
    ): List<Char>? {
        return opt(key, default).asCharList()
    }

    /**
     * Returns the List of [Date] for the given [key]
     *
     * @throws JsonException If the given key doesn't exist
     */
    fun getDateList(key: String): List<Date> {
        return get(key).asDateList()
    }

    /**
     * Returns an optional List of [Date] for the given [key] or [default]
     * if the value is null or the [key] doesn't exist
     *
     * By default the [default] value is an empty list
     */
    fun optDateList(
        key: String,
        default: List<Date>? = emptyList()
    ): List<Date>? {
        return opt(key, default).asDateList()
    }

    /**
     * Returns the List of [Date] for the given [key]
     *
     * @throws JsonException If the given key doesn't exist
     */
    fun getDateList(
        key: String,
        dateFormat: DateFormat
    ): List<Date> {
        return get(key).asDateList(dateFormat)
    }

    /**
     * Returns an optional List of [Date] for the given [key] or [default]
     * if the value is null or the [key] doesn't exist
     *
     * By default the [default] value is an empty list
     */
    fun optDateList(
        key: String,
        dateFormat: DateFormat,
        default: List<Date>? = emptyList()
    ): List<Date>? {
        return opt(key, default).asDateList(dateFormat)
    }

    /**
     * Returns the List of [Date] for the given [key]
     *
     * @throws JsonException If the given key doesn't exist
     */
    fun getDateList(
        key: String,
        format: String,
        locale: Locale = Locale.getDefault()
    ): List<Date> {
        return get(key).asDateList(format, locale)
    }

    /**
     * Returns an optional List of [Date] for the given [key] or [default]
     * if the value is null or the [key] doesn't exist
     *
     * By default the [default] value is an empty list
     */
    fun optDateList(
        key: String,
        format: String,
        locale: Locale = Locale.getDefault(),
        default: List<Date>? = emptyList()
    ): List<Date>? {
        return opt(key, default).asDateList(format, locale)
    }

    /**
     * Returns the List of [Calendar] for the given [key]
     *
     * @throws JsonException If the given key doesn't exist
     */
    fun getCalendarList(key: String): List<Calendar> {
        return get(key).asCalendarList()
    }

    /**
     * Returns an optional List of [Calendar] for the given [key] or [default]
     * if the value is null or the [key] doesn't exist
     *
     * By default the [default] value is an empty list
     */
    fun optCalendarList(
        key: String,
        default: List<Calendar>? = emptyList()
    ): List<Calendar>? {
        return opt(key, default).asCalendarList()
    }

    /**
     * Returns the List of [Calendar] for the given [key]
     *
     * @throws JsonException If the given key doesn't exist
     */
    fun getCalendarList(
        key: String,
        dateFormat: DateFormat
    ): List<Calendar> {
        return get(key).asCalendarList(dateFormat)
    }

    /**
     * Returns an optional List of [Calendar] for the given [key] or [default]
     * if the value is null or the [key] doesn't exist
     *
     * By default the [default] value is an empty list
     */
    fun optCalendarList(
        key: String,
        dateFormat: DateFormat,
        default: List<Calendar>? = emptyList()
    ): List<Calendar>? {
        return opt(key, default).asCalendarList(dateFormat)
    }

    /**
     * Returns the List of [Calendar] for the given [key]
     *
     * @throws JsonException If the given key doesn't exist
     */
    fun getCalendarList(
        key: String,
        format: String,
        locale: Locale = Locale.getDefault()
    ): List<Calendar> {
        return get(key).asCalendarList(format, locale)
    }

    /**
     * Returns an optional List of [Calendar] for the given [key] or [default]
     * if the value is null or the [key] doesn't exist
     *
     * By default the [default] value is an empty list
     */
    fun optCalendarList(
        key: String,
        format: String,
        locale: Locale = Locale.getDefault(),
        default: List<Calendar>? = emptyList()
    ): List<Calendar>? {
        return opt(key, default).asCalendarList(format, locale)
    }

    /**
     * Returns the List of [IntRange] for the given [key]
     *
     * @throws JsonException If the given key doesn't exist
     */
    fun getRangeList(key: String): List<IntRange> {
        return get(key).asRangeList()
    }

    /**
     * Returns an optional List of [IntRange] for the given [key] or [default]
     * if the value is null or the [key] doesn't exist
     *
     * By default the [default] value is an empty list
     */
    fun optRangeList(
        key: String,
        default: List<IntRange>? = emptyList()
    ): List<IntRange>? {
        return opt(key, default).asRangeList()
    }

    /**
     * Returns the List of [BigDecimal] for the given [key]
     *
     * @throws JsonException If the given key doesn't exist
     */
    fun getBigDecimalList(key: String): List<BigDecimal> {
        return get(key).asBigDecimalList()
    }

    /**
     * Returns an optional List of [BigDecimal] for the given [key] or [default]
     * if the value is null or the [key] doesn't exist
     *
     * By default the [default] value is an empty list
     */
    fun optBigDecimalList(
        key: String,
        default: List<BigDecimal>? = emptyList()
    ): List<BigDecimal>? {
        return opt(key, default).asBigDecimalList()
    }

    /**
     * Returns the List of [URL] for the given [key]
     *
     * @throws JsonException If the given key doesn't exist
     */
    fun getURLList(key: String): List<URL> {
        return get(key).asURLList()
    }

    /**
     * Returns an optional List of [URL] for the given [key] or [default]
     * if the value is null or the [key] doesn't exist
     *
     * By default the [default] value is an empty list
     */
    fun optURLList(
        key: String,
        default: List<URL>? = emptyList()
    ): List<URL>? {
        return opt(key, default).asURLList()
    }

    /**
     * Returns the List of Enum of type [T] for the given [key]
     *
     * @throws JsonException If the given key doesn't exist
     */
    inline fun <reified T : Enum<T>> getEnumList(
        key: String,
        transform: (String) -> String = { it.capitalize() }
    ): List<T> {
        return get(key).asEnumList(transform)
    }

    /**
     * Returns an optional List of Enum of type [T] for the given [key] or [default]
     * if the value is null or the [key] doesn't exist
     *
     * By default the [default] value is an empty list
     */
    inline fun <reified T : Enum<T>> optEnumList(
        key: String,
        default: List<T>? = emptyList(),
        transform: (String) -> String = { it.capitalize() }
    ): List<T>? {
        return opt(key, default).asEnumList(transform)
    }

    /**
     * Returns the List of type [T] for the given [key]
     *
     * @throws JsonException If the given key doesn't exist
     */
    inline fun <reified T> getList(
        key: String,
        transform: (JsonValue) -> T
    ): List<T> {
        return get(key).asList(transform)
    }

    /**
     * Returns an optional List of type [T] for the given [key] or [default]
     * if the value is null or the [key] doesn't exist
     *
     * By default the [default] value is an empty list
     */
    inline fun <reified T> optList(
        key: String,
        transform: (JsonValue) -> T,
        default: List<T>? = emptyList()
    ): List<T>? {
        return opt(key, default).asList(transform)
    }

    /**
     * Returns the object of type [T] for the given [key]
     *
     * @throws JsonException If the given key doesn't exist
     */
    inline fun <T> getObject(
        key: String,
        transform: (JsonObject) -> T
    ): T {
        return get(key).asObject(transform)
    }

    /**
     * Returns an optional object of type [T] for the given [key] or [default]
     * if the value is null or the [key] doesn't exist
     *
     * By default the [default] value is a null instance
     */
    inline fun <T> optObject(
        key: String,
        transform: (JsonObject) -> T,
        default: T? = null
    ): T? {
        return opt(key, default).asObject(transform)
    }

    /**
     * Returns the List of objects of type [T] for the given [key]
     *
     * @throws JsonException If the given key doesn't exist
     */
    inline fun <T> getObjectList(
        key: String,
        transform: (JsonObject) -> T
    ): List<T> {
        return get(key).asObjectList(transform)
    }

    /**
     * Returns an optional List of objects of type [T] for the given [key] or [default]
     * if the value is null or the [key] doesn't exist
     *
     * By default the [default] value is an empty list
     */
    inline fun <reified T> optObjectList(
        key: String,
        transform: (JsonObject) -> T,
        default: List<T>? = emptyList()
    ): List<T>? {
        return opt(key, default).asObjectList(transform)
    }
}