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

import com.maxpilotto.kon.extensions.notInside
import com.maxpilotto.kon.util.JsonException
import java.io.File
import java.io.InputStream
import java.io.Reader

/**
 * Json parsing utility
 *
 * This utility is meant to be an alternative to the [JsonObject] and [JsonArray] constructors
 * which can only parse from strings
 *
 * The [nextObject] and [nextArray] are the only public methods, which can be used to
 * parse the content into a [JsonObject] or [JsonArray]
 */
class JsonParser {     //TODO Add option for detailed errors using the IndexPath class
    private var reader: Reader
    private var previous = END
    private var eof = false
    private var usePrevious = false

    /**
     * Creates a parser with the content of the given [file]
     */
    constructor(file: File) : this(file.reader())

    /**
     * Creates a parser with the content of the given [inputStream]
     */
    constructor(inputStream: InputStream) : this(inputStream.reader())

    /**
     * Creates a parse with the content of the given [string]
     */
    constructor(string: String) : this(string.reader())

    /**
     * Creates a parse with the content of the given [reader]
     */
    constructor(reader: Reader) {
        this.reader = reader
    }

    /**
     * Returns the next value as a [JsonObject]
     *
     * A json object must be surrounded by curly brackets
     */
    fun nextObject(): JsonObject {
        val result = JsonObject()
        var key: String?

        if (next() != '{') {
            throw JsonException("A JSON object must begin with '{'")
        }

        loop@ while (true) {
            // Key
            when (next()) {
                END -> {
                    throw JsonException("A JSON object must end with '}'")
                }
                '}' -> {
                    return result
                }

                else -> {
                    back()

                    key = nextValue().toString()
                }
            }

            // Key separator
            if (next() != ':') {
                throw JsonException("Expected ':' after a key")
            }

            // Value associated with the key, if the key is not a duplicate
            if (result.has(key)) {
                throw JsonException("Duplicate key: $key")
            } else {
                result[key] = nextValue()
            }

            when (next()) {
                ';', ',' -> {
                    if (next() == '}') {
                        return result
                    }

                    back()
                }
                '}' -> return result

                else -> throw JsonException("Expected a ',' or '}'")
            }
        }
    }

    /**
     * Returns the next value as a [JsonArray]
     *
     * A json array must be surrounded by square brackets
     */
    fun nextArray(): JsonArray {
        val result = JsonArray()
        var c = next()

        // Unexpected start of array
        if (c != '[') {
            throw JsonException("A JSON array must start with '['")
        }

        c = next()

        // Unexpected end of array
        if (c == END) {
            throw JsonException("Expected a ',' or ']'")
        }

        // Array is not empty
        if (c != ']') {
            back()

            loop@ while (true) {
                if (next() == ',') {
                    back()

                    result.add(null)
                } else {
                    back()

                    result.add(nextValue())
                }

                when (next()) {
                    END -> throw JsonException(
                        "Expected a ',' or ']'"
                    )
                    ',' -> {
                        c = next()

                        if (c == END) {
                            throw JsonException("Expected a ',' or ']'")
                        }

                        if (c == ']') {
                            break@loop
                        }

                        back()
                    }
                    ']' -> break@loop

                    else -> throw JsonException("Expected a ',' or ']'")
                }
            }
        }

        return result
    }

    /**
     * Returns the next value
     */
    fun nextValue(): Any? {
        var c = next()

        return when (c) {
            '"' -> {       //TODO Add single quotes
                nextString()
            }
            '{' -> {
                back()

                nextObject()
            }
            '[' -> {
                back()

                nextArray()
            }

            else -> {
                val value = StringBuilder().apply {
                    while (c >= SPACE && c.notInside(",:]}/\\\"[{;=#")) {
                        append(c)

                        c = next(false)
                    }

                    if (!eof) {
                        back()
                    }
                }.toString().trim()

                if (value.isEmpty()) {
                    throw JsonException("Missing value")
                }

                parseValue(value)
            }
        }
    }

    /**
     * Returns the next value as an escaped character
     *
     * Supported escaped characters
     * + t      (tab)
     * + b      (backspace)
     * + n      (line feed)
     * + r      (carriage return)
     * + "      (double quote)
     * + '      (single quote)
     * + \      (backslash)
     * + /      (slash)
     * + f      (form feed)
     * + uXXXX  (unicode character
     */
    private fun nextEscaped(): Char {
        return when (val next = next(false)) {
            't' -> '\t'
            'b' -> '\b'
            'n' -> '\n'
            'r' -> '\r'
            'f' -> '\u000C'
            '"', '\'',
            '\\', '/' -> next

            'u' -> {
                try {
                    next(4).toInt(16).toChar()
                } catch (e: Exception) {
                    throw JsonException("Illegal escape", e)
                }
            }

            else -> throw JsonException("Illegal escape")
        }
    }

    /**
     * Returns the next value as a String
     *
     * A string must be surrounded by two double quotes
     */
    private fun nextString(): String {   //TODO Add configuration that parses strings with single quotes
        return StringBuilder().run {
            loop@ while (true) {
                when (val c = next(false)) {
                    END -> throw JsonException(
                        "Missing string closing character"
                    )
                    '\r', '\n' -> throw JsonException("Unexpected line break inside string")

                    '\\' -> append(nextEscaped())

                    '"' -> break@loop

                    else -> append(c)
                }
            }

            toString()
        }
    }

    /**
     * Parses the given string [value], which will result in a Number, Boolean or null value
     */
    private fun parseValue(value: String): Any? {
        return when {
            value.equals("true", true) -> true
            value.equals("false", true) -> false
            value.equals("null", true) -> null

            value[0] in '0'..'9' || value[0] == '-' -> {
                if (isDecimalNotation(value)) {
                    value.toDouble()
                } else {
                    value.toLong()
                }
            }

            else -> value
        }
    }

    /**
     * Returns whether or not the given [value] is a decimal number or not
     */
    private fun isDecimalNotation(value: String): Boolean {
        return value.indexOf('.') > -1
                || value.indexOf('e') > -1
                || value.indexOf('E') > -1
                || value == "-0"
    }

    /**
     * Returns if there's more characters to read or not
     */
    private fun hasNext(): Boolean {
        return !eof || usePrevious
    }

    /**
     * Returns the next character in the [reader]
     *
     * If [clean] is set to false, this method will return any character from the [reader],
     * otherwise it will return a clean character
     *
     * By default it will return a clean character
     */
    private fun next(clean: Boolean = true): Char {
        val next: Char

        if (clean) {
            while (true) {
                val c = next(false)

                if (c == END || c > SPACE) {
                    next = c

                    break
                }
            }
        } else {
            if (usePrevious) {
                next = previous

                usePrevious = false
            } else {
                next = reader.read().toChar()
            }

            if (next == END) {
                eof = true
            }
        }

        previous = next

        return next
    }

    /**
     * Returns the next [n] characters
     */
    private fun next(n: Int): String {
        return CharArray(n) {
            if (hasNext()) {
                next(true)
            } else {
                throw JsonException("Substring bounds error")
            }
        }.toString()
    }

    /**
     * Backs up one character. This provides a sort of lookahead capability,
     * so that you can test for a character before attempting to
     * parse the next character or identifier
     */
    private fun back() {
        usePrevious = true
        eof = false
    }

    companion object {
        private const val SPACE = 32.toChar()
        private const val END = 0.toChar()
    }
}