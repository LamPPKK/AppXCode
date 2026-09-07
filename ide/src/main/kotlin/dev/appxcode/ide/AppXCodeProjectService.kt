package dev.appxcode.ide
import com.intellij.openapi.components.Service
import com.intellij.openapi.project.Project
import java.util.concurrent.atomic.AtomicBoolean
import dev.appxcode.ide.project.XcodeContainer
import dev.appxcode.ide.project.XcodeProjectModel
import dev.appxcode.ide.test.TestFramework
import dev.appxcode.ide.test.TestFrameworkRegistry
import dev.appxcode.ide.project.ProjectSnapshot
import dev.appxcode.ide.project.ProjectSnapshotLoader
import java.nio.file.Path
import dev.appxcode.ide.toolchain.AppleToolchain
import dev.appxcode.ide.toolchain.AppleToolchainDetector
import dev.appxcode.ide.dependency.DependencyModel
import dev.appxcode.ide.dependency.DependencyPin
import dev.appxcode.ide.flutter.FlutterProject
import dev.appxcode.ide.flutter.FlutterProjectDetector
import dev.appxcode.ide.language.SwiftSymbol
import dev.appxcode.ide.language.SwiftSymbolIndex
import dev.appxcode.ide.project.XcodeTarget
import dev.appxcode.ide.project.XcodeProjectModel
@Service(Service.Level.PROJECT)
class AppXCodeProjectService(private val project: Project) {
    private val initialized = AtomicBoolean(false)
    private val swiftSymbols = SwiftSymbolIndex()
    fun initialize() { initialized.compareAndSet(false, true) }
    fun isInitialized(): Boolean = initialized.get()
    fun projectName(): String = project.name
    fun discoverXcodeContainers(root: Path): List<XcodeContainer> = XcodeProjectModel.discover(root)
    fun appleToolchain(): AppleToolchain = AppleToolchainDetector.detect()
    fun dependencies(root: Path): List<DependencyPin> = DependencyModel.read(root)
    fun flutterProject(root: Path): FlutterProject? = FlutterProjectDetector.detect(root)
    fun indexSwift(files: Iterable<Path>) { swiftSymbols.index(files) }
    fun findSwiftSymbols(name: String): List<SwiftSymbol> = swiftSymbols.find(name)
    fun completeSwift(prefix: String): List<SwiftSymbol> = swiftSymbols.complete(prefix)
    fun xcodeTargets(projectFile: Path): List<XcodeTarget> = XcodeProjectModel.readTargets(projectFile)
    fun testFrameworks(root: Path): Set<TestFramework> = TestFrameworkRegistry.detect(root)
    fun snapshot(root: Path): ProjectSnapshot = ProjectSnapshotLoader.load(root)
}
