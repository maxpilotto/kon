package com.maxpilotto.kon.samples

import com.maxpilotto.kon.JsonArray
import com.maxpilotto.kon.JsonObject
import com.maxpilotto.kon.JsonWriter

fun main() {
    val array = JsonArray(
        JsonArray(10, 20, 30),        //TODO JsonArray of arrays doesn't work
        JsonArray(10, 20, 30),
        JsonArray(10, 20, 30)
    )
    val obj = JsonObject(
        "firstName" to "John",
        "lastName" to "Doe",
        "array" to JsonArray(1, 2, 3, 4)
    )

//    println(array)
//    println(array.prettify("\t",true))

    println(
        JsonWriter()
            .write(array)
            .intoString("\t", true)
    )
    println(
        JsonWriter()
            .write(obj)
            .intoString("\t", true)
    )
}