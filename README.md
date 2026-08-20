# Ouroboros Data Model

本仓库是 Ouroboros 动态数据模型的公开投影。它由内部 monorepo 按 manifest 作用域导出，公开仓库历史从首次开源发布重新开始，不携带内部 Git 历史。

English documentation: [`README.en.md`](README.en.md)

- 导出 manifest：`internal export manifest (not copied)`
- Manifest SHA-256：`4e14ff7412b973929966184f3b362455f03c2c2e8297e3527ce97bc6cb601604`
- 导出模式：`sync`
- 开源协议：Apache License 2.0，见 `LICENSE`
- 非自动动作：本导出流程不会自动执行 remote push、merge、publish、release、deploy 或 promotion

## 公开构建面

运行 public profile 冒烟测试和 manifest 选中模块的打包验证：

```bash
mvn -Ppublic -pl core/ouroboros-data -am test
mvn -Ppublic -pl core/ouroboros-data/ouroboros-data-core,core/ouroboros-data/ouroboros-data-plugins,core/ouroboros-data/ouroboros-data-typed-core,core/ouroboros-data/ouroboros-data-typed-meta-processor,core/ouroboros-data/ouroboros-data-sql,core/ouroboros-data/ouroboros-data-sql-migration,core/ouroboros-data/ouroboros-data-builders,core/ouroboros-data/ouroboros-data-test-support,core/ouroboros-data/ouroboros-data-pkgen-coding -am -DskipTests package
```

public profile 是构建和校验边界，不代表 Maven 发布承诺。下方坐标仅作为公开评审草案。

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

## 文档

- 公开 API 入口：[`docs/public-api.md`](docs/public-api.md) / [`docs/public-api.en.md`](docs/public-api.en.md)
- 贡献指南：[`CONTRIBUTING.md`](CONTRIBUTING.md) / [`CONTRIBUTING.en.md`](CONTRIBUTING.en.md)
- 适配器接入指南：[`docs/adapter-integration-guide.md`](docs/adapter-integration-guide.md) / [`docs/adapter-integration-guide.en.md`](docs/adapter-integration-guide.en.md)
- 最小示例：[`docs/examples/minimal-data-model.md`](docs/examples/minimal-data-model.md) / [`docs/examples/minimal-data-model.en.md`](docs/examples/minimal-data-model.en.md)
- 数据模块总览：[`core/ouroboros-data/README.md`](core/ouroboros-data/README.md) / [`core/ouroboros-data/README.en.md`](core/ouroboros-data/README.en.md)

## 贡献策略

外部贡献需要 DCO sign-off。当前公开投影流程不要求 CLA。

## 主键边界

`data-pkgen-coding` 属于首批公开 profile。它通过 `ouroboros-data-core` 中的 `DataExpressionEvaluator` 端口接入表达式能力；具体表达式引擎、数据库序列、脚本包装和雪花算法运行时适配器都留在外部 adapter 中。

## 发布门禁

`release-gate-report.md` 记录本地开源协议、敏感内容、公开 API、CI 模板和主键文档门禁结果。
