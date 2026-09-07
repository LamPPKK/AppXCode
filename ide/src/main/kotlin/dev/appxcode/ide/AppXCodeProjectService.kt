package dev.appxcode.ide
import com.intellij.openapi.components.Service
import com.intellij.openapi.project.Project
import com.intellij.openapi.Disposable
import java.util.concurrent.atomic.AtomicBoolean
import dev.appxcode.ide.project.XcodeContainer
import dev.appxcode.ide.project.XcodeProjectModel
import dev.appxcode.ide.project.XcodeProjectWatcher
import dev.appxcode.ide.test.TestFramework
import dev.appxcode.ide.test.TestFrameworkRegistry
import dev.appxcode.ide.test.DiscoveredTest
import dev.appxcode.ide.project.ProjectSnapshot
import dev.appxcode.ide.project.ProjectSnapshotLoader
import dev.appxcode.ide.debug.DebugSessionRegistry
import dev.appxcode.ide.debug.DebugSessionState
import dev.appxcode.ide.flutter.FlutterToolService
import dev.appxcode.ide.git.GitBranch
import dev.appxcode.ide.git.GitService
import dev.appxcode.ide.git.GitStatus
import java.nio.file.Path
import java.util.concurrent.CopyOnWriteArrayList
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
class AppXCodeProjectService(private val project: Project) : Disposable {
    private val initialized = AtomicBoolean(false)
    private val swiftSymbols = SwiftSymbolIndex()
    private val git = GitService()
    private val debugSessions = DebugSessionRegistry()
    private val flutter = FlutterToolService()
    @Volatile private var projectWatcher: XcodeProjectWatcher? = null
    private val changeListeners = CopyOnWriteArrayList<(Path) -> Unit>()
    fun initialize() {
        if (initialized.compareAndSet(false, true)) {
            project.basePath?.let { base ->
                val root = Path.of(base)
                if (java.nio.file.Files.isDirectory(root)) {
                    projectWatcher = XcodeProjectWatcher(root).also { watcher ->
                        watcher.onChange { changed -> changeListeners.forEach { it(changed) } }
                    }
                }
            }
        }
    }
    fun onProjectFileChange(listener: (Path) -> Unit) { changeListeners += listener }
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
    fun discoverXCTest(root: Path): List<DiscoveredTest> = TestFrameworkRegistry.discoverXCTest(root)
    fun discoverTests(root: Path): List<DiscoveredTest> = TestFrameworkRegistry.discover(root)
    fun snapshot(root: Path): ProjectSnapshot = ProjectSnapshotLoader.load(root)
    fun gitStatus(root: Path): GitStatus = git.status(root)
    fun gitBranches(root: Path): List<GitBranch> = git.branches(root)
    fun createDebugSession(sessionId: String) = debugSessions.create(sessionId)
    fun updateDebugSession(sessionId: String, state: DebugSessionState) = debugSessions.update(sessionId, state)
    fun debugSessionState(sessionId: String): DebugSessionState? = debugSessions.state(sessionId)
    fun flutterService(): FlutterToolService = flutter
    override fun dispose() {
        projectWatcher?.close()
        projectWatcher = null
        changeListeners.clear()
    }
}
