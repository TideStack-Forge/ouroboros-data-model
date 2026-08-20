# 最小动态数据模型示例

这个示例刻意保持在配置层，便于在公开 API 边界继续稳定前保持可读。

English documentation: [`minimal-data-model.en.md`](minimal-data-model.en.md)

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

使用 `ouroboros-data-core` 承载模型元数据和 contract。runtime adapter 只有在 runtime split 决策进入公开范围后才应加入公开构建面。
