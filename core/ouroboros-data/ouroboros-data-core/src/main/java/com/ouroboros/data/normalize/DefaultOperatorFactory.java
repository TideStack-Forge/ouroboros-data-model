package com.ouroboros.data.normalize;

import java.util.HashMap;
import java.util.Map;
import java.util.Optional;

import jakarta.annotation.Priority;

import com.ouroboros.data.util.DataServices;
import com.querydsl.core.types.Operator;

@Priority(0)
public final class DefaultOperatorFactory implements OperatorFactory {

  private final Map<String, Operator> OPERATOR_MAP = new HashMap<>();

  public DefaultOperatorFactory() {
    DataServices.getSortedServiceStream(OperatorAliasRegister.class)
        .forEach(register -> register.register(OPERATOR_MAP));
  }

  @Override
  public Optional<Operator> apply(String alias) {
    return Optional.ofNullable(OPERATOR_MAP.get(alias));
  }
}
