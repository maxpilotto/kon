package com.maxpilotto.kon.samples

import com.maxpilotto.kon.JsonObject
import java.text.SimpleDateFormat
import java.util.*

fun main() {
    val d1 = Date()
    val json = JsonObject()

    json["date", "yyyy-MM-dd"] = d1

    println(json["date"])
}