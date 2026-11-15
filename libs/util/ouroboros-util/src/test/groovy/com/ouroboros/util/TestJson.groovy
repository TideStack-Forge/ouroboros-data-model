package com.ouroboros.util

import static org.junit.jupiter.api.Assertions.assertEquals

import com.ouroboros.util.Dates
import com.ouroboros.util.Json

import org.junit.Test

class TestJson {
    @Test
    void testToString() {
        // 简单对象
        var json1 = Json.toJsonString([id: 1, name: 'Jack'])
        assertEquals('{"id":1,"name":"Jack"}', json1)

        // LocalDateTime、LocalDate
        var json2 = Json.toJsonString([id: 1, name: 'Jack', createdAt: Dates.toLocalDateTime('2025-09-01T14:37:33.857727')])
        var json3 = Json.toJsonString([id: 1, name: 'Jack', createdAt: Dates.toLocalDateTime('2025-09-01')])
        var json4 = Json.toJsonString([id: 1, name: 'Jack', createdAt: Dates.toLocalDate('2025-09-01')])
        assertEquals('{"id":1,"name":"Jack","createdAt":"2025-09-01T14:37:33.857727"}', json2)
        assertEquals('{"id":1,"name":"Jack","createdAt":"2025-09-01T00:00"}', json3)
        assertEquals('{"id":1,"name":"Jack","createdAt":"2025-09-01"}', json4)

        // Date
        var json5 = Json.toJsonString([id: 1, name: 'Jack', createdAt: Dates.toDate('2025-09-01T14:37:33.857')])
        assertEquals('{"id":1,"name":"Jack","createdAt":"2025-09-01T14:37:33.857"}', json5)

        // 嵌套Map
        var json6 = Json.toJsonString([id: 1, name: 'Jack', user: [id: 1, name: 'Jack']])
        assertEquals('{"id":1,"name":"Jack","user":{"id":1,"name":"Jack"}}', json6)

        // 嵌套简单List
        var json7 = Json.toJsonString([id: 1, name: 'Jack', users: [1, 2, 3]])
        assertEquals('{"id":1,"name":"Jack","users":[1,2,3]}', json7)

        // 嵌套List
        var json8 = Json.toJsonString([id: 1, name: 'Jack', users: [[id: 1, name: 'Jack'], [id: 2, name: 'Rose']]])
        assertEquals('{"id":1,"name":"Jack","users":[{"id":1,"name":"Jack"},{"id":2,"name":"Rose"}]}', json8)
    }

    @Test
    void testToMap() {
        // 简单Map
        var json1 = Json.toJsonString([id: 1, name: 'Jack'])
        var map1 = Json.toMap(json1)
        assertEquals([id: 1, name: 'Jack'], map1)

        // 嵌套Map
        var json2 = Json.toJsonString([id: 1, name: 'Jack', user: [id: 1, name: 'Jack']])
        var map2 = Json.toMap(json2)
        assertEquals([id: 1, name: 'Jack', user: [id: 1, name: 'Jack']], map2)

        // 简单嵌套List
        var json3 = Json.toJsonString([id: 1, name: 'Jack', users: [1, 2, 3]])
        var map3 = Json.toMap(json3)
        assertEquals([id: 1, name: 'Jack', users: [1, 2, 3]], map3)

        // 嵌套List
        var json4 = Json.toJsonString([id: 1, name: 'Jack', users: [[id: 1, name: 'Jack'], [id: 2, name: 'Rose']]])
        var map4 = Json.toMap(json4)
        assertEquals([id: 1, name: 'Jack', users: [[id: 1, name: 'Jack'], [id: 2, name: 'Rose']]], map4)
    }

    @Test
    void testToList() {
        // 简单List
        var json1 = Json.toJsonString([1, 2, 3])
        var list1 = Json.toList(json1)
        assertEquals([1, 2, 3], list1)

        // 嵌套List
        var json2 = Json.toJsonString([[id: 1, name: 'Jack'], [id: 2, name: 'Rose']])
        var list2 = Json.toList(json2)
        assertEquals([[id: 1, name: 'Jack'], [id: 2, name: 'Rose']], list2)
    }

    @Test
    void testToJsonBag() {
        // 简单Map
        var json1 = Json.toJsonString([id: 1, name: 'Jack'])
        var bag1 = Json.toJsonBag(json1)
        assertEquals([id: 1, name: 'Jack'], bag1.getMap())

        // 简单List
        var json2 = Json.toJsonString([1, 2, 3])
        var bag2 = Json.toJsonBag(json2)
        assertEquals([1, 2, 3], bag2.getList())
//
//    // 嵌套Map
//    var json2 = Json.toJsonString([id: 1, name: 'Jack', user: [id: 1, name: 'Jack']])
//    var bag2 = Json.toJsonBag(json2)
//    assertEquals([id: 1, name: 'Jack', user: [id: 1, name: 'Jack']], bag2)
//
//    // 简单嵌套List
//    var json3 = Json.toJsonString([id: 1, name: 'Jack', users: [1,2,3]])
//    var bag3 = Json.toJsonBag(json3)
//    assertEquals([id: 1, name: 'Jack', users: [1,2,3]], bag3)
//
//    // 嵌套List
//    var json4 = Json.toJsonString([id: 1, name: 'Jack', users: [[id: 1, name: 'Jack'], [id: 2, name: 'Rose']]])
//    var bag4 = Json.toJsonBag(json4)
//    assertEquals([id: 1, name: 'Jack', users: [[id: 1, name: 'Jack'], [id: 2, name: 'Rose']]], bag4)
    }
}
