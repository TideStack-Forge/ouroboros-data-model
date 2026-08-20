package com.ouroboros.data.dsl;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;

import com.ouroboros.data.util.DataJson;
import com.querydsl.core.types.Operator;

public class SExpression<T> implements Serializable {
  private static final long serialVersionUID = 1L;

  private final Class<T> dataType;
  private Operator operator;
  private List<Object> params;

  @SuppressWarnings("unchecked")
  protected SExpression(Operator operator, List<Object> list) {
    this.operator = operator;
    this.params = list;
    this.dataType = (Class<T>) operator.getType();
  }

  protected SExpression(Class<T> type, Operator operator, List<Object> list) {
    this.operator = operator;
    this.params = new ArrayList<>(list);
    this.dataType = type;
  }

  public static <T> SExpression<T> create(Operator op, List<?> list) {
    return new SExpression<>(op, new ArrayList<>(list));
  }

  public static <T> SExpression<T> create(Operator op, Object... args) {
    return create(op, Arrays.asList(args));
  }

  public static SExpression<?> field(String... segments) {
    return create(Operators.FIELD, (Object[]) segments);
  }

  public static SExpression<?> field(List<String> segments) {
    return create(Operators.FIELD, new ArrayList<>(segments));
  }

  @SuppressWarnings("unchecked")
  public static <T> SExpression<T> constant(Object constValue) {
    return new SExpression<>((Class<T>) constValue.getClass(), Operators.CONSTANT, new ArrayList<>(Collections.singletonList(constValue)));
  }

  public static SExpression<?> alias(SExpression<?> expression, String alias) {
    return create(Operators.ALIAS, expression, alias);
  }

  public static SExpression<?> columns(SExpression<?>... expressions) {
    return create(Operators.COLUMNS, (Object[]) expressions);
  }

  public static SExpression<?> columns(List<? extends SExpression<?>> expressions) {
    return create(Operators.COLUMNS, new ArrayList<>(expressions));
  }

  public static <T> SExpression<T> empty(Class<T> type) {
    return new SExpression<>(type, null, Collections.emptyList());
  }

  public static SExpression<?> empty() {
    return new SExpression<>(Object.class, null, Collections.emptyList());
  }

  public Operator getOperator() {
    return operator;
  }

  /**
   * 设置操作符
   *
   * @param operator 新的操作符
   * @since 1.0.0-beta.2
   */
  public void setOperator(Operator operator) {
    this.operator = operator;
  }

  public Object getParam(int index) {
    return params.get(index);
  }

  public SExpression<?> getParamAsSExpression(int index) {
    return (SExpression<?>) params.get(index);
  }

  public void updateParam(int index, Object value) {
    params.set(index, value);
  }

  /**
   * 设置指定位置的参数
   *
   * @param index 参数索引
   * @param value 新值
   * @since 1.0.0-beta.2
   */
  public void setParam(int index, Object value) {
    params.set(index, value);
  }

  /**
   * 添加参数到末尾
   *
   * @param value 参数值
   * @since 1.0.0-beta.2
   */
  public void addParam(Object value) {
    params.add(value);
  }

  /**
   * 移除指定位置的参数
   *
   * @param index 参数索引
   * @since 1.0.0-beta.2
   */
  public void removeParam(int index) {
    params.remove(index);
  }

  public List<Object> getParams() {
    return params;
  }

  /**
   * 替换所有参数
   *
   * @param newParams 新的参数列表
   * @since 1.0.0-beta.2
   */
  public void setParams(List<Object> newParams) {
    this.params = new ArrayList<>(newParams);
  }

  @SuppressWarnings("unchecked")
  public List<SExpression<?>> getParamsAsSExpression() {
    return (List<SExpression<?>>) (List<?>) params;
  }

  public Class<T> getDataType() {
    return dataType;
  }

  @Override
  public boolean equals(Object obj) {
    if (!(obj instanceof SExpression<?> sExpr)) {
      return super.equals(obj);
    }
    if (sExpr.getOperator() != getOperator()) {
      return false;
    }
    if (sExpr.getParams().size() != getParams().size()) {
      return false;
    }
    return sExpr.getOperator() == getOperator() && sExpr.getParams().equals(getParams());
  }

  @Override
  public String toString() {
    return DataJson.toJsonString(this);
  }

  public boolean isEmpty() {
    return operator == null && params.isEmpty();
  }

  /**
   * 深度优先遍历表达式树（先序遍历）
   * <p>
   * 先访问当前节点，再递归访问子节点中的 SExpression。
   *
   * @param visitor 访问器
   * @since 1.0.0-beta.2
   */
  public void walk(SExpressionVisitor visitor) {
    walk(visitor, SExpressionTraversalContext.root());
  }

  private void walk(SExpressionVisitor visitor, SExpressionTraversalContext context) {
    // 1. 先访问当前节点
    visitor.visit(this, context);

    // 2. 再递归访问子节点
    for (int i = 0; i < params.size(); i++) {
      Object param = params.get(i);
      if (param instanceof SExpression<?> expression) {
        expression.walk(visitor, context.child(this, i));
      }
    }
  }

  /**
   * 转换表达式树（后序遍历，返回新树）
   * <p>
   * 先递归转换所有子节点，再对当前节点应用转换函数。
   * 不修改原表达式，返回新的表达式树。
   *
   * @param transformer 转换函数
   * @return 转换后的新表达式
   * @since 1.0.0-beta.2
   */
  public SExpression<?> transform(SExpressionTransformer transformer) {
    return transform(transformer, SExpressionTraversalContext.root());
  }

  private SExpression<?> transform(SExpressionTransformer transformer, SExpressionTraversalContext context) {
    // 1. 先递归转换所有子节点
    List<Object> transformedParams = new ArrayList<>();
    for (int i = 0; i < params.size(); i++) {
      Object param = params.get(i);
      if (param instanceof SExpression<?> expression) {
        transformedParams.add(expression.transform(transformer, context.child(this, i)));
      } else {
        transformedParams.add(param);
      }
    }

    // 2. 创建新表达式（使用转换后的子节点）
    SExpression<?> newExpression = isEmpty()
        ? SExpression.empty(dataType)
        : SExpression.create(operator, transformedParams);

    // 3. 应用转换函数
    return transformer.transform(newExpression, context);
  }
}
