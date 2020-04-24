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
import java.math.BigDecimal
import java.net.URL
import java.text.DateFormat
import java.util.*

/**
 * # JsonArray
 *
 * Representation of a JSON array, which is implemented using a mutable list of Any?
 *
 * The class does implement all of the methods of the [MutableList] interface
 *
 * ## Adding elements
 *
 * Elements can be added using the [add] and [addAll] methods or the [plusAssign] operator
 *
 * The methods for adding a single value can also work with a [JsonValue], this will be
 * unwrapped and added to the array
 *
 * ## Removing elements
 *
 * Elements can be removed using the [remove], [removeAt], [removeAll] methods or the [minusAssign] operator
 *
 * The [remove] and [minusAssign] methods that takes a single value will do
 * any unwrapping of the value if necessary
 *
 * ## Get/Set operators
 *
 * Both the [get] and [set] will operate with [JsonValue], in the last case you don't necessary need
 * to pass it a [JsonValue], the value will be unwrapped anyway
 *
 * ## Other method that can work with [JsonValue]
 *
 * + [contains]
 * + [indexOf]
 */
class JsonArray : Json, MutableList<Any?> {
    private val list: MutableList<Any?>

    /**
     * Number of elements in this [JsonArray]
     */
    override val size: Int
        get() = list.size

    /**
     * Creates an empty JsonArray
     */
    constructor() : this(emptyList<Any?>())

    /**
     * Creates a JsonArray from the given [string]
     */
    constructor(string: String) : this(JsonParser(string).nextArray())

    /**
     * Clones the given [jsonArray]
     */
    constructor(jsonArray: JsonArray) : this(jsonArray.toList())

    /**
     * Creates a JsonArray from the given [items]
     */
    constructor(vararg items: Any?) : this(items.toList())

    /**
     * Creates a JsonArray from the given [collection]
     *
     * This won't work unwrap the values if they're [JsonValue] instances
     */
    constructor(collection: Collection<*>) {
        this.list = collection.toMutableList()
    }

    override fun toString(): String {
        return prettify()
    }

    override fun prettify(): String {
        return list.joinToString(",", "[", "]", transform = {
            when (it) {
                is String -> "\"$it\""

                else -> it.toString()
            }
        })
    }

    override fun get(index: Int): JsonValue {
        return list[index].toJsonValue()
    }

    override fun set(index: Int, element: Any?): Any? {
        val value = unwrap(element).let {
            when (it) {
                is Map<*, *> -> JsonObject(it as Map<String, Any?>)
                is Collection<*> -> JsonArray(it)

                else -> it
            }
        }

        return list.set(index, value)
    }

    override fun contains(element: Any?): Boolean {
        return list.contains(unwrap(element))
    }

    override fun containsAll(elements: Collection<Any?>): Boolean {
        return list.containsAll(elements)
    }

    override fun indexOf(element: Any?): Int {
        return list.indexOf(unwrap(element))
    }

    override fun isEmpty(): Boolean {
        return size == 0
    }

    override fun iterator(): MutableIterator<Any?> {
        return list.iterator()
    }

    override fun lastIndexOf(element: Any?): Int {
        return list.lastIndexOf(element)
    }

    override fun add(element: Any?): Boolean {
        return list.add(unwrap(element))
    }

    override fun add(index: Int, element: Any?) {
        return list.add(index, unwrap(element))
    }

    override fun addAll(index: Int, elements: Collection<Any?>): Boolean {
        return list.addAll(index, elements)
    }

    override fun addAll(elements: Collection<Any?>): Boolean {
        return addAll(elements)
    }

    override fun clear() {
        list.clear()
    }

    override fun listIterator(): MutableListIterator<Any?> {
        return list.listIterator()
    }

    override fun listIterator(index: Int): MutableListIterator<Any?> {
        return list.listIterator(index)
    }

    override fun remove(element: Any?): Boolean {
        return list.remove(unwrap(element))
    }

    override fun removeAll(elements: Collection<Any?>): Boolean {
        return list.removeAll(elements)
    }

    override fun removeAt(index: Int): Any? {
        return list.removeAt(index)
    }

    override fun retainAll(elements: Collection<Any?>): Boolean {
        return list.retainAll(elements)
    }

    override fun subList(fromIndex: Int, toIndex: Int): MutableList<Any?> {
        return list.subList(fromIndex, toIndex)
    }

    operator fun plusAssign(element: Any?) {
        add(unwrap(element))
    }

    operator fun plusAssign(elements: Collection<Any?>) {
        addAll(elements)
    }

    operator fun minusAssign(element: Any?) {
        remove(element)
    }

    operator fun minusAssign(elements: Collection<Any?>) {
        removeAll(elements)
    }

