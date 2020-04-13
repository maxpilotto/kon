package com.maxpilotto.kon.annotations

/**
 * # Codable annotation
 *
 * Annotation used to mark classes that can be parsed into and from a JsonObject
 *
 * ## Encode
 * An extension method for the marked class with name `encode` will be generated
 *
 * This method will return a JsonObject that represents the calling object
 *
 * The optional lambda parameter can be used to
 * parse some objects that are not marked with this annotation,
 * by default the lambda returns null
 *
 * If an object is not marked with this annotation, its [toString] method
 * will be called
 *
 * ## Decode
 * An extension method for the marked class' Companion object with the name `decode` will be generated
 *
 * This method can both work with a JsonParser, JsonArray, JsonObject and String
 *
 * ## Java
 *
 * Since Java doesn't have extension methods, this will result in the
 * creation of a class named (ClassName)Kt and this class will have a
 * static method that takes your object instance
 */
@Target(AnnotationTarget.CLASS)
@Retention(AnnotationRetention.SOURCE)
annotation class Codable

//TODO Add option for a complete different parsing behavior
//The default behavior should be Class.Companion.fromJson() and Class.toJson()
//In java that probably doesn't work