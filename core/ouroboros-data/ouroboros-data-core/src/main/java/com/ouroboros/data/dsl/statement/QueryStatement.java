package com.ouroboros.data.dsl.statement;

import static com.ouroboros.data.util.Asserts.assertValidEntityName;
import static com.ouroboros.data.util.Asserts.assertValidPathNode;

import java.io.Serializable;
import java.util.*;

import com.ouroboros.data.dsl.JoinType;
import com.ouroboros.data.dsl.Keyword;
import com.ouroboros.data.dsl.Order;
import com.ouroboros.data.dsl.SExpression;
import com.ouroboros.data.util.DataJson;
import com.ouroboros.data.util.DataMaps;

@SuppressWarnings({"unchecked", "rawtypes", "unused"})
public class QueryStatement extends Statement {
  private static final long serialVersionUID = 1L;

  public QueryStatement(Map<String, Object> metaMap) {
    super(metaMap);
  }

  public static QueryStatementBuilder builder() {
    return new QueryStatementBuilder();
  }

  public List<CTEDefinition> getWith() {
    return (List<CTEDefinition>) getOrDefault(Keyword.WITH.toString(), Collections.emptyList());
  }

  public Boolean getDistinct() {
    return (Boolean) getOrDefault(Keyword.DISTINCT.toString(), false);
  }

  public List<SExpression<?>> getSelect() {
    var select = (List) get(Keyword.SELECT.toString());
    return select == null ? Collections.emptyList() : select;
  }

  public TableSource getFrom() {
    return (TableSource) get(Keyword.FROM.toString());
  }

  public SExpression<Boolean> getWhere() {
    return (SExpression<Boolean>) getOrDefault(Keyword.WHERE.toString(), SExpression.empty(Boolean.class));
  }

  public Long getOffset() {
    var offset = get(Keyword.OFFSET.toString());
    if (offset == null) {
      return null;
    }
    return (Long) offset;
  }

  public Long getLimit() {
    var limit = get(Keyword.LIMIT.toString());
    if (limit == null) {
      return null;
    }
    return (Long) limit;
  }

  public List<JoinEntry> getJoins() {
    var joins = (List<JoinEntry>) get(Keyword.JOIN.toString());
    return joins == null ? Collections.emptyList() : joins;
  }

  public List<OrderEntry> getOrders() {
    return (List<OrderEntry>) getOrDefault(Keyword.ORDER.toString(), Collections.emptyList());
  }

  public SExpression<?> getGroup() {
    return (SExpression<?>) getOrDefault(Keyword.GROUP.toString(), SExpression.empty());
  }

  public SExpression<Boolean> getHaving() {
    return (SExpression<Boolean>) getOrDefault(Keyword.HAVING.toString(), SExpression.empty(Boolean.class));
  }

  public List<UnionEntry> getUnions() {
    return (List<UnionEntry>) getOrDefault(Keyword.UNION.toString(), Collections.emptyList());
  }

  public List<String> getPopulate() {
    Object raw = get(Keyword.POPULATE.toString());
    if (raw == null) {
      return Collections.emptyList();
    }
    if (raw instanceof List) {
      List<?> list = (List<?>) raw;
      List<String> result = new ArrayList<String>(list.size());
      for (Object item : list) {
        if (!(item instanceof String)) {
          throw new IllegalStateException(
              "POPULATE list contains non-String element: "
                  + (item == null ? "null" : item.getClass().getName())
                  + ". Use PopulateClause.fromRaw() for structured parsing.");
        }
        result.add((String) item);
      }
      return result;
    }
    throw new IllegalStateException(
        "POPULATE value is not a List: " + raw.getClass().getName()
            + ". Use PopulateClause.fromRaw() for structured parsing.");
  }

  /**
   * @deprecated Since 1.0.0-beta.2, for removal in 1.0.0-beta.4. Use {@link #getPopulate()} or PopulateClause.fromRaw() instead.
   */
  @Deprecated
  public Object getRawPopulate() {
    return get(Keyword.POPULATE.toString());
  }

