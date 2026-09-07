package dev.appxcode.ide.remote

import java.nio.file.Files
import java.nio.file.Path
import java.security.MessageDigest

object BuildArtifactScanner {
    fun verify(transfer: BuildTransfer): Boolean = transfer.artifacts.all(::verify)

    fun verify(artifact: BuildArtifact): Boolean = runCatching {
        Files.isRegularFile(artifact.path) && Files.size(artifact.path) == artifact.sizeBytes && sha256(artifact.path).equals(artifact.sha256, ignoreCase = true)
    }.getOrDefault(false)

    fun scan(requestId: String, files: Map<ArtifactKind, Path>): BuildTransfer =
        BuildTransfer(requestId, files.mapNotNull { (kind, path) ->
            if (!Files.isRegularFile(path)) return@mapNotNull null
            runCatching { BuildArtifact(kind, path, sha256(path), Files.size(path)) }.getOrNull()
        })

    private fun sha256(path: Path): String {
        val digest = MessageDigest.getInstance("SHA-256")
        Files.newInputStream(path).use { input ->
            val buffer = ByteArray(DEFAULT_BUFFER_SIZE)
            var read = input.read(buffer)
            while (read >= 0) { if (read > 0) digest.update(buffer, 0, read); read = input.read(buffer) }
        }
        return digest.digest().joinToString("") { "%02x".format(it) }
    }
}
