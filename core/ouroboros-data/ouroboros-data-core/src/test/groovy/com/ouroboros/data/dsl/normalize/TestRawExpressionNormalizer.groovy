package com.ouroboros.data.normalize

import static org.junit.jupiter.api.Assertions.*

import com.ouroboros.data.dsl.Operators
import com.ouroboros.data.dsl.SExpression
import com.querydsl.core.types.Ops

import org.junit.Test

// 原始表达式归一化为SExpression相关功能
class TestRawExpressionNormalizer {
    private static SExpression<?> field(String fieldName) {
        SExpressionBuilder.build(Operators.FIELD, fieldName).get()
    }

    private static SExpression<?> eq(String fieldName, Object value) {
        SExpressionBuilder.build(Ops.EQ, field(fieldName), value).get()
    }

    // 测试空表达式
    @Test
    void testEmpty() {
        var result = RawExpressionNormalizer.normalize(Collections.emptyMap())
        assertTrue(result.isSuccess(), "Map空表达式解析失败")
        assertTrue(result.get().isEmpty(), "Map空表达式解析错误")

        var result2 = RawExpressionNormalizer.normalize(Collections.emptyList())
        assertFalse(result2.isSuccess(), "List空表达式应解析失败")

        var result3 = RawExpressionNormalizer.normalize(null)
        assertTrue(result3.isSuccess(), "null空表达式解析失败")
        assertTrue(result3.get().isEmpty(), "null空表达式解析错误")
    }

    // 测试单一 Entry 的Map表达式
    @Test
    void testSingleEntryMapExpression() {
        def expr = [id: 1]
        def sExpr = eq('id', 1)

        def result = RawExpressionNormalizer.normalize(expr)
        assertTrue(result.isSuccess(), "表达式 ${expr} 解析失败")
        assertEquals(sExpr, result.get(), "表达式 ${expr} 解析错误")
    }

    // 测试多 Entry 的 Map 表达式
    @Test
    void testMultipleEntryMapExpression() {
        def expr = [id: 1, name: 'Jack']
        def sExpr1 = eq('id', 1)
        def sExpr2 = eq('name', 'Jack')
        def sExpr = SExpressionBuilder.build(Ops.AND, sExpr1, sExpr2).get()

        def result = RawExpressionNormalizer.normalize(expr)
        assertTrue(result.isSuccess(), "表达式 ${expr} 解析失败")
        assertEquals(sExpr, result.get(), "表达式 ${expr} 解析错误")
    }

    // 测试非法 Map 表达式
    @Test
    void testIllegalMapExpression() {
        def expr = [id: 1, name: [eq: 'Jack', and: [name: 'Jack']]]

        def result = RawExpressionNormalizer.normalize(expr)
        assertFalse(result.isSuccess(), "表达式 ${expr} 解析错误")
    }

    // 比较表达式，如 > < like leftLike rightLike between 的 Map 表达式
    @Test
    void testCompareExpression() {
        def expr = [
                id  : ['>': 1, '<': 2, '>=': 5, '<=': 10],
                name: [like: 'Jack', leftLike: 'Jack', rightLike: 'Jack'],
                age : [between: [18, 30]],
                x   : [1, 2, 3, 4]
        ]
        def id = field('id')
        def name = field('name')
        def age = field('age')
        def x = field('x')
        def idGT = SExpressionBuilder.build(Ops.GT, id, 1).get()
        def idLT = SExpressionBuilder.build(Ops.LT, id, 2).get()
        def idGOE = SExpressionBuilder.build(Ops.GOE, id, 5).get()
        def idLOE = SExpressionBuilder.build(Ops.LOE, id, 10).get()
        def nameLike = SExpressionBuilder.build(Ops.LIKE, name, 'Jack').get()
        def nameLeftLike = SExpressionBuilder.build(Ops.STARTS_WITH, name, 'Jack').get()
        def nameRightLike = SExpressionBuilder.build(Ops.ENDS_WITH, name, 'Jack').get()
        def ageExpr = SExpressionBuilder.build(Ops.BETWEEN, age, 18, 30).get()
        def xExpr = SExpressionBuilder.build(Ops.IN, x, [1, 2, 3, 4]).get()
        def sExpr = SExpressionBuilder.build(Ops.AND, idGT, idLT, idGOE, idLOE, nameLike, nameLeftLike, nameRightLike, ageExpr, xExpr).get()

        def result = RawExpressionNormalizer.normalize(expr)
        assertTrue(result.isSuccess(), "表达式 ${expr} 解析失败")
        assertEquals(sExpr, result.get(), "表达式 ${expr} 解析错误")
    }

    // Fields表达式
    @Test
    void testSingleFieldExpression() {
        def expr1 = ['id']
        def expr2 = ['column', 'id']
        def sExpr = field('id')

        def result1 = RawExpressionNormalizer.normalize(expr1)
        def result2 = RawExpressionNormalizer.normalize(expr2)

        assertFalse(result1.isSuccess(), "表达式 ${expr1} 应解析失败")
        assertTrue(result2.isSuccess(), "表达式 ${expr2} 解析失败")
        assertEquals(sExpr, result2.get(), "表达式 ${expr2} 解析错误")
    }

