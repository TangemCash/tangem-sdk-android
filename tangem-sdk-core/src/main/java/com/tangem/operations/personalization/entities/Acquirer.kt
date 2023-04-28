package com.tangem.operations.personalization.entities

import com.tangem.common.KeyPair

data class Acquirer(
    val keyPair: KeyPair,
    val name: String? = null,
    val id: String? = null,
)
