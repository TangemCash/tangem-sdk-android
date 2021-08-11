package com.tangem.jvm.demo

import com.tangem.Log
import com.tangem.TangemSdk
import com.tangem.common.CompletionResult
import com.tangem.common.card.Card
import com.tangem.common.card.EllipticCurve
import com.tangem.common.card.EncryptionMode
import com.tangem.common.core.*
import com.tangem.common.extensions.guard
import com.tangem.common.extensions.hexToBytes
import com.tangem.common.files.DataToWrite
import com.tangem.common.files.FileDataProtectedByPasscode
import com.tangem.common.hdwallet.DerivationPath
import com.tangem.common.json.MoshiJsonConverter
import com.tangem.jvm.init
import com.tangem.operations.CalculateSharedSecretCommand
import com.tangem.operations.CommandResponse
import com.tangem.operations.PreflightReadMode
import com.tangem.operations.pins.SetUserCodeCommand
import com.tangem.operations.read.ReadCommand
import com.tangem.operations.read.ReadWalletCommand
import com.tangem.operations.read.ReadWalletsListCommand
import com.tangem.operations.read.WalletPointer
import com.tangem.operations.sign.*
import com.tangem.operations.sign.SignHashCommand
import com.tangem.operations.wallet.CreateWalletCommand
import com.tangem.operations.wallet.PurgeWalletCommand
import kotlinx.coroutines.*
import org.apache.commons.cli.CommandLine
import java.io.PrintWriter
import java.io.StringWriter
import java.util.*
import kotlin.coroutines.resume
import kotlin.coroutines.suspendCoroutine

val scope = CoroutineScope(Dispatchers.IO) + CoroutineExceptionHandler { _, ex ->
    val sw = StringWriter()
    ex.printStackTrace(PrintWriter(sw))
    Log.error { sw.toString() }
}

