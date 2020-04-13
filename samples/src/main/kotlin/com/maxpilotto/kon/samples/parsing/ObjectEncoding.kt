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
package com.maxpilotto.kon.samples.parsing

import com.maxpilotto.kon.JsonObject
import com.maxpilotto.kon.annotations.Codable
import java.util.*

data class Genre(
    val id: Int,
    val name: String
)

@Codable
data class Author(
    val firstName: String,
    val lastName: String,
    val year: Int
)

@Codable
data class Book(
    val title: String,
    val year: Int,
    val author: Author,
    val date: Date = Date(),
    val genres: List<List<Genre>> = listOf(
        listOf(
            Genre(0, "Dystopian Fiction"),
            Genre(0, "Science fiction"),
            Genre(0, "Social science fiction")
        )
    ),
    val map: Map<String, Int> = mapOf(
        "first" to 1,
        "second" to 2,
        "third" to 3
    )
)

fun main() {
    val author = Author("George", "Orwell", 1903)
    val book = Book(
        "1984", 1948, author
    )
    val json = book.encode {
        when (it) {
            is Date -> it.time
            is Genre -> JsonObject().apply {
                //TODO Add a constructor that takes a list of Entities
                //TODO Create a JsonObject builder

                set("id", it.id)
                set("name", it.name)
            }

            else -> null
        }
    }

    println(json)

    try {
        JsonObject(json)

        println("Json is valid")
    } catch (e: Exception) {
        println("Json is not valid")
    }

//    val author2 = Author.fromJson(json)

//    println(author == author2)
}