package com.ouroboros.data.model.annotations;

import static org.junit.jupiter.api.Assertions.*;

import java.lang.annotation.Annotation;
import java.util.Map;

import org.junit.jupiter.api.Test;

import com.ouroboros.data.annotation.Field;
import com.ouroboros.data.annotation.Model;
import com.ouroboros.data.annotation.TypedModelPluginAnnotation;
import com.ouroboros.data.exception.ModelMetadataException;
import com.ouroboros.data.model.DataModelMeta;
import com.ouroboros.data.model.DataModelMetaDecorator;
import com.ouroboros.data.model.TypedAnnotationDecoratorContext;
import com.ouroboros.data.model.TypedModelAnnotationDecorator;
import com.ouroboros.data.model.annotations.plugins.ArchiveDelete;
import com.ouroboros.data.model.annotations.plugins.ArchiveDeleteAnnotationDecorator;
import com.ouroboros.data.model.annotations.plugins.AuditFields;
import com.ouroboros.data.model.annotations.plugins.AuditFieldsAnnotationDecorator;
import com.ouroboros.data.model.annotations.plugins.LogicalDelete;
import com.ouroboros.data.model.annotations.plugins.LogicalDeleteAnnotationDecorator;
import com.ouroboros.data.model.annotations.plugins.Scd2History;
import com.ouroboros.data.model.annotations.plugins.Scd2HistoryAnnotationDecorator;
import com.ouroboros.data.model.annotations.plugins.SoftDelete;
import com.ouroboros.data.model.annotations.plugins.SoftDeleteAnnotationDecorator;

class TypedPluginAnnotationDecoratorTest {

  @Test
  void auditFieldsAnnotationWritesDecoratorInputsOnly() {
    assertTrue(AuditFields.class.isAnnotationPresent(TypedModelPluginAnnotation.class));

    DataModelMeta meta = decorate(AuditAnnotationModel.class, new AuditFieldsAnnotationDecorator());

    assertEquals(true, meta.getExtraProp(Boolean.class, "enableAuditFields").orElse(false));
    Map<?, ?> config = assertConfigMap(meta, "basicAuditConfig");
    assertEquals("created_by_col", config.get("createdByRawName"));
    assertEquals("created_at_col", config.get("createdAtRawName"));
    assertTrue(meta.getPluginDescriptors().stream()
        .noneMatch(plugin -> "BasicAudit".equals(plugin.getName())));

    DataModelMeta decoratedMeta = DataModelMetaDecorator.applyDecorators(meta);
    assertEquals(1, decoratedMeta.getPluginDescriptors().stream()
        .filter(plugin -> "BasicAudit".equals(plugin.getName()))
        .count());
  }

  @Test
  void logicalDeleteAnnotationWritesDecoratorInputsOnly() {
    assertTrue(LogicalDelete.class.isAnnotationPresent(TypedModelPluginAnnotation.class));

    DataModelMeta meta = decorate(LogicalDeleteAnnotationModel.class, new LogicalDeleteAnnotationDecorator());

    assertEquals(true, meta.getExtraProp(Boolean.class, "enableSoftDelete").orElse(false));
    Map<?, ?> config = assertConfigMap(meta, "softDeleteConfig");
    assertEquals("deleted_flag", config.get("isDeletedRawName"));
    assertEquals(true, config.get("deletedByDisabled"));
    assertEquals("deleted_at_col", config.get("deletedAtRawName"));
    assertTrue(meta.getPluginDescriptors().stream()
        .noneMatch(plugin -> "LogicalDelete".equals(plugin.getName())));

    DataModelMeta decoratedMeta = DataModelMetaDecorator.applyDecorators(meta);
    assertEquals(1, decoratedMeta.getPluginDescriptors().stream()
        .filter(plugin -> "LogicalDelete".equals(plugin.getName()))
        .count());
  }

  @Test
  void legacySoftDeleteAnnotationWritesLogicalDeleteDecoratorInputsOnly() {
    assertTrue(SoftDelete.class.isAnnotationPresent(TypedModelPluginAnnotation.class));

    DataModelMeta meta = decorate(SoftDeleteAnnotationModel.class, new SoftDeleteAnnotationDecorator());

    assertEquals(true, meta.getExtraProp(Boolean.class, "enableSoftDelete").orElse(false));
    assertTrue(meta.getPluginDescriptors().stream()
        .noneMatch(plugin -> "SoftDelete".equals(plugin.getName())));

    DataModelMeta decoratedMeta = DataModelMetaDecorator.applyDecorators(meta);
    assertEquals(1, decoratedMeta.getPluginDescriptors().stream()
        .filter(plugin -> "LogicalDelete".equals(plugin.getName()))
        .count());
  }

