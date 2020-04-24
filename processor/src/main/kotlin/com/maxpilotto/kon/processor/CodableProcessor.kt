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
                    val packageName = processingEnv.elementUtils.getPackageOf(element).toString()
                    val fileName = element.simpleName.toString()
                    val privateConstructor = FunSpec.constructorBuilder()
                        .addModifiers(KModifier.PRIVATE)
                        .build()
                    val encoderCompanion = TypeSpec.companionObjectBuilder()
                        .addFunctions(encodeClass(element))
                        .build()
                    val decoderCompanion = TypeSpec.companionObjectBuilder()
                        .addFunctions(decodeClass(element))
                        .build()
                    val file = FileSpec.builder(packageName, fileName)
                        .addImport("$BASE_PACKAGE.extensions", "toJsonValue")
                        .addImport("$BASE_PACKAGE.util", "JsonException")
                        .addImport(BASE_PACKAGE, "cast")
                        .addImport(BASE_PACKAGE, "castDate")
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
            
            If any of the properties is not supported by the [JsonObject]'s [JsonObject.set] method and it 
            is not marked as Codable, the toString() method of that instance will be called and that
            property will be saved as a String
            
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
                // The property is one of the type that are supported by the JsonObject and don't need any
                // special action, they can be directly added to the object
                isPrimitive(type) ||
                        isString(type) ||
                        isSubclass(type, Number::class) ||
                        isSubclass(type, BigDecimal::class) ||
                        isSubclass(type, JsonObject::class) ||  //TODO Add IntRange, URL, Enum
                        isSubclass(type, JsonArray::class) -> {
                    method.addStatement("""json.set("$name",$getter)""")  //TODO Add the transform for all types
                }

                // The property is a collection or array, this must be resolved recursively
                // due to the fact that it could be a list of lists or an array of arrays
                isArray(type) || isCollection(type) -> {
                    method.addStatement("""json.set("$name",%T(${encodeCollection(type, getter)}))""", JsonArray::class)
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
                // TODO Date and Calendar need their formats, enums too
                else -> {
                    val transform = if (hasAnnotation(prop, Codable::class)) {
                        """${getEncoder(prop)}.encode($getter)"""
                    } else {
                        """transform($getter) ?: $getter.toString()"""
                    }

                    method.addStatement("""json.set("$name",$transform)""")
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
    ): CodeBlock {
        val component = getComponentType(type)
        val code = CodeBlock.builder()

        component?.let {
            when {
                isPrimitive(it) || isString(it) || isEnum(it) -> {
                    code.add("it.toString()")
                }

                isArray(it) || isCollection(it) -> {
                    code.add(encodeCollection(it))
                }

                isMap(it) -> {
                    code.add("JsonObject(it.toMutableMap() as MutableMap<String, Any?>).toString()")
                }

                else -> {
                    if (hasAnnotation(it, Codable::class)) {
                        code.add("""${getEncoder(it)}.encode(it).toString()""")
                    } else {
                        code.add("""(transform(it) ?: it).toString()""")
                    }
                }
            }
        }

        return CodeBlock.of("""$propName.joinToString(",","[","]",transform = { ${code.build()} })""")
    }

    private fun decodeClass(element: Element): List<FunSpec> {
        val doc = """
            Creates an instance of ${element.asType().simpleName} from the given [json]
            
            If any of the properties is not supported by the [JsonObject]'s [JsonObject.set] method,
            it is not marked as Codable and it is nullable then null will be returned, otherwise
            an exception is thrown
            
            The [transform] block can be used to decode objects that are not marked as Codable
        """.trimIndent()
        val invokeString = FunSpec.builder("invoke")
            .addKdoc(doc)
            .addModifiers(KModifier.OPERATOR)
            .addAnnotation(JvmOverloads::class)
            .addAnnotation(JvmStatic::class)
            .addParameter("json", String::class)
            .addParameter(transformBlock)
            .addStatement("return decode(json,transform)")
            .returns(element.asType().asTypeName())
        val decodeString = FunSpec.builder("decode")
            .addKdoc(doc)
            .addAnnotation(JvmOverloads::class)
            .addAnnotation(JvmStatic::class)
            .addParameter("json", String::class)
            .addParameter(transformBlock)
            .addStatement(
                "return decode(%T(json),transform)",
                JsonObject::class
            )
            .returns(element.asType().asTypeName())
        val invokeJson = FunSpec.builder("invoke")
            .addKdoc(doc)
            .addModifiers(KModifier.OPERATOR)
            .addAnnotation(JvmOverloads::class)
            .addAnnotation(JvmStatic::class)
            .addParameter("json", JsonObject::class)
            .addParameter(transformBlock)
            .addStatement("return decode(json,transform)")
            .returns(element.asType().asTypeName())
        val decodeJson = FunSpec.builder("decode")
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

                // Kon objects
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
                            parameters.addStatement(
                                """transform(json.getJsonObject("$name")) as ${type.simpleName}? ?: throw JsonException(%S)""",
                                "Class·${type.simpleName}·needs·to·be·marked·as·Codable·or·parsed·using·the·transform·block"
                            )
                        }
                    }
                }
            }

            if (!isLast) {
                parameters.add(", ")
            }
        }

        decodeJson.addCode(
            "return %T( ${parameters.build()} )",
            element.asType()
        )

        return listOf(
            decodeJson.build(),
            decodeString.build(),
            invokeJson.build(),
            invokeString.build()
        )
    }

    private fun decodeCollection(type: TypeMirror): CodeBlock {
        val code = CodeBlock.builder()

        getComponentType(type)?.let {
            when {
                // String
                isString(it) -> code.add("cast<String>(it)")

                // Numbers and primitives
                isInt(it) -> code.add("cast<Int>(it)")
                isLong(it) -> code.add("cast<Long>(it)")
                isBoolean(it) -> code.add("cast<Boolean>(it)")
                isDouble(it) -> code.add("cast<Double>(it)")
                isFloat(it) -> code.add("cast<Float>(it)")
                isByte(it) -> code.add("cast<Byte>(it)")
                isShort(it) -> code.add("cast<Short>(it)")
                isChar(it) -> code.add("cast<Char>(it)")
                isSubclass(it, BigDecimal::class) -> code.add("cast<BigDecimal>(it)")
                isSubclass(it, Number::class) -> code.add("cast<Number>(it)")

                // Kon objects
                isSubclass(it, JsonObject::class) -> code.add("cast<JsonObject>(it)")
                isSubclass(it, JsonArray::class) -> code.add("cast<JsonArray>(it)")

                // Date & Calendar      // FIXME This will only work with timestamps
                isSubclass(it, Date::class) -> code.add("cast<Date>(it)")
                isSubclass(it, Calendar::class) -> code.add("cast<Calendar>(it))")

                // IntRange, URL, Enum
                isSubclass(it, IntRange::class) -> code.add("cast<IntRange>(it)")
                isSubclass(it, URL::class) -> code.add("cast<URL>(it)")
                isEnum(it) -> code.add("it.toJsonValue().asEnum()")

                // Collection or Array
                isCollection(it) || isArray(it) -> {
                    code.add("cast<JsonArray>(it).toList{ ${decodeCollection(it)} }")
                }

                // Map
                isMap(it) -> {
                    code.add("cast<JsonObject>(it).toTypedMap()")
                }

                // Other
                else -> {
                    if (hasAnnotation(it, Codable::class)) {
                        code.add("${getDecoder(it)}.decode(it as JsonObject)")
                    } else {
                        if (hasAnnotation(it, Nullable::class)) {
                            code.add("transform(cast<JsonObject>(it)) as ${it.simpleName}? ?: null")
                        } else {
                            code.add(
                                """transform(cast<JsonObject>(it)) as ${it.simpleName}? ?: throw JsonException(%S)""",
                                "Class·${it.simpleName}·needs·to·be·marked·as·Codable·or·parsed·using·the·transform·block"
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