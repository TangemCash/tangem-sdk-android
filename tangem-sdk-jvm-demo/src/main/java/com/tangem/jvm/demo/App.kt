package com.tangem.jvm.demo

import kotlinx.coroutines.runBlocking
import org.apache.commons.cli.*
import org.apache.commons.cli.HelpFormatter





fun main(args: Array<String>) {
    System.setProperty("sun.security.smartcardio.library", "/System/Library/Frameworks/PCSC.framework/Versions/Current/PCSC")

    val command = Command.byValue(args.first())
    if (command == null) {
        if (args.contains("-h") || args.contains("--help")) {
            val formatter = HelpFormatter()
            formatter.printHelp(GENERAL_HELP, Options())
            return
        }
        println("Please specify valid command")
        return
    }

    val cmd: CommandLine? = createParsedCommandLine(args, command)
    if (cmd == null) {
        println("Please enter correct options for a command")
        return
    }

    val verbose = cmd.hasOption(TangemCommandOptions.Verbose.opt)

    val indexOfTerminal = cmd.getOptionValue(TangemCommandOptions.TerminalNumber.opt)?.toIntOrNull()

    val tangemSdkDesktop = TangemSdkCli(verbose, indexOfTerminal, cmd)

     tangemSdkDesktop.execute(command)
}

fun createParsedCommandLine(args: Array<String>, command: Command): CommandLine? {
    val options = command.generateOptions()
    val parser: CommandLineParser = DefaultParser()
    return try {
        parser.parse(options, args)
    } catch (exception: Exception) {
        null
    }
}
