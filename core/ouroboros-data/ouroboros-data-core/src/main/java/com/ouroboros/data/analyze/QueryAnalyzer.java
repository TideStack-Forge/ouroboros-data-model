package com.ouroboros.data.analyze;

import io.vavr.control.Try;

import com.ouroboros.data.dsl.statement.QueryStatement;

/**
 * 查询分析器接口
 *
 * <p>Analyze 阶段负责对查询语句进行语义验证，不承担结构改写职责。
 * 查询重写由 {@link QueryTransformer} 负责，Analyze 只消费已经完成本阶段前置改写的语句。
 *
 * <p><b>当前架构（三阶段）：</b>
 * <pre>
 * Map → Normalize → QueryStatement → Transform → Analyze → Transpile → QueryDSL
 * </pre>
 *
 * <p><b>Analyze 阶段职责：</b>
 * <ul>
 *   <li>类型检查 - 验证字段类型、操作符兼容性</li>
 *   <li>语义验证 - 检查关联关系、子查询正确性</li>
 *   <li>安全检查 - SQL 注入防护、权限验证</li>
 * </ul>
 *
 * <p><b>使用示例：</b>
 * <pre>{@code
 * QueryTransformer transformer = new DefaultQueryTransformer();
 * QueryAnalyzer analyzer = new DefaultQueryAnalyzer();
 * QueryAnalyzeContext context = QueryAnalyzeContext.builder()
 *     .model(orderModel)
 *     .enableTypeChecking(true)
 *     .enableOptimization(true)
 *     .build();
 *
 * QueryStatement transformed = transformer.transform(normalizedStatement, context).get();
 * Try<QueryStatement> result = analyzer.analyze(transformed, context);
 * }</pre>
 *
 * <p><b>未来架构演进（v2.0）：</b>
 * <pre>
 * 当前：Normalize → Analyze → Transpile
 * 目标：Parse → Analyze → Plan → Generate
 *
 * Plan 阶段预期功能：
 * - 跨数据源查询拆分
 * - JOIN 策略优化
 * - 并行执行规划
 * </pre>
 *
 * @see QueryAnalyzeContext
 * @since 1.0.0-beta.2
 */
public interface QueryAnalyzer {

  /**
   * 判断此分析器是否支持给定的上下文
   *
   * <p>如果返回 false，{@link DefaultQueryAnalyzer} 将跳过此分析器。
   * 默认实现返回 true，表示支持所有上下文。
   *
   * <p>子类可以覆盖此方法来实现条件执行，例如：
   * <ul>
   *   <li>根据配置开关决定是否执行（如 {@code context.isTypeCheckingEnabled()}）</li>
   *   <li>根据上下文状态决定是否执行（如 {@code context.getModel() != null}）</li>
   * </ul>
   *
   * @param context 分析上下文
   * @return true 如果支持，false 如果应跳过
   * @since 1.0.0-beta.2
   */
  default boolean supports(QueryAnalyzeContext context) {
    return true;
  }

  /**
   * 分析查询语句
   *
   * <p>对 transform 后的查询语句进行语义验证，不应再执行结构改写。
   *
   * @param statement transform 后的查询语句
   * @param context   分析上下文，包含场景、模型元数据、配置选项
   * @return 分析后的查询语句
   * - Success: 分析成功，返回验证通过的语句
   * - Failure: 分析失败，包含错误信息（如类型不匹配、字段不存在）
   */
  Try<QueryStatement> analyze(QueryStatement statement, QueryAnalyzeContext context);
}
