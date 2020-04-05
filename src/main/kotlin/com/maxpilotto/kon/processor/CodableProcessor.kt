package com.maxpilotto.kon.processor

import com.google.auto.service.AutoService
import com.maxpilotto.kon.JsonArray
import com.maxpilotto.kon.JsonObject
import com.maxpilotto.kon.annotations.Codable
import com.maxpilotto.kon.annotations.Encodable
import com.maxpilotto.kon.extensions.*
import com.squareup.kotlinpoet.*
import javax.annotation.processing.Processor
import javax.lang.model.element.Element
import javax.lang.model.element.ElementKind
import kotlin.reflect.KClass

@AutoService(Processor::class)
class CodableProcessor : BaseProcessor() {
    override fun getSupportedAnnotations() = mutableListOf(
        Codable::class,
        Encodable::class
    )

    override fun process(kClass: KClass<*>, elements: Set<Element>): Boolean {
        when (kClass) {
            Codable::class -> {
                for (element in elements) {
                    val packageName = elementUtils.getPackageOf(element).toString()
                    val fileName = "${element.simpleName}.kt"

                    val file = FileSpec.builder(packageName, fileName)
                        .addImport(JsonObject::class)
                        .addImport(JsonArray::class)
                        .addFunction(encodeClass(element))
//                        .addFunction(decodeClass(element))
                        .indent("\t")
                        .build()

//                    val companion = TypeSpec.companionObjectBuilder()
//                        .addFunction(encodeClass(element))
//                        .build()
//
//                    val klass = TypeSpec.classBuilder(fileName)
//                        .addType(companion)
//                        .build()

                    file.writeTo(generatedDir)
                }

                return true
            }
        }

        return false
    }

