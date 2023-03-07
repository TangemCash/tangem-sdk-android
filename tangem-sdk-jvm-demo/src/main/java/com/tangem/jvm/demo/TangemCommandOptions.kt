package com.tangem.jvm.demo

import org.apache.commons.cli.Option
import org.apache.commons.cli.Options

enum class TangemCommandOptions(val option: Option) {

    Verbose(Option("v", "verbose", false, "provide detailed logs")),
    TerminalNumber(Option("t", "terminal", false, "terminal number")),
    Hashes(Option("hashes", true, "hashes to sign")),
    CardId(Option("cid", "cardid", true, "card id")),
    WalletIndex(Option("wi", "walletIndex", true, "index of the selected wallet")),
    FileIndices(Option("indices", true, "indices of files")),
    ReadPrivateFiles(Option("privatefiles", false, "should the command read private files")),
    Files(Option("files", true, "files as array of bytes"));

    val opt = option.opt
}

fun Command.generateOptions(): Options {
    val options = Options()
    options.addOption(TangemCommandOptions.Verbose.option)
    options.addOption(TangemCommandOptions.TerminalNumber.option)

    when (this) {
        Command.Read -> {}
        Command.Sign -> {
            options.addOption(TangemCommandOptions.Hashes.option)
            options.addOption(TangemCommandOptions.WalletIndex.option)
            options.addOption(TangemCommandOptions.CardId.option)
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
        }
        Command.PurgeWallet -> {
            options.addOption(TangemCommandOptions.WalletIndex.option)
            options.addOption(TangemCommandOptions.CardId.option)
        }
    }
    return options
}
