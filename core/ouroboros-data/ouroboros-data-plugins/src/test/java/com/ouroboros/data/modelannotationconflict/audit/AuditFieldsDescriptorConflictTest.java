package com.ouroboros.data.modelannotationconflict.audit;

import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import org.junit.jupiter.api.Test;

import com.ouroboros.data.annotation.Field;
import com.ouroboros.data.annotation.Model;
import com.ouroboros.data.exception.ModelMetadataException;
import com.ouroboros.data.modelannotationconflict.DecoratorConflictTestSupport;
import com.ouroboros.data.annotation.AuditFields;
import com.ouroboros.data.model.decorators.typed.AuditFieldsAnnotationDecorator;
import com.ouroboros.data.annotation.DataPlugin;
import com.ouroboros.data.model.decorators.typed.DataPluginAnnotationDecorator;

class AuditFieldsDescriptorConflictTest {

  @Test
  void rejectsDecoratorBackedAnnotationAndDirectDataPluginConflict() {
    var context = DecoratorConflictTestSupport.context("AuditConflictModel");
    var meta = DecoratorConflictTestSupport.emptyMeta("AuditConflictModel");
    new AuditFieldsAnnotationDecorator().decorate(
        AuditConflictModel.class,
        AuditConflictModel.class.getAnnotation(AuditFields.class),
        meta,
        context
    );

    ModelMetadataException exception = assertThrows(
        ModelMetadataException.class,
        () -> new DataPluginAnnotationDecorator().decorate(
            AuditConflictModel.class,
            AuditConflictModel.class.getAnnotation(DataPlugin.class),
            meta,
            context
        )
    );

    assertTrue(exception.getMessage().contains("AuditConflictModel"));
    assertTrue(exception.getMessage().contains("BasicAudit"));
    assertTrue(exception.getMessage().contains("@AuditFields"));
    assertTrue(exception.getMessage().contains("@DataPlugin"));
  }

  @Model(fullName = "AuditConflictModel")
  @AuditFields
  @DataPlugin(name = "BasicAudit")
  static class AuditConflictModel {
    @Field
    private String id;
  }
}
