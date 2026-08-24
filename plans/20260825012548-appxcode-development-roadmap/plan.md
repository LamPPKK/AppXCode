# AppXCode Development Roadmap

Status: Proposed

Created: 2026-08-25

Scope: Product architecture and phased delivery plan

## 1. Product goal

AppXCode will be an IntelliJ Platform-based IDE for developing applications for
iOS, iPadOS, macOS, watchOS, and tvOS. The native macOS application is the primary
product, the first production release, and the reference experience against which
all later editions are measured. Its target capabilities are:

- JetBrains-style code analysis, navigation, completion, inspections, and safe
  refactoring for Swift and mixed Apple-platform projects.
- Direct, lossless interoperability with Xcode projects and workspaces without
  converting them into an AppXCode-specific project format.
- First-class Swift Package Manager, CocoaPods, and Git workflows.
- Unified execution and reporting for XCTest, Quick, Kiwi, and Catch/Catch2.
- Build, run, test, and debug workflows for simulators and physical devices.
- A modern macOS experience based on the latest stable IntelliJ Platform UI, with
  AppCode used as the feature and workflow reference rather than a visual template.
- Future Linux and Windows companion editions that use a macOS build agent and can
  interact with a physical device connected locally.

Feature parity is the product destination, not a requirement for the first usable
release. Every phase must produce a testable increment and pass its exit gate
before the next dependent phase becomes a release commitment.

### Product priority

1. Deliver the best possible AppCode successor as a native macOS desktop product.
2. Preserve remote-capable internal boundaries so macOS implementation choices do
   not make future Linux and Windows editions impractical.
3. Begin production Linux and Windows work only after the macOS feature set is
   usable and stable. Cross-platform expansion must not block the macOS release.

The macOS product may use local optimizations and supported Apple integrations as
long as they remain behind replaceable interfaces. Feature design, acceptance, and
release decisions are led by the macOS experience rather than by the lowest common
denominator across desktop operating systems.

### Experience direction

The visual baseline is the latest stable IntelliJ Platform UI at the time of each
supported AppXCode release, adapted to current macOS conventions. The design should
reduce visual complexity, progressively reveal advanced controls, keep the editor
central, and offer Compact Mode for users who prefer higher information density.
AppCode remains a capability and productivity reference only; visual parity with
the discontinued IDE is explicitly not a goal.

AppXCode must use stable platform components, current light/dark themes, accessible
states, its own identity, and legally distributable assets. Product-specific UI is
reserved for Apple concepts such as schemes, destinations, signing, simulators,
devices, test plans, and remote build-agent state.

The detailed cross-cutting workstream is documented in
[Modern AppXCode experience](modern-appxcode-experience.md).

## 2. Confirmed constraints

- Xcode and Apple SDK-backed builds require a macOS build environment.
- Swift toolchains on Linux and Windows can support local Swift editing and host
  development, but they are not the authoritative semantic or build environment
  for projects importing Apple SDK frameworks.
- The standalone product cannot assume access to JetBrains' commercial IDE-only
  LSP implementation. AppXCode needs an owned language-server transport boundary.
- Signing identities, provisioning assets, and Apple developer credentials must
  remain on the macOS build node unless the user explicitly configures otherwise.
- Windows/Linux physical-device integration depends on protocols that Apple does
  not publish as a supported third-party IDE workflow. It must be isolated behind
  a replaceable device backend and treated as a compatibility-sensitive feature.

## 3. Recommended architecture

AppXCode is macOS-first and remote-capable. Even when all components run on one
Mac, Apple-specific operations should cross explicit service boundaries so they can
later be hosted remotely without weakening the primary local experience.

### 3.1 AppXCode client

The primary macOS IntelliJ Platform application owns the editor, project UI, Git
integration, run configurations, test UI, debugger UI, settings, and user
interaction. It must not invoke Xcode-specific executables directly. All build and
device operations go through versioned service contracts. Future Linux and Windows
clients reuse these contracts after the macOS workflows are proven.

### 3.2 Language services

Use a hybrid model:

- SourceKit-LSP provides compiler-backed semantic information.
- A Swift lexer/parser and lightweight PSI provide stable editor structure,
  indexing hooks, local syntax services, and a foundation for richer refactoring.
- AppXCode-owned symbol indexes and analysis layers provide features not exposed
  reliably through LSP.
- Local and remote language backends share one client contract with explicit URI
  and source-path mapping.

### 3.3 macOS build agent

The build agent owns Xcode discovery, SDK inventory, dependency resolution,
`xcodebuild` orchestration, code signing, simulator control, result bundles,
symbol artifacts, and the authoritative SourceKit-LSP process for Apple projects.
The same API supports a loopback local agent and a remote network agent.

### 3.4 device gateway

