package com.ouroboros.data.normalize;

import java.util.List;

import io.vavr.control.Try;

import com.ouroboros.data.dsl.SExpression;
import com.querydsl.core.types.Operator;

/**
 * 表达式规范化上下文
 * <p>
 * 在表达式规范化过程中使用的上下文，提供表达式级别的信息和检查能力
 */
public class ExpressionNormalizeContext implements NormalizeContext {

  private final ClauseNormalizeContext clauseContext;
  private final String expressionPath;

  public ExpressionNormalizeContext(ClauseNormalizeContext clauseContext, String expressionPath) {
    this.clauseContext = clauseContext;
    this.expressionPath = expressionPath;
  }

  // 获取父上下文
  public ClauseNormalizeContext getClauseContext() {
    return clauseContext;
  }

  public QueryNormalizeContext getQueryContext() {
    return clauseContext.getQueryContext();
  }

  // 上下文信息
  public String getExpressionPath() {
    return expressionPath;
  }

  public String getClauseType() {
    return clauseContext.getClauseType();
  }

  // === NormalizeContext 接口实现（委托给 clauseContext）===

  @Override
  public Try<SExpression<?>> normalizeExpression(Object rawExpression, String expressionPath) {
    return clauseContext.normalizeExpression(rawExpression, expressionPath);
  }

  @Override
  public Try<SExpression<Boolean>> normalizeCondition(Object rawExpression, String expressionPath) {
    return clauseContext.normalizeCondition(rawExpression, expressionPath);
  }

  @Override
  public Try<SExpression<?>> buildSExpression(Operator operator, List<SExpression<?>> params, String expressionPath) {
    return clauseContext.buildSExpression(operator, params, expressionPath);
  }

}