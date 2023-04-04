package com.tangem.common.json

import com.tangem.common.SuccessResponse
import com.tangem.common.card.Card
import com.tangem.common.core.CardSessionRunnable
import com.tangem.common.extensions.hexToBytes
import com.tangem.common.hdWallet.DerivationPath
import com.tangem.common.hdWallet.ExtendedPublicKey
import com.tangem.common.hdWallet.HDWalletError
import com.tangem.operations.PreflightReadTask
import com.tangem.operations.ScanTask
import com.tangem.operations.derivation.DeriveWalletPublicKeyTask
import com.tangem.operations.derivation.DeriveWalletPublicKeysTask
import com.tangem.operations.derivation.ExtendedPublicKeysMap
import com.tangem.operations.files.ChangeFileSettingsTask
import com.tangem.operations.files.DeleteFilesTask
import com.tangem.operations.files.File
import com.tangem.operations.files.FileVisibility
import com.tangem.operations.files.ReadFilesTask
import com.tangem.operations.files.WriteFilesResponse
import com.tangem.operations.files.WriteFilesTask
import com.tangem.operations.personalization.DepersonalizeCommand
import com.tangem.operations.personalization.DepersonalizeResponse
import com.tangem.operations.personalization.PersonalizeCommand
import com.tangem.operations.pins.SetUserCodeCommand
import com.tangem.operations.sign.SignCommand
import com.tangem.operations.sign.SignHashCommand
import com.tangem.operations.sign.SignHashResponse
import com.tangem.operations.sign.SignResponse
import com.tangem.operations.wallet.CreateWalletResponse
import com.tangem.operations.wallet.CreateWalletTask
import com.tangem.operations.wallet.PurgeWalletCommand
import java.util.Locale

/**
 * Created by Anton Zhilenkov on 29/06/2021.
 */
private inline fun <reified C> make(params: Map<String, Any?>): C {
    return MoshiJsonConverter.INSTANCE.let { it.fromJson<C>(it.toJson(params))!! }
}

class PersonalizeHandler : JSONRPCHandler<Card> {
    override val method: String = "PERSONALIZE"

    override fun makeRunnable(params: Map<String, Any?>): CardSessionRunnable<Card> {
        return make<PersonalizeCommand>(params)
    }
}

class DepersonalizeHandler : JSONRPCHandler<DepersonalizeResponse> {
    override val method: String = "DEPERSONALIZE"

    override fun makeRunnable(params: Map<String, Any?>): CardSessionRunnable<DepersonalizeResponse> {
        return make<DepersonalizeCommand>(params)
    }
}

class PreflightReadHandler : JSONRPCHandler<Card> {
    override val method: String = "PREFLIGHT_READ"

    override fun makeRunnable(params: Map<String, Any?>): CardSessionRunnable<Card> {
        return make<PreflightReadTask>(params)
    }
}

class ScanHandler : JSONRPCHandler<Card> {
    override val method: String = "SCAN"

    override fun makeRunnable(params: Map<String, Any?>): CardSessionRunnable<Card> = ScanTask()
}

class CreateWalletHandler : JSONRPCHandler<CreateWalletResponse> {
    override val method: String = "CREATE_WALLET"

    override fun makeRunnable(params: Map<String, Any?>): CardSessionRunnable<CreateWalletResponse> {
        return make<CreateWalletTask>(params)
    }
}

class PurgeWalletHandler : JSONRPCHandler<SuccessResponse> {
    override val method: String = "PURGE_WALLET"

    override fun makeRunnable(params: Map<String, Any?>): CardSessionRunnable<SuccessResponse> {
        return make<PurgeWalletCommand>(params)
    }
}

class SignHashHandler : JSONRPCHandler<SignHashResponse> {
    override val method: String = "SIGN_HASH"

    override fun makeRunnable(params: Map<String, Any?>): CardSessionRunnable<SignHashResponse> {
        val hash = (params["hash"] as String).hexToBytes()
        val publicKey = (params["walletPublicKey"] as String).hexToBytes()
        val derivationPath: DerivationPath? = (params["derivationPath"] as? String)?.let {
            try {
                DerivationPath(it)
            } catch (ex: HDWalletError) {
                ex.printStackTrace()
                null
            }
        }

        return SignHashCommand(hash, publicKey, derivationPath)
    }
}

class SignHashesHandler : JSONRPCHandler<SignResponse> {
    override val method: String = "SIGN_HASHES"

