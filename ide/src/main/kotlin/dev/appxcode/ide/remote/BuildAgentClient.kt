package dev.appxcode.ide.remote

import java.util.concurrent.ConcurrentHashMap

interface BuildAgentTransport {
    fun submit(request: AgentRequest): AgentResponse
    fun cancel(requestId: String): AgentResponse
}

class BuildAgentClient(private val transport: BuildAgentTransport, private val retryPolicy: RetryPolicy = RetryPolicy()) {
    private val states = ConcurrentHashMap<String, AgentStatus>()

    fun submit(request: AgentRequest): AgentResponse {
        val errors = BuildAgentProtocol.validate(request)
        if (errors.isNotEmpty()) return AgentResponse(request.requestId, AgentStatus.FAILED, errors.joinToString("; "))
        if (states.putIfAbsent(request.requestId, AgentStatus.ACCEPTED) != null) {
            return AgentResponse(request.requestId, AgentStatus.FAILED, "requestId already exists")
        }
        return runCatching { submitWithRetry(request) }.getOrElse {
            states[request.requestId] = AgentStatus.FAILED
            return AgentResponse(request.requestId, AgentStatus.FAILED, it.message ?: "transport failure")
        }.also { states[request.requestId] = it.status }
    }

    private fun submitWithRetry(request: AgentRequest): AgentResponse {
        var attempt = 1
        while (true) {
            val response = runCatching { transport.submit(request) }.getOrElse { throw it }
            val retryable = response.message?.contains("network", true) == true || response.message?.contains("timeout", true) == true
            if (response.status != AgentStatus.FAILED || !retryable || attempt >= retryPolicy.maxAttempts) return response
            Thread.sleep(retryPolicy.delayFor(attempt++))
        }
    }

    fun cancel(requestId: String): AgentResponse {
        if (!states.containsKey(requestId)) return AgentResponse(requestId, AgentStatus.FAILED, "unknown requestId")
        return runCatching { transport.cancel(requestId) }.getOrElse { AgentResponse(requestId, AgentStatus.FAILED, it.message ?: "transport failure") }
            .also { states[requestId] = it.status }
    }

    fun status(requestId: String): AgentStatus? = states[requestId]
}
