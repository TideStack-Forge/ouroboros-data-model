package com.ouroboros.data.transpile;

import java.util.Optional;

import io.vavr.control.Try;

import com.ouroboros.data.dsl.SExpression;
import com.querydsl.core.types.Expression;
import com.querydsl.core.types.Path;
import com.querydsl.core.types.Predicate;

/**
 * Transpile 阶段上下文
 *
 * <p>提供 Transpile 阶段所需的上下文信息，包括：
 * <ul>
 *   <li>字段路径解析（resolve API）</li>
 *   <li>表/别名解析</li>
 *   <li>SExpressionTranspiler 查找</li>
 * </ul>
 *
 * <p><b>设计理念：隐式作用域</b>：
 * <ul>
 *   <li>调用者无需指定场景（WHERE/ORDER BY），只需调用 {@link #resolve(String)}</li>
 *   <li>场景信息由 Context 链的类型/状态隐式表达</li>
 *   <li>不同 ClauseBuilder 使用不同的 Context 链，自动具有正确的解析行为</li>
 * </ul>
 *
 * @since 1.0.0-beta.2
 */
public interface TranspileContext {

  // ========== 核心 API：调用者只需使用这两个方法 ==========

  /**
   * 解析字段（不指定表）
   *
   * <p>调用者无需关心当前场景，Context 链自动按正确优先级查找：
   * <ul>
   *   <li>ColumnAliasTranspileContext 会先查列别名</li>
   *   <li>JoinTranspileContext 会先查 JOIN 表；若多个 JOIN 来源同名则抛出歧义异常</li>
   *   <li>基础 Context 会查主表</li>
   * </ul>
   *
   * @param field 字段名
   * @return 解析后的 Path，找不到返回 empty
   * @throws AmbiguousFieldException 当同一优先级的多个来源同时匹配时
   */
  Optional<Path<?>> resolve(String field);

  /**
   * 解析字段（指定表名或别名）
   *
   * @param tableOrAlias 表名或别名
   * @param field        字段名
   * @return 解析后的 Path，找不到返回 empty
   */
  Optional<Path<?>> resolve(String tableOrAlias, String field);

  // ========== 辅助方法 ==========

  /**
   * 解析表（通过表名或别名）
   *
   * @param nameOrAlias 表名或别名
   * @return FieldSource，找不到返回 empty
   */
  Optional<FieldSource> resolveTable(String nameOrAlias);

  // ========== SExpressionTranspiler 查找 ==========

  /**
   * 获取支持该表达式的 SExpressionTranspiler
   *
   * <p>默认实现通过 SPI 查找，代理类可以拦截并返回自定义实现。
   *
   * @param expr 表达式
   * @return SExpressionTranspiler，如果不存在返回 empty
   */
  Optional<SExpressionTranspiler> getSExpressionTranspiler(SExpression<?> expr);

  // ========== QueryTranspiler 获取 ==========

  /**
   * 获取用于子查询转译的 QueryTranspiler 实例
   *
   * <p>SubQueryTranspiler 使用此方法获取 QueryTranspiler 来转译子查询。
   * 默认返回 QueryTranspiler.DEFAULT，代理类可以重写以返回定制实现。
   *
   * <p><b>使用场景</b>：
   * <ul>
   *   <li>SubQueryTranspiler 转译 SUBQUERY 操作符</li>
   *   <li>支持 DataModel 级别的 QueryTranspiler 定制</li>
   * </ul>
   *
   * @return QueryTranspiler 实例
   * @since 1.0.0-beta.2
   */
  default QueryTranspiler getQueryTranspiler() {
    return QueryTranspiler.DEFAULT;
  }

  // ========== 转译方法 ==========

  /**
   * 转译 SExpression 为 SQL Expression
   *
   * <p>查找对应的 SExpressionTranspiler 并调用，默认传递 this 作为 context。
   *
   * @param sExpr 要转译的 SExpression
   * @return 转译结果
   * @since 1.0.0-beta.2
   */
  default Try<Expression<?>> transpile(SExpression<?> sExpr) {
    return transpile(sExpr, this);
  }

  /**
   * 转译 SExpression 为 SQL Expression，使用指定的 context
   *
   * <p>当通过装饰器模式调用被代理的 Context 实例时，需要传递装饰器对象实例，
   * 以确保 Transpiler 使用正确的 context（包含装饰器添加的功能）。
   *
   * <p><b>使用场景</b>：
   * <ul>
   *   <li>普通调用：使用 {@link #transpile(SExpression)} 即可</li>
   *   <li>装饰器内部：委托给 delegate 时传递 this（装饰器实例）</li>
   * </ul>
   *
   * @param sExpr   要转译的 SExpression
   * @param context 用于转译的 context（通常是装饰器实例）
   * @return 转译结果
   * @since 1.0.0-beta.2
   */
  default Try<Expression<?>> transpile(SExpression<?> sExpr, TranspileContext context) {
    return SExpressionTranspiler.transpile(sExpr, context);
  }

  /**
   * 转译谓词表达式（返回 Predicate 类型）
   *
   * @param sExpr 要转译的 SExpression（必须是谓词类型）
   * @return 转译结果
   * @since 1.0.0-beta.2
   */
  default Try<Predicate> transpilePredicate(SExpression<?> sExpr) {
    return transpilePredicate(sExpr, this);
  }

  /**
   * 转译谓词表达式，使用指定的 context
   *
   * @param sExpr   要转译的 SExpression（必须是谓词类型）
   * @param context 用于转译的 context
   * @return 转译结果
   * @since 1.0.0-beta.2
   */
  default Try<Predicate> transpilePredicate(SExpression<?> sExpr, TranspileContext context) {
    return SExpressionTranspiler.transpilePredicate(sExpr, context);
  }
}
