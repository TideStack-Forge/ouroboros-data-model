package com.ouroboros.data.normalize.builders;

import java.util.Arrays;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import com.ouroboros.data.dsl.ExtOps;
import com.ouroboros.data.dsl.Operators;
import com.ouroboros.data.dsl.SExpression;
import com.ouroboros.data.exception.NormalizeException;
import com.ouroboros.data.normalize.ExpressionNormalizeContext;
import com.ouroboros.data.normalize.SExpressionBuilder;
import com.querydsl.core.types.Operator;

/**
 * 关联操作符表达式构建器
 * <p>
 * 处理: REL_ANY, REL_ALL, REL_NONE
 */
public class RelationExpressionBuilder implements SExpressionBuilder {

  private static final Set<Operator> SUPPORTED = new HashSet<>(Arrays.asList(
      ExtOps.REL_ANY, ExtOps.REL_ALL, ExtOps.REL_NONE
  ));

  @Override
  public boolean supports(Operator operator) {
    return SUPPORTED.contains(operator);
  }

  @Override
  public SExpression<?> build(Operator operator, List<SExpression<?>> params, ExpressionNormalizeContext context) {
    if (params.size() != 2) {
      throw new NormalizeException(operator + " 操作符需要2个参数，得到 " + params.size() + " 个");
    }

    SExpression<?> fieldExpr = params.get(0);
    if (fieldExpr.getOperator() != Operators.FIELD) {
      throw new NormalizeException(operator + " 操作符的第一个参数必须是 FIELD 表达式");
    }

    SExpression<?> conditionExpr = params.get(1);
    if (!Boolean.class.isAssignableFrom(conditionExpr.getDataType())) {
      throw new NormalizeException(operator + " 操作符的第二个参数必须是布尔表达式");
    }

    return SExpression.create(operator, fieldExpr, conditionExpr);
  }
}