  public List<String> getOmit() {
    Object raw = get(Keyword.OMIT.toString());
    if (raw == null) {
      return Collections.emptyList();
    }
    if (raw instanceof List) {
      List<?> list = (List<?>) raw;
      List<String> result = new ArrayList<String>(list.size());
      for (Object item : list) {
        if (!(item instanceof String)) {
          throw new IllegalStateException(
              "OMIT list contains non-String element: "
                  + (item == null ? "null" : item.getClass().getName())
                  + ". Use OmitClause.fromRaw() for structured parsing.");
        }
        result.add((String) item);
      }
      return result;
    }
    throw new IllegalStateException(
        "OMIT value is not a List: " + raw.getClass().getName()
            + ". Use OmitClause.fromRaw() for structured parsing.");
  }

  /**
   * @deprecated Since 1.0.0-beta.2, for removal in 1.0.0-beta.4. Use {@link #getOmit()} or OmitClause.fromRaw() instead.
   */
  @Deprecated
  public Object getRawOmit() {
    return get(Keyword.OMIT.toString());
  }

  public QueryStatementBuilder getBuilder() {
    return new QueryStatementBuilder(this);
  }

  /**
   * 将已规范化的查询语句降级为可重新规范化的原始 Map。
   *
   * <p>该表示用于只接受 raw Map 的数据模型插件链。插件需要读取的 WHERE、
   * POPULATE 和 OMIT 会降级为原始结构；其他已规范化子句保留 canonical 节点，
   * 由 normalizer 原样接收，以避免有序子句在反向编码时发生重排。
   */
  public Map<String, Object> toRawMap() {
    Map<String, Object> raw = new TreeMap<>(String.CASE_INSENSITIVE_ORDER);
    raw.putAll(this);

    downgradeWhere(raw);
    downgradeModelClauses(raw);
    return raw;
  }

  private void downgradeWhere(Map<String, Object> raw) {
    Object where = raw.get(Keyword.WHERE.toString());
    if (!(where instanceof SExpression<?> expression)) {
      return;
    }
    if (expression.isEmpty()) {
      raw.remove(Keyword.WHERE.toString());
      return;
    }
    raw.put(
        Keyword.WHERE.toString(),
        Collections.singletonMap("AND", expression)
    );
  }


  private void downgradeModelClauses(Map<String, Object> raw) {
    if (!(this instanceof ModelQueryStatement modelStatement)) {
      return;
    }
    if (modelStatement.getPopulateClause() != null) {
      List<Object> populate = modelStatement.getPopulateClause().getEntries().stream()
          .map(entry -> entry.options() == null
              ? entry.fieldName()
              : Collections.singletonMap(entry.fieldName(), entry.options()))
          .collect(java.util.stream.Collectors.toList());
      raw.put(Keyword.POPULATE.toString(), populate);
    }
    if (modelStatement.getOmitClause() != null) {
      raw.put(Keyword.OMIT.toString(), new ArrayList<>(modelStatement.getOmitClause().getFields()));
    }
  }


  @Override
  public boolean equals(Object obj) {
    if (!(obj instanceof QueryStatement)) {
      return false;
    }
    QueryStatement statement = (QueryStatement) obj;
    if (this == obj) {
      return true;
    }
    if (this.size() != statement.size()) {
      return false;
    }
    if (!this.keySet().equals(statement.keySet())) {
      return false;
    }
    return this.entrySet().stream().allMatch(entry -> Objects.equals(statement.get(entry.getKey()), entry.getValue()));
  }

  @Override
  public int hashCode() {
    return Objects.hash(entrySet());
  }

  @Override
  public String toString() {
    return DataJson.toJsonString(this);
  }

  public static class TableSource implements Serializable {
    private static final long serialVersionUID = 1L;

    private final String alias;
    private final Object origin;

    public TableSource(String origin, String alias) {
      this.alias = alias;
      this.origin = origin;
    }

    public TableSource(QueryStatement subQuery, String alias) {
      this.alias = alias;
      this.origin = subQuery;
    }

    public String getAlias() {
      return alias;
    }

    public Object getOrigin() {
      return origin;
    }

    public Boolean isSubQuery() {
      return origin instanceof QueryStatement;
    }

    public String getTableName() {
      return isSubQuery() ? null : (String) origin;
    }

    public QueryStatement getSubQuery() {
      return isSubQuery() ? (QueryStatement) origin : null;
    }

    public String getName() {
      return alias == null ? getTableName() : alias;
    }
  }

  public static class JoinEntry implements Serializable {
    private static final long serialVersionUID = 1L;

    private final JoinType type;
    private final String alias;
    private final Object origin;
    private final SExpression<Boolean> on;

