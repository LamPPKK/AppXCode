# Phase 0 — Decisions and Risk Retirement

## Objective

Resolve the high-cost unknowns before choosing irreversible implementation details.

## Workstreams

- Record product scope, parity definition, support matrix, and release vocabulary.
- Review IntelliJ Platform, bundled runtime, Apple tooling, and third-party device
  library licensing and distribution constraints.
- Spike a standalone IntelliJ application that can start an owned external service.
- Spike SourceKit-LSP transport, document synchronization, cancellation, and remote
  source-path mapping without depending on the commercial JetBrains LSP module.
- Open representative Xcode projects and compare project-file parsing with
  `xcodebuild`-reported schemes, targets, settings, and destinations.
- Demonstrate a signed development build installed on a locally attached iPhone
  from macOS, then assess Linux/Windows discovery and installation separately.
- Evaluate protocol transport, authentication, capability negotiation, and artifact
  transfer options through architecture decision records.
- Assemble a living reference pack from the latest stable IntelliJ Platform UI
  guidelines and current macOS conventions; decide the platform-version policy,
  density, theme, icon, branding, accessibility, and custom-component policies.
- Inventory the upstream IntelliJ modules AppXCode can lawfully ship, classify
  representative Marketplace plugins by declared dependencies, run Plugin Verifier
  against a custom product build, and decide the product-code/repository strategy.
- Spike the official Dart and Flutter IntelliJ plugins against the candidate AppXCode
  distribution; inventory their exact runtime dependencies, distribution terms,
  project import, analysis, hot reload, debug, DevTools, and native iOS workspace
  integration requirements.
- Spike the provider-neutral Embedded Devices model against a deterministic fake,
  Apple Simulator, and a pinned `Lakr233/vphone-cli` release. For vPhone, verify
  machine-readable inventory, supervised lifecycle, local VNC display, control-
  socket input/screenshot/clipboard, failure recovery, and resource isolation.
- Determine the public Apple boundary for embedding simulator and physical-device
  screens. Record a supported fallback instead of relying on undocumented Xcode UI
  internals or loading private frameworks into the IDE.
- Prove or reject build/install/launch, logs, XCTest-family execution, LLDB attach,
  and Flutter hot reload/debug on candidate vPhone firmware/variant tuples. Complete
  host-security, firmware, licensing, redistribution, and supply-chain review.
- Create the first compatibility fixtures and a feature-parity ledger.

## Deliverables

- Accepted architecture decision records in `docs/adr`.
- Initial compatibility and parity matrices.
- Modern AppXCode UX quality matrix and legally reviewed reference/asset policy.
- Initial plugin support tiers, module inventory, compatibility catalog, and
  Marketplace/custom-repository decision record.
- Flutter/Dart compatibility spike, pinned version proposal, dependency gap report,
  and upstream-versus-adapter decision record.
- Embedded device provider/display/input ADRs, measured performance report, Apple
  public-API boundary, and vPhone security/distribution/capability decision record.
- Spike reports with reproducible evidence and discarded approaches.
- Approved repository/module layout and dependency policy.
- A risk register with owners and review cadence.

## Exit gate

Proceed only when the team has demonstrated viable IntelliJ distribution,
SourceKit communication, Xcode project interpretation, local signed build/install,
and a credible remote protocol. Unresolved licensing or redistribution questions
block dependent implementation.
