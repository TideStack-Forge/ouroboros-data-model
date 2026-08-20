package com.ouroboros.data.transpile

import static org.junit.jupiter.api.Assertions.assertEquals
import static org.junit.jupiter.api.Assertions.assertTrue

import java.util.function.Supplier

import io.vavr.Tuple3
import io.vavr.control.Try

import com.ouroboros.data.dsl.SExpression
import com.ouroboros.data.normalize.QueryNormalizeContext
import com.querydsl.core.types.Expression
import com.querydsl.core.types.Ops
import com.querydsl.core.types.PathType
import com.querydsl.core.types.Projections
import com.querydsl.core.types.dsl.Expressions

import org.junit.Test

// 测试 SExpression 转译 QueryDSL Expression 功能
class TestSExpressionTranspiler {

    private def ctx = QueryNormalizeContext.builder().withDefaultNormalizers().build()

    private def normalize(Object raw) {
        return ctx.forClause("WHERE").normalizeExpression(raw, "test")
    }

    Try<Expression<?>> simplePathResolver(Tuple3<PathType, String, String> pathTuple) {
        var path = pathTuple._2().split("\\.").toList()
        var parent = path.size() == 1 ? null : Expressions.path(Object.class, path.subList(0, path.size() - 1).join("."))
        var name = path.get(path.size() - 1)
        return Try.success(Expressions.path(Object.class, parent, name))
    }

    // 测试空表达式
    @Test
    void testEmptyExpression() {

        var pathRaw = ['id']
        var pathSExpr = normalize(pathRaw).get()
        var pathA = SExpressionTranspiler.transpile(pathSExpr, this::simplePathResolver).get()
        var pathB = SExpressionTranspiler.transpile(pathSExpr, this::simplePathResolver).get()
        assertEquals(pathA, pathB)

        var expression = SExpression.empty()
        var expr = SExpressionTranspiler.transpile(expression, this::simplePathResolver)
        assertTrue(expr.isSuccess())
        assertEquals(Expressions.asBoolean(true), expr.get(), "空表达式转译结果不正确")
    }

    // 简单比较表达式
    @Test
    void testSimpleComparator() {
        def sexpr = normalize([id: 1, name: 'Jack', age: [goe: 18, lt: 30]]).getOrElseThrow({ it -> it } as Supplier)
        def idExpr = Expressions.booleanOperation(Ops.EQ, Expressions.numberPath('id'), Expressions.constant(1))
        def nameExpr = Expressions.booleanOperation(Ops.EQ, Expressions.stringPath('name'), Expressions.constant('Jack'))
        def agePath = Expressions.numberPath('age', null)
        def ageExpr1 = Expressions.booleanOperation(Ops.GOE, agePath, Expressions.constant(18))
        def ageExpr2 = Expressions.booleanOperation(Ops.LT, agePath, Expressions.constant(30))
        def ageExpr = Expressions.booleanOperation(Ops.AND, ageExpr1, ageExpr2)
        def expr = Expressions.booleanOperation(Ops.AND, idExpr, nameExpr)
        expr = Expressions.booleanOperation(Ops.AND, expr, ageExpr)
        def result = SExpressionTranspiler.transpile(sexpr, this::simplePathResolver)
        assertTrue(result.isSuccess(), "表达式 ${sexpr} 转译失败")
        assertEquals(expr, result.get(), "表达式 ${sexpr} 转译错误")
    }

    // BETWEEN 表达式
    @Test
    void testBetween() {
        def sexpr = normalize([age: [between: [18, 30]]]).getOrElseThrow({ it -> it } as Supplier)
        var age = Expressions.numberPath('age', null)
        def expr = Expressions.booleanOperation(Ops.BETWEEN, age, Expressions.constant(18), Expressions.constant(30))
        def result = SExpressionTranspiler.transpile(sexpr, this::simplePathResolver)
        assertTrue(result.isSuccess(), "表达式 ${sexpr} 转译失败")
        assertEquals(expr, result.get(), "表达式 ${sexpr} 转译错误")
    }

