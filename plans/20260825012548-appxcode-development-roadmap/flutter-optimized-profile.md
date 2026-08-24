# Flutter Optimized Compatibility Profile

## Objective

Make Flutter development a certified, first-class workflow in AppXCode while keeping
the product's primary strength: deep Apple-platform and Xcode integration.

AppXCode should use the official Dart and Flutter IntelliJ plugins as the language
and Flutter workflow foundation whenever possible. AppXCode-owned code should focus
on product compatibility, native Apple project integration, build/device bridging,
and UX gaps that cannot be solved upstream. A private long-lived fork is a last
resort requiring an explicit maintenance decision.

## Scope and platform policy

### Primary macOS profile

The primary release targets:

- Dart language editing and analysis.
- Flutter applications, packages, plugins, and module/add-to-app projects.
- Flutter SDK stable release bands and common version managers such as FVM.
- Pub dependencies and project metadata.
- iOS simulator and physical-device development.
- macOS desktop development.
- Direct access to Flutter-generated/native Xcode workspaces, Swift/Objective-C
  sources, CocoaPods/SPM dependencies, signing, entitlements, capabilities, assets,
  and Apple build diagnostics.
- Flutter Run, Debug, and Profile modes; hot reload and hot restart.
- Flutter DevTools, Widget Inspector, performance views, logs, and debugging.
- Unit, widget, golden, and integration tests.

### Expanded Flutter profile

After the Apple-focused profile is stable, add certified tooling for:

- Android targets and native Kotlin/Java/Gradle project integration.
- Web targets and supported browsers.
- Windows and Linux desktop targets on suitable hosts.
- Cross-platform monorepos and shared run/test configurations.

The expanded profile may require Android Studio-derived modules, additional language
plugins, browser tooling, Gradle/JDK/Android SDKs, and host-native build dependencies.
It must not block the primary macOS Apple-development release.

### Unsupported implications

Flutter's official deployment matrix includes Android, iOS, web, Windows, macOS,
and Linux. It does not list watchOS or tvOS as supported deployment targets.
AppXCode's native watchOS/tvOS support therefore does not imply Flutter support for
those platforms.

## Certified dependency matrix

Every AppXCode release that advertises optimized Flutter support pins and verifies:

- AppXCode version and underlying IntelliJ Platform build.
- JetBrains Runtime/JDK.
- Official Flutter IntelliJ plugin ID and version.
- Official Dart IntelliJ plugin ID and version.
- Every required bundled IntelliJ/Android/YAML/Java/Kotlin/Gradle/LSP or other module
  discovered from the actual plugin descriptors and runtime.
- Flutter SDK release channel and supported version band.
- Bundled Dart SDK relationship.
- Xcode, Swift, CocoaPods, and supported Apple SDK/device OS bands.
- Android SDK, JDK, Gradle, browser, and desktop toolchains when the expanded profile
  is enabled.

Declared build compatibility alone is not certification. Plugin Verifier, official
plugin tests where reusable, AppXCode integration tests, runtime smoke tests, and
end-to-end Flutter workflows must all pass for the exact matrix entry.

## Distribution and upstream policy

- Confirm redistribution and bundling permissions for each Dart/Flutter/plugin
  artifact before including it in the AppXCode installer.
- Prefer installing verified official releases from an approved repository when
  bundling is not appropriate.
- Contribute platform compatibility fixes to `flutter/flutter-intellij` or the Dart
  plugin upstream when feasible.
- Keep AppXCode adapters small, documented, and outside official plugin namespaces.
- Do not patch plugin binaries after publication or fake missing IntelliJ module IDs.
- If a temporary fork becomes unavoidable, publish its source, provenance, patch
  set, security/update process, and exit plan as required by applicable licenses.

## Project model

Recognize and model:

- Flutter app roots and nested Dart packages.
- `pubspec.yaml`, lockfiles, workspace/monorepo metadata, analysis options, generated
  plugin registrants, tool metadata, and build outputs.
