package com.ouroboros.data.transpile;

import java.util.ArrayList;
import java.util.List;

import io.vavr.Tuple2;

import com.querydsl.core.DefaultQueryMetadata;
import com.querydsl.core.QueryMetadata;
import com.querydsl.core.types.Path;

public class DefaultOuroborosQueryMetadata extends DefaultQueryMetadata implements OuroborosQueryMetadata {
  boolean withRecursive = false;
  List<QueryMetadata> unions = new ArrayList<>();
  List<QueryMetadata> unionAlls = new ArrayList<>();
  List<Tuple2<Path<?>, QueryMetadata>> withs = new ArrayList<>();

  @Override
  public void addUnion(QueryMetadata metadata) {
    unions.add(metadata);
  }

  @Override
  public void addUnionAll(QueryMetadata metadata) {
    unionAlls.add(metadata);
  }

  @Override
  public void addWith(Path<?> alias, QueryMetadata metadata) {
    withs.add(new Tuple2<>(alias, metadata));
  }

  @Override
  public void addWithRecursive(Path<?> alias, QueryMetadata metadata) {
    withRecursive = true;
    withs.add(new Tuple2<>(alias, metadata));
  }

  @Override
  public boolean isWithRecursive() {
    return withRecursive;
  }

  @Override
  public List<QueryMetadata> getUnions() {
    return unions;
  }

  @Override
  public List<QueryMetadata> getUnionAlls() {
    return unionAlls;
  }

  @Override
  public List<Tuple2<Path<?>, QueryMetadata>> getWiths() {
    return withs;
  }
}
