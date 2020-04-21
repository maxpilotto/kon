package com.maxpilotto.kon.processor

import com.google.auto.service.AutoService
import com.maxpilotto.kon.JsonArray
import com.maxpilotto.kon.JsonObject
import com.maxpilotto.kon.annotations.Codable
import com.maxpilotto.kon.extensions.plusAssign
import com.maxpilotto.kon.processor.extensions.simpleName
import com.squareup.kotlinpoet.*
import java.math.BigDecimal
import java.net.URL
import java.util.*
import javax.annotation.Nullable
import javax.annotation.processing.Processor
import javax.lang.model.AnnotatedConstruct
import javax.lang.model.element.Element
import javax.lang.model.element.Modifier
import javax.lang.model.type.TypeKind
import javax.lang.model.type.TypeMirror
import kotlin.reflect.KClass

/**
 * Processor which task is to handle all classes marked with the [Codable] annotation
 */
@AutoService(Processor::class)
class CodableProcessor : KonProcessor() {
    private val transformLambda = LambdaTypeName.get(
        null,
        listOf(
            ParameterSpec
                .builder("", OPTIONAL_ANY)
                .build()
        ),
        OPTIONAL_ANY
    )
    private val transformBlock = ParameterSpec
        .builder("transform", transformLambda)
        .defaultValue("{ null }")
        .build()

    override fun getSupportedAnnotationTypes() = mutableSetOf(
        Codable::class.java.canonicalName
    )

    override fun process(kClass: KClass<*>, elements: Set<Element>): Boolean {
        when (kClass) {
            Codable::class -> {
                for (element in elements) {
                    val packageName = elementUtils.getPackageOf(element).toString()
                    val fileName = element.simpleName.toString()
                    val privateConstructor = FunSpec.constructorBuilder()
                        .addModifiers(KModifier.PRIVATE)
                        .build()
                    val encoderCompanion = TypeSpec.companionObjectBuilder().apply {
                        encodeClass(element).forEach {
                            addFunction(it)
                        }
                    }.build()
                    val decoderCompanion = TypeSpec.companionObjectBuilder().apply {
                        decodeString(element).forEach {
                            addFunction(it)
                        }
                        decodeJson(element).forEach {
                            addFunction(it)
                        }
                    }.build()
                    val file = FileSpec.builder(packageName, fileName)
                        .addImport("com.maxpilotto.kon.extensions", "toJsonValue")
                        .addImport("com.maxpilotto.kon.util", "JsonException")
                        .addType(
                            TypeSpec.classBuilder("${fileName}Encoder")
                                .primaryConstructor(privateConstructor)
                                .addType(encoderCompanion)
                                .build()
                        )
                        .addType(
                            TypeSpec.classBuilder("${fileName}Decoder")
                                .primaryConstructor(privateConstructor)
                                .addType(decoderCompanion)
                                .build()
                        )
                        .indent("\t")
                        .build()

                    file.writeTo(generatedDir)
                }

                return true
            }
        }

        return false
    }

