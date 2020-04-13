package com.maxpilotto.kon.samples.operators

import com.maxpilotto.kon.JsonArray

fun main() {
    val array = JsonArray()

    array += "Hello"
    array += "world"
    array += "!"

    println(array)
}