    // IN 表达式
    @Test
    void testIn() {
        def sexpr = normalize([age: [in: [18, 19, 25, 30]]]).getOrElseThrow({ it -> it } as Supplier)
        var age = Expressions.numberPath('age', null)
        def expr = Expressions.booleanOperation(Ops.IN, age, Expressions.constant([18, 19, 25, 30]))

        def result = SExpressionTranspiler.transpile(sexpr, this::simplePathResolver)
        assertTrue(result.isSuccess(), "表达式 ${sexpr} 转译失败")
        assertEquals(expr, result.get(), "表达式 ${sexpr} 转译错误")
    }

    // FIELDS、ALIAS 表达式
    @Test
    void testFieldAlias() {
        def sexpr = normalize([[id2: 'id'], 'name']).get()
        def idExpr = Expressions.as(Expressions.path(Object.class, 'id'), 'id2')
        def nameExpr = Expressions.path(Object.class, 'name')
        def expr = Projections.list(idExpr, nameExpr)

        def result = SExpressionTranspiler.transpile(sexpr, this::simplePathResolver)
        assertTrue(result.isSuccess(), "表达式 ${sexpr} 转译失败")
        assertEquals(expr.toString(), result.get().toString(), "表达式 ${sexpr} 转译错误")
    }

    // 逻辑组合表达式
    @Test
    void testLogicCombination() {
        def sexpr0 = normalize([and: [id: 1, name: 'Jack']]).getOrElseThrow({ it -> it } as Supplier)
        def sexpr1 = normalize([or: [id: 1, name: 'Jack']]).getOrElseThrow({ it -> it } as Supplier)
        def sexpr2 = normalize([xor: [id: 1, name: 'Jack']]).getOrElseThrow({ it -> it } as Supplier)
        def sexpr3 = normalize([xnor: [id: 1, name: 'Jack']]).getOrElseThrow({ it -> it } as Supplier)
        def sexpr4 = normalize([not: [id: 1]]).getOrElseThrow({ it -> it } as Supplier)
        def sexpr5 = normalize([not: [id: 1, name: 'Jack']]).getOrElseThrow({ it -> it } as Supplier)
        def sexpr6 = normalize([or: [[id: 1, name: 'Jack'], [id: [between: [100, 200]]]]]).getOrElseThrow({ it -> it } as Supplier)

        def idExpr = Expressions.booleanOperation(Ops.EQ, Expressions.numberPath('id'), Expressions.constant(1))
        def nameExpr = Expressions.booleanOperation(Ops.EQ, Expressions.stringPath('name'), Expressions.constant('Jack'))
        def idBetweenExpr = Expressions.booleanOperation(Ops.BETWEEN, Expressions.numberPath('id'), Expressions.constant(100), Expressions.constant(200))
        def expr0 = Expressions.booleanOperation(Ops.AND, idExpr, nameExpr)
        def expr1 = Expressions.booleanOperation(Ops.OR, idExpr, nameExpr)
        def expr2 = Expressions.booleanOperation(Ops.XOR, idExpr, nameExpr)
        def expr3 = Expressions.booleanOperation(Ops.XNOR, idExpr, nameExpr)
        def expr4 = Expressions.booleanOperation(Ops.NOT, idExpr)
        def expr5 = Expressions.booleanOperation(Ops.NOT, expr0)
        def expr6 = Expressions.booleanOperation(Ops.OR, expr0, idBetweenExpr)

        def self = this
        def run = { sexpr, expr ->
            def result = SExpressionTranspiler.transpile(sexpr, self::simplePathResolver)
            assertTrue(result.isSuccess(), "表达式 ${sexpr} 转译失败")
            assertEquals(expr, result.get(), "表达式 ${sexpr} 转译错误")
        }
        run.call(sexpr0, expr0)
        run.call(sexpr1, expr1)
        run.call(sexpr2, expr2)
        run.call(sexpr3, expr3)
        run.call(sexpr4, expr4)
        run.call(sexpr5, expr5)
        run.call(sexpr6, expr6)
    }
}
