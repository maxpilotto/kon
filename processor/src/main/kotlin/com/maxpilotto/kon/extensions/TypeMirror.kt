package com.maxpilotto.kon.extensions

import com.squareup.kotlinpoet.asTypeName
import javax.annotation.processing.ProcessingEnvironment
import javax.lang.model.type.ArrayType
import javax.lang.model.type.DeclaredType
import javax.lang.model.type.TypeKind
import javax.lang.model.type.TypeMirror

val TypeMirror.typeName: String
    get() = asTypeName().toString()

val TypeMirror.simpleTypeName: String
    get() = typeName.let {
        if (it.contains('.')) {
            it.substring(
                it.lastIndexOf('.') + 1,
                it.length
            )
        } else {
            it
        }
    }

fun TypeMirror.isPrimitive(
    includeJavaWrappers: Boolean = true
): Boolean {
    return kind.isPrimitive || (includeJavaWrappers && when (asTypeName().toString()) {
        "java.lang.Integer",
        "java.lang.Boolean",
        "java.lang.Byte",
        "java.lang.Short",
        "java.lang.Character",
        "java.lang.Long",
        "java.lang.Float",
        "java.lang.Double" -> true

        else -> false
    })
}

fun TypeMirror.isString(env: ProcessingEnvironment): Boolean {
    return isSubclassOf(String::class.java, env)
}

fun TypeMirror.isCollection(env: ProcessingEnvironment): Boolean {
    return isSubclassOf(Collection::class.java, env)
}

fun TypeMirror.isList(env: ProcessingEnvironment): Boolean {   //TODO Should be iterable instead of list
    return isSubclassOf(List::class.java, env)
}

fun TypeMirror.isArray(): Boolean {
    return kind == TypeKind.ARRAY
}

fun TypeMirror.isMap(env: ProcessingEnvironment): Boolean {
    return isSubclassOf(Map::class.java, env)
}

/**
 * Returns whether this TypeMirror is complex or not
 *
 * A complex object is an object that is not a primitive, string, collection, map or array
 */
fun TypeMirror.isComplex(
    env: ProcessingEnvironment,
    excludeJavaWrappers: Boolean = false
): Boolean {
    return !isPrimitive(!excludeJavaWrappers) &&
            !isArray() &&
            !isString(env) &&
            !isCollection(env) &&
            !isMap(env)
}

/**
 * Returns whether or not this element is a subclass of the given [base] class name
 */
fun TypeMirror.isSubclassOf(
    base: String,
    env: ProcessingEnvironment
): Boolean {
    return try {
        val type = env.elementUtils
            .getTypeElement(base)
            .asType()
        val actualType = env.typeUtils.erasure(type)

        env.typeUtils.isAssignable(
            this,
            actualType
        )
    } catch (e: Exception) {
        false
    }

    
}

/**
 * Returns whether or not this element is a subclass of the given [base] java class
 */
fun TypeMirror.isSubclassOf(
    base: Class<*>,
    env: ProcessingEnvironment
): Boolean {
    return isSubclassOf(
        base.name ?: "",
        env
    )
}

/**
 * Returns the component type of this collection/array
 *
 * If the element is not a collection or array it will return null
 */
fun TypeMirror.getComponentType(env: ProcessingEnvironment): TypeMirror? {
    return when {
        isArray() -> {
            return (this as ArrayType).componentType
        }
        isList(env) -> {
            return (this as DeclaredType).typeArguments[0]
        }

        else -> null
    }
}