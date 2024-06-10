package com.tangem.operations.preflightread

import com.tangem.common.card.Card
import com.tangem.common.core.SessionEnvironment

/** Use this filter to filter out cards on preflight read stage */
interface PreflightReadFilter {

    /**
     * Method calls right after public information is read. User code is not required. If PreflightReadMode is set to
     * ReadCardOnly or FullCardRead
     *
     * @param card        card that was read
     * @param environment current environment
     */
    fun onCardRead(card: Card, environment: SessionEnvironment)

    /**
     * Method calls right after full card information is read. User code is required. If PreflightReadMode is set to
     * FullCardRead
     *
     * @param card        card that was read
     * @param environment current environment
     */
    fun onFullCardRead(card: Card, environment: SessionEnvironment)
}
