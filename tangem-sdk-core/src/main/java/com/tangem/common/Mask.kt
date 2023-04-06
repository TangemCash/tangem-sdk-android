package com.tangem.common

/**
 * Created by Anton Zhilenkov on 14/07/2021.
 */
interface Mask {
    val rawValue: Int
    val values: List<Code>

    fun contains(code: Code): Boolean {
        return rawValue and code.value != 0
    }

    interface Code {
        val value: Int
        val name: String
    }
}

abstract class BaseMask : Mask {
    fun toList(): List<Mask.Code> = values.filter { contains(it) }.map { it }

    override fun toString(): String = values.filter { contains(it) }.joinToString(", ")
}

internal class MaskBuilder {
    var maskValue = 0
        private set

    fun add(maskCode: Mask.Code) {
        maskValue = maskValue or maskCode.value
    }

    inline fun <reified T : Mask> build(): T = T::class.java.constructors[0].newInstance(maskValue) as T
}
