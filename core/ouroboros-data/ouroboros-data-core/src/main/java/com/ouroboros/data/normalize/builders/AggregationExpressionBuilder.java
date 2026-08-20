package com.ouroboros.data.normalize.builders;

import java.util.Collections;
import java.util.List;
import java.util.Set;

import com.ouroboros.data.dsl.Operators;
import com.ouroboros.data.dsl.SExpression;
import com.ouroboros.data.exception.NormalizeException;
import com.ouroboros.data.normalize.ExpressionNormalizeContext;
import com.ouroboros.data.normalize.SExpressionBuilder;
import com.querydsl.core.types.Operator;

/**
 * 聚合操作符表达式构建器
 * <p>
 * 处理: COUNT（SUM/AVG/MAX/MIN 计划在 v1.3 支持）
 */
public class AggregationExpressionBuilder implements SExpressionBuilder {

  private static final Set<Operator> SUPPORTED = Collections.singleton(Operators.COUNT);

  @Override
  public boolean supports(Operator operator) {
    return SUPPORTED.contains(operator);
  }

  @Override
  public SExpression<?> build(Operator operator, List<SExpression<?>> params, ExpressionNormalizeContext context) {
    if (operator != Operators.COUNT) {
      throw new NormalizeException("聚合操作符 " + operator + " 计划在 v1.3 支持（当前版本仅支持 COUNT）");
    }
    if (params.size() != 1) {
      throw new NormalizeException("COUNT 操作符需要1个参数，得到 " + params.size() + " 个");
    }
    return SExpression.create(operator, params);
  }
}
