package com.tangem.common.backup

import com.tangem.common.card.Card

class BackupMaster(
    var backupKey: ByteArray,
    var cardKey: ByteArray,
    var certificate: ByteArray?=null
)

class BackupSlave(
    var backupKey: ByteArray,
    var cardKey: ByteArray,
    var attestSignature: ByteArray,
    var certificate: ByteArray?=null,
    var encryptionSalt: ByteArray?=null,
    var encryptedData: ByteArray?=null,
    var state: Card.BackupStatus=Card.BackupStatus.NoBackup
)

class BackupSession (
    var master: BackupMaster,
    var slaves: MutableMap<String,BackupSlave> = mutableMapOf(),
    var attestSignature: ByteArray?=null,
    var newPIN: ByteArray?=null,
    var newPIN2: ByteArray?=null
)