package com.tangem.common

import com.squareup.moshi.JsonAdapter
import com.squareup.moshi.JsonClass
import com.squareup.moshi.Moshi
import com.squareup.moshi.Types
import com.tangem.common.authentication.AuthenticatedStorage
import com.tangem.common.authentication.KeystoreManager
import com.tangem.common.json.MoshiJsonConverter
import com.tangem.common.services.secure.SecureStorage
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

@JsonClass(generateAdapter = true)
class CardTokens constructor(
    val accessToken: ByteArray?,
    val identifyToken: ByteArray?,
)

@Suppress("unused")
class CardTokensRepository(
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
    private val cardTokensAdapter: JsonAdapter<CardTokens> = moshi.adapter(
        CardTokens::class.java,
    )

    private val cardIdToTokens: HashMap<String, CardTokens> = hashMapOf()

    suspend fun unlock(): CompletionResult<Unit> = withContext(Dispatchers.IO) {
        val cardIdToUserCodeInternal = getSavedCardsIds()
            .associateWith { cardId ->
                when (val result = getSavedCardTokens(cardId)) {
                    is CompletionResult.Success -> result.data
                    is CompletionResult.Failure -> return@withContext CompletionResult.Failure(result.error)
                }
            }

        catching {
            cardIdToTokens.clear()
            cardIdToUserCodeInternal.forEach { (cardId, cardTokens) ->
                if (cardTokens != null) {
                    cardIdToTokens[cardId] = cardTokens
                }
            }
        }
    }

    fun lock() {
        cardIdToTokens.clear()
    }

    suspend fun save(cardId: String, cardTokens: CardTokens): CompletionResult<Unit> {
        return save(setOf(cardId), cardTokens)
    }

    suspend fun save(cardsIds: Set<String>, cardTokens: CardTokens): CompletionResult<Unit> {
        if (!updateTokensIfNeeded(cardsIds, cardTokens)) {
            return CompletionResult.Success(Unit) // Nothing changed. Return
        }

        return withContext(Dispatchers.IO) {
            saveCardTokens(cardsIds, cardTokens)
                .map { saveCardsIds(cardsIds = getSavedCardsIds() + cardsIds) }
        }
    }

    fun get(cardId: String): CardTokens? {
        return cardIdToTokens[cardId]
    }

    suspend fun delete(cardsIds: Set<String>): CompletionResult<Unit> {
        return withContext(Dispatchers.IO) {
            catching {
                cardsIds.forEach { cardId ->
                    deleteCardTokens(cardId)
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

    suspend fun hasSavedCardTokens(): Boolean = getSavedCardsIds().isNotEmpty()
    suspend fun hasSavedCardTokens(cardId: String): Boolean = getSavedCardsIds().contains(cardId)

    private fun updateTokensIfNeeded(cardIds: Set<String>, cardTokens: CardTokens): Boolean {
        var hasChanges = false

        for (cardId in cardIds) {
            val existingIdentifyToken = cardIdToTokens[cardId]?.identifyToken
            when (cardTokens.identifyToken) {
                existingIdentifyToken -> continue // We already know this code. Ignoring
                null -> {
                    // User deleted the token. We should update the storage
                    cardIdToTokens.remove(cardId)
                    hasChanges = true
                }

                else -> {
                    // Save a new code
                    cardIdToTokens[cardId] = cardTokens
                    hasChanges = true
                }
            }
        }

        return hasChanges
    }

    private suspend fun getSavedCardTokens(cardId: String): CompletionResult<CardTokens?> {
        return catching {
            authenticatedStorage.get(StorageKey.CardTokens(cardId).name)
                .decodeToCardTokens()
        }
    }

    private suspend fun saveCardTokens(cardsIds: Set<String>, cardTokens: CardTokens): CompletionResult<Unit> {
        return catching {
            cardsIds.forEach { cardId ->
                authenticatedStorage.store(StorageKey.CardTokens(cardId).name, cardTokens.encode())
            }
        }
    }

    private fun deleteCardTokens(cardId: String) {
        return authenticatedStorage.delete(StorageKey.CardTokens(cardId).name)
    }

    private suspend fun getSavedCardsIds(): Set<String> {
        return withContext(Dispatchers.IO) {
            secureStorage.get(StorageKey.CardsWithSavedTokens.name)
                .decodeToCardsIds()
        }
    }

    private suspend fun saveCardsIds(cardsIds: Set<String>) {
        withContext(Dispatchers.IO) {
            secureStorage.store(cardsIds.encode(), StorageKey.CardsWithSavedTokens.name)
        }
    }

    private suspend fun deleteCardsIds(cardsIds: Set<String>) {
        cardsIds.forEach { cardId ->
            cardIdToTokens.remove(cardId)
        }
        val remainingCardsIds = getSavedCardsIds() - cardsIds
        saveCardsIds(remainingCardsIds)
    }

    private suspend fun clearSavedCardsIds() {
        cardIdToTokens.clear()
        withContext(Dispatchers.IO) {
            secureStorage.delete(StorageKey.CardsWithSavedTokens.name)
        }
    }

    private suspend fun CardTokens.encode(): ByteArray {
        return withContext(Dispatchers.Default) {
            this@encode
                .let(cardTokensAdapter::toJson)
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

    private suspend fun ByteArray?.decodeToCardTokens(): CardTokens? {
        return withContext(Dispatchers.Default) {
            this@decodeToCardTokens
                ?.decodeToString(throwOnInvalidSequence = true)
                ?.let(cardTokensAdapter::fromJson)
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

        class CardTokens(cardId: String) : StorageKey {
            override val name: String = "card_tokens_$cardId"
        }

        object CardsWithSavedTokens : StorageKey {
            override val name: String = "cards_with_saved_tokens"
        }
    }
}