The device gateway runs on the computer to which a device is connected. It owns
pairing, discovery, installation, launch, test transport, logs, crash reports,
port forwarding, and debug-server access. macOS uses supported Apple tooling where
available; Linux and Windows use a separately replaceable compatibility backend.

### 3.5 Versioned protocol

Client, build agent, and device gateway communicate through a versioned protocol
that supports capability negotiation, cancellation, progress, streamed logs,
artifact transfer, source-path mapping, authentication, and compatibility checks.
The exact transport must be selected by an architecture decision record in Phase
0; gRPC with a schema-first contract is the leading candidate, not yet a locked
implementation choice.

## 4. Planned repository boundaries

The exact Gradle modules will be validated during Phase 0. The intended ownership
boundaries are:

- `platform/client`: standalone IntelliJ application shell.
- `platform/protocol`: versioned contracts and capability models.
- `services/build-agent`: macOS Xcode, build, signing, and artifact service.
- `services/device-gateway`: physical-device discovery and transport.
- `plugins/swift-language`: Swift file model, PSI, SourceKit bridge, and indexes.
- `plugins/xcode-project`: projects, workspaces, schemes, settings, SPM, CocoaPods.
- `plugins/apple-run`: targets, destinations, run configurations, deployment.
- `plugins/apple-test`: framework adapters and unified test model.
- `plugins/apple-debug`: LLDB and IntelliJ debugger integration.
- `design/modern-appxcode`: current IntelliJ/macOS references, interaction inventory,
  design decisions, tokens, and visual-regression baselines.
- `tests/fixtures`: representative Xcode, SwiftPM, CocoaPods, and mixed projects.
- `docs/adr`: durable architecture and licensing decisions.

Platform-neutral contracts must not depend on macOS-only types or filesystem
semantics.

## 5. Delivery sequence

| Phase | Outcome | Depends on |
|---|---|---|
| 0 | Architecture, licensing, compatibility policy, and executable technical spikes | None |
| 1 | Primary macOS IDE foundation with remote-capable service contracts | Phase 0 |
| 2 | Useful Swift editing, navigation, completion, analysis, and index foundation | Phase 1 |
| 3 | Lossless Xcode/SPM/CocoaPods project model and Git-aware synchronization | Phases 1-2 |
| 4 | Build, run, test, and debug on macOS across supported Apple platforms | Phases 2-3 |
| 5 | Advanced inspections, refactoring, and mixed-language intelligence | Phases 2-4 |
| 6 | Future remote macOS build node plus production Linux and Windows clients | Phases 1-5 and the macOS release gate |
| 7 | Future physical-device gateway on Linux and Windows | Phases 4 and 6 |
| 8A | macOS AppCode-parity audit, hardening, and primary-product release | Phases 1-5; does not depend on Phases 6-7 |
| 8B | Cross-platform expansion hardening and release readiness | Phases 6-7 and the macOS release baseline |

Detailed phase plans:

- [Phase 0 — Decisions and risk retirement](phase-00-decisions-and-risk-retirement.md)
- [Phase 1 — Platform foundation](phase-01-platform-foundation.md)
- [Phase 2 — Swift language intelligence](phase-02-swift-language-intelligence.md)
- [Phase 3 — Project and dependency systems](phase-03-project-and-dependency-systems.md)
- [Phase 4 — Build, run, test, and debug](phase-04-build-run-test-debug.md)
- [Phase 5 — Advanced intelligence and refactoring](phase-05-advanced-intelligence-refactoring.md)
- [Phase 6 — Cross-platform remote development](phase-06-cross-platform-remote-development.md)
- [Phase 7 — Cross-platform physical devices](phase-07-cross-platform-physical-devices.md)
- [Phase 8 — macOS release and cross-platform expansion hardening](phase-08-parity-and-release-hardening.md)

Cross-cutting workstream:

- [Modern AppXCode experience](modern-appxcode-experience.md)

## 6. Validation strategy

### Compatibility fixtures

Maintain versioned fixtures covering:

- Swift-only and mixed Swift/Objective-C/C/C++ projects.
- Single-project and multi-workspace arrangements.
- SwiftPM-only, CocoaPods-only, and combined dependency graphs.
- Application, framework, extension, test, watch companion, and tvOS targets.
- Shared/user schemes, custom build configurations, generated sources, and build
  scripts.
- Projects produced by every supported Xcode compatibility band.

### End-to-end release gates

For every supported platform, a release candidate must demonstrate:

1. Open the project without conversion or unintended project-file changes.
2. Resolve dependencies and index the active build context.
3. Build with diagnostics mapped back to the correct client files.
4. Run on the appropriate simulator or physical destination.
5. Discover, run, filter, and report supported tests.
6. Stop at breakpoints and inspect stack frames and variables.
7. Repeat the workflow through a remote build agent.
8. For iOS/iPadOS, repeat install/run/test on a device connected to a supported
   Linux or Windows gateway before that gateway is declared production-ready.

