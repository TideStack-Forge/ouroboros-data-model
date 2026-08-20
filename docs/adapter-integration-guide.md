# Adapter 接入指南

Ouroboros Data Model 的公开 core 保持平台中立。运行时行为通过公开 contract 由 adapter 提供，而不是依赖内部平台模块。

English documentation: [`adapter-integration-guide.en.md`](adapter-integration-guide.en.md)

## Adapter 点位

| Adapter 点位 | 公开 contract | 何时需要 | 接入方式 |
| --- | --- | --- | --- |
| 表达式求值 | `com.ouroboros.data.expression.DataExpressionEvaluator` | 默认值、编码主键或插件需要计算表达式。 | 在 `META-INF/services/com.ouroboros.data.expression.DataExpressionEvaluator` 注册 SPI 实现，把具体表达式引擎留在 adapter 模块。 |
| Data station/runtime | `com.ouroboros.data.model.DataStation` 及相关模型 contract | 应用需要执行模型 CRUD/query 行为。 | 在公开 core 外构建 runtime 模块，把 station 实现接到存储、事务和应用生命周期。 |
| SQL migration | `com.ouroboros.data.migration.SqlMigrationService` | 应用需要根据数据模型元数据变更 schema。 | 依赖 `ouroboros-data-sql-migration`，由应用部署/runtime 层调用迁移服务。 |
| 编码主键序列 | `com.ouroboros.data.pkgenerator.CodingSequencer` | `ouroboros-data-pkgen-coding` 模板使用 `<0001>` 或命名序列。 | 提供基于存储或序列服务的 sequencer，并注入 `FixLengthCodingGeneratorFactory`。 |
| Typed metadata 生成 | `ouroboros-data-typed-core` annotation 与 `ouroboros-data-typed-meta-processor` | 项目希望从 typed Java class 编译期生成数据模型元数据。 | 配置 annotation processor；仅在公开源码策略要求时提交生成元数据。 |
| 测试 fixture | `ouroboros-data-test-support` | 公开 adapter 需要不依赖平台 runtime glue 的可重复测试。 | 使用公开 fixture 与 H2/Spring 测试支撑，不导入内部 monorepo 测试模块。 |

## 表达式 Adapter

应用需要具体表达式语言时，实现 `DataExpressionEvaluator`。公开模块只依赖端口：

```java
public final class MyExpressionEvaluator implements DataExpressionEvaluator {
  @Override
  public Either<Throwable, Object> evaluate(String expression, Map<String, Object> context) {
    // Delegate to the expression engine owned by the adapter.
  }
}
```

注册文件：

```text
META-INF/services/com.ouroboros.data.expression.DataExpressionEvaluator
```

## 编码主键 Adapter

`ouroboros-data-pkgen-coding` 负责解析模板，并把序列状态委托给 `CodingSequencer`。持久化、分布式锁和租户级序列策略应保留在 adapter 中。

## Runtime 边界

公开模块不依赖平台生命周期、安全、脚本或 app 模块。相关能力应放在消费公开 contract 的 adapter 中，并可独立测试。
