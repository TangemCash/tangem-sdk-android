package com.tangem.common.extensions

import com.tangem.common.card.Card
import com.tangem.common.card.FirmwareVersion

fun Card.getType(): FirmwareVersion.FirmwareType = firmwareVersion.type
