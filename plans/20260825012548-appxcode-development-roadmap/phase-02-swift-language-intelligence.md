# Phase 2 — Swift Language Intelligence

## Objective

Deliver a useful Swift editing experience and create the architecture required for
JetBrains-grade analysis and refactoring.

## Workstreams

- Register Swift files and implement lexer/parser/PSI coverage sufficient for
  structure, syntax highlighting, folding, comments, selection, and local edits.
- Build the owned SourceKit-LSP client boundary with lifecycle, restart, toolchain
  selection, document synchronization, custom request support, and diagnostics.
- Support both local SourceKit and remote macOS SourceKit with stable URI mapping.
- Implement completion, quick documentation, signature help, definition,
  references, symbols, semantic highlighting, formatting, and rename where the
  backend can prove safety.
- Establish persistent indexes and invalidation rules for workspace symbols,
  dependencies, generated sources, and build configurations.
- Define mixed-language symbol identity so Swift/Objective-C interoperability can
  be added without replacing the index model.
- Add performance and correctness fixtures derived from realistic Swift codebases.

## Validation

- Compare language results with compiler builds across supported Swift versions.
- Exercise incomplete and temporarily invalid code without UI freezes or index
  corruption.
- Verify local and remote backends return equivalent source locations.
- Benchmark initial indexing, incremental edits, completion responsiveness, memory,
  server recovery, and cancellation.

## Exit gate

Swift projects provide reliable daily-use editing and navigation, remote Apple SDK
symbols resolve correctly, and the PSI/index foundation can support Phase 5 without
a language architecture rewrite.
