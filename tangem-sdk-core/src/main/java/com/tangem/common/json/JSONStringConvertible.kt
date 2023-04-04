package com.tangem.common.json

/**
 * Created by Anton Zhilenkov on 26/05/2021.
 */
interface JSONStringConvertible {
    fun toJson(): String = MoshiJsonConverter.INSTANCE.toJson(this)
}
