package com.ouroboros.data.transpile;

import java.util.List;

/**
 * 字段歧义异常
 *
 * <p>当 {@link TranspileContext#resolve(String)} 在同一优先级找到多个匹配字段时抛出此异常。
 *
 * <p><b>使用场景</b>：
 * <ul>
 *   <li>多个 JOIN 表都有同名字段</li>
 * </ul>
 *
 * <p><b>解决方法</b>：
 * 使用 {@link TranspileContext#resolve(String, String)} 明确指定表名或别名。
 *
 * @since 1.0.0-beta.2
 */
public class AmbiguousFieldException extends RuntimeException {

  private final String fieldName;
  private final List<String> matchedSources;

  /**
   * 构造字段歧义异常
   *
   * @param fieldName      字段名
   * @param matchedSources 匹配到的来源列表（表名或别名）
   */
  public AmbiguousFieldException(String fieldName, List<String> matchedSources) {
    super(buildMessage(fieldName, matchedSources));
    this.fieldName = fieldName;
    this.matchedSources = matchedSources;
  }

  private static String buildMessage(String fieldName, List<String> sources) {
    return "字段 '" + fieldName + "' 存在于多个表中: " + String.join(", ", sources) +
        "。请使用 resolve(tableOrAlias, field) 明确指定表名或别名。";
  }

  /**
   * 获取字段名
   *
   * @return 字段名
   */
  public String getFieldName() {
    return fieldName;
  }

  /**
   * 获取匹配到的来源列表
   *
   * @return 来源列表（表名或别名）
   */
  public List<String> getMatchedSources() {
    return matchedSources;
  }
}
