package com.ouroboros.data.station

import static org.junit.jupiter.api.Assertions.assertEquals
import static org.junit.jupiter.api.Assertions.assertNotNull

import com.ouroboros.data.util.DataJson

import org.junit.Test

class TestDataStationDefine {
    @Test
    void testSerializeDataStationDefine() {
        var define = new DataStationDefine()
        define.name = "test"
        define.label = "test"
        define.description = "test"
        define.type = "test"
        define.properties = ["A": 1, "B": 2]
        var json = DataJson.toJsonString(define)
        assertNotNull(json)
    }

    @Test
    void testDeserializeDataStationDefine() {
        var defineJsonString = '{"name": "test", "description": "test", "type": "test", "label": "test", "A": 1, "B": 2}'
        var define = new DataStationDefine()
        define.name = "test"
        define.label = "test"
        define.description = "test"
        define.type = "test"
        define.properties = ["A": 1, "B": 2]
        var result = DataJson.toBean(DataStationDefine.class, defineJsonString)
        assertEquals(define, result)
    }
}
