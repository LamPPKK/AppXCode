package dev.appxcode.ide.git

import java.nio.file.Path

data class GitStatus(val branch: String?, val changedFiles: List<String>, val available: Boolean)

class GitService(private val command: (List<String>, Path) -> String = { args, root ->
    ProcessBuilder(args).directory(root.toFile()).redirectErrorStream(true).start().inputStream.bufferedReader().readText()
}) {
    fun status(root: Path): GitStatus {
        val output = run(root, listOf("git", "status", "--short", "--branch")) ?: return GitStatus(null, emptyList(), false)
        val lines = output.lineSequence().filter(String::isNotBlank).toList()
        val branch = lines.firstOrNull()?.removePrefix("## ")?.substringBefore("...")
        val changed = lines.drop(1).mapNotNull { it.trim().takeIf(String::isNotBlank) }
        return GitStatus(branch, changed, true)
    }

    fun diff(root: Path, staged: Boolean = false): String =
        run(root, if (staged) listOf("git", "diff", "--cached") else listOf("git", "diff")) ?: ""

    private fun run(root: Path, args: List<String>): String = runCatching { command(args, root) }.getOrNull()
}
