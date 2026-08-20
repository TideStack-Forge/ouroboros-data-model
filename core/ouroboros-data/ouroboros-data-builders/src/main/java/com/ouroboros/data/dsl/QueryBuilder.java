package com.ouroboros.data.dsl;

import java.util.*;
import java.util.stream.Collectors;
import java.util.stream.Stream;

public class QueryBuilder {
  private final String from;
  private List<Map<String, String>> select;
  private Map<String, Object> where;
  private String orderBy;
  private String orderByDirection;
  private List<String> groupBy = new ArrayList<>();
  private Integer limit = null;
  private Integer offset = null;

  private QueryBuilder(String from) {
    this.from = from;
  }

  public static QueryBuilder create() {
    return new QueryBuilder(null);
  }

  public static QueryBuilder create(String from) {
    return new QueryBuilder(from);
  }

  public QueryBuilder select(String... fields) {
    select = new ArrayList<>();
    Stream.of(fields)
        .flatMap(s -> Stream.of(s.split(",")))
        .map(String::trim)
        .forEach(field -> {
          var fieldAliasArray = Stream.of(field.split("(?i)( as | )"))
              .map(String::trim)
              .filter(s -> !s.isEmpty())
              .collect(Collectors.toList());
          if (fieldAliasArray.size() == 2) {
            select.add(Collections.singletonMap(fieldAliasArray.get(0), fieldAliasArray.get(1)));
          } else {
            select.add(Collections.singletonMap(fieldAliasArray.get(0), fieldAliasArray.get(0)));
          }
        });
    return this;
  }

  @SafeVarargs
  public final QueryBuilder select(Map<String, String>... fields) {
    select = Arrays.asList(fields);
    return this;
  }

  public QueryBuilder where(Map<String, Object> where) {
    this.where = where;
    return this;
  }

  public QueryBuilder orderBy(String orderBy) {
    this.orderBy = orderBy;
    return this;
  }

  public QueryBuilder orderBy(String orderBy, String orderByDirection) {
    this.orderBy = orderBy;
    this.orderByDirection = orderByDirection;
    return this;
  }

  public QueryBuilder groupBy(String... groupBy) {
    this.groupBy = Arrays.asList(groupBy);
    return this;
  }

  public QueryBuilder limit(Integer limit) {
    this.limit = limit;
    return this;
  }

  public QueryBuilder offset(Integer offset) {
    this.offset = offset;
    return this;
  }

  public Map<String, Object> build() {
    Map<String, Object> query = new LinkedHashMap<>();
    query.put("FROM", from);
    query.put("SELECT", select == null ? "*" : select);
    if (where != null) {
      query.put("WHERE", where);
    }
    if (orderBy != null) {
      var direction = orderByDirection == null ? "asc" : orderByDirection;
      query.put("ORDER", Collections.singletonMap(orderBy, direction));
    }
    if (!groupBy.isEmpty()) {
      query.put("GROUP", groupBy);
    }
    if (limit != null) {
      query.put("LIMIT", limit);
    }
    if (offset != null) {
      query.put("OFFSET", offset);
    }
    return query;
  }
}
