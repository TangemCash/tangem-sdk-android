package com.tangem.common.usersCode

import com.squareup.moshi.JsonAdapter
import com.squareup.moshi.Moshi
import com.squareup.moshi.Types
import com.tangem.common.CompletionResult
import com.tangem.common.UserCode
import com.tangem.common.biometric.BiometricManager
import com.tangem.common.biometric.BiometricStorage
import com.tangem.common.catching
import com.tangem.common.core.TangemSdkError
import com.tangem.common.extensions.calculateSha256
import com.tangem.common.fold
import com.tangem.common.json.MoshiJsonConverter
import com.tangem.common.map
import com.tangem.common.services.secure.SecureStorage
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

class UserCodeRepository(
    private val biometricManager: BiometricManager,
    private val secureStorage: SecureStorage,
) {
    private val biometricStorage = BiometricStorage(
        biometricManager = biometricManager,
        secureStorage = secureStorage,
    )
    private val moshi: Moshi = MoshiJsonConverter.INSTANCE.moshi
    private val cardsIdsAdapter: JsonAdapter<Set<String>> = moshi.adapter(
        Types.newParameterizedType(Set::class.java, String::class.java),
    )
    private val userCodeAdapter: JsonAdapter<UserCode> = moshi.adapter(
        UserCode::class.java,
    )

    private val cardIdToUserCode: HashMap<String, UserCode> = hashMapOf()

    suspend fun unlock(): CompletionResult<Unit> {
        if (!biometricManager.canAuthenticate) {
            return CompletionResult.Failure(TangemSdkError.BiometricsUnavailable())
        }

        val cardIdToUserCodeInternal = getCardsIds()
            .associateWith { cardId ->
                when (val result = getUserCode(cardId)) {
                    is CompletionResult.Success -> result.data
                    is CompletionResult.Failure -> return CompletionResult.Failure(result.error)
                }
            }

        return catching {
            cardIdToUserCode.clear()
            cardIdToUserCodeInternal.forEach { (cardId, userCode) ->
                if (userCode != null) {
                    cardIdToUserCode[cardId] = userCode
                }
            }
        }
    }

    fun lock() {
        cardIdToUserCode.clear()
    }

    suspend fun save(
        cardId: String,
        userCode: UserCode,
    ): CompletionResult<Unit> {
        return save(setOf(cardId), userCode)
    }

    suspend fun save(cardsIds: Set<String>, userCode: UserCode): CompletionResult<Unit> {
        if (!biometricManager.canAuthenticate) {
            return CompletionResult.Failure(TangemSdkError.BiometricsUnavailable())
        }

        if (!updateCodesIfNeeded(cardsIds, userCode)) {
            return CompletionResult.Success(Unit) // Nothing changed. Return
        }

        return saveUserCode(cardsIds, userCode)
            .map { saveCardsIds() }
    }

    fun get(cardId: String): UserCode? {
        return cardIdToUserCode[cardId]
    }

    suspend fun delete(cardsIds: Set<String>): CompletionResult<Unit> {
        return cardsIds
            .map { cardId -> deleteUserCode(cardId) }
            .fold()
            .map { deleteCardsIds(cardsIds) }
    }

    suspend fun clear(): CompletionResult<Unit> {
        return getCardsIds()
            .map { cardId -> deleteUserCode(cardId) }
            .fold()
            .map { clearSavedCardsIds() }
    }

    suspend fun hasSavedUserCodes(): Boolean = getCardsIds().isNotEmpty()
    suspend fun hasSavedUserCode(cardId: String): Boolean = getCardsIds().contains(cardId)

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

    private suspend fun getUserCode(cardId: String): CompletionResult<UserCode?> {
        return biometricStorage.get(StorageKey.UserCode(cardId).name)
            .map { it.decodeToUserCode() }
    }

    private suspend fun saveUserCode(cardsIds: Set<String>, userCode: UserCode): CompletionResult<Unit> {
        return cardsIds.map { cardId ->
            biometricStorage.store(StorageKey.UserCode(cardId).name, userCode.encode())
        }
            .fold()
    }

    private suspend fun deleteUserCode(cardId: String): CompletionResult<Unit> {
        return biometricStorage.delete(StorageKey.UserCode(cardId).name)
    }

    private suspend fun getCardsIds(): Set<String> {
        return withContext(Dispatchers.IO) {
            secureStorage.get(StorageKey.CardsWithSavedUserCode.name)
                .decodeToCardsIds()
        }
    }

    private suspend fun saveCardsIds(cardsIds: Set<String> = cardIdToUserCode.keys) {
        withContext(Dispatchers.IO) {
            secureStorage.store(cardsIds.encode(), StorageKey.CardsWithSavedUserCode.name)
        }
    }

    private suspend fun deleteCardsIds(cardsIds: Set<String>) {
        cardsIds.forEach { cardId ->
            cardIdToUserCode.remove(cardId)
        }
        val remainingCardsIds = getCardsIds() - cardsIds
        saveCardsIds(remainingCardsIds)
    }

    private suspend fun clearSavedCardsIds() {
        cardIdToUserCode.clear()
        withContext(Dispatchers.IO) {
            secureStorage.delete(StorageKey.CardsWithSavedUserCode.name)
        }
    }

    private suspend fun UserCode.encode(): ByteArray {
        return withContext(Dispatchers.Default) {
            this@encode
                .let(userCodeAdapter::toJson)
                .encodeToByteArray(throwOnInvalidSequence = true)
        }
    }

    private suspend fun Set<String>.encode(): ByteArray {
        return withContext(Dispatchers.Default) {
            this@encode
                .let(cardsIdsAdapter::toJson)
                .encodeToByteArray(throwOnInvalidSequence = true)
        }
    }

    private suspend fun ByteArray?.decodeToUserCode(): UserCode? {
        return withContext(Dispatchers.Default) {
            this@decodeToUserCode
                ?.decodeToString(throwOnInvalidSequence = true)
                ?.let(userCodeAdapter::fromJson)
        }
    }

    private suspend fun ByteArray?.decodeToCardsIds(): Set<String> {
        return withContext(Dispatchers.Default) {
            this@decodeToCardsIds
                ?.decodeToString(throwOnInvalidSequence = true)
                ?.let(cardsIdsAdapter::fromJson)
                .orEmpty()
        }
    }

    private sealed interface StorageKey {
        val name: String

        class UserCode(cardId: String) : StorageKey {
            override val name: String = "user_code_$cardId"
        }

        object CardsWithSavedUserCode : StorageKey {
            override val name: String = "cards_with_saved_user_code"
        }
    }
}
