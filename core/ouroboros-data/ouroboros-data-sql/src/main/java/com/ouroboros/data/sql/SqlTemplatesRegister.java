package com.ouroboros.data.sql;

import java.util.Map;

/**
 * QueryDSL 模版生成器注册器
 */
public interface SqlTemplatesRegister {
  void register(Map<String, SQLTemplatesSupplier> registry);
}
