package dev.appxcode.ide.build

import java.nio.file.Path

data class BuildDiagnostic(val file: Path?, val line: Int?, val column: Int?, val message: String, val severity: DiagnosticSeverity)
enum class DiagnosticSeverity { ERROR, WARNING, NOTE }

object XcodeDiagnosticParser {
    private val withColumn = Regex("^(.+?):(\\d+):(\\d+):\\s*(error|warning|note):\\s*(.+)$", RegexOption.IGNORE_CASE)
    private val withoutColumn = Regex("^(.+?):(\\d+):\\s*(error|warning|note):\\s*(.+)$", RegexOption.IGNORE_CASE)
    private val global = Regex("^(fatal\\s+)?(error|warning|note):\\s*(.+)$", RegexOption.IGNORE_CASE)
    fun parse(output: String): List<BuildDiagnostic> = output.lineSequence().mapNotNull { line ->
        val value = line.trim()
        val match = withColumn.matchEntire(value)
        if (match != null) {
            val severity = severity(match.groupValues[4])
            return@mapNotNull BuildDiagnostic(Path.of(match.groupValues[1].trim()), match.groupValues[2].toInt(), match.groupValues[3].toInt(), match.groupValues[5].trim(), severity)
        }
        val short = withoutColumn.matchEntire(value)
        if (short != null) return@mapNotNull BuildDiagnostic(Path.of(short.groupValues[1].trim()), short.groupValues[2].toInt(), null, short.groupValues[4].trim(), severity(short.groupValues[3]))
        val generic = global.matchEntire(value) ?: return@mapNotNull null
        BuildDiagnostic(null, null, null, generic.groupValues[3].trim(), severity(generic.groupValues[2]))
    }.toList()

    private fun severity(value: String): DiagnosticSeverity = when (value.lowercase()) {
        "error" -> DiagnosticSeverity.ERROR
        "warning" -> DiagnosticSeverity.WARNING
        else -> DiagnosticSeverity.NOTE
    }
}
