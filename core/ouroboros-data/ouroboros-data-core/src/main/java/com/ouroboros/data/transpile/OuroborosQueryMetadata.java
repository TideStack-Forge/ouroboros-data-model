package com.ouroboros.data.transpile;

import java.util.List;

import io.vavr.Tuple2;

import com.querydsl.core.QueryMetadata;
import com.querydsl.core.types.Path;

public interface OuroborosQueryMetadata extends QueryMetadata {
  void addUnion(QueryMetadata metadata);

  void addUnionAll(QueryMetadata metadata);

  void addWith(Path<?> alias, QueryMetadata metadata);

  void addWithRecursive(Path<?> alias, QueryMetadata metadata);

  List<QueryMetadata> getUnions();

  List<QueryMetadata> getUnionAlls();

  List<Tuple2<Path<?>, QueryMetadata>> getWiths();

  boolean isWithRecursive();
}
