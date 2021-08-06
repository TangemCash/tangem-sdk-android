package com.tangem.common.json

import com.tangem.common.SuccessResponse
import com.tangem.common.card.Card
import com.tangem.common.core.CardSessionRunnable
import com.tangem.common.extensions.hexToBytes
import com.tangem.common.hdWallet.DerivationPath
import com.tangem.common.hdWallet.ExtendedPublicKey
import com.tangem.operations.DerivePublicKeyCommand
import com.tangem.operations.PreflightReadTask
import com.tangem.operations.ScanTask
import com.tangem.operations.personalization.DepersonalizeCommand
import com.tangem.operations.personalization.DepersonalizeResponse
import com.tangem.operations.personalization.PersonalizeCommand
import com.tangem.operations.pins.SetUserCodeCommand
import com.tangem.operations.sign.SignCommand
import com.tangem.operations.sign.SignHashCommand
import com.tangem.operations.sign.SignHashResponse
import com.tangem.operations.sign.SignResponse
import com.tangem.operations.wallet.CreateWalletCommand
import com.tangem.operations.wallet.CreateWalletResponse
import com.tangem.operations.wallet.PurgeWalletCommand

/**
 * Created by Anton Zhilenkov on 29/06/2021.
 */
class PersonalizeHandler : JSONRPCHandler<Card> {
    override val method: String = "PERSONALIZE"
    override val requiresCardId: Boolean = false

    override fun makeRunnable(params: Map<String, Any?>): CardSessionRunnable<Card> {
        return MoshiJsonConverter.INSTANCE.let {
            it.fromJson<PersonalizeCommand>(it.toJson(params))!!
        }
    }
}

class DepersonalizeHandler : JSONRPCHandler<DepersonalizeResponse> {
    override val method: String = "DEPERSONALIZE"
    override val requiresCardId: Boolean = false

    override fun makeRunnable(params: Map<String, Any?>): CardSessionRunnable<DepersonalizeResponse> {
        return DepersonalizeCommand()
    }
}

class PreflightReadHandler : JSONRPCHandler<Card> {
    override val method: String = "PREFLIGHT_READ"
    override val requiresCardId: Boolean = true

    override fun makeRunnable(params: Map<String, Any?>): CardSessionRunnable<Card> {
        return MoshiJsonConverter.INSTANCE.let {
            it.fromJson<PreflightReadTask>(it.toJson(params))!!
        }
    }
}

class ScanHandler : JSONRPCHandler<Card> {
    override val method: String = "SCAN"
    override val requiresCardId: Boolean = false

    override fun makeRunnable(params: Map<String, Any?>): CardSessionRunnable<Card> = ScanTask()
}

class CreateWalletHandler : JSONRPCHandler<CreateWalletResponse> {
    override val method: String = "CREATE_WALLET"
    override val requiresCardId: Boolean = true

    override fun makeRunnable(params: Map<String, Any?>): CardSessionRunnable<CreateWalletResponse> {
        return MoshiJsonConverter.INSTANCE.let {
            it.fromJson<CreateWalletCommand>(it.toJson(params))!!
        }
    }
}

class PurgeWalletHandler : JSONRPCHandler<SuccessResponse> {
    override val method: String = "PURGE_WALLET"
    override val requiresCardId: Boolean = true

    override fun makeRunnable(params: Map<String, Any?>): CardSessionRunnable<SuccessResponse> {
        return MoshiJsonConverter.INSTANCE.let {
            it.fromJson<PurgeWalletCommand>(it.toJson(params))!!
        }
    }
}

class SignHashHandler : JSONRPCHandler<SignHashResponse> {
    override val method: String = "SIGN_HASH"
    override val requiresCardId: Boolean = true

    override fun makeRunnable(params: Map<String, Any?>): CardSessionRunnable<SignHashResponse> {
        val hash = (params["hash"] as String).hexToBytes()
        val publicKey = (params["walletPublicKey"] as String).hexToBytes()
        val hdPath: DerivationPath? = (params["hdPath"] as? String)?.let { DerivationPath(it) }

        return SignHashCommand(hash, publicKey, hdPath)
    }
}

class SignHashesHandler : JSONRPCHandler<SignResponse> {
    override val method: String = "SIGN_HASHES"
    override val requiresCardId: Boolean = true

    override fun makeRunnable(params: Map<String, Any?>): CardSessionRunnable<SignResponse> {
        val hashes = (params["hashes"] as List<String>).map { it.hexToBytes() }
        val publicKey = (params["walletPublicKey"] as String).hexToBytes()
        val hdPath: DerivationPath? = (params["hdPath"] as? String)?.let { DerivationPath(it) }

        return SignCommand(hashes.toTypedArray(), publicKey, hdPath)
    }
}

class SetAccessCodeHandler : JSONRPCHandler<SuccessResponse> {
    override val method: String = "SET_ACCESSCODE"
    override val requiresCardId: Boolean = true

    override fun makeRunnable(params: Map<String, Any?>): CardSessionRunnable<SuccessResponse> {
        return SetUserCodeCommand.changeAccessCode(params["accessCode"] as String)
    }
}

class SetPasscodeHandler : JSONRPCHandler<SuccessResponse> {
    override val method: String = "SET_PASSCODE"
    override val requiresCardId: Boolean = true

    override fun makeRunnable(params: Map<String, Any?>): CardSessionRunnable<SuccessResponse> {
        return SetUserCodeCommand.changePasscode(params["passcode"] as String)
    }
}

class ResetUserCodesHandler : JSONRPCHandler<SuccessResponse> {
    override val method: String = "RESET_USERCODES"
    override val requiresCardId: Boolean = true

    override fun makeRunnable(params: Map<String, Any?>): CardSessionRunnable<SuccessResponse> {
        return SetUserCodeCommand.resetUserCodes()
    }
}

class DerivePublicKeyHandler : JSONRPCHandler<ExtendedPublicKey> {
    override val method: String = "DERIVE_PUBLIC_KEY"
    override val requiresCardId: Boolean = true

    override fun makeRunnable(params: Map<String, Any?>): CardSessionRunnable<ExtendedPublicKey> {
        val publicKey = (params["walletPublicKey"] as String).hexToBytes()
        val hdPath = DerivationPath((params["hdPath"] as String))
        return DerivePublicKeyCommand(publicKey, hdPath)
    }
}

