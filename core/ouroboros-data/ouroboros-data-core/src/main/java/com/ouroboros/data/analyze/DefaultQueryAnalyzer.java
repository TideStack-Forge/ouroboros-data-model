package com.ouroboros.data.analyze;

import java.util.ArrayList;
import java.util.List;

import io.vavr.control.Try;

import com.ouroboros.data.dsl.statement.QueryStatement;

/**
 * 默认查询分析器
 *
 * <p>组合多个专门的分析器，按顺序执行语义验证任务：
 * <ol>
 *   <li>类型检查 - {@link TypeCheckAnalyzer}</li>
 *   <li>关联验证 - {@link RelationExpandAnalyzer}</li>
 * </ol>
 *
 * <p>分析器采用责任链模式，每个分析器的输出作为下一个分析器的输入。
 * 如果任何一个分析器失败，整个分析过程会立即终止并返回失败结果。
 *
 * <p><b>使用示例：</b>
 * <pre>{@code
 * QueryAnalyzer analyzer = new DefaultQueryAnalyzer();
 * QueryAnalyzeContext context = QueryAnalyzeContext.builder()
 *     .model(dataModel)
 *     .build();
 *
 * Try<QueryStatement> result = analyzer.analyze(statement, context);
 * if (result.isSuccess()) {
 *     QueryStatement analyzed = result.get();
 *     // 继续 Transpile 阶段...
 * } else {
 *     Throwable error = result.getCause();
 *     // 处理分析错误...
 * }
 * }</pre>
 *
 * @see QueryAnalyzer
 * @see TypeCheckAnalyzer
 * @see RelationExpandAnalyzer
 * @since 1.0.0-beta.2
 */
public class DefaultQueryAnalyzer implements QueryAnalyzer {

  /**
   * 分析器列表（按执行顺序）
   */
  private final List<QueryAnalyzer> analyzers;

  /**
   * 默认构造函数
   *
   * <p>使用标准的分析器组合：类型检查 → 关联验证
   */
  public DefaultQueryAnalyzer() {
    this.analyzers = new ArrayList<>();
    this.analyzers.add(new TypeCheckAnalyzer());
    this.analyzers.add(new RelationExpandAnalyzer());
  }

  /**
   * 自定义构造函数
   *
   * <p>允许使用自定义的分析器列表
   *
   * @param analyzers 分析器列表（将按给定顺序执行）
   */
  public DefaultQueryAnalyzer(List<QueryAnalyzer> analyzers) {
    this.analyzers = new ArrayList<>(analyzers);
  }

  @Override
  public Try<QueryStatement> analyze(QueryStatement statement, QueryAnalyzeContext context) {
    // 使用责任链模式，依次执行每个分析器
    Try<QueryStatement> current = Try.success(statement);

    for (QueryAnalyzer analyzer : analyzers) {
      // 如果上一个分析器失败，直接返回失败结果
      if (current.isFailure()) {
        return current;
      }

      // 检查分析器是否支持当前上下文
      if (!analyzer.supports(context)) {
        continue;
      }

      // 执行当前分析器
      current = analyzer.analyze(current.get(), context);
    }

    return current;
  }

  /**
   * 获取分析器列表
   *
   * @return 分析器列表（只读）
   */
  public List<QueryAnalyzer> getAnalyzers() {
    return new ArrayList<>(analyzers);
  }
}
