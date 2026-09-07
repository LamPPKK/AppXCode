package dev.appxcode.ide.language

import dev.appxcode.ide.toolchain.AppleToolchain
import java.nio.file.Path

data class SwiftCompletion(val label: String, val detail: String? = null, val insertText: String = label)
data class SwiftDiagnostic(val file: Path, val line: Int, val column: Int, val message: String, val severity: Severity)
enum class Severity { ERROR, WARNING, INFO }

interface SwiftLanguageService {
    fun complete(file: Path, line: Int, column: Int): List<SwiftCompletion>
    fun diagnostics(files: List<Path>): List<SwiftDiagnostic>
}

object SwiftLanguageServiceFactory {
    fun create(toolchain: AppleToolchain, workspace: Path): SwiftLanguageService {
        val executable = workspace.resolve(".appxcode/sourcekit-lsp")
            .takeIf { java.nio.file.Files.isExecutable(it) }
            ?: System.getenv("PATH").orEmpty().split(java.io.File.pathSeparator).asSequence()
                .map { Path.of(it, "sourcekit-lsp") }.firstOrNull { java.nio.file.Files.isExecutable(it) }
        return if (executable != null) {
            LspSwiftLanguageService(toolchain, workspace, LspProcessManager(LspServerConfig(executable, workspace)))
        } else UnavailableSwiftLanguageService(toolchain)
    }
}

class LspSwiftLanguageService(
    private val toolchain: AppleToolchain,
    private val workspace: Path,
    private val processManager: LspProcessManager
) : SwiftLanguageService, AutoCloseable {
    init { processManager.start() }
    fun isAlive(): Boolean = processManager.isAlive()
    fun restart(): Boolean = processManager.restart()
    override fun complete(file: Path, line: Int, column: Int): List<SwiftCompletion> = emptyList()
    override fun diagnostics(files: List<Path>): List<SwiftDiagnostic> =
        UnavailableSwiftLanguageService(toolchain).diagnostics(files)
    override fun close() = processManager.close()
}

class UnavailableSwiftLanguageService(private val toolchain: AppleToolchain) : SwiftLanguageService {
    override fun complete(file: Path, line: Int, column: Int): List<SwiftCompletion> = emptyList()
    override fun diagnostics(files: List<Path>): List<SwiftDiagnostic> = files.flatMap { file ->
        if (!java.nio.file.Files.isRegularFile(file)) return@flatMap emptyList()
        val lines = java.nio.file.Files.readAllLines(file)
        buildList {
            if (toolchain.swiftPath == null) add(SwiftDiagnostic(file, 1, 1, "Swift toolchain is unavailable", Severity.INFO))
            lines.forEachIndexed { index, line ->
                if (line.contains("TODO") || line.contains("FIXME")) add(SwiftDiagnostic(file, index + 1, 1, line.trim(), Severity.INFO))
            }
            val balance = lines.joinToString("").count { it == '{' } - lines.joinToString("").count { it == '}' }
            if (balance != 0) add(SwiftDiagnostic(file, lines.size, 1, "Unbalanced braces", Severity.ERROR))
        }
    }
}