  @Test
  void scd2HistoryAnnotationWithClassTargetWritesDecoratorInputsOnly() {
    assertTrue(Scd2History.class.isAnnotationPresent(TypedModelPluginAnnotation.class));

    DataModelMeta meta = decorate(Scd2SourceAnnotationModel.class, new Scd2HistoryAnnotationDecorator());

    assertEquals(true, meta.getExtraProp(Boolean.class, "enableScd2History").orElse(false));
    Map<?, ?> config = assertConfigMap(meta, "scd2HistoryConfig");
    assertEquals("demo.Scd2HistoryAnnotationModel", config.get("historyModelFullName"));
    assertTrue(meta.getPluginDescriptors().stream()
        .noneMatch(plugin -> "Scd2History".equals(plugin.getName())));

    DataModelMeta decoratedMeta = DataModelMetaDecorator.applyDecorators(meta);
    assertEquals(1, decoratedMeta.getPluginDescriptors().stream()
        .filter(plugin -> "Scd2History".equals(plugin.getName()))
        .count());
    assertEquals("demo.Scd2HistoryAnnotationModel", decoratedMeta.getPluginDescriptors().stream()
        .filter(plugin -> "Scd2History".equals(plugin.getName()))
        .findFirst()
        .orElseThrow(AssertionError::new)
        .getConfig()
        .get("historyModelFullName"));
  }

  @Test
  void scd2HistoryAnnotationWithStringTargetWritesDecoratorInputsOnly() {
    DataModelMeta meta = decorate(Scd2StringSourceAnnotationModel.class, new Scd2HistoryAnnotationDecorator());

    assertEquals(true, meta.getExtraProp(Boolean.class, "enableScd2History").orElse(false));
    Map<?, ?> config = assertConfigMap(meta, "scd2HistoryConfig");
    assertEquals("demo.ExternalHistoryModel", config.get("historyModelFullName"));
    assertTrue(meta.getPluginDescriptors().stream()
        .noneMatch(plugin -> "Scd2History".equals(plugin.getName())));
  }

  @Test
  void scd2HistoryAnnotationRejectsMissingTarget() {
    ModelMetadataException exception = assertThrows(
        ModelMetadataException.class,
        () -> new Scd2HistoryAnnotationDecorator().decorate(
            MissingScd2TargetModel.class,
            MissingScd2TargetModel.class.getAnnotation(Scd2History.class),
            emptyMeta(),
            decoratorContext()
        )
    );

    assertTrue(exception.getMessage().contains("historyModel"));
    assertTrue(exception.getMessage().contains("historyModelName"));
  }

  @Test
  void scd2HistoryAnnotationRejectsClassAndStringTargetsTogether() {
    ModelMetadataException exception = assertThrows(
        ModelMetadataException.class,
        () -> new Scd2HistoryAnnotationDecorator().decorate(
            DoubleScd2TargetModel.class,
            DoubleScd2TargetModel.class.getAnnotation(Scd2History.class),
            emptyMeta(),
            decoratorContext()
        )
    );

    assertTrue(exception.getMessage().contains("historyModel"));
    assertTrue(exception.getMessage().contains("historyModelName"));
  }

  @Test
  void archiveDeleteAnnotationWithClassTargetWritesDirectDescriptor() {
    assertTrue(ArchiveDelete.class.isAnnotationPresent(TypedModelPluginAnnotation.class));

    DataModelMeta meta = decorate(ArchiveDeleteSourceAnnotationModel.class, new ArchiveDeleteAnnotationDecorator());

    assertEquals(1, meta.getPluginDescriptors().stream()
        .filter(plugin -> "ArchiveDelete".equals(plugin.getName()))
        .count());
    assertEquals("demo.ArchiveDeleteArchiveAnnotationModel", meta.getPluginDescriptors().stream()
        .filter(plugin -> "ArchiveDelete".equals(plugin.getName()))
        .findFirst()
        .orElseThrow(AssertionError::new)
        .getConfig()
        .get("archiveModel"));
  }

  @Test
  void archiveDeleteAnnotationWithStringTargetWritesDirectDescriptor() {
    DataModelMeta meta = decorate(ArchiveDeleteStringSourceAnnotationModel.class, new ArchiveDeleteAnnotationDecorator());

    assertEquals(1, meta.getPluginDescriptors().stream()
        .filter(plugin -> "ArchiveDelete".equals(plugin.getName()))
        .count());
    assertEquals("demo.ExternalArchiveModel", meta.getPluginDescriptors().stream()
        .filter(plugin -> "ArchiveDelete".equals(plugin.getName()))
        .findFirst()
        .orElseThrow(AssertionError::new)
        .getConfig()
        .get("archiveModel"));
  }

  @Test
  void archiveDeleteAnnotationRejectsMissingTarget() {
    ModelMetadataException exception = assertThrows(
        ModelMetadataException.class,
        () -> new ArchiveDeleteAnnotationDecorator().decorate(
            MissingArchiveTargetModel.class,
            MissingArchiveTargetModel.class.getAnnotation(ArchiveDelete.class),
            emptyMeta(),
            decoratorContext()
        )
    );

    assertTrue(exception.getMessage().contains("archiveModel"));
    assertTrue(exception.getMessage().contains("archiveModelName"));
  }

