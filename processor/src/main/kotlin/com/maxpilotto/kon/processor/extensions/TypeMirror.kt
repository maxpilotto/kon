package com.maxpilotto.kon.processor.extensions

import com.squareup.kotlinpoet.asTypeName
import javax.lang.model.type.TypeMirror

/**
 * Returns the simple type name of this [TypeMirror]
 *
 * Example:
 *
 * "com.example.models.Person" would result into "Person"
 */
internal val TypeMirror.simpleName: String
    get() {
        val typeName = asTypeName().toString()

        return typeName.let {
            if (it.contains('.')) {
                it.substring(
                    it.lastIndexOf('.') + 1,
                    it.length
                )
            } else {
                it
            }
        }
    }