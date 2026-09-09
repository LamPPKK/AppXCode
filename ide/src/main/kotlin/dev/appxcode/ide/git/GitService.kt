package dev.appxcode.ide.git

import java.nio.file.Path

data class GitStatus(val branch: String?, val changedFiles: List<String>, val available: Boolean) {
    val isClean: Boolean get() = available && changedFiles.isEmpty()
    val isDetached: Boolean get() = branch == null || branch == "HEAD"
}
data class GitSyncStatus(val ahead: Int, val behind: Int)
data class GitBranch(val name: String, val remote: Boolean)
data class GitCommit(val hash: String, val subject: String, val author: String, val timestamp: Long?)
data class GitStash(val index: Int, val name: String, val message: String)
data class GitRemote(val name: String, val url: String, val pushUrl: String?)
data class GitTag(val name: String, val hash: String?)
data class GitBlameLine(val line: Int, val hash: String, val author: String, val text: String)

class GitService(private val command: (List<String>, Path) -> String? = { args, root ->
    val process = ProcessBuilder(args).directory(root.toFile()).redirectErrorStream(true).start()
    val output = process.inputStream.bufferedReader().readText()
    if (process.waitFor() == 0) output else null
}) {
    fun isRepository(root: Path): Boolean = run(root, listOf("git", "rev-parse", "--is-inside-work-tree"))?.trim() == "true"
    fun currentRevision(root: Path): String? = run(root, listOf("git", "rev-parse", "HEAD"))?.trim()?.takeIf { it.matches(Regex("[0-9a-fA-F]{40}")) }

    fun status(root: Path): GitStatus {
        val output = run(root, listOf("git", "status", "--short", "--branch")) ?: return GitStatus(null, emptyList(), false)
        val lines = output.lineSequence().filter(String::isNotBlank).toList()
        val branch = lines.firstOrNull()?.removePrefix("## ")?.substringBefore("...")?.takeUnless { it.startsWith("HEAD (") }
        val changed = lines.drop(1).mapNotNull { it.trim().takeIf(String::isNotBlank) }
        return GitStatus(branch, changed, true)
    }

    fun syncStatus(root: Path): GitSyncStatus? {
        val output = run(root, listOf("git", "rev-list", "--left-right", "--count", "@{upstream}...HEAD")) ?: return null
        val parts = output.trim().split(Regex("\\s+"))
        return if (parts.size == 2) GitSyncStatus(parts[1].toIntOrNull() ?: return null, parts[0].toIntOrNull() ?: return null) else null
    }

    fun diff(root: Path, staged: Boolean = false): String =
        run(root, if (staged) listOf("git", "diff", "--cached") else listOf("git", "diff")) ?: ""
    fun fileDiff(root: Path, file: Path, staged: Boolean = false): String {
        val resolved = root.resolve(file).normalize()
        require(resolved.startsWith(root.normalize())) { "file must stay within repository" }
        val flag = if (staged) "--cached" else "--"
        return run(root, listOf("git", "diff") + listOf(flag, root.normalize().relativize(resolved).toString())) ?: ""
    }

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

    fun remotes(root: Path): List<GitRemote> {
        val output = run(root, listOf("git", "remote", "-v")) ?: return emptyList()
        return output.lineSequence().mapNotNull { line ->
            val parts = line.trim().split(Regex("\\s+"))
            if (parts.size < 3) null else GitRemote(parts[0], parts[1], parts.getOrNull(2)?.takeIf { it == "(push)" }?.let { parts[1] })
        }.groupBy { it.name }.mapNotNull { (name, entries) ->
            val fetch = entries.firstOrNull { it.pushUrl == null }
            val push = entries.firstOrNull { it.pushUrl != null }
            (fetch ?: push)?.let { GitRemote(name, fetch?.url ?: it.url, push?.url) }
        }.sortedBy(GitRemote::name)
    }

    fun tags(root: Path): List<GitTag> {
        val output = run(root, listOf("git", "tag", "--list", "--format=%(refname:short)%x1f%(objectname)")) ?: return emptyList()
        return output.lineSequence().mapNotNull { line ->
            val parts = line.split('\u001f', limit = 2)
            parts.firstOrNull()?.takeIf(String::isNotBlank)?.let { GitTag(it, parts.getOrNull(1)?.ifBlank { null }) }
        }.sortedBy(GitTag::name).toList()
    }

    fun branches(root: Path): List<GitBranch> = run(root, listOf("git", "branch", "--all"))?.lineSequence()?.mapNotNull { line ->
        val name = line.trim().removePrefix("*").trim().takeIf(String::isNotBlank) ?: return@mapNotNull null
        GitBranch(name.removePrefix("remotes/"), name.startsWith("remotes/"))
    }?.distinctBy { it.name }?.sortedBy(GitBranch::name)?.toList() ?: emptyList()

    fun createBranch(root: Path, name: String): Boolean {
        require(validRef(name)) { "invalid branch name" }
        return run(root, listOf("git", "switch", "-c", name)) != null
    }

    fun checkout(root: Path, name: String): Boolean {
        require(validRef(name)) { "invalid branch name" }
        return run(root, listOf("git", "switch", name)) != null
    }

    fun commit(root: Path, message: String): String? {
        require(message.isNotBlank()) { "Commit message must not be blank" }
        return run(root, listOf("git", "commit", "-m", message))?.trim()?.takeIf { it.isNotBlank() }
    }
    fun stage(root: Path, files: Collection<Path>): Boolean = updateIndex(root, files, true)
    fun unstage(root: Path, files: Collection<Path>): Boolean = updateIndex(root, files, false)

    fun merge(root: Path, branch: String): Boolean {
        require(validRef(branch)) { "invalid branch name" }
        return run(root, listOf("git", "merge", "--", branch)) != null
    }

    fun fetch(root: Path, remote: String = "origin"): Boolean {
        require(validRef(remote)) { "invalid remote name" }
        return run(root, listOf("git", "fetch", "--", remote)) != null
    }

    fun pull(root: Path, remote: String = "origin", branch: String? = null): Boolean {
        require(validRef(remote)) { "invalid remote name" }
        branch?.let { require(validRef(it)) { "invalid branch name" } }
        return run(root, buildList { addAll(listOf("git", "pull", "--ff-only", "--", remote)); branch?.let { add(it) } }) != null
    }

    fun push(root: Path, remote: String = "origin", branch: String? = null): Boolean {
        require(validRef(remote)) { "invalid remote name" }
        branch?.let { require(validRef(it)) { "invalid branch name" } }
        return run(root, buildList { addAll(listOf("git", "push", "--", remote)); branch?.let { add(it) } }) != null
    }

    fun blame(root: Path, file: Path): List<GitBlameLine> {
        val resolved = root.resolve(file).normalize()
        require(resolved.startsWith(root.normalize())) { "file must stay within repository" }
        val relative = root.normalize().relativize(resolved).toString()
        val output = run(root, listOf("git", "blame", "--line-porcelain", "--", relative)) ?: return emptyList()
        var line = 0
        var hash = ""
        var author = ""
        return output.lineSequence().mapNotNull { value ->
            when {
                value.matches(Regex("^[0-9a-f]{40} \\d+ \\d+.*")) -> { hash = value.substringBefore(' '); line++; null }
                value.startsWith("author ") -> { author = value.removePrefix("author "); null }
                value.startsWith("\\t") -> GitBlameLine(line, hash, author, value.drop(1))
                else -> null
            }
        }.toList()
    }

    fun hooks(root: Path): List<String> = run(root, listOf("git", "config", "--get-regexp", "^core.hooksPath$"))
        ?.lineSequence()?.map { it.substringAfterLast(' ').trim() }?.filter(String::isNotBlank)?.toList() ?: emptyList()

    private fun run(root: Path, args: List<String>): String? = runCatching { command(args, root) }.getOrNull()
    private fun updateIndex(root: Path, files: Collection<Path>, add: Boolean): Boolean {
        require(files.isNotEmpty()) { "At least one file is required" }
        val relative = files.map { file ->
            val resolved = root.resolve(file).normalize()
            require(resolved.startsWith(root.normalize())) { "file must stay within repository" }
            root.normalize().relativize(resolved).toString()
        }
        val command = if (add) listOf("git", "add", "--") + relative else listOf("git", "restore", "--staged", "--") + relative
        return run(root, command) != null
    }
    private fun validRef(value: String): Boolean = value.isNotBlank() && !value.startsWith('-') && !value.contains(' ') && !value.contains("..")
}
