package com.tangem.common.hdWallet

/**
 * Created by Anton Zhilenkov on 03/08/2021.
 */
sealed class HDWalletError : Exception() {
    object HardenedNotSupported : HDWalletError()
    object DerivationFailed : HDWalletError()
    object WrongPath : HDWalletError()
    object WrongIndex : HDWalletError()
    object UnsupportedCurve : HDWalletError()

    override fun toString(): String = this::class.java.simpleName
}
