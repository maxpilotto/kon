package com.maxpilotto.kon.processor

import com.google.auto.service.AutoService
import com.maxpilotto.kon.JsonArray
import com.maxpilotto.kon.JsonObject
import com.maxpilotto.kon.annotations.JsonDate
import com.maxpilotto.kon.annotations.JsonEncodable
import com.maxpilotto.kon.extensions.getDefaultValue
import com.maxpilotto.kon.extensions.getFormat
import com.maxpilotto.kon.extensions.getLocale
import com.maxpilotto.kon.extensions.getName
import com.maxpilotto.kon.processor.extensions.simpleName
import com.maxpilotto.kon.processor.extensions.wrap
import com.squareup.kotlinpoet.*
import java.math.BigDecimal
import java.net.URL
import java.text.SimpleDateFormat
import java.util.*
import javax.annotation.Nullable
import javax.annotation.processing.Processor
import javax.lang.model.AnnotatedConstruct
import javax.lang.model.element.Element
import javax.lang.model.element.Modifier
import javax.lang.model.type.TypeMirror
import kotlin.reflect.KClass

/**
 * Processor which task is to handle all classes marked with the [JsonEncodable] annotation
 */
@AutoService(Processor::class)
class EncodableProcessor : KonProcessor() {
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
        JsonEncodable::class.java.canonicalName
    )

    override fun process(kClass: KClass<*>, elements: Set<Element>): Boolean {
        when (kClass) {
            JsonEncodable::class -> {
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
                        .addImport("$BASE_PACKAGE.util", "stringify")
                        .addImport("$BASE_PACKAGE.util", "localeFor")
                        .addImport("$BASE_PACKAGE.util", "parse")
                        .addImport("$BASE_PACKAGE.util", "parseDate")
                        .addImport("$BASE_PACKAGE.util", "parseEnum")
                        .addImport(BASE_PACKAGE, "JsonArray")
                        .addImport(BASE_PACKAGE, "JsonObject")  //TODO Import these only if needed
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

                    file.writeTo(processingEnv.filer)
                }

                return true
            }
        }

        return false
    }

    private fun encodeClass(element: Element): List<FunSpec> {  //TODO Add method for list decoding/encoding
        val doc = """
            Returns the calling object as a [JsonObject]
            
            If any of the properties is not supported by the [JsonObject]'s [JsonObject.set] method and it 
            is not marked as [JsonEncodable], the toString() method of that instance will be called and that
            property will be saved as a String
            
            The [transform] block can be used to encode objects that are not marked as [JsonEncodable],
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
            .addParameter(transformBlock)   //TODO transform could return Any?, so that an object can be turned into a JsonArray too or String
            .addStatement("val json = JsonObject()")
            .returns(JsonObject::class)
        val isKotlin = isKotlinClass(element)

        getProperties(element) { prop, name, type, _, annotation ->
            val isPublic = prop.modifiers.contains(Modifier.PUBLIC)
            val getter = if (isKotlin || isPublic) {
                "data.$name"
            } else {
                "data.get${name.capitalize()}()"
            }
            val actualName = annotation?.let {
                if (it.name.isNotEmpty()) it.name else name
            } ?: name

            when {
                // Date & Calendar
                isSubclass(type, Date::class) || isSubclass(type, Calendar::class) -> {
                    val isDateType = isSubclass(type, Date::class)
                    val date = prop.getAnnotation(JsonDate::class.java)

                    if (date == null || date.isTimestamp) {
                        if (isDateType) {
                            method.addStatement("""json.set("$actualName",$getter.time)""")
                        } else {
                            method.addStatement("""json.set("$actualName",$getter.timeInMillis)""")
                        }
                    } else {
                        val format = """%T("${date.getFormat()}",localeFor("${date.getLocale()}"))"""

                        if (isDateType) {
                            method.addStatement(
                                """json.set("$actualName",$format.format($getter))""",
                                SimpleDateFormat::class
                            )
                        } else {
                            method.addStatement(
                                """json.set("$actualName",$format.format($getter.time))""",
                                SimpleDateFormat::class
                            )
                        }
                    }
                }

                // The property is one of the type that are supported by the JsonObject and don't need any
                // special action, they can be directly added to the object
                isSupportedType(type) -> {
                    method.addStatement("""json.set("$actualName",$getter)""")  //TODO Add the transform for all types
                }

                // The property is a collection or array, this must be resolved recursively
                // due to the fact that it could be a list of lists or an array of arrays
                isArray(type) || isCollection(type) -> {
                    val component = getComponentType(type)

                    method.addStatement(
                        """json.set("$actualName",%T(${encodeCollection(component, prop, getter)}))""",
                        JsonArray::class
                    )
                }

                // The property type is not supported
                //
                // If the property's class is marked as JsonEncodable, the encode() method will be called,
                // otherwise the transform block will be called and if that block returns a null object
                // the toString() method will be used
                else -> {
                    val transform = if (hasAnnotation(type, JsonEncodable::class)) {
                        "${getEncoder(prop)}($getter)"
                    } else {
                        "transform($getter) ?: $getter"
                    }

                    method.addStatement("""json.set("$actualName",$transform)""")
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
        component: TypeMirror,
        root: Element,
        propName: String = "it"
    ): CodeBlock {
        val code = CodeBlock.builder()

        when {
            isSubclass(component, Date::class) || isSubclass(component, Calendar::class) -> {
                val isDateType = isSubclass(component, Date::class)
                val date = root.getAnnotation(JsonDate::class.java)

                if (date == null || date.isTimestamp) {
                    if (isDateType) {
                        code.add("it.time")
                    } else {
                        code.add("it.timeInMillis")
                    }
                } else {
                    val format = """%T("${date.getFormat()}",localeFor("${date.getLocale()}"))"""

                    if (isDateType) {
                        code.add(
                            "stringify($format.format(it))",
                            SimpleDateFormat::class
                        )
                    } else {
                        code.add(
                            "stringify($format.format(it.time))",
                            SimpleDateFormat::class
                        )
                    }
                }
            }

            isSupportedType(component) -> {
                code.add("stringify(it)")
            }

            isArray(component) || isCollection(component) -> {
                code.add(encodeCollection(getComponentType(component), root))
            }

            else -> {
                if (hasAnnotation(component, JsonEncodable::class)) {
                    code.add("${getEncoder(component)}(it).toString()")
                } else {
                    code.add("(transform(it) ?: it).toString()")
                }
            }
        }

        return CodeBlock.of("""$propName.joinToString(",","[","]",transform = { ${code.build()} })""")
    }

    //TODO Add a keyTransform callback
    //encode(keyTransform = { key, value
    //      when (it) {
    //          "firstName" -> {    // Key
    //              value.capitalize()
    //          }

    //          "data/array" -> {   // Path
    //              val array = value as JsonArray
    //
    //              Address(array[0], array[1], array[2])
    //          }
    //      }
    // })

    private fun decodeClass(element: Element): List<FunSpec> {
        val doc = """
            Creates an instance of ${element.asType().simpleName} from the given [json]
            
            If any of the properties is not supported by the [JsonObject]'s [JsonObject.set] method,
            it is not marked as [JsonEncodable] and it is nullable then null will be returned, otherwise
            an exception is thrown
            
            The [transform] block can be used to decode objects that are not marked as [JsonEncodable]
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
            .addStatement("return decode(JsonObject(json),transform)")
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

        getProperties(element) { prop, name, type, isLast, annotation ->
            val actualName = (annotation.getName() ?: name).wrap("\"")
            val operator = if (annotation?.isOptional == true) "opt" else "get"
            val default = if (annotation?.isOptional == true) ""","${annotation.getDefaultValue()}"""" else ""

            when {
                // Supported types that do not require additional parsing
                isString(type) -> parameters.add("json.${operator}String($actualName$default)")
                isInt(type) -> parameters.add("json.${operator}Int($actualName$default)")
                isLong(type) -> parameters.add("json.${operator}Long($actualName$default)")
                isBoolean(type) -> parameters.add("json.${operator}Boolean($actualName$default)")
                isDouble(type) -> parameters.add("json.${operator}Double($actualName$default)")
                isFloat(type) -> parameters.add("json.${operator}Float($actualName$default)")
                isByte(type) -> parameters.add("json.${operator}Byte($actualName$default)")
                isShort(type) -> parameters.add("json.${operator}Short($actualName$default)")
                isChar(type) -> parameters.add("json.${operator}Char($actualName$default)")
                isSubclass(type, BigDecimal::class) -> parameters.add("json.${operator}BigDecimal($actualName$default)")
                isSubclass(type, Number::class) -> parameters.add("json.${operator}Number($actualName$default)")
                isSubclass(type, JsonObject::class) -> parameters.add("json.${operator}JsonObject($actualName$default)")
                isSubclass(type, JsonArray::class) -> parameters.add("json.${operator}JsonArray($actualName$default)")
                isSubclass(type, IntRange::class) -> parameters.add("json.${operator}Range($actualName$default)")
                isSubclass(type, URL::class) -> parameters.add("json.${operator}URL($actualName$default)")
                isSubclass(type, Enum::class) -> parameters.add("json.${operator}Enum($actualName$default)")

                // Date & Calendar
                isSubclass(type, Date::class) || isSubclass(type, Calendar::class) -> {
                    val typeName = if (isSubclass(type, Date::class)) "Date" else "Calendar"    //TODO Use the variable "type"
                    val date = prop.getAnnotation(JsonDate::class.java)

                    if (date == null || date.isTimestamp) {
                        parameters.add("json.$operator$typeName($actualName$default)")
                    } else {
                        //FIXME The format should be fixed here and in the JsonObject
                        parameters.add("""json.$operator$typeName($actualName,"${date.getFormat()}"$default,localeFor("${date.getLocale()}"))""")
                    }
                }

                // Collection
                isCollection(type) -> { //TODO Merge this and the array
                    val decoder = decodeCollection(getComponentType(type), prop)

                    parameters.add("json.${operator}List($actualName) { $decoder }")
                }

                // Array
                isArray(type) -> {
                    val decoder = decodeCollection(getComponentType(type), prop)

                    parameters.add("json.${operator}List($actualName) { $decoder } }.toTypedArray()")
                }

                // Map
                isMap(type) -> {
                    parameters.add("json.${operator}JsonObject($actualName).toTypedMap()")
                }

                // Unsupported type
                else -> {
                    if (hasAnnotation(prop, JsonEncodable::class)) {
                        parameters.add("${getDecoder(prop)}(json.${operator}JsonObject($actualName))")  //FIXME Pass the transform too
                    } else {
                        if (hasAnnotation(prop, Nullable::class)) {     //FIXME This doesn't work
                            parameters.add("transform(json.${operator}JsonObject($actualName)) as ${type.simpleName}? ?: null")
                        } else {
                            parameters.add(
                                """transform(json.${operator}JsonObject($actualName)) as ${type.simpleName}? ?: throw JsonException(""%S"")""",
                                "Class ${type.simpleName} needs to be marked as JsonEncodable or parsed using the transform block"
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

    private fun decodeCollection(component: TypeMirror, root: Element): CodeBlock {
        val code = CodeBlock.builder()

        when {
            // Supported types that do not require additional parsing
            isString(component) -> code.add("parse<String>(it)")
            isInt(component) -> code.add("parse<Int>(it)")
            isLong(component) -> code.add("parse<Long>(it)")
            isBoolean(component) -> code.add("parse<Boolean>(it)")
            isDouble(component) -> code.add("parse<Double>(it)")
            isFloat(component) -> code.add("parse<Float>(it)")
            isByte(component) -> code.add("parse<Byte>(it)")
            isShort(component) -> code.add("parse<Short>(it)")
            isChar(component) -> code.add("parse<Char>(it)")
            isSubclass(component, Enum::class) -> code.add("parseEnum<%T>(it)", component)
            isSubclass(component, BigDecimal::class) -> code.add("parse<%T>(it)", BigDecimal::class)
            isSubclass(component, Number::class) -> code.add("parse<Number>(it) ")
            isSubclass(component, JsonObject::class) -> code.add("parse<JsonObject>(it)")
            isSubclass(component, JsonArray::class) -> code.add("parse<JsonArray>(it)")
            isSubclass(component, IntRange::class) -> code.add("parse<IntRange>(it)")
            isSubclass(component, URL::class) -> code.add("parse<URL>(it)")

            // Date & Calendar
            isSubclass(component, Date::class) || isSubclass(component, Calendar::class) -> {
                val date = root.getAnnotation(JsonDate::class.java)

                if (date == null || date.isTimestamp) {
                    code.add("parse<%T>(it)", component)
                } else {
                    code.add(
                        """parseDate<%T>(it,"${date.getFormat()}",localeFor("${date.getLocale()}"))""",
                        component
                    )
                }
            }

            // Collection
            isCollection(component) -> {
                val decoder = decodeCollection(getComponentType(component), root)

                code.add("parse<JsonArray>(it).toList{ $decoder }")
            }

            // Array
            isArray(component) -> {
                val decoder = decodeCollection(getComponentType(component), root)

                code.add("parse<JsonArray>(it).toList{ $decoder }.toTypedArray()")
            }

            // Map
            isMap(component) -> {
                code.add("parse<JsonObject>(it).toTypedMap()")
            }

            // Unsupported type
            else -> {
                if (hasAnnotation(component, JsonEncodable::class)) {
                    code.add("${getDecoder(component)}(it as JsonObject)")
                } else {
                    if (hasAnnotation(component, Nullable::class)) {
                        code.add("transform(parse<JsonObject>(it)) as ${component.simpleName}? ?: null")
                    } else {
                        code.add(
                            """transform(parse<JsonObject>(it)) as ${component.simpleName}? ?: throw JsonException(""%S"")""",
                            "Class ${component.simpleName} needs to be marked as JsonEncodable or parsed using the transform block"
                        )
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