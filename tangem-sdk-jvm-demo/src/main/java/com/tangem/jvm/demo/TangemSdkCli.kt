package com.tangem.jvm.demo

import com.tangem.Log
import com.tangem.TangemSdk
import com.tangem.common.CompletionResult
import com.tangem.common.UserCode
import com.tangem.common.backup.BackupSession
import com.tangem.common.backup.ResetPinSession
import com.tangem.common.card.Card
import com.tangem.common.card.EllipticCurve
import com.tangem.common.card.FirmwareVersion
import com.tangem.common.core.CompletionCallback
import com.tangem.common.core.TangemError
import com.tangem.common.core.TangemSdkError
import com.tangem.common.extensions.calculateSha256
import com.tangem.common.extensions.hexToBytes
import com.tangem.common.files.DataToWrite
import com.tangem.common.files.FileDataProtectedByPasscode
import com.tangem.common.json.MoshiJsonConverter
import com.tangem.jvm.init
import com.tangem.operations.CommandResponse
import com.tangem.operations.sign.SignHashesCommand
import com.tangem.operations.sign.SignHashesResponse
import kotlinx.coroutines.runBlocking
import org.apache.commons.cli.CommandLine
import java.nio.file.Files
import java.nio.file.Path
import java.nio.file.Paths
import kotlin.coroutines.resume
import kotlin.coroutines.suspendCoroutine

class TangemSdkCli(verbose: Boolean = false, indexOfTerminal: Int? = null, private val cmd: CommandLine) {

    val sdk = TangemSdk.init(verbose, indexOfTerminal)
    private val responseConverter = MoshiJsonConverter.INSTANCE

    private var card: Card? = null

    private val backupSessionFileName = "./backupSession.json"
    private val backupSessionFilePath: Path = Paths.get(backupSessionFileName)
    private val resetPinSessionFileName = "./resetPinSession.json"
    private val resetPinSessionFilePath: Path = Paths.get(resetPinSessionFileName)

    private var backupSession: BackupSession? = null
    private var resetPinSession: ResetPinSession? = null

    fun execute(command: Command) {
        if (sdk == null) {
            println("There's no NFC terminal to execute command.")
            return
        }
        sdk.config.filter.allowedCardTypes = listOf(FirmwareVersion.FirmwareType.Release, FirmwareVersion.FirmwareType.Sdk)

        if (backupSessionFilePath.toFile().exists()) {
            backupSession = responseConverter.fromJson<BackupSession>(String(Files.readAllBytes(backupSessionFilePath), Charsets.UTF_8))
        }
        if (resetPinSessionFilePath.toFile().exists()) {
            resetPinSession = responseConverter.fromJson<ResetPinSession>(String(Files.readAllBytes(resetPinSessionFilePath), Charsets.UTF_8))
        }

        runBlocking {
            val result: CompletionResult<out CommandResponse> = suspendCoroutine { continuation ->
                when (command) {
                    Command.Read -> read(sdk) { continuation.resume(it) }
                    Command.Sign -> sign(sdk) { continuation.resume(it) }
                    Command.ReadFiles -> readFiles(sdk) { continuation.resume(it) }
                    Command.WriteFiles -> writeFiles(sdk) { continuation.resume(it) }
                    Command.DeleteFiles -> deleteFiles(sdk) { continuation.resume(it) }
                    Command.CreateWallet -> createWallet(sdk) { continuation.resume(it) }
                    Command.PurgeWallet -> purgeWallet(sdk) { continuation.resume(it) }
                    Command.BackupGetMasterKey -> backupGetMasterKey(sdk) { continuation.resume(it) }
                    Command.BackupGetSlaveKey -> backupGetSlaveKey(sdk) { continuation.resume(it) }
                    Command.BackupLinkSlaveCards -> backupLinkSlaveCards(sdk) { continuation.resume(it) }
                    Command.BackupReadData -> backupReadData(sdk) { continuation.resume(it) }
                    Command.BackupLinkMasterCard -> backupLinkMasterCard(sdk) { continuation.resume(it) }
                    Command.BackupWriteData -> backupWriteData(sdk) { continuation.resume(it) }
                    Command.BackupReset -> backupReset(sdk) { continuation.resume(it) }
                    Command.ResetPinGetToken -> resetPinGetToken(sdk) { continuation.resume(it) }
                    Command.ResetPinSignToken -> resetPinSignToken(sdk) { continuation.resume(it) }
                    Command.ResetPinAuthorizeAndSetNew -> resetPinAuthorizeTokenAndSetNew(sdk) { continuation.resume(it) }
                }
            }
            handleResult(result)
            if (backupSession != null) {
                Files.write(Paths.get(backupSessionFileName), responseConverter.toJson(backupSession).toByteArray(Charsets.UTF_8))
            }
            if (resetPinSession != null) {
                Files.write(Paths.get(resetPinSessionFileName), responseConverter.toJson(resetPinSession).toByteArray(Charsets.UTF_8))
            }
        }
    }

