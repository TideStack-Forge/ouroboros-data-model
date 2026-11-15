package com.ouroboros.util

import static org.junit.jupiter.api.Assertions.assertEquals
import static org.junit.jupiter.api.Assertions.assertInstanceOf

import java.time.LocalDateTime

import com.ouroboros.util.Dates
import com.ouroboros.util.Maps

import org.junit.Test

class TestMaps {
    @Test
    void testFlattenMap() {
        var nestedMap = [id: 1, name: [like: 'Jack'], arr: [1, 2, [cc: 3]]]
        var flattenMap = [id: 1, 'name.like': 'Jack', 'arr[0]': 1, 'arr[1]': 2, 'arr[2].cc': 3]
        var result = Maps.Nested.flatten(nestedMap)
        assertEquals(flattenMap, result)
    }

    @Test
    void testBuildNestedMap() {
        var nestedMap = [id: 1, name: [like: 'Jack'], arr: [1, 2, [cc: 3]]]
        var flattenMap = [id: 1, 'name.like': 'Jack', 'arr[0]': 1, 'arr[1]': 2, 'arr[2].cc': 3]
        var result = Maps.Nested.build(flattenMap)
        assertEquals(nestedMap, result)
    }

    @Test
    void testBuildNestedMapWithSymbol() {
        var nestedMap = [id: ['>=': 1]]
        var flattenMap1 = ['id[>=]': 1]
        var flattenMap2 = ['id.>=': 1]
        var result1 = Maps.Nested.build(flattenMap1)
        var result2 = Maps.Nested.build(flattenMap2)
        assertEquals(nestedMap, result1)
        assertEquals(nestedMap, result2)
    }

    static class TestBean {
        String name
        String value
        LocalDateTime date
        Integer intValue
        List<TestBean2> list
        TestBean2 bean2
        Map<String, TestBean2> map
    }

    static class TestBean2 {
        String name
    }

    @Test
    void testConvertToBean() {
        var map = [name: 'Jack', value: 'JackValue', intValue: 1, date: Dates.now(), list: [[name: 'bean2inList']], bean2: [name: 'bean2', value: 'a']]
        TestBean result = Maps.toBean(TestBean.class, map)
        assertEquals('Jack', result.name)
        assertEquals('JackValue', result.value)
        assertEquals(1, result.intValue)
        assertEquals(map.date, result.date)
        assertEquals(map.list.size(), result.list.size())
        assertInstanceOf(TestBean2.class, result.list[0])
        assertEquals(map.list.get(0).name, result.list.get(0).name)
        assertEquals(map.bean2.name, result.bean2.name)
    }

    @Test
    void testConvertToMap() {
        var bean = new TestBean()
        bean.name = 'Jack'
        bean.value = 'JackValue'
        bean.intValue = 1
        bean.date = Dates.now()
        bean.bean2 = new TestBean2()
        bean.bean2.name = 'bean2'
        bean.list = [new TestBean2(name: 'bean2inList')]
        Map result = Maps.fromBean(bean)
        assertEquals('Jack', result.name)
        assertEquals('JackValue', result.value)
        assertEquals(1, result.intValue)
        assertEquals(bean.date, result.date)
        assertEquals(bean.list.size(), result.list.size())
        assertInstanceOf(Map.class, result.list[0])
        assertEquals(bean.list.get(0).name, result.list.get(0).name)
        assertEquals(bean.bean2.name, result.bean2.name)
    }


    @Test
    void testConvertToBean2() {
        TestBean2 bean2 = new TestBean2();
        bean2.name = 'bean2';

        List<TestBean2> list = new ArrayList<>();
        list.add(bean2);

        Map<String, TestBean2> map1 = new HashMap<>()
        map1.put('bean2', bean2)

        var map = [
                name    : 'Jack',
                value   : 'JackValue',
                intValue: 1,
                date    : Dates.now(),
                list    : list,
                bean2   : bean2,
                map     : map1
        ]

        TestBean result = Maps.toBean(TestBean.class, map)
        assertEquals('Jack', result.name)
        assertEquals('JackValue', result.value)
        assertEquals(1, result.intValue)
        assertEquals(map.date, result.date)
        assertEquals(map.list.size(), result.list.size())
        assertInstanceOf(TestBean2.class, result.list[0])
        assertEquals(map.list.get(0).name, result.list.get(0).name)
        assertEquals(map.bean2.name, result.bean2.name)
        assertEquals(map.map.bean2, result.map.bean2)
    }

    @Test
    void testDeepClone() {
        var map = [
                "list": [
                        [
                                "a": "aaa"
                        ]
                ],
                "map" : [
                        "b": "bbb"
                ]
        ]

        def clone = Maps.deepClone(map, HashMap::new)

        assertEquals(map.list[0].a, clone.list[0].a)
        assertEquals(map.map.b, clone.map.b)

        if (map.list.is(clone.list) && map.map.is(clone.map)) {
            throw new RuntimeException("list should not be same")
        }
    }

    @Test
    void testRemove() {
        var map = [
                "a": "aaa"
        ]

        Maps.remove(map, "a")

        assertEquals(null, map.a)
    }
}
