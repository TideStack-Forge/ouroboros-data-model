# Adapter Integration Guide

Ouroboros Data Model keeps the public core platform-neutral. Runtime behavior
is supplied by adapters through public contracts instead of internal platform
dependencies.

## Adapter Points

| Adapter point | Public contract | Required when | Integration approach |
| --- | --- | --- | --- |
| Expression evaluation | `com.ouroboros.data.expression.DataExpressionEvaluator` | Default values, coding primary keys or plugins evaluate expressions. | Provide an SPI implementation in `META-INF/services/com.ouroboros.data.expression.DataExpressionEvaluator`. Keep the expression engine dependency in the adapter module. |
| Data station/runtime | `com.ouroboros.data.model.DataStation` and related model contracts | Applications need to execute model CRUD/query behavior. | Build a runtime module that wires a station implementation to storage, transactions and application lifecycle outside the public core. |
| SQL migration | `com.ouroboros.data.migration.SqlMigrationService` | Applications need schema changes from data model metadata. | Depend on `ouroboros-data-sql-migration` and invoke migration services from the application's deployment/runtime layer. |
| Coding primary-key sequence | `com.ouroboros.data.pkgenerator.CodingSequencer` | `ouroboros-data-pkgen-coding` templates use `<0001>` or named sequences. | Provide a sequencer backed by your storage or sequence service and inject it into `FixLengthCodingGeneratorFactory`. |
| Typed metadata generation | `ouroboros-data-typed-core` annotations plus `ouroboros-data-typed-meta-processor` | Projects want compile-time generation of data model metadata from typed Java classes. | Add the processor to annotation processing and commit generated metadata only when it is part of your public source policy. |
| Test fixtures | `ouroboros-data-test-support` | Public adapters need repeatable tests without platform runtime glue. | Use public fixtures and H2/Spring test support; do not import internal monorepo test modules. |

## Expression Adapter

Implement `DataExpressionEvaluator` when an application needs a concrete
expression language. The public modules only know the port:

```java
public final class MyExpressionEvaluator implements DataExpressionEvaluator {
  @Override
  public Either<Throwable, Object> evaluate(String expression, Map<String, Object> context) {
    // Delegate to the expression engine owned by the adapter.
  }
}
```

Register the implementation:

```text
META-INF/services/com.ouroboros.data.expression.DataExpressionEvaluator
```

## Coding Primary-Key Adapter

`ouroboros-data-pkgen-coding` parses templates and delegates sequence state to
`CodingSequencer`. Keep persistence, distributed locks and tenant-specific
sequence policies in your adapter.

## Runtime Boundary

Public modules should not depend on platform lifecycle, security, script or app
modules. Put those concerns in an adapter that consumes public contracts and can
be tested independently.
