package com.ouroboros.data.transpile;

import java.util.Optional;

import com.ouroboros.data.dsl.SExpression;
import com.querydsl.core.types.Path;

/**
 * 委托模式基类，统一装饰器的委托行为
 *
 * <p>子类只需覆盖有差异的 resolve 方法，其余自动委托给 delegate。
 *
 * @since 1.0.0-beta.2
 */
public abstract class DelegatingTranspileContext implements TranspileContext {

  private final TranspileContext delegate;

  protected DelegatingTranspileContext(TranspileContext delegate) {
    this.delegate = delegate;
  }

  protected TranspileContext getDelegate() {
    return delegate;
  }

  @Override
  public Optional<Path<?>> resolve(String field) {
    return delegate.resolve(field);
  }

  @Override
  public Optional<Path<?>> resolve(String tableOrAlias, String field) {
    return delegate.resolve(tableOrAlias, field);
  }

  @Override
  public Optional<FieldSource> resolveTable(String nameOrAlias) {
    return delegate.resolveTable(nameOrAlias);
  }

  @Override
  public Optional<SExpressionTranspiler> getSExpressionTranspiler(SExpression<?> expr) {
    return delegate.getSExpressionTranspiler(expr);
  }

  @Override
  public QueryTranspiler getQueryTranspiler() {
    return delegate.getQueryTranspiler();
  }
}
