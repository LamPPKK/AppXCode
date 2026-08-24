# Phase 3 — Project and Dependency Systems

## Objective

Open and synchronize real Xcode projects, workspaces, Swift packages, and CocoaPods
workspaces without conversion or destructive rewriting.

## Workstreams

- Model projects, workspaces, targets, schemes, configurations, file references,
  build phases, dependencies, SDKs, destinations, and generated sources.
- Build a tolerant `project.pbxproj` reader that retains unknown constructs and
  stable identities.
- Treat `xcodebuild` output as the authoritative build-context source and reconcile
  it with the editor project model.
- Support SwiftPM manifests, resolved versions, local packages, package plugins,
  binary targets, and packages embedded in Xcode projects.
- Detect CocoaPods workspaces, lockfiles, generated projects, source roots, build
  settings, and safe user-initiated installation/update workflows.
- Recognize Flutter applications, packages, plugins, modules/add-to-app projects,
  monorepos, `pubspec.yaml`, lockfiles, generated metadata, and their iOS/macOS
  Xcode workspaces without treating generated native files as unrelated projects.
- Integrate Flutter SDK/FVM selection, `pub get`, dependency refresh, code generation
  status, and native CocoaPods/SPM dependencies with one coherent project model.
- Integrate IntelliJ Git facilities with generated-file policies, change lists,
  history, branches, conflict handling, and project refresh.
- Add file watching and incremental model refresh without discarding unsaved editor
  state.
- Defer project-file writes until a lossless, reviewable writer passes golden tests.

## Validation

- Open every compatibility fixture and match targets, schemes, source roots,
  dependencies, and build settings against Xcode.
- Prove that read-only open/close creates no project-file diff.
- For approved write operations, prove round-trip preservation of unknown fields and
  minimal deterministic diffs.
- Switch branches and dependency versions without stale indexes or generated files.
- Open Flutter fixtures, navigate Dart-to-native plugin code and back, refresh pub
  dependencies, and preserve generated/native workspace consistency.

## Exit gate

Representative Xcode, SwiftPM, CocoaPods, and mixed projects can be opened, indexed,
refreshed, and built without conversion, and Xcode continues to open the unchanged
workspace.
