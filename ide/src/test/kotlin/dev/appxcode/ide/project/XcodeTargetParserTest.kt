package dev.appxcode.ide.project

import org.junit.Assert.assertEquals
import org.junit.Test
import java.nio.file.Files

class XcodeTargetParserTest {
    @Test
    fun readsNativeTargetsFromRealisticPbxObjects() {
        val project = Files.createTempDirectory("appxcode-targets").resolve("Targets.xcodeproj")
        Files.createDirectories(project)
        Files.writeString(project.resolve("project.pbxproj"), """
            // !$*UTF8*$!
            {
              objects = {
                13B07F861A680F5B00A75B9A /* App */ = {
                  isa = PBXNativeTarget;
                  buildPhases = ( 13B07F8E1A680F5B00A75B9A, );
                  name = App;
                  productName = App;
                  productType = "com.apple.product-type.application";
                };
                AAAAAAAAAAAAAAAAAAAAAAAA /* Watch { App } */ = {
                  isa = PBXNativeTarget;
                  /* isa = PBXProject; name = Phantom; { \" } */
                  name = "Watch App";
                  productName = "Watch App";
                  productType = "com.apple.product-type.application.watchapp2";
                };
                BBBBBBBBBBBBBBBBBBBBBBBB /* Project */ = { isa = PBXProject; name = Ignored; };
                /*
                DDDDDDDDDDDDDDDDDDDDDDDD = {
                  isa = PBXNativeTarget;
                  name = CommentedOut;
                  productType = "com.apple.product-type.application";
                };
                */
              };
            }
        """.trimIndent())
        val targets = XcodeProjectModel.readTargets(project)
        assertEquals(listOf("App", "Watch App"), targets.map { it.name })
        assertEquals("com.apple.product-type.application.watchapp2", targets.last().productType)
    }

    @Test
    fun supportsPbxprojPathAndMalformedObject() {
        val pbx = Files.createTempFile("project", ".pbxproj")
        Files.writeString(pbx, "CCCCCCCCCCCCCCCCCCCCCCCC = { isa = PBXNativeTarget; name = Broken;")
        assertEquals(emptyList<XcodeTarget>(), XcodeProjectModel.readTargets(pbx))
    }
}
