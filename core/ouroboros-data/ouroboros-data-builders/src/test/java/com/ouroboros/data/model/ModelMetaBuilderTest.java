package com.ouroboros.data.model;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import org.junit.jupiter.api.Test;

class ModelMetaBuilderTest {

  @Test
  void builderSupportsFieldUniquenessScopeAndModelUniqueConstraints() {
    DataModelMeta meta = ModelMetaBuilder.create("ProjectNode")
        .fields()
        .longField("projectId").end()
        .stringField("code").unique(UniquenessScope.ACTIVE_RECORDS).end()
        .end()
        .uniqueConstraint("project_code", UniquenessScope.ACTIVE_RECORDS, "projectId", "code")
        .build();

    assertEquals(2, meta.getFields().size());
    assertTrue(meta.getFields().get(1).getIsUnique());
    assertEquals(UniquenessScope.ACTIVE_RECORDS, meta.getFields().get(1).getUniquenessScope());
    assertEquals(1, meta.getUniqueConstraints().size());
    assertEquals("project_code", meta.getUniqueConstraints().get(0).getName());
    assertEquals(UniquenessScope.ACTIVE_RECORDS, meta.getUniqueConstraints().get(0).getScope());
    assertEquals("projectId", meta.getUniqueConstraints().get(0).getFields().get(0));
    assertEquals("code", meta.getUniqueConstraints().get(0).getFields().get(1));
  }
}
