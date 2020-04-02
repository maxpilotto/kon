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
package com.maxpilotto.kon

import java.net.URL
import kotlin.concurrent.thread

class JsonRequest {
    companion object {
        internal fun fetchSync(url: String): JsonParser {
            val stream = URL(url).openConnection().getInputStream()   //TODO Wrap around a try-catch

            return JsonParser(stream)
        }

        fun fetchObjectSync(url: String): JsonObject {
            return fetchSync(url).nextObject()
        }

        fun fetchArraySync(url: String): JsonArray {
            return fetchSync(url).nextArray()
        }

        fun fetchObject(url: String, callback: (JsonObject) -> Unit) {
            thread {
                callback(fetchObjectSync(url))
            }
        }

        fun fetchArray(url: String, callback: (JsonArray) -> Unit) {
            thread {
                callback(fetchArraySync(url))
            }
        }
    }
}