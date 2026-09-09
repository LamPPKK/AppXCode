package dev.appxcode.ide.build

import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class BuildCancellationTest {
    @Test
    fun cancelAndResetAreThreadSafeStateTransitions() {
        val cancellation = BuildCancellation()
        assertFalse(cancellation.isCancelled())
        cancellation.cancel()
        assertTrue(cancellation.isCancelled())
        cancellation.reset()
        assertFalse(cancellation.isCancelled())
    }
}