    override fun makeRunnable(params: Map<String, Any?>): CardSessionRunnable<SignResponse> {
        val hashes = (params["hashes"] as List<String>).map { it.hexToBytes() }
        val publicKey = (params["walletPublicKey"] as String).hexToBytes()
        val derivationPath: DerivationPath? = (params["derivationPath"] as? String)?.let {
            try {
                DerivationPath(it)
            } catch (ex: HDWalletError) {
                ex.printStackTrace()
                null
            }
        }

        return SignCommand(hashes.toTypedArray(), publicKey, derivationPath)
    }
}

class DeriveWalletPublicKeyHandler : JSONRPCHandler<ExtendedPublicKey> {
    override val method: String = "DERIVE_WALLET_PUBLIC_KEY"

    override fun makeRunnable(params: Map<String, Any?>): CardSessionRunnable<ExtendedPublicKey> {
        val walletPublicKey: ByteArray = (params["walletPublicKey"] as String).hexToBytes()
        val rawDerivationPath: String = params["derivationPath"] as String
        val derivationPath = DerivationPath(rawDerivationPath)

        return DeriveWalletPublicKeyTask(walletPublicKey, derivationPath)
    }
}

class DeriveWalletPublicKeysHandler : JSONRPCHandler<ExtendedPublicKeysMap> {
    override val method: String = "DERIVE_WALLET_PUBLIC_KEYS"

    override fun makeRunnable(params: Map<String, Any?>): CardSessionRunnable<ExtendedPublicKeysMap> {
        val walletPublicKey: ByteArray = (params["walletPublicKey"] as String).hexToBytes()
        val rawDerivationPaths: List<String> = params["derivationPaths"] as List<String>
        val derivationPaths: List<DerivationPath> = rawDerivationPaths.map { DerivationPath(it) }

        return DeriveWalletPublicKeysTask(walletPublicKey, derivationPaths)
    }
}

class SetAccessCodeHandler : JSONRPCHandler<SuccessResponse> {
    override val method: String = "SET_ACCESSCODE"

    override fun makeRunnable(params: Map<String, Any?>): CardSessionRunnable<SuccessResponse> {
        return SetUserCodeCommand.changeAccessCode(params["accessCode"] as? String)
    }
}

class SetPasscodeHandler : JSONRPCHandler<SuccessResponse> {
    override val method: String = "SET_PASSCODE"

    override fun makeRunnable(params: Map<String, Any?>): CardSessionRunnable<SuccessResponse> {
        return SetUserCodeCommand.changePasscode(params["passcode"] as? String)
    }
}

class ResetUserCodesHandler : JSONRPCHandler<SuccessResponse> {
    override val method: String = "RESET_USERCODES"

    override fun makeRunnable(params: Map<String, Any?>): CardSessionRunnable<SuccessResponse> {
        return SetUserCodeCommand.resetUserCodes()
    }
}

class ReadFilesHandler : JSONRPCHandler<List<File>> {
    override val method: String = "READ_FILES"

    override fun makeRunnable(params: Map<String, Any?>): CardSessionRunnable<List<File>> {
        val task = make<ReadFilesTask>(params)
        task.shouldReadPrivateFiles = params["readPrivateFiles"] as? Boolean ?: false

        return task
    }
}

class WriteFilesHandler : JSONRPCHandler<WriteFilesResponse> {
    override val method: String = "WRITE_FILES"

    override fun makeRunnable(params: Map<String, Any?>): CardSessionRunnable<WriteFilesResponse> {
        return make<WriteFilesTask>(params)
    }
}

class DeleteFilesHandler : JSONRPCHandler<SuccessResponse> {
    override val method: String = "DELETE_FILES"

    override fun makeRunnable(params: Map<String, Any?>): CardSessionRunnable<SuccessResponse> {
        val indices = params["indices"] as? List<Int>
        return DeleteFilesTask(indices)
    }
}

class ChangeFileSettingsHandler : JSONRPCHandler<SuccessResponse> {
    override val method: String = "CHANGE_FILE_SETTINGS"

    override fun makeRunnable(params: Map<String, Any?>): CardSessionRunnable<SuccessResponse> {
        val changes = (params["changes"] as Map<String, String>).entries.associate {
            it.key.toInt() to FileVisibility.valueOf(it.value.capitalize(Locale.getDefault()))
        }
        return ChangeFileSettingsTask(changes)
    }
}
