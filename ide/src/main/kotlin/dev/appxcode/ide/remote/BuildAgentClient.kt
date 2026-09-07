package dev.appxcode.ide.remote

import java.util.concurrent.ConcurrentHashMap

interface BuildAgentTransport {
    fun submit(request: BuildAgentRequest): BuildAgentResponse
    fun cancel(requestId: String): BuildAgentResponse
}

class BuildAgentClient(private val transport: BuildAgentTransport, private val retryPolicy: RetryPolicy = RetryPolicy()) {
    private val states = ConcurrentHashMap<String, BuildAgentResponse>()

    fun submit(request: BuildAgentRequest): BuildAgentResponse {
        if (!request.isValid()) return BuildAgentResponse.rejected(request, BuildAgentErrorCode.INVALID_REQUEST, "invalid build agent request")
        if (request.isCancelled) return BuildAgentResponse.cancelled(request)
        if (states.putIfAbsent(request.requestId, BuildAgentResponse.accepted(request)) != null) {
            return BuildAgentResponse.rejected(request, BuildAgentErrorCode.INVALID_REQUEST, "requestId already exists")
        }
        return runCatching { submitWithRetry(request) }.getOrElse {
            BuildAgentResponse.failed(request, BuildAgentErrorCode.TRANSPORT_UNAVAILABLE, it.message ?: "transport failure")
        }.also { states[request.requestId] = it }
    }

    private fun submitWithRetry(request: BuildAgentRequest): BuildAgentResponse {
        var attempt = 1
        while (true) {
            val response = runCatching { transport.submit(request) }.getOrElse { throw it }
            val retryable = response.errorCode == BuildAgentErrorCode.TRANSPORT_UNAVAILABLE || response.errorCode == BuildAgentErrorCode.TIMEOUT
            if (!response.isError || !retryable || attempt >= retryPolicy.maxAttempts) return response
            Thread.sleep(retryPolicy.delayFor(attempt++))
        }
    }

    fun cancel(requestId: String): BuildAgentResponse {
        if (!states.containsKey(requestId)) return BuildAgentResponse(requestId = requestId, accepted = false, errorCode = BuildAgentErrorCode.INVALID_REQUEST, message = "unknown requestId")
        return runCatching { transport.cancel(requestId) }.getOrElse { BuildAgentResponse(requestId = requestId, accepted = false, errorCode = BuildAgentErrorCode.TRANSPORT_UNAVAILABLE, message = it.message ?: "transport failure") }
            .also { states[requestId] = it }
    }

    fun status(requestId: String): BuildAgentResponse? = states[requestId]
}
