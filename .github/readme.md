# Kon
KON (improperly Kotlin Object Notation) is a JSON parsing library written in Kotlin that uses Kotlin's features

## Index

+ [Getting started](#getting-started)
+ [Supported types](#supported-types)
+ Usage
    + Encoding
        + [Basic](#basic-encoding)
        + [Inline](#inline-encoding)
        + [Automatic](#automatic-encoding)
    + Decoding    
        + [Basic](#basic-decoding)
        + [Inline](#basic-decoding)
        + [Automatic](#basic-decoding)
    + Operators and JsonValue
        + [get/set operators](#get/set-operators)
    + Networking
        + [Basic fetch](#basic-fetch)
        + [Basic service](#basic-service)
        + [Full service](#full-service)
    + Annotations
        + [Ignore](#ignore)
        + [Property](#property)
        + [Optional](#optional)

## Getting started
In your project's build.gradle

```gradle
repositories {
	maven { url "https://jitpack.io" }
}
```

In your modules's build.gradle

```gradle
dependencies {
    implementation 'com.github.maxpilotto:kon:$version'
}
```

## Supported types

The following is a table showing the supported types, how they're stored inside a JSON String and how they're stored inside a `JsonObject`

| Type | Json | JsonObject | Json storing examples |
| --- | --- | --- | --- |
| String | String, Any | String, Any | "hello", 1, 0.0 |
| Number | String, Number | String, Number | 1, 2, 0.0, "3" |
| Boolean | String, Number | String, Number, Boolean | "true", "false" |
| Calendar/Date | String, Number (timestamp) | String, Number (timestamp), Date, Calendar | "1587497556000", 1587497556000, "2020-04-20" |
| IntRange | String, Number | String, Number, IntRange | "0..20", "20", 20 |
| BigDecimal | String, Number | String, Number, BigDecimal | "0.000003", 0.000003 |
| URL | String | String, URL | "https://www.google.com" |
| Enum | String, Number | String, Number, Enum | 1, "0", "Value1", "VALUE_1" | 

## Usage

#### Basic encoding
//TODO

#### Inline encoding
//TODO

#### Automatic encoding
The `Codable` annotation will automatically generated an extension for the marked class, which can be used to encode the object
```kotlin
@Codable
data class Author(
    val firstName: String,
    val lastName: String,
    val year: Int
)

val author = Author("George", "Orwell", 1903)
val json = author.encode()
```

#### Basic decoding

```kotlin
val json = JsonObject(
    """
    {
        "firstName": "Yui",
        "lastName": "Hirasawa",
        "dob": "1991/11/27"
    }
    """
)

println(json.getString("firstName"))
println(json.getDate("dob", "yyyy/MM/dd"))
```

#### Inline decoding
```kotlin
data class Address(
    val country: String,
    val city: String
)

val json = JsonObject(
    """
    {
        "firstName": "Mio",
        "lastName": "Akiyama",
        "address": {
            "country": "Japan",
            "city": "Kyoto"
        }
    }
    """
)
val address = json["address"].asObject {
    Address(
        it.getString("country"),
        it.getString("city")
    )
}
```

#### Automatic decoding
//TODO

#### Get/Set operators
```kotlin
val json = JsonObject(
    """
    {
        "firstName": "Azusa",
        "lastName": "Nakano",
        "addresses": [
            {
                "country": "Japan",
                "city": "Kyoto"
            }
        ]
    }
    """
)

println(json["firstName"])
println(json["addresses"][0]["city"])
```

Comparison with and without the Operators + JsonValue
```kotlin
val json = JsonObject(
    """
    {
        "people": [
            {
                "data": {
                    "dob": 1586723311
                }
            }
        ]
    }
    """.trimIndent()
)
val with = json["people"][0]["data"]["dob"].asDate()
val without = (json.getJsonArray("people").getValue(0) as JsonObject).getJsonObject("data").getDate("dob")
```

#### Basic fetch
```kotlin
// Sync call
val todos = JsonService.fetchArray("https://apiservice.com/todos")

// Async call
JsonService.fetchArray("https://apiservice.com/todos") {
    println(it)
}
```

#### Fetch and parse
//TODO
