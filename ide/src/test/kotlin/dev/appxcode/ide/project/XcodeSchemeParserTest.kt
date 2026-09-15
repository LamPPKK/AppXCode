package dev.appxcode.ide.project

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test
import java.nio.file.Files

class XcodeSchemeParserTest {
    @Test
    fun separatesBuildablesFromTestables() {
        val path = Files.createTempFile("App", ".xcscheme")
        Files.writeString(path, """
            <Scheme>
              <BuildAction><BuildActionEntries><BuildActionEntry><BuildableReference BuildableName="App.app" BlueprintName="App"/></BuildActionEntry></BuildActionEntries></BuildAction>
              <TestAction><Testables><TestableReference><BuildableReference BuildableName="AppTests.xctest" BlueprintName="AppTests"/></TestableReference></Testables></TestAction>
              <LaunchAction><BuildableProductRunnable><BuildableReference BuildableName="App.app" BlueprintName="App"/></BuildableProductRunnable></LaunchAction>
            </Scheme>
        """.trimIndent())
        val scheme = XcodeProjectModel.readScheme(path)!!
        assertEquals(listOf("App.app"), scheme.buildables)
        assertEquals(listOf("AppTests"), scheme.testables)
    }

    @Test
    fun rejectsMalformedAndDoctypeDocuments() {
        val malformed = Files.createTempFile("Broken", ".xcscheme")
        Files.writeString(malformed, "<Scheme>")
        assertNull(XcodeProjectModel.readScheme(malformed))
        val doctype = Files.createTempFile("Unsafe", ".xcscheme")
        Files.writeString(doctype, "<!DOCTYPE Scheme [<!ENTITY xxe SYSTEM \"file:///etc/passwd\">]><Scheme>&xxe;</Scheme>")
        assertNull(XcodeProjectModel.readScheme(doctype))
        val wrongRoot = Files.createTempFile("NotScheme", ".xcscheme")
        Files.writeString(wrongRoot, "<Workspace/>")
        assertNull(XcodeProjectModel.readScheme(wrongRoot))
    }
}
