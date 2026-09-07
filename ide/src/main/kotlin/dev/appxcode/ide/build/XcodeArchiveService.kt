package dev.appxcode.ide.build

import java.nio.file.Path
import java.time.Duration

data class ArchiveRequest(val container: Path, val scheme: String, val destination: String, val archivePath: Path, val configuration: String = "Release") {
    init {
        require(scheme.isNotBlank()) { "Archive scheme must not be blank" }
        require(destination.isNotBlank()) { "Archive destination must not be blank" }
        require(configuration.isNotBlank()) { "Archive configuration must not be blank" }
        require(container.fileName != null) { "Archive container must point to a file" }
        require(archivePath.fileName != null) { "Archive path must point to a file" }
    }
}
data class ArchiveResult(val build: XcodeBuildResult, val archivePath: Path?)

class XcodeArchiveService(private val builder: XcodeBuildService) {
    fun archive(request: ArchiveRequest, timeout: Duration = Duration.ofMinutes(30)): ArchiveResult {
        request.archivePath.parent?.toFile()?.mkdirs()
        val result = builder.execute(
            XcodeBuildRequest(request.container, request.scheme, request.destination, request.configuration, action = "archive", arguments = listOf("-archivePath", request.archivePath.toString())), timeout
        )
        return ArchiveResult(result, request.archivePath.takeIf { result.succeeded })
    }
}
