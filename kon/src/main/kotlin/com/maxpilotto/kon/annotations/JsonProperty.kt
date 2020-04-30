package com.maxpilotto.kon.annotations

import com.maxpilotto.kon.JsonObject

/**
 * Annotation used to customize a Json property in a Kotlin/Java class
 *
 * @param name Identifier of the property, this is the name that will be used
 * to read/write a property from and into a [JsonObject]
 *
 * @param isIgnored Whether or not this property should be read/written in a [JsonObject]
 * when encoding/decoding an object
 */
@Target(AnnotationTarget.FIELD)
@Retention(AnnotationRetention.BINARY)
@MustBeDocumented
annotation class JsonProperty(
    val name: String = "",
    val isIgnored: Boolean = false
)