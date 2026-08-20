package com.ouroboros.data.transpile;

import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

import com.ouroboros.data.model.DataModel;
import com.querydsl.core.types.Path;
import com.querydsl.core.types.dsl.Expressions;

/**
 * 为查询阶段提供带别名的字段视图。
 *
 * <p>仅包装字段解析结果，不改变底层表路径，避免影响 FROM/JOIN/DML 对原始表路径的使用。
 */
public class AliasedFieldSource implements FieldSource {

  private final FieldSource delegate;
  private final Path<?> aliasPath;

  public AliasedFieldSource(FieldSource delegate, String alias) {
    this.delegate = delegate;
    this.aliasPath = Expressions.path(Object.class, alias);
  }

  @Override
  public Optional<Path<?>> getField(String fieldName) {
    return delegate.getField(fieldName)
        .map(path -> FieldSource.transformParent(path, aliasPath));
  }

  @Override
  public List<Path<?>> getFields() {
    return delegate.getFields().stream()
        .map(path -> FieldSource.transformParent(path, aliasPath))
        .collect(Collectors.toList());
  }

  @Override
  public Path<?> getSelfPath() {
    return delegate.getSelfPath();
  }

  @Override
  public Optional<DataModel> getDataModel() {
    return delegate.getDataModel();
  }
}