    public JoinEntry(JoinType type, String tableName, String alias, SExpression<Boolean> on) {
      this.type = type;
      this.alias = alias;
      this.origin = tableName;
      this.on = on;
    }

    public JoinEntry(JoinType type, QueryStatement subQuery, String alias, SExpression<Boolean> on) {
      this.type = type;
      this.alias = alias;
      this.origin = subQuery;
      this.on = on;
    }

    public JoinEntry(JoinType type, TableSource tableSource, SExpression<Boolean> on) {
      this.type = type;
      this.alias = tableSource.getAlias();
      this.origin = tableSource.getOrigin();
      this.on = on;
    }

    public JoinType getType() {
      return type;
    }

    public String getAlias() {
      return alias;
    }

    public Object getOrigin() {
      return origin;
    }

    public SExpression<Boolean> getOn() {
      return Optional.ofNullable(on)
          .orElse(SExpression.empty(Boolean.class));
    }

    public QueryStatement getSubQuery() {
      return origin instanceof QueryStatement
          ? (QueryStatement) origin
          : null;
    }

    public String getTableName() {
      return origin instanceof String
          ? (String) origin
          : null;
    }

    public Boolean isSubQuery() {
      return origin instanceof QueryStatement;
    }
  }

  @SuppressWarnings("ClassCanBeRecord")
  public static class UnionEntry implements Serializable {
    private static final long serialVersionUID = 1L;

    private final QueryStatement query;
    private final Boolean isAll;

    public UnionEntry(QueryStatement query, Boolean isAll) {
      this.query = query;
      this.isAll = isAll;
    }

    public QueryStatement getQuery() {
      return query;
    }

    public Boolean isAll() {
      return isAll;
    }
  }

  public static class OrderEntry implements Serializable {
    private static final long serialVersionUID = 1L;

    private final String column;
    private final Order order;

    public OrderEntry(String column, Order order) {
      this.column = column;
      this.order = order;
    }

    public OrderEntry(String column, String order) {
      this.column = column;
      this.order = "desc".equalsIgnoreCase(order)
          ? Order.DESC
          : Order.ASC;
    }

    public String getColumn() {
      return column;
    }

    public Order getOrder() {
      return order;
    }
  }

  @SuppressWarnings("ClassCanBeRecord")
  public static class CTEDefinition implements Serializable {
    private static final long serialVersionUID = 1L;

    private final String alias;
    private final QueryStatement query;

    private final Boolean isRecursive;

    public CTEDefinition(QueryStatement query, String alias, Boolean isRecursive) {
      this.query = query;
      this.alias = alias;
      this.isRecursive = isRecursive;
    }

    public String getAlias() {
      return alias;
    }

    public QueryStatement getQuery() {
      return query;
    }

    public Boolean isRecursive() {
      return isRecursive;
    }
  }

  @SuppressWarnings("UnusedReturnValue")
  public static class QueryStatementBuilder {
    final Map<String, Object> metaMap;

    QueryStatementBuilder() {
      metaMap = new TreeMap<>(String.CASE_INSENSITIVE_ORDER);
    }

    QueryStatementBuilder(QueryStatement statement) {
      this();
      metaMap.putAll(statement);
    }

    public QueryStatementBuilder select(SExpression<?>... selects) {
      return select(Arrays.asList(selects));
    }

    public QueryStatementBuilder select(Collection<SExpression<?>> selects) {
      metaMap.put(Keyword.SELECT.toString(), new ArrayList<>(selects));
      return this;
    }

    public QueryStatementBuilder addSelect(SExpression<?>... selects) {
      return addSelect(Arrays.asList(selects));
    }

    public QueryStatementBuilder addSelect(Collection<SExpression<?>> selects) {
      List oldSelect = (List) metaMap.get(Keyword.SELECT.toString());
      if (oldSelect == null) {
        oldSelect = new ArrayList();
        metaMap.put(Keyword.SELECT.toString(), oldSelect);
      }
      oldSelect.addAll(selects);
      return this;
    }

    public QueryStatementBuilder distinct(Boolean isDistinct) {
      metaMap.put(Keyword.DISTINCT.toString(), isDistinct);
      return this;
    }

    public QueryStatementBuilder from(TableSource tableSource) {
      metaMap.put(Keyword.FROM.toString(), tableSource);
      return this;
    }

