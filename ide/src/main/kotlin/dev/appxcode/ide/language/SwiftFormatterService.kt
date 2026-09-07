package dev.appxcode.ide.language

import java.nio.file.Files
import java.nio.file.Path

data class FormatResult(val success: Boolean, val output: String, val exitCode: Int?)

class SwiftFormatterService(
    private val executable: String = "swift-format",
    private val runner: (List<String>, Path) -> FormatResult = { args, root ->
        val process = ProcessBuilder(args).directory(root.toFile()).redirectErrorStream(true).start()
        val output = process.inputStream.bufferedReader().readText()
        val code = process.waitFor()
        FormatResult(code == 0, output, code)
    },
) {
    fun format(file: Path): FormatResult {
        if (!Files.isRegularFile(file)) return FormatResult(false, "Swift file not found: $file", null)
        return runCatching { runner(listOf(executable, "format", "--in-place", file.toString()), file.parent) }
            .getOrElse { FormatResult(false, it.message ?: "swift-format unavailable", null) }
    }
}
