# Phase 5 — Advanced Intelligence and Refactoring

## Objective

Move beyond baseline LSP behavior toward the analysis and safe transformation
quality expected from a JetBrains IDE.

## Workstreams

- Establish a parity ledger for inspections, intentions, quick fixes, navigation,
  search, hierarchy views, documentation, and refactoring.
- Implement configuration-aware inspections using PSI, indexes, compiler data, and
  build settings with clear confidence levels.
- Add safe rename, move, change-signature, extract, inline, generate, and structural
  transformations in an evidence-driven order.
- Support Swift/Objective-C symbol relationships, generated interfaces, bridging
  headers, module maps, C/C++ headers, and cross-language navigation.
- Ensure transformations understand targets, conditional compilation, platform
  availability, tests, and dependency boundaries.
- Add preview, conflict detection, atomic writes, rollback, undo, and formatter
  coordination for every multi-file transformation.

## Validation

- Corpus tests compare expected findings and edits across Swift and Xcode versions.
- Every refactoring has positive, conflict, cancellation, rollback, and invalid-code
  coverage.
- Multi-target and conditional-compilation fixtures prove edits do not silently
  break inactive configurations.
- Compiler validation runs after transformations in representative projects.

## Exit gate

The declared advanced-intelligence subset is measurably safer and richer than the
baseline LSP feature set, and every advertised refactoring has documented scope and
automated safety evidence.