    private fun encodeClass(element: Element): List<FunSpec> {
        val doc = """
            Returns the calling object as a [JsonObject]
            
            If any of the properties is not a primitive, String, Collection or Map and is not
            marked as Codable, the toString() method will be called on that property
            to retrieve the data
            
            The [transform] block can be used to encode objects that are not marked as Codable,
            this block should return the value as it should be saved, generally a 
            JsonObject, a String or a Number
        """.trimIndent()
        val invoke = FunSpec.builder("invoke")
            .addKdoc(doc)
            .addModifiers(KModifier.OPERATOR)
            .addAnnotation(JvmOverloads::class)
            .addAnnotation(JvmStatic::class)
            .addParameter("data", element.asType().asTypeName())
            .addParameter(transformBlock)
            .addStatement("return encode(data,transform)")
        val method = FunSpec.builder("encode")
            .addKdoc(doc)
            .addAnnotation(JvmOverloads::class)
            .addAnnotation(JvmStatic::class)
            .addParameter("data", element.asType().asTypeName())
            .addParameter(transformBlock)
            .addStatement("val json = %T()", JsonObject::class)
            .returns(JsonObject::class)
        val isKotlin = isKotlinClass(element)

        getProperties(element) { prop, name, type, _ ->
            val isPublic = prop.modifiers.contains(Modifier.PUBLIC)
            val getter = if (isKotlin || isPublic) {
                "data.$name"
            } else {
                "data.get${name.capitalize()}()"
            }

            when {
                // The property is a primitive or String, they can just be added to the object
                // with no additional action
                isPrimitive(type) ||
                        isString(type) ||
                        isSubclass(type, Number::class) ||
                        isSubclass(type, BigDecimal::class) -> {
                    method.addStatement("""json.set("$name",$getter)""")  //TODO Add the transform for all types
                }

                // The property is a collection or array, this must be resolved recursively
                // due to the fact that it could be a list of lists or an array of arrays
                isArray(type) || isCollection(type) -> {
                    val content = encodeCollection(type, getter)

                    method.addStatement(
                        """json.set("$name",%T($content))""",
                        JsonArray::class
                    )
                }

                // The property is a map, this can be added but must be converted into
                // a JsonObject first, this can be done using one of the JsonObject's constructors
                isMap(type) -> {
                    method.addStatement(
                        """json.set("$name",%T($getter.toMutableMap() as MutableMap<String, Any?>))""",
                        JsonObject::class
                    )
                }

                // The property is not a primitive, collection, array or map
                //
                // If the property's class is marked as Codable, the encode() method will be called,
                // otherwise the transform block will be called and if that block returns a null object
                // the toString() method will be used
                //
                // This is also used to parse IntRange, Enum, URL, Date and Calendar //TODO Date and Calendar need their formats, enums too
                else -> {
                    val transform = if (hasAnnotation(prop, Codable::class)) {
                        """${getEncoder(prop)}.encode($getter)"""
                    } else {
                        """transform($getter) ?: $getter.toString()"""
                    }

                    method.addStatement(
                        """json.set("$name",$transform)"""
                    )
                }
            }
        }

        method.addStatement("return json")

        return listOf(
            method.build(),
            invoke.build()
        )
    }

    private fun encodeCollection(
        type: TypeMirror,
        propName: String = "it"
    ): String {
        val component = getComponentType(type)
        val builder = StringBuilder(
            """$propName.joinToString(",","[","]",transform = {"""
        )

        component?.let {
            when {
                isPrimitive(it) || isString(it) || isEnum(it) -> {
                    builder += "it.toString()"
                }

                isArray(it) || isCollection(it) -> {
                    builder.append(encodeCollection(it))
                }

                isMap(it) -> {
                    builder.append("JsonObject(it.toMutableMap() as MutableMap<String, Any?>).toString()")
                }

                else -> {
                    if (hasAnnotation(it, Codable::class)) {
                        builder.append("""${getEncoder(it)}.encode(it).toString()""")
                    } else {
                        builder.append("""(transform(it) ?: it).toString()""")
                    }
                }
            }
        }

        builder += "})"

        return builder.toString()
    }

    private fun decodeString(element: Element): List<FunSpec> {
        val doc = """
            Creates an instance of ${element.asType().simpleName} from the given [string]
        """.trimIndent()
        val invoke = FunSpec.builder("invoke")
            .addKdoc(doc)
            .addModifiers(KModifier.OPERATOR)
            .addAnnotation(JvmOverloads::class)
            .addAnnotation(JvmStatic::class)
            .addParameter("string", String::class)
            .addParameter(transformBlock)
            .addStatement("return decode(string,transform)")
            .returns(element.asType().asTypeName())
        val method = FunSpec.builder("decode")
            .addKdoc(doc)
            .addAnnotation(JvmOverloads::class)
            .addAnnotation(JvmStatic::class)
            .addParameter("string", String::class)
            .addParameter(transformBlock)
            .addStatement(
                "return decode(%T(string),transform)",
                JsonObject::class
            )
            .returns(element.asType().asTypeName())

        return listOf(
            method.build(),
            invoke.build()
        )
    }

