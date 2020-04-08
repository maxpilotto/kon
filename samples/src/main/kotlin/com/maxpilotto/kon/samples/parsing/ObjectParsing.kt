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

data class Address(
    val street: String,
    val number: Int,
    val country: String
)

fun main() {
    val json = JsonObject(
        """
        {
            "firstName": "John",
            "lastName": "Doe",
            "address": {
                "street": "Downing Street",
                "number": 10,
                "country": "England"
            }
        }
        """
    )
    val address = json["address"].asObject {
        Address(
            it.getString("street"),
            it.getInt("number"),
            it.getString("country")
        )
    }
}