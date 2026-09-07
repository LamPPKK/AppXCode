package dev.appxcode.ide.remote

const val BUILD_AGENT_PROTOCOL_VERSION = "1.0"

enum class AgentOperation { BUILD, TEST, ARCHIVE }
enum class AgentStatus { ACCEPTED, RUNNING, SUCCEEDED, FAILED, CANCELLED }

data class AgentCapabilities(
    val operations: Set<AgentOperation>,
    val platforms: Set<String>,
    val protocolVersion: String = BUILD_AGENT_PROTOCOL_VERSION,
)

data class AgentRequest(
    val requestId: String,
    val operation: AgentOperation,
    val projectPath: String,
    val scheme: String,
    val destination: String,
    val configuration: String = "Debug",
    val protocolVersion: String = BUILD_AGENT_PROTOCOL_VERSION,
)

data class AgentResponse(
    val requestId: String,
    val status: AgentStatus,
    val message: String? = null,
    val artifactPath: String? = null,
    val protocolVersion: String = BUILD_AGENT_PROTOCOL_VERSION,
)

data class AgentHealth(val ready: Boolean, val version: String = BUILD_AGENT_PROTOCOL_VERSION, val capabilities: AgentCapabilities? = null, val message: String? = null)

object BuildAgentProtocol {
    fun isCompatible(version: String): Boolean = version.substringBefore('.') == BUILD_AGENT_PROTOCOL_VERSION.substringBefore('.')
    fun validate(request: AgentRequest): List<String> = buildList {
        if (request.requestId.isBlank()) add("requestId is required")
        if (request.projectPath.isBlank()) add("projectPath is required")
        if (request.scheme.isBlank()) add("scheme is required")
        if (request.destination.isBlank()) add("destination is required")
        if (!isCompatible(request.protocolVersion)) add("unsupported protocol version: ${request.protocolVersion}")
    }

    fun validateHealth(health: AgentHealth): List<String> = buildList {
        if (!isCompatible(health.version)) add("unsupported agent version: ${health.version}")
        if (health.ready && health.capabilities == null) add("ready agent must advertise capabilities")
    }
}
