package com.tangem.jvm.demo

import org.apache.commons.cli.Option
import org.apache.commons.cli.Options

enum class TangemCommandOptions(val option: Option) {

    Verbose(Option("v", "verbose", false, "provide detailed logs")),
    TerminalNumber(Option("t", "terminal", false, "terminal number")),
    Hashes(Option("hashes", true, "hashes to sign")),
    CardId(Option("cid", "cardid", true, "card id")),
    Wallet(Option("wallet", true, "index or public key (hex) of the selected wallet")),
    FileIndices(Option("indices", true, "indices of files")),
    ReadPrivateFiles(Option("privatefiles", false, "should the command read private files")),
    Files(Option("files", true, "files as array of bytes")),
    Encryption(Option("encryption", true, "encryption mode")),
    Passphrase(Option("passphrase", true, "Passphrase for wallet generation from the Seed Phrase")),
    Curve(Option("curve", true, "Desired elliptic curve for the wallet (secp256k1 or secp for secp256k1 and ed25519 or ed for ed25519)")),
    WalletHdPath(Option("path", true, "Specify as m/0'/1'/0, max 5 segments")),
    WalletTweak(Option("tweak", true, "Additive tweak value")),
    SessionKeyA(Option("key", true, "Public key A for a shared secret calculation")),
    ;

    val opt = option.opt
}

fun Command.generateOptions(): Options {
    val options = Options()
    options.addOption(TangemCommandOptions.Verbose.option)
    options.addOption(TangemCommandOptions.TerminalNumber.option)
    options.addOption(TangemCommandOptions.Encryption.option)

    when (this) {
        Command.Read -> {}
        Command.ReadWallets -> {}
        Command.ReadWallet -> {
            options.addOption(TangemCommandOptions.Wallet.option)
            options.addOption(TangemCommandOptions.CardId.option)
            options.addOption(TangemCommandOptions.WalletHdPath.option)
            options.addOption(TangemCommandOptions.WalletTweak.option)
        }
        Command.CalculateSharedSecret -> {
            options.addOption(TangemCommandOptions.SessionKeyA.option)
            options.addOption(TangemCommandOptions.Wallet.option)
            options.addOption(TangemCommandOptions.CardId.option)
            options.addOption(TangemCommandOptions.WalletHdPath.option)
            options.addOption(TangemCommandOptions.WalletTweak.option)
        }
        Command.SetPasscode -> {}
        Command.SetAccessCode -> {}
        Command.Sign -> {
            options.addOption(TangemCommandOptions.Hashes.option)
            options.addOption(TangemCommandOptions.Wallet.option)
            options.addOption(TangemCommandOptions.CardId.option)
            options.addOption(TangemCommandOptions.WalletHdPath.option)
            options.addOption(TangemCommandOptions.WalletTweak.option)
        }
        Command.ReadFiles -> {
            options.addOption(TangemCommandOptions.CardId.option)
            options.addOption(TangemCommandOptions.FileIndices.option)
            options.addOption(TangemCommandOptions.ReadPrivateFiles.option)
        }
        Command.WriteFiles -> {
            options.addOption(TangemCommandOptions.CardId.option)
            options.addOption(TangemCommandOptions.Files.option)
        }
        Command.DeleteFiles -> {
            options.addOption(TangemCommandOptions.CardId.option)
            options.addOption(TangemCommandOptions.FileIndices.option)
        }

        Command.CreateWallet -> {
            options.addOption(TangemCommandOptions.CardId.option)
            options.addOption(TangemCommandOptions.Passphrase.option)
            options.addOption(TangemCommandOptions.Curve.option)
        }
        Command.PurgeWallet -> {
            options.addOption(TangemCommandOptions.Wallet.option)
            options.addOption(TangemCommandOptions.CardId.option)
        }
    }
    return options
}

