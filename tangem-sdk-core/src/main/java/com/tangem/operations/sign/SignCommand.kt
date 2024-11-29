package com.tangem.operations.sign

import com.squareup.moshi.JsonClass
import com.tangem.LocatorMessage
import com.tangem.Log
import com.tangem.common.CompletionResult
import com.tangem.common.KeyPair
import com.tangem.common.StringsLocator
import com.tangem.common.apdu.CommandApdu
import com.tangem.common.apdu.Instruction
import com.tangem.common.apdu.ResponseApdu
import com.tangem.common.card.Card
import com.tangem.common.card.EllipticCurve
import com.tangem.common.card.FirmwareVersion
import com.tangem.common.card.SigningMethod
import com.tangem.common.core.CardSession
import com.tangem.common.core.CompletionCallback
import com.tangem.common.core.SessionEnvironment
import com.tangem.common.core.TangemSdkError
import com.tangem.common.extensions.guard
import com.tangem.common.extensions.toByteArray
import com.tangem.common.tlv.TlvBuilder
import com.tangem.common.tlv.TlvDecoder
import com.tangem.common.tlv.TlvTag
import com.tangem.crypto.CryptoUtils
import com.tangem.crypto.hdWallet.DerivationPath
import com.tangem.crypto.sign
import com.tangem.operations.Command
import com.tangem.operations.CommandResponse

/**
 * @property cardId CID, Unique Tangem card ID number
 * @property signatures Signed hashes (array of resulting signatures)
 * @property totalSignedHashes Total number of signed  hashes returned by the wallet since its creation. COS: 1.16+
 */
@JsonClass(generateAdapter = true)
open class SignResponse(
    open val cardId: String,
    val signatures: List<ByteArray>,
    open val totalSignedHashes: Int?,
) : CommandResponse {

    /**
     * Model to store chunked sign response.
     * @property cardId CID, Unique Tangem card ID number
     * @property signedChunk Signed hashes of given chunk
     * @property totalSignedHashes Total number of signed  hashes returned by the wallet since its creation. COS: 1.16+
     */
    @JsonClass(generateAdapter = true)
    data class PartialSignResponse(
        override val cardId: String,
        val signedChunk: SignedChunk,
        override val totalSignedHashes: Int?,
    ) : SignResponse(
        cardId = cardId,
        signatures = emptyList(),
        totalSignedHashes = totalSignedHashes,
    )
}

/**
 * Signs transaction hashes using a wallet private key, stored on the card.
 * @property hashes Array of transaction hashes.
 * @property walletPublicKey Public key of the wallet, using for sign.
 * @property derivationPath: Derivation path of the wallet. Optional. COS v. 4.28 and higher,
 */
