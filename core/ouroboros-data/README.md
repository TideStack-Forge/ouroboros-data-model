# Ouroboros Data Public Modules

This directory contains the public dynamic data model build surface selected by
the export manifest.

## Build

```bash
mvn -Ppublic -pl core/ouroboros-data -am test
mvn -Ppublic -pl core/ouroboros-data/ouroboros-data-core,core/ouroboros-data/ouroboros-data-plugins,core/ouroboros-data/ouroboros-data-typed-core,core/ouroboros-data/ouroboros-data-typed-meta-processor,core/ouroboros-data/ouroboros-data-sql,core/ouroboros-data/ouroboros-data-sql-migration,core/ouroboros-data/ouroboros-data-builders,core/ouroboros-data/ouroboros-data-test-support,core/ouroboros-data/ouroboros-data-pkgen-coding -am -DskipTests package
```

## Module Boundary

- `ouroboros-data-core`: public contracts, metadata, statements, ports, and
  platform-neutral behavior.
- `ouroboros-data-sql`: SQL query and mutation execution.
- `ouroboros-data-sql-migration`: schema migration implementation and
  `SqlMigrationService` tool surface, consumed explicitly by SQL/test support
  while keeping schema-changing code isolated.
- `ouroboros-data-typed-core`: typed annotations and model contracts.
- `ouroboros-data-typed-meta-processor`: typed metadata annotation processor.
- `ouroboros-data-pkgen-coding`: coding primary-key template parser and factory
  backed by the `DataExpressionEvaluator` and `CodingSequencer` public ports.
- `ouroboros-data-builders`: public builder helpers.
- `ouroboros-data-test-support`: public test fixtures and helpers.

Runtime-backed sequence implementations, script wrappers and snowflake adapters
are outside the public coding module and must be supplied by adapters.
