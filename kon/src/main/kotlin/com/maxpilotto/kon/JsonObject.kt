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

import com.maxpilotto.kon.extensions.toJsonValue
import com.maxpilotto.kon.protocols.Json
import com.maxpilotto.kon.util.*
import com.maxpilotto.kon.util.isNull
import java.math.BigDecimal
import java.net.URL
import java.text.DateFormat
import java.util.*

/**
 * # JsonObject
 *
 * Representation of a JSON Object, which is implemented using a
 * mutable map of <String,Any?>
 *
 * ## Get/Get value
 * The [get] and [getValue] methods can be used to retrieve a value for a given key
 *
 * The first will return a [JsonValue], which is used to quickly navigate through
 * objects and arrays
 *
 * The second one instead will return the actual value
 *
 * ## Values and Optional values
 *
 * An optional value is a value that might not exist or might be null
 *
 * Every method that returns a value will throw an exception if the key doesn't exist
 * or the value is null, a null value is represented by the string "null"
 *
 * ```
 *
 * val json = JsonObject("{ "value": "null" }")
 *
 * val n1 = json.getInt("value")    // Throws an exception
 * val n2 = json.optInt("value")    // Will return 0
 *
 * ```
 */
class JsonObject : Json, MutableIterable<MutableMap.MutableEntry<String, Any?>> {   //TODO Add value observer
    private var map: MutableMap<String, Any?>

    /**
     * Returns a [MutableSet] of all key/value pairs in this object
     */
    val entries: MutableSet<MutableMap.MutableEntry<String, Any?>>
        get() = map.entries

    /**
     * Returns a [MutableSet] of all keys in this map
     */
    val keys: MutableSet<String>
        get() = map.keys

    /**
     * Returns a MutableCollection of all values in this map
     */
    val values: MutableCollection<Any?>
        get() = map.values

    /**
     * Returns the number of key/value pairs in this object
     */
    val size: Int
        get() = map.size

    /**
     * Creates an empty JsonObject
     */
    constructor() : this(emptyMap<String, Any?>())

    /**
     * Creates a JsonObject from the given [string]
     */
    constructor(string: String) : this(JsonParser(string).nextObject())

    /**
     * Clones the given [jsonObject]
     */
    constructor(jsonObject: JsonObject) : this(jsonObject.map)

    /**
     * Creates a JsonObject from the given [properties]
     */
    constructor(vararg properties: Pair<String, Any?>) : this(properties.toMap())

    /**
     * Creates a JsonObject from the given [map]
     */
    constructor(map: Map<*, *>) {
        this.map = mutableMapOf()

        for ((k, v) in map) {
            this.map[k as String] = wrap(v)
        }
    }

    override fun toString(): String {
        return prettify()
    }

    override fun prettify(): String {
        return map.entries.joinToString(",", "{", "}", transform = {
            "\"${it.key}\":${stringify(it.value)}"
        })
    }

    override fun get(key: String): JsonValue {
        return (getValue(key)).toJsonValue()
    }

    override fun set(key: String, element: Any?) {
        map[key] = wrap(element)
    }

    override fun iterator(): MutableIterator<MutableMap.MutableEntry<String, Any?>> {
        return map.iterator()
    }

    /**
     * Returns the number of key/value pairs in this object
     *
     * @param recursiveLookup If true, the size of all children that are JsonObject will be added
     */
    fun size(recursiveLookup: Boolean = false): Int {
        return if (recursiveLookup) {
            var s = size

            for (e in map) {
                if (e.value is JsonObject) {
                    s += (e.value as JsonObject).size(true)
                }
            }

            s
        } else {
            size
        }
    }

    /**
     * Returns an optional wrapped value associated with the given [key] or [default] if
     * the value is null or the [key] doesn't exist
     */
    fun opt(key: String, default: Any?): JsonValue {
        return optValue(key, default).toJsonValue()
    }

    /**
     * Returns the value associated with the given [key]
     *
     * @throws JsonException If the [key] doesn't exist
     */
    fun getValue(key: String): Any? {
        return map[key] ?: throw JsonException("Key '$key' was not found")
    }

    /**
     * Returns the optional value associated with the given [key] or [default] if the value
     * is null or if the [key] doesn't exist
     */
    fun optValue(key: String, default: Any? = null): Any? {
        val value = map[key]

        return if (isNull(value)) default else value
    }

