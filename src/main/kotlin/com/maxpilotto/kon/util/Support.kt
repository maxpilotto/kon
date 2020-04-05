package com.maxpilotto.kon.util

import com.maxpilotto.kon.extensions.simpleTypeName
import com.squareup.kotlinpoet.ClassName
import javax.lang.model.element.Element
import javax.lang.model.type.TypeMirror

fun classNameFor(
    pack: String,
    element: Element,
    postfix: String = ""
): ClassName {
    return ClassName(
        pack,
        element.simpleName.toString() + postfix
    )
}

fun classNameFor(
    element: Element,
    postfix: String = ""
): String {
    return element.simpleName.toString() + postfix
}

fun classNameFor(
    pack: String,
    type: TypeMirror,
    postfix: String = ""
): ClassName {
    return ClassName(
        pack,
        type.simpleTypeName + postfix
    )
}

fun classNameFor(
    type: TypeMirror,
    postfix: String = ""
): String {
    return type.simpleTypeName + postfix
}