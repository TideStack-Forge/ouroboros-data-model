package com.ouroboros.data.modelannotationconflict.archive;

import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import org.junit.jupiter.api.Test;

import com.ouroboros.data.annotation.Field;
import com.ouroboros.data.annotation.Model;
import com.ouroboros.data.exception.ModelMetadataException;
import com.ouroboros.data.modelannotationconflict.DecoratorConflictTestSupport;
import com.ouroboros.data.annotation.ArchiveDelete;
import com.ouroboros.data.model.decorators.typed.ArchiveDeleteAnnotationDecorator;
import com.ouroboros.data.annotation.DataPlugin;
import com.ouroboros.data.model.decorators.typed.DataPluginAnnotationDecorator;

class ArchiveDeleteDescriptorConflictTest {

  @Test
  void rejectsArchiveDeleteAnnotationAndDirectDataPluginConflict() {
    var context = DecoratorConflictTestSupport.context("ArchiveConflictModel");
    var meta = DecoratorConflictTestSupport.emptyMeta("ArchiveConflictModel");
    new ArchiveDeleteAnnotationDecorator().decorate(
        ArchiveConflictModel.class,
        ArchiveConflictModel.class.getAnnotation(ArchiveDelete.class),
        meta,
        context
    );

    ModelMetadataException exception = assertThrows(
        ModelMetadataException.class,
        () -> new DataPluginAnnotationDecorator().decorate(
            ArchiveConflictModel.class,
            ArchiveConflictModel.class.getAnnotation(DataPlugin.class),
            meta,
            context
        )
    );

    assertTrue(exception.getMessage().contains("ArchiveConflictModel"));
    assertTrue(exception.getMessage().contains("ArchiveDelete"));
    assertTrue(exception.getMessage().contains("@ArchiveDelete"));
    assertTrue(exception.getMessage().contains("@DataPlugin"));
  }

  @Model(fullName = "ArchiveConflictModel")
  @ArchiveDelete(archiveModelName = "demo.Archive")
  @DataPlugin(name = "ArchiveDelete", config = {"archiveModel", "demo.OtherArchive"})
  static class ArchiveConflictModel {
    @Field
    private String id;
  }
}
