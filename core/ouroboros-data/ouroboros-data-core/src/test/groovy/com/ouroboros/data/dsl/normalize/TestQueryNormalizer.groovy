package com.ouroboros.data.normalize

import static org.junit.jupiter.api.Assertions.assertEquals
import static org.junit.jupiter.api.Assertions.assertFalse
import static org.junit.jupiter.api.Assertions.assertNotNull
import static org.junit.jupiter.api.Assertions.assertTrue

import com.querydsl.core.types.Ops
import com.ouroboros.data.dsl.Operators
import com.ouroboros.data.dsl.SExpression
import com.ouroboros.data.dsl.statement.QueryStatement

import org.junit.Test

// 测试原始 Query 归一化为 QueryStatement 功能
class TestQueryNormalizer {
    private static SExpression<?> field(String fieldName) {
        SExpressionBuilder.build(Operators.FIELD, fieldName).get()
    }

    private static SExpression<?> whereIdEq1() {
        SExpressionBuilder.build(Ops.EQ, field('id'), 1).get()
    }

    private static void assertNormalizedQuery(QueryStatement query, String tableName, String alias, SExpression<?> select, SExpression<?> where) {
        assertEquals(false, query.getDistinct())
        assertNotNull(query.getFrom())
        assertFalse(query.getFrom().isSubQuery())
        assertEquals(tableName, query.getFrom().getTableName())
        assertEquals(alias, query.getFrom().getAlias())
        assertEquals(1, query.getSelect().size())
        assertEquals(select, query.getSelect().get(0))
        assertEquals(where, query.getWhere())
    }

    @Test
    void testSimpleQuery() {
        var rawQuery1 = [
                select: ['id', [n: 'name']],
                from  : 'user',
                where : [id: 1]
        ]
        var rawQuery2 = [
                select: 'id, name as n',
                from  : 'user',
                where : [id: 1]
        ]
        var select = SExpressionBuilder.build(Operators.COLUMNS, 'id', [n: 'name']).get()
        var result = QueryNormalizer.normalize(rawQuery1)
        assertTrue(result.isSuccess(), "查询 ${rawQuery1} 解析失败")
        assertNormalizedQuery(result.get(), 'user', 'user', select, whereIdEq1())


        result = QueryNormalizer.normalize(rawQuery2)
        assertTrue(result.isSuccess(), "查询 ${rawQuery2} 解析失败")
        assertNormalizedQuery(result.get(), 'user', 'user', select, whereIdEq1())

    }

    @Test
    void testAliasQuery() {
        var rawQuery = [
                select: ['id', 'name'],
                from  : ['u': 'user'],
                where : [id: 1]
        ]
        var select = SExpressionBuilder.build(Operators.COLUMNS, 'id', 'name').get()
        var result = QueryNormalizer.normalize(rawQuery)
        assertTrue(result.isSuccess(), "查询 ${rawQuery} 解析失败")
        assertNormalizedQuery(result.get(), 'user', 'u', select, whereIdEq1())
    }

    @Test
    void testSubQuery() {
        var rawQuery = [
                select: ['id', 'name'],
                from  : ['u': [
                        select: ['id', 'name'],
                        from  : 'user',
                        where : [id: 1]
                ]],
                where : [id: 1]
        ]
        var select = SExpressionBuilder.build(Operators.COLUMNS, 'id', 'name').get()
        var result = QueryNormalizer.normalize(rawQuery)
        assertTrue(result.isSuccess(), "查询 ${rawQuery} 解析失败")
        var query = result.get()
        assertEquals(false, query.getDistinct())
        assertNotNull(query.getFrom())
        assertTrue(query.getFrom().isSubQuery())
        assertEquals('u', query.getFrom().getAlias())
        assertEquals(1, query.getSelect().size())
        assertEquals(select, query.getSelect().get(0))
        assertEquals(whereIdEq1(), query.getWhere())

        var subQuery = query.getFrom().getSubQuery()
        assertNormalizedQuery(subQuery, 'user', 'user', select, whereIdEq1())
    }
}
