package com.tangem.tangem_demo

import com.tangem.common.json.MoshiJsonConverter
import com.tangem.operations.personalization.entities.Acquirer
import com.tangem.operations.personalization.entities.CardConfig
import com.tangem.operations.personalization.entities.Issuer
import com.tangem.operations.personalization.entities.Manufacturer

class Utils {

    companion object {

        fun randomInt(from: Int, to: Int): Int {
            return kotlin.random.Random.nextInt(from, to)
        }

        fun randomString(length: Int): String {
            val charPool: List<Char> = ('a'..'z') + ('A'..'Z') + ('0'..'9')
            val randomString = (1..length)
                    .map { randomInt(0, charPool.size) }
                    .map(charPool::get)
                    .joinToString("");
            return randomString
        }
    }
}

class Personalization {
    companion object {
        fun manufacturer(): Manufacturer = MoshiJsonConverter.INSTANCE.fromJson(manufacturerJson)!!
        fun issuer(): Issuer = MoshiJsonConverter.INSTANCE.fromJson(issuerJson)!!
        fun acquirer(): Acquirer = MoshiJsonConverter.INSTANCE.fromJson(acquirerJson)!!

        private val manufacturerJson =
                """
    {
        "keyPair": {
            "privateKey": "1b48cfd24bbb5b394771ed81f2bacf57479e4735eb1405083927372d40da9e92",
            "publicKey": "04bab86d56298c996f564a84fc88e28aed38184b12f07e519113bef48c76f3df3adc303599b08ac05b55ec3df98d9338573a6242f76f5d28f4f0f364e87e8fca2f"
        },
        "name": "Tangem"
    }
    """

        private val issuerJson =
                """
    {
        "dataKeyPair": {
            "privateKey": "11121314151617184771ED81F2BACF57479E4735EB1405083927372D40DA9E92",
            "publicKey": "045F16BD1D2EAFE463E62A335A09E6B2BBCBD04452526885CB679FC4D27AF1BD22F553C7DEEFB54FD3D4F361D14E6DC3F11B7D4EA183250A60720EBDF9E110CD26"
        },
        "transactionKeyPair": {
            "privateKey": "11121314151617184771ED81F2BACF57479E4735EB1405081918171615141312",
            "publicKey": "0484C5192E9BFA6C528A344F442137A92B89EA835BFEF1D04CB4362EB906B508C5889846CFEA71BA6DC7B3120C2208DF9C46127D3D85CB5CFBD1479E97133A39D8"
        },
        "name": "TANGEM SDK",
        "id": "TANGEM SDK"
    }
    """

        private val acquirerJson =
                """
    {
        "keyPair": {
            "privateKey": "21222324252627284771ED81F2BACF57479E4735EB1405083927372D40DA9E92",
            "publicKey": "0456ad1a82b22bcb40c38fd08939f87e6b80e40dec5b3bdb351c55fcd709e47f9fb2ed00c2304d3a986f79c5ae0ac3c84e88da46dc8f513b7542c716af8c9a2daf"
        },
        "name": "Smart Cash",
        "id": "Smart Cash"
    }
    """
    }

