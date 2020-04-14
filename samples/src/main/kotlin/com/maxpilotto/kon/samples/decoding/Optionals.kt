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
package com.maxpilotto.kon.samples.decoding

import com.maxpilotto.kon.JsonObject

fun main() {
    val json = JsonObject(
        """
        {
            "firstName": "John",
            "lastName": "Doe",
            "age": "null"
        }
        """
    )

    // Using JsonValue
    println(json.opt("age", 32))
    println(json.get("age"))

    // Using Any
    println(json.optValue("age", 32))
    println(json.getValue("age"))

    // Using String
    println(json.optString("age", "32"))
    println(json.getString("age"))

    // Using Int
    println(json.optInt("age", 32))

    try {
        println(json.getInt("age"))     // null cannot be parsed as Int
    } catch (e: Exception) {
        println(e.message)
    }

}