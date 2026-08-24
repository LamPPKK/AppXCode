# Phase 8 — macOS Release and Cross-Platform Expansion Hardening

## Objective

First close the declared AppCode capability gaps and prepare the primary macOS
application for a supportable production release. After Phases 6-7, repeat the
relevant hardening gates for the Linux and Windows expansion without redefining the
macOS product around cross-platform limitations.

## Release tracks

- **Phase 8A — macOS:** starts after Phases 1-5 and produces the primary AppXCode
  release. It does not wait for Linux/Windows clients or their device gateway.
- **Phase 8B — Cross-platform expansion:** starts after Phases 6-7 and validates
  Linux/Windows behavior against the released macOS reference implementation.

## Workstreams

- Audit every promised macOS capability against the parity ledger and support
  matrix, then maintain a separate expansion ledger for Linux and Windows.
- Stabilize project import, indexing, dependency refresh, build, test, debugger,
  simulator, device, signing, and remote-session recovery.
- Complete accessibility, localization readiness, keyboard navigation, settings
  migration, update rollback, diagnostics collection, and support tooling.
- Close the modern AppXCode UX quality ledger for Project, editor, navigation,
  search, Build, Run, Test, Debug, Git, inspections, settings, and window modes.
- Close the declared plugin compatibility catalog for the release, publish exact
  supported tiers/modules, and verify install, update, disable, uninstall, crash
  isolation, safe mode, and recovery behavior.
- Publish and pass the exact AppXCode/Dart-plugin/Flutter-plugin/Flutter-SDK/Xcode/
  CocoaPods compatibility matrix and all optimized Flutter profile gates.
- Publish and pass exact embedded-provider matrices for Apple Simulator, eligible
  physical screens, remote Mac, and any experimental vPhone release tuple. Keep
  vPhone experimental until its security, compatibility, recovery, latency, and
  legal gates explicitly justify a status change.
- Profile startup, indexing, memory, completion, remote sync, build streaming, and
  long-running IDE sessions against agreed budgets.
- Complete threat modeling, penetration review, dependency audit, artifact signing,
  supply-chain controls, privacy review, and secret-redaction verification.
- Publish compatibility, backup, recovery, data handling, remote-agent operations,
  known limitations, and migration documentation.
- Operate preview, alpha, beta, and release-candidate channels with explicit exit
  criteria and rollback procedures.

## Validation

- Run the complete fixture, platform, remote-agent, and real-device matrices.
- Conduct dogfooding and external beta projects without requiring conversion from
  Xcode.
- Conduct task-based usability tests with former AppCode users and visual-regression
  reviews across the approved macOS appearance matrix.
- Verify update and rollback across supported client/agent version combinations.
- Reopen every modified project in supported Xcode versions and confirm reproducible
  builds and controlled diffs.
- Require zero unresolved critical security, data-loss, signing, project-corruption,
  or silent-refactoring defects before release.

## Exit gates

### macOS primary release

All macOS capabilities advertised for the release have passing evidence,
operational ownership, documented limitations, and a supported recovery path. No
Linux or Windows deliverable is required for this gate.

### Cross-platform expansion release

The Linux and Windows capability ledger passes its declared remote-build and device
matrix against the macOS reference behavior. Platform-specific limitations are
explicitly documented rather than weakening or redefining the macOS product.
