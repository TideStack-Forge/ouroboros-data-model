package com.ouroboros.data.orchestration;

import java.util.Map;
import java.util.Optional;
import java.util.TreeMap;

import com.ouroboros.data.dsl.statement.QueryStatement;

/**
 * 关联改写内部元数据。
 *
 * <p>用于在不污染 DSL 语义的前提下，在递归 rewrite 阶段保留结构化 relation path。
 */
public final class RelationRewriteMetadata {

  private static final String RELATION_FIELD_PATH_KEY = "_orchestrationRelationFieldPath";

  private RelationRewriteMetadata() {
  }

  public static QueryStatement attachRelationFieldPath(QueryStatement statement, String relationFieldPath) {
    Map<String, Object> metaMap = new TreeMap<>(String.CASE_INSENSITIVE_ORDER);
    metaMap.putAll(statement);
    if (relationFieldPath == null || relationFieldPath.isEmpty()) {
      metaMap.remove(RELATION_FIELD_PATH_KEY);
    } else {
      metaMap.put(RELATION_FIELD_PATH_KEY, relationFieldPath);
    }
    return new QueryStatement(metaMap);
  }

  public static Optional<String> getRelationFieldPath(QueryStatement statement) {
    if (statement == null) {
      return Optional.empty();
    }
    Object value = statement.get(RELATION_FIELD_PATH_KEY);
    if (value instanceof String && !((String) value).isEmpty()) {
      return Optional.of((String) value);
    }
    return Optional.empty();
  }
}
