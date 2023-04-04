package com.tangem.jvm.demo

const val GENERAL_HELP = """
Tangem CLI
    -t,--terminal         Number of connected terminal, if you have more then one
    -v,--verbose          Make the operation more talkative
List of commands:
    read
    Obtain all data about the card and the wallet, including unique card number (CID) that has to be submitted while calling all other commands.
    
    sign
    Sign the following one or several data hashes separated by commas using Wallet's Private Key. 
      -cid,     <hex_string>        Card ID (CID)
      -hashes   <hex_strings>       One or several data hashes separated by commas, hashes should be he same size
    
    createwallet
    Сreate a new wallet on the card having Empty state. A key pair Wallet_PublicKey / Wallet_PrivateKey will be generated and securely stored in the card. 
      -cid,     <hex_string>        Card ID (CID)
    
    purgewallet
    Delete all wallet data.
      -cid,     <hex_string>        Card ID (CID)
    
    readissuerdata
    Read 512-byte Issuer_Data field and its issuer’s signature.
      -cid,     <hex_string>        Card ID (CID)
    
    readissuerextradata
    Read Issuer_Extra_Data field and its issuer’s signature.
      -cid,     <hex_string>        Card ID (CID)
    
    writeissuerdata
    Writte up to 512-byte Data signed by Issuer Key
      -cid,     <hex_string>        Card ID (CID)
      -data     <issuer_data>       Issuer data to be written
      -counter  <int>               (Optional) Counter to be written
      -sig      <signature_hex>     Issuer’s signature SHA256(CID | Data [| Issuer_Data_Counter]) with Issuer Data Private Key 
      
    writeissuerextradata
    Writte up to 32 kbytes Exra Data signed by Issuer Key
      -cid,     <hex_string>        Card ID (CID)
      -data     <issuer_data>       Issuer data to be written
      -counter  <int>               (Optional) Counter to be written
      -startsig <signature_hex>     Issuer’s signature SHA256(CID | [| Issuer_Data_Counter] | Size ) with Issuer Data Private Key
      -finsig   <signature_hex>     Issuer’s signature SHA256(CID | Data [| Issuer_Data_Counter]) with Issuer Data Private Key
      
"""
