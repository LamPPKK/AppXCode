package dev.appxcode.ide.test

import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import java.nio.file.Files

class TestFrameworkRegistryTest {
    @Test
    fun detectsFrameworksFromImportsAndDependencyManifests() {
        val root = Files.createTempDirectory("appxcode-tests")
        Files.writeString(root.resolve("Package.swift"), "dependencies: [ .package(url: \"https://github.com/Quick/Quick\", from: \"7.0.0\") ]")
        Files.writeString(root.resolve("Spec.swift"), "import XCTest\nimport Quick\nfinal class Spec: QuickSpec {}")
        val detected = TestFrameworkRegistry.detect(root)
        assertTrue(detected.contains(TestFramework.XCTEST))
        assertTrue(detected.contains(TestFramework.QUICK))
        assertEquals(false, detected.contains(TestFramework.KIWI))
    }
}
