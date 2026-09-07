package dev.appxcode.ide.remote

data class BuildAgentRequest(
    val protocolVersion: Int = CURRENT_PROTOCOL_VERSION,
    val requestId: String,
    val operation: String,
    val projectPath: String,
    val timeoutMillis: Long = 900_000,
    val cancellationRequested: Boolean = false,
) {
    val isCancelled: Boolean get() = cancellationRequested
    val typedOperation: BuildAgentOperation? get() = BuildAgentOperation.fromWireName(operation)
    val isKnownOperation: Boolean get() = typedOperation != null
    fun cancelledCopy(): BuildAgentRequest = copy(cancellationRequested = true)

    constructor(requestId: String, operation: BuildAgentOperation, projectPath: String, timeoutMillis: Long = 900_000) :
        this(CURRENT_PROTOCOL_VERSION, requestId, operation.wireName, projectPath, timeoutMillis, false)

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

    fun matches(content: ByteArray): Boolean {
        val digest = java.security.MessageDigest.getInstance("SHA-256").digest(content)
            .joinToString("") { "%02x".format(it) }
        return digest.equals(sha256, ignoreCase = true) && content.size.toLong() == sizeBytes
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

data class BuildAgentCancelRequest(
    val requestId: String,
    val reason: String = "cancelled by client",
) {
    init {
        require(requestId.isNotBlank()) { "Cancel request id must not be blank" }
        require(reason.isNotBlank()) { "Cancel reason must not be blank" }
    }
}

data class BuildAgentEndpoint(
    val host: String,
    val port: Int,
    val tlsEnabled: Boolean = true,
    val pairingId: String? = null,
) {
    init {
        require(host.isNotBlank()) { "Build agent host must not be blank" }
        require(port in 1..65535) { "Build agent port must be between 1 and 65535" }
        require(pairingId == null || pairingId.isNotBlank()) { "Build agent pairing id must not be blank" }
    }

    fun baseUri(): java.net.URI = java.net.URI("${if (tlsEnabled) "https" else "http"}://$host:$port")

    fun uri(path: String): java.net.URI {
        require(path.startsWith("/")) { "Build agent API path must start with /" }
        return baseUri().resolve(path)
    }
}

data class BuildAgentArtifactRequest(
    val requestId: String,
    val references: List<String>,
) {
    init {
        require(requestId.isNotBlank()) { "Artifact request id must not be blank" }
        require(references.isNotEmpty()) { "Artifact request must include at least one reference" }
        require(references.all { it.isNotBlank() }) { "Artifact references must not be blank" }
        require(references.distinct().size == references.size) { "Artifact references must be unique" }
    }
}

fun BuildAgentArtifactRequest.accepted(artifacts: List<BuildAgentArtifact>, message: String = "artifacts ready"): BuildAgentResponse =
    BuildAgentResponse(requestId = requestId, accepted = true, message = message, artifacts = artifacts.map { it.reference }, artifactMetadata = artifacts)

fun BuildAgentArtifactRequest.rejected(errorCode: String, message: String): BuildAgentResponse =
    BuildAgentResponse(requestId = requestId, accepted = false, message = message, errorCode = errorCode)

data class BuildAgentHealth(
    val protocolVersion: Int = CURRENT_PROTOCOL_VERSION,
    val agentId: String,
    val online: Boolean,
    val toolchainAvailable: Boolean,
    val message: String = "",
) {
    val ready: Boolean get() = online && toolchainAvailable && protocolVersion == CURRENT_PROTOCOL_VERSION

    init {
        require(protocolVersion == CURRENT_PROTOCOL_VERSION) { "Unsupported build agent protocol: $protocolVersion" }
        require(agentId.isNotBlank()) { "Build agent id must not be blank" }
    }
}
