# 参与贡献 Ouroboros Data Model

感谢你改进公开数据模型项目。本仓库是内部 Ouroboros monorepo 的公开投影，维护者会通过 manifest 作用域同步流程把被接受的变更回流到内部仓库。

English documentation: [`CONTRIBUTING.en.md`](CONTRIBUTING.en.md)

## 开发基线

- 需要 Java 21。
- 提交前运行 `mvn -Ppublic -pl core/ouroboros-data -am test`。
- 公开模块不得引入 security、lifecycle、script、application 或内部 runtime glue 等平台依赖。
- 行为变更必须新增或更新测试。

## 公开边界规则

- 表达式能力通过 `ouroboros-data-core` 的 `DataExpressionEvaluator` 端口接入，不依赖具体平台表达式引擎。
- DB 序列实现、雪花算法适配器和平台生命周期桥接不得放入 `data-pkgen-coding`。
- runtime/application adapter 不进入 core、SQL、typed、builder 和 test-support 公开模块。
- 不提交生成产物、本地构建输出、私有元数据或内部仓库路径。

## Pull Request 检查清单

- [ ] 变更只触及公开模块或公开文档。
- [ ] `mvn -Ppublic -pl core/ouroboros-data -am test` 通过。
- [ ] 没有新增内部平台 import。
- [ ] 提交带有 DCO sign-off。
- [ ] API 或 adapter 行为变化已更新公开文档。
