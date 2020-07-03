package com.maxpilotto.kon.samples

import com.maxpilotto.kon.JsonObject
import java.text.SimpleDateFormat

fun main() {
    val format = SimpleDateFormat("yyyy/MM/dd")
    val default = "2020/07/03"
    val json = JsonObject(
        """
        {
            "date1": "",
            "date2": "null"
        }
        """
    )

    println(json.getString("date1"))
    println(json.optDate("date1",format,default))
    println(json.optDate("date2",format,default))
    println(json.optDate("date3",format,default))
}