    /**
     * Pops the last item of this [JsonArray] and returns the same instance
     */
    operator fun dec(): JsonArray {
        return also {
            it.removeAt(lastIndex)  //TODO Should I return a clone and leave the original unaltered?
        }
    }

    /**
     * Returns the unwrapped value at the given [index]
     */
    fun getValue(index: Int): Any? {
        return list[index]
    }

    /**
     * Returns a copy of the list used internally to implement this [JsonArray]
     */
    fun toList(): List<Any?> {
        return list.toList()
    }

    /**
     * Returns this [JsonArray] as a List of String
     */
    fun toStringList(): List<String> {
        return List(size) {
            cast<String>(list[it])
        }
    }

    /**
     * Returns this [JsonArray] as a List of Number
     */
    fun toNumberList(): List<Number> {
        return List(size) {
            cast<Number>(list[it])
        }
    }

    /**
     * Returns this [JsonArray] as a List of [JsonObject]
     */
    fun toJsonObjectList(): List<JsonObject> {
        return List(size) {
            cast<JsonObject>(list[it])
        }
    }

    /**
     * Returns this [JsonArray] as a List of Int
     */
    fun toIntList(): List<Int> {
        return List(size) {
            cast<Int>(list[it])
        }
    }

    /**
     * Returns this [JsonArray] as a List of Long
     */
    fun toLongList(): List<Long> {
        return List(size) {
            cast<Long>(list[it])
        }
    }

    /**
     * Returns this [JsonArray] as a List of Boolean
     */
    fun toBooleanList(): List<Boolean> {
        return List(size) {
            cast<Boolean>(list[it])
        }
    }

    /**
     * Returns this [JsonArray] as a List of Double
     */
    fun toDoubleList(): List<Double> {
        return List(size) {
            cast<Double>(list[it])
        }
    }

    /**
     * Returns this [JsonArray] as a List of Float
     */
    fun toFloatList(): List<Float> {
        return List(size) {
            cast<Float>(list[it])
        }
    }

    /**
     * Returns this [JsonArray] as a List of Byte
     */
    fun toByteList(): List<Byte> {
        return List(size) {
            cast<Byte>(list[it])
        }
    }

    /**
     * Returns this [JsonArray] as a List of Short
     */
    fun toShortList(): List<Short> {
        return List(size) {
            cast<Short>(list[it])
        }
    }

    /**
     * Returns this [JsonArray] as a List of Char
     */
    fun toCharList(): List<Char> {
        return List(size) {
            cast<Char>(list[it])
        }
    }

    /**
     * Returns this [JsonArray] as a List of [Date]
     */
    fun toDateList(): List<Date> {
        return List(size) {
            cast<Date>(list[it])
        }
    }

    /**
     * Returns this [JsonArray] as a List of [Date]
     */
    fun toDateList(dateFormat: DateFormat): List<Date> {
        return List(size) {
            castDate<Date>(list[it], dateFormat)
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
            castDate<Date>(list[it], format, locale)
        }
    }

    /**
     * Returns this [JsonArray] as a List of [Calendar]
     */
    fun toCalendarList(): List<Calendar> {
        return List(size) {
            cast<Calendar>(list[it])
        }
    }

    /**
     * Returns this [JsonArray] as a List of [Calendar]
     */
    fun toCalendarList(dateFormat: DateFormat): List<Calendar> {
        return List(size) {
            castDate<Calendar>(list[it], dateFormat)
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
            castDate<Calendar>(list[it], format, locale)
        }
    }

    /**
     * Returns this [JsonArray] as a List of [IntRange]
     */
    fun toRangeList(): List<IntRange> {
        return List(size) {
            cast<IntRange>(list[it])
        }
    }

    /**
     * Returns this [JsonArray] as a List of [BigDecimal]
     */
    fun toBigDecimalList(): List<BigDecimal> {
        return List(size) {
            cast<BigDecimal>(list[it])
        }
    }

    /**
     * Returns this [JsonArray] as a List of [URL]
     */
    fun toURLList(): List<URL> {
        return List(size) {
            cast<URL>(list[it])
        }
    }

    /**
     * Returns this [JsonArray] as a List of [T], which values
     * are parsed using the [transform] block
     */
    inline fun <T> toList(transform: (Any?) -> T): List<T> {
        val list = toList()

        return List(size) {
            transform(list[it])
        }
    }

    /**
     * Returns this [JsonArray] as a List of Enum of type [T]
     */
    inline fun <reified T : Enum<T>> toEnumList(    //TODO Improve support for Java with @JavaOverloads
        transform: (String) -> String = { it.capitalize() }
    ): List<T> {
        val list = toList()

        return List(size) {
            list[it].toJsonValue().asEnum<T>(transform)
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