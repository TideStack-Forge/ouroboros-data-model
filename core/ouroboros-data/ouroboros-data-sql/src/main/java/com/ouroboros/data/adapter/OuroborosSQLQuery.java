package com.ouroboros.data.adapter;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import com.ouroboros.data.transpile.OuroborosQueryMetadata;
import com.ouroboros.data.transpile.DefaultOuroborosQueryMetadata;
import com.querydsl.core.JoinExpression;
import com.querydsl.core.DefaultQueryMetadata;
import com.querydsl.core.JoinType;
import com.querydsl.core.types.OrderSpecifier;
import com.querydsl.core.QueryFlag;
import com.querydsl.core.QueryMetadata;
import com.querydsl.core.types.Expression;
import com.querydsl.core.types.ExpressionUtils;
import com.querydsl.core.types.dsl.Wildcard;
import com.querydsl.core.types.ParamExpression;
import com.querydsl.core.types.SubQueryExpression;
import com.querydsl.core.types.dsl.Expressions;
import com.querydsl.sql.SQLOps;
import com.querydsl.sql.SQLQuery;
import com.querydsl.sql.SQLSerializer;
import com.querydsl.sql.SQLTemplates;

public class OuroborosSQLQuery extends SQLQuery<Object> {

  private final SQLTemplates templates;

  public OuroborosSQLQuery(SQLTemplates templates, QueryMetadata metadata) {
    super(null, templates, metadata);
    this.templates = templates;
    preprocessMetadata(metadata);
  }

  public OuroborosSQLQuery(SQLTemplates templates) {
    super(templates);
    this.templates = templates;
  }

  private void preprocessMetadata(QueryMetadata metadata) {
    if (metadata instanceof OuroborosQueryMetadata ouroborosMetadata) {
      preprocessUnionMetadata(ouroborosMetadata);
      preprocessWith(ouroborosMetadata);
    }
  }

  private void preprocessWith(OuroborosQueryMetadata metadata) {
    var withs = metadata.getWiths();
    var isWithRecursive = metadata.isWithRecursive();
    withs.forEach(with -> {
      var path = with._1();
      var queryMetadata = with._2();
      var subQuery = new OuroborosSQLQuery(templates, queryMetadata);
      if (isWithRecursive) {
        withRecursive(path, subQuery);
      } else {
        with(path, subQuery);
      }
    });
  }

  private void preprocessUnionMetadata(OuroborosQueryMetadata metadata) {
    List<SubQueryExpression<?>> subQueries = new ArrayList<>();

    if (!metadata.getUnions().isEmpty() || !metadata.getUnionAlls().isEmpty()) {
      subQueries.add(new OuroborosSQLQuery(templates, createMainUnionSubQueryMetadata(metadata)));
    }

    for (QueryMetadata union : metadata.getUnions()) {
      subQueries.add(new OuroborosSQLQuery(templates, union));
    }

    for (QueryMetadata unionAll : metadata.getUnionAlls()) {
      subQueries.add(new OuroborosSQLQuery(templates, unionAll));
    }

    if (subQueries.size() < 2) {
      return;
    }

    Expression<?> unionExpression = subQueries.get(0);
    for (int i = 1; i < subQueries.size(); i++) {
      var operator = i <= metadata.getUnions().size() ? SQLOps.UNION : SQLOps.UNION_ALL;
      unionExpression = Expressions.operation(
          subQueries.get(0).getType(),
          operator,
          unionExpression,
          subQueries.get(i)
      );
    }

    setUnionState(unionExpression, subQueries.get(0));
  }

  private QueryMetadata createMainUnionSubQueryMetadata(QueryMetadata source) {
    DefaultOuroborosQueryMetadata copy = copyMetadata(source);
    copy.clearOrderBy();
    copy.setModifiers(com.querydsl.core.QueryModifiers.EMPTY);
    copy.getUnions().clear();
    copy.getUnionAlls().clear();
    copy.getWiths().clear();
    return copy;
  }

