package com.tangem.common

interface StringsLocator {
    fun getString(stringId: ID, vararg formatArgs: String): String

    enum class ID {
        backup_prepare_primary_card_message_format,
        backup_prepare_primary_card_message,
        backup_add_backup_card_message,
        backup_finalize_primary_card_message_format,
        backup_finalize_backup_card_message_format
    }
}