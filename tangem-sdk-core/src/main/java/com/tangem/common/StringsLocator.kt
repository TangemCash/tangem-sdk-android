package com.tangem.common

interface StringsLocator {

    /**
     * Get string from resources
     * @param stringId [ID] of string resource
     * @param formatArgs string format arguments
     * @param defaultValue value to be returned in case the string with [stringId] is not found
     * */
    fun getString(stringId: ID, vararg formatArgs: Any, defaultValue: String = ""): String

    enum class ID {
        BACKUP_PREPARE_PRIMARY_CARD_MESSAGE_FORMAT,
        BACKUP_PREPARE_PRIMARY_CARD_MESSAGE,
        BACKUP_ADD_BACKUP_CARD_MESSAGE,
        BACKUP_FINALIZE_PRIMARY_CARD_MESSAGE_FORMAT,
        BACKUP_FINALIZE_BACKUP_CARD_MESSAGE_FORMAT,

        RESET_CODES_SCAN_FIRST_CARD,
        RESET_CODES_SCAN_CONFIRMATION_CARD,
        RESET_CODES_SCAN_TO_RESET,
        RESET_CODES_MESSAGE_TITLE_RESTORE,
        RESET_CODES_MESSAGE_TITLE_BACKUP,

        RESET_CODES_MESSAGE_BODY_RESTORE,
        RESET_CODES_MESSAGE_BODY_RESTORE_FINAL,
        RESET_CODES_MESSAGE_BODY_BACKUP,
        RESET_CODES_SUCCESS_MESSAGE,

        COMMON_SUCCESS,

        PIN_1,
        PIN_2,
        PIN_RESET_CODE_FORMAT,

        SIGN_MULTIPLE_CHUNKS_PART,
    }
}
