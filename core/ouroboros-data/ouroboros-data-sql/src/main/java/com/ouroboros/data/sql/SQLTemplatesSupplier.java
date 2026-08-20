package com.ouroboros.data.sql;

import java.util.function.Supplier;

import com.querydsl.sql.SQLTemplates;

/**
 * QueryDSL 模版生成器
 */
public interface SQLTemplatesSupplier extends Supplier<SQLTemplates> {
  default String getName() {
    return null;
  }
}
