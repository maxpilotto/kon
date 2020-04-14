package com.maxpilotto.kon.samples.encoding

import com.maxpilotto.kon.annotations.Codable

@Codable
data class Author(
    val firstName: String,
    val lastName: String,
    val year: Int
)

fun main() {
    val author = Author("George", "Orwell", 1903)
    val json = author.encode()

    println(json)
}