- Flutter packages and plugins with native iOS/macOS/Android implementations.
- Flutter module/add-to-app projects embedded in existing native applications.
- Generated `ios` and `macos` Xcode projects/workspaces and their CocoaPods or SPM
  dependency graphs.
- Multiple Flutter SDKs across projects without global state leakage.

The project view should present one coherent Flutter project while allowing direct
navigation into native platform code. Generated files are labeled, refreshable, and
protected from accidental edits where regeneration would discard changes.

## SDK and diagnostics

- Discover Flutter SDKs from configured paths and supported version managers.
- Show exact Flutter/Dart versions, channel, cache state, doctor status, and matrix
  compatibility.
- Run environment diagnostics through structured tasks with actionable fixes rather
  than an opaque terminal dump.
- Never change SDK channel, upgrade an SDK, accept licenses, install CocoaPods, or
  mutate global tool configuration without explicit user action.
- Keep per-project SDK choice stable across restart, branch change, and remote
  sessions.

## Editing and code intelligence

Through the certified Dart/Flutter plugins, verify:

- Syntax highlighting, completion, documentation, navigation, usages, rename,
  formatting, analysis issues, assists, intentions, and quick fixes.
- Widget-aware editing, snippets/templates, outline/structure, and source generation
  interactions.
- Correct invalidation after `pub get`, generated-code changes, SDK switch, flavor,
  conditional import, and native plugin refresh.
- Navigation among Dart calls, platform channels, generated registrants, Swift/
  Objective-C implementations, and Xcode build diagnostics where symbol information
  permits it.

AppXCode must not create a competing Dart analysis implementation while the official
plugin remains viable.

## Pub and dependency workflows

- Support `pub get`, upgrade, outdated, dependency diagnostics, package search links,
  and lockfile-aware refresh through structured IDE tasks.
- Preserve normal Git behavior and make generated/cache directories explicit.
- Detect dependency conflicts, SDK constraints, native pod/package changes, and
  stale generated registrants.
- Support common monorepo layouts without assuming every package is an application.
- Treat build-runner and other generators as project tasks with progress,
  cancellation, logs, and refresh, not automatic hidden mutations.

## Run, debug, and profile

- Discover compatible Flutter targets and present simulator, physical device,
  desktop, browser, and remote targets with capability-aware filtering.
- Support Flutter flavors, Dart entrypoints, build modes, environment definitions,
  additional tool arguments, and per-project run configurations.
- Preserve official hot reload and hot restart semantics, state, availability, and
  failure explanations.
- Integrate Dart breakpoints, frames, variables, watches, exceptions, console, and
  source mapping into the IntelliJ debugger experience.
- Launch DevTools and Widget Inspector with authenticated session discovery and clear
  lifecycle ownership.
- Surface frame, rebuild, memory, CPU, network, logging, and application-size tools
  where supported by the official toolchain.

## Apple-native integration

- Resolve the Flutter iOS/macOS project to the correct Xcode workspace and scheme,
  including CocoaPods-generated workspaces.
- Reuse AppXCode's Xcode project, build setting, signing, simulator/device, archive,
  result, and diagnostic services rather than implementing parallel partial copies.
- Allow users to edit Swift, Objective-C, C/C++, entitlements, property lists, asset
  catalogs, capabilities, native tests, and plugin implementations in the same IDE.
- Synchronize Flutter flavors and build modes with Xcode configurations and schemes
  through explicit, testable mapping.
- Map Flutter tool and Xcode diagnostics back to the originating Dart or native file
  when evidence exists; otherwise preserve the real native diagnostic source.
- Support physical-device Developer Mode, trust, signing, provisioning, install,
  launch, logs, and debug through the normal AppXCode destination model.
- Support archive/export/TestFlight/App Store preparation through Flutter and Xcode
  flows without hiding the resulting native artifacts or signing decisions.

## Testing

Integrate into the unified test UI:

- Dart unit tests.
- Flutter widget tests.
- Golden tests, including artifact/diff presentation.
- `integration_test` suites on simulators and physical devices.
- Native XCTest suites located under Flutter iOS/macOS projects.

