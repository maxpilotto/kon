package com.maxpilotto.kon

import com.maxpilotto.kon.protocols.Json
import java.net.URL

//TODO Class that writes JsonObjects to file, url or stream
class JsonWriter {
    var json: Json = JsonValue.NULL

    fun write(json: Json) = apply { this.json = json }

//    fun into(url: URL, onComplete: () -> Unit) {
//
//    }

//    fun into(
//        string: StringBuilder,
//        indent: String = "",
//        lineBreak: Boolean = false
//    ) {
//        string.append(json)
//    }

//    fun intoBuffer(): StringBuffer {
//        return StringBuffer(print())
//    }

    fun intoString(
        indent: String,
        lineBreak: Boolean
    ): String {
        return print(json, indent, "", lineBreak)
    }

    private fun print(
        json: Json,
        indent: String,
        currentIndent: String,
        lineBreak: Boolean
    ): String {
        val br = if (lineBreak) "\n" else ""

        return buildString {
            val string = when (json) {
                is JsonObject -> json.entries.joinToString(",$br", "$currentIndent{$br", "$br$currentIndent}") {
                    val value = when (it.value) {
                        is JsonObject,
                        is JsonArray -> print(
                            it.value as Json,
                            indent,
                            currentIndent + indent,
                            lineBreak
                        )

                        else ->  stringify(it.value)
                    }

                    currentIndent + indent + "\"${it.key}\":${(value)}"
                }
                is JsonArray -> json.toList().joinToString(",$br", "[$br", "$br$currentIndent]") {
                    when (it) {
                        is JsonObject,
                        is JsonArray -> print(
                            it as Json,
                            indent,
                            currentIndent + indent,
                            lineBreak
                        )

                        else -> currentIndent + indent + stringify(it)
                    }
                }

                else -> stringify(json)
            }

            append(string)
        }
    }
}