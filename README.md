[![Release](https://jitpack.io/v/Tangem/tangem-sdk-android.svg)](https://jitpack.io/#tangem/tangem-sdk-android)
[![License: MIT](https://img.shields.io/badge/License-MIT-yellow.svg)](https://opensource.org/licenses/MIT)
# Welcome to Tangem

The Tangem card is a self-custodial hardware wallet for blockchain assets. The main functions of Tangem cards are to securely create and store a private key from a blockchain wallet and sign blockchain transactions. The Tangem card does not allow users to import/export, backup/restore private keys, thereby guaranteeing that the wallet is unique and unclonable. 

- [Getting Started](#getting-started)
	- [Requirements](#requirements)
	- [Installation](#installation)
- [Usage](#usage)
	- [Initialization](#initialization)
	- [Card interaction](#card-interaction)
		- [Scan card](#scan-card)
		- [Sign](#sign)
        - [Wallet](#wallet)
		    - [Create Wallet](#create-wallet)
		    - [Purge Wallet](#purge-wallet)
		- [Issuer Data](#issuer-data)
		    - [Read Issuer Data](#read-issuer-data)
		    - [Read Issuer Extra Data](#read-issuer-extra-data)
		    - [Write Issuer Data](#write-issuer-data)
		    - [Write Issuer Extra Data](#write-issuer-extra-data)
		- [User Data](#user-data)
		    - [Write User Data](#write-user-data)
		    - [Read User Data](#read-user-data)
		- [Personalization](#personalization)
		    - [Depersonalize](#depersonalize)
		    - [Personalize](#personalize)
- [Customization](#customization)
	- [UI](#ui)
	- [Reader](#reader)
	- [Custom Tasks](#custom-tasks)


## Getting Started

### Requirements
Android with minimal SDK version of 21 and a device with NFC support

### Installation

1. Add Tangem library to the project:

Add to a project `build.gradle` file:

```gradle
allprojects {
    repositories {
        maven { url "https://jitpack.io" }
    }
}
```

And add Tangem library to the dependencies (in an app or module `build.gradle` file):

```gradle 
dependencies {
    implementation "com.tangem.tangem-sdk-kotlin:android:$latestVersion"
    implementation "com.tangem.tangem-sdk-kotlin:core:$latestVersion"
}
```
`tangem-core` is a JVM library (without Android dependencies) that provides core functionality of interacting with Tangem cards.
`tangem-sdk` is an Android library that implements NFC interaction between Android devices and Tangem cards and graphical interface for this interaction. 

2. Save the file (you can name it anything you wish) with the following tech-list filters in the `<project-root>/res/xml`

```xml
<resources>
   <tech-list>
       <tech>android.nfc.tech.IsoDep</tech>
       <tech>android.nfc.tech.Ndef</tech>
       <tech>android.nfc.tech.NfcV</tech>
   </tech-list>
</resources>
```

3. Add to `AndroidManifest.xml`:
```xml
<intent-filter>
    <action android:name=“android.nfc.action.TECH_DISCOVERED” />
</intent-filter>
<meta-data
    android:name=“android.nfc.action.TECH_DISCOVERED”
    android:resource=“@xml/tech_filter” />
```

## Getting Started - JVM

### Requirements
Compatible NFC reader, connected to the computer.

### Installation

1. Add Tangem library to the project:

Add to a project `build.gradle` file:

```gradle
allprojects {
    repositories {
        maven { url "https://jitpack.io" }
    }
}
```

And add Tangem library to the dependencies (in an app or module `build.gradle` file):

```gradle 
dependencies {
    implementation "com.tangem.tangem-sdk-kotlin:core:$latestVersion"
    implementation "com.tangem.tangem-sdk-kotlin:jvm:$latestVersion"
}
```
`tangem-core` is a JVM library that provides core functionality of interacting with Tangem cards.
`tangem-jvm` is a JVM library that allowes to use Tangem core library with NFC readers connected to a Windows/Linux/Mac computer. 

2. Make sure that external reader is connected and that all the necessary drivers are installed so that the system have access to the reader. 

On Mac and Linux execute the terminal command `pcsctest` to check that the reader is connected.  This command lists all available connected readers.

On Windows available readers should be listed in `Device Manager` under  `Smart Card readers` header.

3. Initialize TangemSdk interface using this static initializer function:

```kotlin 
val tangemSdk = TangemSdk.init(
    verbose = true, // select depending on whether detailed logs are needed
    indexOfTerminal = 0 // can be omitted when only one NFC reader is connected to the computer
)
```


## Usage
Tangem SDK is a self-sufficient solution that implements a card abstraction model, methods of interaction with the card and interactions with the user via UI.

### Initialization
To get started, you need to create an instance of the `TangemSdk` class. It provides the simple way of interacting with the card. 

```kotlin
val tangemSdk: TangemSdk = TangemSdk.init(activity)
```

TangemSdk requires a reference to activity in order to use Android API for interacting with NFC and to render views. 
Default implementation of `TangemSdk` allows you to start using SDK in your application without any additional setup.
(You can read about additional options in [Customization](#customization)).

### Card interaction
#### Scan card 
To obtain data from the card use `scanCard()` method. This method launches an NFC session, and once it’s connected with the card, it obtains the card data. If the card contains a wallet (private and public key pair), it proves that the wallet owns a private key that corresponds to a public one.

Example:
```kotlin
tangemSdk.scanCard { result ->
    when (result) {
        is CompletionResult.Success -> {
            // Handle returned card data
            val card = result.data
            cardId = card.cardId
            // Switch to UI thread to show results in UI
            runOnUiThread {
                tv_card_cid?.text = cardId
            }
        }
        is CompletionResult.Failure -> {
            if (result.error is TangemSdkError.UserCancelledError) {
            // Handle case when user cancelled manually
            }
            // Handle other errors
        }
    }
}

```

Communication with the card is an asynchronous operation. In order to get a result for the method, you need to implement the callback function. In order to render the callback results on UI, you need to switch to the main thread.

`CompletionResult<T>` – this is the sealed class for the results of `CardSessionRunnable`.

`Success<T>(val data: T)` is triggered after successful operation and contains a `CommandResponse`. 
`Failure<T>(val error: TangemSdkError)` is triggered on error. 

#### Sign
This method allows you to sign one or multiple hashes. Simultaneous signing of array of hashes in a single SIGN command is required to support Bitcoin-type multi-input blockchains (UTXO). The SIGN command will return a corresponding array of signatures.

```kotlin
tangemSdk.sign(
        hashes = arrayOf(hash1, hash2),
        cardId) { result ->
    when (result) {
        is CompletionResult.Failure -> {
           if (result.error is TangemSdkError.UserCancelledError) {
               // Handle case when user cancelled manually
           }
           // Handle other errors
        }
        is CompletionResult.Success -> {
            val signResponse = result.data
        }
    }
}
```

#### Issuer Data

##### Read Issuer Data
An example of usage (description is available at documentation for `TangemSdk.readIssuerData` method and corresponding command class): 

```kotlin
        tangemSdk.readIssuerData(cardId) { result ->
            when (result) {
                is CompletionResult.Failure -> {
                    if (result.error is TangemSdkError.UserCancelledError) {
                        // Handle case when user cancelled manually
                        }
                        // Handle other errors  
                }
                is CompletionResult.Success -> {
                    val issuerData = result.data.issuerData
                    val issuerDataSignature = result.data.issuerDataSignature
                }
            }
        }
```

##### Read Issuer Extra Data
An example of usage (description is available at documentation for `TangemSdk.readIssuerExtraData` method and corresponding command class): 

```kotlin
        tangemSdk.readIssuerExtraData(cardId) { result ->
            when (result) {
                is CompletionResult.Failure -> {
                    if (result.error is TangemSdkError.UserCancelledError) {
                        // Handle case when user cancelled manually
                        }
                        // Handle other errors  
                }
                is CompletionResult.Success -> {
                    val issuerData = result.data.issuerData
                    val issuerDataSignature = result.data.issuerDataSignature
                }
            }
        }
```

##### Write Issuer Data
An example of usage (description is available at documentation for `TangemSdk.writeIssuerData` method and corresponding command class): 

```kotlin
        tangemSdk.writeIssuerData(cardId, issuerData, issuerDataSignature, issuerDataCounter) { result ->
            when (result) {
                is CompletionResult.Failure -> {
                    if (result.error is TangemSdkError.UserCancelledError) {
                        // Handle case when user cancelled manually
                        }
                        // Handle other errors  
                }
                is CompletionResult.Success -> {
                    val writeIssuerDataResult = result.data
                }
            }
        }
```

##### Write Issuer Extra Data
An example of usage (description is available at documentation for `TangemSdk.writeIssuerExtraData` method and corresponding command class): 

```kotlin
        tangemSdk.writeIssuerExtraData(cardId, 
            issuerData, startingSignature, finalizingSignature, issuerDataCounter
        ) { result ->
            when (result) {
                is CompletionResult.Failure -> {
                    if (result.error is TangemSdkError.UserCancelledError) {
                        // Handle case when user cancelled manually
                        }
                        // Handle other errors  
                }
                is CompletionResult.Success -> runOnUiThread {
                    val writeIssuerDataResult = result.data
                }
            }
        }
```
#### User Data
##### Write User Data
An example of usage (description is available at documentation for `TangemSdk.writeUserData` method and corresponding command class): 

```kotlin
        tangemSdk.writeUserData(
            cardId, userData, userProtectedData, userCounter, userProtectedCounter 
        ) { result ->
            when (result) {
                is CompletionResult.Failure -> {
                    if (result.error is TangemSdkError.UserCancelledError) {
                        // Handle case when user cancelled manually
                        }
                        // Handle other errors  
                }
                is CompletionResult.Success -> runOnUiThread {
                    val writeUserDataResult = result.data
                }
            }
        }
```

##### Read User Data
An example of usage (description is available at documentation for `TangemSdk.readUserData` method and corresponding command class): 

```kotlin
        tangemSdk.readUserData(cardId) { result ->
            when (result) {
                is CompletionResult.Failure -> {
                    if (result.error is TangemSdkError.UserCancelledError) {
                        // Handle case when user cancelled manually
                        }
                        // Handle other errors  
                }
                is CompletionResult.Success -> runOnUiThread {
                    val readUserDataResult = result.data
                }
            }
        }
```
#### Wallet
##### Create Wallet
An example of usage (description is available at documentation for `TangemSdk.createWallet` method and corresponding command class): 

```kotlin
        tangemSdk.createWallet(cardId) { result ->
            when (result) {
                is CompletionResult.Failure -> {
                    if (result.error is TangemSdkError.UserCancelledError) {
                        // Handle case when user cancelled manually
                        }
                        // Handle other errors  
                }
                is CompletionResult.Success -> runOnUiThread {
                    val createWalletResult = result.data
                }
            }
        }
```

##### Purge Wallet
An example of usage (description is available at documentation for `TangemSdk.purgeWallet` method and corresponding command class): 

```kotlin
        tangemSdk.purgeWallet(cardId) { result ->
            when (result) {
                is CompletionResult.Failure -> {
                    if (result.error is TangemSdkError.UserCancelledError) {
                        // Handle case when user cancelled manually
                        }
                        // Handle other errors  
                }
                is CompletionResult.Success -> runOnUiThread {
                    val purgeWalletResult = result.data
                }
            }
        }
```

#### Personalization
##### Depersonalize
An example of usage (description is available at documentation for `TangemSdk.depersonalize` method and corresponding command class): 

```kotlin
        tangemSdk.depersonalize(cardId) { result ->
            when (result) {
                is CompletionResult.Failure -> {
                    if (result.error is TangemSdkError.UserCancelledError) {
                        // Handle case when user cancelled manually
                        }
                        // Handle other errors  
                }
                is CompletionResult.Success -> runOnUiThread {
                    val depersonalizeResult = result.data
                }
            }
        }
```

##### Personalize
An example of usage (description is available at documentation for `TangemSdk.personalize` method and corresponding command class): 

```kotlin
        tangemSdk.personalize(config, issuer, manufacturer, acquirer) { result ->
            when (result) {
                is CompletionResult.Failure -> {
                    if (result.error is TangemSdkError.UserCancelledError) {
                        // Handle case when user cancelled manually
                        }
                        // Handle other errors  
                }
                is CompletionResult.Success -> runOnUiThread {
                    val card = result.data
                }
            }
        }
```


## Customization
### UI
If the interaction with user is required, the SDK performs the entire cycle of this interaction. In order to change the appearance or behavior of the user UI, you can provide you own implementation of the `SessionViewDelegate` interface. After this, initialize the `TangemSdk` class with your delegate class.

```kolin
val tangemSdk = TangemSdk.customInit(this, MySessionViewDelegate())
```

### Reader

TangemSdk uses Android NFC capabilities to communicate with TangemCards. If there is a need to use other means of communication or other platforms, it is possible to implement this functionality by implementing `CardReader` interface and using it instead of the default `NfcReader`.

### Custom Tasks
`TangemSdk` specific methods run particular commands and tasks. If you want to trigger card commands in a different order, or implement additional
 business logic, you have several options.
 
One option is to start a session, get an instance of `CardSession`, and use it to perform commands.
To do this, you need to call `tangemSdk.startSession()` method and get a `CardSession` instance in a callback.

> For example, if you want to read the card and immediately sign a transaction with it, you can achieve it this way.

```kotlin
 tangemSdk.startSession { session, error ->
            if (error == null) {
                session.startWithRunnable(
                        SignCommand(createSampleHashes())) {result ->  
                     when (result) {
                                    is CompletionResult.Failure -> {
                                       if (result.error is TangemSdkError.UserCancelledError) {
                                           // Handle case when user cancelled manually
                                       }
                                       // Handle other errors  
                                    }
                                    is CompletionResult.Success -> {
                                    val signResponse = result.data
                                    }
                                }
                }
            }
        }
```

Another option is to put all logic in a in `CardSessionRunnable` class and launch it using `tangemSdk.startSessionWithRunnable()` method.
In Tangem SDK we use this approach for our `ScanTask`.

> For example, if you want to read the card and immediately sign a transaction with it, with subclassing  `CardSessionRunnable` you can achieve it this way.

```kotlin
class OneTapSignTask(private val hashesToSign: Array<ByteArray>) : CardSessionRunnable<Card> {

    override fun run(session: CardSession, callback: (result: CompletionResult<Card>) -> Unit) {

        val card = session.environment.card

        if (card == null) {
            callback(CompletionResult.Failure(TangemSdkError.MissingPreflightRead()))

        } else if (card.cardData?.productMask?.contains(Product.Tag) != false) {
            callback(CompletionResult.Success(card))

        } else if (card.status != CardStatus.Loaded) {
            callback(CompletionResult.Success(card))

        } else if (card.curve == null || card.walletPublicKey == null) {
            callback(CompletionResult.Failure(TangemSdkError.CardError()))

        } else {
            val signCommand = SignCommand(hashesToSign)

            signCommand.run(session) { result ->
                when (result) {
                    is CompletionResult.Success -> callback(CompletionResult.Success(result.data))
                    is CompletionResult.Failure -> callback(CompletionResult.Failure(result.error))
                }
            }
        }
    }
}
```
