package dev.appxcode.ide.toolchain

import java.nio.file.Files
import java.nio.file.Path
import java.nio.file.Paths

data class AppleToolchain(
    val developerDirectory: Path?,
    val xcodebuildPath: Path?,
    val swiftPath: Path?,
    val xcodeVersion: String?,
) {
    val available: Boolean get() = xcodebuildPath != null && developerDirectory != null
    val capabilities: Set<String> get() = buildSet {
        if (available) add("xcodebuild")
        if (swiftPath != null) add("swift")
        if (available && swiftPath != null) add("apple-platform-build")
    }
}

/** Resolves installed Apple tooling without mutating the developer machine. */
object AppleToolchainDetector {
    fun detect(
        developerDirectory: Path? = pathFromProperty("xcode-select.path", false),
        xcodebuild: Path? = pathFromProperty("xcodebuild.path"),
        swift: Path? = pathFromProperty("swift.path"),
        version: String? = System.getProperty("xcode.version"),
    ): AppleToolchain {
        val developer = developerDirectory ?: existing(Path.of("/Applications/Xcode.app/Contents/Developer"))
        val build = xcodebuild ?: existing(Path.of("/usr/bin/xcodebuild"))
        val swiftPath = swift ?: existing(Path.of("/usr/bin/swift"))
        return AppleToolchain(developer, build, swiftPath, version)
    }

    private fun pathFromProperty(name: String, regularFile: Boolean = true): Path? =
        System.getProperty(name)?.let(Paths::get)?.takeIf { if (regularFile) Files.isRegularFile(it) else Files.isDirectory(it) }
    private fun existing(path: Path): Path? = path.takeIf { Files.exists(it) }
}
