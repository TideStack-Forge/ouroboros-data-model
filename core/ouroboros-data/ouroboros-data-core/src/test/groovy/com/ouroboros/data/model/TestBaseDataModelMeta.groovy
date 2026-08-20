package com.ouroboros.data.model

import static org.junit.jupiter.api.Assertions.assertNotNull

import com.fasterxml.jackson.databind.ObjectMapper
import com.ouroboros.data.util.DataJson

import org.junit.Test

class TestBaseDataModelMeta {
    @Test
    void testSerializeSimpleModelMeta() {
        var meta = new DataModelMeta()
        meta.name = "testName"
        meta.label = "testLabel"
        meta.description = "testDescription"
        meta.migrationStrategy = MigrationStrategy.AUTO
        var fieldA = new DataModelFieldMeta()
        fieldA.name = "fieldA"
        fieldA.label = "fieldALabel"
        fieldA.description = "fieldADescription"
        fieldA.type = "fieldAType"
        fieldA.defaultValue = "fieldADefaultValue"
        fieldA.isNullable = true
        fieldA.extraProps = ["fieldAExtraProp1": "fieldAExtraProp1Value", "fieldAExtraProp2": "fieldAExtraProp2Value"]
        meta.fields = [fieldA]
        var json = DataJson.toPrettyJsonString(meta)
        ObjectMapper objectMapper = new ObjectMapper()
        var meta2 = objectMapper.readValue(json, DataModelMeta.class)
        assertNotNull(json)
    }
}
