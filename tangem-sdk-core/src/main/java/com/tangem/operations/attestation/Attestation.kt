package com.tangem.operations.attestation

import com.squareup.moshi.JsonClass
import com.tangem.operations.CommandResponse

@JsonClass(generateAdapter = true)
data class Attestation(
    val cardKeyAttestation: Status,
    val walletKeysAttestation: Status,
    val firmwareAttestation: Status,
    val cardUniquenessAttestation: Status,
) : CommandResponse {

    /**
     * Index for storage
     */
    @Transient
    var index: Int = 0

    val status: Status
        get() {
            if (statuses.filter { it == Status.Skipped }.size == statuses.size) return Status.Skipped
            if (statuses.contains(Status.Failed)) return Status.Failed
            if (statuses.contains(Status.Warning)) return Status.Warning
            if (statuses.contains(Status.VerifiedOffline)) return Status.VerifiedOffline
            return Status.Verified
        }

    val mode: AttestationTask.Mode
        get() = if (walletKeysAttestation == Status.Skipped) AttestationTask.Mode.Normal else AttestationTask.Mode.Full

    val rawRepresentation: String
        get() {
            val joinedStatuses = statuses.map { it.intRepresentation }.joinToString(separator = ",")
            return "$index,$joinedStatuses"
        }

    @Transient
    private val statuses: List<Status> = listOf(
        cardKeyAttestation,
        walletKeysAttestation,
        firmwareAttestation,
        cardUniquenessAttestation,
    )

    companion object {
        val empty: Attestation = Attestation(Status.Skipped, Status.Skipped, Status.Skipped, Status.Skipped)

        @Suppress("MagicNumber")
        fun fromRawRepresentation(rawRepresentation: String): Attestation? {
            val values = rawRepresentation.split(",").mapNotNull { it.toIntOrNull() }
            if (values.size != 5) return null

            val index = values[0]
            val statusList = values.subList(1, 5).mapNotNull { Status.fromInt(it) }
            if (statusList.size != 4) return null

            return Attestation(
                statusList[0],
                statusList[1],
                statusList[2],
                statusList[3],
            ).apply { this.index = index }
        }
    }

    enum class Status(val intRepresentation: Int) {
        Failed(intRepresentation = 0),
        Warning(intRepresentation = 1),
        Skipped(intRepresentation = 2),
        VerifiedOffline(intRepresentation = 3),
        Verified(intRepresentation = 4),
        ;

        companion object {
            fun fromInt(intRepresentation: Int): Status? {
                return values().firstOrNull { it.intRepresentation == intRepresentation }
            }
        }
    }
}
