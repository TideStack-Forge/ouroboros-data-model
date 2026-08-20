package com.ouroboros.data.analyze;

import com.ouroboros.data.model.DataModel;

/**
 * 查询分析上下文
 *
 * <p>提供查询分析阶段所需的上下文信息，包括场景、模型元数据和分析选项。
 *
 * <p><b>典型用法：</b>
 * <pre>{@code
 * // 使用 Builder 创建
 * QueryAnalyzeContext context = QueryAnalyzeContext.builder()
 *     .model(orderModel)
 *     .build();
 * }</pre>
 *
 * @see QueryAnalyzer
 * @since 1.0.0-beta.2
 */
public class QueryAnalyzeContext {

  /**
   * 数据模型（可选）
   */
  private final DataModel model;

  /**
   * 是否启用类型检查（默认: true）
   *
   * <p>类型检查包括：
   * <ul>
   *   <li>字段是否存在于模型中</li>
   *   <li>操作符是否适用于字段类型</li>
   *   <li>常量值类型是否与字段类型兼容</li>
   * </ul>
   */
  private final boolean enableTypeChecking;

  /**
   * 是否启用查询优化（默认: true）
   *
   * <p>查询优化包括：
   * <ul>
   *   <li>常量折叠（如: AND(TRUE, x) → x）</li>
   *   <li>条件简化（如: OR(x, FALSE) → x）</li>
   *   <li>冗余表达式移除（如: AND(x, TRUE) → x）</li>
   *   <li>布尔逻辑优化（如: NOT(NOT(x)) → x）</li>
   * </ul>
   */
  private final boolean enableOptimization;

  private QueryAnalyzeContext(Builder builder) {
    this.model = builder.model;
    this.enableTypeChecking = builder.enableTypeChecking;
    this.enableOptimization = builder.enableOptimization;
  }

  /**
   * 创建 Builder
   *
   * @return Builder 实例
   */
  public static Builder builder() {
    return new Builder();
  }

  // ===== Getters =====

  public DataModel getModel() {
    return model;
  }

  public boolean isTypeCheckingEnabled() {
    return enableTypeChecking;
  }

  public boolean isOptimizationEnabled() {
    return enableOptimization;
  }

  // ===== Builder =====

  public static class Builder {
    private DataModel model;
    private boolean enableTypeChecking = true;
    private boolean enableOptimization = true;

    private Builder() {
    }

    /**
     * 设置数据模型
     *
     * @param model 数据模型
     * @return Builder
     */
    public Builder model(DataModel model) {
      this.model = model;
      return this;
    }

    /**
     * 设置是否启用类型检查
     *
     * @param enable true 启用，false 禁用
     * @return Builder
     */
    public Builder enableTypeChecking(boolean enable) {
      this.enableTypeChecking = enable;
      return this;
    }

    /**
     * 设置是否启用查询优化
     *
     * @param enable true 启用，false 禁用
     * @return Builder
     */
    public Builder enableOptimization(boolean enable) {
      this.enableOptimization = enable;
      return this;
    }

    /**
     * 构建 QueryAnalyzeContext
     *
     * @return QueryAnalyzeContext 实例
     */
    public QueryAnalyzeContext build() {
      return new QueryAnalyzeContext(this);
    }
  }
}
