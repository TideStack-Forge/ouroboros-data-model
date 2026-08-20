# Public API Entry

This page is the public documentation entry for the dynamic data model export.
It intentionally describes only the manifest-selected public surface.

## Included Modules

| Module | Path | Draft coordinate |
| --- | --- | --- |
| data-core | core/ouroboros-data/ouroboros-data-core | `com.ouroboros:ouroboros-data-core:${project.version}` |
| data-plugins | core/ouroboros-data/ouroboros-data-plugins | `com.ouroboros:ouroboros-data-plugins:${project.version}` |
| data-typed-core | core/ouroboros-data/ouroboros-data-typed-core | `com.ouroboros:ouroboros-data-typed-core:${project.version}` |
| data-typed-meta-processor | core/ouroboros-data/ouroboros-data-typed-meta-processor | `com.ouroboros:ouroboros-data-typed-meta-processor:${project.version}` |
| data-sql | core/ouroboros-data/ouroboros-data-sql | `com.ouroboros:ouroboros-data-sql:${project.version}` |
| data-sql-migration | core/ouroboros-data/ouroboros-data-sql-migration | `com.ouroboros:ouroboros-data-sql-migration:${project.version}` |
| data-builders | core/ouroboros-data/ouroboros-data-builders | `com.ouroboros:ouroboros-data-builders:${project.version}` |
| data-test-support | core/ouroboros-data/ouroboros-data-test-support | `com.ouroboros:ouroboros-data-test-support:${project.version}` |
| data-pkgen-coding | core/ouroboros-data/ouroboros-data-pkgen-coding | `com.ouroboros:ouroboros-data-pkgen-coding:${project.version}` |

## API Surface

- `ouroboros-data-core` owns model metadata, statements, data station contracts,
  expression ports, migration contracts, and generic primary-key SPI.
- `ouroboros-data-typed-core` owns typed annotations/model contracts.
- `ouroboros-data-typed-meta-processor` owns typed metadata annotation
  processing.
- `ouroboros-data-sql` owns SQL query and mutation execution and explicitly
  composes `ouroboros-data-sql-migration` to preserve existing migration APIs.
- `ouroboros-data-sql-migration` owns schema-changing migration implementation
  and does not depend back on the SQL query module.
- `ouroboros-data-pkgen-coding` owns the coding primary-key factory and uses
  the expression port defined by `ouroboros-data-core`.
- `ouroboros-data-builders` and `ouroboros-data-test-support` provide optional
  construction and test helpers when their dependencies remain public.

## Deferred Or Runtime-Owned Surface

- `data-pkgen-coding` is public, but DB-backed sequence behavior remains
  runtime-owned and must be provided by an adapter through `CodingSequencer`.
- Snowflake-specific concrete algorithms remain runtime-owned. If a public
  snowflake contract becomes necessary, it should be introduced as a dedicated
  interface-only module rather than folded into coding primary-key contracts.
- Typed model runtime has its own public/deferred decision path and is not
  described as part of util or exception support.

## Build

```bash
mvn -Ppublic -pl core/ouroboros-data -am test
mvn -Ppublic -pl core/ouroboros-data/ouroboros-data-core,core/ouroboros-data/ouroboros-data-plugins,core/ouroboros-data/ouroboros-data-typed-core,core/ouroboros-data/ouroboros-data-typed-meta-processor,core/ouroboros-data/ouroboros-data-sql,core/ouroboros-data/ouroboros-data-sql-migration,core/ouroboros-data/ouroboros-data-builders,core/ouroboros-data/ouroboros-data-test-support,core/ouroboros-data/ouroboros-data-pkgen-coding -am -DskipTests package
```
