package com.maxpilotto.kon.processor

import com.google.auto.service.AutoService
import com.maxpilotto.kon.JsonArray
import com.maxpilotto.kon.JsonObject
import com.maxpilotto.kon.annotations.Codable
import com.maxpilotto.kon.extensions.plusAssign
import com.squareup.kotlinpoet.*
import javax.annotation.processing.Processor
import javax.lang.model.element.Element
import javax.lang.model.element.ElementKind
import javax.lang.model.type.TypeMirror
import kotlin.reflect.KClass

@AutoService(Processor::class)
class CodableProcessor : KonProcessor() {
    override fun getSupportedAnnotationTypes() = mutableSetOf(
        Codable::class.java.canonicalName
    )

    override fun process(kClass: KClass<*>, elements: Set<Element>): Boolean {
        when (kClass) {
            Codable::class -> {
                for (element in elements) {
                    val packageName = elementUtils.getPackageOf(element).toString()
                    val fileName = element.simpleName.toString()

                    val fileBuilder = FileSpec.builder(packageName, fileName)
                        .addFunction(encodeClass(element))
//                        .addFunction(decodeClass(element))


                    fileBuilder.indent("\t")
                    fileBuilder.build()
                        .writeTo(generatedDir)
                }

                return true
            }
        }

        return false
    }

    /**
     * Returns a [FunSpec] which creates the `encode` method extension
     * used to encode a class marked with the Codable annotation
     *
     * The extension method can be accessed within Java using the
     * static method encode() inside the class named (CodableClass)Kt
     */
    private fun encodeClass(element: Element): FunSpec {
        val docs = """
            Returns the calling object as a [JsonObject]
            
            If any of the properties is not a primitive, String, Collection or Map and is not
            marked as Codable, the toString() method will be called on that property
            to retrieve the data
            
            The [encoder] block can be used to encode objects that are not marked as Codable,
            this block should return the value as it should be saved, generally a 
            JsonObject, a String or a Number
        """.trimIndent()
        val encoderLambda = LambdaTypeName.get(
            null,
            listOf(
                ParameterSpec
                    .builder("", OPTIONAL_ANY)
                    .build()
            ),
            OPTIONAL_ANY
        )
        val encoder = ParameterSpec
            .builder("encoder", encoderLambda)
            .defaultValue("{ null }")
            .build()
        val method = FunSpec.builder("encode")
            .addKdoc(docs)
            .addAnnotation(JvmOverloads::class)
            .receiver(element.asType().asTypeName())
            .addParameter(encoder)
            .addStatement("val json = %T()", JsonObject::class)
            .returns(JsonObject::class)

        for (prop in element.enclosedElements) {
            if (prop.kind == ElementKind.FIELD) {
                val propName = prop.simpleName.toString()
                val propType = prop.asType()

                when {
                    // The property is a primitive or String, they can just be added to the object
                    // with no additional action
                    isPrimitive(propType) || isString(propType) -> {
                        method.addStatement(
                            """json.set("$propName",$propName)"""
                        )
                    }

                    // The property is a collection or array, this must be resolved recursively
                    // due to the fact that it could be a list of lists or an array of arrays
                    isArray(propType) || isCollection(propType) -> {
                        val content = encodeCollection(propType, propName)

                        method.addStatement(
                            """json.set("$propName",%T($content))""",
                            JsonArray::class
                        )
                    }

                    // The property is a map, this can be added but must be converted into
                    // a JsonObject first
                    // This can be done using one of the JsonObject's constructors
                    isMap(propType) -> {
                        method.addStatement(
                            """json.set("$propName",%T($propName.toMutableMap() as MutableMap<String, Any?>))""",
                            JsonObject::class
                        )
                    }

                    // The property is not a primitive, collection, array or map
                    //
                    // If the property's class is marked as Codable, the encode() method will be called,
                    // otherwise the encoder block will be called and if that block returns a null object
                    // the toString() method will be used
                    else -> {
                        val codable = Codable::class.java
                        val typeElement = getTypeElement(prop)
                        val transform = if (typeElement.getAnnotation(codable) != null) {
                            """$propName.encode()"""
                        } else {
                            """encoder($propName) ?: $propName.toString()"""
                        }

                        method.addStatement(
                            """json.set("$propName",$transform)"""
                        )
                    }
                }
            }
        }

        return method.addStatement("return json").build()
    }

    /**
     * Encodes the given element, which must be an Iterable, and returns a string containing its
     * encoding code
     *
     * The string will will have the format `[ data ]`, if the Iterable's type is another Iterable
     * the result will look like `[ [ data ] ]` and so on
     */
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
                isArray(it) || isCollection(it) -> {
                    builder.append(encodeCollection(it))
                }

                isMap(it) -> {
                    builder.append("JsonObject(it.toMutableMap() as MutableMap<String, Any?>).toString()")
                }

                isPrimitive(it) || isString(it) -> {
                    builder += "it.toString()"
                }

                else -> {
                    val codable = Codable::class.java
                    val typeElement = getTypeElement(it)

                    if (typeElement.getAnnotation(codable) != null) {
                        builder.append("""it.encode().toString()""")
                    } else {
                        builder.append("""(encoder(it) ?: it).toString()""")
                    }
                }
            }
        }

        builder += "})"

        return builder.toString()
    }
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
