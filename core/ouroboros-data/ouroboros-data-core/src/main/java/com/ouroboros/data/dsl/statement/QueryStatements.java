package com.ouroboros.data.dsl.statement;

import java.util.ArrayList;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.function.Function;

import com.ouroboros.data.dsl.Keyword;
import com.ouroboros.data.dsl.SExpression;

public final class QueryStatements {

  private QueryStatements() {}

  public static QueryStatement rewriteExpressions(QueryStatement statement,
                                                  Function<StatementExpressionPath, SExpression<?>> mapper) {
    Objects.requireNonNull(statement, "statement must not be null");
    Objects.requireNonNull(mapper, "mapper must not be null");
    return rewriteStatement(statement, mapper, List.of());
  }

  private static QueryStatement rewriteStatement(QueryStatement statement,
                                                 Function<StatementExpressionPath, SExpression<?>> mapper,
                                                 List<Object> path) {
    QueryStatement.QueryStatementBuilder builder = statement.getBuilder();
    rewriteFrom(statement, builder, mapper, path);
    rewriteSelect(statement, builder, mapper, path);
    rewriteWhere(statement, builder, mapper, path);
    rewriteGroup(statement, builder, mapper, path);
    rewriteHaving(statement, builder, mapper, path);
    rewriteJoins(statement, builder, mapper, path);
    rewriteWith(statement, builder, mapper, path);
    rewriteUnions(statement, builder, mapper, path);
    rewriteModelClauses(statement, builder, mapper, path);
    return builder.build();
  }

  private static void rewriteFrom(QueryStatement statement, QueryStatement.QueryStatementBuilder builder,
                                  Function<StatementExpressionPath, SExpression<?>> mapper, List<Object> path) {
    QueryStatement.TableSource from = statement.getFrom();
    if (from == null || !from.isSubQuery()) {
      return;
    }
    QueryStatement subQuery = rewriteStatement(from.getSubQuery(), mapper, append(path, Keyword.FROM.toString()));
    builder.from(new QueryStatement.TableSource(subQuery, from.getAlias()));
  }

  private static void rewriteSelect(QueryStatement statement, QueryStatement.QueryStatementBuilder builder,
                                    Function<StatementExpressionPath, SExpression<?>> mapper, List<Object> path) {
    if (!statement.containsKey(Keyword.SELECT.toString())) {
      return;
    }
    List<SExpression<?>> rewritten = new ArrayList<>();
    List<SExpression<?>> select = statement.getSelect();
    for (int i = 0; i < select.size(); i++) {
      rewritten.add(rewriteExpression(statement, select.get(i), mapper,
          append(path, Keyword.SELECT.toString(), i)));
    }
    builder.replaceSelect(rewritten);
  }

  private static void rewriteWhere(QueryStatement statement, QueryStatement.QueryStatementBuilder builder,
                                   Function<StatementExpressionPath, SExpression<?>> mapper, List<Object> path) {
    if (statement.containsKey(Keyword.WHERE.toString())) {
      builder.where(asBoolean(rewriteExpression(statement, statement.getWhere(), mapper,
          append(path, Keyword.WHERE.toString()))));
    }
  }

  private static void rewriteGroup(QueryStatement statement, QueryStatement.QueryStatementBuilder builder,
                                   Function<StatementExpressionPath, SExpression<?>> mapper, List<Object> path) {
    if (statement.containsKey(Keyword.GROUP.toString())) {
      builder.group(rewriteExpression(statement, statement.getGroup(), mapper,
          append(path, Keyword.GROUP.toString())));
    }
  }

  private static void rewriteHaving(QueryStatement statement, QueryStatement.QueryStatementBuilder builder,
                                    Function<StatementExpressionPath, SExpression<?>> mapper, List<Object> path) {
    if (statement.containsKey(Keyword.HAVING.toString())) {
      builder.having(asBoolean(rewriteExpression(statement, statement.getHaving(), mapper,
          append(path, Keyword.HAVING.toString()))));
    }
  }

  private static void rewriteJoins(QueryStatement statement, QueryStatement.QueryStatementBuilder builder,
                                   Function<StatementExpressionPath, SExpression<?>> mapper, List<Object> path) {
    if (!statement.containsKey(Keyword.JOIN.toString())) {
      return;
    }
    List<QueryStatement.JoinEntry> joins = statement.getJoins();
    List<QueryStatement.JoinEntry> rewritten = new ArrayList<>(joins.size());
    for (int i = 0; i < joins.size(); i++) {
      QueryStatement.JoinEntry join = joins.get(i);
      List<Object> joinPath = append(path, Keyword.JOIN.toString(), i);
      QueryStatement subQuery = join.isSubQuery()
          ? rewriteStatement(join.getSubQuery(), mapper, append(joinPath, Keyword.FROM.toString()))
          : null;
      SExpression<Boolean> on = asBoolean(rewriteExpression(statement, join.getOn(), mapper,
          append(joinPath, "ON")));
      rewritten.add(join.isSubQuery()
          ? new QueryStatement.JoinEntry(join.getType(), subQuery, join.getAlias(), on)
          : new QueryStatement.JoinEntry(join.getType(), join.getTableName(), join.getAlias(), on));
    }
    builder.replaceJoins(rewritten);
  }

