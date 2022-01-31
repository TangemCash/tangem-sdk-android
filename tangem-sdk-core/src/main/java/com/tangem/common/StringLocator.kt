package com.tangem.common

interface StringsLocator {
    fun getString(stringId: ID, vararg formatArgs: String): String

    enum class ID {
        backup_prepare_primary_card_message_format,
        backup_prepare_primary_card_message,
        backup_add_backup_card_message,
        backup_finalize_primary_card_message_format,
        backup_finalize_backup_card_message_format,

        reset_codes_scan_first_card,
        reset_codes_scan_confirmation_card,
        reset_codes_scan_to_reset,
        reset_codes_message_title_restore,
        reset_codes_message_title_backup,

        reset_codes_message_body_restore,
        reset_codes_message_body_restore_final,
        reset_codes_message_body_backup,
        reset_codes_success_message,

        common_success
    }
}