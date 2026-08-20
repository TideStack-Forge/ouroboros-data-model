package com.ouroboros.data.model;

import java.util.Map;

/**
 * 主键生成器
 *
 * @author Song Mingxu
 */
public interface PrimaryKeyGenerator<T> {
  /**
   * 生成主键
   *
   * @return 主键
   */
  T next(Map<String, Object> record);
}