  private static void rewriteWith(QueryStatement statement, QueryStatement.QueryStatementBuilder builder,
                                  Function<StatementExpressionPath, SExpression<?>> mapper, List<Object> path) {
    if (!statement.containsKey(Keyword.WITH.toString())) {
      return;
    }
    List<QueryStatement.CTEDefinition> ctes = statement.getWith();
    List<QueryStatement.CTEDefinition> rewritten = new ArrayList<>(ctes.size());
    for (int i = 0; i < ctes.size(); i++) {
      QueryStatement.CTEDefinition cte = ctes.get(i);
      rewritten.add(new QueryStatement.CTEDefinition(
          rewriteStatement(cte.getQuery(), mapper, append(path, Keyword.WITH.toString(), i)),
          cte.getAlias(),
          cte.isRecursive()));
    }
    builder.metaMap.put(Keyword.WITH.toString(), rewritten);
  }

  private static void rewriteUnions(QueryStatement statement, QueryStatement.QueryStatementBuilder builder,
                                    Function<StatementExpressionPath, SExpression<?>> mapper, List<Object> path) {
    if (!statement.containsKey(Keyword.UNION.toString())) {
      return;
    }
    List<QueryStatement.UnionEntry> unions = statement.getUnions();
    List<QueryStatement.UnionEntry> rewritten = new ArrayList<>(unions.size());
    for (int i = 0; i < unions.size(); i++) {
      QueryStatement.UnionEntry union = unions.get(i);
      rewritten.add(new QueryStatement.UnionEntry(
          rewriteStatement(union.getQuery(), mapper, append(path, Keyword.UNION.toString(), i)),
          union.isAll()));
    }
    builder.metaMap.put(Keyword.UNION.toString(), rewritten);
  }

  private static void rewriteModelClauses(QueryStatement statement, QueryStatement.QueryStatementBuilder builder,
                                          Function<StatementExpressionPath, SExpression<?>> mapper,
                                          List<Object> path) {
    if (statement.containsKey(Keyword.POPULATE.toString())) {
      Object rawPopulate = rewriteRawValue(statement.get(Keyword.POPULATE.toString()), statement, mapper,
          append(path, Keyword.POPULATE.toString()));
      builder.metaMap.put(Keyword.POPULATE.toString(), rawPopulate);
    }
    if (statement.containsKey(Keyword.OMIT.toString())) {
      Object rawOmit = rewriteRawValue(statement.get(Keyword.OMIT.toString()), statement, mapper,
          append(path, Keyword.OMIT.toString()));
      builder.metaMap.put(Keyword.OMIT.toString(), rawOmit);
    }
    if (statement instanceof ModelQueryStatement modelStatement
        && builder instanceof ModelQueryStatementBuilder modelBuilder
        && modelStatement.getPopulateClause() != null) {
      modelBuilder.populateClause(rewritePopulateClause(modelStatement, mapper,
          append(path, Keyword.POPULATE.toString())));
    }
  }

  private static PopulateClause rewritePopulateClause(ModelQueryStatement statement,
                                                      Function<StatementExpressionPath, SExpression<?>> mapper,
                                                      List<Object> path) {
    List<Object> rawEntries = new ArrayList<>();
    List<PopulateClause.PopulateEntry> entries = statement.getPopulateClause().getEntries();
    for (int i = 0; i < entries.size(); i++) {
      PopulateClause.PopulateEntry entry = entries.get(i);
      if (entry.options() == null) {
        rawEntries.add(entry.fieldName());
      } else {
        rawEntries.add(Collections.singletonMap(entry.fieldName(),
            rewriteRawValue(entry.options(), statement, mapper, append(path, i, "options"))));
      }
    }
    return PopulateClause.fromRaw(rawEntries);
  }

  private static Object rewriteRawValue(Object value, QueryStatement root,
                                        Function<StatementExpressionPath, SExpression<?>> mapper,
                                        List<Object> path) {
    if (value instanceof SExpression<?> expression) {
      return rewriteExpression(root, expression, mapper, path);
    }
    if (value instanceof QueryStatement statement) {
      return rewriteStatement(statement, mapper, path);
    }
    if (value instanceof Map<?, ?> map) {
      Map<Object, Object> rewritten = new LinkedHashMap<>();
      for (Map.Entry<?, ?> entry : map.entrySet()) {
        rewritten.put(entry.getKey(), rewriteRawValue(entry.getValue(), root, mapper, append(path, entry.getKey())));
      }
      return rewritten;
    }
    if (value instanceof List<?> list) {
      List<Object> rewritten = new ArrayList<>(list.size());
      for (int i = 0; i < list.size(); i++) {
        rewritten.add(rewriteRawValue(list.get(i), root, mapper, append(path, i)));
      }
      return rewritten;
    }
    return value;
  }

  private static SExpression<?> rewriteExpression(QueryStatement statement, SExpression<?> expression,
                                                  Function<StatementExpressionPath, SExpression<?>> mapper,
                                                  List<Object> path) {
    if (expression == null || expression.isEmpty()) {
      return expression;
    }
    List<Object> params = new ArrayList<>(expression.getParams().size());
    for (int i = 0; i < expression.getParams().size(); i++) {
      Object param = expression.getParam(i);
      if (param instanceof SExpression<?> nested) {
        params.add(rewriteExpression(statement, nested, mapper, append(path, "params", i)));
      } else if (param instanceof QueryStatement nestedStatement) {
        params.add(rewriteStatement(nestedStatement, mapper, append(path, "params", i)));
      } else {
        params.add(param);
      }
    }
    SExpression<?> rewritten = SExpression.create(expression.getOperator(), params);
    return mapper.apply(new StatementExpressionPath(statement, path, rewritten));
  }

  private static SExpression<Boolean> asBoolean(SExpression<?> expression) {
    return (SExpression<Boolean>) expression;
  }

  private static List<Object> append(List<Object> path, Object... segments) {
    List<Object> next = new ArrayList<>(path);
    Collections.addAll(next, segments);
    return next;
  }
}
