# Phase 8 — Parity and Release Hardening

## Objective

Close the declared AppCode capability gaps and prepare a supportable 1.0 product.

## Workstreams

- Audit every promised capability against the parity ledger and support matrix.
- Stabilize project import, indexing, dependency refresh, build, test, debugger,
  simulator, device, signing, and remote-session recovery.
- Complete accessibility, localization readiness, keyboard navigation, settings
  migration, update rollback, diagnostics collection, and support tooling.
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
- Verify update and rollback across supported client/agent version combinations.
- Reopen every modified project in supported Xcode versions and confirm reproducible
  builds and controlled diffs.
- Require zero unresolved critical security, data-loss, signing, project-corruption,
  or silent-refactoring defects before release.

## Exit gate

All capabilities advertised for 1.0 have passing evidence, operational ownership,
documented limitations, and a supported recovery path. Remaining parity items are
explicitly deferred rather than implied complete.
