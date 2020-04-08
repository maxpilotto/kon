package com.maxpilotto.kon.extensions

import javax.lang.model.element.Element
import kotlin.reflect.KClass

fun Element.hasAnnotation(annotation: Annotation): Boolean {
    return annotationMirrors.any {
        it.annotationType.typeName == annotation.javaClass.canonicalName
    }
}

fun Element.hasAnnotation(annotation: KClass<*>): Boolean {
    return annotationMirrors.any {
        it.annotationType.typeName == annotation.qualifiedName
    }
}