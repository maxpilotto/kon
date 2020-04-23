/*
 * Copyright 2020 Max Pilotto
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *       http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package com.maxpilotto.kon.processor

import com.squareup.kotlinpoet.ClassName
import com.squareup.kotlinpoet.TypeName
import com.squareup.kotlinpoet.asTypeName
import java.io.File
import javax.annotation.processing.AbstractProcessor
import javax.annotation.processing.RoundEnvironment
import javax.lang.model.element.Element
import javax.lang.model.element.ElementKind
import javax.lang.model.element.TypeElement
import javax.lang.model.type.ArrayType
import javax.lang.model.type.DeclaredType
import javax.lang.model.type.TypeKind
import javax.lang.model.type.TypeMirror
import javax.lang.model.util.Elements
import javax.lang.model.util.Types
import kotlin.reflect.KClass


/**
 * Base Processor class used to process Kon's annotations
 */
abstract class KonProcessor : AbstractProcessor() {
    protected val typeUtils: Types
        get() = processingEnv.typeUtils

    protected val elementUtils: Elements
        get() = processingEnv.elementUtils

    protected val generatedDir: File
        get() = File(processingEnv.options["kapt.kotlin.generated"])

    protected abstract fun process(kClass: KClass<*>, elements: Set<Element>): Boolean

    override fun process(p0: MutableSet<out TypeElement>, p1: RoundEnvironment): Boolean {
        for (a in supportedAnnotationTypes) {
            val javaClass = Class.forName(a) as Class<Annotation>
            val elements = p1.getElementsAnnotatedWith(javaClass)

            return process(javaClass.kotlin, elements)
        }

        return false
    }

    /**
     * Returns whether or not the given [element] has the given [annotation]
     */
    protected fun <A : Annotation> hasAnnotation(element: Element, annotation: KClass<A>): Boolean {
        return element.getAnnotation(annotation.java) != null ||
                getTypeElement(element).getAnnotation(annotation.java) != null
    }

    /**
     * Returns whether or not the given [typeMirror] has the given [annotation]
     */
    protected fun <A : Annotation> hasAnnotation(typeMirror: TypeMirror, annotation: KClass<A>): Boolean {
        return typeMirror.getAnnotation(annotation.java) != null ||
                getTypeElement(typeMirror).getAnnotation(annotation.java) != null
    }

    /**
     * Returns whether or not the given [element] is a Kotlin class
     *
     * This will look for the [Metadata] annotation
     */
    protected fun isKotlinClass(element: Element): Boolean {
        return hasAnnotation(getTypeElement(element), Metadata::class)
    }

    /**
     * Returns the given [element] as a [TypeElement]
     */
    protected fun getTypeElement(element: Element): TypeElement {
        return getTypeElement(element.asType())
    }

    /**
     * Returns the given [typeMirror] as a [TypeElement]
     */
    protected fun getTypeElement(typeMirror: TypeMirror): TypeElement { //TODO Replace all elements and typeMirrors with AnnotatedConstruct
        return typeUtils.asElement(typeMirror) as TypeElement
    }

    /**
     * Returns the [TypeName] of the given [typeMirror]'s Companion object
     */
    protected fun getCompanionObject(typeMirror: TypeMirror): TypeName {
        return ClassName.bestGuess("$typeMirror.Companion")
    }

    /**
     * Returns whether or not the given [typeMirror] is a primitive type
     *
     * This does not include boxed types, like Integer or Long
     */
    protected fun isPrimitive(typeMirror: TypeMirror): Boolean {
        return typeMirror.kind.isPrimitive
    }

    /**
     * Returns whether or not the given [typeMirror] is a String
     */
    protected fun isString(typeMirror: TypeMirror): Boolean {
        return isSubclass(typeMirror, String::class.java)
    }

    /**
     * Returns whether or not the given [typeMirror] is an Int
     *
     * This will check for both Int (kotlin) and int/Integer (java)
     */
    protected fun isInt(typeMirror: TypeMirror): Boolean {
        return typeMirror.kind == TypeKind.INT ||
                isSubclass(typeMirror, Int::class) ||
                isSubclass(typeMirror, java.lang.Integer::class)
    }

    /**
     * Returns whether or not the given [typeMirror] is a Long
    *
    * This will check for both Long (kotlin) and long/Long (java)
    */
    protected fun isLong(typeMirror: TypeMirror): Boolean {
        return typeMirror.kind == TypeKind.LONG ||
                isSubclass(typeMirror, Long::class) ||
                isSubclass(typeMirror, java.lang.Long::class)
    }

    /**
     * Returns whether or not the given [typeMirror] is a Boolean
     *
     * This will check for both Boolean (kotlin) and bool/Boolean (java)
     */
    protected fun isBoolean(typeMirror: TypeMirror): Boolean {
        return typeMirror.kind == TypeKind.BOOLEAN ||
                isSubclass(typeMirror, Boolean::class) ||
                isSubclass(typeMirror, java.lang.Boolean::class)
    }

