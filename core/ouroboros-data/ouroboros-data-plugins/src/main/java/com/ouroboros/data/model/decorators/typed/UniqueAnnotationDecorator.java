package com.ouroboros.data.model.decorators.typed;

import java.lang.reflect.Field;

import com.ouroboros.data.annotation.Unique;
import com.ouroboros.data.model.DataModelFieldMeta;
import com.ouroboros.data.model.DataModelMeta;
import com.ouroboros.data.model.TypedAnnotationDecoratorContext;
import com.ouroboros.data.model.TypedFieldAnnotationDecorator;
import com.ouroboros.data.model.UniquenessScope;

public class UniqueAnnotationDecorator implements TypedFieldAnnotationDecorator<Unique> {

  @Override
  public Class<Unique> annotationType() {
    return Unique.class;
  }

  @Override
  public DataModelFieldMeta decorate(
      Class<?> modelClass,
      Field field,
      Unique annotation,
      DataModelFieldMeta fieldMeta,
      DataModelMeta modelMeta,
      TypedAnnotationDecoratorContext context
  ) {
    DataModelFieldMeta decorated = fieldMeta.deepCopy();
    decorated.setIsUnique(true);
    decorated.setUniquenessScope(toExplicitScope(annotation.scope()));
    return decorated;
  }

  private UniquenessScope toExplicitScope(UniquenessScope scope) {
    if (scope == UniquenessScope.DEFAULT) {
      return null;
    }
    return scope;
  }
}
