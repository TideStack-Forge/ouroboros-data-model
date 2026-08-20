package com.ouroboros.data.model.decorators.typed;

import java.util.Arrays;

import com.ouroboros.data.annotation.UniqueConstraint;
import com.ouroboros.data.model.DataModelMeta;
import com.ouroboros.data.model.DataModelUniqueConstraintMeta;
import com.ouroboros.data.model.TypedAnnotationDecoratorContext;
import com.ouroboros.data.model.TypedModelAnnotationDecorator;
import com.ouroboros.data.model.UniquenessScope;

public class UniqueConstraintAnnotationDecorator implements TypedModelAnnotationDecorator<UniqueConstraint> {

  @Override
  public Class<UniqueConstraint> annotationType() {
    return UniqueConstraint.class;
  }

  @Override
  public DataModelMeta decorate(
      Class<?> modelClass,
      UniqueConstraint annotation,
      DataModelMeta modelMeta,
      TypedAnnotationDecoratorContext context
  ) {
    DataModelMeta decorated = modelMeta.deepCopy();
    DataModelUniqueConstraintMeta constraint = new DataModelUniqueConstraintMeta();
    constraint.setName(annotation.name());
    constraint.setFields(Arrays.asList(annotation.fields()));
    constraint.setScope(toExplicitScope(annotation.scope()));
    decorated.addUniqueConstraint(constraint);
    return decorated;
  }

  private UniquenessScope toExplicitScope(UniquenessScope scope) {
    if (scope == UniquenessScope.DEFAULT) {
      return null;
    }
    return scope;
  }
}
