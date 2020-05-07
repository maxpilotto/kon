package com.maxpilotto.kon

import com.maxpilotto.kon.extensions.Calendar
import com.maxpilotto.kon.protocols.Json
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
        Int::class -> when (value) {
            is Int -> value

            else -> cast<Number>(value).toInt()
        }
        Long::class -> when (value) {
            is Long -> value

            else -> cast<Number>(value).toLong()
        }
        Double::class -> when (value) {
            is Double -> value

            else -> cast<Number>(value).toDouble()
        }
        Float::class -> when (value) {
            is Float -> value

            else -> cast<Number>(value).toFloat()
        }
        Byte::class -> when (value) {
            is Byte -> value

            else -> cast<Number>(value).toByte()
        }
        Short::class -> when (value) {
            is Short -> value

            else -> cast<Number>(value).toShort()
        }
        Char::class -> {
            when (value) {
                is String -> value.single()

                else -> cast<Number>(value).toChar()
            }
        }
        Boolean::class -> when (value) {
            is Boolean -> value
            is Number -> value.toInt() != 0
            is String -> value.equals("true", true)

            else -> throw JsonException("Value cannot be cast/parsed as Boolean")
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

        Date::class -> castDate(value)
        Calendar::class -> Calendar(cast<Date>(value))

        IntRange::class -> when (value) {
            is IntRange -> value    //TODO Remove the base type since it's not used
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

        else -> value
    } as T
}

/**
 * Casts or parses the given [value] into a Date or Calendar
 *
 * @param dateFormat Format used to parse a [Date]/[Calendar] instance if the value is a String
 */
inline fun <reified T : Any> castDate(
    value: Any?,
    dateFormat: DateFormat = SimpleDateFormat(Json.DATE_FORMAT)
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
 * Casts or parses the given [value] into a Date or Calendar
 *
 * @param dateFormat Format used to parse a [Date]/[Calendar] instance if the value is a String
 */
fun <T : Any> castDate(
    value: Any?,
    dateFormat: DateFormat,
    type: KClass<T>
): T {   //TODO Use this one and delete the others
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
        Calendar::class -> Calendar(castDate<Date>(value, dateFormat))  //FIXME What the hell is this ?!?

        else -> throw JsonException("Value cannot be cast/parsed as Date/Calendar")

    } as T
}

/**
 * Casts or parses the given [value] into an Enum of type [T]
 *
 * The value can be either a Number or a String, if the value is a Number the enum constant
 * at index [value] will be returned
 *
 * If the value is a String, the enum constant that matches the string will be returned, the case
 * will be ignored
 */
inline fun <reified T : Enum<T>> castEnum(value: Any?): T {
    val enumClass = T::class.java
    val enumValues = enumClass.enumConstants

    return if (value is Number) {
        val ordinal = cast<Int>(value)

        if (ordinal < enumValues.size) {
            enumValues[ordinal]
        }
        else {
            throw JsonException("Index out of bound for enum ${enumClass.simpleName}: Size: ${enumValues.size}, Index: $ordinal")
        }
    } else {
        for (enum in enumValues) {
            if (enum.name.equals(value.toString(), true)) {
                return enum
            }
        }

        throw JsonException("No enum constant for value $value")
    }
}

/**
 * Returns a Locale instance using the given [tag]
 */
fun localeFor(tag: String): Locale {
    require(tag.matches(Regex("[a-zA-z]{2,}[,_-]?[a-zA-z]*"))) {
        Exception("Locale doesn't match the supported formats")
    }

    return Locale.forLanguageTag(
        tag.replace(
            Regex("[,_]"),
            "-"
        )
    )
}

/**
 * Returns a stringified version of given [value] in a Json supported
 * format
 *
 * This will wrap strings and chars around the double quotes
 */
fun stringify(value: Any?): String {
    return when (value) {
        is IntRange,
        is Enum<*>,
        is URL,
        is String -> "\"$value\""

        else -> value.toString()
    }
}