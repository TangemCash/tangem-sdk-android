package com.tangem.tangem_sdk_new.extensions

import androidx.activity.ComponentActivity
import com.tangem.TangemSdk
import com.tangem.TangemSdkImpl
import com.tangem.common.json.MoshiJsonConverter
import com.tangem.common.services.secure.SecureStorage
import com.tangem.operations.backup.BackupRepo
import com.tangem.operations.backup.BackupService
import com.tangem.tangem_sdk_new.AndroidStringLocator
import com.tangem.tangem_sdk_new.storage.create

fun BackupService.Companion.init(sdk: TangemSdk, activity: ComponentActivity): BackupService {
    return BackupService(
        sdk,
        BackupRepo(SecureStorage.create(activity), MoshiJsonConverter.INSTANCE),
        AndroidStringLocator(activity)
    )
}