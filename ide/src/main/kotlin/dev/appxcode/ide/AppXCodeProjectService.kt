package dev.appxcode.ide
import com.intellij.openapi.components.Service
import com.intellij.openapi.project.Project
import com.intellij.openapi.Disposable
import java.util.concurrent.atomic.AtomicBoolean
import dev.appxcode.ide.project.XcodeContainer
import dev.appxcode.ide.project.XcodeScheme
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
import dev.appxcode.ide.git.GitBlameLine
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
import dev.appxcode.ide.build.SigningService
import dev.appxcode.ide.build.SigningConfiguration
import dev.appxcode.ide.build.SigningCheck
import dev.appxcode.ide.test.XcodeTestService
import dev.appxcode.ide.test.XcodeTestResult
import dev.appxcode.ide.test.TestFrameworkCommand
import java.time.Duration
import dev.appxcode.ide.project.XcodeTarget
import dev.appxcode.ide.device.DeviceRegistry
import dev.appxcode.ide.device.AppleDevice
import dev.appxcode.ide.device.DeviceRegistrySnapshot
import dev.appxcode.ide.device.DeviceProvider
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
    private val devices: DeviceRegistry by lazy {
        project.getService(dev.appxcode.ide.device.DeviceRegistryService::class.java).sharedRegistry()
    }
    private val xcodeBuildService = XcodeBuildService(AppleToolchainDetector.detect())
    private val xcodeArchiveService = XcodeArchiveService(xcodeBuildService)
    private val xcodeExportService = XcodeExportService()
    private val signingService = SigningService()
    private val xcodeTestService = XcodeTestService(xcodeBuildService)
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
    fun xcodeSchemes(container: XcodeContainer): List<XcodeScheme> = XcodeProjectModel.readSchemes(container)
    fun appleToolchain(): AppleToolchain = AppleToolchainDetector.detect()
    fun deviceRegistry(): DeviceRegistry = devices
    fun discoverDevices(): List<AppleDevice> = devices.discover()
    fun deviceProviderErrors(): Map<String, String> = devices.providerErrors()
    fun deviceSnapshot(): DeviceRegistrySnapshot = devices.snapshot()
    fun findDevice(deviceId: String): AppleDevice? = devices.find(deviceId)
    fun preferredDevice(): AppleDevice? = devices.preferred()
    fun selectDevice(deviceId: String? = null): AppleDevice? = devices.select(deviceId)
    fun registerDeviceProvider(provider: DeviceProvider) = devices.register(provider)
    fun unregisterDeviceProvider(providerId: String) = devices.unregister(providerId)
    fun dependencies(root: Path): List<DependencyPin> = DependencyModel.read(root)
    fun resolveSwiftPackages(root: Path, timeoutMillis: Long = 600_000): ResolveResult =
        dependencyResolver.resolveSwift(root, timeoutMillis = timeoutMillis)
    fun installCocoaPods(root: Path, timeoutMillis: Long = 600_000): ResolveResult =
        dependencyResolver.installPods(root, timeoutMillis = timeoutMillis)
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
    fun testCommand(framework: TestFramework, filter: String? = null): TestFrameworkCommand = TestFrameworkRegistry.command(framework, filter)
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
    fun gitBlame(root: Path, file: Path): List<GitBlameLine> = git.blame(root, file)
    fun gitHooks(root: Path): List<String> = git.hooks(root)
    fun gitCommit(root: Path, message: String): String? = git.commit(root, message)
    fun gitMerge(root: Path, branch: String): Boolean = git.merge(root, branch)
    fun gitFetch(root: Path, remote: String = "origin"): Boolean = git.fetch(root, remote)
    fun gitPull(root: Path, remote: String = "origin", branch: String? = null): Boolean = git.pull(root, remote, branch)
    fun gitPush(root: Path, remote: String = "origin", branch: String? = null): Boolean = git.push(root, remote, branch)
    fun gitStage(root: Path, files: Collection<Path>): Boolean = git.stage(root, files)
    fun gitUnstage(root: Path, files: Collection<Path>): Boolean = git.unstage(root, files)
    fun createDebugSession(sessionId: String) = debugSessions.create(sessionId)
    fun updateDebugSession(sessionId: String, state: DebugSessionState) = debugSessions.update(sessionId, state)
    fun debugSessionState(sessionId: String): DebugSessionState? = debugSessions.state(sessionId)
    fun debugSessions(): Map<String, DebugSessionState> = debugSessions.all()
    fun flutterService(): FlutterToolService = flutter
    fun putRunConfiguration(configuration: RunConfiguration) = runConfigurations.put(configuration)
    fun removeRunConfiguration(name: String) = runConfigurations.remove(name)
    fun runConfiguration(name: String): RunConfiguration? = runConfigurations.get(name)
    fun runConfigurations(): List<RunConfiguration> = runConfigurations.all()
    fun hasRunConfiguration(name: String): Boolean = runConfigurations.contains(name)
    fun clearRunConfigurations() = runConfigurations.clear()
    fun xcodeBuild(configuration: RunConfiguration, container: Path, timeout: Duration = Duration.ofMinutes(15)): XcodeBuildResult = xcodeBuildService.execute(configuration, container, timeout)
    fun xcodeBuildOnDevice(configuration: RunConfiguration, container: Path, device: AppleDevice, timeout: Duration = Duration.ofMinutes(15)): XcodeBuildResult {
        require(device.state == dev.appxcode.ide.device.DeviceState.AVAILABLE) { "Device is not available: ${device.id}" }
        val platform = when (device.platform.lowercase()) {
            "ios" -> "iOS"
            "ipados", "ipad os" -> "iPadOS"
            "watchos", "watch os" -> "watchOS"
            "tvos", "tv os" -> "tvOS"
            "macos", "mac os" -> "macOS"
            else -> device.platform
        }
        val kind = if (device.kind == dev.appxcode.ide.device.DeviceKind.PHYSICAL) dev.appxcode.ide.build.DestinationKind.DEVICE else dev.appxcode.ide.build.DestinationKind.SIMULATOR
        val destination = dev.appxcode.ide.build.AppleDestination(configuration.destination.platform, kind, device.name, device.id)
            .copy(platform = when (platform) {
                "iOS" -> dev.appxcode.ide.build.ApplePlatform.IOS
                "iPadOS" -> dev.appxcode.ide.build.ApplePlatform.IPADOS
                "watchOS" -> dev.appxcode.ide.build.ApplePlatform.WATCHOS
                "tvOS" -> dev.appxcode.ide.build.ApplePlatform.TVOS
                "macOS" -> dev.appxcode.ide.build.ApplePlatform.MACOS
                else -> configuration.destination.platform
            })
        return xcodeBuildService.execute(configuration.copy(destination = destination), container, timeout)
    }
    fun xcodeBuildOnSelectedDevice(configuration: RunConfiguration, container: Path, deviceId: String? = null, timeout: Duration = Duration.ofMinutes(15)): XcodeBuildResult? {
        val device = devices.select(deviceId) ?: return null
        return xcodeBuildOnDevice(configuration, container, device, timeout)
    }
    fun xcodeRun(configuration: RunConfiguration, container: Path, timeout: Duration = Duration.ofMinutes(15)): XcodeBuildResult = xcodeBuildService.run(configuration, container, timeout)
    fun xcodeRunOnDevice(configuration: RunConfiguration, container: Path, device: AppleDevice, timeout: Duration = Duration.ofMinutes(15)): XcodeBuildResult {
        require(device.state == dev.appxcode.ide.device.DeviceState.AVAILABLE) { "Device is not available: ${device.id}" }
        val destination = configuration.destination.copy(
            platform = when (device.platform.lowercase()) {
                "ios" -> dev.appxcode.ide.build.ApplePlatform.IOS
                "ipados", "ipad os" -> dev.appxcode.ide.build.ApplePlatform.IPADOS
                "watchos", "watch os" -> dev.appxcode.ide.build.ApplePlatform.WATCHOS
                "tvos", "tv os" -> dev.appxcode.ide.build.ApplePlatform.TVOS
                "macos", "mac os" -> dev.appxcode.ide.build.ApplePlatform.MACOS
                else -> configuration.destination.platform
            },
            kind = if (device.kind == dev.appxcode.ide.device.DeviceKind.PHYSICAL) dev.appxcode.ide.build.DestinationKind.DEVICE else dev.appxcode.ide.build.DestinationKind.SIMULATOR,
            name = device.name,
            identifier = device.id
        )
        return xcodeBuildService.run(configuration.copy(destination = destination), container, timeout)
    }
    fun xcodeRunOnSelectedDevice(configuration: RunConfiguration, container: Path, deviceId: String? = null, timeout: Duration = Duration.ofMinutes(15)): XcodeBuildResult? {
        val device = devices.select(deviceId) ?: return null
        return xcodeRunOnDevice(configuration, container, device, timeout)
    }
    fun xcodeTest(configuration: RunConfiguration, container: Path, timeout: Duration = Duration.ofMinutes(15)): XcodeBuildResult = xcodeBuildService.test(configuration, container, timeout)
    fun xcodeClean(configuration: RunConfiguration, container: Path, timeout: Duration = Duration.ofMinutes(15)): XcodeBuildResult = xcodeBuildService.clean(configuration, container, timeout)
    fun xcodeCleanOnDevice(configuration: RunConfiguration, container: Path, device: AppleDevice, timeout: Duration = Duration.ofMinutes(15)): XcodeBuildResult {
        require(device.state == dev.appxcode.ide.device.DeviceState.AVAILABLE) { "Device is not available: ${device.id}" }
        val platform = when (device.platform.lowercase()) {
            "ios" -> dev.appxcode.ide.build.ApplePlatform.IOS
            "ipados", "ipad os" -> dev.appxcode.ide.build.ApplePlatform.IPADOS
            "watchos", "watch os" -> dev.appxcode.ide.build.ApplePlatform.WATCHOS
            "tvos", "tv os" -> dev.appxcode.ide.build.ApplePlatform.TVOS
            "macos", "mac os" -> dev.appxcode.ide.build.ApplePlatform.MACOS
            else -> configuration.destination.platform
        }
        val kind = if (device.kind == dev.appxcode.ide.device.DeviceKind.PHYSICAL) dev.appxcode.ide.build.DestinationKind.DEVICE else dev.appxcode.ide.build.DestinationKind.SIMULATOR
        val destination = dev.appxcode.ide.build.AppleDestination(platform, kind, device.name, device.id)
        return xcodeBuildService.clean(configuration.copy(destination = destination), container, timeout)
    }
    fun xcodeArchive(request: ArchiveRequest, timeout: Duration = Duration.ofMinutes(30)): ArchiveResult = xcodeArchiveService.archive(request, timeout)
    fun xcodeArchiveOnDevice(request: ArchiveRequest, device: AppleDevice, timeout: Duration = Duration.ofMinutes(30)): ArchiveResult {
        require(device.state == dev.appxcode.ide.device.DeviceState.AVAILABLE) { "Device is not available: ${device.id}" }
        val platform = when (device.platform.lowercase()) {
            "ios" -> "iOS"
            "ipados", "ipad os" -> "iPadOS"
            "watchos", "watch os" -> "watchOS"
            "tvos", "tv os" -> "tvOS"
            "macos", "mac os" -> "macOS"
            else -> device.platform
        }
        val kind = if (device.kind == dev.appxcode.ide.device.DeviceKind.PHYSICAL) platform else "$platform Simulator"
        return xcodeArchiveService.archive(request.copy(destination = "platform=$kind,name=${device.name},id=${device.id}"), timeout)
    }
    fun xcodeArchiveOnSelectedDevice(request: ArchiveRequest, deviceId: String? = null, timeout: Duration = Duration.ofMinutes(30)): ArchiveResult? {
        val device = devices.select(deviceId) ?: return null
        return xcodeArchiveOnDevice(request, device, timeout)
    }
    fun xcodeExport(options: ExportOptions): XcodeBuildResult = xcodeExportService.export(options)
    fun checkSigning(configuration: SigningConfiguration): SigningCheck = signingService.check(configuration)
    fun xcodeTest(container: Path, scheme: String, destination: String, configuration: String = "Debug", timeout: Duration = Duration.ofMinutes(20)): XcodeTestResult =
        xcodeTestService.run(container, scheme, destination, configuration, timeout)
    fun xcodeTestOnDevice(container: Path, scheme: String, device: AppleDevice, configuration: String = "Debug", timeout: Duration = Duration.ofMinutes(20)): XcodeTestResult {
        require(device.state == dev.appxcode.ide.device.DeviceState.AVAILABLE) { "Device is not available: ${device.id}" }
        val platform = when (device.platform.lowercase()) {
            "ios" -> "iOS"
            "ipados", "ipad os" -> "iPadOS"
            "watchos", "watch os" -> "watchOS"
            "tvos", "tv os" -> "tvOS"
            "macos", "mac os" -> "macOS"
            else -> device.platform
        }
        val kind = if (device.kind == dev.appxcode.ide.device.DeviceKind.PHYSICAL) platform else "$platform Simulator"
        return xcodeTestService.run(container, scheme, "platform=$kind,name=${device.name},id=${device.id}", configuration, timeout)
    }
    fun xcodeTestOnSelectedDevice(container: Path, scheme: String, deviceId: String? = null, configuration: String = "Debug", timeout: Duration = Duration.ofMinutes(20)): XcodeTestResult {
        val device = devices.select(deviceId)
            ?: return XcodeTestResult(emptyList(), "No available device matched selection")
        return xcodeTestOnDevice(container, scheme, device, configuration, timeout)
    }
    fun xcodeRerunFailed(container: Path, scheme: String, destination: String, previous: XcodeTestResult, configuration: String = "Debug", timeout: Duration = Duration.ofMinutes(20)): XcodeTestResult =
        xcodeTestService.rerunFailed(container, scheme, destination, previous, configuration, timeout)
    fun xcodeRerunFailedOnDevice(container: Path, scheme: String, device: AppleDevice, previous: XcodeTestResult, configuration: String = "Debug", timeout: Duration = Duration.ofMinutes(20)): XcodeTestResult {
        require(device.state == dev.appxcode.ide.device.DeviceState.AVAILABLE) { "Device is not available: ${device.id}" }
        val platform = when (device.platform.lowercase()) {
            "ios" -> "iOS"
            "ipados", "ipad os" -> "iPadOS"
            "watchos", "watch os" -> "watchOS"
            "tvos", "tv os" -> "tvOS"
            "macos", "mac os" -> "macOS"
            else -> device.platform
        }
        val kind = if (device.kind == dev.appxcode.ide.device.DeviceKind.PHYSICAL) platform else "$platform Simulator"
        return xcodeTestService.rerunFailed(container, scheme, "platform=$kind,name=${device.name},id=${device.id}", previous, configuration, timeout)
    }
    fun flutterPubGet(root: Path): FlutterCommandResult = flutter.pubGet(root)
    fun flutterDoctor(root: Path): FlutterCommandResult = flutter.doctor(root)
    fun flutterRun(root: Path, deviceId: String? = null): FlutterCommandResult {
        val selected = devices.select(deviceId)
        if (deviceId != null && selected == null) {
            return FlutterCommandResult(false, "Flutter device is not available: $deviceId", null)
        }
        return flutter.run(root, selected?.id)
    }
    fun startFlutterSession(root: Path, deviceId: String? = null): Boolean {
        val selected = devices.select(deviceId)
        if (deviceId != null && selected == null) return false
        return flutter.startSession(root, selected?.id)
    }
    fun stopFlutterSession() = flutter.stopSession()
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
        debugSessions.clear()
        devices.clear()
        runConfigurations.clear()
        changeListeners.clear()
    }
}
