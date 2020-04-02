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
package com.maxpilotto.kon.arrays

import com.maxpilotto.kon.JsonObject

fun main() {
    val json =
        """
        {
            "name": "Nested arrays",
            "array": [
                [
                    [
                        [ "apples", "bananas", "oranges" ]
                    ]
                ]
            ]
        }
    """.trimIndent()
    val obj = JsonObject(json)
    val apple = obj["array"][0][0][0][0].asString()

    println(apple)
}