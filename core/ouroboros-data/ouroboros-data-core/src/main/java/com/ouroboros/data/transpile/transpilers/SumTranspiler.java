package com.ouroboros.data.transpile.transpilers;

import io.vavr.control.Try;

import com.ouroboros.data.dsl.Operators;
import com.ouroboros.data.dsl.SExpression;
import com.ouroboros.data.transpile.SExpressionTranspiler;
import com.ouroboros.data.transpile.TranspileContext;
import com.ouroboros.data.exception.TranspileException;
import com.querydsl.core.types.Expression;
import com.querydsl.core.types.dsl.Expressions;
import com.querydsl.core.types.dsl.NumberExpression;

/**
 * SUM 聚合操作符转译器
 * <p>
 * 将 SUM 表达式转换为 QueryDSL 的 sum() 方法调用。
 * <p>
 * <b>输入示例</b>:
 * <pre>
 * SUM(FIELD("amount"))
 * </pre>
 * <p>
 * <b>输出 QueryDSL</b>:
 * <pre>
 * amountPath.sum()  // NumberExpression&lt;?&gt;
 * </pre>
 *
 * @since 1.0.0-beta.2
 */
public class SumTranspiler implements SExpressionTranspiler {

  @Override
  public Try<Expression<?>> apply(SExpression<?> sExpr, TranspileContext context) {
    return Try.of(() -> {
      // 验证参数数量
      if (sExpr.getParams().isEmpty()) {
        throw new TranspileException("SUM 表达式缺少参数");
      }

      if (sExpr.getParams().size() > 1) {
        throw new TranspileException(
            "SUM 表达式只接受 1 个参数，实际: " + sExpr.getParams().size()
        );
      }

      // 获取被聚合的表达式
      Object param = sExpr.getParams().get(0);
      if (!(param instanceof SExpression)) {
        throw new TranspileException(
            "SUM 参数必须是 SExpression，实际: " + param.getClass().getSimpleName()
        );
      }

      SExpression<?> fieldExpr = (SExpression<?>) param;

      // 转译字段表达式
      Expression<?> transpiled = context.transpile(fieldExpr)
          .getOrElseThrow(e -> new TranspileException("无法转译 SUM 参数: " + e.getMessage()));

      // 调用 QueryDSL 的 sum() 方法
      if (transpiled instanceof NumberExpression) {
        return ((NumberExpression<?>) transpiled).sum();
      }

      // 兜底：使用通用的 sum 表达式
      return Expressions.numberOperation(Double.class, Operators.SUM, transpiled);
    });
  }


  @Override
  public Boolean support(SExpression<?> sExpr) {
    return Operators.SUM == sExpr.getOperator();
  }
}
