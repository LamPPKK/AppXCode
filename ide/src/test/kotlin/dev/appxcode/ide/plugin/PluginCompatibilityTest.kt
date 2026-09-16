package dev.appxcode.ide.plugin

import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class PluginCompatibilityTest {
    @Test
    fun acceptsWildcardBuildRange() {
        val descriptor = PluginDescriptor("com.example.flutter", "251.*", "252.*", setOf("com.intellij.modules.platform"))
        assertTrue(PluginCompatibility.check(descriptor, "251.18000", setOf("com.intellij.modules.platform")).compatible)
        assertTrue(PluginCompatibility.check(descriptor, "252.100", setOf("com.intellij.modules.platform")).compatible)
        assertFalse(PluginCompatibility.check(descriptor, "253.1", setOf("com.intellij.modules.platform")).compatible)
    }
}
