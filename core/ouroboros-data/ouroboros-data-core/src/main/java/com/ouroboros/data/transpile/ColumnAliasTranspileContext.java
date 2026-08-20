package com.ouroboros.data.transpile;

import java.util.Map;
import java.util.Optional;

import com.querydsl.core.types.Path;

/**
 * 添加列别名解析能力的 Context 代理
 *
 * <p>使用场景：SelectBuilder 处理完 SELECT 子句后，包装一层此代理，
 * 后续的 OrderBuilder、HavingBuilder 就能自动解析列别名。
 *
 * <p>解析顺序：
 * <ol>
 *   <li>先查列别名（columnAliases）</li>
 *   <li>再委托给原 Context（查 JOIN 表、主表等）</li>
 * </ol>
 *
 * @since 1.0.0-beta.2
 */
public class ColumnAliasTranspileContext extends DelegatingTranspileContext {

  private final Map<String, Path<?>> columnAliases;

  /**
   * 构造列别名 Context
   *
   * @param delegate      被包装的 Context
   * @param columnAliases 列别名映射（别名 -> Path）
   */
  public ColumnAliasTranspileContext(TranspileContext delegate, Map<String, Path<?>> columnAliases) {
    super(delegate);
    this.columnAliases = columnAliases;
  }

  // ========== 核心 resolve API ==========

  @Override
  public Optional<Path<?>> resolve(String field) {
    // 先查列别名
    Path<?> aliasPath = columnAliases.get(field);
    if (aliasPath != null) {
      return Optional.of(aliasPath);
    }
    // 再委托给原 Context（查 JOIN 表、主表等）
    return getDelegate().resolve(field);
  }

  @Override
  public Optional<Path<?>> resolve(String tableOrAlias, String field) {
    // 指定了表名，直接委托（列别名不带表前缀）
    return getDelegate().resolve(tableOrAlias, field);
  }

  /**
   * 获取被包装的 Context
   *
   * @return 被包装的 Context
   */
  @Override
  public TranspileContext getDelegate() {
    return super.getDelegate();
  }
}
