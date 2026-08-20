package com.ouroboros.data.transpile;

import java.util.Optional;

import com.ouroboros.data.dsl.SExpression;
import com.querydsl.core.types.Path;

/**
 * 子查询 Context
 *
 * <p>特点：
 * <ul>
 *   <li>有自己的主表和 JOIN 表（localContext）</li>
 *   <li>可以 fallback 到父 Context 查找外层表（相关子查询）</li>
 *   <li>父 Context 的列别名也可被访问（SQL 标准允许）</li>
 * </ul>
 *
 * <p>解析顺序：
 * <ol>
 *   <li>先在本地 Context 查找</li>
 *   <li>本地找不到，fallback 到父 Context（相关子查询场景）</li>
 * </ol>
 *
 * @since 1.0.0-beta.2
 */
public class SubqueryTranspileContext implements TranspileContext {

  private final TranspileContext parent;
  private final TranspileContext localContext;

  private static final TranspileContext EMPTY_LOCAL = new TranspileContext() {
    @Override public Optional<Path<?>> resolve(String field) { return Optional.empty(); }
    @Override public Optional<Path<?>> resolve(String tableOrAlias, String field) { return Optional.empty(); }
    @Override public Optional<FieldSource> resolveTable(String nameOrAlias) { return Optional.empty(); }
    @Override public Optional<SExpressionTranspiler> getSExpressionTranspiler(SExpression<?> expr) { return Optional.empty(); }
  };

  /**
   * 构造子查询 Context（空 localContext，仅 parent fallback）
   *
   * @param parent 父 Context（外层查询的 Context）
   */
  public SubqueryTranspileContext(TranspileContext parent) {
    this.parent = parent;
    this.localContext = EMPTY_LOCAL;
  }

  // ========== 核心 resolve API ==========

  @Override
  public Optional<Path<?>> resolve(String field) {
    // 先在本地查找
    Optional<Path<?>> local = localContext.resolve(field);
    if (local.isPresent()) {
      return local;
    }
    // 本地找不到，fallback 到父 Context（相关子查询场景）
    return parent.resolve(field);
  }

  @Override
  public Optional<Path<?>> resolve(String tableOrAlias, String field) {
    // 先在本地查找
    Optional<Path<?>> local = localContext.resolve(tableOrAlias, field);
    if (local.isPresent()) {
      return local;
    }
    // 本地找不到，fallback 到父 Context
    return parent.resolve(tableOrAlias, field);
  }

  @Override
  public Optional<FieldSource> resolveTable(String nameOrAlias) {
    // 先在本地查找
    Optional<FieldSource> local = localContext.resolveTable(nameOrAlias);
    if (local.isPresent()) {
      return local;
    }
    // 本地找不到，fallback 到父 Context
    return parent.resolveTable(nameOrAlias);
  }

  // ========== SExpressionTranspiler 查找 ==========

  @Override
  public Optional<SExpressionTranspiler> getSExpressionTranspiler(SExpression<?> expr) {
    // 使用本地 Context 的 SExpressionTranspiler 查找
    return localContext.getSExpressionTranspiler(expr);
  }

  // ========== QueryTranspiler 获取 ==========

  @Override
  public QueryTranspiler getQueryTranspiler() {
    return parent.getQueryTranspiler();
  }
}
