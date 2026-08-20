package com.ouroboros.data.dsl;

/**
 * SExpression 访问器接口
 * <p>
 * 用于遍历 SExpression 树结构，对每个节点执行自定义操作。
 *
 * @since 1.0.0-beta.2
 */
@FunctionalInterface
public interface SExpressionVisitor {
  /**
   * 访问表达式节点
   *
   * @param expression 当前节点
   * @param context    当前遍历上下文
   */
  void visit(SExpression<?> expression, SExpressionTraversalContext context);
}
