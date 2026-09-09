package dev.appxcode.ide.flutter

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test
import java.nio.file.Files

class FlutterToolServiceTest {
    @Test
    fun pubCommandsValidateProjectAndForwardAction() {
        val root = Files.createTempDirectory("appxcode-flutter")
        val calls = mutableListOf<List<String>>()
        val service = FlutterToolService("flutter") { args, _ -> calls += args; FlutterCommandResult(true, "ok", 0) }
        assertFalse(service.pubOutdated(root).success)
        Files.writeString(root.resolve("pubspec.yaml"), "name: sample\n")
        assertTrue(service.pubOutdated(root).success)
        assertTrue(service.pubUpgrade(root).success)
        assertTrue(service.pubDeps(root).success)
        assertEquals(listOf("flutter", "pub", "outdated"), calls[0])
        assertEquals(listOf("flutter", "pub", "upgrade"), calls[1])
        assertEquals(listOf("flutter", "pub", "deps"), calls[2])
    }
}
