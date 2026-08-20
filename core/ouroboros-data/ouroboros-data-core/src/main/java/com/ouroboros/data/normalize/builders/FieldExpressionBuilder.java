package com.ouroboros.data.normalize.builders;

import java.util.ArrayList;
import java.util.List;

import com.ouroboros.data.dsl.Operators;
import com.ouroboros.data.dsl.SExpression;
import com.ouroboros.data.exception.NormalizeException;
import com.ouroboros.data.normalize.ExpressionNormalizeContext;
import com.ouroboros.data.normalize.SExpressionBuilder;
import com.querydsl.core.types.Operator;

public class FieldExpressionBuilder implements SExpressionBuilder {

  @Override
  public boolean supports(Operator operator) {
    return operator == Operators.FIELD;
  }

  @Override
  public SExpression<?> build(Operator operator, List<SExpression<?>> params, ExpressionNormalizeContext context) {
    if (params.isEmpty()) {
      throw new NormalizeException("FIELD 表达式至少需要1个参数");
    }

    List<Object> fieldPath = new ArrayList<>();
    for (SExpression<?> param : params) {
      if (param.getOperator() != Operators.CONSTANT) {
        throw new NormalizeException("FIELD 表达式参数必须是字符串常量");
      }
      Object value = param.getParam(0);
      if (!(value instanceof CharSequence) || value.toString().isEmpty()) {
        throw new NormalizeException("FIELD 表达式参数必须是非空字符串常量");
      }
      fieldPath.add(value.toString());
    }

    return SExpression.create(operator, fieldPath.toArray());
  }
}
