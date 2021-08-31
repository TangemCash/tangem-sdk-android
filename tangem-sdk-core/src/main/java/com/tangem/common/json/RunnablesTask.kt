package com.tangem.common.json

import com.tangem.common.CompletionResult
import com.tangem.common.core.CardSession
import com.tangem.common.core.CardSessionRunnable
import com.tangem.common.core.CompletionCallback
import com.tangem.operations.CommandResponse

/**
 * Created by Anton Zhilenkov on 31/08/2021.
 */
class RunnablesTaskResponse(
    val responses: List<String>
) : CommandResponse

internal class RunnablesTask(
    private val linkersList: List<JSONRPCLinker>
) : CardSessionRunnable<RunnablesTaskResponse> {

    override fun run(session: CardSession, callback: CompletionCallback<RunnablesTaskResponse>) {
        run(session, 0, callback)
    }

    private fun run(session: CardSession, index: Int, callback: CompletionCallback<RunnablesTaskResponse>) {
        if (index >= linkersList.size) {
            val completeResponse = linkersList.map { it.response.toJson() }
            callback(CompletionResult.Success(RunnablesTaskResponse(completeResponse)))
            return
        }

        linkersList[index].let { linker ->
            linker.runnable?.run(session) {
                linker.linkResult(it)
                run(session, index + 1, callback)
            }
        }
    }
}