  private DefaultOuroborosQueryMetadata copyMetadata(QueryMetadata source) {
    DefaultOuroborosQueryMetadata copy = new DefaultOuroborosQueryMetadata();
    copy.setDistinct(source.isDistinct());
    copy.setUnique(source.isUnique());
    copy.setProjection(source.getProjection());
    copy.setModifiers(source.getModifiers());

    if (source.getWhere() != null) {
      copy.addWhere(source.getWhere());
    }
    if (source.getHaving() != null) {
      copy.addHaving(source.getHaving());
    }

    for (var group : source.getGroupBy()) {
      copy.addGroupBy(group);
    }
    for (var order : source.getOrderBy()) {
      copy.addOrderBy(order);
    }
    for (JoinExpression join : source.getJoins()) {
      copy.addJoin(join.getType(), join.getTarget());
      if (join.getCondition() != null) {
        copy.addJoinCondition(join.getCondition());
      }
      for (var flag : join.getFlags()) {
        copy.addJoinFlag(flag);
      }
    }
    for (QueryFlag flag : source.getFlags()) {
      copy.addFlag(flag);
    }
    copyParams(source.getParams(), copy);

    if (source instanceof OuroborosQueryMetadata ouroborosSource) {
      ouroborosSource.getUnions().forEach(copy::addUnion);
      ouroborosSource.getUnionAlls().forEach(copy::addUnionAll);
      ouroborosSource.getWiths().forEach(with -> {
        if (ouroborosSource.isWithRecursive()) {
          copy.addWithRecursive(with._1(), with._2());
        } else {
          copy.addWith(with._1(), with._2());
        }
      });
    }

    return copy;
  }

  @SuppressWarnings({"unchecked", "rawtypes"})
  private void copyParams(Map<ParamExpression<?>, Object> params, QueryMetadata target) {
    for (Map.Entry<ParamExpression<?>, Object> entry : params.entrySet()) {
      target.setParam((ParamExpression) entry.getKey(), entry.getValue());
    }
  }

  private void setUnionState(Expression<?> unionExpression, SubQueryExpression<?> firstSubQuery) {
    this.union = unionExpression;
    this.firstUnionSubQuery = firstSubQuery;
    this.unionAll = false;
  }

  @Override
  protected SQLSerializer serialize(boolean forCountRow) {
    if (union == null || !getMetadata().getModifiers().isRestricting()) {
      return super.serialize(forCountRow);
    }

    // QueryDSL's union serializer omits modifiers. A derived table lets the
    // dialect's normal query serializer own pagination, including Oracle/SQL Server.
    var innerMetadata = createMainUnionSubQueryMetadata(getMetadata());
    for (var flag : List.copyOf(innerMetadata.getFlags())) {
      innerMetadata.removeFlag(flag);
    }
    var inner = new OuroborosSQLQuery(templates, innerMetadata);
    inner.setUnionState(union, firstUnionSubQuery);

    var outer = new DefaultQueryMetadata();
    outer.setProjection(Wildcard.all);
    outer.addJoin(JoinType.DEFAULT, ExpressionUtils.as(inner, Expressions.path(Object.class, "union_result")));
    outer.setModifiers(getMetadata().getModifiers());
    getMetadata().getOrderBy().forEach(outer::addOrderBy);
    getMetadata().getFlags().forEach(outer::addFlag);

    var serializer = new SQLSerializer(configuration) {
      @Override
      protected void handleOrderBy(List<OrderSpecifier<?>> orderBy) {
        // UNION order expressions refer to output columns, as in serializeUnion.
        boolean previous = skipParent;
        skipParent = true;
        super.handleOrderBy(orderBy);
        skipParent = previous;
      }
    };
    serializer.setUseLiterals(useLiterals);
    serializer.serialize(outer, forCountRow);
    return serializer;
  }
}
