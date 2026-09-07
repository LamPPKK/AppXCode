package dev.appxcode.ide.remote

import java.nio.file.Files
import java.nio.file.Path
import java.security.MessageDigest

object BuildArtifactScanner {
    fun verify(artifact: BuildArtifact): Boolean =
        Files.isRegularFile(artifact.path) && Files.size(artifact.path) == artifact.sizeBytes && sha256(artifact.path) == artifact.sha256

    fun scan(requestId: String, files: Map<ArtifactKind, Path>): BuildTransfer =
        BuildTransfer(requestId, files.mapNotNull { (kind, path) ->
            if (!Files.isRegularFile(path)) return@mapNotNull null
            BuildArtifact(kind, path, sha256(path), Files.size(path))
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
