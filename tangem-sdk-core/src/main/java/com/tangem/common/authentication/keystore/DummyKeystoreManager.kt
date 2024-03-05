package com.tangem.common.authentication.keystore

import javax.crypto.SecretKey

class DummyKeystoreManager : KeystoreManager {

    override suspend fun get(masterKeyConfig: KeystoreManager.MasterKeyConfig, keyAlias: String): SecretKey? = null

    override suspend fun get(
        masterKeyConfig: KeystoreManager.MasterKeyConfig,
        keyAliases: Collection<String>,
    ): Map<String, SecretKey> = emptyMap()

    override suspend fun store(masterKeyConfig: KeystoreManager.MasterKeyConfig, keyAlias: String, key: SecretKey) =
        Unit
}
