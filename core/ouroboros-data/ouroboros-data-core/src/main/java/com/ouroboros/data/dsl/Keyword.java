package com.ouroboros.data.dsl;

import java.util.Collections;
import java.util.HashMap;
import java.util.Map;

public enum Keyword {

  WITH("WITH"),
  WITH_RECURSIVE("WITHRECURSIVE", "WITH_RECURSIVE"),
  /**
   * SELECT
   */
  SELECT("SELECT"),
  DISTINCT("DISTINCT"),
  OMIT("OMIT"),
  FROM("FROM"),
  WHERE("WHERE"),

  // Pagination
  LIMIT("LIMIT"),
  OFFSET("OFFSET", "skip"),
  PAGE("PAGE", "PAGENUM"),
  PAGESIZE("PAGESIZE", "PERPAGE"),

  // Ordering
  ORDER("ORDER"),

  // Grouping
  GROUP("GROUP", "GROUPBY", "GROUP_BY"),
  HAVING("HAVING"),

  // Union
  UNION("UNION"),
  UNION_ALL("UNIONALL", "UNION_ALL"),

  // Populate
  POPULATE("POPULATE"),

  AS("AS"),

  /**
   * JOIN
   */
  JOIN("JOIN"),
  INNER_JOIN("INNERJOIN"),
  LEFT_JOIN("LEFTJOIN"),
  RIGHT_JOIN("RIGHTJOIN"),
  FULL_JOIN("FULLJOIN"),
  CROSS_JOIN("CROSSJOIN"),

  /**
   * INSERT
   */
  INSERT("INSERT"),
  VALUES("VALUES"),

  /**
   * DELETE
   */
  DELETE("DELETE"),

  /**
   * UPDATE
   */
  UPDATE("UPDATE"),
  SET("SET");

  private static final Map<String, Keyword> LOOKUP;

  static {
    Map<String, Keyword> map = new HashMap<>();
    for (Keyword kw : values()) {
      map.put(kw.keyword.toUpperCase(), kw);
      for (String alias : kw.aliases) {
        map.put(alias.toUpperCase(), kw);
      }
    }
    LOOKUP = Collections.unmodifiableMap(map);
  }

  private final String keyword;
  private final String[] aliases;

  Keyword(String keyword, String... aliases) {
    this.keyword = keyword;
    this.aliases = aliases;
  }

  public static Keyword fromString(String key) {
    if (key == null) return null;
    return LOOKUP.get(key.toUpperCase());
  }

  public Object findIn(Map<String, ?> map) {
    for (Map.Entry<String, ?> entry : map.entrySet()) {
      if (this.equals(Keyword.fromString(entry.getKey()))) {
        return entry.getValue();
      }
    }
    return null;
  }

  @Override
  public String toString() {
    return keyword;
  }
}
