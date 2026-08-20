package com.ouroboros.data.analyze;

import io.vavr.control.Try;

import com.ouroboros.data.dsl.statement.QueryStatement;

/**
 * 通配符处理预留转换器。
 *
 * <p>当前架构下，关联模型数据应通过 populate 进入，`relation.*` 不应在 Transform 阶段被展开为主查询投影。
 * 因此该转换器目前保持 no-op，由后续 Analyze / Transpile 路径按各自语义继续处理或报错。
 */
public class WildcardExpandAnalyzer implements QueryTransformer {

  @Override
  public boolean supports(QueryAnalyzeContext context) {
    return true;
  }

  @Override
  public Try<QueryStatement> transform(QueryStatement statement, QueryAnalyzeContext context) {
    return Try.success(statement);
  }
}
