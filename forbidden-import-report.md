# Public Forbidden Import/API Guard Report

## Summary

- Status: `passed`
- Manifest: `internal export manifest`
- Manifest SHA-256: `e9dea1db9d21822518b024a449de22023baf3be9825fa2df55fc3ff5295a18df`
- Public tree: `public-tree-local-fixture`

## Scan Roots

- `core/ouroboros-data/ouroboros-data-core`
- `core/ouroboros-data/ouroboros-data-plugins`
- `core/ouroboros-data/ouroboros-data-typed-core`
- `core/ouroboros-data/ouroboros-data-typed-meta-processor`
- `core/ouroboros-data/ouroboros-data-sql`
- `core/ouroboros-data/ouroboros-data-sql-migration`
- `core/ouroboros-data/ouroboros-data-builders`
- `core/ouroboros-data/ouroboros-data-test-support`
- `core/ouroboros-data/ouroboros-data-pkgen-coding`

## Forbidden Import Matches

- Count: `0`

- none

## Public API/Docs Forbidden Mentions

- Count: `0`

- none

## Runtime Adapter Note

Runtime adapter imports are not treated as public core/sql/sql-migration allowlists.
This guard scans only manifest-selected public roots and the SQL migration split module.
