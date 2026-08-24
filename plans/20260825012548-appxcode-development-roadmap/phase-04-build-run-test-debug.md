# Phase 4 — Build, Run, Test, and Debug

## Objective

Provide an end-to-end macOS-hosted development workflow for all declared Apple
target families before distributing that workflow across multiple computers.

## Workstreams

- Implement build-agent discovery for Xcode installations, toolchains, SDKs,
  simulators, devices, schemes, configurations, and destinations.
- Stream structured build progress and map compiler/linker diagnostics to project
  files and editor locations.
- Model signing teams, identities, profiles, entitlements, capabilities, and safe
  automatic/manual signing behavior without exposing private keys to the client.
- Build run configurations for applications, extensions, command-line tools, tests,
  and multi-target relationships.
- Add simulator/device install, launch, stop, logs, screenshots, crash reports, and
  process attachment through backend capabilities.
- Integrate LLDB with IntelliJ debugger abstractions for source breakpoints, stacks,
  threads, variables, watches, expressions, stepping, and source mapping.
- Build one unified test model and adapters for XCTest, Quick, Kiwi, and
  Catch/Catch2, including discovery, gutter actions, filtering, rerun-failed,
  streamed results, attachments, and result-bundle retention.
- Bring platforms up in the planned order: iOS/iPadOS, macOS, tvOS, watchOS.

## Validation

- Execute the roadmap's local end-to-end release gate for every supported target.
- Validate physical-device signing, installation, launch, tests, and debugging on
  supported OS bands.
- Compare test counts, names, statuses, failures, attachments, and durations with
  Xcode results.
- Exercise app extensions, watch companion relationships, disconnected devices,
  locked devices, expired profiles, simulator resets, and cancelled builds.

## Exit gate

Each declared Apple platform has an evidence-backed macOS workflow for build, run,
test, and debug, with unsupported capability combinations reported explicitly.
