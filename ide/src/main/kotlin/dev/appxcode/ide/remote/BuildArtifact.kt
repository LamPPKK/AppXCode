package dev.appxcode.ide.remote

import java.nio.file.Path

enum class ArtifactKind { APP, IPA, XCARCHIVE, LOG }

data class BuildArtifact(
    val kind: ArtifactKind,
    val path: Path,
    val sha256: String,
    val sizeBytes: Long,
)

data class BuildTransfer(val requestId: String, val artifacts: List<BuildArtifact>, val totalBytes: Long = artifacts.sumOf { it.sizeBytes }) {
    init {
        require(requestId.isNotBlank()) { "Transfer request id must not be blank" }
        require(totalBytes >= 0) { "Transfer size must not be negative" }
        require(totalBytes == artifacts.sumOf { it.sizeBytes }) { "Transfer size does not match artifacts" }
        require(artifacts.map { it.path.toAbsolutePath().normalize() }.distinct().size == artifacts.size) { "Transfer artifacts must be unique" }
    }
}
