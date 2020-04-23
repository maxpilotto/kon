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
    open operator fun set(key: String, element: Any?) { //FIXME Both set methods should take care of wrapping lists and maps in JsonArray and JsonObject
        throw JsonException("Operation not supported")
    }

    /**
     * Sets the given [element] at the given [index] and
     * returns the item previously at the given [index]
     *
     * If the [element] is a [JsonValue], the value will be unwrapped
     */
    open operator fun set(index: Int, element: Any?): Any? {
        throw JsonException("Operation not supported")
    }

    /**
     * Unwraps the given [value] if it is inside a [JsonValue],
     * otherwise it will return the [value] itself
     *
     * This is a handy method that handles the type check
     */
    protected fun unwrap(value: Any?): Any? {
        if (value is JsonValue) {
            return value.content
        }

        return value
    }

    /**
     * Returns this entity as a pretty printable output
     */
    //TODO Add indent option
    //TODO Add dateFormat, so all dates can print accordingly
    //TODO Add params
    // showType, shows the type of the property
    // useBraces, shows {properties} instead of JsonObject(properties), enabled by default
    abstract fun prettify(): String
}