Preserve suite/test names, hierarchy, status, duration, stdout/stderr, failure
locations, stack traces, retries where supported, attachments, screenshots, and
coverage. Rerun failed, run-at-caret, debug test, filter, cancellation, and history
must behave consistently with other AppXCode test frameworks.

## Remote and future hosts

- On macOS, local Flutter and Xcode workflows are the reference implementation.
- From Linux/Windows clients, Dart analysis and host-native Flutter targets may run
  locally when the profile is certified.
- iOS/macOS build, signing, archive, native analysis, simulator, and Apple debugging
  remain on the macOS build node.
- A physical iOS device connected to Linux/Windows follows the AppXCode device-gateway
  compatibility policy after the native gateway phase is certified.
- Remote mode preserves source mapping, hot reload, VM service/DevTools connectivity,
  logs, test events, and artifact identity across client, Mac agent, and device.

## Fixtures

Maintain pinned fixtures for:

- Minimal Flutter app targeting iOS and macOS.
- App using multiple native plugins through CocoaPods and Swift Package Manager.
- Custom Flutter plugin with Dart, Swift, and Objective-C implementations.
- Flutter module embedded in an existing native iOS app.
- Multi-package/monorepo workspace with shared packages.
- Flavors/configurations and multiple Dart entrypoints.
- Unit, widget, golden, integration, and native XCTest suites.
- Real-device signing and deployment.
- Expanded Android, web, Windows, and Linux targets when those profiles begin.

Fixtures pin source, SDK, plugin dependencies, toolchains, expected diagnostics,
test results, and generated-project diffs.

## Validation gates

- The certified Dart and Flutter plugins install and load with no missing modules or
  critical Plugin Verifier failures against the exact AppXCode build.
- Create/open/import recognizes every supported fixture without conversion.
- Dart analysis, completion, navigation, refactoring, formatting, and quick fixes
  pass representative official-style workflows.
- SDK selection, doctor, pub, refresh, generation, and branch changes do not create
  stale indexes or corrupt generated/native projects.
- Run/Debug/Profile, hot reload/restart, DevTools, Widget Inspector, and performance
  tools work on every declared target/mode pair.
- All declared test types discover, execute, report, debug, rerun, and retain
  artifacts correctly.
- Flutter iOS/macOS builds, native code navigation, CocoaPods/SPM refresh, signing,
  simulator/device deployment, archive, and native diagnostics pass end to end.
- AppXCode publishes the exact certified matrix and clearly rejects unverified or
  incompatible plugin/SDK combinations.

## Risks and mitigations

### Flutter/Dart plugin tracks IntelliJ or Android Studio differently

Pin exact combinations, test pre-release candidates early, maintain a dependency
gap report, and contribute custom-product compatibility upstream.

### Android dependencies expand the product too far

Keep the Apple-focused Flutter profile first. Add Android modules only through the
expanded profile after footprint, license, security, update, and maintenance review.

### Duplicate build/project models drift

Make Flutter integration call shared AppXCode Xcode/build/device services and the
official Flutter tool rather than maintaining parallel representations.

### Generated native projects are overwritten

Label generated ownership, detect regeneration, preserve user-owned native files,
preview destructive changes, and test clean regeneration with Git diffs.

### SDK/plugin updates silently break certified workflows

Disable automatic promotion into the certified channel. Run the full matrix first,
publish results, then deliberately advance supported versions.

## Official references

- Flutter support for Android Studio and IntelliJ:
  https://docs.flutter.dev/tools/android-studio
- Flutter supported deployment platforms:
  https://docs.flutter.dev/reference/supported-platforms
- Flutter iOS development setup:
  https://docs.flutter.dev/platform-integration/ios/setup
- Flutter iOS build and release:
  https://docs.flutter.dev/deployment/ios
- Official Flutter IntelliJ plugin source:
  https://github.com/flutter/flutter-intellij

These references are living inputs. Each AppXCode release records the exact Flutter,
Dart, plugin, IntelliJ Platform, Xcode, dependency, and device versions used for its
certification.
