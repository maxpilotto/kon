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

import com.squareup.kotlinpoet.FileSpec
import com.squareup.kotlinpoet.TypeSpec
import java.io.File
import javax.annotation.processing.AbstractProcessor
import javax.annotation.processing.RoundEnvironment
import javax.lang.model.element.Element
import javax.lang.model.element.TypeElement
import javax.lang.model.util.Elements
import javax.lang.model.util.Types
import kotlin.reflect.KClass

/**
 * Base Processor class
 */
abstract class BaseProcessor : AbstractProcessor() {
    protected val typeUtils: Types
        get() = processingEnv.typeUtils

    protected val elementUtils: Elements
        get() = processingEnv.elementUtils

    protected val generatedDir: File
        get() = File(processingEnv.options["kapt.kotlin.generated"])

    protected abstract fun process(kClass: KClass<*>, elements: Set<Element>): Boolean

    protected abstract fun getSupportedAnnotations(): MutableList<KClass<out Annotation>>

    override fun getSupportedAnnotationTypes(): MutableSet<String> {
        val annotations = getSupportedAnnotations()

        return MutableList(annotations.size) {
            annotations[it].qualifiedName!!
        }.toMutableSet()
    }

    override fun process(p0: MutableSet<out TypeElement>, p1: RoundEnvironment): Boolean {
        for (a in supportedAnnotationTypes) {
            val javaClass = Class.forName(a) as Class<Annotation>
            val elements = p1.getElementsAnnotatedWith(javaClass)

            return process(javaClass.kotlin, elements)
        }

        return false
    }
}