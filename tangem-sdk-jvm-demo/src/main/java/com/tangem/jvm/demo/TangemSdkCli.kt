package com.tangem.jvm.demo

import com.tangem.Log
import com.tangem.TangemSdk
import com.tangem.common.CompletionResult
import com.tangem.common.card.Card
import com.tangem.common.card.EllipticCurve
import com.tangem.common.core.CompletionCallback
import com.tangem.common.core.TangemError
import com.tangem.common.extensions.hexToBytes
import com.tangem.common.json.MoshiJsonConverter
import com.tangem.jvm.init
import com.tangem.operations.CommandResponse
import com.tangem.operations.files.FileToWrite
import com.tangem.operations.files.FileVisibility
import com.tangem.operations.sign.SignHashesCommand
import com.tangem.operations.sign.SignHashesResponse
import kotlinx.coroutines.runBlocking
import org.apache.commons.cli.CommandLine
import kotlin.coroutines.resume
import kotlin.coroutines.suspendCoroutine

class TangemSdkCli(verbose: Boolean = false, indexOfTerminal: Int? = null, private val cmd: CommandLine) {

    private val sdk = TangemSdk.init(verbose, indexOfTerminal)
    private val responseConverter = MoshiJsonConverter.INSTANCE

    private var card: Card? = null

    fun execute(command: Command) {
        if (sdk == null) {
            println("There's no NFC terminal to execute command.")
            return
        }

        runBlocking {
            val result: CompletionResult<*> = suspendCoroutine { continuation ->
                when (command) {
                    Command.Read -> read(sdk) { continuation.resume(it) }
                    Command.Sign -> sign(sdk) { continuation.resume(it) }
                    Command.ReadFiles -> readFiles(sdk) { continuation.resume(it) }
                    Command.WriteFiles -> writeFiles(sdk) { continuation.resume(it) }
                    Command.DeleteFiles -> deleteFiles(sdk) { continuation.resume(it) }
                    Command.CreateWallet -> createWallet(sdk) { continuation.resume(it) }
                    Command.PurgeWallet -> purgeWallet(sdk) { continuation.resume(it) }
                }
            }
            handleResult(result)
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
        if (cardId == null) {
            println("Missing option value")
            return
        }
        sdk.createWallet(EllipticCurve.Secp256k1, cardId, null, callback)
    }

    private fun purgeWallet(sdk: TangemSdk, callback: CompletionCallback<out CommandResponse>) {
        if (card == null) {
            println("Scan the card, before trying to use the method")
            return
        }

        val index: Int? = cmd.getOptionValue(TangemCommandOptions.WalletIndex.opt)?.toIntOrNull()
        val cardId: String? = cmd.getOptionValue(TangemCommandOptions.CardId.opt)
        if (index == null || cardId == null) {
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

    private fun readFiles(sdk: TangemSdk, callback: CompletionCallback<*>) {
        val cid: String? = cmd.getOptionValue(TangemCommandOptions.CardId.opt)
        val readPrivateFiles: Boolean = cmd.hasOption(TangemCommandOptions.ReadPrivateFiles.opt)
        val fileIndices: List<Int>? = cmd.getOptionValue(TangemCommandOptions.FileIndices.opt)
                ?.split(",")
                ?.mapNotNull { it.trim().toIntOrNull() }

        sdk.readFiles(
            readPrivateFiles = readPrivateFiles,
            cardId = cid,
            callback = callback
        )
    }

    private fun writeFiles(sdk: TangemSdk, callback: CompletionCallback<*>) {
        val cid: String? = cmd.getOptionValue(TangemCommandOptions.CardId.opt)
        val files: List<FileToWrite>? = cmd.getOptionValue(TangemCommandOptions.Files.opt)
                ?.split(",")
                ?.map { it.trim().hexToBytes() }
                ?.map { FileToWrite.ByUser(it, FileVisibility.Public, null) }

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

    private fun handleResult(result: CompletionResult<*>) {
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
    PurgeWallet("purgewallet");

    companion object {
        private val values = values()
        fun byValue(value: String): Command? = values.find { it.value == value }
    }
}