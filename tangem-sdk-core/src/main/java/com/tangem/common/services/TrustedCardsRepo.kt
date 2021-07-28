package com.tangem.common.services

import com.tangem.common.extensions.calculateSha256
import com.tangem.common.extensions.mapNotNullValues
import com.tangem.common.json.MoshiJsonConverter
import com.tangem.common.services.secure.SecureService
import com.tangem.common.services.secure.SecureStorage
import com.tangem.operations.attestation.Attestation

/**
 * Created by Anton Zhilenkov on 25/07/2021.
 */
class TrustedCardsRepo internal constructor(
    private val storage: SecureStorage,
    private val jsonConverter: MoshiJsonConverter,
    private val secureService: SecureService
) {
    //Key is Hash of card's public key
    private val attestationData = mutableMapOf<ByteArray, Attestation>()

    init {
        fetch()
    }

    fun append(cardPublicKey: ByteArray, attestation: Attestation) {
        val maxIndex = attestationData.map { it.value.index }.maxOrNull() ?: 0
        val newAttestation = attestation.copy().apply { index = maxIndex + 1 }
        if (newAttestation.index >= maxCards) {
            attestationData.minByOrNull { it.value.index }?.key?.let { attestationData.remove(it) }
        }

        val hash = cardPublicKey.calculateSha256()
        attestationData[hash] = newAttestation
        save()
    }

    fun attestation(cardPublicKey: ByteArray): Attestation? {
        val hash = cardPublicKey.calculateSha256()
        return attestationData[hash]
    }

    private fun save() {
        val rawData = attestationData.mapValues { it.value.rawRepresentation }
        val jsonData = jsonConverter.toJson(rawData)
        val data = jsonData.toByteArray()
        val signature = secureService.sign(data)
        storage.store(data, StorageKey.AttestedCards.name)
        storage.store(signature, StorageKey.SignatureOfAttestedCards.name)
    }

    private fun fetch() {
        val data = storage.get(StorageKey.AttestedCards.name)
        val signature = storage.get(StorageKey.SignatureOfAttestedCards.name)
        if (data == null || signature == null) return
        if (!secureService.verify(signature, data)) return

        val jsonData = String(data)
        val parameterizedType = jsonConverter.typedMap(ByteArray::class, String::class)
        val rawData: MutableMap<ByteArray, String> = jsonConverter.fromJson(jsonData, parameterizedType) ?: return
        val newData = rawData.mapNotNullValues { entry -> Attestation.fromRawRepresentation(entry.value) }
        attestationData.putAll(newData)
    }

    companion object {
        val maxCards = 1000
    }

    enum class StorageKey {
        AttestedCards, SignatureOfAttestedCards
    }
}