    /**
     * Returns whether or not the given [typeMirror] is a Double
     *
     * This will check for both Double (kotlin) and double/Double (java)
     */
    protected fun isDouble(typeMirror: TypeMirror): Boolean {
        return typeMirror.kind == TypeKind.DOUBLE ||
                isSubclass(typeMirror, Double::class) ||
                isSubclass(typeMirror, java.lang.Double::class)
    }

    /**
     * Returns whether or not the given [typeMirror] is a Float
     *
     * This will check for both Float (kotlin) and float/Float (java)
     */
    protected fun isFloat(typeMirror: TypeMirror): Boolean {
        return typeMirror.kind == TypeKind.FLOAT ||
                isSubclass(typeMirror, Float::class) ||
                isSubclass(typeMirror, java.lang.Float::class)
    }

    /**
     * Returns whether or not the given [typeMirror] is a Byte
     *
     * This will check for both Byte (kotlin) and byte/Byte (java)
     */
    protected fun isByte(typeMirror: TypeMirror): Boolean {
        return typeMirror.kind == TypeKind.BYTE ||
                isSubclass(typeMirror, Byte::class) ||
                isSubclass(typeMirror, java.lang.Byte::class)
    }

    /**
     * Returns whether or not the given [typeMirror] is a Short
     *
     * This will check for both Short (kotlin) and short/Short (java)
     */
    protected fun isShort(typeMirror: TypeMirror): Boolean {
        return typeMirror.kind == TypeKind.SHORT ||
                isSubclass(typeMirror, Short::class) ||
                isSubclass(typeMirror, java.lang.Short::class)
    }

    /**
     * Returns whether or not the given [typeMirror] is a Char
     *
     * This will check for both Char (kotlin) and char/Character (java)
     */
    protected fun isChar(typeMirror: TypeMirror): Boolean {
        return typeMirror.kind == TypeKind.CHAR ||
                isSubclass(typeMirror, Char::class) ||
                isSubclass(typeMirror, java.lang.Character::class)
    }

    /**
     * Returns whether or not the given [typeMirror] is a Collection or
     * a Collection sub type (List, MutableList, ArrayList, Set, ...)
     */
    protected fun isCollection(typeMirror: TypeMirror): Boolean {
        return isSubclass(typeMirror, Collection::class.java)
    }

    /**
     * Returns whether or not the given [typeMirror] is an Array type
     */
    protected fun isArray(typeMirror: TypeMirror): Boolean {
        return typeMirror.kind == TypeKind.ARRAY
    }

    /**
     * Returns whether or not the given [typeMirror] is a Map or a
     * Map subtype (MutableMap, HashMap, LinkedMap; ...)
     */
    protected fun isMap(typeMirror: TypeMirror): Boolean {
        return isSubclass(typeMirror, Map::class.java)
    }

    /**
     * Returns whether or not the given [typeMirror] is an Enum
     */
    protected fun isEnum(typeMirror: TypeMirror): Boolean {
        return isSubclass(typeMirror, Enum::class)
    }

    /**
     * Returns whether or not the given [typeMirror] is a subclass of the given
     * [base] class
     */
    protected fun isSubclass(typeMirror: TypeMirror, base: Class<*>): Boolean {
        return try {
            val type = processingEnv.elementUtils
                .getTypeElement(base.name)
                .asType()
            val actualType = processingEnv.typeUtils.erasure(type)

            processingEnv.typeUtils.isAssignable(
                typeMirror,
                actualType
            )
        } catch (e: Exception) {
            false
        }
    }

    /**
     * Returns whether or not the given [typeMirror] is a subclass of the given
     * [base] class
     */
    protected fun isSubclass(typeMirror: TypeMirror, base: KClass<*>): Boolean {
        return isSubclass(typeMirror, base.java)
    }

    /**
     * Returns the component type of the given [typeMirror] in case it is an Array or
     * a Collection, otherwise it returns null
     */
    protected fun getComponentType(typeMirror: TypeMirror): TypeMirror? {
        return when {
            isArray(typeMirror) -> (typeMirror as ArrayType).componentType
            isCollection(typeMirror) -> (typeMirror as DeclaredType).typeArguments[0]

            else -> null
        }
    }

    /**
     * Loops through all the properties of the given [element] that are [ElementKind.FIELD]
     * and are not the Companion object
     */
    protected inline fun getProperties(
        element: Element,
        block: (prop: Element, name: String, type: TypeMirror, isLast: Boolean) -> Unit
    ) {
        val elements = element.enclosedElements.filter {
            it.kind == ElementKind.FIELD && it.simpleName.toString() != "Companion"
        }

        for ((i, prop) in elements.withIndex()) {
            val name = prop.simpleName.toString()
            val type = prop.asType()
            val last = i == elements.lastIndex

            block(prop, name, type, last)
        }
    }

    companion object {
        internal val BASE_PACKAGE = "com.maxpilotto.kon"
        internal val OPTIONAL_ANY = Any::class.asTypeName().copy(true)
    }
}