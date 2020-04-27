package com.maxpilotto.kon.annotations

/**
 * # Property
 *
 * Annotation used to mark a Json property and customize it
 *
 * ### Name
 *
 * The [name] is the identifier of the property in the Json string, by default this is set as the same
 * as the class' field name
 *
 * ### Ignoring properties
 *
 * A field can be ignored by setting the [isIgnored] value to true, this will not encode/decode the field
 */
@Target(AnnotationTarget.FIELD)
@Retention(AnnotationRetention.SOURCE)
@MustBeDocumented
annotation class JsonProperty (
    val name: String = "",
    val isIgnored: Boolean = false
)