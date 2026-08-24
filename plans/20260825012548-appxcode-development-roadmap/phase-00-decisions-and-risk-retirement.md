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
- Create the first compatibility fixtures and a feature-parity ledger.

## Deliverables

- Accepted architecture decision records in `docs/adr`.
- Initial compatibility and parity matrices.
- Modern AppXCode UX quality matrix and legally reviewed reference/asset policy.
- Spike reports with reproducible evidence and discarded approaches.
- Approved repository/module layout and dependency policy.
- A risk register with owners and review cadence.

## Exit gate

Proceed only when the team has demonstrated viable IntelliJ distribution,
SourceKit communication, Xcode project interpretation, local signed build/install,
and a credible remote protocol. Unresolved licensing or redistribution questions
block dependent implementation.