    public QueryStatementBuilder from(String from) {
      String[] arr = from.split("(?i)( as | )");
      String alias = arr.length == 1 ? from : arr.length == 2 ? arr[1] : null;
      return from(from, alias);
    }

    public QueryStatementBuilder from(String table, String alias) {
      assertValidEntityName(table);
      assertValidPathNode(alias);
      metaMap.put(Keyword.FROM.toString(), new TableSource(table, alias));
      return this;
    }

    public QueryStatementBuilder from(QueryStatement subQuery, String alias) {
      metaMap.put(Keyword.FROM.toString(), new TableSource(subQuery, alias));
      return this;
    }

    public QueryStatementBuilder join(Collection<JoinEntry> joins) {
      List oldJoins = (List) metaMap.get(Keyword.JOIN.toString());
      if (oldJoins == null) {
        oldJoins = new ArrayList();
        metaMap.put(Keyword.JOIN.toString(), oldJoins);
      }
      oldJoins.addAll(joins);
      return this;
    }

    public QueryStatementBuilder join(JoinEntry join) {
      List joins = (List) metaMap.get(Keyword.JOIN.toString());
      if (joins == null) {
        joins = new ArrayList();
        metaMap.put(Keyword.JOIN.toString(), joins);
      }
      joins.add(join);
      return this;
    }

    public QueryStatementBuilder join(JoinType type, QueryStatement subQuery, String alias, SExpression<Boolean> on) {
      JoinEntry join = new JoinEntry(type, subQuery, alias, on);
      return join(join);
    }

    public QueryStatementBuilder join(JoinType type, String table, String alias, SExpression<Boolean> on) {
      JoinEntry join = new JoinEntry(type, table, alias, on);
      return join(join);
    }

    public QueryStatementBuilder innerJoin(QueryStatement subQuery, String alias, SExpression<Boolean> on) {
      join(JoinType.INNERJOIN, subQuery, alias, on);
      return this;
    }

    public QueryStatementBuilder innerJoin(String tableName, String alias, SExpression<Boolean> on) {
      join(JoinType.INNERJOIN, tableName, alias, on);
      return this;
    }

    public QueryStatementBuilder leftJoin(QueryStatement subQuery, String alias, SExpression<Boolean> on) {
      join(JoinType.LEFTJOIN, subQuery, alias, on);
      return this;
    }

    public QueryStatementBuilder leftJoin(String tableName, String alias, SExpression<Boolean> on) {
      join(JoinType.LEFTJOIN, tableName, alias, on);
      return this;
    }

    public QueryStatementBuilder rightJoin(QueryStatement subQuery, String alias, SExpression<Boolean> on) {
      join(JoinType.RIGHTJOIN, subQuery, alias, on);
      return this;
    }

    public QueryStatementBuilder rightJoin(String tableName, String alias, SExpression<Boolean> on) {
      join(JoinType.RIGHTJOIN, tableName, alias, on);
      return this;
    }

    public QueryStatementBuilder fullJoin(QueryStatement subQuery, String alias, SExpression<Boolean> on) {
      join(JoinType.FULLJOIN, subQuery, alias, on);
      return this;
    }

    public QueryStatementBuilder fullJoin(String tableName, String alias, SExpression<Boolean> on) {
      join(JoinType.FULLJOIN, tableName, alias, on);
      return this;
    }

    public QueryStatementBuilder crossJoin(QueryStatement subQuery, String alias) {
      join(JoinType.DEFAULT, subQuery, alias, null);
      return this;
    }

    public QueryStatementBuilder crossJoin(String tableName, String alias) {
      join(JoinType.DEFAULT, tableName, alias, null);
      return this;
    }

    public QueryStatementBuilder where(SExpression<Boolean> where) {
      where = where == null
          ? SExpression.empty(Boolean.class)
          : where;
      metaMap.put(Keyword.WHERE.toString(), where);
      return this;
    }

    public QueryStatementBuilder replaceJoins(List<JoinEntry> joins) {
      if (joins == null || joins.isEmpty()) {
        metaMap.remove(Keyword.JOIN.toString());
      } else {
        metaMap.put(Keyword.JOIN.toString(), new ArrayList<>(joins));
      }
      return this;
    }

    public QueryStatementBuilder order(Collection<OrderEntry> orders) {
      List oldOrders = (List) metaMap.get(Keyword.ORDER.toString());
      if (oldOrders == null) {
        oldOrders = new ArrayList();
        metaMap.put(Keyword.ORDER.toString(), oldOrders);
      }
      oldOrders.addAll(orders);
      return this;
    }

