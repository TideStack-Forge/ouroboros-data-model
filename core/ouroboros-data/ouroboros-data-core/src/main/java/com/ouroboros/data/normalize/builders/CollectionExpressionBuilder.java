package com.ouroboros.data.normalize.builders;

import java.util.Arrays;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import com.ouroboros.data.dsl.Operators;
import com.ouroboros.data.dsl.SExpression;
import com.ouroboros.data.exception.NormalizeException;
import com.ouroboros.data.normalize.ExpressionNormalizeContext;
import com.ouroboros.data.normalize.SExpressionBuilder;
import com.querydsl.core.types.Operator;

/**
 * 集合操作符表达式构建器
 * <p>
 * 处理: IN, NOT_IN
 */
public class CollectionExpressionBuilder implements SExpressionBuilder {

  private static final Set<Operator> SUPPORTED = new HashSet<>(Arrays.asList(Operators.IN, Operators.NOT_IN));

  @Override
  public boolean supports(Operator operator) {
    return SUPPORTED.contains(operator);
  }

  @Override
  public SExpression<?> build(Operator operator, List<SExpression<?>> params, ExpressionNormalizeContext context) {
    if (params.size() != 2) {
      throw new NormalizeException(operator + " 操作符需要2个参数，得到 " + params.size() + " 个");
    }

    SExpression<?> right = params.get(1);
    if (right.getOperator() != Operators.CONSTANT) {
      throw new NormalizeException(operator + " 操作符的右值必须是列表常量表达式");
    }

    Object rightValue = right.getParam(0);
    if (!(rightValue instanceof List<?>)) {
      throw new NormalizeException(operator + " 操作符的右值必须是列表常量");
    }

    return SExpression.create(operator, params);
  }
}
