package com.tangem.common.authentication.keystore

import kotlin.time.Duration

internal enum class MasterKeyConfigs : KeystoreManager.MasterKeyConfig {
    V1 {
        override val alias = "master_key"
        override val securityDelay: Duration = with(Duration) { 5.seconds }
        override val userConfirmationRequired = true
    },
    V2 {
        override val alias = "master_key_v2"
        override val securityDelay: Duration = with(Duration) { 90.seconds }
        override val userConfirmationRequired = false
    }, ;

    val isDeprecated: Boolean
        get() = alias != current.alias

    companion object {

        val all: List<MasterKeyConfigs>
            get() = values().toList()

        val current: MasterKeyConfigs
            get() = all.last()
    }
}
