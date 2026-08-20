# Minimal Dynamic Data Model Example

This example is intentionally configuration-level so it can stay stable while
the public API surface is finalized.

```yaml
dataModel:
  name: sample_user
  station: sql
  table: sample_user
  fields:
    - name: id
      valueType: string
      primaryKey: true
    - name: display_name
      valueType: string
query:
  select:
    - id
    - display_name
  from: sample_user
  where:
    display_name: Ada
```

Use `ouroboros-data-core` for model metadata and contracts, and add a public
runtime adapter only after the runtime split decision marks it as public.
