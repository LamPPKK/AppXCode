package dev.appxcode.ide.project

import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import java.nio.file.Files

class XcodeBuildConfigurationParserTest {
    @Test
    fun readsNamedConfigurationsAndBuildSettings() {
        val project = Files.createTempDirectory("appxcode-configs").resolve("App.xcodeproj")
        Files.createDirectories(project)
        Files.writeString(project.resolve("project.pbxproj"), """
            {
              objects = {
                AAAAAAAAAAAAAAAAAAAAAAAA /* Debug */ = {
                  isa = XCBuildConfiguration;
                  buildSettings = {
                    SDKROOT = iphoneos;
                    PRODUCT_BUNDLE_IDENTIFIER = com.example.app;
                    DEVELOPMENT_TEAM = ABC123;
                    "CODE_SIGN_IDENTITY[sdk=iphoneos*]" = "Apple Development";
                  };
                  name = Debug;
                };
                BBBBBBBBBBBBBBBBBBBBBBBB /* Debug */ = {
                  isa = XCBuildConfiguration;
                  buildSettings = {
                    PRODUCT_BUNDLE_IDENTIFIER = com.example.app.tests;
                    OTHER_LDFLAGS = (
                      "${'$'}(inherited)",
                      "value;with:semicolon",
                    );
                  };
                  name = Debug;
                };
                CCCCCCCCCCCCCCCCCCCCCCCC /* Release */ = {
                  isa = XCBuildConfiguration;
                  buildSettings = { SWIFT_OPTIMIZATION_LEVEL = "-O"; };
                  name = Release;
                };
                DDDDDDDDDDDDDDDDDDDDDDDD = {
                  isa = XCConfigurationList;
                  buildConfigurations = ( AAAAAAAAAAAAAAAAAAAAAAAA, );
                };
                EEEEEEEEEEEEEEEEEEEEEEEE = {
                  isa = XCConfigurationList;
                  buildConfigurations = ( BBBBBBBBBBBBBBBBBBBBBBBB, CCCCCCCCCCCCCCCCCCCCCCCC, );
                };
                FFFFFFFFFFFFFFFFFFFFFFFF = {
                  isa = PBXNativeTarget;
                  buildConfigurationList = EEEEEEEEEEEEEEEEEEEEEEEE;
                  name = AppTests;
                };
                111111111111111111111111 = {
                  isa = PBXProject;
                  buildConfigurationList = DDDDDDDDDDDDDDDDDDDDDDDD;
                };
              };
            }
        """.trimIndent())
        val configurations = XcodeProjectModel.readBuildConfigurations(project)
        assertEquals(3, configurations.size)
        val debug = configurations.first { it.ownerKind == XcodeConfigurationOwnerKind.PROJECT }
        assertEquals("iphoneos", debug.buildSettings["SDKROOT"])
        assertEquals("com.example.app", debug.buildSettings["PRODUCT_BUNDLE_IDENTIFIER"])
        assertEquals("Apple Development", debug.buildSettings["CODE_SIGN_IDENTITY[sdk=iphoneos*]"])
        val targetDebug = configurations.first { it.ownerKind == XcodeConfigurationOwnerKind.TARGET && it.name == "Debug" }
        assertEquals("AppTests", targetDebug.ownerName)
        assertEquals("com.example.app.tests", targetDebug.buildSettings["PRODUCT_BUNDLE_IDENTIFIER"])
        assertTrue(targetDebug.buildSettings["OTHER_LDFLAGS"].orEmpty().contains("\"value;with:semicolon\""))
        assertEquals("-O", configurations.first { it.name == "Release" }.buildSettings["SWIFT_OPTIMIZATION_LEVEL"])
    }
}
