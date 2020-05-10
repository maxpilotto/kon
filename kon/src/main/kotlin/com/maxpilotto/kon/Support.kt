package com.maxpilotto.kon

import com.maxpilotto.kon.protocols.Json
import com.maxpilotto.kon.util.JsonException
import java.math.BigDecimal
import java.net.URL
import java.text.DateFormat
import java.text.SimpleDateFormat
import java.util.*
import kotlin.reflect.KClass

/**
 * Parses the given [value] into the specified type [T]
 */
inline fun <reified T : Any> parse(value: Any?): T {
    return parse(value, T::class)
}

/**
 * Parses the given [value] into the specified [type]
 */
fun <T : Any> parse(value: Any?, type: KClass<T>): T {
    //TODO Check for a null type
    // value == null -> T?
    // value == "null" -> T?

    return when (type) {
        String::class -> when (value) {
            is String -> value

            else -> value.toString()
        }
        Int::class -> when (value) {
            is Int -> value

            else -> parse<Number>(value).toInt()
        }
        Long::class -> when (value) {
            is Long -> value

            else -> parse<Number>(value).toLong()
        }
        Double::class -> when (value) {
            is Double -> value

            else -> parse<Number>(value).toDouble()
        }
        Float::class -> when (value) {
            is Float -> value

            else -> parse<Number>(value).toFloat()
        }
        Byte::class -> when (value) {
            is Byte -> value

            else -> parse<Number>(value).toByte()
        }
        Short::class -> when (value) {
            is Short -> value

            else -> parse<Number>(value).toShort()
        }
        Char::class -> {
            when (value) {
                is Char -> value
                is String -> value.single()

                else -> parse<Number>(value).toChar()
            }
        }
        Boolean::class -> when (value) {
            is Boolean -> value
            is Number -> value.toInt() != 0
            is String -> value.equals("true", true)

            else -> throw JsonException("Value cannot be parsed into Boolean")
        }
        Number::class -> when (value) {
            is Number -> value
            is String -> try {
                BigDecimal(value)
            } catch (e: Exception) {
                throw JsonException("Cannot parse value as Number: ${e.message}")
            }

            else -> throw JsonException("Value cannot be parsed into Number")
        }

        Date::class -> parseDate(value)
        Calendar::class -> calendarOf(parse<Date>(value))

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

            else -> throw JsonException("Value cannot be parsed into IntRange")
        }
        BigDecimal::class -> when (value) {
            is BigDecimal -> value
            is Number -> BigDecimal(value.toDouble())
            is String -> BigDecimal(value)

            else -> throw JsonException("Value cannot be parsed into BigDecimal")
        }
        URL::class -> when (value) {
            is URL -> value
            is String -> URL(value)

            else -> throw JsonException("Value cannot be parsed into URL")
        }

        else -> value
    } as T
}

/**
 * Parses the given [value] into the specified a [Date] or [Calendar], using the
 * [dateFormat] to parse the value if needed
 */
inline fun <reified T : Any> parseDate(
    value: Any?,
    dateFormat: DateFormat = SimpleDateFormat(Json.DATE_FORMAT)
): T {
    return parseDate(value, dateFormat, T::class)
}

/**
 * Parses the given [value] into the specified a [Date] or [Calendar], using the
 * [format] and [locale] to parse the value if needed
 */
inline fun <reified T : Any> parseDate(
    value: Any?,
    format: String,
    locale: Locale = Locale.getDefault()
): T {
    return parseDate(value, SimpleDateFormat(format, locale), T::class)
}

/**
 * Parses the given [value] into the specified a [Date] or [Calendar], using the
 * [dateFormat] to parse the value if needed
 */
fun <T : Any> parseDate(
    value: Any?,
    dateFormat: DateFormat,
    type: KClass<T>
): T {
    return when (type) {
        Date::class -> when (value) {
            is Date -> value
            is Calendar -> value.time
            is Number -> Date(parse<Long>(value))
            is String -> try {
                dateFormat.parse(value)
            } catch (e: Exception) {
                throw JsonException(e.message)
            }

            else -> throw JsonException("Value cannot be parsed as Date/Calendar")
        }
        Calendar::class -> when (value) {
            is Calendar -> value

            else -> calendarOf(parseDate<Date>(value, dateFormat))
        }

        else -> throw JsonException("Value cannot be parsed as Date/Calendar")

    } as T
}

/**
 * Parses the given [value] into an Enum of type [T]
 *
 * The value can be either a Number or a String, if the value is a Number the enum constant
 * at index [value] will be returned
 *
 * If the value is a String, the enum constant that matches the string will be returned, the case
 * will be ignored
 */
inline fun <reified T : Enum<T>> parseEnum(value: Any?): T {
    val enumClass = T::class.java
    val enumValues = enumClass.enumConstants

    return if (value is Number) {
        val ordinal = parse<Int>(value)

        if (ordinal < enumValues.size) {
            enumValues[ordinal]
        } else {
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

/**
 * Creates an instance of [Calendar] with the given [time],
 * which must be expressed in milliseconds
 */
internal fun calendarOf(time: Long): Calendar {
    return Calendar.getInstance().apply {
        timeInMillis = time
    }
}

/**
 * Creates an instance of [Calendar] with the given [date]
 */
internal fun calendarOf(date: Date): Calendar {
    return Calendar.getInstance().apply {
        time = date
    }
}