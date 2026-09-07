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

class XcodeExportService(private val runner: (List<String>) -> XcodeBuildResult = { command ->
    val process = ProcessBuilder(command).redirectErrorStream(true).start()
    val output = process.inputStream.bufferedReader().readText()
    val code = process.waitFor()
    XcodeBuildResult(code, output, false)
}) {
    fun export(options: ExportOptions): XcodeBuildResult {
        if (!options.archivePath.toFile().exists()) return XcodeBuildResult(null, "Archive not found: ${options.archivePath}", false)
        if (!options.optionsPlist.toFile().isFile) return XcodeBuildResult(null, "Export options plist not found: ${options.optionsPlist}", false)
        options.outputDirectory.toFile().mkdirs()
        val args = mutableListOf("xcodebuild", "-exportArchive", "-archivePath", options.archivePath.toString(), "-exportPath", options.outputDirectory.toString(), "-exportOptionsPlist", options.optionsPlist.toString())
        return runner(args)
    }
}
