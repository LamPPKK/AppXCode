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
import dev.appxcode.ide.git.GitCommit
import dev.appxcode.ide.git.GitStash
import dev.appxcode.ide.git.GitRemote
import dev.appxcode.ide.git.GitTag
import dev.appxcode.ide.git.GitSyncStatus
import java.nio.file.Path
import java.util.concurrent.CopyOnWriteArrayList
import dev.appxcode.ide.toolchain.AppleToolchain
import dev.appxcode.ide.toolchain.AppleToolchainDetector
import dev.appxcode.ide.dependency.DependencyModel
import dev.appxcode.ide.dependency.DependencyPin
import dev.appxcode.ide.dependency.DependencyResolver
import dev.appxcode.ide.dependency.ResolveResult
import dev.appxcode.ide.flutter.FlutterProject
import dev.appxcode.ide.flutter.FlutterProjectDetector
import dev.appxcode.ide.flutter.FlutterCommandResult
import dev.appxcode.ide.language.SwiftSymbol
import dev.appxcode.ide.language.SwiftSymbolIndex
import dev.appxcode.ide.language.ObjCSymbol
import dev.appxcode.ide.language.ObjCSymbolIndex
import dev.appxcode.ide.language.SwiftLanguageService
import dev.appxcode.ide.language.SwiftLanguageServiceFactory
import dev.appxcode.ide.language.SwiftCompletion
import dev.appxcode.ide.language.SwiftDiagnostic
import dev.appxcode.ide.language.LspSwiftLanguageService
import dev.appxcode.ide.language.SwiftFormatterService
import dev.appxcode.ide.language.FormatResult
import dev.appxcode.ide.language.BatchFormatResult
import dev.appxcode.ide.build.RunConfiguration
import dev.appxcode.ide.build.RunConfigurationRegistry
import dev.appxcode.ide.build.XcodeBuildService
import dev.appxcode.ide.build.XcodeBuildResult
import dev.appxcode.ide.build.XcodeArchiveService
import dev.appxcode.ide.build.ArchiveRequest
import dev.appxcode.ide.build.ArchiveResult
import dev.appxcode.ide.build.XcodeExportService
import dev.appxcode.ide.build.ExportOptions
import java.time.Duration
import dev.appxcode.ide.project.XcodeTarget
import dev.appxcode.ide.project.XcodeProjectModel
@Service(Service.Level.PROJECT)
class AppXCodeProjectService(private val project: Project) : Disposable {
    private val initialized = AtomicBoolean(false)
    private val swiftSymbols = SwiftSymbolIndex()
    private val objcSymbols = ObjCSymbolIndex()
    private val swiftFormatter = SwiftFormatterService()
    @Volatile private var swiftLanguage: SwiftLanguageService? = null
    private val git = GitService()
    private val dependencyResolver = DependencyResolver()
    private val debugSessions = DebugSessionRegistry()
    private val flutter = FlutterToolService()
    private val runConfigurations = RunConfigurationRegistry()
    private val xcodeBuildService = XcodeBuildService(AppleToolchainDetector.detect())
    private val xcodeArchiveService = XcodeArchiveService(xcodeBuildService)
    private val xcodeExportService = XcodeExportService()
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
        if (swiftLanguage == null) project.basePath?.let { swiftLanguage = SwiftLanguageServiceFactory.create(appleToolchain(), Path.of(it)) }
    }
    fun onProjectFileChange(listener: (Path) -> Unit) { changeListeners += listener }
    fun isInitialized(): Boolean = initialized.get()
    fun projectName(): String = project.name
    fun discoverXcodeContainers(root: Path): List<XcodeContainer> = XcodeProjectModel.discover(root)
    fun appleToolchain(): AppleToolchain = AppleToolchainDetector.detect()
    fun dependencies(root: Path): List<DependencyPin> = DependencyModel.read(root)
    fun resolveSwiftPackages(root: Path, timeoutMillis: Long = 600_000): ResolveResult = dependencyResolver.resolveSwift(root, timeoutMillis)
    fun installCocoaPods(root: Path, timeoutMillis: Long = 600_000): ResolveResult = dependencyResolver.installPods(root, timeoutMillis)
    fun flutterProject(root: Path): FlutterProject? = FlutterProjectDetector.detect(root)
    fun indexSwift(files: Iterable<Path>) { swiftSymbols.index(files) }
    fun findSwiftSymbols(name: String): List<SwiftSymbol> = swiftSymbols.find(name)
    fun completeSwift(prefix: String): List<SwiftSymbol> = swiftSymbols.complete(prefix)
    fun swiftCompletions(file: Path, line: Int, column: Int): List<SwiftCompletion> =
        swiftLanguage?.complete(file, line, column)?.takeIf { it.isNotEmpty() }
            ?: swiftSymbols.complete(runCatching {
                val sourceLine = java.nio.file.Files.readAllLines(file).getOrNull(line - 1).orEmpty()
                sourceLine.take(column.coerceIn(0, sourceLine.length)).takeLastWhile { it.isLetterOrDigit() || it == '_' }
            }.getOrDefault("")).map { SwiftCompletion(it.name, it.kind) }
    fun swiftDiagnostics(files: List<Path>): List<SwiftDiagnostic> = swiftLanguage?.diagnostics(files).orEmpty()
    fun swiftLanguageAlive(): Boolean = (swiftLanguage as? LspSwiftLanguageService)?.isAlive() ?: false
    fun restartSwiftLanguage(): Boolean = (swiftLanguage as? LspSwiftLanguageService)?.restart() ?: false
    fun formatSwift(file: Path): FormatResult = swiftFormatter.format(file)
    fun formatSwiftFiles(files: Iterable<Path>): BatchFormatResult = swiftFormatter.formatFiles(files)
    fun swiftFormatterAvailable(root: Path): Boolean = swiftFormatter.isAvailable(root)
    fun indexObjectiveC(files: Iterable<Path>) { objcSymbols.index(files) }
    fun findObjectiveCSymbols(name: String): List<ObjCSymbol> = objcSymbols.find(name)
    fun completeObjectiveC(prefix: String): List<ObjCSymbol> = objcSymbols.complete(prefix)
    fun completeMixed(prefix: String): List<String> =
        (swiftSymbols.complete(prefix).map { it.name } + objcSymbols.complete(prefix).map { it.name }).distinct().sorted()
    fun xcodeTargets(projectFile: Path): List<XcodeTarget> = XcodeProjectModel.readTargets(projectFile)
    fun testFrameworks(root: Path): Set<TestFramework> = TestFrameworkRegistry.detect(root)
    fun discoverXCTest(root: Path): List<DiscoveredTest> = TestFrameworkRegistry.discoverXCTest(root)
    fun discoverTests(root: Path): List<DiscoveredTest> = TestFrameworkRegistry.discover(root)
    fun snapshot(root: Path): ProjectSnapshot = ProjectSnapshotLoader.load(root)
    fun gitStatus(root: Path): GitStatus = git.status(root)
    fun gitFileDiff(root: Path, file: Path, staged: Boolean = false): String = git.fileDiff(root, file, staged)
    fun isGitRepository(root: Path): Boolean = git.isRepository(root)
    fun gitCurrentRevision(root: Path): String? = git.currentRevision(root)
    fun gitBranches(root: Path): List<GitBranch> = git.branches(root)
    fun gitLog(root: Path, limit: Int = 50): List<GitCommit> = git.log(root, limit)
    fun gitStashes(root: Path): List<GitStash> = git.stashes(root)
    fun gitStashDiff(root: Path, index: Int): String = git.stashDiff(root, index)
    fun gitRemotes(root: Path): List<GitRemote> = git.remotes(root)
    fun gitTags(root: Path): List<GitTag> = git.tags(root)
    fun gitSyncStatus(root: Path): GitSyncStatus? = git.syncStatus(root)
    fun createDebugSession(sessionId: String) = debugSessions.create(sessionId)
    fun updateDebugSession(sessionId: String, state: DebugSessionState) = debugSessions.update(sessionId, state)
    fun debugSessionState(sessionId: String): DebugSessionState? = debugSessions.state(sessionId)
    fun flutterService(): FlutterToolService = flutter
    fun putRunConfiguration(configuration: RunConfiguration) = runConfigurations.put(configuration)
    fun removeRunConfiguration(name: String) = runConfigurations.remove(name)
    fun runConfiguration(name: String): RunConfiguration? = runConfigurations.get(name)
    fun runConfigurations(): List<RunConfiguration> = runConfigurations.all()
    fun hasRunConfiguration(name: String): Boolean = runConfigurations.contains(name)
    fun clearRunConfigurations() = runConfigurations.clear()
    fun xcodeBuild(configuration: RunConfiguration, container: Path, timeout: Duration = Duration.ofMinutes(15)): XcodeBuildResult = xcodeBuildService.execute(configuration, container, timeout)
    fun xcodeRun(configuration: RunConfiguration, container: Path, timeout: Duration = Duration.ofMinutes(15)): XcodeBuildResult = xcodeBuildService.run(configuration, container, timeout)
    fun xcodeTest(configuration: RunConfiguration, container: Path, timeout: Duration = Duration.ofMinutes(15)): XcodeBuildResult = xcodeBuildService.test(configuration, container, timeout)
    fun xcodeClean(configuration: RunConfiguration, container: Path, timeout: Duration = Duration.ofMinutes(15)): XcodeBuildResult = xcodeBuildService.clean(configuration, container, timeout)
    fun xcodeArchive(request: ArchiveRequest, timeout: Duration = Duration.ofMinutes(30)): ArchiveResult = xcodeArchiveService.archive(request, timeout)
    fun xcodeExport(options: ExportOptions): XcodeBuildResult = xcodeExportService.export(options)
    fun flutterPubGet(root: Path): FlutterCommandResult = flutter.pubGet(root)
    fun flutterDoctor(root: Path): FlutterCommandResult = flutter.doctor(root)
    fun flutterRun(root: Path, deviceId: String? = null): FlutterCommandResult = flutter.run(root, deviceId)
    fun flutterTest(root: Path): FlutterCommandResult = flutter.test(root)
    fun flutterHotReload(root: Path): FlutterCommandResult = flutter.hotReload(root)
    fun flutterHotRestart(root: Path): FlutterCommandResult = flutter.hotRestart(root)
    fun flutterSessionAlive(): Boolean = flutter.sessionAlive()
    fun flutterSessionExitCode(): Int? = flutter.sessionExitCode()
    fun clearFlutterSessionOutput() = flutter.clearSessionOutput()
    override fun dispose() {
        projectWatcher?.close()
        projectWatcher = null
        (swiftLanguage as? AutoCloseable)?.close()
        swiftLanguage = null
        flutter.stopSession()
        runConfigurations.clear()
        changeListeners.clear()
    }
}
