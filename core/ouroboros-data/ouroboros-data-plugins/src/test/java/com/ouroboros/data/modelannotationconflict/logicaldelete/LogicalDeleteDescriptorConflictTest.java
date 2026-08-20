package com.ouroboros.data.modelannotationconflict.logicaldelete;

import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import org.junit.jupiter.api.Test;

import com.ouroboros.data.annotation.Field;
import com.ouroboros.data.annotation.Model;
import com.ouroboros.data.exception.ModelMetadataException;
import com.ouroboros.data.annotation.DataPlugin;
import com.ouroboros.data.model.decorators.typed.DataPluginAnnotationDecorator;
import com.ouroboros.data.annotation.LogicalDelete;
import com.ouroboros.data.model.decorators.typed.LogicalDeleteAnnotationDecorator;
import com.ouroboros.data.modelannotationconflict.DecoratorConflictTestSupport;

class LogicalDeleteDescriptorConflictTest {

  @Test
  void rejectsLogicalDeleteAnnotationAndDirectDataPluginConflict() {
    var context = DecoratorConflictTestSupport.context("LogicalDeleteConflictModel");
    var meta = DecoratorConflictTestSupport.emptyMeta("LogicalDeleteConflictModel");
    new LogicalDeleteAnnotationDecorator().decorate(
        LogicalDeleteConflictModel.class,
        LogicalDeleteConflictModel.class.getAnnotation(LogicalDelete.class),
        meta,
        context
    );

    ModelMetadataException exception = assertThrows(
        ModelMetadataException.class,
        () -> new DataPluginAnnotationDecorator().decorate(
            LogicalDeleteConflictModel.class,
            LogicalDeleteConflictModel.class.getAnnotation(DataPlugin.class),
            meta,
            context
        )
    );

    assertTrue(exception.getMessage().contains("LogicalDeleteConflictModel"));
    assertTrue(exception.getMessage().contains("LogicalDelete"));
    assertTrue(exception.getMessage().contains("@LogicalDelete"));
    assertTrue(exception.getMessage().contains("@DataPlugin"));
  }

  @Test
  void rejectsLogicalDeleteAnnotationAndLegacyDirectDataPluginConflict() {
    var context = DecoratorConflictTestSupport.context("LegacySoftDeleteConflictModel");
    var meta = DecoratorConflictTestSupport.emptyMeta("LegacySoftDeleteConflictModel");
    new LogicalDeleteAnnotationDecorator().decorate(
        LegacySoftDeleteConflictModel.class,
        LegacySoftDeleteConflictModel.class.getAnnotation(LogicalDelete.class),
        meta,
        context
    );

    ModelMetadataException exception = assertThrows(
        ModelMetadataException.class,
        () -> new DataPluginAnnotationDecorator().decorate(
            LegacySoftDeleteConflictModel.class,
            LegacySoftDeleteConflictModel.class.getAnnotation(DataPlugin.class),
            meta,
            context
        )
    );

    assertTrue(exception.getMessage().contains("LegacySoftDeleteConflictModel"));
    assertTrue(exception.getMessage().contains("SoftDelete"));
    assertTrue(exception.getMessage().contains("@LogicalDelete"));
    assertTrue(exception.getMessage().contains("@DataPlugin"));
  }

  @Model(fullName = "LogicalDeleteConflictModel")
  @LogicalDelete
  @DataPlugin(name = "LogicalDelete")
  static class LogicalDeleteConflictModel {
    @Field
    private String id;
  }

  @Model(fullName = "LegacySoftDeleteConflictModel")
  @LogicalDelete
  @DataPlugin(name = "SoftDelete")
  static class LegacySoftDeleteConflictModel {
    @Field
    private String id;
  }
}
