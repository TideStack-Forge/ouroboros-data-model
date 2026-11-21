# Public PR Import Dry Run Report

## Summary

- Status: `passed`
- Generated at: `2026-11-21T13:07:00.707693+00:00`
- Public repo: `public-tree-local-fixture`
- Public PR/ref: `fixture`
- Public ref recorded in public tree: `false`
- Target branch: `import-branch-dry-run`
- Manifest: `internal export manifest (not copied)`
- Manifest SHA-256: `e9dea1db9d21822518b024a449de22023baf3be9825fa2df55fc3ff5295a18df`
- Dry run: `true`
- Direct merge to master: `false`
- Remote actions: none

## Manifest-Owned Import Boundary

- Allowed roots:

- `.github/workflows/public-ci.yml`
- `LICENSE`
- `README.md`
- `core/ouroboros-data/README.md`
- `core/ouroboros-data/ouroboros-data-builders`
- `core/ouroboros-data/ouroboros-data-core`
- `core/ouroboros-data/ouroboros-data-pkgen-coding`
- `core/ouroboros-data/ouroboros-data-plugins`
- `core/ouroboros-data/ouroboros-data-sql`
- `core/ouroboros-data/ouroboros-data-sql-migration`
- `core/ouroboros-data/ouroboros-data-test-support`
- `core/ouroboros-data/ouroboros-data-typed-core`
- `core/ouroboros-data/ouroboros-data-typed-meta-processor`
- `docs`

- Candidate changed paths:

- `core/ouroboros-data/ouroboros-data-core/PUBLIC_IMPORT_FIXTURE.md`
- `platform/internal-only/SHOULD_NOT_IMPORT.md`

- Would import:

- `core/ouroboros-data/ouroboros-data-core/PUBLIC_IMPORT_FIXTURE.md`

- Blocked as out-of-boundary:

- `platform/internal-only/SHOULD_NOT_IMPORT.md`

## Attribution Evidence

- Public author: `Fixture Public Contributor <fixture.public@example.invalid>`
- Commit author: `Fixture Public Contributor <fixture.public@example.invalid>`
- Co-authors: `none`
- DCO evidence: `missing-fixture-evidence`
- CLA evidence: `not-verified-fixture-evidence`
- Policy note: public imports record DCO evidence; CLA is informational historical evidence only, not a release gate.

## Non-Automatic Actions

- Branch creation: not performed.
- Direct merge: not performed.
- Remote push: not performed.
- Publish/release/deploy: not performed.

## Public Worktree Cleanliness

```text
?? forbidden-import-report.md
```
