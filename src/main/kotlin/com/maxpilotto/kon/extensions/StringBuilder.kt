package com.maxpilotto.kon.extensions

operator fun StringBuilder.plusAssign(string: String){
    append(string)
}

operator fun StringBuilder.plusAssign(builder: StringBuilder){
    append(builder)
}