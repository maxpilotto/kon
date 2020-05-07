package com.maxpilotto.kon.samples

import com.maxpilotto.kon.extensions.toJsonValue

private enum class Animals {
    Dog,
    Cat,
    Fox
}

fun main() {
    val v1 = "Dog".toJsonValue()
    val v2 = "CAT".toJsonValue()
    val v3 = "fox".toJsonValue()

    println(v1.asEnum<Animals>())
    println(v2.asEnum<Animals>())
    println(v3.asEnum<Animals>())
}