package dev.appxcode.ide.build

import java.util.concurrent.atomic.AtomicBoolean

class BuildCancellation {
    private val cancelled = AtomicBoolean(false)
    fun cancel() { cancelled.set(true) }
    fun reset() { cancelled.set(false) }
    fun isCancelled(): Boolean = cancelled.get()
}
