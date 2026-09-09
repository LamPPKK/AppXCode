package dev.appxcode.ide.remote

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Test

class BuildAgentHealthGateTest {
    @Test
    fun staleHealthPreventsTransportSubmission() {
        var submissions = 0
        val transport = object : BuildAgentTransport {
            override fun submit(request: BuildAgentRequest): BuildAgentResponse { submissions++; return BuildAgentResponse.accepted(request) }
            override fun cancel(requestId: String): BuildAgentResponse = BuildAgentResponse(requestId = requestId, accepted = false, errorCode = BuildAgentErrorCode.CANCELLED)
        }
        val health = BuildAgentHealth.ready("mac").copy(observedAtEpochMillis = 1)
        val client = BuildAgentClient(transport, healthProvider = { health }, healthMaxAgeMillis = 10)
        val response = client.submit(BuildAgentRequest("r1", BuildAgentOperation.BUILD, "/tmp/App.xcodeproj"))
        assertFalse(response.accepted)
        assertEquals(BuildAgentErrorCode.HEALTH_STALE, response.errorCode)
        assertEquals(0, submissions)
    }
}
