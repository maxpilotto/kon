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

import com.maxpilotto.kon.JsonValue
import com.maxpilotto.kon.JsonObject
import com.maxpilotto.kon.JsonArray
import com.maxpilotto.kon.util.JsonException

/**
 * Interface containing all the access operators used by
 * Json entities like the [JsonObject] and [JsonArray]
 */
interface Json {
    /**
     * Returns a JsonValue for the given [key]
     *
     * @throws JsonException If the given [key] doesn't exist
     */
    operator fun get(key: String): JsonValue {
        throw JsonException("Operation not supported")
    }

    /**
     * Returns a JsonValue at the given [index]
     */
    operator fun get(index: Int): JsonValue {
        throw JsonException("Operation not supported")
    }

    /**
     * Sets the given [element] for the given [key]
     *
     * The [element] will be wrapped around a [JsonValue] if needed
     */
    operator fun set(key: String, element: Any?) {
        throw JsonException("Operation not supported")
    }

    /**
     * Sets the given [element] at the given [index] and
     * returns the item previously at the given [index]
     *
     * The [element] will be wrapped around a [JsonValue] if needed
     */
    operator fun set(index: Int, element: Any?): JsonValue {
        throw JsonException("Operation not supported")
    }

    /**
     * Wraps the given [value] inside a [JsonValue] object
     *
     * If [value] is already a [JsonValue],
     * the value won't be wrapped again
     */
    fun wrap(value: Any?): JsonValue {
        if (value is JsonValue) {
            return value
        }

        return when (value) {
            true -> JsonValue.TRUE
            false -> JsonValue.FALSE
            null -> JsonValue.NULL

            else -> JsonValue(value)
        }
    }

    /**
     * Unwraps the given [value] if it is inside a [JsonValue]
     *
     * If the [value] is already a raw value,
     * nothing will change
     */
    fun unwrap(value: Any?): Any? {
        if (value is JsonValue){
            return value.content
        }

        return value
    }

    /**
     * Wraps all items of the given [collection] inside a [JsonValue] object,
     * resulting in a List of [JsonValue]
     *
     * If values are already instances of the [JsonValue] class,
     * they won't be wrapped again
     */
    fun wrap(collection: Collection<Any?>): List<JsonValue> {
        val list = mutableListOf<JsonValue>()

        for (item in collection) {
            list.add(wrap(item))
        }

        return list
    }
}