    // 多字段
    @Test
    void testMultipleFieldsExpression() {
        def expr1 = ['id', 'name', 'age']
        def expr2 = ['columns', 'id', 'name', 'age']
        def sExpr = SExpressionBuilder.build(Operators.COLUMNS, 'id', 'name', 'age').get()

        def result1 = RawExpressionNormalizer.normalize(expr1)
        def result2 = RawExpressionNormalizer.normalize(expr2)

        assertFalse(result1.isSuccess(), "表达式 ${expr1} 应解析失败")
        assertTrue(result2.isSuccess(), "表达式 ${expr2} 解析失败")
        assertEquals(sExpr, result2.get(), "表达式 ${expr2} 解析错误")
    }

    // 包含别名的多字段
    @Test
    void testMultipleFieldsWithAliasExpression() {
        def expr1 = [[ID: 'id'], 'name', 'age']
        def expr2 = ['columns', [ID: 'id'], 'name', 'age']
        def sExpr = SExpressionBuilder.build(Operators.COLUMNS, [ID: 'id'], 'name', 'age').get()

        def result1 = RawExpressionNormalizer.normalize(expr1)
        def result2 = RawExpressionNormalizer.normalize(expr2)

        assertFalse(result1.isSuccess(), "表达式 ${expr1} 应解析失败")
        assertTrue(result2.isSuccess(), "表达式 ${expr2} 解析失败")
        assertEquals(sExpr, result2.get(), "表达式 ${expr2} 解析错误")
    }

    // Not 表达式
    @Test
    void testNotMapExpression() {
        def expr0 = [not: [id: 1]]
        def expr1 = [not: [[id: 1], [name: 'Jack']]]
        def expr2 = [not: [id: 1, name: 'Jack']]
        def sExpr1 = eq('id', 1)
        def sExpr2 = eq('name', 'Jack')
        def sExpr0 = sExpr1
        def sExpr = SExpressionBuilder.build(Ops.NOT, SExpressionBuilder.build(Ops.AND, sExpr1, sExpr2).get()).get()

        def result0 = RawExpressionNormalizer.normalize(expr0)
        def result1 = RawExpressionNormalizer.normalize(expr1)
        def result2 = RawExpressionNormalizer.normalize(expr2)

        assertTrue(result0.isSuccess(), "表达式 ${expr0} 解析失败")
        assertTrue(result1.isSuccess(), "表达式 ${expr1} 解析失败")
        assertTrue(result2.isSuccess(), "表达式 ${expr2} 解析失败")

        assertEquals(sExpr0, result0.get(), "表达式 ${expr0} 解析错误")
        assertEquals(sExpr, result1.get(), "表达式 ${expr1} 解析错误")
        assertEquals(sExpr, result2.get(), "表达式 ${expr2} 解析错误")
    }

    // 逻辑组合表达式
    @Test
    void testAndOrXorXNorMapExpression() {
        def runTest = {
            var opKey = it.toString().toLowerCase().toString()
            def expr1 = ["${opKey}": [[id: 1], [name: 'Jack']]]
            def expr2 = ["${opKey}": [id: 1, name: 'Jack']]
            def sExpr1 = eq('id', 1)
            def sExpr2 = eq('name', 'Jack')
            def sExpr = SExpressionBuilder.build(it, sExpr1, sExpr2).get()

            def result1 = RawExpressionNormalizer.normalize(expr1)
            def result2 = RawExpressionNormalizer.normalize(expr2)
            assertTrue(result1.isSuccess(), "表达式 ${expr1} 解析失败")
            assertTrue(result1.isSuccess(), "表达式 ${expr2} 解析失败")

            assertEquals(sExpr, result1.get(), "表达式 ${expr1} 解析错误")
            assertEquals(sExpr, result2.get(), "表达式 ${expr2} 解析错误")
        }
        runTest.call(Ops.AND)
        runTest.call(Ops.OR)
        runTest.call(Ops.XOR)
        runTest.call(Ops.XNOR)
    }

    // S/M混合表达式
    @Test
    void testHybridExpression() {
        def expr1 = ['OR', ['=', ['column', 'id'], 1], [name: 'Jack']]
        def expr2 = ['OR': [['=', ['column', 'id'], 1], [name: 'Jack']]]
        def sExpr1 = eq('id', 1)
        def sExpr2 = eq('name', 'Jack')
        def sExpr = SExpressionBuilder.build(Ops.OR, sExpr1, sExpr2).get()

        def result1 = RawExpressionNormalizer.normalize(expr1)
        def result2 = RawExpressionNormalizer.normalize(expr2)

        assertTrue(result1.isSuccess(), "表达式 ${expr1} 解析失败")
        assertEquals(sExpr, result1.get(), "表达式 ${expr1} 解析错误")
        assertTrue(result2.isSuccess(), "表达式 ${expr2} 解析失败")
        assertEquals(sExpr, result2.get(), "表达式 ${expr2} 解析错误")
    }

}
