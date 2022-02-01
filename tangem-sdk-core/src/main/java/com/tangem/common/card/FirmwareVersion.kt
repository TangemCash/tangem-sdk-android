package com.tangem.common.card

/**
 * Holds information about card firmware version included version saved on card `version`,
 * splitted to `major`, `minor` and `patch` and `FirmwareType`
 */
class FirmwareVersion : Comparable<FirmwareVersion> {
    /**
     * Version that saved on card
     */
    val stringValue: String

    val doubleValue: Double
        get() = "$major.$minor".toDouble()

    var major: Int = 0
        private set

    var minor: Int = 0
        private set

    var patch: Int = 0
        private set

    var type: FirmwareType
        private set


    constructor(version: String) {
        this.stringValue = version

        val versionCleaned = version.removeSuffix("\u0000")

        val cardTypeStr = versionCleaned.trim { "0123456789.".contains(it) }
        val result = versionCleaned.replace(cardTypeStr, "")
        val splitted = result.split(".").toMutableList()

        splitted.removeFirstOrNull()?.let { this.major = it.toInt() }
        splitted.removeFirstOrNull()?.let { this.minor = it.toInt() }
        splitted.removeFirstOrNull()?.let { this.patch = it.toInt() }

        type = FirmwareType.from(cardTypeStr)
    }

    constructor(major: Int, minor: Int, patch: Int = 0, type: FirmwareType = FirmwareType.Sdk) {
        this.major = major
        this.minor = minor
        this.patch = patch
        this.type = type

        this.stringValue = StringBuilder()
                .append("$major.$minor")
                .append(if (patch != 0) ".$patch" else "")
                .append(type.rawValue)
                .toString()
    }

    override fun compareTo(other: FirmwareVersion): Int = when {
        major != other.major -> major.compareTo(other.major)
        minor != other.minor -> minor.compareTo(other.minor)
        else -> patch.compareTo(other.patch)
    }

    override fun equals(other: Any?): Boolean {
        val other = other as? FirmwareVersion ?: return false
        return stringValue == other.stringValue
    }

    override fun hashCode(): Int = stringValue.hashCode()

    companion object {
        val Min = FirmwareVersion(0, 0)
        val Max = FirmwareVersion(Int.MAX_VALUE, 0)

        /**
         * Read-write files
         */
        val FilesAvailable = FirmwareVersion(3, 29)

        /**
         * Multi-wallet
         */
        val MultiWalletAvailable = FirmwareVersion(4, 0)

        /**
         * Field on card that describes is passcode is default value or not
         */
        val IsPasscodeStatusAvailable = FirmwareVersion(4, 1)

        /**
         * Is create wallet command answers with the whole wallet
         */
        val CreateWalletResponseAvailable = FirmwareVersion(4, 25)

        /**
         * HD Wallet
         */
        val HDWalletAvailable = FirmwareVersion(4, 28)

        /**
         * Field on card that describes is accessCode is default value or not
         */
        val IsAccessCodeStatusAvailable = FirmwareVersion(4, 33)

        /**
         * Backup
         */
        val BackupAvailable = FirmwareVersion(4, 43)
    }

    enum class FirmwareType(val rawValue: String?) {
        Sdk("d SDK"),
        Release("r"),
        Sprecial(null);

        companion object {
            fun from(type: String): FirmwareType {
                val trimmed = type.trim()
                if (trimmed.isEmpty()) return Release

                return values().firstOrNull { it.rawValue == type } ?: Sprecial
            }
        }
    }

}