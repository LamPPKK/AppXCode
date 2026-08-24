# Phase 4 — Build, Run, Test, and Debug

## Objective

Provide an end-to-end macOS-hosted development workflow for all declared Apple
target families before distributing that workflow across multiple computers.

## Workstreams

- Implement build-agent discovery for Xcode installations, toolchains, SDKs,
  simulators, devices, schemes, configurations, and destinations.
- Stream structured build progress and map compiler/linker diagnostics to project
  files and editor locations.
- Deliver modern IntelliJ Build, Run, Test, and Debug tool-window workflows with
  stable tabs, compact task trees, source navigation, console filters, failure focus,
  progressive disclosure, and keyboard-driven transitions back to the editor.
- Model signing teams, identities, profiles, entitlements, capabilities, and safe
  automatic/manual signing behavior without exposing private keys to the client.
- Build run configurations for applications, extensions, command-line tools, tests,
  and multi-target relationships.
- Add simulator/device install, launch, stop, logs, screenshots, crash reports, and
  process attachment through backend capabilities.
- Ship the local Apple Simulator path in the Embedded Devices tool window and bind
  its destination/session identity to Run, Test, Debug, logs, screenshots, recording,
  input, rotation, and lifecycle state. Preserve a supported detached fallback.
- Add local physical-device screen viewing only where a verified Apple or approved
  gateway capability exists; do not infer interaction/debug support from display.
- Offer the `vphone-cli` provider only as an optional experimental feature after its
  Phase 0 security gates pass. Manage existing VMs first; keep firmware preparation,
  security-policy changes, and destructive VM management outside normal Run/Debug.
- Integrate LLDB with IntelliJ debugger abstractions for source breakpoints, stacks,
  threads, variables, watches, expressions, stepping, and source mapping.
- Build one unified test model and adapters for XCTest, Quick, Kiwi, and
  Catch/Catch2, including discovery, gutter actions, filtering, rerun-failed,
  streamed results, attachments, and result-bundle retention.
- Bring platforms up in the planned order: iOS/iPadOS, macOS, tvOS, watchOS.
- Certify Flutter Run/Debug/Profile modes on iOS simulator/device and macOS, including
  target selection, hot reload, hot restart, logs, Dart breakpoints, Widget
  Inspector, DevTools, performance tooling, and transitions into native Swift/
  Objective-C debugging where the supported plugin/toolchain stack permits it.
- Integrate `flutter test`, widget/golden tests, and `integration_test` results into
  the unified test UI while preserving official Flutter semantics and artifacts.

## Validation

- Execute the roadmap's local end-to-end release gate for every supported target.
- Validate physical-device signing, installation, launch, tests, and debugging on
  supported OS bands.
- Compare test counts, names, statuses, failures, attachments, and durations with
  Xcode results.
- Run canonical Apple development scenarios and compare focus changes, action
  availability, information hierarchy, and keyboard operation with the approved
  current-platform UX baseline.
- Exercise app extensions, watch companion relationships, disconnected devices,
  locked devices, expired profiles, simulator resets, and cancelled builds.
- Run pinned Flutter fixtures through edit, analyze, pub, build, hot reload/restart,
  test, profile, debug, signing, archive, and real-device scenarios.
- Exercise every embedded provider through first frame, input transforms, rotation,
  resize, multi-device tabs, detach/redock, IDE restart, provider crash, reconnect,
  unsupported actions, and teardown; measure the agreed latency/resource budgets.

## Exit gate

Each declared Apple platform has an evidence-backed macOS workflow for build, run,
test, and debug, with unsupported capability combinations reported explicitly.
