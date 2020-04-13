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

import com.maxpilotto.kon.JsonObject
import com.squareup.kotlinpoet.asTypeName
import java.io.File
import javax.annotation.processing.AbstractProcessor
import javax.annotation.processing.RoundEnvironment
import javax.lang.model.element.Element
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
     * Returns the given [element] as a [TypeElement]
     */
    protected fun getTypeElement(element: Element): TypeElement {
        return getTypeElement(element.asType())
    }

    /**
     * Returns the given [typeMirror] as a [TypeElement]
     */
    protected fun getTypeElement(typeMirror: TypeMirror): TypeElement {
        return typeUtils.asElement(typeMirror) as TypeElement
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

    companion object {
        val OPTIONAL_ANY = Any::class.asTypeName().copy(true)
        val OPTIONAL_OBJECT = JsonObject::class.asTypeName().copy(true)

        private val log = File("processor_log.log")

        fun logg(any: Any?) {
            log.appendText(any.toString() + "\n")
        }
    }
}