    public QueryStatementBuilder order(OrderEntry... orders) {
      return order(Arrays.asList(orders));
    }

    public QueryStatementBuilder order(String field, Order order) {
      return order(new OrderEntry(field, order));
    }

    public QueryStatementBuilder order(String field, String order) {
      return order(new OrderEntry(field, order));
    }

    public QueryStatementBuilder group(SExpression<?> group) {
      metaMap.put(Keyword.GROUP.toString(), group);
      return this;
    }

    public QueryStatementBuilder having(SExpression<Boolean> having) {
      metaMap.put(Keyword.HAVING.toString(), having);
      return this;
    }

    public QueryStatementBuilder offset(Long offset) {
      metaMap.put(Keyword.OFFSET.toString(), offset);
      return this;
    }

    public QueryStatementBuilder offset(Number offset) {
      return offset(offset.longValue());
    }

    public QueryStatementBuilder limit(Long limit) {
      metaMap.put(Keyword.LIMIT.toString(), limit);
      return this;
    }

    public QueryStatementBuilder limit(Number limit) {
      return limit(limit.longValue());
    }

    public QueryStatementBuilder union(QueryStatement query) {
      List unions = (List) metaMap.get(Keyword.UNION.toString());
      if (unions == null) {
        unions = new ArrayList();
        metaMap.put(Keyword.UNION.toString(), unions);
      }
      unions.add(new UnionEntry(query, false));
      return this;
    }

    public QueryStatementBuilder unionAll(QueryStatement query) {
      List unions = (List) metaMap.get(Keyword.UNION.toString());
      if (unions == null) {
        unions = new ArrayList();
        metaMap.put(Keyword.UNION.toString(), unions);
      }
      unions.add(new UnionEntry(query, true));
      return this;
    }

    public QueryStatementBuilder with(Collection<CTEDefinition> cteDefinitions) {
      List with = (List) metaMap.get(Keyword.WITH.toString());
      if (with == null) {
        with = new ArrayList();
        metaMap.put(Keyword.WITH.toString(), with);
      }
      with.addAll(cteDefinitions);
      return this;
    }

    public QueryStatementBuilder with(String alias, QueryStatement another) {
      List with = (List) metaMap.get(Keyword.WITH.toString());
      if (with == null) {
        with = new ArrayList();
        metaMap.put(Keyword.WITH.toString(), with);
      }
      with.add(new CTEDefinition(another, alias, false));
      return this;
    }

    public QueryStatementBuilder withRecursive(String alias, QueryStatement another) {
      List with = (List) metaMap.get(Keyword.WITH.toString());
      if (with == null) {
        with = new ArrayList();
        metaMap.put(Keyword.WITH.toString(), with);
      }
      with.add(new CTEDefinition(another, alias, true));
      return this;
    }

    public QueryStatementBuilder populate(String... fields) {
      return populate(Arrays.asList(fields));
    }

    public QueryStatementBuilder populate(Collection<String> fields) {
      List populate = (List) metaMap.get(Keyword.POPULATE.toString());
      if (populate == null) {
        populate = new ArrayList();
        metaMap.put(Keyword.POPULATE.toString(), populate);
      }
      populate.addAll(fields);
      return this;
    }

    /**
     * @deprecated Since 1.0.0-beta.2, for removal. Use {@link #populate(String...)} instead.
     */
    @Deprecated
    public QueryStatementBuilder putRawPopulate(Object rawPopulate) {
      metaMap.put(Keyword.POPULATE.toString(), rawPopulate);
      return this;
    }

    /**
     * @deprecated Since 1.0.0-beta.2, for removal. Use {@link #populate(String...)} instead.
     */
    @Deprecated
    public QueryStatementBuilder putRawOmit(Object rawOmit) {
      metaMap.put(Keyword.OMIT.toString(), rawOmit);
      return this;
    }

    public QueryStatement build() {
      return new QueryStatement(metaMap);
    }

    public QueryStatementBuilder replaceSelect(List<SExpression<?>> selects) {
      metaMap.put(Keyword.SELECT.toString(), new ArrayList<>(selects));
      return this;
    }

    public QueryStatementBuilder merge(QueryStatementBuilder another) {
      DataMaps.mergeTo(metaMap, another.metaMap);
      return this;
    }

  }
}
