package com.ouroboros.data.modelannotationconflict.softdelete;

import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import org.junit.jupiter.api.Test;

import com.ouroboros.data.annotation.Field;
import com.ouroboros.data.annotation.Model;
import com.ouroboros.data.exception.ModelMetadataException;
import com.ouroboros.data.modelannotationconflict.DecoratorConflictTestSupport;
import com.ouroboros.data.model.annotations.plugins.DataPlugin;
import com.ouroboros.data.model.annotations.plugins.DataPluginAnnotationDecorator;
import com.ouroboros.data.model.annotations.plugins.SoftDelete;
import com.ouroboros.data.model.annotations.plugins.SoftDeleteAnnotationDecorator;

class SoftDeleteDescriptorConflictTest {

  @Test
  void rejectsSoftDeleteAnnotationAndLegacyDirectDataPluginConflict() {
    var context = DecoratorConflictTestSupport.context("SoftDeleteConflictModel");
    var meta = DecoratorConflictTestSupport.emptyMeta("SoftDeleteConflictModel");
    new SoftDeleteAnnotationDecorator().decorate(
        SoftDeleteConflictModel.class,
        SoftDeleteConflictModel.class.getAnnotation(SoftDelete.class),
        meta,
        context
    );

    ModelMetadataException exception = assertThrows(
        ModelMetadataException.class,
        () -> new DataPluginAnnotationDecorator().decorate(
            SoftDeleteConflictModel.class,
            SoftDeleteConflictModel.class.getAnnotation(DataPlugin.class),
            meta,
            context
        )
    );

    assertTrue(exception.getMessage().contains("SoftDeleteConflictModel"));
    assertTrue(exception.getMessage().contains("SoftDelete"));
    assertTrue(exception.getMessage().contains("@SoftDelete"));
    assertTrue(exception.getMessage().contains("@DataPlugin"));
  }

  @Model(fullName = "SoftDeleteConflictModel")
  @SoftDelete
  @DataPlugin(name = "SoftDelete")
  static class SoftDeleteConflictModel {
    @Field
    private String id;
  }
}
