# Public Forbidden Import/API Guard Report

## Summary

- Status: `passed`
- Manifest: `internal export manifest`
- Manifest SHA-256: `4e14ff7412b973929966184f3b362455f03c2c2e8297e3527ce97bc6cb601604`
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
