package dev.appxcode.ide.remote

import java.nio.file.Path

enum class ArtifactKind { APP, IPA, XCARCHIVE, LOG }

data class BuildArtifact(
    val kind: ArtifactKind,
    val path: Path,
    val sha256: String,
    val sizeBytes: Long,
)

data class BuildTransfer(val requestId: String, val artifacts: List<BuildArtifact>, val totalBytes: Long = artifacts.sumOf { it.sizeBytes })
