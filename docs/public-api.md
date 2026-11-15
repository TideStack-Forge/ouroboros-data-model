# 公开 API 入口

本页是动态数据模型公开投影的 API 文档入口，只描述 manifest 选中的公开边界。

English documentation: [`public-api.en.md`](public-api.en.md)

## 包含模块

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

## API 边界

- `ouroboros-data-core` 负责模型元数据、statement、data station contract、表达式端口、迁移 contract 和通用主键 SPI。
- `ouroboros-data-typed-core` 负责 typed annotation 与模型 contract。
- `ouroboros-data-typed-meta-processor` 负责 typed metadata 的 annotation processing。
- `ouroboros-data-sql` 负责 SQL 查询与写入执行，并显式组合 `ouroboros-data-sql-migration` 以保留既有迁移 API。
- `ouroboros-data-sql-migration` 负责 schema-changing migration 实现，不反向依赖 SQL 查询模块。
- `ouroboros-data-pkgen-coding` 负责编码主键工厂，并使用 `ouroboros-data-core` 定义的表达式端口。
- `ouroboros-data-builders` 已弃用；新代码使用 data-core 查询门面与模型元数据 API。`ouroboros-data-test-support` 提供公开测试辅助能力。

## 暂缓或 runtime-owned 边界

- `data-pkgen-coding` 是公开模块，但 DB 序列状态仍由 runtime adapter 通过 `CodingSequencer` 提供。
- 雪花算法的具体实现仍由 runtime 持有；如果未来需要公开 contract，应作为独立 interface-only 模块引入。
- typed model runtime 有独立的公开/暂缓决策路径，不属于 util 或 exception support 范围。

## 构建

```bash
mvn -Ppublic -pl core/ouroboros-data -am test
mvn -Ppublic -pl core/ouroboros-data/ouroboros-data-core,core/ouroboros-data/ouroboros-data-plugins,core/ouroboros-data/ouroboros-data-typed-core,core/ouroboros-data/ouroboros-data-typed-meta-processor,core/ouroboros-data/ouroboros-data-sql,core/ouroboros-data/ouroboros-data-sql-migration,core/ouroboros-data/ouroboros-data-builders,core/ouroboros-data/ouroboros-data-test-support,core/ouroboros-data/ouroboros-data-pkgen-coding -am -DskipTests package
```