internal class SignCommand(
    private val hashes: Array<ByteArray>,
    private val walletPublicKey: ByteArray,
    private val derivationPath: DerivationPath? = null,
) : Command<SignResponse>() {

    private var terminalKeys: KeyPair? = null

    private val chunkHashesHelper: ChunkedHashesContainer = ChunkedHashesContainer(hashes)
    private var environment: SessionEnvironment? = null

    override fun requiresPasscode(): Boolean = true

    override fun performPreCheck(card: Card): TangemSdkError? {
        val wallet = card.wallet(walletPublicKey) ?: return TangemSdkError.WalletNotFound()

        if (derivationPath != null) {
            if (card.firmwareVersion < FirmwareVersion.HDWalletAvailable) {
                return TangemSdkError.NotSupportedFirmwareVersion()
            }
            if (wallet.curve == EllipticCurve.Secp256r1) {
                return TangemSdkError.UnsupportedCurve()
            }
            if (!card.settings.isHDWalletAllowed) {
                return TangemSdkError.HDWalletDisabled()
            }
        }

        // Before v4
        if (wallet.remainingSignatures == 0) {
            return TangemSdkError.NoRemainingSignatures()
        }
        if (card.settings.defaultSigningMethods?.contains(SigningMethod.Code.SignHash) == false) {
            return TangemSdkError.SignHashesNotAvailable()
        }

        return null
    }

    override fun run(session: CardSession, callback: CompletionCallback<SignResponse>) {
        Log.command(this)
        val card = session.environment.card.guard {
            callback(CompletionResult.Failure(TangemSdkError.MissingPreflightRead()))
            return
        }
        if (chunkHashesHelper.isEmpty) {
            callback(CompletionResult.Failure(TangemSdkError.EmptyHashes()))
            return
        }
        terminalKeys = retrieveTerminalKeys(card, session.environment)

        sign(session, callback)
    }

    private fun sign(session: CardSession, callback: CompletionCallback<SignResponse>) {
        environment = session.environment
        if (chunkHashesHelper.chunksCount > 1) setSignedChunksMessage(session)

        transceive(session) { result ->
            when (result) {
                is CompletionResult.Success -> {
                    val response = result.data as SignResponse.PartialSignResponse
                    chunkHashesHelper.addSignedChunk(response.signedChunk)

                    if (chunkHashesHelper.currentChunkIndex >= chunkHashesHelper.chunksCount) {
                        val signatures = processSignatures(session.environment)
                        session.environment.card?.wallet(walletPublicKey)?.let {
                            val wallet = it.copy(
                                totalSignedHashes = result.data.totalSignedHashes,
                                remainingSignatures = it.remainingSignatures?.minus(signatures.size),
                            )
                            session.environment.card = session.environment.card?.updateWallet(wallet)
                        }

                        val finalResponse = SignResponse(
                            cardId = result.data.cardId,
                            signatures = signatures,
                            totalSignedHashes = result.data.totalSignedHashes,
                        )
                        callback(CompletionResult.Success(finalResponse))
                    } else {
                        sign(session, callback)
                    }
                }

                is CompletionResult.Failure -> callback(result)
            }
        }
    }

    private fun setSignedChunksMessage(session: CardSession) {
        val message = LocatorMessage(
            headerSource = LocatorMessage.Source(
                id = StringsLocator.ID.SIGN_MULTIPLE_CHUNKS_PART,
                formatArgs = arrayOf(chunkHashesHelper.currentChunkIndex + 1, chunkHashesHelper.chunksCount),
            ),
            bodySource = null,
        )
        session.setMessage(message)
    }

    /**
     * Application can optionally submit a public key Terminal_PublicKey in [SignCommand].
     * Submitted key is stored by the Tangem card if it differs from a previous submitted Terminal_PublicKey.
     * The Tangem card will not enforce security delay if [SignCommand] will be called with
     * TerminalTransactionSignature parameter containing a correct signature of raw data to be signed made with
     * TerminalPrivateKey (this key should be generated and security stored by the application).
     */
    @Suppress("MagicNumber")
    override fun serialize(environment: SessionEnvironment): CommandApdu {
        val walletIndex = environment.card?.wallet(walletPublicKey)?.index ?: throw TangemSdkError.WalletNotFound()

        val chunk = chunkHashesHelper.getCurrentChunk()
        val hashSize = chunk.hashSize
        val hashSizeData = if (hashSize > 255) hashSize.toByteArray(2) else hashSize.toByteArray(1)
        val flattedHashes = chunk.hashes.map { it.data }.reduce { arr1, arr2 -> arr1 + arr2 }

        val tlvBuilder = TlvBuilder()
        tlvBuilder.append(TlvTag.Pin, environment.accessCode.value)
        tlvBuilder.append(TlvTag.Pin2, environment.passcode.value)
        tlvBuilder.append(TlvTag.CardId, environment.card?.cardId)
        tlvBuilder.append(TlvTag.TransactionOutHashSize, hashSizeData)
        tlvBuilder.append(TlvTag.TransactionOutHash, flattedHashes)
        tlvBuilder.append(TlvTag.Cvc, environment.cvc)
        // Wallet index works only on COS v. 4.0 and higher. For previous version index will be ignored
        tlvBuilder.append(TlvTag.WalletIndex, walletIndex)

        terminalKeys?.let {
            val signedData = flattedHashes.sign(it.privateKey)
            tlvBuilder.append(TlvTag.TerminalTransactionSignature, signedData)
            tlvBuilder.append(TlvTag.TerminalPublicKey, it.publicKey)
        }
        tlvBuilder.append(TlvTag.WalletHDPath, derivationPath)

        return CommandApdu(Instruction.Sign, tlvBuilder.serialize())
    }

    override fun deserialize(environment: SessionEnvironment, apdu: ResponseApdu): SignResponse.PartialSignResponse {
        val tlvData = deserializeApdu(environment, apdu)

        val decoder = TlvDecoder(tlvData)

        val chunk = chunkHashesHelper.getCurrentChunk()
        val signatureBLOB: ByteArray = decoder.decode(TlvTag.WalletSignature)
        val signatures = splitSignatureBLOB(signatureBLOB, chunk.hashes.size)

        val signedHashes = chunk.hashes.zip(signatures).map { (hash, signature) ->
            SignedHash(
                index = hash.index,
                data = hash.data,
                signature = signature,
            )
        }

        val signedChunk = SignedChunk(signedHashes)

        return SignResponse.PartialSignResponse(
            cardId = decoder.decode(TlvTag.CardId),
            signedChunk = signedChunk,
            totalSignedHashes = decoder.decode(TlvTag.WalletSignedHashes),
        )
    }

    private fun processSignatures(environment: SessionEnvironment): List<ByteArray> {
        val signatures = chunkHashesHelper.getSignatures()

        if (!environment.config.canonizeSecp256k1Signatures) return signatures
        val wallet = environment.card?.wallet(walletPublicKey) ?: return signatures
        if (wallet.curve != EllipticCurve.Secp256k1) return signatures

        return signatures.map { CryptoUtils.normalize(it) }
    }

    private fun splitSignatureBLOB(signature: ByteArray, numberOfSignatures: Int): List<ByteArray> {
        val signatures = mutableListOf<ByteArray>()
        val signatureSize = signature.size / numberOfSignatures
        for (index in 0 until numberOfSignatures) {
            val offsetMin = index * signatureSize
            val offsetMax = offsetMin + signatureSize
            val sig = signature.copyOfRange(offsetMin, offsetMax)
            signatures.add(sig)
        }

        return signatures
    }

    private fun retrieveTerminalKeys(card: Card, environment: SessionEnvironment): KeyPair? {
        if (!card.settings.isLinkedTerminalEnabled || card.firmwareVersion >= FirmwareVersion.HDWalletAvailable) {
            return null
        }

        return environment.terminalKeys
    }
}
