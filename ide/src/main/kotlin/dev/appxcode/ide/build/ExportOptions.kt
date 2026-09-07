package dev.appxcode.ide.build

import java.nio.file.Path
import java.nio.file.Files

enum class ExportMethod { APP_STORE, AD_HOC, DEVELOPMENT, ENTERPRISE }

data class ExportOptions(
    val archivePath: Path,
    val outputDirectory: Path,
    val optionsPlist: Path,
    val method: ExportMethod,
    val teamId: String? = null,
    val signingStyle: String? = null,
) {
    init {
        require(archivePath.fileName != null) { "Archive path must point to a file" }
        require(outputDirectory.fileName != null) { "Export directory must have a name" }
        require(optionsPlist.fileName != null) { "Options plist path must point to a file" }
        require(teamId == null || teamId.isNotBlank()) { "Team ID must not be blank" }
        require(signingStyle == null || signingStyle.isNotBlank()) { "Signing style must not be blank" }
    }

    fun writePlist(): Path {
        val body = buildString {
            append("<?xml version=\"1.0\" encoding=\"UTF-8\"?>\n<plist version=\"1.0\"><dict>")
            append("<key>method</key><string>").append(method.name.lowercase().replace('_', '-')).append("</string>")
            teamId?.let { append("<key>teamID</key><string>").append(escapeXml(it)).append("</string>") }
            signingStyle?.let { append("<key>signingStyle</key><string>").append(escapeXml(it)).append("</string>") }
            append("</dict></plist>")
        }
        optionsPlist.parent?.let(Files::createDirectories)
        Files.writeString(optionsPlist, body)
        return optionsPlist
    }

    private fun escapeXml(value: String): String = value
        .replace("&", "&amp;")
        .replace("<", "&lt;")
        .replace(">", "&gt;")
        .replace("\"", "&quot;")
        .replace("'", "&apos;")
}

class XcodeExportService(
    private val executable: String = "xcodebuild",
    private val runner: ((List<String>) -> XcodeBuildResult)? = null,
) {
    constructor(runner: (List<String>) -> XcodeBuildResult) : this("xcodebuild", runner)

    private fun run(command: List<String>): XcodeBuildResult = runner?.invoke(command) ?: run {
    val process = ProcessBuilder(command).redirectErrorStream(true).start()
    val output = process.inputStream.bufferedReader().readText()
    val code = process.waitFor()
    XcodeBuildResult(code, output, false)
    }

    fun export(options: ExportOptions): XcodeBuildResult {
        if (!options.archivePath.toFile().exists()) return XcodeBuildResult(null, "Archive not found: ${options.archivePath}", false)
        if (!options.optionsPlist.toFile().isFile) return XcodeBuildResult(null, "Export options plist not found: ${options.optionsPlist}", false)
        options.outputDirectory.toFile().mkdirs()
        val args = mutableListOf(executable, "-exportArchive", "-archivePath", options.archivePath.toString(), "-exportPath", options.outputDirectory.toString(), "-exportOptionsPlist", options.optionsPlist.toString())
        return run(args)
    }
}
