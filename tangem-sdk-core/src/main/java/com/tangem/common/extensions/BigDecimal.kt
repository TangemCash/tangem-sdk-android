package com.tangem.common.extensions

import java.math.BigDecimal

fun BigDecimal.isZero(): Boolean {
    return this.compareTo(BigDecimal.ZERO) == 0
}