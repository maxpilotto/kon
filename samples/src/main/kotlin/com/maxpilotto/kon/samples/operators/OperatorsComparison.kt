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
    val dob1 = json["people"][0]["data"]["dob"].asDate()
    val dob2 = (json.getJsonArray("people").getValue(0) as JsonObject).getJsonObject("data").getDate("dob")
}