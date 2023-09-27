package com.tangem.common.authentication

import javax.crypto.SecretKey

class DummyKeystoreManager : KeystoreManager {

    override suspend fun authenticateAndGetKey(keyAlias: String): SecretKey? = null

    override suspend fun storeKey(keyAlias: String, key: SecretKey) = Unit
}
