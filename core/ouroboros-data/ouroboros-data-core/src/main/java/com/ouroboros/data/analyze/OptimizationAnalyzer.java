package com.ouroboros.data.analyze;

import java.util.ArrayList;
import java.util.List;

import io.vavr.control.Try;

import com.ouroboros.data.dsl.Operators;
import com.ouroboros.data.dsl.SExpression;
import com.ouroboros.data.dsl.statement.QueryStatement;

/**
 * 查询优化分析器
 *
 * <p>对查询语句进行优化，提高查询效率。
 *
 * <p><b>优化策略：</b>
 * <ul>
 *   <li>常量折叠 - AND(TRUE, x) → x, OR(FALSE, x) → x</li>
 *   <li>短路优化 - AND(FALSE, ...) → FALSE, OR(TRUE, ...) → TRUE</li>
 *   <li>冗余消除 - AND(x, TRUE) → x, OR(x, FALSE) → x</li>
 *   <li>双重否定消除 - NOT(NOT(x)) → x</li>
 *   <li>空表达式清理 - AND(x, EMPTY) → x</li>
 * </ul>
 *
 * <p><b>注意：</b>
 * <ul>
 *   <li>如果 {@code enableOptimization} 为 false，则跳过优化</li>
 *   <li>优化不改变查询语义，仅改变表达式结构</li>
 *   <li>优化过程是幂等的（多次优化结果相同）</li>
 * </ul>
 *
 * @see QueryAnalyzer
 * @see QueryAnalyzeContext#isOptimizationEnabled()
 * @since 1.0.0-beta.2
 */
public class OptimizationAnalyzer implements QueryTransformer {

  @Override
  public boolean supports(QueryAnalyzeContext context) {
    return context.isOptimizationEnabled();
  }

  @Override
  public Try<QueryStatement> transform(QueryStatement statement, QueryAnalyzeContext context) {
    // 执行查询优化
    return Try.of(() -> {
      QueryStatement.QueryStatementBuilder builder = statement.getBuilder();

      // 优化 WHERE 子句
      SExpression<Boolean> where = statement.getWhere();
      if (where != null && !where.isEmpty()) {
        SExpression<Boolean> optimizedWhere = (SExpression<Boolean>) optimizeExpression(where);
        builder.where(optimizedWhere);
      }

      // 优化 HAVING 子句
      SExpression<Boolean> having = statement.getHaving();
      if (having != null && !having.isEmpty()) {
        SExpression<Boolean> optimizedHaving = (SExpression<Boolean>) optimizeExpression(having);
        builder.having(optimizedHaving);
      }

      return builder.build();
    });
  }

  /**
   * 递归优化表达式
   */
  private SExpression<?> optimizeExpression(SExpression<?> expression) {
    if (expression == null || expression.isEmpty()) {
      return expression;
    }
    return expression.transform((node, context) -> applyOptimizationRules(node));
  }

  /**
   * 应用优化规则
   */
  private SExpression<?> applyOptimizationRules(SExpression<?> expression) {
    // 规则1: 双重否定消除 NOT(NOT(x)) → x
    if (expression.getOperator() == Operators.NOT) {
      Object param = expression.getParam(0);
      if (param instanceof SExpression) {
        SExpression<?> inner = (SExpression<?>) param;
        if (inner.getOperator() == Operators.NOT) {
          return (SExpression<?>) inner.getParam(0);
        }
      }
    }

    // 规则2-5: 逻辑操作符优化
    if (expression.getOperator() == Operators.AND) {
      return optimizeAnd(expression);
    }

    if (expression.getOperator() == Operators.OR) {
      return optimizeOr(expression);
    }

    return expression;
  }

  /**
   * 优化 AND 表达式
   *
   * <p>优化规则：
   * <ul>
   *   <li>AND(FALSE, ...) → FALSE（短路）</li>
   *   <li>AND(TRUE, x) → x（消除恒真）</li>
   *   <li>AND(x) → x（单参数）</li>
   *   <li>AND() → TRUE（空参数）</li>
   * </ul>
   */
  private SExpression<?> optimizeAnd(SExpression<?> expression) {
    List<Object> params = expression.getParams();

    // 过滤常量和空表达式
    List<SExpression<?>> filtered = new ArrayList<>();
    for (Object param : params) {
      if (param instanceof SExpression) {
        SExpression<?> expr = (SExpression<?>) param;

        // 跳过空表达式
        if (expr.isEmpty()) {
          continue;
        }

        // 如果遇到 FALSE，直接返回 FALSE（短路）
        if (expr.getOperator() == Operators.CONSTANT) {
          Object value = expr.getParam(0);
          if (Boolean.FALSE.equals(value)) {
            return SExpression.constant(Boolean.FALSE);
          }
          // 跳过 TRUE 常量
          if (Boolean.TRUE.equals(value)) {
            continue;
          }
        }

        filtered.add(expr);
      }
    }

    // 空参数 → TRUE
    if (filtered.isEmpty()) {
      return SExpression.constant(Boolean.TRUE);
    }

    // 单参数 → 直接返回参数
    if (filtered.size() == 1) {
      return filtered.get(0);
    }

    // 多参数 → 返回优化后的 AND
    return SExpression.create(Operators.AND, new ArrayList<>(filtered));
  }

  /**
   * 优化 OR 表达式
   *
   * <p>优化规则：
   * <ul>
   *   <li>OR(TRUE, ...) → TRUE（短路）</li>
   *   <li>OR(FALSE, x) → x（消除恒假）</li>
   *   <li>OR(x) → x（单参数）</li>
   *   <li>OR() → FALSE（空参数）</li>
   * </ul>
   */
  private SExpression<?> optimizeOr(SExpression<?> expression) {
    List<Object> params = expression.getParams();

    // 过滤常量和空表达式
    List<SExpression<?>> filtered = new ArrayList<>();
    for (Object param : params) {
      if (param instanceof SExpression) {
        SExpression<?> expr = (SExpression<?>) param;

        // 跳过空表达式
        if (expr.isEmpty()) {
          continue;
        }

        // 如果遇到 TRUE，直接返回 TRUE（短路）
        if (expr.getOperator() == Operators.CONSTANT) {
          Object value = expr.getParam(0);
          if (Boolean.TRUE.equals(value)) {
            return SExpression.constant(Boolean.TRUE);
          }
          // 跳过 FALSE 常量
          if (Boolean.FALSE.equals(value)) {
            continue;
          }
        }

        filtered.add(expr);
      }
    }

    // 空参数 → FALSE
    if (filtered.isEmpty()) {
      return SExpression.constant(Boolean.FALSE);
    }

    // 单参数 → 直接返回参数
    if (filtered.size() == 1) {
      return filtered.get(0);
    }

    // 多参数 → 返回优化后的 OR
    return SExpression.create(Operators.OR, new ArrayList<>(filtered));
  }
}
