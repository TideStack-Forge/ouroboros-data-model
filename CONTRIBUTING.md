# Contributing to Ouroboros Data Model

Thank you for improving the public data model project. This repository is a
public projection of the internal Ouroboros monorepo, so maintainers import
accepted changes back through the manifest-scoped sync workflow.

## Development Baseline

- Java 21 is required.
- Run `mvn -Ppublic -pl core/ouroboros-data -am test` before submitting a change.
- Keep public modules free of platform imports such as security, lifecycle,
  script, application, or internal runtime glue.
- Add or update tests for behavioral changes.

## Public Boundary Rules

- Use `DataExpressionEvaluator` from `ouroboros-data-core` for expression needs;
  do not depend on a concrete expression engine from a platform module.
- Keep DB-backed sequence implementations, snowflake adapters and platform
  lifecycle bridges outside `data-pkgen-coding`.
- Keep runtime/application adapters out of core, SQL, typed, builder and test
  support modules.
- Do not add generated files, local build output, private metadata or internal
  repository paths.

## Pull Request Checklist

- [ ] The change is limited to public modules or public documentation.
- [ ] `mvn -Ppublic -pl core/ouroboros-data -am test` passes.
- [ ] No internal platform imports were added.
- [ ] Contributions are signed off with DCO.
- [ ] Public documentation was updated for API or adapter behavior changes.