class TangemSdkCli(
    verbose: Boolean = false,
    indexOfTerminal: Int? = null,
    private val cmd: CommandLine,
) {

    private val sdk = TangemSdk.init(verbose, indexOfTerminal)
    private val responseConverter = MoshiJsonConverter.INSTANCE

    private var card: Card? = null

    fun execute(command: Command) {
        if (sdk == null) {
            println("There's no NFC terminal to execute command.")
            return
        }

        runBlocking {
            val result: CompletionResult<out CommandResponse> = suspendCoroutine { continuation ->
                when (command) {
                    Command.Read -> read(sdk) { continuation.resume(it) }
                    Command.ReadWallets -> readWallets(sdk) { continuation.resume(it) }
                    Command.ReadWallet -> readWallet(sdk) { continuation.resume(it) }
                    Command.SetAccessCode -> setAccessCode(sdk) { continuation.resume(it) }
                    Command.SetPasscode -> setPasscode(sdk) { continuation.resume(it) }
                    Command.ResetCodes -> resetCodes(sdk) { continuation.resume(it) }
                    Command.Sign -> sign(sdk) { continuation.resume(it) }
                    Command.ReadFiles -> readFiles(sdk) { continuation.resume(it) }
                    Command.WriteFiles -> writeFiles(sdk) { continuation.resume(it) }
                    Command.DeleteFiles -> deleteFiles(sdk) { continuation.resume(it) }
                    Command.CreateWallet -> createWallet(sdk) { continuation.resume(it) }
                    Command.PurgeWallet -> purgeWallet(sdk) { continuation.resume(it) }
                    Command.CalculateSharedSecret -> calculateSharedSecret(sdk) {
                        continuation.resume(it)
                    }
                }
            }
            handleResult(result)
        }
    }

    private fun read(sdk: TangemSdk, callback: CompletionCallback<out CommandResponse>) {
        val encryptionMode = getEncryptionMode()
        sdk.startSessionWithRunnableAndEstablishEncryptionMode(
            ReadCommand(), encryption = encryptionMode
        ) {
            if (it is CompletionResult.Success) card = it.data.card
            callback(it)
        }
    }

    private fun readWallets(sdk: TangemSdk, callback: CompletionCallback<out CommandResponse>) {
        val encryptionMode = getEncryptionMode()
        sdk.startSessionWithRunnableAndEstablishEncryptionMode(
            ReadWalletsListCommand(), encryption = encryptionMode
        ) {
            if (it is CompletionResult.Success) card?.setWallets(it.data.wallets)
            callback(it)
        }
    }

    private fun readWallet(sdk: TangemSdk, callback: CompletionCallback<out CommandResponse>) {
        val encryptionMode = getEncryptionMode()
        sdk.startSessionWithRunnableAndEstablishEncryptionMode(
            ReadWalletCommand(
                getWalletPointer() ?: return
            ), encryption = encryptionMode
        ) {
            callback(it)
        }
    }

    private fun getEncryptionMode(): EncryptionMode {
        val encryptionParam = cmd.getOptionValue(TangemCommandOptions.Encryption.opt)
            ?.toLowerCase(Locale.ROOT)
        return when (encryptionParam) {
            "none" -> {
                EncryptionMode.None
            }
            "fast" -> {
                EncryptionMode.Fast
            }
            "strong" -> {
                EncryptionMode.Strong
            }
            else -> {
                EncryptionMode.Strong
            }
        }

    }

    private fun sign(sdk: TangemSdk, callback: CompletionCallback<SignHashesResponse>) {

        val hashes = cmd.getOptionValue(TangemCommandOptions.Hashes.opt)
        val cid: String? = cmd.getOptionValue(TangemCommandOptions.CardId.opt)

        if (hashes == null) {
            println("Missing option value")
            return
        }
        val encryptionMode = getEncryptionMode()

        val command = SignCommand(
            hashes = parseHashes(hashes),
            walletPointer = getWalletPointer() ?: return
        )
        sdk.startSessionWithRunnableAndEstablishEncryptionMode(
            runnable = command,
            encryption = encryptionMode,
            cardId = cid,
            initialMessage = null,
            callback = callback
        )
    }

    private fun getWalletPointer(): WalletPointer? {
        val wallet: String? = cmd.getOptionValue(TangemCommandOptions.Wallet.opt)
        val hdPathString: String? = cmd.getOptionValue(TangemCommandOptions.WalletHdPath.opt)
        val tweak: ByteArray? =
            cmd.getOptionValue(TangemCommandOptions.WalletTweak.opt)?.hexToBytes()

        val hdPath = if (hdPathString != null) {
            try {
                DerivationPath(hdPathString)
            } catch (error: TangemSdkError) {
                println(error.customMessage)
                return null
            }
        } else {
            null
        }

        if (wallet == null) {
            println("Please specify the wallet by its index or public key (-wallet [index/public key])")
            return null
        }

        return if (wallet.length <= 2) {
            val index = wallet.toIntOrNull().guard {
                println("Wrong wallet index format")
                return null
            }
            WalletPointer.WalletIndex(index, hdPath, tweak)
        } else {
            val publicKey = wallet.hexToBytes()
            WalletPointer.WalletPublicKey(publicKey, hdPath, tweak)
        }
    }

    private fun getWalletPublicKey(index: Int): ByteArray? {
        val card = card ?: return null
        if (index < 0 || index >= card.wallets.size) return null

        return card.wallets[index].publicKey
    }

    private fun parseHashes(hashesArgument: String): Array<ByteArray> {
        return hashesArgument
            .split(",")
            .map { hash -> hash.trim().hexToBytes() }
            .toTypedArray()
    }

    private fun calculateSharedSecret(
        sdk: TangemSdk, callback: CompletionCallback<out CommandResponse>,
    ) {
        val cid: String? = cmd.getOptionValue(TangemCommandOptions.CardId.opt)
        val sessionKeyA: ByteArray = cmd.getOptionValue(TangemCommandOptions.SessionKeyA.opt)
            ?.hexToBytes().guard {
                println("Missing option value")
                return
            }

        val encryptionMode = getEncryptionMode()
        sdk.startSessionWithRunnableAndEstablishEncryptionMode(
            runnable = CalculateSharedSecretCommand(
                sessionKeyA = sessionKeyA,
                walletPointer = getWalletPointer() ?: return
            ),
            encryption = encryptionMode,
            cardId = cid
        ) {
            callback(it)
        }
    }


    private fun setPasscode(sdk: TangemSdk, callback: CompletionCallback<out CommandResponse>) {
        val encryptionMode = getEncryptionMode()
        sdk.startSessionWithRunnableAndEstablishEncryptionMode(
            SetUserCodeCommand.changePasscode(null), encryption = encryptionMode
        ) {
            callback(it)
        }
    }

    private fun setAccessCode(sdk: TangemSdk, callback: CompletionCallback<out CommandResponse>) {
        val encryptionMode = getEncryptionMode()
        sdk.startSessionWithRunnableAndEstablishEncryptionMode(
            SetUserCodeCommand.changeAccessCode(null), encryption = encryptionMode
        ) {
            callback(it)
        }
    }

    private fun resetCodes(sdk: TangemSdk, callback: CompletionCallback<out CommandResponse>) {
        val encryptionMode = getEncryptionMode()
        sdk.startSessionWithRunnableAndEstablishEncryptionMode(
            SetUserCodeCommand.resetUserCodes(), encryption = encryptionMode
        ) {
            callback(it)
        }
    }


    private fun createWallet(sdk: TangemSdk, callback: CompletionCallback<out CommandResponse>) {
        val cardId: String? = cmd.getOptionValue(TangemCommandOptions.CardId.opt)
        val passphrase: String? = cmd.getOptionValue(TangemCommandOptions.Passphrase.opt)
        val curveString: String? = cmd.getOptionValue(TangemCommandOptions.Curve.opt)

        val curve = when {
            curveString?.startsWith("secp256r1") == true -> {
                EllipticCurve.Secp256r1
            }
            curveString?.startsWith("secp256k1") == true -> {
                EllipticCurve.Secp256k1
            }
            curveString?.startsWith("secp") == true -> {
                EllipticCurve.Secp256k1
            }
            curveString?.startsWith("ed") == true -> {
                EllipticCurve.Ed25519
            }
            else -> {
                EllipticCurve.Secp256k1
            }
        }

        sdk.startSessionWithRunnableAndEstablishEncryptionMode(
            CreateWalletCommand(
                curve = curve,
                isPermanent = false,
                passphrase = passphrase,
            ),
            encryption = getEncryptionMode(),
            cardId = cardId,
            initialMessage = null,
            callback = callback
        )
    }

    private fun purgeWallet(sdk: TangemSdk, callback: CompletionCallback<out CommandResponse>) {
        if (card == null) {
            println("Scan the card, before trying to use the method")
            return
        }

        val cardId: String? = cmd.getOptionValue(TangemCommandOptions.CardId.opt)

        sdk.startSessionWithRunnableAndEstablishEncryptionMode(
            PurgeWalletCommand(getWalletPointer() ?: return),
            getEncryptionMode(),
            cardId,
            null,
            callback
        )

    }

    private fun readFiles(sdk: TangemSdk, callback: CompletionCallback<out CommandResponse>) {
        val cid: String? = cmd.getOptionValue(TangemCommandOptions.CardId.opt)
        val readPrivateFiles: Boolean = cmd.hasOption(TangemCommandOptions.ReadPrivateFiles.opt)
        val fileIndices: List<Int>? = cmd.getOptionValue(TangemCommandOptions.FileIndices.opt)
            ?.split(",")
            ?.mapNotNull { it.trim().toIntOrNull() }

        sdk.readFiles(
            readPrivateFiles = readPrivateFiles, indices = fileIndices, cardId = cid,
            callback = callback
        )
    }

    private fun writeFiles(sdk: TangemSdk, callback: CompletionCallback<out CommandResponse>) {
        val cid: String? = cmd.getOptionValue(TangemCommandOptions.CardId.opt)
        val files: List<DataToWrite>? = cmd.getOptionValue(TangemCommandOptions.Files.opt)
            ?.split(",")
            ?.map { it.trim().hexToBytes() }
            ?.map { FileDataProtectedByPasscode(it) }

        if (files == null) {
            println("Missing option value")
            return
        }

        sdk.writeFiles(files = files, cardId = cid, callback = callback)
    }


    private fun deleteFiles(sdk: TangemSdk, callback: CompletionCallback<out CommandResponse>) {
        val cid: String? = cmd.getOptionValue(TangemCommandOptions.CardId.opt)
        val fileIndices: List<Int>? = cmd.getOptionValue(TangemCommandOptions.FileIndices.opt)
            ?.split(",")
            ?.mapNotNull { it.trim().toIntOrNull() }

        sdk.deleteFiles(indices = fileIndices, cardId = cid, callback = callback)
    }

    private fun handleResult(result: CompletionResult<out CommandResponse>) {
        when (result) {
            is CompletionResult.Success -> {
                println(responseConverter.toJson(result.data))
            }
            is CompletionResult.Failure -> handleError(result.error)
        }
        Log.command(this) { "completed" }
    }

    private fun handleError(error: TangemError) {
        println("Task error: ${error.code}, ${error.javaClass.simpleName}")
    }
}


enum class Command(val value: String) {
    Read("read"),
    ReadWallets("readwallets"),
    ReadWallet("readwallet"),
    CalculateSharedSecret("secret"),
    SetPasscode("setpasscode"),
    SetAccessCode("setaccesscode"),
    ResetCodes("resetcodes"),
    Sign("sign"),
    ReadFiles("readfiles"),
    WriteFiles("writefiles"),
    DeleteFiles("deletefiles"),
    CreateWallet("createwallet"),
    PurgeWallet("purgewallet");

    companion object {
        private val values = values()
        fun byValue(value: String): Command? = values.find { it.value == value }
    }
}