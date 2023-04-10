package com.tangem.crypto.hdWallet.bip32.serialization

sealed class ExtendedKeySerializationError : Exception() {
    object WrongLength : ExtendedKeySerializationError()
    object DecodingFailed : ExtendedKeySerializationError()
    object WrongVersion : ExtendedKeySerializationError()
    object WrongKey : ExtendedKeySerializationError()

    override fun toString(): String = this::class.java.simpleName
}
