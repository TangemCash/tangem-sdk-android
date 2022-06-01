package com.tangem.common.services

interface Storage {
    fun putString(key: String, value: String)
    fun getString(key: String): String?
}
