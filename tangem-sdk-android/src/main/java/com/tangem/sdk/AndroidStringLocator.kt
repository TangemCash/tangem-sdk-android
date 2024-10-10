package com.tangem.sdk

import android.content.Context
import com.tangem.Log
import com.tangem.common.StringsLocator
import java.lang.reflect.Field

class AndroidStringLocator(val context: Context) : StringsLocator {

    override fun getString(stringId: StringsLocator.ID, vararg formatArgs: Any, defaultValue: String): String {
        return runCatching {
            val idField = getField(stringId)
            idField.getInt(idField)
        }
            .map { context.getString(it, *formatArgs) }
            .onFailure { e ->
                Log.error {
                    """
                        Unable to find string
                        |- ID: $stringId
                        |- Args: $formatArgs
                        |- Cause: ${e.localizedMessage}
                    """.trimIndent()
                }
            }
            .getOrDefault(defaultValue)
    }

    // Because of string IDs mapping
    @Suppress("CyclomaticComplexMethod")
    private fun getField(stringId: StringsLocator.ID): Field {
        val resourceId = when (stringId) {
            StringsLocator.ID.BACKUP_PREPARE_PRIMARY_CARD_MESSAGE_FORMAT -> "backup_prepare_primary_card_message_format"
            StringsLocator.ID.BACKUP_PREPARE_PRIMARY_CARD_MESSAGE -> "backup_prepare_primary_card_message"
            StringsLocator.ID.BACKUP_ADD_BACKUP_CARD_MESSAGE -> "backup_add_backup_card_message"
            StringsLocator.ID.BACKUP_FINALIZE_PRIMARY_CARD_MESSAGE_FORMAT ->
                "backup_finalize_primary_card_message_format"
            StringsLocator.ID.BACKUP_FINALIZE_PRIMARY_RING_MESSAGE -> "backup_finalize_primary_ring_message"
            StringsLocator.ID.BACKUP_FINALIZE_BACKUP_CARD_MESSAGE_FORMAT -> "backup_finalize_backup_card_message_format"
            StringsLocator.ID.BACKUP_FINALIZE_BACKUP_RING_MESSAGE -> "backup_finalize_backup_ring_message"
            StringsLocator.ID.RESET_CODES_SCAN_FIRST_CARD -> "reset_codes_scan_first_card"
            StringsLocator.ID.RESET_CODES_SCAN_CONFIRMATION_CARD -> "reset_codes_scan_confirmation_card"
            StringsLocator.ID.RESET_CODES_SCAN_TO_RESET -> "reset_codes_scan_to_reset"
            StringsLocator.ID.RESET_CODES_MESSAGE_TITLE_RESTORE -> "reset_codes_message_title_restore"
            StringsLocator.ID.RESET_CODES_MESSAGE_TITLE_BACKUP -> "reset_codes_message_title_backup"
            StringsLocator.ID.RESET_CODES_MESSAGE_BODY_RESTORE -> "reset_codes_message_body_restore"
            StringsLocator.ID.RESET_CODES_MESSAGE_BODY_RESTORE_FINAL -> "reset_codes_message_body_restore_final"
            StringsLocator.ID.RESET_CODES_MESSAGE_BODY_BACKUP -> "reset_codes_message_body_backup"
            StringsLocator.ID.RESET_CODES_SUCCESS_MESSAGE -> "reset_codes_success_message"
            StringsLocator.ID.COMMON_SUCCESS -> "common_success"
            StringsLocator.ID.PIN_1 -> "pin1"
            StringsLocator.ID.PIN_2 -> "pin2"
            StringsLocator.ID.PIN_RESET_CODE_FORMAT -> "pin_reset_code_format"
            StringsLocator.ID.SIGN_MULTIPLE_CHUNKS_PART -> "sign_multiple_chunks_part"
            StringsLocator.ID.VIEW_DELEGATE_SCAN_DESCRIPTION_FORMAT -> "view_delegate_scan_description_format"
            StringsLocator.ID.VIEW_DELEGATE_SECURITY_DELAY_DESCRIPTION_FORMAT -> {
                "view_delegate_security_delay_description_format"
            }
        }

        return R.string::class.java.getDeclaredField(resourceId)
    }
}
