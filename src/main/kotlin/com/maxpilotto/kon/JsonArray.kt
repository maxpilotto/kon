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
import java.math.BigDecimal
import java.net.URL
import java.text.DateFormat
import java.util.*

class JsonArray : Json, MutableList<Any?> {
    private val list: MutableList<JsonValue>

    /**
     * Number of elements in this [JsonArray]
     */
    override val size: Int
        get() = list.size

    constructor() : this(listOf())

    constructor(string: String) : this(JsonParser(string).nextArray())

    constructor(jsonArray: JsonArray) : this(jsonArray.list)

    constructor(list: List<JsonValue>) {
        this.list = list.toMutableList()
    }

    override fun toString(): String {
        return list.joinToString(",", "[", "]")
    }

    override fun get(index: Int): JsonValue {
        return list[index]
    }

    override fun set(index: Int, element: Any?): JsonValue {
        return list.set(index, wrap(element))
    }

    override fun contains(element: Any?): Boolean {
        return list.contains(wrap(element))
    }

    override fun containsAll(elements: Collection<Any?>): Boolean {
        return list.containsAll(wrap(elements))
    }

    override fun indexOf(element: Any?): Int {
        return list.indexOf(wrap(element))
    }

    override fun isEmpty(): Boolean {
        return size == 0
    }

    override fun iterator(): MutableIterator<JsonValue> {
        return list.iterator()
    }

    override fun lastIndexOf(element: Any?): Int {
        return list.lastIndexOf(wrap(element))
    }

    override fun add(element: Any?): Boolean {
        return list.add(wrap(element))
    }

    override fun add(index: Int, element: Any?) {
        return list.add(index, wrap(element))
    }

    override fun addAll(index: Int, elements: Collection<Any?>): Boolean {
        return list.addAll(index, wrap(elements))
    }

    override fun addAll(elements: Collection<Any?>): Boolean {
        return addAll(wrap(elements))
    }

    override fun clear() {
        list.clear()
    }

    override fun listIterator(): MutableListIterator<Any?> {
        return toValueList().toMutableList().listIterator()
    }

    override fun listIterator(index: Int): MutableListIterator<Any?> {
        return toValueList().toMutableList().listIterator(index)
    }

    override fun remove(element: Any?): Boolean {
        return list.remove(wrap(element))
    }

    override fun removeAll(elements: Collection<Any?>): Boolean {
        return list.removeAll(wrap(elements))
    }

    override fun removeAt(index: Int): Any? {
        return list.removeAt(index)
    }

    override fun retainAll(elements: Collection<Any?>): Boolean {
        return list.retainAll(wrap(elements))
    }

    override fun subList(fromIndex: Int, toIndex: Int): MutableList<Any?> {
        return toValueList().subList(fromIndex, toIndex).toMutableList()
    }

    /**
     * Returns the unwrapped value at the given [index]
     */
    fun getValue(index: Int): Any? {
        return list[index].content
    }

    /**
     * Returns an iterator that iterates through the unwrapped values of this [JsonArray]
     *
     * The [iterator] method will return an iterator of [JsonValue], this instead returns
     * an iterator of [Any] which are the unwrapped values inside each of the [JsonValue]
     */
    fun values(): MutableIterator<Any?> {
        return toValueList().toMutableList().iterator()
    }

    /**
     * Returns a List containing all of the unwrapped values of this [JsonArray]
     */
    fun toValueList(): List<Any?> {
        return List(size) {
            list[it].content
        }
    }

    /**
     * Returns a List containing all of the values of this [JsonArray]
     */
    fun toList(): List<JsonValue> {
        return list.toList()
    }

    /**
     * Returns this [JsonArray] as a List of String
     */
    fun toStringList(): List<String> {
        return List(size) {
            list[it].asString()
        }
    }

    /**
     * Returns this [JsonArray] as a List of Number
     */
    fun toNumberList(): List<Number> {
        return List(size) {
            list[it].asNumber()
        }
    }

    /**
     * Returns this [JsonArray] as a List of [JsonObject]
     */
    fun toJsonObjectList(): List<JsonObject> {
        return List(size) {
            list[it].asJsonObject()
        }
    }

