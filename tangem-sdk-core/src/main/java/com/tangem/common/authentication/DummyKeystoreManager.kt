package com.tangem.common.authentication

import javax.crypto.SecretKey

class DummyKeystoreManager : KeystoreManager {

    override suspend fun get(keyAlias: String): SecretKey? = null

    override suspend fun get(keyAliases: Collection<String>): Map<String, SecretKey> = emptyMap()

    override suspend fun store(keyAlias: String, key: SecretKey) = Unit
}
