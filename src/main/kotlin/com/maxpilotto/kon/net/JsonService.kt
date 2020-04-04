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
package com.maxpilotto.kon.net

import com.maxpilotto.kon.JsonArray
import com.maxpilotto.kon.JsonObject
import com.maxpilotto.kon.JsonParser
import java.net.URL
import kotlin.concurrent.thread

class JsonService private constructor() {
    companion object {
        internal fun fetch(url: String): JsonParser {
            val stream = URL(url).openConnection().getInputStream()   //TODO Wrap around a try-catch and return an error

            return JsonParser(stream)
        }

        @JvmStatic
        fun fetchObjectList(url: String): List<JsonObject> {
            return fetch(url).nextArray().toJsonObjectList()
        }

        @JvmStatic
        fun fetchObject(url: String): JsonObject {
            return fetch(url).nextObject()
        }

        @JvmStatic
        fun fetchArray(url: String): JsonArray {
            return fetch(url).nextArray()
        }

        @JvmStatic
        inline fun fetchObject(url: String, crossinline callback: (JsonObject) -> Unit) {
            thread {
                callback(fetchObject(url))
            }
        }

        @JvmStatic
        inline fun fetchArray(url: String, crossinline callback: (JsonArray) -> Unit) {
            thread {
                callback(fetchArray(url))
            }
        }

        @JvmStatic
        inline fun fetchObjectList(url: String, crossinline callback: (List<JsonObject>) -> Unit) {
            thread {
                callback(fetchObjectList(url))
            }
        }
    }
}