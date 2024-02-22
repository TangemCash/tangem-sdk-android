package com.tangem.common.usersCode

import com.squareup.moshi.JsonAdapter
import com.squareup.moshi.Moshi
import com.squareup.moshi.Types
import com.tangem.common.CompletionResult
import com.tangem.common.UserCode
import com.tangem.common.authentication.AuthenticatedStorage
import com.tangem.common.authentication.KeystoreManager
import com.tangem.common.catching
import com.tangem.common.extensions.calculateSha256
import com.tangem.common.extensions.mapNotNullValues
import com.tangem.common.flatMap
import com.tangem.common.json.MoshiJsonConverter
import com.tangem.common.map
import com.tangem.common.services.secure.SecureStorage
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

@Suppress("unused")
class UserCodeRepository(
    keystoreManager: KeystoreManager,
    private val secureStorage: SecureStorage,
) {
    private val authenticatedStorage = AuthenticatedStorage(
        secureStorage = secureStorage,
        keystoreManager = keystoreManager,
    )

    private val moshi: Moshi = MoshiJsonConverter.INSTANCE.moshi
    private val cardsIdsAdapter: JsonAdapter<Set<String>> = moshi.adapter(
        Types.newParameterizedType(Set::class.java, String::class.java),
    )
    private val userCodeAdapter: JsonAdapter<UserCode> = moshi.adapter(
        UserCode::class.java,
    )

    private val cardIdToUserCode: HashMap<String, UserCode> = hashMapOf()

    suspend fun unlock(): CompletionResult<Unit> = withContext(Dispatchers.IO) {
        catching {
            val cardsIds = getSavedCardsIds()
            val userCodes = getSavedUserCodes(cardsIds)

            cardIdToUserCode.clear()
            cardIdToUserCode.putAll(userCodes)
        }
    }

    private suspend fun getSavedUserCodes(cardsIds: Set<String>): Map<String, UserCode> {
        val keys = cardsIds.map { StorageKey.UserCode(it).name }
        val encodedData = authenticatedStorage.get(keys)

        return encodedData
            .mapKeys { (key, _) -> key.removePrefix(StorageKey.UserCode.PREFIX) }
            .mapNotNullValues { (_, value) -> value.decodeToUserCode() }
    }

    fun lock() {
        cardIdToUserCode.clear()
    }

    suspend fun save(cardId: String, userCode: UserCode): CompletionResult<Unit> {
        return save(setOf(cardId), userCode)
    }

    suspend fun save(cardsIds: Set<String>, userCode: UserCode): CompletionResult<Unit> {
        if (!updateCodesIfNeeded(cardsIds, userCode)) {
            return CompletionResult.Success(Unit) // Nothing changed. Return
        }

        return withContext(Dispatchers.IO) {
            saveUserCode(cardsIds, userCode)
                .map { saveCardsIds(cardsIds = getSavedCardsIds() + cardsIds) }
        }
    }

    fun get(cardId: String): UserCode? {
        return cardIdToUserCode[cardId]
    }

    suspend fun delete(cardsIds: Set<String>): CompletionResult<Unit> {
        return withContext(Dispatchers.IO) {
            catching {
                cardsIds.forEach { cardId ->
                    deleteUserCode(cardId)
                }

                deleteCardsIds(cardsIds)
            }
        }
    }

    suspend fun clear(): CompletionResult<Unit> {
        return withContext(Dispatchers.IO) {
            catching { getSavedCardsIds() }
                .flatMap { cardsIds -> delete(cardsIds) }
        }
    }

    suspend fun hasSavedUserCodes(): Boolean = getSavedCardsIds().isNotEmpty()
    suspend fun hasSavedUserCode(cardId: String): Boolean = getSavedCardsIds().contains(cardId)

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

    private suspend fun saveUserCode(cardsIds: Set<String>, userCode: UserCode): CompletionResult<Unit> {
        return catching {
            cardsIds.forEach { cardId ->
                authenticatedStorage.store(StorageKey.UserCode(cardId).name, userCode.encode())
            }
        }
    }

    private fun deleteUserCode(cardId: String) {
        return authenticatedStorage.delete(StorageKey.UserCode(cardId).name)
    }

    private suspend fun getSavedCardsIds(): Set<String> {
        return withContext(Dispatchers.IO) {
            secureStorage.get(StorageKey.CardsWithSavedUserCode.name)
                .decodeToCardsIds()
        }
    }

    private suspend fun saveCardsIds(cardsIds: Set<String>) {
        withContext(Dispatchers.IO) {
            secureStorage.store(cardsIds.encode(), StorageKey.CardsWithSavedUserCode.name)
        }
    }

    private suspend fun deleteCardsIds(cardsIds: Set<String>) {
        cardsIds.forEach { cardId ->
            cardIdToUserCode.remove(cardId)
        }
        val remainingCardsIds = getSavedCardsIds() - cardsIds
        saveCardsIds(remainingCardsIds)
    }

    private suspend fun clearSavedCardsIds() {
        cardIdToUserCode.clear()
        withContext(Dispatchers.IO) {
            secureStorage.delete(StorageKey.CardsWithSavedUserCode.name)
        }
    }

    private fun UserCode.encode(): ByteArray {
        return this@encode
            .let(userCodeAdapter::toJson)
            .encodeToByteArray(throwOnInvalidSequence = true)
    }

    private fun Set<String>.encode(): ByteArray {
        return this@encode
            .let(cardsIdsAdapter::toJson)
            .encodeToByteArray(throwOnInvalidSequence = true)
    }

    private fun ByteArray?.decodeToUserCode(): UserCode? {
        return this@decodeToUserCode
            ?.decodeToString(throwOnInvalidSequence = true)
            ?.let(userCodeAdapter::fromJson)
    }

    private fun ByteArray?.decodeToCardsIds(): Set<String> {
        return this@decodeToCardsIds
            ?.decodeToString(throwOnInvalidSequence = true)
            ?.let(cardsIdsAdapter::fromJson)
            .orEmpty()
    }

    private sealed interface StorageKey {
        val name: String

        class UserCode(cardId: String) : StorageKey {
            override val name: String = PREFIX + cardId

            companion object {
                const val PREFIX = "user_code_"
            }
        }

        object CardsWithSavedUserCode : StorageKey {
            override val name: String = "cards_with_saved_user_code"
        }
    }
}
