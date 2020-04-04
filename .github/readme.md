# Kon
KON (improperly Kotlin Object Notation) is a JSON parsing library written in Kotlin that uses Kotlin's features

## Index
+ Parsing
    + [Basic parsing](#basic-parsing)
    + [Inline object parsing](#inline-object-parsing)
    + [Automatic object parsing](#automatic-object-parsing)
+ Operators and JsonValue
    + [get/set operators](#get/set-operators)
+ Networking
    + [Basic fetch](#basic-fetch)

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

## Quick overview

#### Basic parsing

```kotlin
val string = """
    {
        "firstName": "John",
        "lastName": "Doe",
        "dob": "1987/09/25"
    }
"""
val json = JsonObject(string)

println(json.getString("firstName"))
println(json.getDate("dob", "yyyy/MM/dd"))
```

#### Inline object parsing
```kotlin
data class Address(
    val street: String,
    val number: Int,
    val country: String
)

val json = JsonObject("""
    {
        "firstName": "John",
        "lastName": "Doe",
        "address": {
            "street": "Downing Street",
            "number": 10,
            "country": "England"
        }
    }
""")
val address = json["address"].asObject { 
    Address(
        it.getString("street"),
        it.getInt("number"),
        it.getString("country")
    )
}
```

#### Automatic object parsing
//TODO

#### Get/Set operators

```kotlin
val json = JsonObject("""
    {
        "firstName": "John",
        "lastName": "Doe",
        "addresses": [
            {
                "street": "Downing Street",
                "number": 10,
                "country": "England"
            }
        ]
    }
""")

println(json["firstName"])
println(json["addresses"][0]["street"])
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
