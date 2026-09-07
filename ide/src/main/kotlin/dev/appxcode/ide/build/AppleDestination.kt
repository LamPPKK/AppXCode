package dev.appxcode.ide.build

enum class ApplePlatform { IOS, IPADOS, MACOS, WATCHOS, TVOS }
enum class DestinationKind { SIMULATOR, DEVICE, MAC }

data class AppleDestination(
    val platform: ApplePlatform,
    val kind: DestinationKind,
    val name: String,
    val identifier: String? = null,
) {
    init {
        require(name.isNotBlank() && name == name.trim()) { "Destination name must not be blank or padded" }
        require(identifier == null || (identifier.isNotBlank() && identifier == identifier.trim() && !identifier.any(Char::isWhitespace))) { "Destination identifier must be a compact value" }
    }

    fun xcodebuildSpecifier(): String = buildString {
        append("platform=")
        val platformName = when (platform) {
            ApplePlatform.IOS -> "iOS"
            ApplePlatform.IPADOS -> "iPadOS"
            ApplePlatform.MACOS -> "macOS"
            ApplePlatform.WATCHOS -> "watchOS"
            ApplePlatform.TVOS -> "tvOS"
        }
        append(if (kind == DestinationKind.SIMULATOR) "$platformName Simulator" else platformName)
        append(",name=").append(name)
        identifier?.let { append(",id=").append(it) }
    }
}
