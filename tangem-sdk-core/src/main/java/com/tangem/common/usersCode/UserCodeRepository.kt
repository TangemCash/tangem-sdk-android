package com.tangem.common.usersCode

import com.tangem.common.CompletionResult
import com.tangem.common.UserCode
import com.tangem.common.biometric.BiometricManager
import com.tangem.common.biometric.BiometricStorage
import com.tangem.common.catching
import com.tangem.common.core.TangemSdkError
import com.tangem.common.extensions.calculateSha256
import com.tangem.common.extensions.mapNotNullValues
import com.tangem.common.flatMap
import com.tangem.common.json.MoshiJsonConverter
import com.tangem.common.map
import com.tangem.common.services.secure.SecureStorage
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

class UserCodeRepository(
    private val biometricManager: BiometricManager,
    private val secureStorage: SecureStorage,
    private val jsonConverter: MoshiJsonConverter = MoshiJsonConverter.INSTANCE,
) {
    private val biometricStorage = BiometricStorage(
        biometricManager = biometricManager,
        secureStorage = secureStorage
    )
    private val cardIdToUserCode: HashMap<String, UserCode> = hashMapOf()

    fun lock(): CompletionResult<Unit> {
        cardIdToUserCode.clear()
        return CompletionResult.Success(Unit)
    }

    suspend fun unlock(): CompletionResult<Unit> {
        if (!biometricManager.canAuthenticate)
            return CompletionResult.Failure(TangemSdkError.BiometricsUnavailable())

        return biometricStorage.get(StorageKey.UserCodes.name)
            .map { data ->
                cardIdToUserCode.clear()
                data
                    ?.decodeToString(throwOnInvalidSequence = true)
                    ?.let(jsonConverter::toMap)
                    ?.mapNotNullValues { (_, value) ->
                        jsonConverter.fromJson<UserCode>(value as String)
                    }
                    ?.also(cardIdToUserCode::putAll)
            }
    }

    suspend fun save(
        cardId: String,
        userCode: UserCode,
    ): CompletionResult<Unit> {
        return save(setOf(cardId), userCode)
    }

    fun get(cardId: String): UserCode? {
        return cardIdToUserCode[cardId]
    }

    suspend fun hasSavedUserCodes(): Boolean = getCards().isNotEmpty()
    suspend fun hasSavedUserCode(cardId: String): Boolean = getCards().contains(cardId)

    suspend fun clear(): CompletionResult<Unit> {
        return biometricStorage.delete(StorageKey.UserCodes.name)
            .map { cardIdToUserCode.clear() }
    }

    suspend fun save(cardIds: Set<String>, userCode: UserCode): CompletionResult<Unit> {
        if (!biometricManager.canAuthenticate)
            return CompletionResult.Failure(TangemSdkError.BiometricsUnavailable())

        if (!updateCodesIfNeeded(cardIds, userCode))
            return CompletionResult.Success(Unit) // Nothing changed. Return

        return catching {
            cardIdToUserCode
                .mapValues { jsonConverter.toJson(it.value) }
                .let { jsonConverter.toJson(it) }
                .encodeToByteArray(throwOnInvalidSequence = true)
        }
            .flatMap { data -> biometricStorage.store(StorageKey.UserCodes.name, data) }
            .map { saveCards() }
    }

    private fun updateCodesIfNeeded(cardIds: Set<String>, userCode: UserCode): Boolean {
        var hasChanges = false

        for (cardId in cardIds) {
            val existingCode = cardIdToUserCode[cardId]?.value
            when (userCode.value) {
                existingCode -> continue // We already know this code. Ignoring
                userCode.type.defaultValue.calculateSha256() -> if (existingCode == null) {
                    continue // Ignore default code
                } else {
                    // User deleted the code. We should update the storage
                    cardIdToUserCode.remove(cardId)
                    hasChanges = true
                }
                else -> {
                    // Save a new code
                    cardIdToUserCode[cardId] = userCode
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
        cardIdToUserCode.keys
            .let(jsonConverter::toJson)
            .let(String::toByteArray)
            .also {
                withContext(Dispatchers.IO) {
                    secureStorage.store(it, StorageKey.CardsWithSavedAccessCode.name)
                }
            }
    }

    enum class StorageKey {
        UserCodes,
        CardsWithSavedAccessCode,
    }
}
