package com.maxpilotto.kon.samples.operators

import com.maxpilotto.kon.JsonObject

fun main() {
    val json = JsonObject(
        """
        {
            "people": [
                {
                    "data": {
                        "dob": 1586723311
                    }
                }
            ]
        }
        """.trimIndent()
    )
    val with = json["people"][0]["data"]["dob"].asDate()
    val without = (json.getJsonArray("people").getValue(0) as JsonObject).getJsonObject("data").getDate("dob")

    println(with)
    println(without)
}