    class Backup {
        companion object {
            fun primaryCardConfig(): CardConfig = MoshiJsonConverter.INSTANCE.fromJson(configJsonPrimary)!!
            fun backup1Config(): CardConfig = MoshiJsonConverter.INSTANCE.fromJson(configJsonBackup1)!!
            fun backup2Config(): CardConfig = MoshiJsonConverter.INSTANCE.fromJson(configJsonBackup2)!!

            private val configJsonPrimary =
                    """
    {
       "releaseVersion": true,
       "issuerName": "TANGEM AG",
       "series": "CB",
       "startNumber": 7900000000000,
       "count": 2500,
       "numberFormat": "",
       "PIN": "000000",
       "PIN2": "000",
       "CVC": "000",
       "walletsCount": 6,
       "pauseBeforePIN2": 5000,
       "smartSecurityDelay": true,
       "curveID": "secp256k1",
       "SigningMethod": 0,
       "isReusable": false,
       "allowSwapPIN": false,
       "allowSwapPIN2": true,
       "useActivation": false,
       "useCVC": false,
       "useNDEF": true,
       "useBlock": false,
       "allowSelectBlockchain": true,
       "forbidPurgeWallet": false,
       "protocolAllowUnencrypted": true,
       "protocolAllowStaticEncryption": true,
       "forbidDefaultPIN": false,
       "skipSecurityDelayIfValidatedByIssuer": false,
       "skipCheckPIN2andCVCIfValidatedByIssuer": false,
       "skipSecurityDelayIfValidatedByLinkedTerminal": true,
       "disableIssuerData": true,
       "disableUserData": false,
       "disableFiles": false,
       "allowHDWallets": true,
       "allowBackup": true,
       "NDEF": [],
       "cardData": {
           "date": "2021-03-15",
           "batch": "AC01",
           "blockchain": "ANY",
           "product_note": true,
           "product_tag": false,
           "product_id_card": false,
           "product_id_issuer": false,
           "product_authentication": false,
           "product_twin": false
       },
       "createWallet": 0
    }
    """
            private val configJsonBackup1 =
                    """
    {
       "releaseVersion": true,
       "issuerName": "TANGEM AG",
       "series": "CB",
       "startNumber": 7900000000001,
       "count": 2500,
       "numberFormat": "",
       "PIN": "000000",
       "PIN2": "000",
       "CVC": "000",
       "walletsCount": 6,
       "pauseBeforePIN2": 5000,
       "smartSecurityDelay": true,
       "curveID": "secp256k1",
       "SigningMethod": 0,
       "isReusable": false,
       "allowSwapPIN": false,
       "allowSwapPIN2": true,
       "useActivation": false,
       "useCVC": false,
       "useNDEF": true,
       "useBlock": false,
       "allowSelectBlockchain": true,
       "forbidPurgeWallet": false,
       "protocolAllowUnencrypted": true,
       "protocolAllowStaticEncryption": true,
       "forbidDefaultPIN": false,
       "skipSecurityDelayIfValidatedByIssuer": false,
       "skipCheckPIN2andCVCIfValidatedByIssuer": false,
       "skipSecurityDelayIfValidatedByLinkedTerminal": true,
       "disableIssuerData": true,
       "disableUserData": false,
       "disableFiles": false,
       "allowHDWallets": true,
       "allowBackup": true,
       "NDEF": [],
       "cardData": {
           "date": "2021-03-15",
           "batch": "AC01",
           "blockchain": "ANY",
           "product_note": true,
           "product_tag": false,
           "product_id_card": false,
           "product_id_issuer": false,
           "product_twin": false
       },
       "createWallet": 0
    }
    """

            private val configJsonBackup2 =
                    """
    {
       "releaseVersion": true,
       "issuerName": "TANGEM AG",
       "series": "CB",
       "startNumber": 7900000000002,
       "count": 2500,
       "numberFormat": "",
       "PIN": "000000",
       "PIN2": "000",
       "CVC": "000",
       "walletsCount": 6,
       "pauseBeforePIN2": 5000,
       "smartSecurityDelay": true,
       "curveID": "secp256k1",
       "SigningMethod": 0,
       "isReusable": false,
       "allowSwapPIN": false,
       "allowSwapPIN2": true,
       "useActivation": false,
       "useCVC": false,
       "useNDEF": true,
       "useBlock": false,
       "allowSelectBlockchain": true,
       "forbidPurgeWallet": false,
       "protocolAllowUnencrypted": true,
       "protocolAllowStaticEncryption": true,
       "forbidDefaultPIN": false,
       "skipSecurityDelayIfValidatedByIssuer": false,
       "skipCheckPIN2andCVCIfValidatedByIssuer": false,
       "skipSecurityDelayIfValidatedByLinkedTerminal": true,
       "disableIssuerData": true,
       "disableUserData": false,
       "disableFiles": false,
       "allowHDWallets": true,
       "allowBackup": true,
       "NDEF": [],
       "cardData": {
           "date": "2021-03-15",
           "batch": "AC01",
           "blockchain": "ANY",
           "product_note": true,
           "product_tag": false,
           "product_id_card": false,
           "product_id_issuer": false,
           "product_authentication": false,
           "product_twin": false
       },
       "createWallet": 0
    }
    """
        }
    }
}