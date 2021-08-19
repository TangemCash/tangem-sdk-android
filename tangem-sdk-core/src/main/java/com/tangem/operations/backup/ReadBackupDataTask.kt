package com.tangem.operations.backup

import com.squareup.moshi.JsonClass
import com.tangem.common.CompletionResult
import com.tangem.common.backup.BackupSession
import com.tangem.common.backup.BackupSlave
import com.tangem.common.core.CardSession
import com.tangem.common.core.CardSessionRunnable
import com.tangem.common.core.CompletionCallback
import com.tangem.operations.CommandResponse

@JsonClass(generateAdapter = true)
class ReadBackupDataTaskResponse(
    val slaves: MutableMap<String,BackupSlave>
) : CommandResponse

class ReadBackupDataTask(
    private val backupSession: BackupSession
) : CardSessionRunnable<ReadBackupDataTaskResponse> {

    private var index = 0
    private val slaves = mutableMapOf<String,BackupSlave>()

    override fun run(session: CardSession, callback: CompletionCallback<ReadBackupDataTaskResponse>) {
        readAllBackupData(session, callback)
    }

    private fun readAllBackupData(session: CardSession, callback: CompletionCallback<ReadBackupDataTaskResponse>) {
        val command = ReadBackupDataCommand(backupSession, backupSession.slaves.toList()[index].second.backupKey)
        command.run(session) { result ->
            when (result) {
                is CompletionResult.Success -> {
                    val slave=backupSession.slaves.toList()[index].second
                    val slaveCardId=backupSession.slaves.toList()[index].first
                    slave.encryptionSalt=result.data.encryptionSalt
                    slave.encryptedData=result.data.encryptedData
                    slaves[slaveCardId]=slave
                    index++
                    if( index>=backupSession.slaves.count() )
                    {
                        callback(CompletionResult.Success(ReadBackupDataTaskResponse(slaves)))
                    }else {
                        readAllBackupData(session, callback)
                    }
                }
                is CompletionResult.Failure -> {
                        callback(CompletionResult.Failure(result.error))
                }
            }
        }
    }
}