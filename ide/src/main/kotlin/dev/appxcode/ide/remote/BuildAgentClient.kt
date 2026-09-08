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
    private val requests = ConcurrentHashMap<String, BuildAgentRequest>()

    fun submit(request: BuildAgentRequest): BuildAgentResponse {
        if (!request.isValid()) return BuildAgentResponse.rejected(request, BuildAgentErrorCode.INVALID_REQUEST, "invalid build agent request")
        if (request.isCancelled) return BuildAgentResponse.cancelled(request)
        if (states.putIfAbsent(request.requestId, BuildAgentResponse.accepted(request)) != null) {
            return BuildAgentResponse.rejected(request, BuildAgentErrorCode.INVALID_REQUEST, "requestId already exists")
        }
        requests[request.requestId] = request
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
    fun request(requestId: String): BuildAgentRequest? = requests[requestId]

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
        requests.clear()
        return count
    }

    fun retry(requestId: String): BuildAgentResponse? {
        val previous = states[requestId] ?: return null
        if (!previous.isError || previous.isCancelled) return previous
        val original = requests[requestId] ?: return null
        return submit(original.copy(requestId = java.util.UUID.randomUUID().toString()))
    }

    fun retryFailed(): List<BuildAgentResponse> = failedRequests()
        .mapNotNull { previous ->
            retry(previous.requestId)?.also { retried ->
                if (retried.isSuccessful) {
                    states.remove(previous.requestId, previous)
                    requests.remove(previous.requestId)
                }
            }
        }

    fun forgetFailed(): Int {
        val failed = failedRequests()
        failed.forEach { response ->
            states.remove(response.requestId, response)
            requests.remove(response.requestId)
        }
        return failed.size
    }

    fun downloadArtifact(request: BuildAgentArtifactRequest, destination: java.nio.file.Path): BuildAgentResponse {
        if (!request.isValid || !destination.isAbsolute) {
            return BuildAgentResponse(requestId = request.requestId, accepted = false, errorCode = BuildAgentErrorCode.INVALID_REQUEST, message = "invalid artifact download request")
        }
        return runCatching {
            java.nio.file.Files.createDirectories(destination)
            transport.let { (it as? BuildArtifactTransport)?.download(request, destination)
                ?: BuildAgentResponse(requestId = request.requestId, accepted = false, errorCode = BuildAgentErrorCode.INVALID_REQUEST, message = "artifact transport is unavailable") }
        }.getOrElse { BuildAgentResponse(requestId = request.requestId, accepted = false, errorCode = BuildAgentErrorCode.TRANSPORT_UNAVAILABLE, message = it.message ?: "artifact download failed") }
    }

    fun downloadArtifacts(request: BuildAgentArtifactRequest, destination: java.nio.file.Path): BuildAgentResponse =
        downloadArtifact(request.normalized(), destination)

    fun forget(requestId: String): Boolean = states.remove(requestId) != null

    fun forgetCompleted(): Int {
        val completed = states.entries.filter { it.value.isSuccessful || it.value.errorCode == BuildAgentErrorCode.CANCELLED }
        completed.forEach { states.remove(it.key, it.value) }
        return completed.size
    }
}
