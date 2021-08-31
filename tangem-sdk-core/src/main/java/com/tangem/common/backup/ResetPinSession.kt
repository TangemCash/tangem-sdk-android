package com.tangem.common.backup

import com.tangem.common.card.Card

class ResetPin_CardToReset(
    var backupKey: ByteArray,
    var attestSignature: ByteArray?=null,
    var token: ByteArray?=null,
)

class ResetPin_CardToConfirm(
    var backupKey: ByteArray,
    var salt: ByteArray?=null,
    var authorizeSignature: ByteArray?=null,
)

class ResetPinSession (
    var cardToReset: ResetPin_CardToReset?=null,
    var cardToConfirm: ResetPin_CardToConfirm?=null,
    var newPIN: ByteArray?=null,
    var newPIN2: ByteArray?=null
)