  @Test
  void archiveDeleteAnnotationRejectsClassAndStringTargetsTogether() {
    ModelMetadataException exception = assertThrows(
        ModelMetadataException.class,
        () -> new ArchiveDeleteAnnotationDecorator().decorate(
            DoubleArchiveTargetModel.class,
            DoubleArchiveTargetModel.class.getAnnotation(ArchiveDelete.class),
            emptyMeta(),
            decoratorContext()
        )
    );

    assertTrue(exception.getMessage().contains("archiveModel"));
    assertTrue(exception.getMessage().contains("archiveModelName"));
  }

  private static <A extends Annotation> DataModelMeta decorate(
      Class<?> modelClass,
      TypedModelAnnotationDecorator<A> decorator
  ) {
    A annotation = modelClass.getAnnotation(decorator.annotationType());
    assertNotNull(annotation);
    return decorator.decorate(modelClass, annotation, emptyMeta(modelClass), decoratorContext(modelClass));
  }

  private static Map<?, ?> assertConfigMap(DataModelMeta meta, String key) {
    Object config = meta.getExtraProp(key).orElseThrow(AssertionError::new);
    assertInstanceOf(Map.class, config);
    return (Map<?, ?>) config;
  }

  private static DataModelMeta emptyMeta() {
    return emptyMeta(DirectDecoratorSource.class);
  }

  private static DataModelMeta emptyMeta(Class<?> modelClass) {
    DataModelMeta meta = new DataModelMeta();
    meta.setName(modelName(modelClass));
    return meta;
  }

  private static TypedAnnotationDecoratorContext decoratorContext() {
    return decoratorContext(DirectDecoratorSource.class);
  }

  private static TypedAnnotationDecoratorContext decoratorContext(Class<?> sourceModelClass) {
    String sourceModelName = modelName(sourceModelClass);
    return new TypedAnnotationDecoratorContext() {
      @Override
      public String modelName() {
        return sourceModelName;
      }

      @Override
      public String resolveModelName(Class<?> modelClass) {
        return TypedPluginAnnotationDecoratorTest.modelName(modelClass);
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

  private static String modelName(Class<?> modelClass) {
    Model model = modelClass.getAnnotation(Model.class);
    if (model == null || model.fullName().isBlank()) {
      return modelClass.getSimpleName();
    }
    return model.fullName();
  }

  @Model(fullName = "DirectDecoratorSource")
  static class DirectDecoratorSource {
  }

  @Model(fullName = "AuditAnnotationModel")
  @AuditFields(
      createdByRawName = "created_by_col",
      createdAtRawName = "created_at_col"
  )
  static class AuditAnnotationModel {
    @Field
    private String id;
  }

  @Model(fullName = "LogicalDeleteAnnotationModel")
  @LogicalDelete(
      isDeletedRawName = "deleted_flag",
      deletedByDisabled = true,
      deletedAtRawName = "deleted_at_col"
  )
  static class LogicalDeleteAnnotationModel {
    @Field
    private String id;
  }

  @Model(fullName = "SoftDeleteAnnotationModel")
  @SoftDelete
  static class SoftDeleteAnnotationModel {
    @Field
    private String id;
  }

  @Model(fullName = "Scd2SourceAnnotationModel")
  @Scd2History(historyModel = Scd2HistoryAnnotationModel.class)
  static class Scd2SourceAnnotationModel {
    @Field
    private String id;
  }

  @Model(fullName = "demo.Scd2HistoryAnnotationModel")
  static class Scd2HistoryAnnotationModel {
    @Field
    private String id;
  }

  @Model(fullName = "Scd2StringSourceAnnotationModel")
  @Scd2History(historyModelName = "demo.ExternalHistoryModel")
  static class Scd2StringSourceAnnotationModel {
    @Field
    private String id;
  }

  @Scd2History
  static class MissingScd2TargetModel {
  }

  @Scd2History(
      historyModel = Scd2HistoryAnnotationModel.class,
      historyModelName = "demo.OtherHistoryModel"
  )
  static class DoubleScd2TargetModel {
  }

  @Model(fullName = "ArchiveDeleteSourceAnnotationModel")
  @ArchiveDelete(archiveModel = ArchiveDeleteArchiveAnnotationModel.class)
  static class ArchiveDeleteSourceAnnotationModel {
    @Field
    private String id;
  }

  @Model(fullName = "demo.ArchiveDeleteArchiveAnnotationModel")
  static class ArchiveDeleteArchiveAnnotationModel {
    @Field
    private String id;
  }

  @Model(fullName = "ArchiveDeleteStringSourceAnnotationModel")
  @ArchiveDelete(archiveModelName = "demo.ExternalArchiveModel")
  static class ArchiveDeleteStringSourceAnnotationModel {
    @Field
    private String id;
  }

  @ArchiveDelete
  static class MissingArchiveTargetModel {
  }

  @ArchiveDelete(
      archiveModel = ArchiveDeleteArchiveAnnotationModel.class,
      archiveModelName = "demo.OtherArchiveModel"
  )
  static class DoubleArchiveTargetModel {
  }
}
