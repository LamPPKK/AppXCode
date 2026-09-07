package dev.appxcode.ide.build

import java.nio.file.Path

data class BuildDiagnostic(val file: Path?, val line: Int?, val column: Int?, val message: String, val severity: DiagnosticSeverity)
enum class DiagnosticSeverity { ERROR, WARNING, NOTE }

object XcodeDiagnosticParser {
    private val pattern = Regex("^(.+?):(\\d+):(\\d+): (error|warning|note): (.+)$")
    fun parse(output: String): List<BuildDiagnostic> = output.lineSequence().mapNotNull { line ->
        val match = pattern.find(line.trim()) ?: return@mapNotNull null
        val severity = when (match.groupValues[4]) { "error" -> DiagnosticSeverity.ERROR; "warning" -> DiagnosticSeverity.WARNING; else -> DiagnosticSeverity.NOTE }
        BuildDiagnostic(Path.of(match.groupValues[1]), match.groupValues[2].toInt(), match.groupValues[3].toInt(), match.groupValues[5], severity)
    }.toList()
}
