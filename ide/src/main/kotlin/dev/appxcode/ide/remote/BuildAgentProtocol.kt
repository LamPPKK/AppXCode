package dev.appxcode.ide.remote

data class BuildAgentRequest(
    val protocolVersion: Int = CURRENT_PROTOCOL_VERSION,
    val requestId: String,
    val operation: String,
    val projectPath: String,
) {
    init {
        require(protocolVersion == CURRENT_PROTOCOL_VERSION) { "Unsupported build agent protocol: $protocolVersion" }
        require(requestId.isNotBlank()) { "Build agent request id must not be blank" }
        require(operation.isNotBlank()) { "Build agent operation must not be blank" }
        require(projectPath.isNotBlank()) { "Build agent project path must not be blank" }
    }
}

data class BuildAgentResponse(
    val protocolVersion: Int = CURRENT_PROTOCOL_VERSION,
    val requestId: String,
    val accepted: Boolean,
    val message: String = "",
    val errorCode: String? = null,
) {
    init {
        require(protocolVersion == CURRENT_PROTOCOL_VERSION) { "Unsupported build agent protocol: $protocolVersion" }
        require(requestId.isNotBlank()) { "Build agent response id must not be blank" }
        require(accepted || !errorCode.isNullOrBlank()) { "Rejected responses must include an error code" }
    }

    companion object {
        fun accepted(request: BuildAgentRequest, message: String = "accepted"): BuildAgentResponse =
            BuildAgentResponse(requestId = request.requestId, accepted = true, message = message)

        fun rejected(request: BuildAgentRequest, errorCode: String, message: String): BuildAgentResponse =
            BuildAgentResponse(requestId = request.requestId, accepted = false, message = message, errorCode = errorCode)
    }
}

const val CURRENT_PROTOCOL_VERSION: Int = 1
