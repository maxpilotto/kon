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

data class Genre(
    val id: Long,
    val name: String
)

fun main() {
    val obj = JsonObject(
        """
        {
            "name": "Object array",
            "genres": [
                {
                    "id": 10,
                    "name": "Action"
                },
                {
                    "id": 30,
                    "name": "Science fiction"
                },
                {
                    "id": 303,
                    "name": "Adventure"
                },
                {
                    "id": 32,
                    "name": "Romantic"
                }
            ]
        }
        """
    )
    val array = obj["genres"].asJsonArray()
    val genres = array.toObjectList {
        Genre(
            it.getValue("id") as Long,
            it.getValue("name") as String
        )
    }

    println(genres)
}