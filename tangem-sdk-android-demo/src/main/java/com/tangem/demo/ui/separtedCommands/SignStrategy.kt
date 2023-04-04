package com.tangem.demo.ui.separtedCommands

import com.tangem.common.extensions.hexToBytes
import com.tangem.common.services.Result

/**
 * Created by Anton Zhilenkov on 01/07/2022.
 */
abstract class SignStrategy<HexData, SignData>(
    protected val hexHashes: String,
) {

    var onError: ((String) -> Unit)? = null

    protected val hashesSplitSeparator = "|"

    fun execute() {
        if (hexHashes.isEmpty()) {
            executeWithRandomHashes()
            return
        }

        val strategyErrorMessage = checkForStrategyError()
        if (strategyErrorMessage != null) {
            onError?.invoke(strategyErrorMessage)
            return
        }

        when (val result = convert()) {
            is Result.Success -> {
                executeStrategy(result.data)
            }
            is Result.Failure -> {
                onError?.invoke(result.error.localizedMessage ?: "Unknown error")
            }
        }
    }

    protected abstract fun executeWithRandomHashes()
    protected abstract fun checkForStrategyError(): String?
    protected abstract fun getHexData(): HexData
    protected abstract fun convert(): Result<SignData>
    protected abstract fun executeStrategy(dataToSign: SignData)
}

class SingleSignStrategy(
    hexHash: String,
    private val sign: (ByteArray) -> Unit,
    private val randomHashesGenerator: (Int) -> Array<ByteArray>,
) : SignStrategy<String, ByteArray>(hexHash) {

    override fun checkForStrategyError(): String? {
        return if (hexHashes.contains(hashesSplitSeparator)) {
            "To sign multiply hashes use 'Sign Hashes' instead"
        } else {
            null
        }
    }

    override fun executeWithRandomHashes() {
        sign(randomHashesGenerator(1)[0])
    }

    override fun getHexData(): String = hexHashes

    override fun convert(): Result<ByteArray> = try {
        Result.Success(getHexData().hexToBytes())
    } catch (e: Exception) {
        Result.Failure(Throwable("Invalid HEX"))
    }

    override fun executeStrategy(dataToSign: ByteArray) {
        sign(dataToSign)
    }
}

class MultiplySignStrategy(
    hexHashes: String,
    private val sign: (Array<ByteArray>) -> Unit,
    private val randomHashesGenerator: (Int) -> Array<ByteArray>,
) : SignStrategy<List<String>, Array<ByteArray>>(hexHashes) {

    override fun checkForStrategyError(): String? {
        return if (!hexHashes.contains(hashesSplitSeparator)) {
            "To sign a single hash use 'Sign Hash' instead"
        } else {
            null
        }
    }

    override fun executeWithRandomHashes() {
        sign(randomHashesGenerator(RANDOM_HASHES_COUNT))
    }

    override fun getHexData(): List<String> = hexHashes.split(hashesSplitSeparator)

    override fun convert(): Result<Array<ByteArray>> = try {
        val dataToSign = getHexData().map { it.hexToBytes() }.toTypedArray()
        Result.Success(dataToSign)
    } catch (e: Exception) {
        Result.Failure(Throwable("Invalid HEX"))
    }

    override fun executeStrategy(dataToSign: Array<ByteArray>) {
        sign(dataToSign)
    }

    private companion object {
        const val RANDOM_HASHES_COUNT = 11
    }
}

enum class SignStrategyType {
    SINGLE, MULTIPLE
}
