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

    val isCurrent: Boolean
        get() = alias == current.alias

    companion object {

        val all: List<MasterKeyConfigs> = values().toList()

        val current: MasterKeyConfigs = all.last()

        val old: List<MasterKeyConfigs> = all.dropLast(n = 1)
    }
}