    /**
     * Returns this [JsonArray] as a List of Int
     */
    fun toIntList(): List<Int> {
        return List(size) {
            list[it].asInt()
        }
    }

    /**
     * Returns this [JsonArray] as a List of Long
     */
    fun toLongList(): List<Long> {
        return List(size) {
            list[it].asLong()
        }
    }

    /**
     * Returns this [JsonArray] as a List of Boolean
     */
    fun toBooleanList(): List<Boolean> {
        return List(size) {
            list[it].asBoolean()
        }
    }

    /**
     * Returns this [JsonArray] as a List of Double
     */
    fun toDoubleList(): List<Double> {
        return List(size) {
            list[it].asDouble()
        }
    }

    /**
     * Returns this [JsonArray] as a List of Float
     */
    fun toFloatList(): List<Float> {
        return List(size) {
            list[it].asFloat()
        }
    }

    /**
     * Returns this [JsonArray] as a List of Byte
     */
    fun toByteList(): List<Byte> {
        return List(size) {
            list[it].asByte()
        }
    }

    /**
     * Returns this [JsonArray] as a List of Short
     */
    fun toShortList(): List<Short> {
        return List(size) {
            list[it].asShort()
        }
    }

    /**
     * Returns this [JsonArray] as a List of Char
     */
    fun toCharList(): List<Char> {
        return List(size) {
            list[it].asChar()
        }
    }

    /**
     * Returns this [JsonArray] as a List of [Date]
     */
    fun toDateList(): List<Date> {
        return List(size) {
            list[it].asDate()
        }
    }

    /**
     * Returns this [JsonArray] as a List of [Date]
     */
    fun toDateList(dateFormat: DateFormat): List<Date> {
        return List(size) {
            list[it].asDate(dateFormat)
        }
    }

    /**
     * Returns this [JsonArray] as a List of [Date]
     */
    fun toDateList(
        format: String,
        locale: Locale = Locale.getDefault()
    ): List<Date> {
        return List(size) {
            list[it].asDate(format, locale)
        }
    }

    /**
     * Returns this [JsonArray] as a List of [Calendar]
     */
    fun toCalendarList(): List<Calendar> {
        return List(size) {
            list[it].asCalendar()
        }
    }

    /**
     * Returns this [JsonArray] as a List of [Calendar]
     */
    fun toCalendarList(dateFormat: DateFormat): List<Calendar> {
        return List(size) {
            list[it].asCalendar(dateFormat)
        }
    }

    /**
     * Returns this [JsonArray] as a List of [Calendar]
     */
    fun toCalendarList(
        format: String,
        locale: Locale = Locale.getDefault()
    ): List<Calendar> {
        return List(size) {
            list[it].asCalendar(format, locale)
        }
    }

    /**
     * Returns this [JsonArray] as a List of [IntRange]
     */
    fun toRangeList(): List<IntRange> {
        return List(size) {
            list[it].asRange()
        }
    }

    /**
     * Returns this [JsonArray] as a List of [BigDecimal]
     */
    fun toBigDecimalList(): List<BigDecimal> {
        return List(size) {
            list[it].asBigDecimal()
        }
    }

    /**
     * Returns this [JsonArray] as a List of [URL]
     */
    fun toURLList(): List<URL> {
        return List(size) {
            list[it].asURL()
        }
    }

    /**
     * Returns this [JsonArray] as a List of Enum of type [T]
     */
    inline fun <reified T : Enum<T>> toEnumList(
        transform: (String) -> String = { it.capitalize() }
    ): List<T> {
        val list = toList()

        return List(size) {
            list[it].asEnum<T>(transform)
        }
    }

    /**
     * Returns this [JsonArray] as a List of [T], which values
     * are parsed using the [transform] block
     */
    inline fun <T> toList(transform: (JsonValue) -> T): List<T> {
        val list = toList()

        return List(size) {
            transform(list[it])
        }
    }

    /**
     * Returns this [JsonArray] as a List of objects of type [T], the values
     * are parsed using the [transform] block
     *
     * Note: This [JsonArray] must be an array of [JsonObject]
     */
    inline fun <T> toObjectList(transform: (JsonObject) -> T): List<T> {
        val list = toJsonObjectList()

        return List(size) {
            transform(list[it])
        }
    }
}