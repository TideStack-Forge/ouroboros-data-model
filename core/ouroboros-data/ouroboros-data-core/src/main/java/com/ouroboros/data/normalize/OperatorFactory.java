package com.ouroboros.data.normalize;

import java.util.Optional;
import java.util.function.Function;

import com.ouroboros.data.util.DataServices;
import com.querydsl.core.types.Operator;

/**
 * 操作符工厂
 */
public interface OperatorFactory extends Function<String, Optional<Operator>> {

  /**
   * 操作符工厂函数链
   */
  OperatorFactory OPERATOR_FACTORY_CHAIN =
      DataServices.getSortedServiceStream(OperatorFactory.class)
          .reduce(s -> Optional.empty(),
              (prev, curr) -> alias -> {
                var operator = curr.apply(alias);
                if (operator.isPresent()) {
                  return operator;
                }
                return prev.apply(alias);
              });

  /**
   * 通过操作符工厂链获取操作符
   *
   * @param alias 操作符别名
   * @return Operator
   */
  static Optional<Operator> getOperator(String alias) {
    return OPERATOR_FACTORY_CHAIN.apply(alias.toUpperCase());
  }
}
