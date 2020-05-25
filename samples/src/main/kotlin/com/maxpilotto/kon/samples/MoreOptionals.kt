package com.maxpilotto.kon.samples

import com.maxpilotto.kon.JsonArray
import com.maxpilotto.kon.JsonObject
import java.util.*

private enum class Genre {
    Action,
    Western,
    Adventure,
    Comedy,
    Null
}

fun main() {
    val json = JsonObject(
        "array" to JsonArray(12, 3, 4, 5),
        "test" to "John Doe",
        "date" to Date(),
        "date3" to "2020-05-20",
        "enum" to Genre.Western
    )

    println(json)

    println(json.getJsonArray("array"))
    println(json.optJsonArray("array2"))
    println(json.optJsonArray("array2",null))
    println(json.optJsonArray("array2", JsonArray(0, 0, 0, 0)))

    println(json.getDate("date"))
    println(json.optDate("date2", 0))
    println(json.optDate("date2", null))

    println(json.getDate("date3", "yyyy-MM-dd"))
    println(json.optDate("date4", "yyyy-MM-dd", "1986-05-20"))

    println(json.getEnum<Genre>("enum"))
    println(json.optEnum<Genre>("enum2"))
    println(json.optEnum<Genre>("enum2",null))
    println(json.optEnum<Genre>("enum2",Genre.Null))

    println(json.optString("abc",150))
    println(json.optJsonObject("abc","{}"))
    println(json.optInt("abc",false))
    println(json.optChar("abc","c"))
    println(json.optRange("abc",20))
    println(json.optRange("abc","20..50"))
    println(json.optURL("abc","https://www.google.com"))

    println(json.getIntList("array"))
    println(json.optIntList("array2"))
    println(json.optIntList("array2", listOf(0,0)))
}
