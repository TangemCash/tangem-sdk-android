package com.tangem.common.core

import com.tangem.TangemSdk
import com.tangem.common.CompletionResult
import com.tangem.operations.CommandResponse
import com.tangem.operations.PreflightReadMode

/**
 * Basic interface for running tasks and [com.tangem.operations.Command] in a [CardSession]
 */
interface CardSessionRunnable<T : CommandResponse> {
    /**
     * Mode for preflight read. Change this property only if you understand what to do
     */
    fun preflightReadMode(): PreflightReadMode = PreflightReadMode.FullCardRead

    /**
     *  This method will be called before nfc session starts.
     *  @param session:You can use view delegate methods at this moment, but not commands execution
     *  @param completion: Call the completion handler to complete the task.
     */
    fun prepare(session: CardSession, callback: CompletionCallback<Unit>) {
        callback(CompletionResult.Success(Unit))
    }

    /**
     * The starting point for custom business logic.
     * Implement this interface and use [TangemSdk.startSessionWithRunnable] to run.
     * @param session run commands in this [CardSession].
     * @param callback trigger the callback to complete the task.
     */
    fun run(session: CardSession, callback: CompletionCallback<T>)
}

typealias CompletionCallback<R> = (result: CompletionResult<R>) -> Unit
