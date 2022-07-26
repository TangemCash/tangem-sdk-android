package com.tangem.common.accesscode

import com.tangem.common.CompletionResult
import com.tangem.common.UserCodeType
import com.tangem.common.biometric.BiometricManager
import com.tangem.common.biometric.BiometricStorage
import com.tangem.common.catching
import com.tangem.common.core.TangemSdkError
import com.tangem.common.doOnSuccess
import com.tangem.common.extensions.calculateSha256
import com.tangem.common.extensions.hexToBytes
import com.tangem.common.extensions.mapNotNullValues
import com.tangem.common.flatMap
import com.tangem.common.json.MoshiJsonConverter
import com.tangem.common.map
import com.tangem.common.services.secure.SecureStorage
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

class AccessCodeRepository(
    private val biometricManager: BiometricManager,
    private val secureStorage: SecureStorage,
    private val jsonConverter: MoshiJsonConverter,
) {
    private val biometricStorage = BiometricStorage(biometricManager, secureStorage)

    private val cardIdToAccessCode: HashMap<String, ByteArray> = hashMapOf()

    fun lock(): CompletionResult<Unit> {
        cardIdToAccessCode.clear()
        return CompletionResult.Success(Unit)
    }

    suspend fun unlock(): CompletionResult<Unit> {
        if (!biometricManager.canAuthenticate)
            return CompletionResult.Failure(TangemSdkError.BiometricsUnavailable())

        return biometricStorage.get(StorageKey.AccessCodes.name)
            .map { data ->
                cardIdToAccessCode.clear()
                data
                    ?.decodeToString(throwOnInvalidSequence = true)
                    ?.let(jsonConverter::toMap)
                    ?.mapNotNullValues { (_, value) ->
                        (value as? String)?.hexToBytes()
                    }
                    ?.also(cardIdToAccessCode::putAll)
            }
    }

    suspend fun save(
        cardId: String,
        accessCode: ByteArray,
    ): CompletionResult<Unit> {
        return save(listOf(cardId), accessCode)
    }

    fun get(cardId: String): ByteArray? {
        return cardIdToAccessCode[cardId]
    }

    suspend fun hasSavedAccessCodes(): Boolean = getCards().isNotEmpty()
    suspend fun hasSavedAccessCode(cardId: String): Boolean = getCards().contains(cardId)
    suspend fun clear(): CompletionResult<Unit> {
        return biometricStorage.delete(StorageKey.AccessCodes.name)
            .doOnSuccess { cardIdToAccessCode.clear() }
            .map { CompletionResult.Success(Unit) }
    }

    private suspend fun save(cardIds: List<String>, accessCode: ByteArray): CompletionResult<Unit> {
        if (!biometricManager.canAuthenticate)
            return CompletionResult.Failure(TangemSdkError.BiometricsUnavailable())

        if (!updateCodesIfNeeded(cardIds, accessCode))
            return CompletionResult.Success(Unit) // Nothing changed. Return
        return catching {
            cardIdToAccessCode
                .let { jsonConverter.toJson(it) }
                .encodeToByteArray(throwOnInvalidSequence = true)
        }
            .flatMap { data -> biometricStorage.store(StorageKey.AccessCodes.name, data) }
            .doOnSuccess { saveCards() }
    }

    private fun updateCodesIfNeeded(cardIds: List<String>, accessCode: ByteArray): Boolean {
        var hasChanges = false

        for (cardId in cardIds) {
            val existingCode = cardIdToAccessCode[cardId]
            when (accessCode) {
                existingCode -> continue // We already know this code. Ignoring
                UserCodeType.AccessCode.defaultValue.calculateSha256() -> if (existingCode == null) {
                    continue // Ignore default code
                } else {
                    // User deleted the code. We should update the storage
                    cardIdToAccessCode.remove(cardId)
                    hasChanges = true
                }
                else -> {
                    // Save a new code
                    cardIdToAccessCode[cardId] = accessCode
                    hasChanges = true
                }
            }
        }

        return hasChanges
    }

    private suspend fun getCards(): List<String> {
        return withContext(Dispatchers.IO) {
            secureStorage.get(StorageKey.CardsWithSavedAccessCode.name)
        }
            ?.let(::String)
            ?.let { jsonConverter.fromJson<List<String>>(it) }
            .orEmpty()
    }

    private suspend fun saveCards() = withContext(Dispatchers.IO) {
        cardIdToAccessCode.keys
            .let(jsonConverter::toJson)
            .let(String::toByteArray)
            .also {
                withContext(Dispatchers.IO) {
                    secureStorage.store(it, StorageKey.CardsWithSavedAccessCode.name)
                }
            }
    }

    enum class StorageKey {
        AccessCodes, CardsWithSavedAccessCode
    }
}
