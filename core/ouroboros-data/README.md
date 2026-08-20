# Ouroboros Data 公开模块

本目录包含 export manifest 选中的动态数据模型公开构建面。

English documentation: [`README.en.md`](README.en.md)

## 构建

```bash
mvn -Ppublic -pl core/ouroboros-data -am test
mvn -Ppublic -pl core/ouroboros-data/ouroboros-data-core,core/ouroboros-data/ouroboros-data-plugins,core/ouroboros-data/ouroboros-data-typed-core,core/ouroboros-data/ouroboros-data-typed-meta-processor,core/ouroboros-data/ouroboros-data-sql,core/ouroboros-data/ouroboros-data-sql-migration,core/ouroboros-data/ouroboros-data-builders,core/ouroboros-data/ouroboros-data-test-support,core/ouroboros-data/ouroboros-data-pkgen-coding -am -DskipTests package
```

## 模块边界

- `ouroboros-data-core`：公开 contract、元数据、statement、port 与平台中立行为。
- `ouroboros-data-sql`：SQL 查询和写入执行。
- `ouroboros-data-sql-migration`：schema migration 实现与 `SqlMigrationService` tool surface，由 SQL/test support 显式消费，同时隔离 schema-changing 代码。
- `ouroboros-data-typed-core`：typed annotation 与模型 contract。
- `ouroboros-data-typed-meta-processor`：typed metadata annotation processor。
- `ouroboros-data-pkgen-coding`：编码主键模板解析与 factory，依赖 `DataExpressionEvaluator` 和 `CodingSequencer` 公开端口。
- `ouroboros-data-builders`：公开 builder 辅助能力。
- `ouroboros-data-test-support`：公开测试 fixture 与辅助能力。

runtime-backed 序列实现、脚本包装和雪花算法 adapter 不属于公开 coding 模块，必须由 adapter 提供。
