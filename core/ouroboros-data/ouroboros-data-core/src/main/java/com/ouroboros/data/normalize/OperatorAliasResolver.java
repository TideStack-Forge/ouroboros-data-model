package com.ouroboros.data.normalize;

import java.util.Map;
import java.util.Optional;
import java.util.concurrent.ConcurrentHashMap;

import com.ouroboros.data.normalize.OperatorFactory;
import com.ouroboros.data.exception.NormalizeException;
import com.querydsl.core.types.Operator;

/**
 * 操作符别名解析器（适配器）
 *
 * <p>桥接旧系统的 {@link OperatorFactory} 和新系统的 normalizers，提供统一的操作符别名解析接口。
 *
 * <p><b>主要功能</b>:
 * <ul>
 *   <li>大写转换：与 {@link OperatorFactory} 的约定保持一致</li>
 *   <li>结果缓存：避免重复查询，提升性能</li>
 *   <li>委托查询：复用旧系统成熟的别名配置机制</li>
 * </ul>
 *
 * <p><b>$ 前缀设计决策</b>: $ 前缀处理已收口到
 * {@code com.ouroboros.data.normalize.expressionnormalizers.MapExpressionNormalizer}。
 * 调用方需在调用前自行去除 $ 前缀。
 *
 * <p><b>支持的别名格式</b>:
 * <ul>
 *   <li>标准格式: {@code GT}, {@code EQ}, {@code AND}</li>
 *   <li>小写格式: {@code gt}, {@code eq}, {@code and}</li>
 *   <li>配置别名: {@code >}, {@code greaterThan}, {@code ~}</li>
 * </ul>
 *
 * <p><b>使用示例</b>:
 * <pre>{@code
 * // 解析操作符（抛异常版本）
 * Operator op1 = OperatorAliasResolver.resolveOperator("gt");   // Ops.GT
 * Operator op2 = OperatorAliasResolver.resolveOperator("GT");   // Ops.GT
 * Operator op3 = OperatorAliasResolver.resolveOperator(">");    // Ops.GT
 *
 * // 尝试解析操作符（返回 Optional）
 * Optional<Operator> op4 = OperatorAliasResolver.tryResolveOperator("unknown");  // Optional.empty()
 * }</pre>
 *
 * @see OperatorFactory
 * @since 1.0.0-beta.2
 */
public final class OperatorAliasResolver {

  /**
   * 缓存：别名 -> 操作符的映射
   *
   * <p>Key: 原始别名（通常是不带 $ 前缀的输入，可能是小写）
   * <p>Value: Optional 包装的操作符（空表示未找到）
   *
   * <p>使用 {@link ConcurrentHashMap} 保证线程安全，使用 {@link Optional} 缓存"未找到"的结果。
   */
  private static final Map<String, Optional<Operator>> CACHE = new ConcurrentHashMap<>();

  /**
   * 私有构造函数，防止实例化
   */
  private OperatorAliasResolver() {
    throw new UnsupportedOperationException("Utility class");
  }

  /**
   * 解析操作符别名（抛异常版本）
   *
   * <p>如果别名无法解析，抛出 {@link NormalizeException}。
   *
   * @param alias 操作符别名（例如 {@code "gt"}, {@code "GT"}, {@code ">"}）
   * @return 对应的 QueryDSL 操作符
   * @throws NormalizeException 如果别名无法解析
   */
  public static Operator resolveOperator(String alias) {
    return tryResolveOperator(alias)
        .orElseThrow(() -> new NormalizeException("未知的操作符: " + alias));
  }

  /**
   * 尝试解析操作符别名（返回 Optional）
   *
   * <p>如果别名无法解析，返回 {@link Optional#empty()}。
   *
   * <p><b>实现说明</b>:
   * <ol>
   *   <li>先从缓存中查找</li>
   *   <li>如果缓存未命中，规范化别名（转大写）</li>
   *   <li>委托给 {@link OperatorFactory#getOperator(String)}</li>
   *   <li>将结果（包括空值）缓存</li>
   * </ol>
   *
   * @param alias 操作符别名（例如 {@code "gt"}, {@code "GT"}, {@code ">"}）
   * @return 对应的 QueryDSL 操作符，如果未找到则返回 {@link Optional#empty()}
   */
  public static Optional<Operator> tryResolveOperator(String alias) {
    return CACHE.computeIfAbsent(alias, key -> {
      String normalized = normalizeAlias(key);
      return OperatorFactory.getOperator(normalized);
    });
  }

  /**
   * 规范化别名
   *
   * <p><b>规范化规则</b>:
   * <ol>
   *   <li>转大写: {@code "gt"} → {@code "GT"}</li>
   * </ol>
   *
   * <p>$ 前缀处理已移至
   * {@code com.ouroboros.data.normalize.expressionnormalizers.MapExpressionNormalizer}，
   * 调用方需在调用前自行去除 $ 前缀。
   *
   * @param alias 原始别名（不应包含 $ 前缀）
   * @return 规范化后的别名
   */
  private static String normalizeAlias(String alias) {
    return alias.toUpperCase();
  }
}
