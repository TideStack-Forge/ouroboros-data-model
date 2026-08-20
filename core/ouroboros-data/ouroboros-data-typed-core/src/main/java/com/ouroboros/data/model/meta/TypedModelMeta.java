package com.ouroboros.data.model.meta;

import java.util.Collections;
import java.util.Objects;

import com.ouroboros.data.dsl.query.QuerySource;

/**
 * Base class for generated typed model meta classes.
 *
 * @param <M>    model type
 * @param <SELF> generated meta type
 */
public abstract class TypedModelMeta<M, SELF extends TypedModelMeta<M, SELF>>
    implements QuerySource {
  private final String modelName;
  private final Class<M> modelClass;
  private final String alias;

  protected TypedModelMeta(String modelName, Class<M> modelClass, String alias) {
    if (modelName == null || modelName.isBlank()) {
      throw new IllegalArgumentException("modelName must not be blank");
    }
    this.modelName = modelName;
    this.modelClass = Objects.requireNonNull(modelClass, "modelClass must not be null");
    this.alias = alias;
  }

  public String getModelName() {
    return modelName;
  }

  public Class<M> getModelClass() {
    return modelClass;
  }

  public String getAlias() {
    return alias;
  }

  @Override
  public Object toRawFrom() {
    return alias == null || alias.isBlank()
        ? modelName
        : Collections.singletonMap(alias, modelName);
  }

  public String qualify(String fieldName) {
    if (fieldName == null || fieldName.isBlank()) {
      throw new IllegalArgumentException("fieldName must not be blank");
    }
    return alias == null || alias.isBlank() ? fieldName : alias + "." + fieldName;
  }

  public abstract SELF as(String alias);

  protected StringField<SELF> stringField(String fieldName) {
    return new StringField<>(this, fieldName);
  }

  protected <T> TypedField<T, SELF> field(String fieldName) {
    return new TypedField<>(this, fieldName);
  }

  protected <E extends Enum<E>> EnumField<E, SELF> enumField(String fieldName, Class<E> enumType) {
    return new EnumField<>(this, fieldName, enumType);
  }
}
