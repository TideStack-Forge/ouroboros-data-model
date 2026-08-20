package com.ouroboros.data.dsl;

import com.ouroboros.data.model.DataModelField;
import com.querydsl.core.types.Path;
import com.querydsl.core.types.dsl.SimpleExpression;
import com.querydsl.core.types.dsl.SimplePath;

public interface ModelFieldPath<T> extends Path<T> {
  static <T> ModelFieldPath<T> of(Class<? extends T> type, DataModelField modelField) {
    return new ModelFieldPathImpl<>(type, modelField);
  }

  static <T> ModelFieldPath<T> of(Class<? extends T> type, DataModelField modelField, Path<?> parent) {
    return new ModelFieldPathImpl<>(type, modelField, parent);
  }

  DataModelField getModelField();

  SimpleExpression<T> as(String alias);

  SimpleExpression<T> as(Path<T> alias);

  ModelFieldPath<T> transformParent(Path<?> parent);

  class ModelFieldPathImpl<T> extends SimplePath<T> implements ModelFieldPath<T> {
    private final DataModelField modelField;

    public ModelFieldPathImpl(Class<? extends T> type, DataModelField modelField) {
      super(type, modelField.getRawName());
      this.modelField = modelField;
    }

    public ModelFieldPathImpl(Class<? extends T> type, DataModelField modelField, Path<?> parent) {
      super(type, parent, modelField.getRawName());
      this.modelField = modelField;
    }

    protected ModelFieldPathImpl(Class<? extends T> type, DataModelField modelField, String variable) {
      super(type, variable);
      this.modelField = modelField;
    }

    protected ModelFieldPathImpl(Class<? extends T> type, DataModelField modelField, Path<?> parent, String variable) {
      super(type, parent, variable);
      this.modelField = modelField;
    }

    @Override
    public SimpleExpression<T> as(String alias) {
      Path<T> aliasPath = new ModelFieldPathImpl<>(getType(), modelField, alias);
      return super.as(aliasPath);
    }

    @Override
    public SimpleExpression<T> as(Path<T> alias) {
      if (alias instanceof ModelFieldPath<?>) {
        return super.as(alias);
      }
      return as(alias.getMetadata().getName());
    }

    @Override
    public DataModelField getModelField() {
      return modelField;
    }

    @Override
    public ModelFieldPath<T> transformParent(Path<?> parent) {
      return new ModelFieldPathImpl<>(getType(), modelField, parent, getMetadata().getName());
    }
  }
}