Items 7-8 apply to the later cross-platform editions and do not block the primary
macOS release. The macOS release must instead pass every applicable workflow using
its local build agent and Apple-supported simulator/device tooling.

### Quality gates

- Automated unit, integration, protocol-compatibility, and UI smoke tests.
- Golden screenshot and interaction tests for the modern AppXCode experience across
  supported macOS appearance, display scale, window size, and accessibility modes.
- IntelliJ Plugin Verifier and supported-JDK compatibility checks.
- Golden-file tests proving lossless Xcode project reads and controlled writes.
- Performance benchmarks for indexing, completion latency, remote synchronization,
  build event throughput, and memory use.
- Failure-injection tests for disconnected agents, cancelled builds, stale source
  mirrors, partial artifact transfers, and device removal.
- Security review of agent authentication, artifact integrity, secret storage, and
  log redaction.

## 7. Major risks and mitigations

### IntelliJ and language API churn

Mitigation: isolate platform APIs behind narrow modules, continuously run Plugin
Verifier, and maintain an explicit IDE compatibility matrix.

### UI customization drifting from the IntelliJ Platform

Mitigation: follow the latest stable IntelliJ UI guidelines, prefer supported
platform components, keep Apple-specific custom UI behind clear ownership boundaries,
and continuously test themes, scaling, accessibility, and platform upgrades. Avoid
private JetBrains assets and undocumented UI internals.

### Incomplete SourceKit-LSP feature coverage

Mitigation: keep an owned PSI/index/refactoring layer and measure every promised
feature against representative projects instead of assuming LSP parity.

### Xcode project format changes

Mitigation: preserve unknown fields, default to read-only project interpretation,
use Xcode tooling as the source of build truth, and gate all write support with
round-trip fixtures.

### Apple toolchain and device protocol changes

Mitigation: capability negotiation, pluggable toolchain/device backends, an OS and
Xcode support policy, nightly compatibility testing, and graceful degradation with
clear diagnostics.

### Remote workspace inconsistency

Mitigation: content-addressed synchronization, explicit source revisions, atomic
workspace activation, generated-file policy, and bidirectional path mapping.

### Signing and credential exposure

Mitigation: keep credentials on the build node, use OS credential stores, redact
logs, restrict agent permissions, authenticate all remote connections, and audit
every signing operation.

### Licensing and redistribution

Mitigation: complete legal/license review before adopting IntelliJ components,
Apple SDK-dependent workflows, or third-party device libraries. Keep optional
device implementations out of the core until their distribution model is approved.

## 8. Assumptions

- The macOS application is the primary product, first production release, and
  long-term reference implementation.
- Linux and Windows are later companion editions; their delivery must not delay or
  reduce the quality of the macOS application.
- Linux and Windows users have access to a user-owned, team-owned, or hosted Mac
  build node with a compatible Xcode installation.
- AppXCode will not redistribute Apple SDKs to non-Apple operating systems.
- Direct Xcode interoperability means Xcode remains able to open and build the
  same workspace after AppXCode use.
- Initial bring-up order is iOS/iPadOS, macOS, tvOS, then watchOS, while the domain
  model supports all target families from the start.
- Release dates will be estimated only after Phase 0 spikes and team capacity are
  known.

## 9. Open decisions

Phase 0 must resolve:

- Open-source license, commercial model, and IntelliJ Platform distribution terms.
- Supported Xcode, Swift, macOS, and device OS compatibility bands.
- Whether remote build nodes are user-managed only or also offered as a hosted
  service.
- Protocol transport and backward-compatibility window.
- Workspace synchronization strategy and treatment of generated files.
- Swift parser/PSI implementation source and ownership strategy.
- Device library adoption, process isolation, and license compatibility.
- Minimum test/debug capabilities required before each platform is marked supported.
- IntelliJ UI baseline/update policy and which Apple-specific controls justify
  AppXCode-owned components rather than standard platform UI.

## 10. Non-goals

- Building Apple SDK applications on Linux or Windows without a macOS/Xcode node.
- Redistributing Xcode or Apple SDK contents.
- Converting projects into a proprietary format.
- Reimplementing the Swift compiler, Xcode build system, or Apple code-signing
  service.
- Claiming full compatibility with every Xcode/iOS release without an explicit,
  tested support policy.

## 11. Definition of roadmap completion

The roadmap is complete when the parity ledger contains no unresolved capability
gaps for the declared support matrix; all platform end-to-end gates pass locally
and remotely; Linux/Windows device support passes its declared compatibility suite;
security, licensing, performance, and recovery reviews are complete; and the IDE
can coexist with Xcode on the same projects without conversion or destructive
project-file changes. The primary macOS product can reach its production release
gate after Phases 1-5 and the macOS portion of Phase 8; completion of the broader
cross-platform roadmap is a later milestone.
