package com.tangem.common.accesscode

import com.tangem.common.extensions.calculateSha256
import com.tangem.common.extensions.hexToBytes
import com.tangem.common.extensions.mapNotNullValues
import com.tangem.common.extensions.toHexString
import com.tangem.common.json.MoshiJsonConverter
import com.tangem.common.services.Storage
import com.tangem.common.services.secure.SecureStorage

internal class DefaultAccessCodeRepository(
    private val storage: Storage,
    private val secureStorage: SecureStorage,
    private val jsonConverter: MoshiJsonConverter,
) : AccessCodeRepository {
    private val cardIdToAccessCode = mutableMapOf<String, ByteArray>()
    private val cardIdToRememberCodeToggleState = mutableMapOf<String, Boolean>()

    init {
        populate()
    }

    override fun get(cardId: String): Result<ByteArray> {
        return runCatching {
            cardIdToAccessCode.getValue(createKey(cardId))
        }
    }

    override fun append(cardId: String, accessCode: ByteArray) {
        if (cardIdToAccessCode.entries.size >= MAX_STORED_CARDS) {
            cardIdToAccessCode.apply {
                remove(keys.first())
            }
        }

        cardIdToAccessCode[createKey(cardId)] = accessCode
        secureStorage.store(
            account = StorageKey.AccessCodes.name,
            data = jsonConverter.toJson(cardIdToAccessCode).toByteArray()
        )
    }

    override fun getRememberCodeToggleState(
        cardId: String,
        defaultState: Boolean
    ): Result<Boolean> {
        return runCatching {
            cardIdToRememberCodeToggleState.getValue(createKey(cardId))
        }
    }

    override fun saveRememberCodeToggleState(cardId: String, isToggled: Boolean) {
        if (cardIdToRememberCodeToggleState.entries.size >= MAX_STORED_CARDS) {
            cardIdToAccessCode.apply {
                remove(keys.first())
            }
        }

        cardIdToRememberCodeToggleState[createKey(cardId)] = isToggled
        storage.putString(
            key = StorageKey.Toggles.name,
            value = jsonConverter.toJson(cardIdToRememberCodeToggleState)
        )
    }

    private fun populate() {
        val accessCodesRaw = secureStorage.get(StorageKey.AccessCodes.name)
        if (accessCodesRaw != null) {
            val accessCodesConverted = jsonConverter
                .toMap(String(accessCodesRaw))
                .mapNotNullValues { (it.value as? String)?.hexToBytes() }

            cardIdToAccessCode.putAll(accessCodesConverted)
        }

        val togglesRaw = storage.getString(StorageKey.Toggles.name)
        if (togglesRaw != null) {
            val togglesConverted = jsonConverter
                .toMap(togglesRaw)
                .mapNotNullValues { it.value as? Boolean }

            cardIdToRememberCodeToggleState.putAll(togglesConverted)
        }
    }

    private fun createKey(cardId: String): String {
        return cardId.calculateSha256().toHexString()
    }

    private enum class StorageKey {
        AccessCodes, Toggles
    }

    companion object {
        private const val MAX_STORED_CARDS = 100
    }
}