    /**
     * Creates a [FunSpec] that encodes the given [element]'s class
     */
    private fun encodeClass(element: Element): FunSpec {
        val fieldName = element.simpleName.toString().decapitalize()
        val method = FunSpec.builder("toJson")
            .receiver(element.asType().asTypeName())
            .addStatement("val json = JsonObject()")
            .returns(JsonObject::class)


//        val encoderLambda = LambdaTypeName.get(
//            null,
//            listOf(
//                ParameterSpec
//                    .builder("", Any::class)    //TODO It should be Any?
//                    .build()
//            ),
//            JSONObject::class.asTypeName().copy(true)  //TODO This must be nullable
//        )
//        val encoder = ParameterSpec
//            .builder("encoder", encoderLambda)
//            .defaultValue("{ null }")
//            .build()
//        val method = FunSpec.builder("encode")  //TODO Mark as inline
//            .addParameter(
//                fieldName,
//                element.asType().asTypeName()
//            )
//            .addParameter(encoder)
//            .addStatement(
//                "val json = %T()",
//                JSONObject::class
//            )
//            .returns(JSONObject::class)

        for (prop in element.enclosedElements) {
            if (prop.kind == ElementKind.FIELD) {
                val propName = prop.simpleName.toString()
                val propType = prop.asType()

                //TODO Add automatic conversion for StringBuilder and buffer
                //TODO Recursively check for complex arrays/collections
                //TODO Add option for specifying the parsing block in the encode method
                /*
                    BookDao.encode(
                        book,
                        forStringBuilder = { it ->
                            it.toString()
                        },
                        forAny = { it ->
                            it.toString()
                        }
                 */

                when {
                    propType.isPrimitive() || propType.isString(processingEnv) -> {
                        method.addStatement(
                            """json.set("$propName",$fieldName.$propName)"""
                        )
                    }

                    propType.isArray() || propType.isList(processingEnv) -> {
//                        val content = encodeCollection(propType, propName, fieldName)

                        method.addStatement(
                            """json.put("$propName",JsonArray($fieldName.$propName))"""
                        )
                    }

                    propType.isMap(processingEnv) -> {
                        //TODO Parse map, technically a map is just another JSONObject
                    }

                    else -> {
//                        val packageName = elementUtils.getPackageOf(element).toString()
//                        val encoderClass = classNameFor(
//                            packageName,
//                            propType,
//                            POSTFIX
//                        )
//
//                        if (com.maxpilotto.kon.extensions.hasAnnotation(Codable::class)) {
//                            method.addStatement(
//                                """json.put("$propName",encoder($fieldName.$propName) ?: %T.encode($fieldName.$propName,encoder))""",
//                                encoderClass
//                            )
//                        } else {
//                            method.addStatement(
//                                """json.put("$propName",encoder($fieldName.$propName) ?: throw %T(%S))""",
//                                Exception::class,    //TODO Add all imports manually to the file
//                                "Class ${propType.simpleTypeName} must be either annotated with Encodable/Codable or the encoder handler must encode the object"
//                            )
//                        }
                    }
                }
            }
        }

        return method.addStatement("return json")
            .build()
    }

//    /**
//     * Encodes the given element, which must be an Iterable, and returns a string containing its
//     * encoding code
//     *
//     * The string will will have the format `[ data ]`, if the Iterable's type is another Iterable
//     * the result will look like `[ [ data ] ]` and so on
//     */
//    private fun encodeCollection(
//        type: TypeMirror,
//        propName: String = "it",
//        parent: String = ""
//    ): String {
//        val component = type.getComponentType(processingEnv)
//        val dot = if (parent.isEmpty()) "" else "."
//        val it = "$parent$dot$propName"
//        val builder = StringBuilder(
//            """$it.joinToString(",","[","]",transform = {"""
//        )
//
//        component?.run {
//            when {
//                isArray() || isList(processingEnv) -> {
//                    builder += encodeCollection(component)
//                }
//
//                isMap(processingEnv) -> {
//                    //TODO Encode map
//                }
//
//                isPrimitive() || isString(processingEnv) -> {
//                    builder += "it.toString()"
//                }
//
//                else -> {
//                    val e = typeUtils.asElement(component)
//                    val pack = elementUtils.getPackageOf(e).toString()
////                    val encoderClass = classNameFor(
////                        pack,
////                        component,
////                        POSTFIX
////                    )
//                    val encoder = classNameFor(component, POSTFIX)
//
//                    if (com.maxpilotto.kon.extensions.hasAnnotation(Codable::class)) {
//                        builder += """encoder(it)?.toString() ?: $encoder.encode(it,encoder)"""
//                    } else {
//                        //FIXME The Exception is too long
////                        builder += """encoder($propName) ?: throw Exception("Class ${component.simpleTypeName} must be either annotated with Encodable/Codable or the encoder handler must encode the object")"""
//                        builder += """encoder(it)?.toString() ?: throw Exception("Cannot encode")"""
//                    }
////
////                        method.addStatement(
////                            """json.put("$propName",encoder($fieldName.$propName) ?: %T.encode($fieldName.$propName))""",
////                            encoderClass
////                        )
////                    } else {
////                        method.addStatement(
////                            //TODO Rename the error, currently a long error is not supported inside kotlinpoet
////                            //"Class ${propType.simpleTypeName} must be either annotated with EncodableCodable or the encoder handler must encode the object"
////                            """json.put("$propName",encoder($fieldName.$propName) ?: throw %T("Cannot encode class: ${propType.simpleTypeName}"))""",
////                            Exception::class
////                        )
////                    }
//
//
//                    // TODO Throw an exception if the type doesn't exists,
//                    //  alternatively use the custom type parser
////                    builder += "$encoder.encode(it).toString()"
//                }
//            }
//        }
//
//        builder += "})"
//
//        return builder.toString()
//    }
//
//    companion object {
//        const val POSTFIX = "Dao"
//
//        internal fun log(message: Any?) {    //TODO Remove in production
//            val output = File("/Users/Max/Desktop/debug.txt")
//
//            output.appendText(message.toString() + "\n")
//        }
//    }
}
