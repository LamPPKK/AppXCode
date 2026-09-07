package dev.appxcode.ide.remote

import java.util.concurrent.ConcurrentHashMap

interface BuildAgentTransport {
    fun submit(request: BuildAgentRequest): BuildAgentResponse
    fun cancel(requestId: String): BuildAgentResponse
}

interface BuildArtifactTransport {
    fun download(request: BuildAgentArtifactRequest, destination: java.nio.file.Path): BuildAgentResponse
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
        val timeoutNanos = if (request.timeoutMillis > Long.MAX_VALUE / 1_000_000L) Long.MAX_VALUE else request.timeoutMillis * 1_000_000L
        val start = System.nanoTime()
        val deadline = if (Long.MAX_VALUE - start < timeoutNanos) Long.MAX_VALUE else start + timeoutNanos
        while (true) {
            if (System.nanoTime() >= deadline) return BuildAgentResponse.failed(request, BuildAgentErrorCode.TIMEOUT, "request timeout exceeded")
            val response = runCatching { transport.submit(request) }.getOrElse { throw it }
            val retryable = RetryDecider.shouldRetry(response.errorCode)
            if (!response.isError || !retryable || attempt >= retryPolicy.maxAttempts) return response
            val delay = retryPolicy.delayFor(attempt++).coerceAtMost(((deadline - System.nanoTime()) / 1_000_000L).coerceAtLeast(0))
            if (delay > 0) Thread.sleep(delay)
        }
    }

    fun cancel(requestId: String): BuildAgentResponse {
        val current = states[requestId]
        if (current == null) return BuildAgentResponse(requestId = requestId, accepted = false, errorCode = BuildAgentErrorCode.INVALID_REQUEST, message = "unknown requestId")
        if (current.isSuccessful || current.errorCode == BuildAgentErrorCode.CANCELLED) return current
        return runCatching { transport.cancel(requestId) }.getOrElse { BuildAgentResponse(requestId = requestId, accepted = false, errorCode = BuildAgentErrorCode.TRANSPORT_UNAVAILABLE, message = it.message ?: "transport failure") }
            .also { states[requestId] = it }
    }

    fun status(requestId: String): BuildAgentResponse? = states[requestId]

    fun activeRequests(): List<BuildAgentResponse> = states.values
        .filter { !it.isSuccessful && it.errorCode != BuildAgentErrorCode.CANCELLED && !it.isError }
        .sortedBy(BuildAgentResponse::requestId)

    fun completedRequests(): List<BuildAgentResponse> = states.values
        .filter { it.isSuccessful || it.errorCode == BuildAgentErrorCode.CANCELLED }
        .sortedBy(BuildAgentResponse::requestId)

    fun failedRequests(): List<BuildAgentResponse> = states.values
        .filter { it.isError }
        .sortedBy(BuildAgentResponse::requestId)

    fun cancelAll(): List<BuildAgentResponse> = activeRequests().map { cancel(it.requestId) }

    fun clear(): Int {
        val count = states.size
        states.clear()
        return count
    }

    fun forget(requestId: String): Boolean = states.remove(requestId) != null

    fun forgetCompleted(): Int {
        val completed = states.entries.filter { it.value.isSuccessful || it.value.errorCode == BuildAgentErrorCode.CANCELLED }
        completed.forEach { states.remove(it.key, it.value) }
        return completed.size
    }
}
