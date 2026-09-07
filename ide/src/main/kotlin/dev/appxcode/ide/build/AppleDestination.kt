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
        require(name.isNotBlank()) { "Destination name must not be blank" }
        require(identifier == null || identifier.isNotBlank()) { "Destination identifier must not be blank" }
    }

    fun xcodebuildSpecifier(): String = buildString {
        append("platform=")
        append(if (kind == DestinationKind.SIMULATOR) "${platform.name.lowercase()} Simulator" else platform.name.lowercase())
        append(",name=").append(name)
        identifier?.let { append(",id=").append(it) }
    }
}
