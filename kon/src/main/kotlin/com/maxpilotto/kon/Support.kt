package com.maxpilotto.kon

import com.maxpilotto.kon.extensions.Calendar
import com.maxpilotto.kon.util.JsonException
import java.math.BigDecimal
import java.net.URL
import java.text.DateFormat
import java.text.SimpleDateFormat
import java.util.*
import kotlin.reflect.KClass

/**
 * Casts or parses the given [value] into the specified type
 */
inline fun <reified T : Any> cast(value: Any?): T {
    return cast(value, T::class)
}

/**
 * Casts or parses the given [value] into a Date or Calendar
 *
 * @param dateFormat Format used to parse a [Date]/[Calendar] instance if the value is a String
 */
inline fun <reified T : Any> castDate(
    value: Any?,
    dateFormat: DateFormat = DateFormat.getDateInstance()
): T {
    return castDate(value, dateFormat, T::class)
}

/**
 * Casts or parses the given [value] into a Date or Calendar
 *
 * @param format Format used to parse a [Date]/[Calendar] instance if the value is a String
 * @param locale Locale used to created the date format
 */
inline fun <reified T : Any> castDate(
    value: Any?,
    format: String,
    locale: Locale = Locale.getDefault()
): T {
    return castDate(value, SimpleDateFormat(format, locale))
}

/**
 * Casts or parses the given [value] into the specified [type]
 *
 * This will not take care of Enums and Date/Calendar parsed with the date formats
 */
fun <T : Any> cast(value: Any?, type: KClass<T>): T {
    return when (type) {
        String::class -> when (value) {
            is String -> value

            else -> value.toString()
        }

        Number::class -> when (value) {
            is Number -> value
            is String -> try {
                BigDecimal(value)
            } catch (e: Exception) {
                throw JsonException("Cannot parse value as Number: ${e.message}")
            }

            else -> throw JsonException("Value cannot be cast/parsed as Number")
        }

        Int::class -> cast<Number>(value).toInt()
        Long::class -> cast<Number>(value).toLong()
        Double::class -> cast<Number>(value).toDouble()
        Float::class -> cast<Number>(value).toFloat()
        Byte::class -> cast<Number>(value).toByte()
        Short::class -> cast<Number>(value).toShort()
        Char::class -> cast<Number>(value).toChar()
        Boolean::class -> when (value) {
            is Boolean -> value
            is Number -> value.toInt() != 0
            is String -> value.equals("true", true)

            else -> throw JsonException("Value cannot be cast/parsed as Boolean")
        }

        Date::class -> castDate(value)
        Calendar::class -> Calendar(cast<Date>(value))

        IntRange::class -> when (value) {
            is IntRange -> value
            is Number -> IntRange(0, value.toInt())
            is String -> if (value.matches(Regex("""[0-9]+\.\.[0-9]+"""))) {
                with(value.split("..")) {
                    IntRange(
                        get(0).toInt(),
                        get(1).toInt()
                    )
                }
            } else {
                IntRange(0, value.toInt())
            }

            else -> throw JsonException("Value cannot be cast/parsed as IntRange")
        }
        BigDecimal::class -> when (value) {
            is BigDecimal -> value
            is Number -> BigDecimal(value.toDouble())
            is String -> BigDecimal(value)

            else -> throw JsonException("Value cannot be cast/parsed as BigDecimal")
        }
        URL::class -> when (value) {
            is URL -> value
            is String -> URL(value)

            else -> throw JsonException("Value cannot be cast/parsed as URL")
        }

        // This will also take care of the following types
        // + JsonObject
        // + JsonArray
        else -> value
    } as T
}

/**
 * Casts or parses the given [value] into a Date or Calendar
 *
 * @param dateFormat Format used to parse a [Date]/[Calendar] instance if the value is a String
 */
fun <T : Any> castDate(value: Any?, dateFormat: DateFormat, type: KClass<T>): T {
    return when (type) {
        Date::class -> when (value) {
            is Date -> value
            is Calendar -> value.time
            is Number -> Date(cast<Long>(value))
            is String -> try {
                dateFormat.parse(value)
            } catch (e: Exception) {
                throw JsonException(e.message)
            }

            else -> throw JsonException("Value cannot be cast/parsed as Date/Calendar")
        }

        Calendar::class -> castDate(value, dateFormat)

        else -> throw JsonException("Value cannot be cast/parsed as Date/Calendar")

    } as T
}