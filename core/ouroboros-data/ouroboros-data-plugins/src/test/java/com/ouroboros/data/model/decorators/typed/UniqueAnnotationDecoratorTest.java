package com.ouroboros.data.model.decorators.typed;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import org.junit.jupiter.api.Test;

import com.ouroboros.data.annotation.Field;
import com.ouroboros.data.annotation.Model;
import com.ouroboros.data.annotation.Unique;
import com.ouroboros.data.annotation.UniqueConstraint;
import com.ouroboros.data.exception.ModelMetadataException;
import com.ouroboros.data.model.DataModelFieldMeta;
import com.ouroboros.data.model.DataModelMeta;
import com.ouroboros.data.model.TypedAnnotationDecoratorContext;
import com.ouroboros.data.model.UniquenessScope;

class UniqueAnnotationDecoratorTest {

  @Test
  void fieldUniqueAnnotationWritesFieldMetadataOnly() throws NoSuchFieldException {
    java.lang.reflect.Field codeField = UniqueAnnotationModel.class.getDeclaredField("code");
    DataModelFieldMeta fieldMeta = new DataModelFieldMeta();
    fieldMeta.setName("code");

    DataModelFieldMeta decorated = new UniqueAnnotationDecorator().decorate(
        UniqueAnnotationModel.class,
        codeField,
        codeField.getAnnotation(Unique.class),
        fieldMeta,
        emptyMeta(),
        decoratorContext()
    );

    assertTrue(decorated.getIsUnique());
    assertEquals(UniquenessScope.ACTIVE_RECORDS, decorated.getUniquenessScope());
    assertTrue(emptyMeta().getUniqueConstraints().isEmpty());
  }

  @Test
  void fieldUniqueDefaultScopeWritesNullScope() throws NoSuchFieldException {
    java.lang.reflect.Field nameField = UniqueAnnotationModel.class.getDeclaredField("name");
    DataModelFieldMeta fieldMeta = new DataModelFieldMeta();
    fieldMeta.setName("name");

    DataModelFieldMeta decorated = new UniqueAnnotationDecorator().decorate(
        UniqueAnnotationModel.class,
        nameField,
        nameField.getAnnotation(Unique.class),
        fieldMeta,
        emptyMeta(),
        decoratorContext()
    );

    assertTrue(decorated.getIsUnique());
    assertNull(decorated.getUniquenessScope());
  }

  @Test
  void modelUniqueConstraintAnnotationWritesModelMetadataOnly() {
    UniqueConstraint annotation = UniqueAnnotationModel.class.getAnnotationsByType(UniqueConstraint.class)[0];

    DataModelMeta decorated = new UniqueConstraintAnnotationDecorator().decorate(
        UniqueAnnotationModel.class,
        annotation,
        emptyMeta(),
        decoratorContext()
    );

    assertEquals(1, decorated.getUniqueConstraints().size());
    assertEquals("project_code", decorated.getUniqueConstraints().get(0).getName());
    assertEquals(UniquenessScope.ACTIVE_RECORDS, decorated.getUniqueConstraints().get(0).getScope());
    assertEquals("projectId", decorated.getUniqueConstraints().get(0).getFields().get(0));
    assertEquals("code", decorated.getUniqueConstraints().get(0).getFields().get(1));
  }

  @Test
  void repeatedModelUniqueConstraintAnnotationsAreVisibleByType() {
    UniqueConstraint[] annotations = UniqueAnnotationModel.class.getAnnotationsByType(UniqueConstraint.class);

    DataModelMeta meta = emptyMeta();
    UniqueConstraintAnnotationDecorator decorator = new UniqueConstraintAnnotationDecorator();
    for (UniqueConstraint annotation : annotations) {
      meta = decorator.decorate(UniqueAnnotationModel.class, annotation, meta, decoratorContext());
    }

    assertEquals(2, meta.getUniqueConstraints().size());
    assertEquals("project_code", meta.getUniqueConstraints().get(0).getName());
    assertEquals("tenant_code", meta.getUniqueConstraints().get(1).getName());
    assertNull(meta.getUniqueConstraints().get(1).getScope());
  }

  private static DataModelMeta emptyMeta() {
    DataModelMeta meta = new DataModelMeta();
    meta.setName("UniqueAnnotationModel");
    return meta;
  }

  private static TypedAnnotationDecoratorContext decoratorContext() {
    return new TypedAnnotationDecoratorContext() {
      @Override
      public String modelName() {
        return "UniqueAnnotationModel";
      }

      @Override
      public String resolveModelName(Class<?> modelClass) {
        return modelClass.getSimpleName();
      }

      @Override
      public void claimPluginDescriptor(String pluginName, String ownerAnnotation) {
      }

      @Override
      public ModelMetadataException metadataException(String message) {
        return new ModelMetadataException(message, modelName());
      }

      @Override
      public ModelMetadataException metadataException(String message, Throwable cause) {
        return new ModelMetadataException(message, modelName(), cause);
      }
    };
  }

  @Model
  @UniqueConstraint(name = "project_code", fields = {"projectId", "code"}, scope = UniquenessScope.ACTIVE_RECORDS)
  @UniqueConstraint(name = "tenant_code", fields = {"tenantId", "code"})
  private static class UniqueAnnotationModel {
    @Field
    private Long projectId;

    @Field
    private Long tenantId;

    @Field
    @Unique(scope = UniquenessScope.ACTIVE_RECORDS)
    private String code;

    @Field
    @Unique
    private String name;
  }
}