    private fun decodeJson(element: Element): List<FunSpec> {
        val doc = """
            Creates an instance of ${element.asType().simpleName} from the given [json]
        """.trimIndent()
        val invoke = FunSpec.builder("invoke")
            .addKdoc(doc)
            .addModifiers(KModifier.OPERATOR)
            .addAnnotation(JvmOverloads::class)
            .addAnnotation(JvmStatic::class)
            .addParameter("json", JsonObject::class)
            .addParameter(transformBlock)
            .addStatement("return decode(json,transform)")
            .returns(element.asType().asTypeName())
        val method = FunSpec.builder("decode")
            .addKdoc(doc)
            .addAnnotation(JvmOverloads::class)
            .addAnnotation(JvmStatic::class)
            .addParameter("json", JsonObject::class)
            .addParameter(transformBlock)
            .returns(element.asType().asTypeName())
        val parameters = CodeBlock.builder()

        getProperties(element) { prop, name, type, isLast ->
            when {
                // String
                isString(type) -> parameters.add("""json.getString("$name")""")

                // Numbers and primitives
                isInt(type) -> parameters.add("""json.getInt("$name")""")
                isLong(type) -> parameters.add("""json.getLong("$name")""")
                isBoolean(type) -> parameters.add("""json.get Boolean("$name")""")
                isDouble(type) -> parameters.add("""json.getDouble("$name")""")
                isFloat(type) -> parameters.add("""json.getFloat("$name")""")
                isByte(type) -> parameters.add("""json.getByte("$name")""")
                isShort(type) -> parameters.add("""json.getShort("$name")""")
                isChar(type) -> parameters.add("""json.getChar("$name")""")
                isSubclass(type, BigDecimal::class) -> parameters.add("""json.getBigDecimal("$name")""")
                isSubclass(type, Number::class) -> parameters.add("""json.getNumber("$name")""")

                // Kon objects  //TODO Add these two to the encoder too
                isSubclass(type, JsonObject::class) -> parameters.add("""json.getJsonObject("$name")""")
                isSubclass(type, JsonArray::class) -> parameters.add("""json.getJsonArray("$name")""")

                // Date & Calendar      //FIXME This will only work with timestamps
                isSubclass(type, Date::class) -> parameters.add("""json.getDate("$name")""")
                isSubclass(type, Calendar::class) -> parameters.add("""json.getCalendar("$name")""")

                // IntRange, URL, Enum
                isSubclass(type, IntRange::class) -> parameters.add("""json.getRange("$name")""")
                isSubclass(type, URL::class) -> parameters.add("""json.getURL("$name")""")
                isEnum(type) -> parameters.add("""json.getEnum("$name")""")

                // Collection or Array
                isCollection(type) || isArray(type) -> {
                    parameters.add("""json.getList("$name") { ${decodeCollection(type)} }""")
//                        parameters.add("""json.getList("$name") {""")
//                        parameters.add(decodeCollection(type))
//                        parameters.add("}")
                }

                // Map
                isMap(type) -> {
                    parameters.add("""json.getJsonObject("$name").toTypedMap()""")
                }

                // Other
                else -> {
                    if (hasAnnotation(prop, Codable::class)) {
                        parameters.add("""${getDecoder(prop)}.decode(json.getJsonObject("$name"))""")
                    } else {
                        if (hasAnnotation(prop, Nullable::class)) {
                            parameters.add("""transform(json.getJsonObject("$name")) as ${type.simpleName}? ?: null""")
                        } else {
                            parameters.add(
                                """transform(json.getJsonObject("$name")) as ${type.simpleName}? ?: throw JsonException()"""
//                                ,
//                                """Class ${type.simpleName} needs to be marked as Codable or parsed using the transform block"""  //FIXME This doesn't get formatted correctly
                            )
                        }
                    }
                }
            }

            if (!isLast) {
                parameters.add(", ")
            }
        }

        method.addCode(
            "return %T(",
            element.asType()
        )
        method.addCode(parameters.build())
        method.addCode(")")

        return listOf(
            method.build(),
            invoke.build()
        )
    }

