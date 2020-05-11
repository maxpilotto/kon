package com.maxpilotto.kon.samples

import com.maxpilotto.kon.annotations.JsonDate
import com.maxpilotto.kon.annotations.JsonEncodable
import com.maxpilotto.kon.annotations.JsonProperty
import java.net.URL
import java.util.*

enum class Fruit {
    None,
    Apple,
    Banana
}

@JsonEncodable
data class SubObject(
    val id: Int = 0,
    val name: String = "John"
)

@JsonEncodable
data class Object(
    @JsonProperty("test_field")
    val string: String = "Test",
    val number: Number = 10,
    val dd: Double = 20.2,
    val bool: Boolean = true,
    val c: Char = 'c',
    @JsonDate("yyyy-MM-dd")
    val date: Date = Date(),
    val range: IntRange = IntRange(10, 50),
    val fruit: Fruit = Fruit.Banana,
    val url: URL = URL("https://www.google.it"),
    val obj: SubObject = SubObject(),
    val strings: List<String> = listOf("1", "2"),
    val ints: List<Int> = listOf(1, 2, 3, 4, 5),
    @JsonDate("yyyy-MM-dd")
    val dates: List<Calendar> = listOf(
        Calendar.getInstance(),
        Calendar.getInstance(),
        Calendar.getInstance()
    ),
    val enums: List<Fruit> = listOf(
        Fruit.Apple,
        Fruit.Banana
    ),
    val objects: List<SubObject> = listOf(
        SubObject(),
        SubObject(),
        SubObject()
    ),
    val ranges: List<IntRange> = listOf(
        IntRange(1, 10),
        IntRange(1, 10),
        IntRange(1, 10)
    )
)

fun main() {
    val obj = Object().also {
        println(it)
    }
    val objToJson = ObjectEncoder(obj).also { println(it) }
    val jsonToObj = ObjectDecoder(objToJson).also { println(it) }
}