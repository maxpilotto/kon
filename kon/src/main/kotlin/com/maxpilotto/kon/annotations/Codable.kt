package com.maxpilotto.kon.annotations

@Target(AnnotationTarget.CLASS)
@Retention(AnnotationRetention.SOURCE)
annotation class Codable

//TODO Add option for a complete different parsing behavior
//The default behavior should be Class.Companion.fromJson() and Class.toJson()
//In java that probably doesn't work