package dev.appxcode.ide.remote

data class BuildAgentRequest(
    val protocolVersion: Int = CURRENT_PROTOCOL_VERSION,
    val requestId: String,
    val operation: String,
    val projectPath: String,
    val timeoutMillis: Long = 900_000,
    val cancellationRequested: Boolean = false,
) {
    init {
        require(protocolVersion == CURRENT_PROTOCOL_VERSION) { "Unsupported build agent protocol: $protocolVersion" }
        require(requestId.isNotBlank()) { "Build agent request id must not be blank" }
        require(operation.isNotBlank()) { "Build agent operation must not be blank" }
        require(operation == operation.trim()) { "Build agent operation must not contain surrounding whitespace" }
        require(projectPath.isNotBlank()) { "Build agent project path must not be blank" }
        require(timeoutMillis > 0) { "Build agent timeout must be positive" }
    }
}

data class BuildAgentResponse(
    val protocolVersion: Int = CURRENT_PROTOCOL_VERSION,
    val requestId: String,
    val accepted: Boolean,
    val message: String = "",
    val errorCode: String? = null,
    val artifacts: List<String> = emptyList(),
    val artifactMetadata: List<BuildAgentArtifact> = emptyList(),
) {
    val status: BuildAgentResponseStatus get() = if (accepted) BuildAgentResponseStatus.ACCEPTED else BuildAgentResponseStatus.REJECTED
    val isError: Boolean get() = !accepted
    init {
        require(protocolVersion == CURRENT_PROTOCOL_VERSION) { "Unsupported build agent protocol: $protocolVersion" }
        require(requestId.isNotBlank()) { "Build agent response id must not be blank" }
        require(accepted || !errorCode.isNullOrBlank()) { "Rejected responses must include an error code" }
        require(artifacts.all { it.isNotBlank() }) { "Artifact references must not be blank" }
        require(artifactMetadata.map { it.reference }.distinct().size == artifactMetadata.size) { "Artifact metadata references must be unique" }
    }

    companion object {
        fun accepted(request: BuildAgentRequest, message: String = "accepted"): BuildAgentResponse =
            BuildAgentResponse(requestId = request.requestId, accepted = true, message = message)

        fun rejected(request: BuildAgentRequest, errorCode: String, message: String): BuildAgentResponse =
            BuildAgentResponse(requestId = request.requestId, accepted = false, message = message, errorCode = errorCode)
    }
}

enum class BuildAgentResponseStatus { ACCEPTED, REJECTED }

enum class BuildAgentLogLevel { DEBUG, INFO, WARN, ERROR }

data class BuildAgentLogEvent(
    val requestId: String,
    val sequence: Long,
    val level: BuildAgentLogLevel = BuildAgentLogLevel.INFO,
    val message: String,
) {
    init {
        require(requestId.isNotBlank()) { "Build agent log request id must not be blank" }
        require(sequence >= 0) { "Build agent log sequence must not be negative" }
        require(message.isNotEmpty()) { "Build agent log message must not be empty" }
    }
}

data class BuildAgentArtifact(
    val reference: String,
    val sizeBytes: Long,
    val sha256: String,
) {
    init {
        require(reference.isNotBlank()) { "Artifact reference must not be blank" }
        require(sizeBytes >= 0) { "Artifact size must not be negative" }
        require(sha256.matches(Regex("[0-9a-fA-F]{64}"))) { "Artifact sha256 must be 64 hexadecimal characters" }
    }
}

const val CURRENT_PROTOCOL_VERSION: Int = 1

enum class BuildAgentOperation {
    BUILD, TEST, ARCHIVE, EXPORT, INSTALL, LOGS;

    val wireName: String get() = name.lowercase()

    companion object {
        fun fromWireName(value: String): BuildAgentOperation? =
            entries.firstOrNull { it.wireName == value.trim().lowercase() }
    }
}

object BuildAgentErrorCode {
    const val UNSUPPORTED_PROTOCOL = "unsupported_protocol"
    const val INVALID_REQUEST = "invalid_request"
    const val UNAUTHORIZED = "unauthorized"
    const val TOOLCHAIN_UNAVAILABLE = "toolchain_unavailable"
    const val BUILD_FAILED = "build_failed"
    const val CANCELLED = "cancelled"
}
