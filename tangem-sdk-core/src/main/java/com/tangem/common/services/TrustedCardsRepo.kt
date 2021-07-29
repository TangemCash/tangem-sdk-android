package com.tangem.common.services

import com.tangem.common.extensions.calculateSha256
import com.tangem.common.extensions.mapNotNullValues
import com.tangem.common.extensions.toHexString
import com.tangem.common.json.MoshiJsonConverter
import com.tangem.common.services.secure.SecureService
import com.tangem.common.services.secure.SecureStorage
import com.tangem.operations.attestation.Attestation
import kotlin.collections.set

/**
 * Created by Anton Zhilenkov on 25/07/2021.
 */
class TrustedCardsRepo internal constructor(
    private val storage: SecureStorage,
    private val jsonConverter: MoshiJsonConverter,
    private val secureService: SecureService
) {
    private val maxCards = 1000

    //Key is Hash of card's public key
    private val data = mutableMapOf<String, Attestation>()

    init {
        fetch()
    }

    fun append(cardPublicKey: ByteArray, attestation: Attestation) {
        val maxIndex = data.map { it.value.index }.maxOrNull() ?: 0
        val newAttestation = attestation.copy().apply { index = maxIndex + 1 }
        if (newAttestation.index >= maxCards) {
            data.minByOrNull { it.value.index }?.key?.let { data.remove(it) }
        }

        val hexHash = cardPublicKey.calculateSha256().toHexString()
        data[hexHash] = newAttestation
        save()
    }

    fun attestation(cardPublicKey: ByteArray): Attestation? {
        val hexHash = cardPublicKey.calculateSha256().toHexString()
        return data[hexHash]
    }

    private fun save() {
        val convertedData: Map<String, String> = data.mapValues { it.value.rawRepresentation }
        val encoded = jsonConverter.toJson(convertedData).toByteArray()
        val signature = secureService.sign(encoded)
        storage.store(encoded, StorageKey.AttestedCards.name)
        storage.store(signature, StorageKey.SignatureOfAttestedCards.name)
    }

    private fun fetch() {
        val data = storage.get(StorageKey.AttestedCards.name)
        val signature = storage.get(StorageKey.SignatureOfAttestedCards.name)
        if (data == null || signature == null) return
        if (!secureService.verify(signature, data)) return

        val jsonData = String(data)
        val decoded: Map<String, Any> = jsonConverter.toMap(jsonData)
        val converted: Map<String, Attestation> = decoded.mapNotNullValues { entry ->
            Attestation.fromRawRepresentation(entry.value.toString())
        }
        this.data.putAll(converted)
    }

    enum class StorageKey {
        AttestedCards, SignatureOfAttestedCards
    }
}