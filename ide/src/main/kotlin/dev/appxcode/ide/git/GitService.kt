package dev.appxcode.ide.git

import java.nio.file.Path

data class GitStatus(val branch: String?, val changedFiles: List<String>, val available: Boolean)
data class GitBranch(val name: String, val remote: Boolean)
data class GitCommit(val hash: String, val subject: String, val author: String, val timestamp: Long?)
data class GitStash(val index: Int, val name: String, val message: String)

class GitService(private val command: (List<String>, Path) -> String? = { args, root ->
    val process = ProcessBuilder(args).directory(root.toFile()).redirectErrorStream(true).start()
    val output = process.inputStream.bufferedReader().readText()
    if (process.waitFor() == 0) output else null
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

    fun log(root: Path, limit: Int = 50): List<GitCommit> {
        require(limit in 1..500) { "limit must be between 1 and 500" }
        val output = run(root, listOf("git", "log", "-n", limit.toString(), "--format=%H%x1f%s%x1f%an%x1f%ct")) ?: return emptyList()
        return output.lineSequence().mapNotNull { line ->
            val parts = line.split('\u001f')
            if (parts.size < 4) null else GitCommit(parts[0], parts[1], parts[2], parts[3].toLongOrNull())
        }.toList()
    }

    fun stashes(root: Path): List<GitStash> {
        val output = run(root, listOf("git", "stash", "list", "--format=%gd%x1f%s")) ?: return emptyList()
        return output.lineSequence().mapNotNull { line ->
            val parts = line.split('\u001f', limit = 2)
            val index = parts.firstOrNull()?.removePrefix("stash@{")?.removeSuffix("}")?.toIntOrNull()
            if (index == null || parts.size < 2) null else GitStash(index, "stash@{$index}", parts[1])
        }.toList()
    }

    fun stashDiff(root: Path, index: Int): String {
        require(index >= 0) { "stash index must be non-negative" }
        return run(root, listOf("git", "stash", "show", "--patch", "stash@{$index}")) ?: ""
    }

    fun branches(root: Path): List<GitBranch> = run(root, listOf("git", "branch", "--all"))?.lineSequence()?.mapNotNull { line ->
        val name = line.trim().removePrefix("*").trim().takeIf(String::isNotBlank) ?: return@mapNotNull null
        GitBranch(name.removePrefix("remotes/"), name.startsWith("remotes/"))
    }?.distinctBy { it.name }?.sortedBy(GitBranch::name) ?: emptyList()

    fun createBranch(root: Path, name: String): Boolean {
        require(name.isNotBlank() && !name.contains(' ')) { "invalid branch name" }
        return run(root, listOf("git", "switch", "-c", name)) != null
    }

    fun checkout(root: Path, name: String): Boolean {
        require(name.isNotBlank() && !name.contains(' ')) { "invalid branch name" }
        return run(root, listOf("git", "switch", name)) != null
    }

    private fun run(root: Path, args: List<String>): String = runCatching { command(args, root) }.getOrNull()
}
