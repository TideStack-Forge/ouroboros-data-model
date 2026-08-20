package com.ouroboros.data.transpile

import static org.junit.jupiter.api.Assertions.assertEquals
import static org.junit.jupiter.api.Assertions.assertTrue

import io.vavr.Tuple3
import io.vavr.control.Try

import com.ouroboros.data.normalize.QueryNormalizeContext
import com.querydsl.core.types.Expression
import com.querydsl.core.types.PathType
import com.querydsl.core.types.dsl.Expressions
import com.querydsl.sql.MySQLTemplates
import com.querydsl.sql.SQLQuery

import org.junit.Test

class TestQueryTranspiler {
    Try<Expression<?>> simplePathResolver(Tuple3<PathType, String, String> pathTuple) {
        var path = pathTuple._2().split("\\.").toList()
        var alias = pathTuple._3()
        var parent = path.size() == 1 ? null : Expressions.path(Object.class, path.subList(0, path.size() - 1).join("."))
        var name = path.get(path.size() - 1)
        var pathExpr = Expressions.path(Object.class, parent, name)

        return Try.success(name == alias ? pathExpr : pathExpr.as(alias))
    }

    @Test
    void testSimpleQuery1() {
        var query = QueryNormalizeContext.builder().withDefaultNormalizers().build().normalizeQuery([
                select: "id,name",
                from  : "user",
                where : [
                        id: [
                                gt: 1
                        ]
                ]
        ]).get()
        var sqlStr = "select user.id, user.name\n" +
                "from user\n" +
                "where user.id > ?"
        var queryMetadata = QueryTranspiler.transpile(query, this::simplePathResolver)
        assertTrue(queryMetadata.isSuccess())
        var sql = new SQLQuery(null, MySQLTemplates.DEFAULT, queryMetadata.get()).getSQL();
        assertEquals(sqlStr, sql.getSQL())
    }

    @Test
    void testSimpleQuery2() {
        var query = QueryNormalizeContext.builder().withDefaultNormalizers().build().normalizeQuery([
                select: "id,name",
                from  : [u: "user"],
                where : [
                        id: [
                                gt: 1
                        ]
                ]
        ]).get()
        var sqlStr = "select u.id, u.name\n" +
                "from user as u\n" +
                "where u.id > ?"
        var queryMetadata = QueryTranspiler.transpile(query, this::simplePathResolver)
        assertTrue(queryMetadata.isSuccess())
        var sql = new SQLQuery(null, MySQLTemplates.DEFAULT, queryMetadata.get()).getSQL();
        assertEquals(sqlStr, sql.getSQL())
    }
}
