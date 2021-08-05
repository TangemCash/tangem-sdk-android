package com.tangem.common.files

import com.tangem.common.card.FirmwareVersion
import com.tangem.common.core.SessionEnvironment
import com.tangem.common.extensions.calculateSha256
import com.tangem.common.extensions.maxFirmwareVersion
import com.tangem.common.extensions.minFirmwareVersion
import com.tangem.common.tlv.TlvBuilder
import com.tangem.common.tlv.TlvTag
import com.tangem.operations.files.settings.FileWriteSettings
import com.tangem.operations.files.settings.FirmwareRestrictable

interface DataToWrite : FirmwareRestrictable {
    val data: ByteArray
    val requiredPasscode: Boolean
    fun addStartingTlvData(tlvBuilder: TlvBuilder, environment: SessionEnvironment): TlvBuilder
    fun addFinalizingTlvData(tlvBuilder: TlvBuilder, environment: SessionEnvironment): TlvBuilder
}

/**
 *  Use this type when protecting data with issuer data signature.
 *  Note: To generate starting and finalizing signatures use [FileHashHelper]
 */
class FileDataProtectedBySignature(
    override val data: ByteArray,
    internal val startingSignature: ByteArray,
    internal val finalizingSignature: ByteArray,
    internal val counter: Int,
    internal val issuerPublicKey: ByteArray?,
) : DataToWrite {

    override val minFirmwareVersion: FirmwareVersion
        get() = settings.minFirmwareVersion()
    override val maxFirmwareVersion: FirmwareVersion
        get() = settings.maxFirmwareVersion()

    override val requiredPasscode: Boolean = false

    private val settings: Set<FileWriteSettings> = setOf(FileWriteSettings.None)

    override fun addStartingTlvData(tlvBuilder: TlvBuilder, environment: SessionEnvironment): TlvBuilder {
        return tlvBuilder.apply {
            append(TlvTag.IssuerDataSignature, startingSignature)
            append(TlvTag.IssuerDataCounter, counter)
        }
    }

    override fun addFinalizingTlvData(tlvBuilder: TlvBuilder, environment: SessionEnvironment): TlvBuilder {
        return tlvBuilder.apply { append(TlvTag.IssuerDataSignature, finalizingSignature) }
    }

}

/**
 * Use this type when protecting data with passcode
 */
class FileDataProtectedByPasscode(
    override val data: ByteArray
) : DataToWrite {

    override val minFirmwareVersion: FirmwareVersion
        get() = settings.minFirmwareVersion()
    override val maxFirmwareVersion: FirmwareVersion
        get() = settings.maxFirmwareVersion()

    override val requiredPasscode: Boolean = true

    private val settings: Set<FileWriteSettings> = setOf(FileWriteSettings.VerifiedWithPasscode)

    override fun addStartingTlvData(tlvBuilder: TlvBuilder, environment: SessionEnvironment): TlvBuilder {
        return tlvBuilder.apply { append(TlvTag.Pin2, environment.passcode.value) }
    }

    override fun addFinalizingTlvData(tlvBuilder: TlvBuilder, environment: SessionEnvironment): TlvBuilder {
        return tlvBuilder.apply {
            append(TlvTag.CodeHash, data.calculateSha256())
            append(TlvTag.Pin2, environment.passcode.value)
        }

    }
}