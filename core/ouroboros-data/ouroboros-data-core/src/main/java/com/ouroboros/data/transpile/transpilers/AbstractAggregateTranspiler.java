package com.ouroboros.data.transpile.transpilers;

import io.vavr.control.Try;

import com.ouroboros.data.dsl.SExpression;
import com.ouroboros.data.transpile.SExpressionTranspiler;
import com.ouroboros.data.transpile.TranspileContext;
import com.ouroboros.data.exception.TranspileException;
import com.querydsl.core.types.Expression;

/**
 * 聚合操作符转译器抽象基类
 * <p>
 * 使用模板方法模式提取 AVG/MIN/MAX 等聚合转译器的公共逻辑。
 * 子类只需实现特定的操作符名称、类型检查和表达式生成逻辑。
 *
 * @since 1.0.0-beta.2
 */
public abstract class AbstractAggregateTranspiler implements SExpressionTranspiler {

  @Override
  public final Try<Expression<?>> apply(SExpression<?> sExpr, TranspileContext context) {
    return Try.of(() -> {
      // 1. 验证参数：不能为空
      if (sExpr.getParams().isEmpty()) {
        throw new TranspileException(getOperatorName() + " 表达式缺少参数");
      }

      // 2. 验证参数：只接受一个参数
      if (sExpr.getParams().size() > 1) {
        throw new TranspileException(
            getOperatorName() + " 表达式只接受 1 个参数，实际: " + sExpr.getParams().size()
        );
      }

      // 3. 验证参数类型：必须是 SExpression
      Object param = sExpr.getParams().get(0);
      if (!(param instanceof SExpression)) {
        throw new TranspileException(
            getOperatorName() + " 参数必须是 SExpression，实际: " + param.getClass().getSimpleName()
        );
      }

      SExpression<?> fieldExpr = (SExpression<?>) param;

      // 4. 转译参数表达式
      Expression<?> transpiled = context.transpile(fieldExpr)
          .getOrElseThrow(e -> new TranspileException(
              "无法转译 " + getOperatorName() + " 参数: " + e.getMessage()
          ));

      // 5. 尝试使用特定类型的表达式方法（如 NumberExpression.avg()）
      Expression<?> specific = trySpecificExpression(transpiled);
      if (specific != null) {
        return specific;
      }

      // 6. 兜底：使用通用的聚合表达式
      return getFallbackExpression(transpiled);
    });
  }

  @Override
  public final Boolean support(SExpression<?> sExpr) {
    return getOperator() == sExpr.getOperator();
  }

  /**
   * 获取操作符名称（用于错误消息）
   *
   * @return 操作符名称，如 "AVG"、"MIN"、"MAX"
   */
  protected abstract String getOperatorName();

  /**
   * 获取操作符常量（用于 support 方法）
   *
   * @return 操作符常量，如 Operators.AVG
   */
  protected abstract com.querydsl.core.types.Operator getOperator();

  /**
   * 尝试使用特定类型的表达式方法
   * <p>
   * 例如：NumberExpression.avg()、ComparableExpressionBase.min()
   *
   * @param transpiled 已转译的表达式
   * @return 特定类型的聚合表达式，如果不适用则返回 null
   */
  protected abstract Expression<?> trySpecificExpression(Expression<?> transpiled);

  /**
   * 获取兜底的通用聚合表达式
   * <p>
   * 例如：Expressions.numberOperation(Double.class, Operators.AVG, transpiled)
   *
   * @param transpiled 已转译的表达式
   * @return 通用聚合表达式
   */
  protected abstract Expression<?> getFallbackExpression(Expression<?> transpiled);
}
