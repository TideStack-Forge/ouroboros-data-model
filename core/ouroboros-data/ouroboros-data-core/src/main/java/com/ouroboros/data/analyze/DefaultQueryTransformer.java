package com.ouroboros.data.analyze;

import java.util.ArrayList;
import java.util.List;

import io.vavr.control.Try;

import com.ouroboros.data.dsl.statement.QueryStatement;

/**
 * 默认查询转换器。
 *
 * <p>按顺序执行一组 {@link QueryTransformer}，用于在 Analyze 前完成语句改写。
 */
public class DefaultQueryTransformer implements QueryTransformer {

  private final List<QueryTransformer> transformers;

  public DefaultQueryTransformer() {
    this.transformers = new ArrayList<>();
    this.transformers.add(new WildcardExpandAnalyzer());
    this.transformers.add(new OptimizationAnalyzer());
  }

  public DefaultQueryTransformer(List<? extends QueryTransformer> transformers) {
    this.transformers = new ArrayList<>(transformers);
  }

  @Override
  public Try<QueryStatement> transform(QueryStatement statement, QueryAnalyzeContext context) {
    Try<QueryStatement> current = Try.success(statement);

    for (QueryTransformer transformer : transformers) {
      if (current.isFailure()) {
        return current;
      }
      if (!transformer.supports(context)) {
        continue;
      }
      current = transformer.transform(current.get(), context);
    }

    return current;
  }
}
