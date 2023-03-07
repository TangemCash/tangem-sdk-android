package com.tangem.common.tlv

class TlvBuilder {
    private val tlvs = mutableListOf<Tlv>()
    private val encoder = TlvEncoder()

    internal inline fun <reified T> append(tag: TlvTag, value: T?) {
        if (value == null) return

        tlvs.add(encoder.encode(tag, value))
    }

    fun serialize(): ByteArray = tlvs.serialize()
}