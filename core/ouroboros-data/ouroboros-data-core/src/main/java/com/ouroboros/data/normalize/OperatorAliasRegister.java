package com.ouroboros.data.normalize;

import java.util.Map;

import com.querydsl.core.types.Operator;

/**
 * 操作符别名注册器
 */
public interface OperatorAliasRegister {
  void register(Map<String, Operator> registry);
}