    private fun read(sdk: TangemSdk, callback: CompletionCallback<out CommandResponse>) {
        sdk.scanCard {
            if (it is CompletionResult.Success) card = it.data
            callback(it)
        }
    }

    private fun sign(sdk: TangemSdk, callback: CompletionCallback<SignHashesResponse>) {
        if (card == null) {
            println("Scan the card, before trying to use the method")
            return
        }

        val hashes = cmd.getOptionValue(TangemCommandOptions.Hashes.opt)
        val index: Int? = cmd.getOptionValue(TangemCommandOptions.WalletIndex.opt)?.toIntOrNull()
        val cid: String? = cmd.getOptionValue(TangemCommandOptions.CardId.opt)

        if (hashes == null || index == null) {
            println("Missing option value")
            return
        }

        val publicKey = getWalletPublicKey(index)
        if (publicKey == null) {
            println("Wallet for the index: $index not found")
            return
        }

        val command = SignHashesCommand(parseHashes(hashes), publicKey)
        sdk.startSessionWithRunnable(command, cid, null, callback)
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

    private fun createWallet(sdk: TangemSdk, callback: CompletionCallback<out CommandResponse>) {
        val cardId: String? = cmd.getOptionValue(TangemCommandOptions.CardId.opt)

        sdk.createWallet(EllipticCurve.Secp256k1, false, cardId, null, callback)
    }

    val sdkIssuerPrivateKey = "11121314151617184771ED81F2BACF57479E4735EB1405083927372D40DA9E92".hexToBytes()

    private fun backupGetMasterKey(sdk: TangemSdk, callback: CompletionCallback<out CommandResponse>) {
        val cardId: String? = cmd.getOptionValue(TangemCommandOptions.CardId.opt)

        sdk.backupGetMasterKey(cardId, null) {
            if (it is CompletionResult.Success) {
                it.data.master.certificate = sdk.getCardCertificate(it.data.master.cardKey, sdkIssuerPrivateKey)
                backupSession = BackupSession(it.data.master)
            }
            callback(it)
        }
    }

    private fun backupGetSlaveKey(sdk: TangemSdk, callback: CompletionCallback<out CommandResponse>) {
        val cardId: String? = cmd.getOptionValue(TangemCommandOptions.CardId.opt)
        if (backupSession == null) throw TangemSdkError.BackupNoMaster()
        backupSession?.let { backupSession ->
            sdk.backupGetSlaveKey(cardId, backupSession, null) {
                if (it is CompletionResult.Success) {
                    it.data.slave.certificate = sdk.getCardCertificate(it.data.slave.cardKey, sdkIssuerPrivateKey)
                    backupSession.slaves[it.data.cardId] = it.data.slave
                }
                callback(it)
            }
        }
    }

    private fun backupLinkSlaveCards(sdk: TangemSdk, callback: CompletionCallback<out CommandResponse>) {
        val cardId: String? = cmd.getOptionValue(TangemCommandOptions.CardId.opt)
        if (backupSession == null) throw TangemSdkError.BackupNoMaster()
        backupSession!!.newPIN = "123456".calculateSha256()
        backupSession!!.newPIN2 = "1234".calculateSha256()
        backupSession?.let { backupSession ->
            sdk.backupLinkSlaveCards(cardId, backupSession, null) {
                if (it is CompletionResult.Success) {
                    backupSession.attestSignature = it.data.attestSignature
                }
                callback(it)
            }
        }
    }

    private fun backupReadData(sdk: TangemSdk, callback: CompletionCallback<out CommandResponse>) {
        val cardId: String? = cmd.getOptionValue(TangemCommandOptions.CardId.opt)
        if (backupSession == null) throw TangemSdkError.BackupNoMaster()
        backupSession?.let { backupSession ->
            sdk.backupReadAllData(cardId, backupSession, null) {
                if (it is CompletionResult.Success) {
                    backupSession.slaves = it.data.slaves
                }
                callback(it)
            }
        }
    }

    private fun backupLinkMasterCard(sdk: TangemSdk, callback: CompletionCallback<out CommandResponse>) {
        val cardId: String? = cmd.getOptionValue(TangemCommandOptions.CardId.opt)
        if (backupSession == null) throw TangemSdkError.BackupNoMaster()
        backupSession?.let { backupSession ->
            sdk.backupLinkMasterCard(cardId, backupSession, null) {
                if (it is CompletionResult.Success) {
                    backupSession.slaves[it.data.cardId]!!.state = it.data.state
                }
                callback(it)
            }
        }
    }

    private fun backupWriteData(sdk: TangemSdk, callback: CompletionCallback<out CommandResponse>) {
        val cardId: String? = cmd.getOptionValue(TangemCommandOptions.CardId.opt)

        if (backupSession == null) throw TangemSdkError.BackupNoMaster()
        backupSession?.let { backupSession ->
            sdk.backupWriteData(cardId, backupSession, null) {
                if (it is CompletionResult.Success) {
                    backupSession.slaves[it.data.cardId]!!.state = it.data.state
                }
                callback(it)
            }
        }
    }

    private fun backupReset(sdk: TangemSdk, callback: CompletionCallback<out CommandResponse>) {
        val cardId: String? = cmd.getOptionValue(TangemCommandOptions.CardId.opt)
        sdk.backupReset(cardId, null) {
            callback(it)
        }
    }

    private fun resetPinGetToken(sdk: TangemSdk, callback: CompletionCallback<out CommandResponse>) {
        val cardId: String? = cmd.getOptionValue(TangemCommandOptions.CardId.opt)

        sdk.resetPinGetToken(cardId, null) {
            if (it is CompletionResult.Success) {
                resetPinSession = ResetPinSession(cardToReset = it.data.cardToReset)
            }
            callback(it)
        }
    }

    private fun resetPinSignToken(sdk: TangemSdk, callback: CompletionCallback<out CommandResponse>) {
        val cardId: String? = cmd.getOptionValue(TangemCommandOptions.CardId.opt)

        if (resetPinSession == null) throw TangemSdkError.ResetPinNoCardToReset()
        resetPinSession?.let { resetPinSession ->
            sdk.resetPinSignToken(cardId, resetPinSession, null) {
                if (it is CompletionResult.Success) {
                    resetPinSession.cardToConfirm = it.data.cartToConfirm
                }
                callback(it)
            }
        }
    }

    private fun resetPinAuthorizeTokenAndSetNew(sdk: TangemSdk, callback: CompletionCallback<out CommandResponse>) {
        val cardId: String? = cmd.getOptionValue(TangemCommandOptions.CardId.opt)

        if (resetPinSession == null) throw TangemSdkError.ResetPinNoCardToReset()
        resetPinSession?.let { resetPinSession ->
            resetPinSession.newPIN = "111111".calculateSha256()
            resetPinSession.newPIN2 = "111".calculateSha256()
            sdk.resetPinAuthorizeTokenAndSetNewPin(cardId, resetPinSession, null) {
                callback(it)
            }
        }
    }


    private fun purgeWallet(sdk: TangemSdk, callback: CompletionCallback<out CommandResponse>) {
        if (card == null) {
            println("Scan the card, before trying to use the method")
            return
        }

        val index: Int? = cmd.getOptionValue(TangemCommandOptions.WalletIndex.opt)?.toIntOrNull()
        val cardId: String? = cmd.getOptionValue(TangemCommandOptions.CardId.opt)
        if (index == null) {
            println("Missing option value")
            return
        }

        val publicKey = getWalletPublicKey(index)
        if (publicKey == null) {
            println("Wallet for the index: $index not found")
            return
        }

        sdk.purgeWallet(publicKey, cardId, null, callback)
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
    Sign("sign"),
    ReadFiles("readfiles"),
    WriteFiles("writefiles"),
    DeleteFiles("deletefiles"),
    CreateWallet("createwallet"),
    PurgeWallet("purgewallet"),
    BackupGetMasterKey("backup-get-master-key"),
    BackupGetSlaveKey("backup-get-slave-key"),
    BackupLinkSlaveCards("backup-link-slave-cards"),
    BackupReadData("backup-read-data"),
    BackupLinkMasterCard("backup-link-master-card"),
    BackupWriteData("backup-write-data"),
    BackupReset("backup-reset"),
    ResetPinGetToken("reset-pin-get-token"),
    ResetPinSignToken("reset-pin-sign-token"),
    ResetPinAuthorizeAndSetNew("reset-pin");

    companion object {
        private val values = values()
        fun byValue(value: String): Command? = values.find { it.value == value }
    }
}