package com.maxpilotto.kon.annotations

/**
 * # [JsonEncodable]
 *
 * Annotation used to mark classes that should be automatically parsed into or from a JsonObject
 *
 * ## Encoding
 *
 * A class named (ClassName)Encoder will be generated and it will two methods
 *
 * + encode(Class)
 * + invoke(Class)
 *
 * Both methods will take the class instance as parameter and return a JsonObject, the second method can only
 * be used when using Kotlin and it's up to the developer to choose which one to use
 *
 * ```kotlin
 * Examples
 *
 * val json = BookEncoder.encode(book)
 * val json = BookEncoder(book)     //Note: This is not a constructor, it's a static invoke operator
 * ```
 *
 * ## Decoding
 *
 * A class name (ClassName)Decoder will be generated and it will have the following methods
 *
 * + decode(String)
 * + decode(JsonObject)
 * + invoke(String)
 * + invoke(JsonObject)
 *
 * All of them will return an instance of the Class marked with [JsonEncodable], the invoke operators can only be used
 * in Kotlin and they will work the same way as the named ones
 *
 * ```kotlin
 * Examples
 *
 * val book = BookDecoder.decode("{"title": "1984"}")
 * val book = BookDecoder("{"title": "1984"}")     //Note: This is not a constructor, it's a static invoke operator
 * ```
 *
 * ## Requirements
 *
 * The class marked with this annotation must provide a constructor that has all of the properties that
 * need to be encoded, properties in the constructor must also have the same order as they are defined in the
 * class body
 */
@Target(AnnotationTarget.CLASS)
@Retention(AnnotationRetention.SOURCE)
@MustBeDocumented
annotation class JsonEncodable