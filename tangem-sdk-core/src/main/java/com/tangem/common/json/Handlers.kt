package com.tangem.common.json

import com.tangem.common.card.Card
import com.tangem.common.card.EllipticCurve
import com.tangem.common.core.CardSessionRunnable
import com.tangem.common.extensions.cast
import com.tangem.common.extensions.hexToBytes
import com.tangem.operations.ScanTask
import com.tangem.operations.personalization.DepersonalizeCommand
import com.tangem.operations.personalization.DepersonalizeResponse
import com.tangem.operations.personalization.PersonalizeCommand
import com.tangem.operations.sign.SignCommand
import com.tangem.operations.sign.SignHashCommand
import com.tangem.operations.sign.SignHashResponse
import com.tangem.operations.sign.SignResponse
import com.tangem.operations.wallet.CreateWalletCommand
import com.tangem.operations.wallet.CreateWalletResponse

/**
 * Created by Anton Zhilenkov on 29/06/2021.
 */

class ScanHandler : JSONRPCHandler<Card> {
    override val method: String = "SCAN"
    override val requiresCardId: Boolean = false

    override fun makeRunnable(params: Map<String, Any?>): CardSessionRunnable<Card> = ScanTask()
}

class SignHashesHandler : JSONRPCHandler<SignResponse> {
    override val method: String = "SIGN_HASHES"
    override val requiresCardId: Boolean = true

    override fun makeRunnable(params: Map<String, Any?>): CardSessionRunnable<SignResponse> {
        val hashes = params["hashes"]!!.cast<List<String>>().map { it.hexToBytes() }.toTypedArray()
        val publicKey = params["walletPublicKey"]!!.cast<String>().hexToBytes()

        return SignCommand(hashes, publicKey)
    }
}

class SignHashHandler : JSONRPCHandler<SignHashResponse> {
    override val method: String = "SIGN_HASH"
    override val requiresCardId: Boolean = true

    override fun makeRunnable(params: Map<String, Any?>): CardSessionRunnable<SignHashResponse> {
        val hash = params["hash"]!!.cast<String>().hexToBytes()
        val publicKey = params["walletPublicKey"]!!.cast<String>().hexToBytes()

        return SignHashCommand(hash, publicKey)
    }
}

class CreateWalletHandler : JSONRPCHandler<CreateWalletResponse> {
    override val method: String = "CREATE_WALLET"
    override val requiresCardId: Boolean = true

    override fun makeRunnable(params: Map<String, Any?>): CardSessionRunnable<CreateWalletResponse> {
        val curve = EllipticCurve.byName(params["curve"]!!.cast())
        return CreateWalletCommand(curve!!, params["isPermanent"]!!.cast())
    }
}

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

//TODO: create other JSONRPC handlers
val doNotForgetList = listOf<String>(
        "CREATE_WALLET",
        "PURGE_WALLET",
        "SET_PIN1",
        "SET_PIN2",
        "PREFLIGHT_READ",
)

/*

@available(iOS 13.0, *)
class PurgeWalletHandler: JSONRPCHandler {
    var method: String { "PURGE_WALLET" }
    var requiresCardId: Bool { true }

    func makeRunnable(from parameters: [String : Any]) throws -> AnyJSONRPCRunnable {
        let walletPublicKey: Data = try parameters.value(for: "walletPublicKey")
            let command = PurgeWalletCommand(publicKey: walletPublicKey)
            return command.eraseToAnyRunnable()
        }
}

@available(iOS 13.0, *)
class SetAccessCodeHandler: JSONRPCHandler {
    var method: String { "SET_ACCESSCODE" }

    var requiresCardId: Bool { true }

    func makeRunnable(from parameters: [String : Any]) throws -> AnyJSONRPCRunnable {
        let accessCode: String? = try parameters.value(for: "accessCode")
            let command = SetUserCodeCommand(accessCode: accessCode)
            return command.eraseToAnyRunnable()
        }
}

@available(iOS 13.0, *)
class SetPasscodeHandler: JSONRPCHandler {
    var method: String { "SET_PASSCODE" }

    var requiresCardId: Bool { true }

    func makeRunnable(from parameters: [String : Any]) throws -> AnyJSONRPCRunnable {
        let passcode: String? = try parameters.value(for: "passcode")
            let command = SetUserCodeCommand(passcode: passcode)
            return command.eraseToAnyRunnable()
        }
}

@available(iOS 13.0, *)
class ResetUserCodesHandler: JSONRPCHandler {
    var method: String { "RESET_USERCODES" }

    var requiresCardId: Bool { true }

    func makeRunnable(from parameters: [String : Any]) throws -> AnyJSONRPCRunnable {
        return SetUserCodeCommand.resetUserCodes.eraseToAnyRunnable()
    }
}

/// Runs PreflightReadTask in `fullCardRead` mode
@available(iOS 13.0, *)
class PreflightReadHandler: JSONRPCHandler {
    var method: String { "PREFLIGHT_READ" }

    var requiresCardId: Bool { false }

    func makeRunnable(from parameters: [String : Any]) throws -> AnyJSONRPCRunnable {
        let cardId: String? = try parameters.value(for: "cardId")
            let mode: PreflightReadMode = try parameters.value(for: "readMode")
                let command = PreflightReadTask(readMode: mode, cardId: cardId)
                return command.eraseToAnyRunnable()
            }
}
 */