package com.ouroboros.data.model.plugins;

import java.util.Optional;

import com.ouroboros.data.model.DataOperationIdentityProvider;

public class TestDataOperationIdentityProvider implements DataOperationIdentityProvider {

  public static final String OPERATOR = "data-operation-test-operator";

  @Override
  public Optional<String> findCurrentOperator() {
    return Optional.of(OPERATOR);
  }
}
