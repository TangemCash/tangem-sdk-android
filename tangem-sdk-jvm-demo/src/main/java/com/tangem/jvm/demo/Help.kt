package com.tangem.jvm.demo

const val GENERAL_HELP = """
Tangem CLI
    -t,--terminal         Number of connected terminal, if you have more then one
    -v,--verbose          Make the operation more talkative
List of commands:

    read
    Obtain all data about the card and the wallet, including unique card number (CID) that has to be submitted while calling all other commands.
  
    readwallets
    Obtain all data about the wallets on the card.

    readwallet
    Obtain all data about a specified wallet on the card.
      -cid,     <hex_string>            Card ID (CID)
      -wallet,  <hex_string or Integer> Index or public key (in hex) of the selected wallet
      -path     <string>                HD Path - Specify as m/0'/1'/0, max 5 segments
      -tweak    <hex_strings>           Additive tweak value
    
    createwallet
    Сreate a new wallet on the card. 
    A key pair Wallet_PublicKey / Wallet_PrivateKey will be generated and securely stored in the card. 
      -cid,         <hex_string>    Card ID (CID)
      -curve,       <string>        Desired elliptic curve for the wallet (specify as secp256k1 or secp for secp256k1 and ed25519 or ed for ed25519)
      -passphrase,  <string>        Passphrase for wallet generation from the Seed Phrase
    
    purgewallet
    Delete all wallet data.
      -cid,     <hex_string>                   Card ID (CID)
      -wallet,  <hex_string or Integer>        Index or public key (in hex) of the selected wallet
      
    setpasscode
    Sets passcode (PIN2) to the card. A passcode value should be inputted into the terminal when prompted.

    setaccesscode
    Sets accesscode (PIN1) to the card. A passcode value should be inputted into the terminal when prompted.

    resetcodes
    Resets all user codes (PIN1 and PIN2) on the card to the default. Current codes should be inputted into the terminal when prompted.

    sign
    Sign the following one or several data hashes separated by commas using Wallet's Private Key. 
      -cid,     <hex_string>        Card ID (CID)
      -hashes   <hex_strings>       One or several data hashes separated by commas, hashes should be he same size
      -path     <string>            HD Path - Specify as m/0'/1'/0, max 5 segments
      -tweak    <hex_strings>       Additive tweak value
          
    secret
    Generates a shared secret.
      -key,     <hex_string>            Session Key A: Public key A for a shared secret calculation
      -cid,     <hex_string>            Card ID (CID)
      -wallet,  <hex_string or Integer> Index or public key (in hex) of the selected wallet
      -path     <string>                HD Path - Specify as m/0'/1'/0, max 5 segments
      -tweak    <hex_strings>           Additive tweak value
         
"""