    private fun decodeCollection(type: TypeMirror): CodeBlock {
        val code = CodeBlock.builder()

        getComponentType(type)?.let {
            when {
                // String
                isString(it) -> code.add("it.toJsonValue().asString()")

                //TODO Create a converter that takes an Any? and returns the desired type,
                // and use this here and inside the JsonValue to avoid the creation of a new JsonValue Instance every time

                // Numbers and primitives
                isInt(it) -> code.add("t.toJsonValue().asInt()")
                isLong(it) -> code.add("it.toJsonValue().asLong()")
                isBoolean(it) -> code.add("it.toJsonValue().asBoolean()")
                isDouble(it) -> code.add("it.toJsonValue().asDouble()")
                isFloat(it) -> code.add("it.toJsonValue().asFloat()")
                isByte(it) -> code.add("it.toJsonValue().asByte()")
                isShort(it) -> code.add("it.toJsonValue().asShort()")
                isChar(it) -> code.add("it.toJsonValue().asChar()")
                isSubclass(it, BigDecimal::class) -> code.add("it.toJsonValue().asBigDecimal()")
                isSubclass(it, Number::class) -> code.add("it.toJsonValue().asNumber()")

                // Kon objects
                isSubclass(it, JsonObject::class) -> code.add("it.toJsonValue().asJsonObject()")
                isSubclass(it, JsonArray::class) -> code.add("it.toJsonValue().asJsonArray()")

                // Date & Calendar      // FIXME This will only work with timestamps
                isSubclass(it, Date::class) -> code.add("it.toJsonValue().asDate()")
                isSubclass(it, Calendar::class) -> code.add("it.toJsonValue().asCalendar()")

                // IntRange, URL, Enum
                isSubclass(it, IntRange::class) -> code.add("it.toJsonValue().asRange()")
                isSubclass(it, URL::class) -> code.add("it.toJsonValue().asURL()")
                isEnum(it) -> code.add("it.toJsonValue().asEnum()")

                // Collection or Array
                isCollection(it) || isArray(it) -> {
                    code.add("(it as JsonArray).toList{")
                    code.add(decodeCollection(it))
                    code.add("}")
                }

                // Map
                isMap(it) -> {
                    code.add("(it as JsonObject).toTypedMap()")
                }

                // Other
                else -> {
                    if (hasAnnotation(it, Codable::class)) {
                        code.add("${getDecoder(it)}.decode(it as JsonObject)")
                    } else {
                        if (hasAnnotation(it, Nullable::class)) {
                            code.add("transform(it as JsonObject) as ${it.simpleName}? ?: null")
                        } else {
                            code.add(
                                """transform(it as JsonObject) as ${it.simpleName}? ?: throw JsonException()"""
//                                ,
//                                "Class ${it.simpleName} needs to be marked as Codable or parsed using the transform block"    //FIXME This breaks
                            )
                        }
                    }
                }
            }
        }

        return code.build()
    }

    private fun getEncoder(element: AnnotatedConstruct): String {
        return when (element) {
            is TypeMirror -> "${element.simpleName}Encoder"
            is Element -> "${element.asType().simpleName}Encoder"

            else -> throw Exception("The given type must be a TypeMirror or an Element")
        }
    }

    private fun getDecoder(element: AnnotatedConstruct): String {
        return when (element) {
            is TypeMirror -> "${element.simpleName}Decoder"
            is Element -> "${element.asType().simpleName}Decoder"

            else -> throw Exception("The given type must be a TypeMirror or an Element")
        }
    }
}