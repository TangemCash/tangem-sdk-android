package com.tangem.operations.files

import com.squareup.moshi.JsonClass
import com.tangem.common.CompletionResult
import com.tangem.common.core.CardSession
import com.tangem.common.core.CardSessionRunnable
import com.tangem.common.core.CompletionCallback
import com.tangem.common.core.TangemSdkError
import com.tangem.common.extensions.guard
import com.tangem.common.files.DataToWrite
import com.tangem.operations.CommandResponse

/**
 * Response for [WriteFilesTask].
 * @property cardId: CID, Unique Tangem card ID number
 * @property fileIndices: Indicies of created files
 */
@JsonClass(generateAdapter = true)
class WriteFilesResponse(
    val cardId: String,
    val fileIndices: List<Int>
) : CommandResponse

/**
 * This task allows to write multiple files to a card.
 * There are two secure ways to write files.
 * 1) Data can be signed by Issuer (the one specified on card during personalization) -
 * [FileDataProtectedBySignature].
 * 2) Data can be protected by Passcode (PIN2). [FileDataProtectedByPasscode] In this case,
 * Passcode (PIN2) is required for the command.
 */
class WriteFilesTask(
    private val data: List<DataToWrite>,
    private val overwriteAllFiles: Boolean = false
) : CardSessionRunnable<WriteFilesResponse> {

    private var currentIndex = 0
    private val savedFilesIndices = mutableListOf<Int>()

    override fun run(session: CardSession, callback: CompletionCallback<WriteFilesResponse>) {
        if (data.isEmpty()) {
            callback(CompletionResult.Success(WriteFilesResponse("", listOf())))
            return
        }
        if (overwriteAllFiles) {
            deleteFiles(session, callback)
        } else {
            writeFiles(session, callback)
        }
    }

    private fun deleteFiles(session: CardSession, callback: CompletionCallback<WriteFilesResponse>) {
        DeleteFilesTask().run(session) { result ->
            when (result) {
                is CompletionResult.Success -> writeFiles(session, callback)
                is CompletionResult.Failure -> callback(CompletionResult.Failure(result.error))
            }
        }
    }

    private fun writeFiles(session: CardSession, callback: CompletionCallback<WriteFilesResponse>) {
        val card = session.environment.card.guard {
            callback(CompletionResult.Failure(TangemSdkError.CardError()))
            return
        }

        val fileData = data[currentIndex]
        WriteFileCommand(fileData).run(session) { result ->
            when (result) {
                is CompletionResult.Success -> {
                    result.data.fileIndex?.let { savedFilesIndices.add(it) }
                    if (currentIndex == data.lastIndex) {
                        callback(CompletionResult.Success(WriteFilesResponse(card.cardId, savedFilesIndices)))
                    } else {
                        currentIndex += 1
                        writeFiles(session, callback)
                    }
                }
                is CompletionResult.Failure -> callback(CompletionResult.Failure(result.error))
            }
        }
    }
}