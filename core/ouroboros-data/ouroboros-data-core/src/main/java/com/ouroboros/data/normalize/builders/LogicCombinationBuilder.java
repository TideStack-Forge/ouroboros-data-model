package com.ouroboros.data.normalize.builders;

import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

import com.ouroboros.data.dsl.Operators;
import com.ouroboros.data.dsl.SExpression;
import com.ouroboros.data.exception.NormalizeException;
import com.ouroboros.data.normalize.ExpressionNormalizeContext;
import com.ouroboros.data.normalize.SExpressionBuilder;
import com.querydsl.core.types.Operator;

public class LogicCombinationBuilder implements SExpressionBuilder {

  @Override
  public boolean supports(Operator operator) {
    return operator == Operators.AND
        || operator == Operators.OR
        || operator == Operators.NOT
        || operator == Operators.XOR
        || operator == Operators.XNOR;
  }

  @Override
  public SExpression<?> build(Operator operator, List<SExpression<?>> params, ExpressionNormalizeContext context) {
    List<SExpression<?>> filtered = filterEmptyExpressions(params);

    if (operator == Operators.NOT) {
      return buildNot(filtered);
    }

    List<SExpression<?>> booleanParams = validateBooleanParams(operator, filtered);
    List<SExpression<?>> flattened = flattenSameOperators(operator, booleanParams);

    if (flattened.isEmpty()) {
      return SExpression.constant(Boolean.TRUE);
    }
    if (flattened.size() == 1) {
      return flattened.get(0);
    }

    Boolean folded = foldConstants(operator, flattened);
    if (folded != null) {
      return SExpression.constant(folded);
    }

    return SExpression.create(operator, flattened);
  }

  private List<SExpression<?>> filterEmptyExpressions(List<SExpression<?>> params) {
    return params.stream()
        .filter(param -> param != null && !param.isEmpty())
        .collect(Collectors.toList());
  }

  private SExpression<?> buildNot(List<SExpression<?>> params) {
    List<SExpression<?>> booleanParams = validateBooleanParams(Operators.NOT, params);
    if (booleanParams.size() != 1) {
      throw new NormalizeException("NOT 操作符需要1个布尔参数，得到 " + booleanParams.size() + " 个");
    }
    return SExpression.create(Operators.NOT, booleanParams.get(0));
  }

  private List<SExpression<?>> validateBooleanParams(Operator operator, List<SExpression<?>> params) {
    List<SExpression<?>> booleanParams = new ArrayList<>();
    for (SExpression<?> param : params) {
      if (param == null) {
        throw new NormalizeException(operator + " 操作符参数不能为空");
      }
      if (!isBooleanExpression(param)) {
        throw new NormalizeException(operator + " 操作符只接受布尔表达式参数，实际得到: " + param.getDataType());
      }
      booleanParams.add(param);
    }
    return booleanParams;
  }

  private boolean isBooleanExpression(SExpression<?> expr) {
    Class<?> dataType = expr.getDataType();
    return dataType != null && Boolean.class.isAssignableFrom(dataType);
  }

  private List<SExpression<?>> flattenSameOperators(Operator operator, List<SExpression<?>> params) {
    List<SExpression<?>> result = new ArrayList<>();
    for (SExpression<?> expr : params) {
      if (expr.getOperator() == operator) {
        expr.getParams().forEach(p -> result.add((SExpression<?>) p));
      } else {
        result.add(expr);
      }
    }
    return result;
  }

  private Boolean foldConstants(Operator operator, List<SExpression<?>> params) {
    List<Boolean> constants = params.stream()
        .filter(e -> e.getOperator() == Operators.CONSTANT)
        .map(e -> (Boolean) e.getParam(0))
        .collect(Collectors.toList());

    if (operator == Operators.OR) {
      if (constants.contains(Boolean.TRUE)) {
        return true;
      }
      return null;
    }
    if (operator == Operators.AND) {
      if (constants.contains(Boolean.FALSE)) {
        return false;
      }
      return null;
    }
    return null;
  }
}
