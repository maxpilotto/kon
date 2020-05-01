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
package com.maxpilotto.kon.extensions

import com.maxpilotto.kon.JsonArray
import com.maxpilotto.kon.JsonObject
import com.maxpilotto.kon.JsonValue
import java.net.URL

/**
 * Returns this entity as a pretty printable output
 */
//TODO Add indent option
//TODO Add dateFormat, so all dates can print accordingly
//TODO Add params
// showType, shows the type of the property
// useBraces, shows {properties} instead of JsonObject(properties), enabled by default
fun Any?.prettify(): String {
    return when (this) {
        is String,
        is IntRange,
        is URL,
        is Enum<*> -> "\"$this\""

        is JsonValue -> content.prettify()
        is JsonObject -> entries.joinToString(",", "{", "}", transform = {
            "\"${it.key}\":${it.value.prettify()}"
        })
        is JsonArray -> toList().joinToString(",", "[", "]", transform = {
            it.prettify()
        })

        else -> toString()
    }
}

/**
 * Wraps this value around a [JsonValue]
 *
 * If this is already a [JsonValue] no extra wrapping will be applied
 */
fun Any?.toJsonValue(): JsonValue {
    if (this is JsonValue) {
        return this
    }

    return when (this) {
        true -> JsonValue.TRUE
        false -> JsonValue.FALSE
        null -> JsonValue.NULL

        else -> JsonValue(this)
    }
}

/**
 * Returns a [JsonObject] created from this string
 */
fun String.toJsonObject() = JsonObject(this)

/**
 * Returns a [JsonObject] created from this map
 */
fun <K, V> Map<K, V>.toJsonObject() = JsonObject(this)

/**
 * Returns a [JsonArray] created from this string
 */
fun String.toJsonArray() = JsonArray(this)

/**
 * Returns a [JsonArray] created from this collection
 */
fun <E> Collection<E>.toJsonArray() = JsonArray(this)