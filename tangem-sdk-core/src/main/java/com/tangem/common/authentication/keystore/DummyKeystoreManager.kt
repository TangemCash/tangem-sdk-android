package com.tangem.common.authentication.keystore

import javax.crypto.SecretKey

class DummyKeystoreManager : KeystoreManager {

    override suspend fun get(
        masterKeyConfig: KeystoreManager.MasterKeyConfig,
        keyAlias: String,
        forceAuthentication: Boolean,
    ): SecretKey? = null

    override suspend fun get(
        masterKeyConfig: KeystoreManager.MasterKeyConfig,
        keyAliases: Set<String>,
        forceAuthentication: Boolean,
    ): Map<String, SecretKey> = emptyMap()

    override suspend fun store(masterKeyConfig: KeystoreManager.MasterKeyConfig, keyAlias: String, key: SecretKey) =
        Unit
}
