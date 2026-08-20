package com.ouroboros.data.normalize.builders;

import java.util.List;

import com.ouroboros.data.dsl.Operators;
import com.ouroboros.data.dsl.SExpression;
import com.ouroboros.data.exception.NormalizeException;
import com.ouroboros.data.normalize.ExpressionNormalizeContext;
import com.ouroboros.data.normalize.SExpressionBuilder;
import com.querydsl.core.types.Operator;

public class ConstantExpressionBuilder implements SExpressionBuilder {

  @Override
  public boolean supports(Operator operator) {
    return operator == Operators.CONSTANT;
  }

  @Override
  public SExpression<?> build(Operator operator, List<SExpression<?>> params, ExpressionNormalizeContext context) {
    if (params.size() != 1) {
      throw new NormalizeException("CONSTANT 表达式需要恰好1个参数，得到 " + params.size() + " 个");
    }

    SExpression<?> param = params.get(0);
    if (param.getOperator() != Operators.CONSTANT) {
      throw new NormalizeException("CONSTANT 表达式参数必须是常量表达式");
    }

    return param;
  }
}