    /**
     * Returns whether or not this object is empty
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
     * Removes the specified [key] and its corresponding value from this object
     */
    fun remove(key: String): Any? {
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
    fun remove(vararg path: Any) {  //TODO Create a JsonPath object
        val last = path.last()
        var current = toJsonValue()

        for (i in 0 until path.lastIndex) {
            val key = path[i]

            current = when (key) {
                is String -> current[key]   //TODO Use the index to fetch a value at keys[index]
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
     * Returns whether or not the value associated with the given [key] is null
     *
     * This will return true if the [key] doesn't exist
     */
    fun isNull(key: String): Boolean {
        return !has(key) || isNull(getValue(key))
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
     * Returns a copy of the map used internally to implement this [JsonObject]
     */
    fun toMap(): Map<String, Any?> {
        return map.toMap()
    }

    /**
     * Returns a copy of the map used internally, as a Map of [K] and [V]
     */
    fun <K, V> toTypedMap(): Map<K, V> {
        return toMap() as Map<K, V>
    }

    //TODO .copy(paths...)
    //TODO .reduce(keys)
    //TODO .merge(json)

    /**
     * Returns the String associated with the given [key]
     *
     * @throws JsonException If the key doesn't exist
     */
    fun getString(key: String): String {
        return parse(getValue(key))
    }

    /**
     * Returns an optional String associated the given [key] or an
     * empty String if the value is null or the [key] doesn't exist
     */
    fun optString(key: String): String {
        return parse(optValue(key, ""))
    }

    /**
     * Returns an optional String associated the given [key] or [default]
     * if the value is null or the [key] doesn't exist
     */
    fun optString(key: String, default: Any?): String? {
        return parseOptional(optValue(key, default))
    }

    /**
     * Returns the Number associated with the given [key]
     *
     * @throws JsonException If the key doesn't exist
     */
    fun getNumber(key: String): Number {
        return parse(getValue(key))
    }

    /**
     * Returns an optional Number associated with the given [key] or 0
     * if the value is null or the [key] doesn't exist
     */
    fun optNumber(key: String): Number {
        return parse(optValue(key, 0))
    }

    /**
     * Returns an optional Number associated with the given [key] or [default]
     * if the value is null or the [key] doesn't exist
     */
    fun optNumber(key: String, default: Any?): Number? {
        return parseOptional(optValue(key, default))
    }

    /**
     * Returns the [JsonObject] associated with the given [key]
     *
     * @throws JsonException If the key doesn't exist
     */
    fun getJsonObject(key: String): JsonObject {
        return parse(getValue(key))
    }

    /**
     * Returns an optional [JsonObject] associated with the given [key] or an
     * empty [JsonObject] if the value is null or the [key] doesn't exist
     */
    fun optJsonObject(key: String): JsonObject {
        return parse(optValue(key, JsonObject()))
    }

    /**
     * Returns an optional [JsonObject] associated with the given [key] or [default]
     * if the value is null or the [key] doesn't exist
     */
    fun optJsonObject(key: String, default: Any?): JsonObject? {
        return parseOptional(optValue(key, default))
    }

    /**
     * Returns the [JsonArray] associated with the given [key]
     *
     * @throws JsonException If the key doesn't exist
     */
    fun getJsonArray(key: String): JsonArray {
        return parse(getValue(key))
    }

    /**
     * Returns an optional [JsonArray] associated with the given [key] or an
     * empty [JsonArray] if the value is null or the [key] doesn't exist
     */
    fun optJsonArray(key: String): JsonArray {
        return parse(optValue(key, JsonArray()))
    }

    /**
     * Returns an optional [JsonArray] associated with the given [key] or [default]
     * if the value is null or the [key] doesn't exist
     */
    fun optJsonArray(key: String, default: Any?): JsonArray? {
        return parseOptional(optValue(key, default))
    }

    /**
     * Returns the Int value associated with the given [key]
     *
     * @throws JsonException If the key doesn't exist
     */
    fun getInt(key: String): Int {
        return parse(getValue(key))
    }

    /**
     * Returns an optional Int value associated with the given [key] or
     * 0 if the value is null or the [key] doesn't exist
     */
    fun optInt(key: String): Int {
        return parse(optValue(key, 0))
    }

    /**
     * Returns an optional Int value associated with the given [key] or [default]
     * if the value is null or the [key] doesn't exist
     */
    fun optInt(key: String, default: Any?): Int? {
        return parseOptional(optValue(key, default))
    }

    /**
     * Returns the Long value associated with the given [key]
     *
     * @throws JsonException If the key doesn't exist
     */
    fun getLong(key: String): Long {
        return parse(getValue(key))
    }

    /**
     * Returns an optional Long value associated with the given [key] or 0
     * if the value is null or the [key] doesn't exist
     */
    fun optLong(key: String): Long {
        return parse(optValue(key, 0L))
    }

    /**
     * Returns an optional Long value associated with the given [key] or [default]
     * if the value is null or the [key] doesn't exist
     */
    fun optLong(key: String, default: Any?): Long? {
        return parseOptional(optValue(key, default))
    }

    /**
     * Returns the Boolean value associated with the given [key]
     *
     * @throws JsonException If the key doesn't exist
     */
    fun getBoolean(key: String): Boolean {
        return parse(getValue(key))
    }

    /**
     * Returns an optional Boolean value associated with the given [key] or false
     * if the value is null or the [key] doesn't exist
     */
    fun optBoolean(key: String): Boolean {
        return parse(optValue(key, false))
    }

    /**
     * Returns an optional Boolean value associated with the given [key] or [default]
     * if the value is null or the [key] doesn't exist
     */
    fun optBoolean(key: String, default: Any?): Boolean? {
        return parseOptional(optValue(key, default))
    }

    /**
     * Returns the Double value associated with the given [key]
     *
     * @throws JsonException If the key doesn't exist
     */
    fun getDouble(key: String): Double {
        return parse(getValue(key))
    }

    /**
     * Returns an optional Double value associated with the given [key] or 0.0
     * if the value is null or the [key] doesn't exist
     */
    fun optDouble(key: String): Double {
        return parse(optValue(key, 0.0))
    }

    /**
     * Returns an optional Double value associated with the given [key] or [default]
     * if the value is null or the [key] doesn't exist
     *
     * By default the [default] value is 0.0
     */
    fun optDouble(key: String, default: Any?): Double? {
        return parseOptional(optValue(key, default))
    }

    /**
     * Returns the Float value associated with the given [key]
     *
     * @throws JsonException If the key doesn't exist
     */
    fun getFloat(key: String): Float {
        return parse(getValue(key))
    }

    /**
     * Returns an optional Float value associated with the given [key] or 0
     * if the value is null or the [key] doesn't exist
     */
    fun optFloat(key: String): Float {
        return parse(optValue(key, 0F))
    }

    /**
     * Returns an optional Float value associated with the given [key] or [default]
     * if the value is null or the [key] doesn't exist
     */
    fun optFloat(key: String, default: Any?): Float? {
        return parseOptional(optValue(key, default))
    }

    /**
     * Returns the Byte value associated with the given [key]
     *
     * @throws JsonException If the key doesn't exist
     */
    fun getByte(key: String): Byte {
        return parse(getValue(key))
    }

    /**
     * Returns an optional Byte value associated with the given [key] or [Byte.MIN_VALUE]
     * if the value is null or the [key] doesn't exist
     */
    fun optByte(key: String): Byte {
        return parse(optValue(key, Byte.MIN_VALUE))
    }

    /**
     * Returns an optional Byte value associated with the given [key] or [default]
     * if the value is null or the [key] doesn't exist
     */
    fun optByte(key: String, default: Any?): Byte? {
        return parseOptional(optValue(key, default))
    }

    /**
     * Returns the Short value associated with the given [key]
     *
     * @throws JsonException If the key doesn't exist
     */
    fun getShort(key: String): Short {
        return parse(getValue(key))
    }

    /**
     * Returns an optional Short value associated with the given [key] or 0
     * if the value is null or the [key] doesn't exist
     */
    fun optShort(key: String): Short {
        return parse(optValue(key, 0))
    }

    /**
     * Returns an optional Short value associated with the given [key] or [default]
     * if the value is null or the [key] doesn't exist
     */
    fun optShort(key: String, default: Any?): Short? {
        return parseOptional(optValue(key, default))
    }

    /**
     * Returns the Char value associated with the given [key]
     *
     * @throws JsonException If the key doesn't exist
     */
    fun getChar(key: String): Char {
        return parse(getValue(key))
    }

    /**
     * Returns an optional Char value associated with the given [key] or [Char.MIN_VALUE]
     * if the value is null or the [key] doesn't exist
     */
    fun optChar(key: String): Char {
        return parse(optValue(key, Char.MIN_VALUE))
    }

    /**
     * Returns an optional Char value associated with the given [key] or [default]
     * if the value is null or the [key] doesn't exist
     */
    fun optChar(key: String, default: Any?): Char? {
        return parseOptional(optValue(key, default))
    }

    /**
     * Returns the [Date] associated with the given [key]
     *
     * @throws JsonException If the key doesn't exist
     */
    fun getDate(key: String): Date {
        return parseDate(getValue(key))
    }

    /**
     * Returns an optional [Date] associated with the given [key] or [default]
     * if the value is null or the [key] doesn't exist
     */
    fun optDate(key: String, default: Any?): Date? {
        return parseOptionalDate(optValue(key, default))
    }

    /**
     * Returns the [Date] associated with the given [key]
     *
     * @throws JsonException If the key doesn't exist
     */
    fun getDate(key: String, dateFormat: DateFormat): Date {
        return parseDate(getValue(key), dateFormat)
    }

    /**
     * Returns an optional [Date] associated with the given [key] or [default]
     * if the value is null or the [key] doesn't exist
     */
    fun optDate(key: String, dateFormat: DateFormat, default: Any?): Date? {
        return parseOptionalDate(optValue(key, default), dateFormat)
    }

    /**
     * Returns the [Date] associated with the given [key]
     *
     * @throws JsonException If the key doesn't exist
     */
    fun getDate(key: String, format: String, locale: Locale = Locale.getDefault()): Date {
        return parseDate(getValue(key), format, locale)
    }

    /**
     * Returns an optional [Date] associated with the given [key] or [default]
     * if the value is null or the [key] doesn't exist
     */
    fun optDate(
        key: String,
        format: String,
        default: Any?,
        locale: Locale = Locale.getDefault()
    ): Date? {  //FIXME The order is dangerous
        return parseOptionalDate(optValue(key, default), format, locale)
    }

    /**
     * Returns the [Calendar] associated with the given [key]
     *
     * @throws JsonException If the key doesn't exist
     */
    fun getCalendar(key: String): Calendar {
        return parseDate(getValue(key))
    }

    /**
     * Returns an optional [Calendar] associated with the given [key] or [default]
     * if the value is null or the [key] doesn't exist
     */
    fun optCalendar(key: String, default: Any?): Calendar? {
        return parseOptionalDate(optValue(key, default))
    }

    /**
     * Returns the [Calendar] associated with the given [key]
     *
     * @throws JsonException If the key doesn't exist
     */
    fun getCalendar(key: String, dateFormat: DateFormat): Calendar {
        return parseDate(getValue(key), dateFormat)
    }

    /**
     * Returns an optional [Calendar] associated with the given [key] or [default]
     * if the value is null or the [key] doesn't exist
     */
    fun optCalendar(key: String, dateFormat: DateFormat, default: Any?): Calendar? {
        return parseOptionalDate(optValue(key, default), dateFormat)
    }

    /**
     * Returns the [Calendar] associated with the given [key]
     *
     * @throws JsonException If the key doesn't exist
     */
    fun getCalendar(key: String, format: String, locale: Locale = Locale.getDefault()): Calendar {
        return parseDate(getValue(key), format, locale)
    }

    /**
     * Returns an optional [Calendar] associated with the given [key] or [default]
     * if the value is null or the [key] doesn't exist
     */
    fun optCalendar(key: String, format: String, default: Any?, locale: Locale = Locale.getDefault()): Calendar? {
        return parseOptionalDate(optValue(key, default), format, locale)
    }

    /**
     * Returns the [IntRange] associated with the given [key]
     *
     * @throws JsonException If the key doesn't exist
     */
    fun getRange(key: String): IntRange {
        return parse(getValue(key))
    }

    /**
     * Returns an optional [IntRange] associated with the given [key] or [default]
     * if the value is null or the [key] doesn't exist
     */
    fun optRange(key: String, default: Any?): IntRange? {
        return parseOptional(optValue(key, default))
    }

    /**
     * Returns the [BigDecimal] associated with the given [key]
     *
     * @throws JsonException If the key doesn't exist
     */
    fun getBigDecimal(key: String): BigDecimal {
        return parse(getValue(key))
    }

    /**
     * Returns an optional [BigDecimal] associated with the given [key] or 0
     * if the value is null or the [key] doesn't exist
     */
    fun optBigDecimal(key: String): BigDecimal {
        return parse(optValue(key, BigDecimal(0)))
    }

    /**
     * Returns an optional [BigDecimal] associated with the given [key] or [default]
     * if the value is null or the [key] doesn't exist
     */
    fun optBigDecimal(key: String, default: Any?): BigDecimal? {
        return parseOptional(optValue(key,default))
    }

    /**
     * Returns the [URL] associated with the given [key]
     *
     * @throws JsonException If the key doesn't exist
     */
    fun getURL(key: String): URL {
        return parse(getValue(key))
    }

    /**
     * Returns an optional [URL] associated with the given [key] or [default]
     * if the value is null or the [key] doesn't exist
     */
    fun optURL(key: String, default: Any?): URL? {
        return parseOptional(optValue(key, default))
    }

    /**
     * Returns the Enum of type [T] associated with the given [key]
     *
     * @throws JsonException If the key doesn't exist
     */
    inline fun <reified T : Enum<T>> getEnum(key: String): T {
        return parseEnum(getValue(key))
    }

    /**
     * Returns an optional Enum of type [T] associated with the given [key] or the
     * first enum constant if the value is null or the [key] doesn't exist
     */
    inline fun <reified T : Enum<T>> optEnum(key: String): T {
        val first = T::class.java.enumConstants.first()

        return parseEnum(optValue(key, first))
    }

    /**
     * Returns an optional Enum of type [T] associated with the given [key] or [default]
     * if the value is null or the [key] doesn't exist
     */
    inline fun <reified T : Enum<T>> optEnum(key: String, default: Any?): T? {
        return parseOptionalEnum<T>(optValue(key, default))
    }

    /**
     * Returns the List of Any? associated with the given [key]
     *
     * @throws JsonException If the key doesn't exist
     */
    fun getList(key: String): List<Any?> {
        return getJsonArray(key).toList()
    }

    /**
     * Returns an optional List of Any? associated with the given [key] or [emptyList]
     * if the value is null or the [key] doesn't exist
     */
    fun optList(key: String): List<Any?> {
        return optJsonArray(key).toList()
    }

    /**
     * Returns an optional List of Any? associated with the given [key] or [default]
     * if the value is null or the [key] doesn't exist
     */
    fun optList(key: String, default: List<Any?>?): List<Any?>? {
        return optJsonArray(key, null)?.toList() ?: default
    }

    /**
     * Returns the List of String associated with the given [key]
     *
     * @throws JsonException If the key doesn't exist
     */
    fun getStringList(key: String): List<String> {
        return getJsonArray(key).toStringList()
    }

    /**
     * Returns an optional List of String associated with the given [key] or [emptyList]
     * if the value is null or the [key] doesn't exist
     */
    fun optStringList(key: String): List<String> {
        return optJsonArray(key).toStringList()
    }

    /**
     * Returns an optional List of String associated with the given [key] or [default]
     * if the value is null or the [key] doesn't exist
     */
    fun optStringList(key: String, default: List<String>?): List<String>? {
        return optJsonArray(key, null)?.toStringList() ?: default
    }

    /**
     * Returns the List of Number associated with the given [key]
     *
     * @throws JsonException If the key doesn't exist
     */
    fun getNumberList(key: String): List<Number> {
        return getJsonArray(key).toNumberList()
    }

    /**
     * Returns an optional List of Number associated with the given [key] or [emptyList]
     * if the value is null or the [key] doesn't exist
     */
    fun optNumberList(key: String): List<Number> {
        return optJsonArray(key).toNumberList()
    }

    /**
     * Returns an optional List of Number associated with the given [key] or [default]
     * if the value is null or the [key] doesn't exist
     */
    fun optNumberList(key: String, default: List<Number>?): List<Number>? {
        return optJsonArray(key, null)?.toNumberList() ?: default
    }

    /**
     * Returns the List of [JsonObject] associated with the given [key]
     *
     * @throws JsonException If the key doesn't exist
     */
    fun getJsonObjectList(key: String): List<JsonObject> {
        return getJsonArray(key).toJsonObjectList()
    }

    /**
     * Returns an optional List of [JsonObject] associated with the given [key] or [emptyList]
     * if the value is null or the [key] doesn't exist
     */
    fun optJsonObjectList(key: String): List<JsonObject> {
        return optJsonArray(key).toJsonObjectList()
    }

    /**
     * Returns an optional List of [JsonObject] associated with the given [key] or [default]
     * if the value is null or the [key] doesn't exist
     *
     * By default the [default] value is an empty list
     */
    fun optJsonObjectList(key: String, default: List<JsonObject>?): List<JsonObject>? {
        return optJsonArray(key, null)?.toJsonObjectList() ?: default
    }

    /**
     * Returns the List of Int associated with the given [key]
     *
     * @throws JsonException If the key doesn't exist
     */
    fun getIntList(key: String): List<Int> {
        return getJsonArray(key).toIntList()
    }

    /**
     * Returns an optional List of Int associated with the given [key] or [emptyList]
     * if the value is null or the [key] doesn't exist
     */
    fun optIntList(key: String): List<Int> {
        return optJsonArray(key).toIntList()
    }

    /**
     * Returns an optional List of Int associated with the given [key] or [default]
     * if the value is null or the [key] doesn't exist
     */
    fun optIntList(key: String, default: List<Int>?): List<Int>? {
        return optJsonArray(key, null)?.toIntList() ?: default
    }

    /**
     * Returns the List of Long associated with the given [key]
     *
     * @throws JsonException If the key doesn't exist
     */
    fun getLongList(key: String): List<Long> {
        return getJsonArray(key).toLongList()
    }

    /**
     * Returns an optional List of Long associated with the given [key] or [emptyList]
     * if the value is null or the [key] doesn't exist
     */
    fun optLongList(key: String): List<Long> {
        return optJsonArray(key).toLongList()
    }

    /**
     * Returns an optional List of Long associated with the given [key] or [default]
     * if the value is null or the [key] doesn't exist
     */
    fun optLongList(key: String, default: List<Long>?): List<Long>? {
        return optJsonArray(key, null)?.toLongList() ?: default
    }

    /**
     * Returns the List of Boolean associated with the given [key]
     *
     * @throws JsonException If the key doesn't exist
     */
    fun getBooleanList(key: String): List<Boolean> {
        return getJsonArray(key).toBooleanList()
    }

    /**
     * Returns an optional List of Boolean associated with the given [key] or [emptyList]
     * if the value is null or the [key] doesn't exist
     */
    fun optBooleanList(key: String): List<Boolean> {
        return optJsonArray(key).toBooleanList()
    }

    /**
     * Returns an optional List of Boolean associated with the given [key] or [default]
     * if the value is null or the [key] doesn't exist
     */
    fun optBooleanList(key: String, default: List<Boolean>?): List<Boolean>? {
        return optJsonArray(key, null)?.toBooleanList() ?: default
    }

    /**
     * Returns the List of Double associated with the given [key]
     *
     * @throws JsonException If the key doesn't exist
     */
    fun getDoubleList(key: String): List<Double> {
        return getJsonArray(key).toDoubleList()
    }

    /**
     * Returns an optional List of Double associated with the given [key] or [emptyList]
     * if the value is null or the [key] doesn't exist
     */
    fun optDoubleList(key: String): List<Double> {
        return optJsonArray(key).toDoubleList()
    }

    /**
     * Returns an optional List of Double associated with the given [key] or [default]
     * if the value is null or the [key] doesn't exist
     */
    fun optDoubleList(key: String, default: List<Double>?): List<Double>? {
        return optJsonArray(key, null)?.toDoubleList() ?: default
    }

    /**
     * Returns the List of Float associated with the given [key]
     *
     * @throws JsonException If the key doesn't exist
     */
    fun getFloatList(key: String): List<Float> {
        return getJsonArray(key).toFloatList()
    }

    /**
     * Returns an optional List of Float associated with the given [key] or [emptyList]
     * if the value is null or the [key] doesn't exist
     */
    fun optFloatList(key: String): List<Float> {
        return optJsonArray(key).toFloatList()
    }

    /**
     * Returns an optional List of Float associated with the given [key] or [default]
     * if the value is null or the [key] doesn't exist
     */
    fun optFloatList(key: String, default: List<Float>?): List<Float>? {
        return optJsonArray(key, null)?.toFloatList() ?: default
    }

    /**
     * Returns the List of Byte associated with the given [key]
     *
     * @throws JsonException If the key doesn't exist
     */
    fun getByteList(key: String): List<Byte> {
        return getJsonArray(key).toByteList()
    }

    /**
     * Returns an optional List of Byte associated with the given [key] or [emptyList]
     * if the value is null or the [key] doesn't exist
     */
    fun optByteList(key: String): List<Byte> {
        return optJsonArray(key).toByteList()
    }

    /**
     * Returns an optional List of Byte associated with the given [key] or [default]
     * if the value is null or the [key] doesn't exist
     */
    fun optByteList(key: String, default: List<Byte>?): List<Byte>? {
        return optJsonArray(key, null)?.toByteList() ?: default
    }

    /**
     * Returns the List of Short associated with the given [key]
     *
     * @throws JsonException If the key doesn't exist
     */
    fun getShortList(key: String): List<Short> {
        return getJsonArray(key).toShortList()
    }

    /**
     * Returns an optional List of Short associated with the given [key] or [emptyList]
     * if the value is null or the [key] doesn't exist
     */
    fun optShortList(key: String): List<Short> {
        return optJsonArray(key).toShortList()
    }

    /**
     * Returns an optional List of Short associated with the given [key] or [default]
     * if the value is null or the [key] doesn't exist
     */
    fun optShortList(key: String, default: List<Short>?): List<Short>? {
        return optJsonArray(key, null)?.toShortList() ?: default
    }

    /**
     * Returns the List of Char associated with the given [key]
     *
     * @throws JsonException If the key doesn't exist
     */
    fun getCharList(key: String): List<Char> {
        return getJsonArray(key).toCharList()
    }

    /**
     * Returns an optional List of Char associated with the given [key] or [emptyList]
     * if the value is null or the [key] doesn't exist
     */
    fun optCharList(key: String): List<Char> {
        return optJsonArray(key).toCharList()
    }

    /**
     * Returns an optional List of Char associated with the given [key] or [default]
     * if the value is null or the [key] doesn't exist
     */
    fun optCharList(key: String, default: List<Char>?): List<Char>? {
        return optJsonArray(key, null)?.toCharList() ?: default
    }

    /**
     * Returns the List of [Date] associated with the given [key]
     *
     * @throws JsonException If the key doesn't exist
     */
    fun getDateList(key: String): List<Date> {
        return getJsonArray(key).toDateList()
    }

    /**
     * Returns an optional List of [Date] associated with the given [key] or [emptyList]
     * if the value is null or the [key] doesn't exist
     */
    fun optDateList(key: String): List<Date> {
        return optJsonArray(key).toDateList()
    }

    /**
     * Returns an optional List of [Date] associated with the given [key] or [default]
     * if the value is null or the [key] doesn't exist
     */
    fun optDateList(key: String, default: List<Date>?): List<Date>? {
        return optJsonArray(key, null)?.toDateList() ?: default
    }

    /**
     * Returns the List of [Date] associated with the given [key]
     *
     * @throws JsonException If the key doesn't exist
     */
    fun getDateList(key: String, dateFormat: DateFormat): List<Date> {
        return getJsonArray(key).toDateList(dateFormat)
    }

    /**
     * Returns an optional List of [Date] associated with the given [key] or [emptyList]
     * if the value is null or the [key] doesn't exist
     */
    fun optDateList(key: String, dateFormat: DateFormat): List<Date> {
        return optJsonArray(key).toDateList(dateFormat)
    }

    /**
     * Returns an optional List of [Date] associated with the given [key] or [default]
     * if the value is null or the [key] doesn't exist
     */
    fun optDateList(key: String, dateFormat: DateFormat, default: List<Date>?): List<Date>? {
        return optJsonArray(key, null)?.toDateList(dateFormat) ?: default
    }

    /**
     * Returns the List of [Date] associated with the given [key]
     *
     * @throws JsonException If the key doesn't exist
     */
    fun getDateList(key: String, format: String, locale: Locale = Locale.getDefault()): List<Date> {
        return getJsonArray(key).toDateList(format, locale)
    }

    /**
     * Returns an optional List of [Date] associated with the given [key] or [emptyList]
     * if the value is null or the [key] doesn't exist
     */
    fun optDateList(key: String, format: String, locale: Locale = Locale.getDefault()): List<Date> {
        return optJsonArray(key).toDateList(format, locale)
    }

    /**
     * Returns an optional List of [Date] associated with the given [key] or [default]
     * if the value is null or the [key] doesn't exist
     */
    fun optDateList(
        key: String,
        format: String,
        default: List<Date>?,
        locale: Locale = Locale.getDefault()
    ): List<Date>? {
        return optJsonArray(key, null)?.toDateList(format, locale) ?: default
    }

    /**
     * Returns the List of [Calendar] associated with the given [key]
     *
     * @throws JsonException If the key doesn't exist
     */
    fun getCalendarList(key: String): List<Calendar> {
        return getJsonArray(key).toCalendarList()
    }

    /**
     * Returns an optional List of [Calendar] associated with the given [key] or [emptyList]
     * if the value is null or the [key] doesn't exist
     */
    fun optCalendarList(key: String): List<Calendar> {
        return optJsonArray(key).toCalendarList()
    }

    /**
     * Returns an optional List of [Calendar] associated with the given [key] or [default]
     * if the value is null or the [key] doesn't exist
     */
    fun optCalendarList(key: String, default: List<Calendar>?): List<Calendar>? {
        return optJsonArray(key, null)?.toCalendarList() ?: default
    }

    /**
     * Returns the List of [Calendar] associated with the given [key]
     *
     * @throws JsonException If the key doesn't exist
     */
    fun getCalendarList(key: String, dateFormat: DateFormat): List<Calendar> {
        return getJsonArray(key).toCalendarList(dateFormat)
    }

    /**
     * Returns an optional List of [Calendar] associated with the given [key] or [emptyList]
     * if the value is null or the [key] doesn't exist
     */
    fun optCalendarList(key: String, dateFormat: DateFormat): List<Calendar> {
        return optJsonArray(key).toCalendarList(dateFormat)
    }

    /**
     * Returns an optional List of [Calendar] associated with the given [key] or [default]
     * if the value is null or the [key] doesn't exist
     */
    fun optCalendarList(key: String, dateFormat: DateFormat, default: List<Calendar>): List<Calendar>? {
        return optJsonArray(key, null)?.toCalendarList(dateFormat) ?: default
    }

    /**
     * Returns the List of [Calendar] associated with the given [key]
     *
     * @throws JsonException If the key doesn't exist
     */
    fun getCalendarList(key: String, format: String, locale: Locale = Locale.getDefault()): List<Calendar> {
        return getJsonArray(key).toCalendarList(format, locale)
    }

    /**
     * Returns an optional List of [Calendar] associated with the given [key] or [emptyList]
     * if the value is null or the [key] doesn't exist
     */
    fun optCalendarList(key: String, format: String, locale: Locale = Locale.getDefault()): List<Calendar> {
        return optJsonArray(key).toCalendarList(format, locale)
    }

    /**
     * Returns an optional List of [Calendar] associated with the given [key] or [default]
     * if the value is null or the [key] doesn't exist
     */
    fun optCalendarList(
        key: String,
        format: String,
        default: List<Calendar>?,
        locale: Locale = Locale.getDefault()
    ): List<Calendar>? {
        return optJsonArray(key, null)?.toCalendarList(format, locale) ?: default
    }

    /**
     * Returns the List of [IntRange] associated with the given [key]
     *
     * @throws JsonException If the key doesn't exist
     */
    fun getRangeList(key: String): List<IntRange> {
        return getJsonArray(key).toRangeList()
    }

    /**
     * Returns an optional List of [IntRange] associated with the given [key] or [emptyList]
     * if the value is null or the [key] doesn't exist
     */
    fun optRangeList(key: String): List<IntRange> {
        return optJsonArray(key).toRangeList()
    }

    /**
     * Returns an optional List of [IntRange] associated with the given [key] or [default]
     * if the value is null or the [key] doesn't exist
     */
    fun optRangeList(key: String, default: List<IntRange>?): List<IntRange>? {
        return optJsonArray(key, null)?.toRangeList() ?: default
    }

    /**
     * Returns the List of [BigDecimal] associated with the given [key]
     *
     * @throws JsonException If the key doesn't exist
     */
    fun getBigDecimalList(key: String): List<BigDecimal> {
        return getJsonArray(key).toBigDecimalList()
    }

    /**
     * Returns an optional List of [BigDecimal] associated with the given [key] or [emptyList]
     * if the value is null or the [key] doesn't exist
     */
    fun optBigDecimalList(key: String): List<BigDecimal> {
        return optJsonArray(key).toBigDecimalList()
    }

    /**
     * Returns an optional List of [BigDecimal] associated with the given [key] or [default]
     * if the value is null or the [key] doesn't exist
     */
    fun optBigDecimalList(key: String, default: List<BigDecimal>?): List<BigDecimal>? {
        return optJsonArray(key, null)?.toBigDecimalList() ?: default
    }

    /**
     * Returns the List of [URL] associated with the given [key]
     *
     * @throws JsonException If the key doesn't exist
     */
    fun getURLList(key: String): List<URL> {
        return getJsonArray(key).toURLList()
    }

    /**
     * Returns an optional List of [URL] associated with the given [key] or [emptyList]
     * if the value is null or the [key] doesn't exist
     */
    fun optURLList(key: String): List<URL> {
        return optJsonArray(key).toURLList()
    }

    /**
     * Returns an optional List of [URL] associated with the given [key] or [default]
     * if the value is null or the [key] doesn't exist
     */
    fun optURLList(key: String, default: List<URL>?): List<URL>? {
        return optJsonArray(key, null)?.toURLList() ?: default
    }

    /**
     * Returns the List of Enum of type [T] associated with the given [key]
     *
     * @throws JsonException If the key doesn't exist
     */
    inline fun <reified T : Enum<T>> getEnumList(key: String): List<T> {
        return getJsonArray(key).toEnumList()
    }

    /**
     * Returns an optional List of Enum of type [T] associated with the given [key] or [emptyList]
     * if the value is null or the [key] doesn't exist
     */
    inline fun <reified T : Enum<T>> optEnumList(key: String): List<T> {
        return optJsonArray(key).toEnumList()
    }

    /**
     * Returns an optional List of Enum of type [T] associated with the given [key] or [default]
     * if the value is null or the [key] doesn't exist
     */
    inline fun <reified T : Enum<T>> optEnumList(key: String, default: List<T>?): List<T>? {
        return optJsonArray(key, null)?.toEnumList() ?: default
    }

    /**
     * Returns the List of type [T] associated with the given [key]
     *
     * @throws JsonException If the key doesn't exist
     */
    inline fun <reified T> getList(key: String, transform: (Any?) -> T): List<T> {
        return getJsonArray(key).toList(transform)
    }

    /**
     * Returns an optional List of type [T] associated with the given [key] or [emptyList]
     * if the value is null or the [key] doesn't exist
     */
    inline fun <reified T> optList(key: String, transform: (Any?) -> T): List<T> {
        return optJsonArray(key).toList(transform)
    }

    /**
     * Returns an optional List of type [T] associated with the given [key] or [default]
     * if the value is null or the [key] doesn't exist
     */
    inline fun <reified T> optList(key: String, default: List<T>?, transform: (Any?) -> T): List<T>? {
        return optJsonArray(key, null)?.toList(transform) ?: default
    }

    /**
     * Returns the object of type [T] associated with the given [key]
     *
     * @throws JsonException If the key doesn't exist
     */
    inline fun <T> getObject(key: String, transform: (JsonObject) -> T): T {
        return transform(getJsonObject(key))
    }

    /**
     * Returns an optional object of type [T] associated with the given [key] or [default]
     * if the value is null or the [key] doesn't exist
     */
    inline fun <T> optObject(key: String, default: T? = null, transform: (JsonObject) -> T): T? {
        return transform(optJsonObject(key)) ?: default
    }

    /**
     * Returns the List of objects of type [T] associated with the given [key]
     *
     * @throws JsonException If the key doesn't exist
     */
    inline fun <T> getObjectList(key: String, transform: (JsonObject) -> T): List<T> {
        return getJsonArray(key).toObjectList(transform)
    }

    /**
     * Returns an optional List of objects of type [T] associated with the given [key] or [emptyList]
     * if the value is null or the [key] doesn't exist
     */
    inline fun <reified T> optObjectList(key: String, transform: (JsonObject) -> T): List<T> {
        return optJsonArray(key).toObjectList(transform)
    }

    /**
     * Returns an optional List of objects of type [T] associated with the given [key] or [default]
     * if the value is null or the [key] doesn't exist
     */
    inline fun <reified T> optObjectList(key: String, default: List<T>?, transform: (JsonObject) -> T): List<T>? {
        return optJsonArray(key, null)?.toObjectList(transform) ?: default
    }
}