package com.ouroboros.data.dsl;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.function.Function;

import com.ouroboros.data.dsl.statement.QueryStatement;

public final class RawExpressions {

  private RawExpressions() {}

  public static Object rewrite(Object raw, Function<RawExpressionPath, Object> mapper) {
    return rewrite(raw, new RawExpressionPath(List.of(), raw), mapper);
  }

  private static Object rewrite(Object raw, RawExpressionPath path, Function<RawExpressionPath, Object> mapper) {
    Object rewritten = rewriteChildren(raw, path, mapper);
    return mapper.apply(new RawExpressionPath(path.segments(), rewritten));
  }

  private static Object rewriteChildren(Object raw, RawExpressionPath path,
                                        Function<RawExpressionPath, Object> mapper) {
    if (raw instanceof SExpression<?> expression) {
      return rewriteExpression(expression, path, mapper);
    }
    if (raw instanceof QueryStatement statement) {
      return rewriteStatement(statement, path, mapper);
    }
    if (raw instanceof Map<?, ?> map) {
      Map<Object, Object> rewritten = new LinkedHashMap<>();
      for (Map.Entry<?, ?> entry : map.entrySet()) {
        rewritten.put(entry.getKey(), rewrite(entry.getValue(), path.append(entry.getKey(), entry.getValue()), mapper));
      }
      return rewritten;
    }
    if (raw instanceof List<?> list) {
      List<Object> rewritten = new ArrayList<>(list.size());
      for (int i = 0; i < list.size(); i++) {
        Object item = list.get(i);
        rewritten.add(rewrite(item, path.append(i, item), mapper));
      }
      return rewritten;
    }
    return raw;
  }

  private static SExpression<?> rewriteExpression(SExpression<?> expression, RawExpressionPath path,
                                                  Function<RawExpressionPath, Object> mapper) {
    if (expression.isEmpty()) {
      return expression;
    }
    List<Object> params = new ArrayList<>(expression.getParams().size());
    for (int i = 0; i < expression.getParams().size(); i++) {
      Object param = expression.getParam(i);
      params.add(rewrite(param, path.append("params").append(i, param), mapper));
    }
    return SExpression.create(expression.getOperator(), params);
  }

  private static QueryStatement rewriteStatement(QueryStatement statement, RawExpressionPath path,
                                                 Function<RawExpressionPath, Object> mapper) {
    Map<String, Object> rewritten = new LinkedHashMap<>();
    for (Map.Entry<String, Object> entry : statement.entrySet()) {
      rewritten.put(entry.getKey(), rewrite(entry.getValue(), path.append(entry.getKey(), entry.getValue()), mapper));
    }
    return new QueryStatement(rewritten);
  }
}
