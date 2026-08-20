package com.ouroboros.data.transpile;

import static io.vavr.API.Try;

import io.vavr.control.Try;

import com.ouroboros.data.dsl.SExpression;
import com.ouroboros.data.exception.TranspileException;
import com.ouroboros.data.util.DataServices;
import com.querydsl.core.types.Expression;
import com.querydsl.core.types.Predicate;

/**
 * SExpression -> QueryDSL Expression 转译器
 */
public interface SExpressionTranspiler {

  /**
   * 使用 TranspileContext 转译表达式
   *
   * @param sExpr   S表达式
   * @param context Transpile 上下文
   * @return Expression
   * @since 1.0.0-beta.2
   */
  static Try<Expression<?>> transpile(SExpression<?> sExpr, TranspileContext context) {
    var transpiler = DataServices.getCachedReversedServiceStream(SExpressionTranspiler.class)
        .filter(t -> t.support(sExpr))
        .findFirst()
        .orElseGet(() -> (s, c) -> Try.failure(new TranspileException("不支持的操作符：" + s.getOperator())));
    return transpiler.apply(sExpr, context);
  }

  /**
   * 使用 TranspileContext 转译谓词表达式
   *
   * @param sExpr   S表达式
   * @param context Transpile 上下文
   * @return Predicate
   * @since 1.0.0-beta.2
   */
  static Try<Predicate> transpilePredicate(SExpression<?> sExpr, TranspileContext context) {
    return Try(() -> {
      if (!sExpr.getDataType().isAssignableFrom(Boolean.class)) {
        throw new TranspileException("必须是布尔表达式");
      }
      var expr = transpile(sExpr, context).getOrElseThrow(e -> e);
      if (!(expr instanceof Predicate predicate)) {
        throw new IllegalArgumentException("转译结果不是谓词表达式");
      }
      return predicate;
    });
  }

  /**
   * 将 SExpression 转译为 QueryDSL 的 Expression
   *
   * <p>使用 TranspileContext 提供完整的上下文信息。
   *
   * @param sExpr   S表达式
   * @param context Transpile 上下文
   * @return Expression
   * @since 1.0.0-beta.2
   */
  Try<Expression<?>> apply(SExpression<?> sExpr, TranspileContext context);

  /**
   * 返回转译器对应的操作符，通过SPI注册转译器时需要覆盖此方法<br>
   * <i>用于自动注册转译器，通过转译器工厂返回的转译器不需要覆盖</i>
   *
   * @return 操作符
   */
  default Boolean support(SExpression<?> sExpr) {
    return true;
  }
}
