package com.ouroboros.data.transpile.transpilers;

import io.vavr.control.Try;

import com.ouroboros.data.dsl.ExtOps;
import com.ouroboros.data.dsl.Operators;
import com.ouroboros.data.dsl.SExpression;
import com.ouroboros.data.transpile.SExpressionTranspiler;
import com.ouroboros.data.transpile.TranspileContext;
import com.ouroboros.data.exception.TranspileException;
import com.querydsl.core.types.Expression;
import com.querydsl.core.types.dsl.Expressions;
import com.querydsl.core.types.dsl.SimpleExpression;

/**
 * COUNT 聚合操作符转译器
 * <p>
 * 将 COUNT 表达式转换为 QueryDSL 的 count() 方法调用。
 * <p>
 * <b>输入示例</b>:
 * <pre>
 * COUNT(FIELD("orderItems"))
 * COUNT(COLUMNS)  // COUNT(*)
 * </pre>
 * <p>
 * <b>输出 QueryDSL</b>:
 * <pre>
 * orderItemsPath.count()  // NumberExpression&lt;Long&gt;
 * Expressions.ONE.count() // COUNT(*)
 * </pre>
 * <p>
 * <b>使用场景</b>:
 * <ul>
 *   <li>统计记录数量</li>
 *   <li>聚合查询中的 COUNT 函数</li>
 *   <li>与 ALIAS 组合使用：ALIAS(COUNT(...), "total")</li>
 * </ul>
 *
 * @since 1.0.0-beta.2
 */
public class CountTranspiler implements SExpressionTranspiler {

  @Override
  public Try<Expression<?>> apply(SExpression<?> sExpr, TranspileContext context) {
    return Try.of(() -> {
      // 验证参数数量
      if (sExpr.getParams().isEmpty()) {
        throw new TranspileException("COUNT 表达式缺少参数");
      }

      if (sExpr.getParams().size() > 1) {
        throw new TranspileException(
            "COUNT 表达式只接受 1 个参数，实际: " + sExpr.getParams().size()
        );
      }

      // 获取被聚合的表达式
      Object param = sExpr.getParams().get(0);
      if (!(param instanceof SExpression)) {
        throw new TranspileException(
            "COUNT 参数必须是 SExpression，实际: " + param.getClass().getSimpleName()
        );
      }

      SExpression<?> fieldExpr = (SExpression<?>) param;

      // 处理 COUNT(*) 场景
      if (fieldExpr.getOperator() == ExtOps.COLUMNS || isFieldStar(fieldExpr)) {
        // COUNT(*) -> 使用常量 1 进行计数
        return Expressions.ONE.count();
      }

      // 转译字段表达式
      Expression<?> transpiled = context.transpile(fieldExpr)
          .getOrElseThrow(e -> new TranspileException("无法转译 COUNT 参数: " + e.getMessage()));

      // 调用 QueryDSL 的 count() 方法
      // NumberExpression, StringExpression 等都有 count() 方法
      if (transpiled instanceof SimpleExpression) {
        return ((SimpleExpression<?>) transpiled).count();
      }

      // 兜底：使用通用的 count 表达式
      return Expressions.numberOperation(Long.class, Operators.COUNT, transpiled);
    });
  }


  @Override
  public Boolean support(SExpression<?> sExpr) {
    return Operators.COUNT == sExpr.getOperator();
  }

  private boolean isFieldStar(SExpression<?> fieldExpr) {
    return fieldExpr.getOperator() == ExtOps.FIELD
        && fieldExpr.getParams().size() == 1
        && "*".equals(fieldExpr.getParam(0));
  }
}
