package com.tangem.common

import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

/**
 * Created by Anton Zhilenkov on 12/03/2021.
 */
class Timer(
    val period: Long,
    val step: Long,
    val delayMs: Long = 1000L,
    private val withInitialDelay: Boolean = true,
    private val dispatcher: CoroutineDispatcher,
) {

    var onTick: ((Long) -> Unit)? = null

    var onCancel: (() -> Unit)? = null
    var onComplete: (() -> Unit)? = null
    var isInterrupted = false
        private set

    private var progress = 0L

    var isCompleted: Boolean = false
        get() = progress >= period
        private set

    private val job = SupervisorJob()

    private val scope = CoroutineScope(Dispatchers.Default + job)
    private val timer: Job = scope.launch(dispatcher) {
        if (withInitialDelay) delay(delayMs)

        while (!isCompleted) {
            progress += step
            onTick?.invoke(progress)
            delay(delayMs)
            if (isInterrupted) return@launch
        }
        onComplete?.invoke()
    }

    init {
        val isIllegalArguments = period < 0 || delayMs < 0 || step < 0 || period < step
        if (isIllegalArguments) error("Illegal arguments")
    }

    fun start() {
        if (isCompleted) return

        timer.start()
    }

    fun cancel() {
        if (isCompleted) return

        isInterrupted = true
        timer.cancel()
        onCancel?.invoke()
    }
}
